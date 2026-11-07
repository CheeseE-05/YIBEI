# 忆杯手记 iOS 版

> **重要说明（请先读）**
>
> 本机（macOS）已安装 **Xcode 27.2（Xcode 27B52）**，并且**已经真实编译通过**：
> 模拟器 Debug 构建成功、真机 Release 无签名构建成功、未签名 IPA 已产出
> （`dist/BeanBook-1.2-unsigned.ipa`，arm64 / iOS / minos 17.0，未签名，SHA256 `d03008e8…`）。
> 已在 **iOS 27.2 模拟器实际运行**并用 `ios/scripts/run-simulator-screenshots.sh` 逐页截图 9 张
> （`screenshots/ios/`：五个主页面 + 豆子详情 / 配方详情 / 数据与分享 / 添加豆子）。
>
> **1.2 新增**：菜单里已保存的配方可以直接编辑——配方详情右上角铅笔进入编辑态，
> 顶部显示「正在修改配方 · 名称」+「取消编辑」，保存时更新原配方（不新增、不重复扣豆）。
> 验证截图见 `ios/artifacts/verify-1.2/`（含编辑入口、编辑态、保存弹窗、取消/保存后）。
>
> 同一版本还把底部系统 Tab Bar 的图标从约 25–28pt 缩小到约 17–20pt（仍用系统原生 TabView，只通过
> `UIImage.SymbolConfiguration(pointSize: 19)` 传入更小的模板图标，文字/布局/点击区域不变）；前后对比见
> `ios/artifacts/verify-1.2-icons/compare-tabbar-before-after.png`。
>
> 安装到真机需要 **Apple ID 签名**（打包 IPA 本身不需要账号）：可用 Sideloadly / AltStore
> 自签（免费 Apple ID 7 天），或在 Xcode 里登录 Apple ID 后直接 Run。你可以：
>
> 1. 在 Mac 上 `open ios/BeanBook.xcodeproj` 一键运行；或
> 2. 运行 `ios/scripts/*.sh` 一条命令完成模拟器构建、自动截图、真机未签名 IPA 打包；或
> 3. 把仓库推到 GitHub，用 Actions 的 `macos-26` runner（自带 Xcode 26+）产出无签名 IPA，
>    下载后用 Sideloadly / AltStore 自签安装到 iPhone / iPad。
>
> **推荐使用 Xcode 26 或更新版本构建**：只有 Xcode 26+（iOS 26 SDK）才会编入 Liquid Glass
> 液态玻璃底栏。**Xcode 16 或更早版本仍可正常编译、运行、截图、打包**，但底栏会自动回退为
> `.ultraThinMaterial` 毛玻璃，没有液态玻璃效果。
>
> iOS 必须由 macOS + Xcode 编译，并且需要 Apple ID/证书签名，这是平台限制，不是少一个文件。

## 已实现的功能

- 五个主页面：首页、豆仓、调一杯、菜单、统计。
- 根视图使用**系统原生 `TabView`**：iOS 26/27 自动获得 Apple 标准 Liquid Glass Tab Bar（胶囊玻璃底栏 + 系统原生大号透明玻璃选中滑块，点击切换时由系统播放滑动/弹出动画，与 Apple Music / News 一致）；iOS 17–25 为系统经典 Tab Bar；iPad 使用系统自适应侧栏。详情页隐藏 Tab Bar 并显示悬浮圆角玻璃操作条。内容使用自适应网格和宽度。
- **系统原生 Liquid Glass 导航**：底部/侧边导航使用系统 `TabView`，iOS 26/27 自动获得
  Apple 标准 Liquid Glass Tab Bar 与大号透明玻璃选中滑块（系统原生动画）；详情页底部为
  自研悬浮圆角玻璃操作条（iOS 17–25 回退 `.ultraThinMaterial`）。细节见下一节。
- 首页：最近在用的豆子、最近调过的咖啡、豆仓余量、杯数与平均评分。
- 豆仓：添加/编辑/删除豆子，烘焙度、风味、克数、烘焙日期、保存方式、赏味期、筛选。
- **iOS 原生离线抠图**：使用 iOS 17 的 Vision `VNGenerateForegroundInstanceMaskRequest`，不依赖云端、不需要安装第三方模型。
- 调一杯：按住原料卡片倒入、撤销、倒掉重来、微调层克数、删除层、搅拌均匀/恢复分层、用豆扣除、存进菜单。
- 菜单：配方目录、评分筛选、数据管理、离线配方导入。
- 数据管理：
  - 整库导出 / 合并导入 / 覆盖恢复。
  - 单杯配方文件导入。
  - 单杯配方文件分享（AirDrop、微信、文件 App 等）。
- 离线分享/备份文件与 Android 版字段和 `kind` 值兼容：
  - 数据库：`.json`，`kind = "beanbook-database"`。
  - 单杯配方：`.brewkinrecipe.json`，`kind = "beanbook-recipe"`。
  - 因此 Android 导出的配方可以在 iPhone/iPad 导入，反之亦然（照片以 base64 一起传输）。
- 统计：时间范围、总杯数、平均评分、用豆量、连续天数、7 天节奏、风味指纹、豆子排行、烘焙光谱、常驻材料、豆仓余量。

## Liquid Glass 底栏

底栏（以及 iPad 侧栏、底部操作条）统一封装在 `Theme.swift` 的
`liquidGlassSurface(...)` 修饰器里，按「编译 SDK + 系统版本」双条件选择质感：

| 构建/运行条件 | 表现 |
| --- | --- |
| iOS 26/27 系统（任意 Xcode 构建） | 系统原生 `TabView` → 系统 Liquid Glass Tab Bar（胶囊玻璃 + 点击时滑动/弹出的大号透明玻璃选中滑块，动画与材质由系统提供，与 Apple Music/News 一致） |
| 设备/模拟器 **iOS 17–25** | 系统经典 Tab Bar；详情页底部操作条回退 `.ultraThinMaterial`，功能和布局不变 |
| **Xcode 16 及更早**（没有 iOS 26 SDK） | 导航仍为系统 TabView；详情页玻璃操作条经 `#if compiler(>=6.2)` 剔除、编译毛玻璃回退路径；工程仍可正常构建（部署目标保持 iOS 17.0） |

要点：

- 部署目标仍是 **iOS 17.0**，没有为了玻璃效果抬高系统要求。
- **推荐用 Xcode 26+ 构建**：只有 Xcode 26+ 才有 iOS 26 SDK，Liquid Glass 才会生效；
  Xcode 16/更早版本可以正常编译、运行、截图、打包，但没有液态玻璃效果（自动回退毛玻璃）。
- 旧系统上视觉与安卓版一致：奶油灰底、白色圆角卡、青柠强调色 + 系统毛玻璃底栏。
- Liquid Glass 的观感需要真机调校；如果后续要调参数，只改 `Theme.swift` 里的
  `liquidGlassSurface(...)` 调用即可，不影响页面内容。
- 旧版 Xcode 构建出来的包在 iOS 26 设备上会显示毛玻璃回退样式（玻璃代码未编入），
  想要玻璃效果请用 Xcode 26+ 构建。

## 目录与脚本一览

```
ios/
├── BeanBook.xcodeproj/           # Xcode 工程（target/scheme 均为 BeanBook）
├── BeanBook/                     # SwiftUI 源码 + Info.plist（bundle id: com.brewkin.beanbook.ios）
├── scripts/
│   ├── build-all.sh              # 一键：模拟器构建 + 截图 + 真机构建 + 打包 IPA
│   ├── build-simulator.sh        # 模拟器 Debug 构建，自动挑 iPhone 17/16 模拟器
│   ├── build-device-unsigned.sh  # 真机 Release 构建，完全跳过签名
│   ├── package-ipa.sh            # .app → dist/BeanBook-<version>-unsigned.ipa + SHA256
│   └── run-simulator-screenshots.sh  # 模拟器启动、安装、逐页截图
└── README-IOS.md                 # 本文件
.github/workflows/ios-unsigned.yml  # 云端 macOS 构建 + 截图 + IPA 流水线
```

脚本产出的构建物（均不会提交到仓库，建议加入 `.gitignore`）：

| 路径 | 内容 |
| --- | --- |
| `ios/build/Build/Products/Debug-iphonesimulator/BeanBook.app` | 模拟器 Debug 包 |
| `ios/build/Build/Products/Release-iphoneos/BeanBook.app` | 真机 Release 未签名包 |
| `ios/artifacts/screenshots/{home,beans,maker,menu,stats}.png` | 自动截图（另含 `build-info.txt`） |
| `dist/BeanBook-1.2-unsigned.ipa` + `.sha256` | 未签名 IPA 与校验和 |

## 一键命令

### 方式一：有完整 Xcode 的 Mac（一条命令）

```bash
cd beans-app
bash ios/scripts/build-all.sh
```

依次完成：模拟器 Debug 构建 → 启动模拟器逐页截图 → 真机 Release 无签名构建 →
打包未签名 IPA；最后打印关键产物路径（`SIM_APP` / `SCREENSHOT_DIR` / `DEVICE_APP` /
`IPA_PATH` / `SHA256`）。只想要 IPA、不截图：

```bash
bash ios/scripts/build-all.sh --skip-screenshots
```

### 方式二：GitHub Actions（没有 Mac）

把仓库推到 GitHub → **Actions → Build unsigned iOS IPA & screenshots → Run workflow**
→ 结束后在运行页面下载 `BeanBook-unsigned-ipa`（未签名 IPA + SHA256）和
`BeanBook-simulator-screenshots`（五个页面截图）两个 artifact。详细说明见下文
「GitHub Actions 云端构建」。

## 在 Mac 上构建（需要完整 Xcode）

> **推荐 Xcode 26+**（Liquid Glass 生效）；Xcode 16 或更早版本可以编译运行，
> 但底栏自动回退毛玻璃、没有液态玻璃效果。下面的分步脚本与 `build-all.sh` 用的是同一套逻辑。

前置：安装 **Xcode 26（推荐）或 Xcode 16+**，并执行一次
`sudo xcode-select -s /Applications/Xcode.app/Contents/Developer`、同意许可协议
`sudo xcodebuild -license accept`。所有脚本都以仓库根目录（`beans-app/`）为基准调用，可重复执行。

### 1. 模拟器构建

```bash
cd beans-app
bash ios/scripts/build-simulator.sh
```

- 默认自动选择可用 iPhone 模拟器：**优先 iPhone 17 / 16 系列**，同机型优先已启动、再优先新 iOS 运行时；
- 指定机型：`--sim "iPhone 16 Pro"` 或 `SIM_NAME="iPhone 16 Pro"`；
- 指定 UDID：`--sim-udid <UDID>` 或 `SIM_UDID=<UDID>`（优先级最高）；
- 覆盖 DerivedData：`--derived-data /tmp/bb-sim` 或 `DERIVED_DATA=/tmp/bb-sim`；
- 脚本内部调用 `xcrun simctl list devices available --json` 解析机型，需要 `python3`（macOS 12.3+ 自带）；
- 产物路径会以 `APP_PATH=...` 打印出来。

### 2. 自动截图（模拟器）

```bash
cd beans-app
bash ios/scripts/run-simulator-screenshots.sh
```

流程：构建（默认会先调用 `build-simulator.sh`，可 `--skip-build` 跳过）→ 启动模拟器
（`simctl boot` + `simctl bootstatus -b` 等待启动完成）→ 安装 App → 逐页启动并截图：

```bash
xcrun simctl launch <udid> com.brewkin.beanbook.ios -uiTestTab home
xcrun simctl io <udid> screenshot ios/artifacts/screenshots/home.png
```

- 页签顺序默认 `home beans maker menu stats`，可 `TABS="home stats"` 或 `--tabs "home stats"` 覆盖；
- 每页启动后默认等 3 秒再截图（`--settle 5` / `SETTLE_SECONDS=5` 可调）；
- 默认把状态栏固定成 9:41 / 满电（`--no-status-bar` 关闭），让截图更稳定；
- `-uiTestTab` 是 **Debug 构建专用**启动参数（`RootView.swift` 里 `#if DEBUG` 读取，也支持
  `UI_TEST_TAB` 环境变量）；Release 包会忽略它，仍从首页启动；
- 想连详情页一起截图：`ROUTES="bean recipe data newbean" bash ios/scripts/run-simulator-screenshots.sh` 或 `--routes "bean recipe data newbean"`，输出 `route-bean.png` / `route-recipe.png` / `route-data.png` / `route-newbean.png`（App 内置 Debug 启动参数 `-uiTestRoute` 支持）；
- 折叠屏等多显示器模拟器（如 iPhone Duo）截图会报 “Cannot choose between N displays”，脚本会自动选择名称带 `(primary)` 的显示；也可 `--display 1` 或 `SIM_DISPLAY=1` 显式指定；
- 想边跑边看界面：加 `--open-simulator`；CI/无界面环境不需要；
- 构建产物不在默认 `ios/build` 时：加 `--derived-data <目录>` 或设置 `DERIVED_DATA`。

本机实测（Xcode 27.2 / iPhone 17 / iOS 27.2 Simulator）：

```bash
bash ios/scripts/run-simulator-screenshots.sh --sim "iPhone 17" --skip-build
# → ios/artifacts/screenshots/{home,beans,maker,menu,stats}.png + build-info.txt
ROUTES="bean recipe data newbean" bash ios/scripts/run-simulator-screenshots.sh --sim "iPhone 17" --skip-build
# → 追加 route-bean.png / route-recipe.png / route-data.png / route-newbean.png（共 9 张）
# 同一份截图已复制到仓库 screenshots/ios/ 方便查看。

# 导航切换动画验证（Debug：-uiTestAutoCycle 自动轮播 + 录屏，再用 AVFoundation 抽帧）
# 注意：底部导航已改为系统原生 TabView，选中滑块的滑动/弹出动画由 iOS 系统提供，
# 不需要也不应该由 App 自绘（在常规 iPhone 上即为系统 Liquid Glass Tab Bar 的滑块动画）。
xcrun simctl io <udid> recordVideo --display=1 --codec h264 --force /tmp/nav.mov &
xcrun simctl launch <udid> com.brewkin.beanbook.ios -uiTestTab home -uiTestAutoCycle 1.8
```

### 3. 真机构建 + 未签名 IPA

```bash
cd beans-app
bash ios/scripts/build-device-unsigned.sh
bash ios/scripts/package-ipa.sh
```

`build-device-unsigned.sh` 以 `Release` + `iphoneos` + 以下设置构建，不需要任何证书/账号：

```
CODE_SIGNING_ALLOWED=NO
CODE_SIGNING_REQUIRED=NO
CODE_SIGN_IDENTITY=""
DEVELOPMENT_TEAM=""
```

`package-ipa.sh` 会把 `.app` 放进 `Payload/` 后 zip 成 `dist/BeanBook-<version>-unsigned.ipa`
（版本号读自产物 `Info.plist` 的 `CFBundleShortVersionString`），并输出 SHA256 校验文件：

```
IPA_PATH=.../dist/BeanBook-1.2-unsigned.ipa
SHA256=...
SHA256_FILE=.../dist/BeanBook-1.2-unsigned.ipa.sha256
```

常用参数：`--app /path/to/BeanBook.app`、`--out dist`、`--version 1.0`。

### 4.（可选）用本地证书重签名 IPA

大多数用户不需要这一步——把未签名 IPA 交给 Sideloadly/AltStore 自动签名即可。
只有在钥匙串里已有证书和私钥、并且有对应 `.mobileprovision` 时才用：

```bash
bash ios/scripts/package-ipa.sh \
  --sign "Apple Development: Your Name (XXXXXXXXXX)" \
  --profile ~/Downloads/BeanBook.mobileprovision
```

脚本会从描述文件里取 `Entitlements`、写入 `embedded.mobileprovision`、先签内嵌框架再签主程序，
产物为 `dist/BeanBook-<version>-signed.ipa`。证书标识可用
`security find-identity -v -p codesigning` 查看；描述文件必须包含目标设备的 UDID。

### 5. 脚本通用说明

- 所有脚本都是 `set -euo pipefail`，失败即停并给出中文原因；
- 都会先检查 `xcodebuild` / `xcrun simctl` 是否真的可用：只有 Command Line Tools 的机器会得到
  明确报错提示（不会尝试联网安装 Xcode）；
- 相对路径的 `DERIVED_DATA` 按仓库根目录解析；`DIST_DIR` 同理（默认仓库根 `dist/`）；
- `bash ios/scripts/<脚本>.sh --help` 可查看全部参数与环境变量。

| 环境变量 | 作用 | 默认值 |
| --- | --- | --- |
| `SIM_NAME` | 模拟器名称 | 自动选择（iPhone 17/16 优先） |
| `SIM_UDID` | 模拟器 UDID | 空 |
| `DERIVED_DATA` | DerivedData 目录 | `ios/build` |
| `SCHEME` | Xcode scheme | `BeanBook` |
| `TABS` | 截图页签列表 | `home beans maker menu stats` |
| `SETTLE_SECONDS` | 每页启动后等待秒数 | `3` |
| `BUNDLE_ID` | App bundle id | `com.brewkin.beanbook.ios` |
| `SKIP_BUILD` | 截图前跳过构建 | `0` |
| `OPEN_SIMULATOR` | 打开 Simulator.app | `0` |
| `STATUS_BAR` | 固定状态栏 | `1` |
| `SIM_DISPLAY` | 多显示器设备截图用的 display 编号 | 自动检测（优先 `(primary)`） |
| `ROUTES` | 额外截取的二级页面 | 空（可选 `bean recipe data newbean`） |
| `DIST_DIR` | IPA 输出目录 | `dist/` |

## GitHub Actions 云端构建（没有 Mac 也能出 IPA）

工作流：`.github/workflows/ios-unsigned.yml`（名称 **Build unsigned iOS IPA & screenshots**）。

1. 把整个仓库推到 GitHub（私有仓库也可以；macOS runner 会消耗 Actions 分钟数）。
2. 打开仓库的 **Actions** → 左侧选 **Build unsigned iOS IPA & screenshots** → **Run workflow**。
   - `run_screenshots`：是否顺便构建模拟器版并抓截图（默认开，关闭可省一半时间）；
   - `sim_name`：可选指定模拟器（如 `iPhone 16 Pro`），留空自动选择；
   - `require_liquid_glass`：是否硬性要求 Xcode ≥ 26（默认开；关掉后旧 Xcode 只回退毛玻璃、不报错）。
3. 流水线在 `macos-26` runner 上依次执行：
   - **自动挑选 runner 上最新的正式版 Xcode**（`/Applications/Xcode*.app`，`sudo xcode-select -s`，
     排除 beta），并打印 `xcodebuild -version` 与 iphoneos SDK 版本；
   - 若选中的 Xcode < 26 且 `require_liquid_glass=true`：直接失败并提示换 `macos-26` runner
     （避免静默产出「没有液态玻璃」的 IPA）；
   - 模拟器 Debug 构建 → 启动模拟器 → 逐页截图；
   - 真机 Release 无签名构建 → 打包未签名 IPA + SHA256；
   - 上传 artifacts，并把 Xcode 版本、是否包含 Liquid Glass、IPA 文件名/SHA256、
     截图数量写进本次运行的 Job Summary。
4. 在本次运行页面下载：
   - `BeanBook-unsigned-ipa`：未签名 IPA + `.sha256`；
   - `BeanBook-simulator-screenshots`：五个页面的模拟器截图 + `build-info.txt`。

说明：

- 云端 runner 自带完整 Xcode 和 iOS 模拟器运行时，**不下载任何大文件**；
- **Liquid Glass 需要 Xcode 26+**（iOS 26 SDK）：`macos-26` runner 默认满足；
  如果你的仓库暂时没有 `macos-26` 标签，可把 `runs-on` 改成 `macos-15`（前提是镜像带 Xcode 26+），
  或者关闭 `require_liquid_glass` 接受「毛玻璃回退」构建（构建照常完成，只是没有玻璃效果）；
- 工程没有 UI test target，所以工作流只做 build + `simctl` 截图，不跑 `xcodebuild test`；
- 截图步骤失败不会影响 IPA 产物（该步骤 `continue-on-error`，摘要里会标出结果）；
- 若想在本地预览流水线行为，在装有 Xcode 的 Mac 上执行 `bash ios/scripts/build-all.sh` 即可。

## 安装到 iPhone / iPad

### 方法 A：Mac + Xcode（推荐）

1. 准备 macOS + Xcode（推荐 26 或更新版本，Liquid Glass 生效；Xcode 16 也可编译，但无液态玻璃）。
2. 打开 `ios/BeanBook.xcodeproj`。
3. 选中左侧 `BeanBook` target → **Signing & Capabilities**：
   - Team 选择你的 Apple ID（免费 Apple ID 也可以）；
   - 如果提示 Bundle Identifier 被占用，把 `com.brewkin.beanbook.ios` 改成你自己的唯一值。
4. iPhone/iPad 用数据线连接 Mac，设备上信任电脑，并打开 **设置 → 隐私与安全性 → 开发者模式**（如果出现）。
5. Xcode 顶部选择你的设备 → 点击 Run。
6. 如果第一次启动提示「不受信任的开发者」，到 **设置 → 通用 → VPN 与设备管理** 信任你的 Apple ID。
7. 想上 TestFlight/App Store 时：Product → Archive → Distribute App（需要付费 Apple Developer 账号）。

### 方法 B：用流水线产出的未签名 IPA + Sideloadly / AltStore 自签

1. 按上一节从 GitHub Actions 下载 `BeanBook-unsigned-ipa` artifact，解压得到
   `BeanBook-1.2-unsigned.ipa`。
2. （可选）核对 SHA256：`shasum -a 256 BeanBook-1.2-unsigned.ipa`，与 `.sha256` 文件一致。
3. 在 Windows / macOS 安装 [Sideloadly](https://sideloadly.io/) 或 AltStore。
4. 用数据线连接 iPhone/iPad：
   - 把未签名 IPA 拖进 Sideloadly；
   - 输入你的 Apple ID（只用于本地签名，不会上传咖啡数据）；
   - 点击 Start，按提示输入 Apple ID 密码/双重认证；
   - 完成后在设备上信任开发者证书，并打开开发者模式。
5. 桌面会出现「忆杯手记」。**免费 Apple ID 签名通常 7 天有效**，到期用同一 IPA 重签即可；
   付费开发者账号有效期更长。

> Sideloadly/AltStore 只负责签名和安装。App 数据仍只在本机，没有 iCloud 同步、也没有账号系统。

### 方法 C：模拟器预览（无需设备）

```bash
cd beans-app
bash ios/scripts/build-simulator.sh
bash ios/scripts/run-simulator-screenshots.sh --skip-build --open-simulator
```

也可以在 Xcode 中选择任意 iPhone / iPad Simulator，Run。模拟器上可以使用照片选择、
数据导入导出和全部界面；Vision 抠图在模拟器上的可用性取决于模拟器版本，真机更稳定。

## 自检（不需要 Xcode 的静态校验）

```bash
cd beans-app

# 1) Shell 语法
for f in ios/scripts/*.sh; do bash -n "$f" && echo "OK: $f"; done

# 2) YAML 语法
ruby -ryaml -e 'YAML.load_file(".github/workflows/ios-unsigned.yml"); puts "YAML OK"'

# 3) plist
plutil -lint ios/BeanBook/Info.plist
```

在装有 Xcode 的机器上，建议再跑：

```bash
xcodebuild -list -project ios/BeanBook.xcodeproj     # 确认 target/scheme 都是 BeanBook
xcrun simctl list devices available                  # 确认有可用 iPhone 模拟器
```

## 当前已知限制

- 已用 Xcode 27.2 真实编译通过（模拟器 Debug + 真机 Release 无签名 + 未签名 IPA）；
  编译器为 Swift 6.4 / iOS 27.2 SDK，Liquid Glass 分支已实际编入。编译期修复过
  `Store.swift` 的一个初始化顺序错误，并修复了 `package-ipa.sh` 在 `pipefail` 下
  `unzip -l | grep -q` 的 SIGPIPE 误报问题。
- 模拟器 UI 截图已完成（iOS 27.2 Simulator / iPhone Duo 折叠屏 9 张，见 `screenshots/ios/`）；
  常规 iPhone（17/16）与 iPad 布局建议再用对应模拟器或真机复核一次。
- 真机安装仍需 Apple ID 签名（免费账号 7 天）：未签名 IPA 不能直接双击安装。
- Liquid Glass 需要 **Xcode 26+ 构建** 且 **iOS 26+ 运行**；旧系统/旧 Xcode 走毛玻璃回退，
  观感需在真机确认后再微调参数。
- 免费 Apple ID 签名的 App 7 天后失效。
- 本机已完成不需要 Xcode 的校验：`bash -n` 全部脚本、Ruby 解析 workflow YAML、
  `plutil -lint` Info.plist，并用假的 `xcodebuild`/`xcrun` 工具链演练过
  「模拟器构建 → 截图 → 真机构建 → IPA 打包」完整编排；真实编译仍需首次在
  有 Xcode 的 Mac 或 GitHub Actions 上验证。
- 没有 iCloud 同步；所有数据在本机。
- 未实现微信小程序式的社区/账号系统。
