# 贡献指南

感谢你有兴趣为 Mdcito 做出贡献！本指南将帮助你了解如何参与项目开发，无论是报告 Bug、提出建议，还是提交代码。

## 目录

- [行为准则](#行为准则)
- [我有问题](#我有问题)
- [如何贡献](#如何贡献)
  - [报告 Bug](#报告-bug)
  - [提出功能建议](#提出功能建议)
  - [参与翻译](#参与翻译)
  - [贡献代码](#贡献代码)
- [开发环境搭建](#开发环境搭建)
- [项目结构](#项目结构)
- [代码风格](#代码风格)
- [提交信息规范](#提交信息规范)
- [Pull Request 流程](#pull-request-流程)
- [许可证](#许可证)

---

## 行为准则

本项目及所有参与者受行为准则约束。参与本项目即表示你同意遵守该准则。如遇到不当行为，请通过 Issues 或邮件向维护者报告。

---

## 我有问题

在提交 Issue 之前，请先完成以下步骤：

1. **查阅 [README](README.md)**：项目文档中可能已经包含了你要找的答案。
2. **搜索已有 [Issues](https://github.com/Jay-Victor/Mdcito/issues)**：确认是否有其他人遇到过相同的问题。
3. **搜索互联网**：有些通用问题（如 Kotlin、Compose 使用问题）可能在 Stack Overflow 等社区已有解答。

如果上述步骤无法解决你的问题，请创建一个新的 Issue，并提供尽可能多的上下文信息。

---

## 如何贡献

### 报告 Bug

高质量的 Bug 报告能帮助维护者快速定位和修复问题。提交前，请做好以下准备：

**提交前检查清单：**

- [ ] 确保使用的是最新版本
- [ ] 确认 Bug 并非由设备兼容性或系统环境引起
- [ ] 搜索已有 [Issues](https://github.com/Jay-Victor/Mdcito/issues)，确认该 Bug 未被报告过
- [ ] 收集关键信息：**Android 版本、设备型号、复现步骤、截图或录屏**

**安全漏洞**：请勿在公开 Issue 中报告安全漏洞。如有发现，请发送邮件至 `18261738221@163.com`。

**创建一个好的 Bug 报告：**

请在 Issue 中包含以下内容：

```
**描述**
清晰简洁地描述 Bug 是什么。

**复现步骤**
1. 打开 '...'
2. 点击 '...'
3. 滚动到 '...'
4. 出现错误

**期望行为**
描述你期望发生什么。

**截图/录屏**
如果适用，附上截图或屏幕录制。

**设备信息**
 - 设备型号：[例如 Pixel 8]
 - Android 版本：[例如 Android 15]
 - Mdcito 版本：[例如 1.0.0]

**附加信息**
关于此问题的任何其他补充信息。
```

### 提出功能建议

欢迎提出功能改进建议！请在 Issue 中说明：

- **你的使用场景**：这个功能要解决什么问题？
- **建议的方案**：你期望的实现方式是什么？
- **是否有替代方案**：考虑过其他实现方式吗？
- **附加信息**：截图、参考链接等有助于说明的内容。

创建 Issue 时请使用清晰的标题，并在描述中尽可能详细。

### 参与翻译

Mdcito 目前支持 7 种语言，欢迎帮助改进现有翻译或增加新语言。

翻译文件位于 `app/src/main/res/` 目录下：
- `values-zh-rCN/strings.xml` - 简体中文
- `values-zh-rTW/strings.xml` - 繁體中文
- `values/strings.xml` - English（默认语言）
- `values-ja/strings.xml` - 日本語
- `values-ko/strings.xml` - 한국어
- `values-de/strings.xml` - Deutsch
- `values-fr/strings.xml` - Français

新增语言时，请参考现有文件创建对应的 `values-{语言代码}/strings.xml`，并在 `values/locales_config.xml` 中添加配置。

### 贡献代码

#### 开发环境搭建

##### 环境要求

| 工具 | 版本要求 |
|------|----------|
| Android Studio | Hedgehog (2023.1.1) 或更高版本 |
| JDK | 17 或更高版本 |
| Android SDK | API Level 36 |
| Gradle | 8.10.1+（项目包含 Wrapper，无需手动安装） |
| 目标设备 | Android 14 (API 34) 及以上 |

##### 搭建步骤

```bash
# 1. Fork 本仓库
# 在 GitHub 上点击仓库右上角的 Fork 按钮

# 2. 克隆你的 Fork
git clone https://github.com/你的用户名/Mdcito.git
cd Mdcito

# 3. 添加上游仓库
git remote add upstream https://github.com/Jay-Victor/Mdcito.git

# 4. 创建功能分支
git checkout -b feature/你的功能名称

# 5. 用 Android Studio 打开项目根目录，等待 Gradle 同步完成

# 6. Debug 模式构建运行
./gradlew installDebug
```

> **关于签名**：Debug 模式使用默认调试签名。如需构建 Release 版本，需在项目根目录创建 `keystore.properties` 文件并配置个人签名。此文件已加入 `.gitignore`，不会被提交。

#### 项目结构

```
Mdcito/
├── app/
│   ├── src/main/java/com/mdcito/app/
│   │   ├── data/                    # 数据层
│   │   │   ├── datastore/           # DataStore 键值存储
│   │   │   ├── db/                  # Room 数据库
│   │   │   ├── files/               # SAF 文件操作
│   │   │   ├── font/                # 字体服务
│   │   │   ├── image/               # 图床 + 图片处理
│   │   │   ├── locale/              # 多语言
│   │   │   ├── log/                 # 日志
│   │   │   ├── model/               # 数据模型
│   │   │   ├── repository/          # Repository 层
│   │   │   ├── sync/                # 云同步引擎
│   │   │   └── update/              # OTA 更新
│   │   ├── di/                      # Hilt 依赖注入模块
│   │   ├── markdown/                # Markdown 渲染与导出
│   │   ├── ui/                      # UI 层
│   │   │   ├── components/          # 共享组件
│   │   │   ├── editor/              # 编辑器
│   │   │   ├── files/               # 文件管理
│   │   │   ├── history/             # 历史记录
│   │   │   ├── home/                # 首页
│   │   │   ├── navigation/          # 导航路由
│   │   │   ├── onboarding/          # 新手引导
│   │   │   ├── settings/            # 设置页面
│   │   │   └── theme/               # 主题系统
│   │   └── util/                    # 工具类
│   ├── src/main/res/                # 资源文件（7 种语言）
│   ├── src/main/assets/             # Assets（highlight.js）
│   └── build.gradle.kts
├── gradle/libs.versions.toml        # 版本目录
├── build.gradle.kts                 # 根构建脚本
├── settings.gradle.kts              # 项目设置
└── gradlew                          # Gradle Wrapper
```

#### 代码风格

##### Kotlin

- 遵循 [Kotlin 官方代码风格](https://kotlinlang.org/docs/coding-conventions.html)（项目已配置 `kotlin.code.style=official`）
- 使用 4 空格缩进（不得使用 Tab）
- 类名使用 PascalCase，函数和变量名使用 camelCase，常量使用 SCREAMING_SNAKE_CASE
- 优先使用 `val` 而非 `var`，优先使用不可变数据结构
- 使用 `when` 表达式代替冗长的 `if-else` 链

##### Compose

- Composable 函数使用 PascalCase 命名
- Composable 函数参数中，`modifier: Modifier = Modifier` 应有默认值，且放第一个可选参数位置
- 使用 `remember` 和 `derivedStateOf` 减少不必要的重组
- 复杂 Composable 应拆分为更小的可复用单元
- 状态提升：将状态提升到合理的最低公共祖先

##### 依赖注入

- 使用 Hilt 进行依赖注入
- 新模块的 DI 定义应添加至 `AppModule.kt` 或创建新的 Hilt Module
- Repository 层必须使用 `@Singleton` + `@Inject constructor`

##### 资源与字符串

- **所有面向用户的字符串必须在 `strings.xml` 中定义**，不得硬编码
- 新增字符串需在所有 7 种语言文件中同步添加（至少提供英文和简体中文翻译）
- 颜色资源定义在 `values/colors.xml` 中

##### 数据库迁移

如需修改 Room 数据库结构，必须：

1. 递增 `@Database(version = ...)` 版本号
2. 创建对应的 `Migration` 对象并添加到数据库构建中
3. 导出新的 Schema JSON 到 `app/schemas/` 目录

#### 提交信息规范

请遵循 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/) 规范：

```
<类型>[可选范围]: <描述>

[可选正文]

[可选脚注]
```

**常用类型：**

| 类型 | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档更新 |
| `style` | 代码格式（不影响功能） |
| `refactor` | 重构（非新功能、非 Bug 修复） |
| `perf` | 性能优化 |
| `test` | 测试相关 |
| `chore` | 构建/工具/依赖更新 |

**示例：**

```
feat(editor): 添加 Markdown 表格可视化选择器

实现 1-20 行 × 1-10 列的可视化网格预览弹窗，
选择后自动生成标准 Markdown 表格语法。

Closes #42
```

#### Pull Request 流程

1. **保持同步**：在开始工作前，确保你的分支与上游 `main` 分支同步：
   ```bash
   git checkout main
   git pull upstream main
   git checkout feature/你的功能名称
   git rebase main
   ```

2. **原子化提交**：每个提交应当是一个独立、完整、可测试的变更单元。

3. **自测**：提交前请确保 Debug 模式编译通过并运行正常：
   ```bash
   ./gradlew assembleDebug
   ```

4. **创建 PR**：推送到你的 Fork 后在 GitHub 上创建 Pull Request。

5. **PR 描述**：请包含以下信息：
   - 解决的问题或实现的功能
   - 实现方案简述
   - 关联的 Issue 编号（如有）
   - 测试方式说明
   - 截图或录屏（如有 UI 变更）

6. **代码审查**：维护者会审查你的代码，可能会提出修改建议。请保持积极沟通，及时回应。

---

## 许可证

Mdcito 采用 [AGPL-3.0 / 商业双协议](LICENSE) 授权。

**重要提示**：向本项目贡献代码，即表示你同意将你的贡献按 AGPL-3.0 许可进行授权。你确认你是所贡献内容的全部作者，拥有必要的权利，且贡献的内容可在本项目的许可下发布。

如果你希望对你的贡献适用不同的许可条款，请在提交前联系项目维护者。
