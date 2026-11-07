#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# 一键完整流程（有完整 Xcode 的 Mac 上使用）：
#   1) 模拟器 Debug 构建
#   2) 启动模拟器并逐页截图
#   3) 真机 Release 无签名构建
#   4) 打包未签名 IPA（并计算 SHA256）
#
# 用法：
#   bash ios/scripts/build-all.sh
#   bash ios/scripts/build-all.sh --sim "iPhone 16 Pro"
#   bash ios/scripts/build-all.sh --skip-screenshots      # 只出未签名 IPA
#   bash ios/scripts/build-all.sh --tabs "home stats" --settle 5
#
# 环境变量（透传给各子脚本）：
#   SIM_NAME / SIM_UDID   模拟器选择
#   DERIVED_DATA          DerivedData 目录（默认 ios/build）
#   TABS                  截图页签（默认 home beans maker menu stats）
#   SETTLE_SECONDS        每页启动后等待秒数（默认 3）
#   DIST_DIR              IPA 输出目录（默认仓库根 dist/）
#   SKIP_SCREENSHOTS      设为 1 等价于 --skip-screenshots
#
# 依赖：完整 Xcode（xcodebuild + simctl）、python3。
# ---------------------------------------------------------------------------
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IOS_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$IOS_DIR/.." && pwd)"

SIM_NAME="${SIM_NAME:-}"
SIM_UDID="${SIM_UDID:-}"
TABS="${TABS:-home beans maker menu stats}"
SETTLE_SECONDS="${SETTLE_SECONDS:-3}"
SKIP_SCREENSHOTS="${SKIP_SCREENSHOTS:-0}"
SHOT_DIR="${SHOT_DIR:-$IOS_DIR/artifacts/screenshots}"

if [[ -n "${DERIVED_DATA:-}" ]]; then
  case "$DERIVED_DATA" in
    /*) ;;
    *) DERIVED_DATA="$REPO_ROOT/$DERIVED_DATA" ;;
  esac
else
  DERIVED_DATA="$IOS_DIR/build"
fi

log() { printf '\n==> %s\n' "$*"; }
die() { printf '错误：%s\n' "$*" >&2; exit 1; }

usage() {
  cat <<'EOF'
用法: bash ios/scripts/build-all.sh [选项]

选项:
  --sim <名称>         指定模拟器名称（如 "iPhone 16 Pro"）
  --sim-udid <UDID>    指定模拟器 UDID（优先级最高）
  --derived-data <目录> DerivedData 输出目录（默认 ios/build）
  --tabs "<列表>"      截图页签，空格/逗号分隔（默认 home beans maker menu stats）
  --settle <秒>        每次启动后等待秒数（默认 3）
  --out <目录>         IPA 输出目录（默认 dist/）
  --skip-screenshots   跳过「模拟器构建 + 截图」，只构建并打包未签名 IPA
  -h, --help           显示本帮助

环境变量: SIM_NAME, SIM_UDID, DERIVED_DATA, TABS, SETTLE_SECONDS, DIST_DIR,
          SHOT_DIR, SKIP_SCREENSHOTS
EOF
}

require_xcodebuild() {
  command -v xcodebuild >/dev/null 2>&1 \
    || die "找不到 xcodebuild：需要安装完整 Xcode（本机若只有 Command Line Tools 无法编译 iOS）。"
  if ! xcodebuild -version >/dev/null 2>&1; then
    die "xcodebuild 不可用（需要完整 Xcode，而不是仅 Command Line Tools）。
     没有 Mac 版 Xcode 时，请用 GitHub Actions：.github/workflows/ios-unsigned.yml"
  fi
}

main() {
  local out_dir="${DIST_DIR:-}"
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --sim)              SIM_NAME="${2:?--sim 需要一个模拟器名称}"; shift 2 ;;
      --sim-udid)         SIM_UDID="${2:?--sim-udid 需要一个 UDID}"; shift 2 ;;
      --derived-data)     DERIVED_DATA="${2:?--derived-data 需要一个目录}"; shift 2 ;;
      --tabs)             TABS="${2:?--tabs 需要一个页签列表}"; shift 2 ;;
      --settle)           SETTLE_SECONDS="${2:?--settle 需要秒数}"; shift 2 ;;
      --out)              out_dir="${2:?--out 需要一个目录}"; shift 2 ;;
      --skip-screenshots) SKIP_SCREENSHOTS="1"; shift ;;
      -h|--help)          usage; exit 0 ;;
      --)                 shift; break ;;
      *)                  die "未知参数：$1（用 --help 查看用法）" ;;
    esac
  done
  # 若通过 --out / --derived-data 覆盖，按仓库根目录解析相对路径。
  case "$DERIVED_DATA" in
    /*) ;;
    *) DERIVED_DATA="$REPO_ROOT/$DERIVED_DATA" ;;
  esac
  if [[ -n "$out_dir" ]]; then
    case "$out_dir" in
      /*) DIST_DIR="$out_dir" ;;
      *) DIST_DIR="$REPO_ROOT/$out_dir" ;;
    esac
  fi
  # 子脚本（截图脚本）默认按环境变量解析 DerivedData，这里统一导出，避免路径不一致。
  export DERIVED_DATA

  require_xcodebuild
  log "Xcode: $(xcodebuild -version | head -1)"
  log "DerivedData: $DERIVED_DATA"

  local -a build_args=(--derived-data "$DERIVED_DATA")
  local -a sim_args=()
  [[ -n "$SIM_NAME" ]] && sim_args+=(--sim "$SIM_NAME")
  [[ -n "$SIM_UDID" ]] && sim_args+=(--sim-udid "$SIM_UDID")

  if [[ "$SKIP_SCREENSHOTS" != "1" ]]; then
    log "[1/4] 构建模拟器版 App"
    bash "$SCRIPT_DIR/build-simulator.sh" "${build_args[@]}" ${sim_args[@]+"${sim_args[@]}"}

    log "[2/4] 启动模拟器并截图 → $SHOT_DIR"
    local -a shot_args=(--skip-build --out "$SHOT_DIR" --tabs "$TABS" --settle "$SETTLE_SECONDS")
    [[ -n "$SIM_NAME" ]] && shot_args+=(--sim "$SIM_NAME")
    [[ -n "$SIM_UDID" ]] && shot_args+=(--sim-udid "$SIM_UDID")
    bash "$SCRIPT_DIR/run-simulator-screenshots.sh" ${shot_args[@]+"${shot_args[@]}"}
  else
    log "[1/4] 跳过模拟器构建"
    log "[2/4] 跳过模拟器截图"
  fi

  log "[3/4] 构建真机版 App（Release，无签名）"
  bash "$SCRIPT_DIR/build-device-unsigned.sh" "${build_args[@]}"

  log "[4/4] 打包未签名 IPA"
  local -a package_args=(--app "$DERIVED_DATA/Build/Products/Release-iphoneos/BeanBook.app")
  [[ -n "$DIST_DIR" ]] && package_args+=(--out "$DIST_DIR")
  bash "$SCRIPT_DIR/package-ipa.sh" ${package_args[@]+"${package_args[@]}"}

  local ipa_path=""
  local search_dir="${DIST_DIR:-$REPO_ROOT/dist}"
  ipa_path="$(find "$search_dir" -maxdepth 1 -name 'BeanBook-*-unsigned.ipa' -print -quit 2>/dev/null || true)"

  log "全部完成"
  printf 'DERIVED_DATA=%s\n' "$DERIVED_DATA"
  if [[ "$SKIP_SCREENSHOTS" != "1" ]]; then
    printf 'SIM_APP=%s\n' "$DERIVED_DATA/Build/Products/Debug-iphonesimulator/BeanBook.app"
    printf 'SCREENSHOT_DIR=%s\n' "$SHOT_DIR"
  fi
  printf 'DEVICE_APP=%s\n' "$DERIVED_DATA/Build/Products/Release-iphoneos/BeanBook.app"
  printf 'IPA_PATH=%s\n' "${ipa_path:-未找到（请检查 package-ipa.sh 输出）}"
  if [[ -f "${ipa_path}.sha256" ]]; then
    printf 'SHA256=%s\n' "$(awk '{print $1}' "${ipa_path}.sha256")"
  fi
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  main "$@"
fi
