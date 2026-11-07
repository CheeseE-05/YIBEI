#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# 构建 BeanBook（iOS 模拟器版，Debug）
#
# 默认自动挑选一个可用的 iPhone 模拟器：优先 iPhone 17 / iPhone 16 系列。
# 模拟器列表来自 `xcrun simctl list devices available --json`，按机型优先级 +
# iOS 运行时新旧 + 是否已启动排序。
#
# 用法：
#   bash ios/scripts/build-simulator.sh
#   bash ios/scripts/build-simulator.sh --sim "iPhone 16 Pro"
#   SIM_NAME="iPhone 16 Pro" bash ios/scripts/build-simulator.sh
#
# 环境变量：
#   SIM_NAME        模拟器名称；留空自动选择
#   SIM_UDID        模拟器 UDID（优先级高于 SIM_NAME）
#   DERIVED_DATA    DerivedData 目录（默认 ios/build；相对路径按仓库根目录解析）
#   SCHEME          Xcode scheme（默认 BeanBook）
#   CONFIGURATION   构建配置（默认 Debug）
#
# 产物：
#   <DERIVED_DATA>/Build/Products/<CONFIGURATION>-iphonesimulator/BeanBook.app
#
# 依赖：完整 Xcode（xcodebuild + simctl）、python3（macOS 12.3+ 自带）。
# 只有 Command Line Tools 的机器会得到明确报错，不会尝试安装任何东西。
# ---------------------------------------------------------------------------
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IOS_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$IOS_DIR/.." && pwd)"
PROJECT="$IOS_DIR/BeanBook.xcodeproj"

APP_NAME="BeanBook"
SCHEME="${SCHEME:-BeanBook}"
CONFIGURATION="${CONFIGURATION:-Debug}"
SIM_NAME="${SIM_NAME:-}"
SIM_UDID="${SIM_UDID:-}"
SIM_RUNTIME=""

# DERIVED_DATA 统一解析为绝对路径：默认 ios/build，相对路径按仓库根目录解析。
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
用法: bash ios/scripts/build-simulator.sh [选项] [额外的 xcodebuild 参数...]

选项:
  --sim <名称>         指定模拟器名称（如 "iPhone 16 Pro"）
  --sim-udid <UDID>    指定模拟器 UDID（优先级高于 --sim）
  --derived-data <目录> DerivedData 输出目录（默认 ios/build）
  --scheme <名称>      Xcode scheme（默认 BeanBook）
  --configuration <名> 构建配置（默认 Debug）
  -h, --help           显示本帮助

环境变量: SIM_NAME, SIM_UDID, DERIVED_DATA, SCHEME, CONFIGURATION
EOF
}

require_xcodebuild() {
  command -v xcodebuild >/dev/null 2>&1 \
    || die "找不到 xcodebuild：需要安装完整 Xcode（本机若只有 Command Line Tools 无法编译 iOS）。"
  if ! xcodebuild -version >/dev/null 2>&1; then
    printf '错误：xcodebuild 不可用。\n' >&2
    printf '      当前开发者目录：%s\n' "$(xcode-select -p 2>/dev/null || echo 未知)" >&2
    printf '      需要完整 Xcode，例如：sudo xcode-select -s /Applications/Xcode.app/Contents/Developer\n' >&2
    printf '      没有 Mac 版 Xcode 时，请改用 GitHub Actions 的 macos-26 runner（.github/workflows/ios-unsigned.yml）。\n' >&2
    exit 1
  fi
}

# 解析可用 iPhone 模拟器，结果写入全局 SIM_NAME / SIM_UDID / SIM_RUNTIME。
# 在 SIM_NAME / SIM_UDID 已指定时只在该范围内挑选；否则按优先级自动挑选。
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

main() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --sim)           SIM_NAME="${2:?--sim 需要一个模拟器名称}"; shift 2 ;;
      --sim-udid)      SIM_UDID="${2:?--sim-udid 需要一个 UDID}"; shift 2 ;;
      --derived-data)  DERIVED_DATA="${2:?--derived-data 需要一个目录}"; shift 2 ;;
      --scheme)        SCHEME="${2:?--scheme 需要一个名称}"; shift 2 ;;
      --configuration) CONFIGURATION="${2:?--configuration 需要一个名称}"; shift 2 ;;
      -h|--help)       usage; exit 0 ;;
      --)              shift; break ;;
      *)               die "未知参数：$1（用 --help 查看用法）" ;;
    esac
  done

  require_xcodebuild
  detect_simulator

  log "Xcode: $(xcodebuild -version | head -1)"
  log "工程: $PROJECT"
  log "scheme=$SCHEME configuration=$CONFIGURATION"
  log "模拟器: $SIM_NAME ($SIM_RUNTIME, UDID=$SIM_UDID)"
  log "DerivedData: $DERIVED_DATA"
  log "开始构建模拟器版 BeanBook..."

  xcodebuild \
    -project "$PROJECT" \
    -scheme "$SCHEME" \
    -configuration "$CONFIGURATION" \
    -sdk iphonesimulator \
    -destination "id=$SIM_UDID" \
    -derivedDataPath "$DERIVED_DATA" \
    build \
    CODE_SIGNING_ALLOWED=NO \
    CODE_SIGNING_REQUIRED=NO \
    CODE_SIGN_IDENTITY="" \
    DEVELOPMENT_TEAM="" \
    "$@"

  local app_path="$DERIVED_DATA/Build/Products/$CONFIGURATION-iphonesimulator/$APP_NAME.app"
  [[ -d "$app_path" ]] || die "构建完成但找不到产物：$app_path"
  [[ -f "$app_path/Info.plist" ]] || die "产物缺少 Info.plist：$app_path"

  log "构建成功"
  printf 'APP_PATH=%s\n' "$app_path"
}

# 直接执行时运行 main；被 source 时只暴露函数（便于本地单测模拟器选择逻辑）。
if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  main "$@"
fi
