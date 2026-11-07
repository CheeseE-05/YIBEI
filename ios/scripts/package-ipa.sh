#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# 把已构建好的 .app 打包成未签名 IPA，并输出 SHA256
#
# 产物：dist/BeanBook-<version>-unsigned.ipa
#       dist/BeanBook-<version>-unsigned.ipa.sha256
#
# 用法：
#   bash ios/scripts/package-ipa.sh
#   bash ios/scripts/package-ipa.sh --app /path/to/BeanBook.app
#   bash ios/scripts/package-ipa.sh --out /tmp/ipa-out
#
# 可选重签名（大多数用户不需要；先把未签名 IPA 装到手机时由 Sideloadly/
# AltStore 完成签名即可）：
#   bash ios/scripts/package-ipa.sh \
#     --sign "Apple Development: Your Name (XXXXXXXXXX)" \
#     --profile ~/Downloads/BeanBook.mobileprovision
#   重签名需要本机钥匙串里有对应证书和私钥，产物为 ...-signed.ipa。
#
# 环境变量：
#   DERIVED_DATA  DerivedData 目录（默认 ios/build；相对路径按仓库根目录解析）
#   DIST_DIR      输出目录（默认 dist/，相对路径按仓库根目录解析）
#   SCHEME        仅用于报错提示（默认 BeanBook）
#
# 依赖：macOS 自带命令（plutil / zip / shasum；重签名时还需要 codesign、security）。
# ---------------------------------------------------------------------------
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IOS_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$IOS_DIR/.." && pwd)"

APP_NAME="${APP_NAME:-BeanBook}"
SCHEME="${SCHEME:-BeanBook}"
APP=""
VERSION=""
SIGN_IDENTITY=""
PROVISION_PROFILE=""

normalize_dir() {
  # normalize_dir <默认值> <用户值>：空值用默认；相对路径按仓库根目录解析。
  local default_path="$1" user_value="${2:-}"
  local path="${user_value:-$default_path}"
  case "$path" in
    /*) printf '%s' "$path" ;;
    *) printf '%s/%s' "$REPO_ROOT" "$path" ;;
  esac
}

DERIVED_DATA="$(normalize_dir "$IOS_DIR/build" "${DERIVED_DATA:-}")"
OUT_DIR="$(normalize_dir "$REPO_ROOT/dist" "${DIST_DIR:-}")"
STAGE_DIR=""

log() { printf '==> %s\n' "$*"; }
die() { printf '错误：%s\n' "$*" >&2; exit 1; }

cleanup() {
  # EXIT trap 在 main 返回后触发，因此这里用全局 STAGE_DIR（不能用函数内 local）。
  if [[ -n "${STAGE_DIR:-}" && -d "$STAGE_DIR" ]]; then
    rm -rf "$STAGE_DIR"
  fi
}

usage() {
  cat <<'EOF'
用法: bash ios/scripts/package-ipa.sh [选项]

选项:
  --app <路径>       要打包的 .app（默认自动查找真机构建产物）
  --out <目录>       输出目录（默认 dist/）
  --version <版本>   覆盖 IPA 文件名里的版本号（默认读 Info.plist）
  --sign <标识>      可选：用该 codesign 标识重签名（需同时给 --profile）
  --profile <路径>   可选：对应的 .mobileprovision 描述文件
  -h, --help         显示本帮助

环境变量: DERIVED_DATA, DIST_DIR
EOF
}

plist_value() {
  # plist_value <plist> <key>：取不到值时输出空串（不因 set -e 中断）。
  local plist="$1" key="$2"
  plutil -extract "$key" raw -o - "$plist" 2>/dev/null || true
}

resolve_app() {
  if [[ -n "$APP" ]]; then
    return 0
  fi
  local candidates=(
    "$DERIVED_DATA/Build/Products/Release-iphoneos/$APP_NAME.app"
    "$DERIVED_DATA/Release-iphoneos/$APP_NAME.app"
  )
  local candidate
  for candidate in "${candidates[@]}"; do
    if [[ -d "$candidate" ]]; then
      APP="$candidate"
      return 0
    fi
  done
  die "找不到真机构建产物。请先运行：bash ios/scripts/build-device-unsigned.sh
     （已查找：${candidates[0]}）
     或用 --app 指定 .app 路径。"
}

validate_app() {
  [[ -d "$APP" ]] || die "不是一个目录：$APP"
  [[ -f "$APP/Info.plist" ]] || die "缺少 Info.plist，不是有效的 iOS App：$APP"
  plutil -lint "$APP/Info.plist" >/dev/null 2>&1 || die "Info.plist 格式无效：$APP/Info.plist"

  BUNDLE_ID="$(plist_value "$APP/Info.plist" CFBundleIdentifier)"
  [[ -n "$BUNDLE_ID" ]] || die "Info.plist 里没有 CFBundleIdentifier：$APP/Info.plist"

  local executable
  executable="$(plist_value "$APP/Info.plist" CFBundleExecutable)"
  [[ -n "$executable" ]] || executable="$APP_NAME"
  [[ -f "$APP/$executable" ]] \
    || die "找不到可执行文件：$APP/${executable}（这不是一个完整的 .app）"
}

resign_app() {
  # resign_app <app 路径> <临时目录>
  local app="$1" tmp="$2"

  [[ -n "$SIGN_IDENTITY" ]] || return 0
  [[ -n "$PROVISION_PROFILE" ]] || die "使用 --sign 时必须同时提供 --profile <path>"
  [[ -f "$PROVISION_PROFILE" ]] || die "描述文件不存在：$PROVISION_PROFILE"
  command -v codesign >/dev/null 2>&1 || die "找不到 codesign（需要 macOS + Xcode 命令行工具）"
  command -v security >/dev/null 2>&1 || die "找不到 security（需要 macOS）"

  # 注意：同样避免 `security ... | grep -q` 的 SIGPIPE 风险（pipefail 下可能误判），
  # 直接把输出收进变量再用 [[ ... == *...* ]] 匹配。
  if [[ "$(security find-identity -v -p codesigning 2>/dev/null || true)" != *"$SIGN_IDENTITY"* ]]; then
    printf '警告：钥匙串里没找到签名标识 "%s"。\n' "$SIGN_IDENTITY" >&2
    printf '      可运行 security find-identity -v -p codesigning 查看可用标识。\n' >&2
  fi

  log "解析描述文件：$PROVISION_PROFILE"
  security cms -D -i "$PROVISION_PROFILE" > "$tmp/profile.plist" 2>/dev/null \
    || die "无法解析描述文件：$PROVISION_PROFILE"
  plutil -extract Entitlements xml1 -o "$tmp/entitlements.plist" "$tmp/profile.plist" >/dev/null 2>&1 \
    || die "描述文件里没有 Entitlements；请确认它是 App 的 .mobileprovision"
  cp "$PROVISION_PROFILE" "$app/embedded.mobileprovision"

  log "重签名 App（先框架后主程序）：$SIGN_IDENTITY"
  if [[ -d "$app/Frameworks" ]]; then
    local item
    while IFS= read -r -d '' item; do
      codesign --force --sign "$SIGN_IDENTITY" --timestamp=none "$item" >/dev/null \
        || die "签名失败：$item"
    done < <(
      find "$app/Frameworks" -maxdepth 1 -type d -name '*.framework' -print0
      find "$app/Frameworks" -maxdepth 1 -type f -name '*.dylib' -print0
    )
  fi
  codesign --force --sign "$SIGN_IDENTITY" \
    --entitlements "$tmp/entitlements.plist" \
    --timestamp=none "$app" >/dev/null \
    || die "主程序签名失败（检查证书与描述文件是否匹配）"
  codesign --verify --verbose=2 "$app" \
    || die "重签名后校验失败：$app"
  log "重签名完成"
}

main() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --app)     APP="${2:?--app 需要一个 .app 路径}"; shift 2 ;;
      --out)     OUT_DIR="${2:?--out 需要一个目录}"; shift 2 ;;
      --version) VERSION="${2:?--version 需要一个版本号}"; shift 2 ;;
      --sign)    SIGN_IDENTITY="${2:?--sign 需要一个 codesign 标识}"; shift 2 ;;
      --profile) PROVISION_PROFILE="${2:?--profile 需要描述文件路径}"; shift 2 ;;
      -h|--help) usage; exit 0 ;;
      --)        shift; break ;;
      *)         die "未知参数：$1（用 --help 查看用法）" ;;
    esac
  done

  resolve_app
  APP="$(cd "$(dirname "$APP")" && pwd)/$(basename "$APP")"
  validate_app

  if [[ -z "$VERSION" ]]; then
    VERSION="$(plist_value "$APP/Info.plist" CFBundleShortVersionString)"
  fi
  [[ -n "$VERSION" ]] || VERSION="1.0"
  VERSION="$(printf '%s' "$VERSION" | sed 's/[^A-Za-z0-9._-]/-/g')"

  local suffix="unsigned"
  [[ -n "$SIGN_IDENTITY" ]] && suffix="signed"

  local ipa_name="$APP_NAME-$VERSION-$suffix.ipa"
  local ipa_path="$OUT_DIR/$ipa_name"

  STAGE_DIR="$(mktemp -d "${TMPDIR:-/tmp}/beanbook-ipa.XXXXXX")"
  trap cleanup EXIT

  log "源 App：$APP"
  log "Bundle ID：$BUNDLE_ID  版本：$VERSION"
  log "准备 Payload/..."
  mkdir -p "$STAGE_DIR/Payload"
  if command -v ditto >/dev/null 2>&1; then
    ditto "$APP" "$STAGE_DIR/Payload/$APP_NAME.app"
  else
    cp -R "$APP" "$STAGE_DIR/Payload/$APP_NAME.app"
  fi

  resign_app "$STAGE_DIR/Payload/$APP_NAME.app" "$STAGE_DIR"

  command -v zip >/dev/null 2>&1 || die "找不到 zip 命令"
  mkdir -p "$OUT_DIR"
  log "压缩 IPA：$ipa_path"
  rm -f "$ipa_path"
  ( cd "$STAGE_DIR" && zip -qry "$ipa_path" Payload -x '*.DS_Store' ) \
    || die "zip 打包失败"

  [[ -s "$ipa_path" ]] || die "IPA 文件为空：$ipa_path"
  # 注意：不要用 `unzip -l | grep -q`。脚本开启了 pipefail，grep -q 匹配后立刻退出会让
  # unzip 收到 SIGPIPE（退出码 141），整个管道被判失败，从而误报「IPA 结构异常」。
  # 先把清单落到临时文件再 grep，避免 SIGPIPE 竞态。
  local ipa_list
  ipa_list="$(mktemp "${TMPDIR:-/tmp}/beanbook-ipa-list.XXXXXX")"
  if ! unzip -Z1 "$ipa_path" > "$ipa_list" 2>/dev/null \
     || ! grep -qx "Payload/$APP_NAME.app/Info.plist" "$ipa_list"; then
    rm -f "$ipa_list"
    die "IPA 结构异常（找不到 Payload/$APP_NAME.app/Info.plist）"
  fi
  rm -f "$ipa_list"

  local sha256
  sha256="$(shasum -a 256 "$ipa_path" | awk '{print $1}')"
  printf '%s  %s\n' "$sha256" "$ipa_name" > "$ipa_path.sha256"

  log "打包完成"
  printf 'IPA_PATH=%s\n' "$ipa_path"
  printf 'IPA_SIZE=%s\n' "$(du -h "$ipa_path" | awk '{print $1}')"
  printf 'SHA256=%s\n' "$sha256"
  printf 'SHA256_FILE=%s\n' "$ipa_path.sha256"
  [[ -n "$SIGN_IDENTITY" ]] \
    && printf '\n已重签名（%s）。安装前请确认描述文件包含该设备 UDID。\n' "$SIGN_IDENTITY"

  if [[ -z "$SIGN_IDENTITY" ]]; then
    cat <<EOF

未签名 IPA 不能直接安装。常见做法：
  1. 在 Windows/macOS 用 Sideloadly 或 AltStore 导入本 IPA，用自己的 Apple ID 自签；
  2. 免费 Apple ID 签名 7 天有效，到期重签即可。
EOF
  fi
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  main "$@"
fi
