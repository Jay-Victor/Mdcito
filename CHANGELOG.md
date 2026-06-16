# Changelog

本项目所有值得记录的变更都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 规范，
并且本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

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

[1.0.6]: https://github.com/Jay-Victor/Mdcito/releases/tag/v1.0.6
[1.0.5]: https://github.com/Jay-Victor/Mdcito/releases/tag/v1.0.5
[1.0.4]: https://github.com/Jay-Victor/Mdcito/releases/tag/v1.0.4
[1.0.3]: https://github.com/Jay-Victor/Mdcito/releases/tag/v1.0.3
[1.0.2]: https://github.com/Jay-Victor/Mdcito/releases/tag/v1.0.2
[1.0.1]: https://github.com/Jay-Victor/Mdcito/releases/tag/v1.0.1
[1.0.0]: https://github.com/Jay-Victor/Mdcito/releases/tag/v1.0.0
