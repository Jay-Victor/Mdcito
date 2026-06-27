# Changelog

本项目所有值得记录的变更都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 规范，
并且本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

---

## [1.2.0] - 2026-06-28

### Changed

#### 卡片与导航栏风格
- 产品术语统一：将"液态玻璃"更名为"液态水玻璃"（8 种语言同步：简中/繁中/英/日/韩/德/法）
- 磨砂玻璃与液态水玻璃的描述从"功能尚在开发"更新为实际功能描述
  - 磨砂玻璃：半通透模糊效果，方向光照浮雕纹理
  - 液态水玻璃：水润折射色散质感
- 卡片与导航栏风格设置页预览组件与实际效果对齐：顶部圆角（磨砂 16dp / 液态水玻璃 28dp）、图标 24dp、文字 12sp、选中项 56×32dp secondaryContainer 背景

### Fixed

#### 卡片与导航栏风格
- 修复磨砂玻璃效果不显示的问题：实现真实的 `.liquidGlass(enableLens=false)` 背景模糊
- 修复液态水玻璃效果不显示的问题：实现真实的 `.waterGlass()` 流体折射与色散效果
- 修复 GlassThemeProvisioning 中 `content()` 放在 `layerBackdrop` Box 内部导致的渲染树自引用环崩溃（SIGSEGV signal 11 SEGV_MAPERR）
- 修复液态水玻璃卡片样式设置页强度滑块无效的问题（glassIntensity 参数未传递到 LiquidGlassCard）
- 修复状态栏预览卡片内容被圆角裁剪的问题：预览容器宽度有限，导航项改用 `Modifier.weight(1f)` 平均分配宽度

#### 主题与状态栏
- 修复切换字体后系统状态栏与页面割裂（变黑/不透明）的问题：将 `enableEdgeToEdge()` 从 `SideEffect` 改为 `DisposableEffect(isDark)`，避免字体切换触发的不必要重置
- 新增 `values-night` 资源限定（`themes.xml` 和 `colors.xml`），确保深色模式下 `windowBackground` 和 `windowLightStatusBar` 正确设置，避免重组间隙的视觉割裂

---

## [1.1.0] - 2026-06-17

### Security

#### Markdown 渲染与预览
- 修复 Markdown 渲染器未启用 HTML 转义的 XSS 漏洞：为 commonmarkRenderer 和 gfmRenderer 添加 `.escapeHtml(true)`，并将 `body.innerHTML.replace` 替换为 TreeWalker 遍历文本节点的方式，避免二次 HTML 解析导致 XSS
- 修复 WebView 任意文件读取漏洞：关闭 MarkdownPreview 和 PdfExporter 中 WebView 的 `allowFileAccess` 和 `allowContentAccess`，添加路径规范化校验（`canonicalPath.startsWith`）和 content:// authority 白名单（仅允许本应用 FileProvider 和系统媒体 Provider）
- 修复锚点跳转 JS 注入：使用 `JSON.stringify` 编码锚点参数
- 移除 SVG MIME 类型支持以缩小攻击面

#### OAuth 与 Deep Link
- 修复 OAuth Deep Link state 参数未校验的 CSRF 漏洞：在 Route.createRoute 中对 code 和 state 使用 `URLEncoder.encode`，在 MainActivity.handleDeepLink 中添加 code 格式校验（长度 ≤512，字符集限制）

#### 云同步
- 修复云同步下载路径遍历漏洞：在 CloudSyncManager 阶段2添加 `..` 路径检查和 canonical path 二次校验，在 listRemoteFilesRecursive 中跳过包含 `..` 的远端条目
- 修复 OAuth Token 刷新竞态：在 GoogleDriveSyncProvider 和 OneDriveSyncProvider 中添加 `tokenRefreshMutex` 串行化令牌刷新，防止并发刷新导致 refresh_token 轮换竞态
- 修复 testConnection 未回写刷新后令牌的问题：显式调用 ensureValidToken 并将刷新后的令牌回写到持久化存储

#### OTA 更新
- 修复 APK 下载路径遍历与完整性校验缺失：添加 `sanitizeFileName` 剥离路径分隔符和 `..`，添加 canonical path 二次校验，新增 `expectedSha256` 参数和 `computeSha256` 实现 SHA-256 完整性校验

#### 图床
- 修复图床 SSRF 漏洞：添加 `validateUploadUrl` 校验上传 URL，禁止指向本地主机、内网地址、链路本地地址和云元数据服务（169.254.169.254、metadata.google.internal）

#### 图片处理
- 修复 ImageProcessor OOM：使用 `inJustDecodeBounds` + `inSampleSize` 降采样，添加 MAX_IMAGE_BYTES (20MB) 和 MAX_DECODE_PIXELS (4096×4096) 限制，中间 Bitmap 调用 `recycle()`
- 修复 EXIF 隐私信息泄露：新增 `stripExifFromBytes` 函数剥离 EXIF 隐私信息
- 添加 ALLOWED_EXTENSIONS 图片格式白名单校验
- 移除 SVG 格式支持

#### 敏感数据存储
- 图床自定义请求头（imageHostCustomHeaders）改用 EncryptedSharedPreferences 加密存储，避免 Authorization 等凭据明文泄露

### Fixed

#### 编辑器
- 修复 EditorViewModel 数据丢失与竞态条件：使用 `saveMutex.withLock` 序列化保存操作，`onCleared()` 中使用 `runBlocking + NonCancellable` 完成最后一次保存，`syncContentToDiskFile` 和 `readContentFromPath` 切换到 `Dispatchers.IO` 避免主线程 ANR

#### 云同步冲突
- 修复 REMOTE_WINS / LOCAL_WINS 冲突逻辑错误：Phase 1 中 REMOTE_WINS 在本地较新时应下载远端覆盖本地（原为跳过）；Phase 2 中 LOCAL_WINS 在远端较新时应上传本地覆盖远端（原为跳过）

### Changed

#### FTP 同步
- FTPS 改用显式 TLS (FTPES) 替代隐式 TLS，兼容性更好（现代 FTPS 服务器默认模式）
- 明文 FTP 连接时在日志和 testConnection 返回消息中附加安全警告，提示用户凭据未加密

#### OneDrive 同步
- 为 OkHttpClient 添加显式超时配置（connect 30s / read 60s / write 120s），避免网络异常时连接挂起
- 修复 deleteFile 和 createDirectory 中 Response body 未关闭导致连接泄漏的问题，改用 `use{}` 块确保资源释放

---

## [1.0.8] - 2026-06-17

### Changed

#### OTA 更新
- 自动检查更新改为每次应用启动时执行（用户开启「自动检查更新」后即生效），移除原先的 1 小时节流间隔，确保新版本发布后用户下次启动即可收到提示
- 移除已无用的 `lastUpdateCheckTime` 持久化字段及其在 DataStore / Repository / ViewModel 中的读写逻辑（该字段仅用于节流判断，节流移除后变为死代码）
- 保留启动时网络未就绪的 30 秒自动重试机制

---

## [1.0.7] - 2026-06-16

### Fixed

#### OTA 更新
- 修复自动检查更新在低版本启动时无提示的问题，根因及修复如下：
  - **节流时间过长**：将自动检查的节流间隔从 8 小时缩短为 1 小时，避免新版本发布后用户长时间收不到更新提示
  - **网络失败仍更新检查时间**：此前网络请求失败时仍会更新 `lastUpdateCheckTime`，导致后续 8 小时内不再检查；改为仅在至少一个平台（Gitee/GitHub）成功联系 API 时才更新检查时间，网络失败时不阻塞下次检查
  - **新增 `DualCheckResult.giteeContacted` / `githubContacted` 字段**：区分"网络失败"与"已是最新版本"两种 null 结果，使节流逻辑能正确判断检查是否成功
  - **新增自动重试机制**：两个平台均联系失败时（如启动时网络未就绪），延迟 30 秒自动重试一次，避免用户需手动检查或重启应用
  - **修复 `wasAutoCheck` 状态残留**：自动检查未发现新版本时重置该标记，避免状态管理不严谨
  - **修复手动检查单平台时 `contacted` 标志不准确**：此前 `giteeContacted = source == UpdateSource.GITEE` 即使网络失败也为 true；改为依据 `checkResult.contacted`，确保网络失败时不更新检查时间

#### 镜像加速
- 替换已失效的 GitHub 镜像节点 `ghp.ci`（2026 年已被 GFW 封锁，ghproxy.link 官方确认）为 `gh-proxy.com`（日请求 3000w+，最活跃）
- 替换假冗余镜像 `ghproxy.cc`（urlquery.net 报告证实其跳转到 `ghfast.top`，非独立服务）为 `ghproxy.net`（独立服务，支持断点续传）
- 现在三个镜像均为独立服务，形成真正的容灾冗余

#### 更新弹窗交互
- 修复「稍后安装」按钮空操作问题：此前 `onClick = {}` 点击后无任何反应，改为正确关闭对话框
- 修复版本不一致提示颜色过强：从 `errorContainer`（红色）改为 `tertiaryContainer`，版本不一致是信息提示而非错误
- 修复暂停状态提示不明确：暂停时显示"已暂停"文字提示（tertiary 颜色），避免用户困惑为何进度不动
- 修复「访问发布页」按钮下载开始后消失：从 `DownloadSection` 的 Idle 状态移至 `UpdateAvailableContent`，所有下载状态下均可见
- 修复更新日志按 char 截断可能在代理对（如 emoji）中间截断的问题
- 移除「重新检查」按钮中多余的 `onSetUpdateSource` 调用，消除时序竞态

### Added

#### OTA 更新
- 新增下载 URL 队列自动回退机制：构建下载 URL 队列（指定的镜像 → 原始地址 → 其他镜像），某个源下载失败后自动切换到下一个，直到成功或全部失败，用户无需手动重试不同镜像
- 新增 `update_paused` 字符串资源（中/英/日/韩/德/法/繁中 7 种语言）

---

## [1.0.6] - 2026-06-16

### Fixed

#### OTA 更新
- 替换已失效的 GitHub 镜像加速节点 `ghproxy.com` 为 `ghfast.top`（实测 2.4 MB/s），替换 `gh.api.99988866.xyz` 为 `ghproxy.cc`（2026 年社区验证可用）
- 修复双源（GitHub + Gitee）版本不一致时盲目优先 Gitee 导致用户可能下载到旧版本的问题：改为优先选择版本号更高的源，版本相同时仍优先 Gitee（国内速度快）
- 更新对话框默认选中平台改为根据版本号高低决定，而非固定选中 Gitee
- 新增双平台版本不一致提示：当 Gitee 和 GitHub 最新版本号不同时，显示醒目提示条告知用户已默认选中更高版本，可手动切换

---

## [1.0.5] - 2026-06-16

### Fixed

#### OTA 更新
- 修复应用启动时不会自动检查更新的问题：此前自动检查更新逻辑仅在用户导航到"关于"页面时触发，而非应用启动时触发
- 将 `UpdateViewModel` 的自动检查更新逻辑从 `AboutScreen` 移至 `MainActivity`，确保应用启动后（完成引导且授予权限时）自动检查
- `AboutScreen` 改为共享 Activity 级别的 `UpdateViewModel` 实例，避免重复创建和重复检查
- 自动检查发现新版本时直接在主界面弹出更新对话框，无需用户手动进入"关于"页面

---

## [1.0.4] - 2026-06-16

### Fixed

#### 开场动画
- 修复开场动画开关设置不生效的问题：`collectAsState(initial = true)` 在 DataStore 加载完成前返回初始值 `true`，导致即使用户关闭了开场动画，启动时仍会显示
- 改为直接从 Flow collect 真实值后再决定是否显示开场动画，避免初始值干扰

---

## [1.0.3] - 2026-06-16

### Fixed

#### OTA 更新
- 修复自动检查更新发现新版本后不会自动弹出更新提示对话框的问题
- 添加 `wasAutoCheck` 标志区分自动检查和手动检查结果
- 添加 `shouldAutoShowDialog()` 和 `markAutoDialogShown()` 方法控制弹窗行为
- 用户关闭自动弹窗后，下次进入 AboutScreen 不会重复弹出

---

## [1.0.2] - 2026-06-16

### Fixed

#### OTA 更新
- 修复 Gitee 下载 URL 使用错误的 API URL 字段问题，改为使用 `browser_download_url` 作为直接下载链接
- 修复 URL 缺少 HTTP/HTTPS 协议前缀导致的下载失败问题（"Expected URL scheme 'http' or 'https' but no scheme was found for"）
- 添加三层 URL 验证机制：UpdateChecker 检查更新时验证、镜像 URL 生成时验证、ApkDownloader 发起下载前验证
- Gitee 下载 URL 回退构造：当 `browser_download_url` 为空时，自动构造标准下载链接格式

---

## [1.0.1] - 2026-06-16

### Changed

#### 主题系统
- 动态主题色预览组件重构：使用实际应用的 `MaterialTheme.colorScheme` 替代独立获取的动态色方案，确保预览颜色与界面完全一致
- 添加动态色状态指示器：显示"动态色已启用/未启用"状态，带有颜色指示灯
- 添加颜色对比视图：左侧显示当前 Primary 颜色，右侧显示静态主题色参考，方便用户对比差异
- 预览组件始终可见：无论是否开启动态色，预览都会显示，让用户能直观看到当前主题色效果
- 新增字符串资源：`dynamic_color_active`、`dynamic_color_inactive`、`dynamic_color_comparison`

---

## [1.0.0] - 2026-06-15

### Added

#### 编辑器
- 双模式编辑系统：纯文本编辑模式与 WebView 渲染预览模式，顶栏按钮一键切换
- 沉浸式全屏预览，隐藏状态栏与导航栏，点击屏幕退出
- 编辑区与预览区双向同步滚动（可在设置中启用/禁用）
- 17 组格式化工具栏按钮，跟随键盘升降：撤销/重做、标题 H1-H6、加粗/斜体/粗斜体/删除线、无序/有序/任务列表、引用块/行内代码/代码块、链接/图片插入、分割线/表格/行内公式/块级公式、全文搜索
- Markdown 语法高亮：标题、粗体、斜体、代码、链接、引用、列表、表格、公式着色，颜色随主题联动
- 100+ 编程语言代码块语法高亮（内嵌 highlight.js）
- LaTeX 数学公式渲染引擎（内嵌 KaTeX），支持行内 `$...$` 和块级 `$$...$$`
- 目录导航（Table of Contents）：右侧滑出侧边栏，显示 H1-H6 标题层级，支持展开/折叠与点击跳转
- 拼写检查引擎：英文 SymSpellKt（1000+ 常用词词典）+ 中文常见错别字规则检测（的/地/得、在/再、即/既等 20+ 组），支持用户自定义词典，红色波浪下划线标注，点击弹出纠正建议
- 全文搜索与替换：支持大小写敏感、全词匹配、正则表达式，搜索历史记录
- 行号显示（可选）+ 当前行高亮（可自定义高亮颜色）
- 智能自动缩进：换行继承上一行缩进，支持空格/制表符切换与缩进大小配置
- 代码块内括号自动匹配
- 可配置间隔的自动保存，保存状态顶栏指示

#### 文件管理
- 基本文件/文件夹操作：创建、重命名、删除、复制、移动、分享
- 多级嵌套文件夹结构，面包屑导航
- 标签系统：为文件和文件夹添加自定义标签，支持按标签过滤
- 置顶功能：置顶常用文件/文件夹，首页置顶区快速访问
- 排序：按名称、修改时间、文件大小排序（升序/降序）
- 过滤：按文件类型（.md/.txt/.markdown）、时间范围（今天/本周/本月/今年）、标签、置顶状态组合过滤
- 卡片左滑快捷删除，右滑快捷置顶
- 外部 Markdown 文件导入：仅查看模式或导入到工作区
- 首页：品牌展示区、四个快捷操作卡片（新建文件/文件夹、打开文件/文件夹）、统计信息、置顶区、最近访问区

#### 导出
- Markdown 格式（.md）：原始源码直接写出
- 纯文本格式（.txt）：去除 Markdown 标记
- HTML 格式（.html）：完整 HTML 页面，含内嵌 CSS 样式
- PDF 格式（.pdf）：WebView 渲染 → PrintManager 生成矢量 PDF（A4、300 DPI），所见即所得
- DOCX 格式（.docx）：Apache POI 生成，完整支持标题层级、粗体/斜体/删除线、行内代码、链接、引用块、列表、代码块（灰色背景）、分割线

#### 云同步
- 支持 6 种协议/服务：WebDAV、FTP、FTPS (TLS)、SFTP (SSH)、OneDrive (OAuth2)、Google Drive (OAuth2)
- 手动/自动同步模式，自动同步最小间隔 15 分钟
- 网络约束：仅 Wi-Fi 同步、充电时同步
- 冲突解决策略：保留较新版本 / 本地优先 / 远程优先 / 手动解决
- 文件过滤：通配符排除特定文件模式
- 基于 WorkManager 的后台自动同步任务调度

#### 图床
- 7 种图床服务：GitHub、七牛云 Kodo、阿里云 OSS、腾讯云 COS、Imgur、SM.MS、自定义 API
- 多方案管理：创建/切换/导入/导出配置方案
- 连接测试：独立验证每个方案配置正确性
- 图片处理：压缩（质量 0-100）、缩放（HD/Full HD/2K/4K 或自定义尺寸）、水印（文字/位置/不透明度）
- 自动重命名：时间戳、日期、随机字符串、UUID、原文件名
- 密码/Token 类字段使用 AndroidX Security Crypto 加密存储

#### 主题系统
- 3 种主题模式：跟随系统、浅色、深色
- 12 种主题色 + Android 12+ Material You 动态取色
- 浅色方案：暖色、冷色、纸质、清新
- 深色方案：暖色深色、冷色深色、OLED、午夜
- 3 种卡片风格：日式简约（Japandi Minimal）、磨砂玻璃（Frosted Glass）、液态玻璃（Liquid Glass）
- 字体管理：界面字体、编辑器字体、代码块字体独立设置；内置思源黑体、JetBrains Mono、霞鹜文楷；支持导入本地 .ttf/.otf
- 背景设置：软件背景、编辑器背景独立设置；高斯模糊、亮度调节；卡片/导航栏透明度

#### 国际化
- 7 种语言：简体中文、繁體中文、English、日本語、한국어、Deutsch、Français
- 运行时切换，重启后生效

#### 其他
- 版本快照：每次保存自动创建，每文件最多保留 50 个；历史版本查看、内容预览、恢复到任意版本
- OTA 更新：GitHub + Gitee 双源并行检查，应用内下载 APK 安装，3 个 GitHub 镜像加速节点
- 日志系统：Error/Warn/Info/Debug 四级（基于 Timber）；调试模式下查看各级别日志，导出日志文件
- 触觉反馈：部分操作提供振动反馈
- 新手引导：首次启动核心功能介绍 + 文件权限授权引导
- 开场动画：启动品牌 Logo 动画（无障碍"减少动画"设置开启时自动跳过）
- 响应式布局：适配小屏手机（≤360dp）、标准手机（361-599dp）、平板竖屏（600-839dp）、平板横屏/折叠屏（≥840dp）
- GPU 加速渲染（可在高级设置中切换）

#### 技术基础
- Kotlin + Jetpack Compose + Material 3 现代化 UI 框架
- Hilt 依赖注入（含 Navigation Compose + Work 集成）
- Room 本地数据库（3 实体、5 次迁移历史）
- DataStore 偏好存储 + 加密 DataStore 敏感凭据存储
- Navigation Compose 路由（24 个路由页面）
- Coil 3 图片加载
- OkHttp 网络请求
- AGPL-3.0 / 商业双协议授权

---

[1.2.0]: https://github.com/Jay-Victor/Mdcito/releases/tag/v1.2.0
[1.1.0]: https://github.com/Jay-Victor/Mdcito/releases/tag/v1.1.0
[1.0.8]: https://github.com/Jay-Victor/Mdcito/releases/tag/v1.0.8
[1.0.7]: https://github.com/Jay-Victor/Mdcito/releases/tag/v1.0.7
[1.0.6]: https://github.com/Jay-Victor/Mdcito/releases/tag/v1.0.6
[1.0.5]: https://github.com/Jay-Victor/Mdcito/releases/tag/v1.0.5
[1.0.4]: https://github.com/Jay-Victor/Mdcito/releases/tag/v1.0.4
[1.0.3]: https://github.com/Jay-Victor/Mdcito/releases/tag/v1.0.3
[1.0.2]: https://github.com/Jay-Victor/Mdcito/releases/tag/v1.0.2
[1.0.1]: https://github.com/Jay-Victor/Mdcito/releases/tag/v1.0.1
[1.0.0]: https://github.com/Jay-Victor/Mdcito/releases/tag/v1.0.0
