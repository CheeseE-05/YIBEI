#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# 模拟器截图：构建(可选) → 启动模拟器 → 安装 → 逐页启动 → 截图
#
# 每一页用 App 的 DEBUG 启动参数直达：
#   xcrun simctl launch <udid> com.brewkin.beanbook.ios -uiTestTab home
# 然后：
#   xcrun simctl io <udid> screenshot ios/artifacts/screenshots/home.png
#
# 截图页签默认依次为 home / beans / maker / menu / stats；-uiTestTab 只在
# Debug 构建里生效（Release 构建会忽略该参数，也会走默认首页）。
#
# 用法：
#   bash ios/scripts/run-simulator-screenshots.sh
#   bash ios/scripts/run-simulator-screenshots.sh --skip-build
#   TABS="home stats" bash ios/scripts/run-simulator-screenshots.sh
#   bash ios/scripts/run-simulator-screenshots.sh --sim "iPhone 16 Pro" --settle 5
#
# 环境变量：
#   SIM_NAME        模拟器名称；留空自动选择（优先 iPhone 17/16 系列）
#   SIM_UDID        模拟器 UDID（优先级高于 SIM_NAME）
#   DERIVED_DATA    DerivedData 目录（默认 ios/build；相对路径按仓库根目录解析）
#   TABS            截图页签，空格或逗号分隔（默认 "home beans maker menu stats"）
#   SETTLE_SECONDS  每次启动后等待界面稳定的秒数（默认 3）
#   BUNDLE_ID       App bundle id（默认 com.brewkin.beanbook.ios）
#   SKIP_BUILD      设为 1 跳过构建（默认 0，即会先构建模拟器版）
#   OPEN_SIMULATOR  设为 1 时打开 Simulator.app（本机观察用；CI 不需要）
#   STATUS_BAR      设为 0 时不固定状态栏（默认 1：9:41 / 满电）
#   SIM_DISPLAY     多显示器设备（折叠屏等）截图所用的 display 编号；留空自动检测
#   ROUTES          额外截取的二级页面（默认空；可选 bean recipe data newbean）
#
# 产物：ios/artifacts/screenshots/<tab>.png（默认目录，可用 --out 覆盖）
# 依赖：完整 Xcode（xcrun simctl）、python3。
# ---------------------------------------------------------------------------
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IOS_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$IOS_DIR/.." && pwd)"
BUILD_SCRIPT="$SCRIPT_DIR/build-simulator.sh"

APP_NAME="BeanBook"
CONFIGURATION="Debug"
BUNDLE_ID="${BUNDLE_ID:-com.brewkin.beanbook.ios}"
TABS="${TABS:-home beans maker menu stats}"
SETTLE_SECONDS="${SETTLE_SECONDS:-3}"
SKIP_BUILD="${SKIP_BUILD:-0}"
OPEN_SIMULATOR="${OPEN_SIMULATOR:-0}"
STATUS_BAR="${STATUS_BAR:-1}"
SHOT_DIR="${SHOT_DIR:-$IOS_DIR/artifacts/screenshots}"
APP=""
SIM_NAME="${SIM_NAME:-}"
SIM_UDID="${SIM_UDID:-}"
SIM_RUNTIME=""
SIM_DISPLAY="${SIM_DISPLAY:-}"
ROUTES="${ROUTES:-}"

if [[ -n "${DERIVED_DATA:-}" ]]; then
  case "$DERIVED_DATA" in
    /*) ;;
    *) DERIVED_DATA="$REPO_ROOT/$DERIVED_DATA" ;;
  esac
else
  DERIVED_DATA="$IOS_DIR/build"
fi

log() { printf '==> %s\n' "$*"; }
die() { printf '错误：%s\n' "$*" >&2; exit 1; }

usage() {
  cat <<'EOF'
用法: bash ios/scripts/run-simulator-screenshots.sh [选项]

选项:
  --app <路径>         已构建的模拟器 .app（默认自动查找 Debug 产物）
  --out <目录>         截图输出目录（默认 ios/artifacts/screenshots）
  --sim <名称>         指定模拟器（默认自动选择 iPhone 17/16 优先）
  --sim-udid <UDID>    指定模拟器 UDID（优先级最高）
  --derived-data <目录> DerivedData 目录（默认 ios/build）
  --tabs "<列表>"      截图页签，空格/逗号分隔（默认 home beans maker menu stats）
  --settle <秒>        每次启动后等待秒数（默认 3）
  --bundle-id <id>     App bundle id（默认 com.brewkin.beanbook.ios）
  --skip-build         跳过构建，直接用已有产物
  --open-simulator     打开 Simulator.app 方便观察
  --no-status-bar      不固定状态栏时间/电量
  --display <编号>     多显示器设备（折叠屏等）截图所用 display（一般留空自动检测）
  --routes "<列表>"    额外截取二级页面：bean（豆子详情）/ recipe（配方详情）/
                       data（数据与分享）/ newbean（添加豆子），默认不截
  -h, --help           显示本帮助

环境变量: SIM_NAME, SIM_UDID, DERIVED_DATA, TABS, SETTLE_SECONDS, BUNDLE_ID,
          SKIP_BUILD, OPEN_SIMULATOR, STATUS_BAR, SIM_DISPLAY, ROUTES
EOF
}

require_simctl() {
  command -v xcrun >/dev/null 2>&1 \
    || die "找不到 xcrun：需要安装完整 Xcode（只有 Command Line Tools 时无法使用模拟器）。"
  xcrun simctl help >/dev/null 2>&1 \
    || die "xcrun simctl 不可用：需要完整 Xcode，而不是仅 Command Line Tools。
     没有 Mac 版 Xcode 时，请在 GitHub Actions 的 macos-26 runner 上运行本脚本。"
}

# 解析可用 iPhone 模拟器，结果写入全局 SIM_NAME / SIM_UDID / SIM_RUNTIME。
# 与 build-simulator.sh 中同一套选择规则（两处保持一致）。
detect_simulator() {
  command -v python3 >/dev/null 2>&1 \
    || die "需要 python3 来解析 simctl 输出（macOS 12.3+ 自带 /usr/bin/python3）。"

  local json
  json="$(xcrun simctl list devices available --json 2>/dev/null)" \
    || die "无法执行 'xcrun simctl list devices available --json'：需要完整 Xcode，而不是仅 Command Line Tools。"

  local resolved
  resolved="$(SIM_JSON="$json" python3 - "${SIM_NAME:-}" "${SIM_UDID:-}" <<'PY'
import json
import os
import re
import sys

want_name = sys.argv[1].strip() if len(sys.argv) > 1 else ""
want_udid = sys.argv[2].strip() if len(sys.argv) > 2 else ""
data = json.loads(os.environ["SIM_JSON"])

# 候选设备：只考虑可用 iPhone，记录 iOS 版本用于排序。
candidates = []
for runtime_id, devices in data.get("devices", {}).items():
    match = re.search(r"iOS-(\d+)-(\d+)", runtime_id)
    version = (int(match.group(1)), int(match.group(2))) if match else (0, 0)
    runtime = "iOS %d.%d" % version if match else runtime_id
    for device in devices:
        name = device.get("name", "")
        udid = device.get("udid", "")
        if not name.startswith("iPhone") or not udid:
            continue
        if not device.get("isAvailable", True):
            continue
        if want_udid and udid != want_udid:
            continue
        if want_name and name.lower() != want_name.lower():
            continue
        candidates.append({
            "name": name,
            "udid": udid,
            "runtime": runtime,
            "version": version,
            "booted": device.get("state") == "Booted",
        })

if not candidates:
    detail = ""
    if want_udid:
        detail = "（SIM_UDID=%s）" % want_udid
    elif want_name:
        detail = "（SIM_NAME=%s）" % want_name
    sys.stderr.write("错误：没有找到可用的 iPhone 模拟器%s。\n" % detail)
    sys.stderr.write("      请在 Xcode 的 Settings > Platforms 里安装 iOS 模拟器运行时，或用 --sim 指定已存在的机型。\n")
    sys.exit(1)

if want_name or want_udid:
    # 指定机型时：已启动优先，其次选最新 iOS 运行时。
    best = max(candidates, key=lambda c: (c["booted"], c["version"]))
else:
    # 自动模式：机型优先级 -> 已启动优先 -> 新运行时优先 -> 名称稳定排序。
    patterns = [
        r"^iPhone 17$", r"^iPhone 17 Pro$", r"^iPhone 17 Pro Max$",
        r"^iPhone 16$", r"^iPhone 16 Pro$", r"^iPhone 16 Pro Max$",
        r"^iPhone 17e$", r"^iPhone 16e$",
        r"^iPhone 17 ", r"^iPhone 16 ", r"^iPhone 15", r"^iPhone 14",
        r"^iPhone 13", r"^iPhone 12", r"^iPhone ",
    ]

    def rank(name):
        for index, pattern in enumerate(patterns):
            if re.search(pattern, name):
                return index
        return len(patterns)

    best = min(
        candidates,
        key=lambda c: (rank(c["name"]), 0 if c["booted"] else 1,
                       -c["version"][0], -c["version"][1], c["name"]),
    )

print("%s\t%s\t%s" % (best["name"], best["udid"], best["runtime"]))
PY
  )" || exit 1

  IFS=$'\t' read -r SIM_NAME SIM_UDID SIM_RUNTIME <<< "$resolved"
  [[ -n "$SIM_NAME" && -n "$SIM_UDID" ]] || die "无法解析模拟器信息（simctl 返回了空结果）。"
}

default_app_path() {
  printf '%s/Build/Products/%s-iphonesimulator/%s.app' \
    "$DERIVED_DATA" "$CONFIGURATION" "$APP_NAME"
}

build_if_needed() {
  if [[ "$SKIP_BUILD" == "1" ]]; then
    log "已指定 --skip-build，跳过构建"
    return 0
  fi
  local -a build_args=(--derived-data "$DERIVED_DATA")
  [[ -n "$SIM_NAME" ]] && build_args+=(--sim "$SIM_NAME")
  [[ -n "$SIM_UDID" ]] && build_args+=(--sim-udid "$SIM_UDID")
  log "构建模拟器版 App（调用 build-simulator.sh）"
  bash "$BUILD_SCRIPT" "${build_args[@]}"
}

boot_simulator() {
  log "启动模拟器：$SIM_NAME ($SIM_RUNTIME, UDID=$SIM_UDID)"
  # 已经启动时 boot 会返回非零，这是正常的。
  xcrun simctl boot "$SIM_UDID" >/dev/null 2>&1 || true
  log "等待模拟器完成启动（simctl bootstatus -b）..."
  xcrun simctl bootstatus "$SIM_UDID" -b
  # App 强制浅色外观，这里也把模拟器设成浅色，避免截图系统组件不一致。
  xcrun simctl ui "$SIM_UDID" appearance light >/dev/null 2>&1 || true
}

open_simulator_if_requested() {
  if [[ "$OPEN_SIMULATOR" == "1" ]]; then
    log "打开 Simulator.app（仅用于本机观察）"
    open -a Simulator --args -CurrentDeviceUDID "$SIM_UDID" >/dev/null 2>&1 \
      || log "提示：打开 Simulator.app 失败，继续后台截图"
  fi
}

fix_status_bar() {
  if [[ "$STATUS_BAR" != "1" ]]; then
    return 0
  fi
  log "固定状态栏：9:41、Wi-Fi、满电"
  xcrun simctl status_bar "$SIM_UDID" override \
    --time "9:41" \
    --dataNetwork wifi \
    --wifiMode active --wifiBars 3 \
    --cellularMode active --cellularBars 4 \
    --batteryState charged --batteryLevel 100 \
    >/dev/null 2>&1 \
    || log "提示：当前 Xcode 的 simctl 不支持 status_bar override，已跳过（不影响截图）"
}

install_app() {
  log "安装 App：$APP"
  xcrun simctl install "$SIM_UDID" "$APP"
  xcrun simctl get_app_container "$SIM_UDID" "$BUNDLE_ID" app >/dev/null 2>&1 \
    || die "安装后找不到 ${BUNDLE_ID}，请检查 app 的 Info.plist / --bundle-id 是否正确。"
}

# 截图：单显示器设备直接截图；折叠屏等多显示器设备会报
# “Cannot choose between N displays”，此时自动选择名称里带 (primary) 的显示
# （例如 iPhone Duo 的 “LCD (primary)”）；也可以用 --display / SIM_DISPLAY 显式指定。
take_screenshot() {
  local out="$1" err disp line
  if [[ -n "$SIM_DISPLAY" ]]; then
    xcrun simctl io "$SIM_UDID" screenshot --display "$SIM_DISPLAY" "$out" >/dev/null
    return $?
  fi
  if err="$(xcrun simctl io "$SIM_UDID" screenshot "$out" 2>&1)"; then
    return 0
  fi
  if [[ "$err" == *"Cannot choose between"* ]]; then
    line="$(printf '%s\n' "$err" | grep -- '(primary)' | head -1 || true)"
    if [[ -n "$line" ]]; then
      disp="$(printf '%s\n' "$line" | grep -o -- '--display=[0-9]*' | cut -d= -f2)"
    fi
    if [[ -z "$disp" ]]; then
      disp="$(printf '%s\n' "$err" | grep -o -- '--display=[0-9]*' | cut -d= -f2 | head -1)"
    fi
    if [[ -n "$disp" ]]; then
      log "检测到多显示器设备，自动使用 --display=$disp 截图"
      xcrun simctl io "$SIM_UDID" screenshot --display "$disp" "$out" >/dev/null
      return $?
    fi
  fi
  printf '%s\n' "$err" >&2
  return 1
}

capture_screenshots() {
  local -a shots=()
  local tab
  for tab in ${TABS//,/ }; do
    case "$tab" in
      home|beans|maker|menu|stats) shots+=("$tab") ;;
      *) die "未知的页签：${tab}（可选：home beans maker menu stats）" ;;
    esac
  done
  [[ ${#shots[@]} -gt 0 ]] || die "TABS 为空。"

  mkdir -p "$SHOT_DIR"
  {
    printf 'simulator=%s\n' "$SIM_NAME"
    printf 'udid=%s\n' "$SIM_UDID"
    printf 'runtime=%s\n' "$SIM_RUNTIME"
    printf 'bundle_id=%s\n' "$BUNDLE_ID"
    printf 'app=%s\n' "$APP"
    printf 'date=%s\n' "$(date '+%Y-%m-%d %H:%M:%S %z')"
  } > "$SHOT_DIR/build-info.txt"

  # 冷启动预热：刚安装的 App 首次启动可能先渲染出白屏，如果直接截图会拍到空界面
  #（CI 上偶发）。这里先启动一次并等待，再进入正式截图循环。
  log "预热 App（首次冷启动）"
  xcrun simctl launch --terminate-running-process "$SIM_UDID" "$BUNDLE_ID" \
    -uiTestTab home >/dev/null
  sleep "$((SETTLE_SECONDS + 3))"
  xcrun simctl terminate "$SIM_UDID" "$BUNDLE_ID" >/dev/null 2>&1 || true

  log "开始截图（${#shots[@]} 张）→ $SHOT_DIR"
  for tab in "${shots[@]}"; do
    log "截图：${tab}（-uiTestTab ${tab}）"
    xcrun simctl terminate "$SIM_UDID" "$BUNDLE_ID" >/dev/null 2>&1 || true
    xcrun simctl launch --terminate-running-process "$SIM_UDID" "$BUNDLE_ID" \
      -uiTestTab "$tab" >/dev/null
    sleep "$SETTLE_SECONDS"
    take_screenshot "$SHOT_DIR/$tab.png" || die "截图失败：$SHOT_DIR/$tab.png"
    [[ -s "$SHOT_DIR/$tab.png" ]] || die "截图失败：$SHOT_DIR/$tab.png"
  done

  # 可选：二级页面（底部操作条 / 玻璃条页面）截图，用于覆盖 BottomActionBar。
  local route parent
  for route in ${ROUTES//,/ }; do
    [[ -n "$route" ]] || continue
    case "$route" in
      bean) parent="beans" ;;
      recipe) parent="menu" ;;
      data) parent="menu" ;;
      newbean|new-bean|beaneditor) parent="beans" ;;
      *) die "未知的 route：${route}（可选：bean recipe data newbean）" ;;
    esac
    log "截图：二级页面 ${route}（-uiTestTab ${parent} -uiTestRoute ${route}）"
    xcrun simctl terminate "$SIM_UDID" "$BUNDLE_ID" >/dev/null 2>&1 || true
    xcrun simctl launch --terminate-running-process "$SIM_UDID" "$BUNDLE_ID" \
      -uiTestTab "$parent" -uiTestRoute "$route" >/dev/null
    sleep "$SETTLE_SECONDS"
    take_screenshot "$SHOT_DIR/route-$route.png" || die "截图失败：$SHOT_DIR/route-$route.png"
    [[ -s "$SHOT_DIR/route-$route.png" ]] || die "截图失败：$SHOT_DIR/route-$route.png"
  done

  log "截图完成"
  ls -lh "$SHOT_DIR"
  printf 'SCREENSHOT_DIR=%s\n' "$SHOT_DIR"
}

main() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --app)           APP="${2:?--app 需要一个 .app 路径}"; shift 2 ;;
      --out)           SHOT_DIR="${2:?--out 需要一个目录}"; shift 2 ;;
      --sim)           SIM_NAME="${2:?--sim 需要一个模拟器名称}"; shift 2 ;;
      --sim-udid)      SIM_UDID="${2:?--sim-udid 需要一个 UDID}"; shift 2 ;;
      --derived-data)  DERIVED_DATA="${2:?--derived-data 需要一个目录}"; shift 2 ;;
      --tabs)          TABS="${2:?--tabs 需要一个页签列表}"; shift 2 ;;
      --settle)        SETTLE_SECONDS="${2:?--settle 需要秒数}"; shift 2 ;;
      --bundle-id)     BUNDLE_ID="${2:?--bundle-id 需要一个 id}"; shift 2 ;;
      --skip-build)    SKIP_BUILD="1"; shift ;;
      --open-simulator) OPEN_SIMULATOR="1"; shift ;;
      --no-status-bar) STATUS_BAR="0"; shift ;;
      --display)       SIM_DISPLAY="${2:?--display 需要一个 display 编号}"; shift 2 ;;
      --routes)        ROUTES="${2:?--routes 需要一个列表}"; shift 2 ;;
      -h|--help)       usage; exit 0 ;;
      --)              shift; break ;;
      *)               die "未知参数：$1（用 --help 查看用法）" ;;
    esac
  done

  case "$DERIVED_DATA" in
    /*) ;;
    *) DERIVED_DATA="$REPO_ROOT/$DERIVED_DATA" ;;
  esac

  require_simctl
  build_if_needed

  if [[ -z "$APP" ]]; then
    APP="$(default_app_path)"
  fi
  APP="$(cd "$(dirname "$APP")" 2>/dev/null && pwd)/$(basename "$APP")"
  [[ -d "$APP" ]] || die "找不到模拟器 App：$APP
     请先运行 bash ios/scripts/build-simulator.sh，或去掉 --skip-build。"
  [[ -f "$APP/Info.plist" ]] || die "不是有效的 .app（缺少 Info.plist）：$APP"

  detect_simulator
  boot_simulator
  open_simulator_if_requested
  fix_status_bar
  install_app
  capture_screenshots
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  main "$@"
fi
