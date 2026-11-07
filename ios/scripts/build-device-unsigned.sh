#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# 构建 BeanBook（真机版，Release，完全无签名）
#
# 只负责产出 .app，不写入任何证书/描述文件；打包成 IPA 见 package-ipa.sh。
#
# 用法：
#   bash ios/scripts/build-device-unsigned.sh
#   bash ios/scripts/build-device-unsigned.sh --derived-data build
#   DERIVED_DATA=/tmp/bb-device bash ios/scripts/build-device-unsigned.sh
#
# 环境变量：
#   DERIVED_DATA  DerivedData 目录（默认 ios/build；相对路径按仓库根目录解析）
#   SCHEME        Xcode scheme（默认 BeanBook）
#
# 产物：
#   <DERIVED_DATA>/Build/Products/Release-iphoneos/BeanBook.app
#
# 说明：CODE_SIGNING_ALLOWED=NO / CODE_SIGNING_REQUIRED=NO / CODE_SIGN_IDENTITY=""
#      / DEVELOPMENT_TEAM="" 让 xcodebuild 跳过所有签名步骤，因此不需要任何
#      Apple 证书或账号；产出的 .app 未签名，不能直接安装，需自签（Sideloadly/
#      AltStore/开发者证书）后再装。
#
# 依赖：完整 Xcode（xcodebuild）。只有 Command Line Tools 的机器会得到明确报错。
# ---------------------------------------------------------------------------
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IOS_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$IOS_DIR/.." && pwd)"
PROJECT="$IOS_DIR/BeanBook.xcodeproj"

APP_NAME="BeanBook"
SCHEME="${SCHEME:-BeanBook}"
CONFIGURATION="Release"
SDK="iphoneos"

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
用法: bash ios/scripts/build-device-unsigned.sh [选项] [额外的 xcodebuild 参数...]

选项:
  --derived-data <目录> DerivedData 输出目录（默认 ios/build）
  --scheme <名称>      Xcode scheme（默认 BeanBook）
  -h, --help           显示本帮助

环境变量: DERIVED_DATA, SCHEME
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

plist_value() {
  # plist_value <plist> <key>：取不到值时输出空串（不因 set -e 中断）。
  local plist="$1" key="$2"
  plutil -extract "$key" raw -o - "$plist" 2>/dev/null || true
}

main() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --derived-data) DERIVED_DATA="${2:?--derived-data 需要一个目录}"; shift 2 ;;
      --scheme)       SCHEME="${2:?--scheme 需要一个名称}"; shift 2 ;;
      -h|--help)      usage; exit 0 ;;
      --)             shift; break ;;
      *)              die "未知参数：$1（用 --help 查看用法）" ;;
    esac
  done

  require_xcodebuild

  log "Xcode: $(xcodebuild -version | head -1)"
  log "工程: $PROJECT"
  log "scheme=$SCHEME configuration=$CONFIGURATION sdk=${SDK}（无签名）"
  log "DerivedData: $DERIVED_DATA"
  log "开始构建真机版 BeanBook..."

  xcodebuild \
    -project "$PROJECT" \
    -scheme "$SCHEME" \
    -configuration "$CONFIGURATION" \
    -sdk "$SDK" \
    -derivedDataPath "$DERIVED_DATA" \
    build \
    CODE_SIGNING_ALLOWED=NO \
    CODE_SIGNING_REQUIRED=NO \
    CODE_SIGN_IDENTITY="" \
    DEVELOPMENT_TEAM="" \
    "$@"

  local app_path="$DERIVED_DATA/Build/Products/$CONFIGURATION-$SDK/$APP_NAME.app"
  [[ -d "$app_path" ]] || die "构建完成但找不到产物：$app_path"
  [[ -f "$app_path/Info.plist" ]] || die "产物缺少 Info.plist：$app_path"

  local bundle_id version build_number
  bundle_id="$(plist_value "$app_path/Info.plist" CFBundleIdentifier)"
  version="$(plist_value "$app_path/Info.plist" CFBundleShortVersionString)"
  build_number="$(plist_value "$app_path/Info.plist" CFBundleVersion)"

  log "构建成功（未签名）"
  printf 'APP_PATH=%s\n' "$app_path"
  printf 'BUNDLE_ID=%s\n' "${bundle_id:-未知}"
  printf 'VERSION=%s\n' "${version:-未知}"
  printf 'BUILD=%s\n' "${build_number:-未知}"
  printf '\n下一步：bash ios/scripts/package-ipa.sh --app "%s"\n' "$app_path"
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  main "$@"
fi
