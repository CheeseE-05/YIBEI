# 忆杯手记 1.2 发行说明（iOS + Android）

> 发行日期：2026-11-07 · 工程路径：`beans-app/` · 版本：**1.2（Android versionCode 3 / iOS build 3；2026-11-07 追加 1.2 功能包，版本号不变）**

## 1. 这一版更新了什么

### 1.1 菜单里的配方可以修改（双端功能对齐）
- 配方详情页新增「**编辑配方**」入口（iOS：右上绿色铅笔；Android：底部操作条「编辑」）。
- 点击后载入「调一杯 / 制作」页的编辑态：顶部显示「**正在修改配方 · 名称**」提示条 + 「取消编辑」。
- 保存按钮变为「**保存修改**」：**更新原配方**（保留 id / 创建时间 / 原图片等，不新增一杯），菜单列表、详情、平均分实时刷新。
- 取消编辑：恢复进入编辑前的草稿，**原配方与数据完全不变**（iOS 实测 `data.json` 逐字节一致；Android 实测草稿恢复）。
- 库存语义（本版有意为之）：**编辑保存只更新配方记录，不会重复扣豆、也不按差值补扣**；保存弹窗已明确提示「保存后更新原配方，不会新增一杯、不影响豆仓库存」。

### 1.2 Android 视觉：传统 Android 布局 + 小米澎湃 OS 4 风格导航（参考官方视频）
- 底部改为**悬浮毛玻璃胶囊导航条**（参考小米澎湃 OS 4 联系人 App 的导航栏设计）：高 62dp、全圆角、左右 16dp 外边距、距手势条 10dp；
  胶囊背景为暖白半透明 + 整块磨砂层（暖白底 alpha 0.90 + 4 个径向色斑 `Modifier.blur(22dp)` + 整块暖白磨砂雾 + 200 颗细噪点；API 31+ 真实模糊、API<31 渐变降级）+
  1dp 白高光描边 + 极轻外阴影；列表内容可从胶囊下经过并隐约透出。
- 胶囊内 **4 个 tab**（首页/豆子/菜单/统计）：图标 22dp、文字 11sp；选中项背后是**浅灰玻璃滑块**，与胶囊**内外圆角同心**
  （外 R=31dp、统一内缩 d=4dp、内 r=27dp），以短促 spring 滑动；未选中无底色；**点击不会出现整块变灰/水波纹的按压效果**
  （无 indication），滑块本身即选中提示。
- 右侧独立**绿色圆形「调一杯」FAB**（58dp、`#35D06A`、白色咖啡杯图标）：点击进入调一杯；选中时放大 1.06 + 柔光环。
  FAB 外层容器固定为 74dp，选中放大/光环只做绘制变换，**不会挤压左侧胶囊宽度**（已修复）。
- 详情/编辑页底部改为**白色悬浮圆角操作条 + 绿色主按钮**；卡片白底 + 1dp 细边 + 1dp 极轻阴影 + 26dp 圆角，页面标题 30sp Bold。
- 平台差异：Android 无系统级 backdrop blur，磨砂为平台差异下的高保真近似（模糊层只作用于胶囊内小面积，不整页 blur）。

### 1.3 版本号
- Android：`versionName 1.1 → 1.2`，`versionCode 2 → 3`。
- iOS：`CFBundleShortVersionString 1.0 → 1.2`，`CFBundleVersion 1 → 3`；pbxproj `MARKETING_VERSION=1.2` / `CURRENT_PROJECT_VERSION=3`。

### 1.4 2026-11-07 追加 1.2 功能包（版本号仍 1.2 / build 3）

1) **「配方别册」→「配方目录」**：双端所有用户可见文案统一改为「配方目录 / 翻目录 / 存进目录 / 从目录删除 / 设置与数据」。
2) **设置页**：入口在「统计」页最底部。内容：AI API 配置（Base URL / API Key / 模型名，保存、测试连接、恢复默认）、
   黑暗模式（跟随系统 / 浅色 / 深色，立即生效并持久化）、全部数据导入导出（整库导出 / 合并导入 / 覆盖恢复 / 导入离线配方，
   从原「数据与分享」页迁移进设置；单杯配方分享仍在配方详情）、关于/版本/隐私说明。
3) **黑暗模式（双端）**：iOS 动态色板 + `.preferredColorScheme`、Android 双调色板 + `darkColorScheme`，全页面（首页/目录/调一杯/统计/设置/详情/卡片）可读。
4) **横版宣传卡片（每个配方一张）**：配方详情/菜单入口；16:9（可选 3:2）社交卡片风——封面（杯图/豆插画/相册图）、大标题、副标题、
   标签 chips、品牌落款；**内置设计工具**：3 套模板、主题色、封面选择、文字实时编辑与预览；配置持久化到 Recipe 可选字段（旧数据兼容）。
5) **AI 生成**：OpenAI 兼容 `chat/completions`（设置页配置 BaseURL/Key/模型）；输入描述 → 模型返回卡片 JSON
   （标题/副标题/标签/主题/模板）→ 解析套用预览；失败可重试；未配置可手动设计。
6) **导出相册**：卡片渲染为高清 PNG（iOS 2400×1350 / Android 1200×675）保存到系统相册（iOS PHPhotoLibrary、Android MediaStore），
   权限拒绝有友好提示；同时卡片配置保存到配方。
7) **Android 材料栏**：调一杯页 INGREDIENT TRAY 改为与 iOS 一致的 30dp 全圆角卡片 + 左右/底部留白（不再贴底平切）。

> 实现与验证细节：iOS `ios/artifacts/verify-1.2-features/`（16 张截图 + VERIFY-REPORT.md）、
> Android `app/artifacts/verify-1.2-features/`（22 张截图 + 2 张导出成品 + NOTES.md）；汇总图在 `screenshots/features-1.2/`。
> AI 联调使用本地 mock 服务（`tools/mock-ai-server.py`，OpenAI 兼容格式），真实 API 同格式即可。

### 1.5 2026-11-07 二次追加：社交卡片风「随杯卡 v2」（版本仍 1.2 / build 3）

参照用户提供的手绘咖啡随杯卡实拍（工程内 `docs/reference/coffee-cards/`）：

- **定位**：每个配方单品的随杯小卡（咖啡店打印随杯附送 / 拍照打卡），**不做 App 宣传**；卡片上不出现任何 App 名称/宣传语。
- **版式**：奶油纸底（#F5F1E6）+ 细纸纹/纤维；顶部左侧分类（拿铁/冷萃/特调，由层推导）+ 右侧烘焙度/处理法 + `¥价格`（小圆点）；细横线；中部 serif 大号英文名 + 中文名；配料中/英两行；「风味参考：」+ 风味关键词；中部偏下**手绘水彩咖啡插画**；底部 2–3 关键词 + 极细主题色线。
- **横版 + 竖版**：竖版 A6 105:148（300dpi 1240×1748）、横版 A6 148:105（1748×1240）；另支持社交尺寸 1200×1600 / 1200×675。导出 PNG 到相册、系统分享、**系统打印（A6/300dpi，Android PrintManager / iOS 分享面板含打印）**。
- **AI 插画（GLM-Image）**：设置页新增「插画生成 API」预置 **智谱 BigModel · GLM-Image（约 0.1 元/次，推荐）** —— Base `https://open.bigmodel.cn/api/paas/v4`、model `glm-image`、`POST /images/generations`、Bearer Key；按卡片方向自动取 1024×1440（竖）/1440×1024（横），响应 URL 下载存私有目录（兼容第三方 b64_json）；prompt 按配方动态生成（手绘水彩/奶油纸底/暖色/配料点缀/无文字/居中，≤1000 字符）；可重新生成、回退本地杯图/豆插画/相册图。
- **导入图片 + 自动抠图**：卡片封面可选「相册导入（自动抠图）」——iOS Vision 前景蒙版（模拟器不可用时本地降级）、Android 内置 U²-Netp ONNX；输出**透明背景 PNG** 直接叠在奶油纸底（无白底硬边），支持重新抠图/用原图/换图，失败有提示。
- **价格设置**：`Recipe.price: Int?`（元；0/空=未设置，旧 JSON 兼容）；保存/编辑配方弹窗可填「菜单价格 ¥」；菜单列表与详情显示 ¥NN（未设置不显示）；卡片编辑器可改价并随卡片一起持久化。
- **验证材料**：`ios/artifacts/verify-1.2-cards-v2/`（13 张截图 + 报告）、`app/artifacts/verify-1.2-cards-v2/`（16 张 + 实际导出 PNG + NOTES.md），汇总在 `screenshots/features-1.2/cards-v2-ios|android/`。
- **注意**：本机无真实智谱 Key，GLM-Image 线上出图未实测（按官方文档 + 本地 mock 的 url/b64 双分支验证）；截图中的插画为 mock 占位图，真实 Key 填入后生成手绘水彩插画。

### 1.6 2026-11-07 三次追加：随杯卡编辑器 v3（全元素编辑 / 拖拽 / 每元素字号）

- **全元素可编辑**：分类·产地、烘焙度/处理法、价格、英文名、中文名、配料行、风味参考、底部关键词均可手写（留空自动按配方生成）；插画来源可切换 AI 插画 / 相册导入自动抠图 / 杯图 / 豆插画；每个元素可显示/隐藏。
- **手动布局**：点击「编辑布局」后元素出现虚线选框，可直接拖动位置；每个文字元素独立 **字号 8–72pt** + 左/中/右对齐，图片支持 50–200% 缩放；实时预览。
- **默认自动排版**：未手动调整时按实测文本高度流式排版，不重叠/不出界，A6 与社交尺寸都不截断；切换模板/横竖版自动重排。
- **防遮挡硬约束**：拖动时实时碰撞检测（重叠红色提示、松手回退）；保存/导出/分享/打印前分别按社交与 A6 尺寸校验，有重叠直接拦截；「恢复自动排版」一键还原。
- **持久化**：全部文案、图片路径/来源、每元素横/竖版归一化位置/缩放/对齐/字号/隐藏状态保存在卡片配置（旧 JSON 兼容）。
- **双端一致**：Android 编辑器 UI 与 iOS 对齐（页面结构/面板/控件/交互/样式），设计文档见 `docs/design/card-editor-ui-spec.md`。
- 截图：`ios/artifacts/verify-1.2-card-editor/`（12 张）、`app/artifacts/verify-1.2-card-editor/`；设计规范 `docs/design/promo-card-v2.md`。

## 2. 发行产物

| 产物 | 路径 | 大小 | SHA256 |
| --- | --- | --- | --- |
| Android APK（已签名，arm64） | `dist/BeanBook-v1.2-arm64.apk`（同内容 `dist/忆杯手记-v1.2-arm64.apk`） | 31,326,016 B（约 29.7 MiB） | `cf44a987babe9cbe707b14df4fa1d19ff9ff8e0b8b86b16ada45e8a7e08ffb3e` |
| iOS 未签名 IPA（arm64，minos 17.0） | `dist/BeanBook-1.2-unsigned.ipa` + `.sha256` | 1,736,368 B（约 1.3 MB） | `d03008e85a37374bee1d31101c7cc74df86c889e1c15305fa181d9d7c0cb972d` |
| 源码包（Android + iOS + 文档 + 脚本） | `dist/BeanBook-source-v1.2.zip` | 见 `dist/SHA256.txt` | 见 `dist/SHA256.txt` |
| 校验清单 | `dist/SHA256.txt` | — | 含上述全部文件的 SHA256 与字节数 |

- Android 安装：直接安装 `dist/BeanBook-v1.2-arm64.apk`（Android 8.0+ / arm64；首次安装需允许「安装未知应用」）。
- iOS 安装：IPA 未签名，需用 **Sideloadly / AltStore** 以 Apple ID 自签（免费账号 7 天），
  或 Xcode 登录 Apple ID 后 Run。**打包 IPA 本身不需要 Apple ID，只有装真机需要签名。**
- 历史版本（1.0 IPA / 1.1 APK / 1.1 源码包）已归档到 `dist/archive/`。

## 3. 双端验证记录

### 3.1 iOS（Xcode 27.2 · iPhone 17 模拟器 iOS 27.2）
- `swiftc -parse` 全部通过；Debug / Release 构建均 `BUILD SUCCEEDED`；Release 产物 arm64 / minos 17.0 / 未签名。
- 编辑闭环实测：菜单 3 杯 → 编辑「荔枝气泡冷萃」→ 改层（220g/3 层 → 230g/4 层）→ 保存 → 配方数仍 3、id/createdAt 保留、
  菜单/详情实时刷新、豆仓库存不变；取消编辑前后 `data.json` 逐字节一致。
- 截图（`ios/artifacts/verify-1.2/`）：菜单、详情（编辑入口）、编辑态、保存弹窗、取消后、保存后菜单/详情，共 7 张 + `VERIFY-REPORT.md`。
- 回归：5 个 tab + 豆子/配方/数据三个二级页截图全部成功（输出在 /tmp，未覆盖工程截图目录）。

### 3.2 Android（JDK 17 + Android SDK 35 · API 35 arm64 模拟器 1080×2400）
- `./gradlew :app:assembleRelease` BUILD SUCCESSFUL；APK `versionCode=3` / `versionName=1.2` / minSdk 26 / targetSdk 35，
  apksigner 验签通过（证书 CN=BeanBook）。
- 模拟器冷启动与全流程操作无 FATAL；编辑→保存（菜单仍 3 杯、平均分/详情同步）、编辑→取消（草稿恢复 20g/1 层）均实测通过。
- 截图（`screenshots/android-1.2/` 13 张 PNG + `16-tab-switch.gif` + `VERIFY-NOTES.md`；原始证据在 `app/artifacts/verify-1.2-hyperos/`）：
  首页/豆子/菜单/统计选中态、调一杯 FAB 选中态、详情白色悬浮操作条、编辑态、保存面板、保存后、取消后、
  内容滚过胶囊、导航栏特写（玻璃滑块/整块磨砂）与**逐帧真实的滑块切换 GIF**（`16-tab-switch.gif`）。

### 3.3 静态与工程校验
- iOS：`swiftc -parse` 17/17、数据层测试 211 PASS、工程校验 24 PASS、Liquid Glass 门禁 6 PASS（本机工具链已核对）。
- Android：Gradle assembleDebug/Release 均成功；`aapt2 dump badging`、`apksigner verify` 通过。

## 4. 已知限制
1. **Android 无系统 backdrop blur**：玻璃后的真实页面内容仍清晰透出（1.2 用半透明底 + 模糊色斑 + 颗粒柔化近似），
   与 iOS 系统真实毛玻璃存在平台差异；API < 31 时 `blur` 自动降级为渐变。
2. Android 噪点/色斑为静态层，不随滚动内容变化；未做低端真机性能实测（绘制仅小面积 Canvas，无整页 blur）。
3. iOS 编辑保存不更新豆子的 `lastUsedAt`；「改豆子/改克数按差值修正库存」需要产品先定义规则，本版不做。
4. iOS 未上真机 / iPad；建议后续抽检 iPad、iOS 17–25 经典 Tab Bar、系统「降低透明度」。

## 5. 相关文档
- 完整交付报告（含历史 1.0 记录）：[`docs/DELIVERY-REPORT.md`](DELIVERY-REPORT.md)
- QA 验收报告：[`docs/qa/qa-report.md`](qa/qa-report.md)
- iOS 工程说明：[`ios/README-IOS.md`](../ios/README-IOS.md)
- Android 导航实现说明与近似取舍：[`screenshots/android-1.2/VERIFY-NOTES.md`](../screenshots/android-1.2/VERIFY-NOTES.md)
