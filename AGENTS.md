# Repository Guidelines

## 项目背景与复刻动机

锤子科技已经退出历史舞台，原版音乐播放器也停留在旧版 Android 时代。这套应用曾经承载了 Smartisan OS 最鲜明的一段审美表达，黑胶唱片的光影层次、唱针交互的机械阻尼感，以及近乎偏执的细节打磨，至今依然拥有无与伦比的美感。这个项目选择复刻它，正是因为团队始终怀念这套 UI，并希望用 Jetpack Compose 与 Media3 在现代设备上把它重新带回日常使用场景。贡献者在实现功能时应优先维护 1:1 像素级复刻目标，持续贴近原版视觉、动效和交互质感。

## 开发工作流程

开始写代码前，先完成这套顺序化检查：

1. 阅读 `reverse/音乐_6.8.0-逆向整理与复刻规划.md`，明确当前页面、资源、交互和阶段目标。
2. 按需查看 `reverse/decompiled/音乐_6.8.0/resources/` 下的 `layout/`、`drawable-xxhdpi-v4/`、`values-xxhdpi-v4/` 与 `AndroidManifest.xml`，确认原版结构、尺寸和资源命名。
3. 打开 Android 官方最新文档，再核对当前实现方案后开始编码。Android 相关文档统一以 `developer.android.com` 为准，常用入口包括 `https://developer.android.com/compose`、`https://developer.android.com/develop/ui/compose/navigation`、`https://developer.android.com/media/media3`、`https://developer.android.com/training/permissions/requesting`。Kotlin 版本以 `https://kotlinlang.org/docs/releases.html` 为准，Compose 版本以 `https://developer.android.com/develop/ui/compose/bom/bom-mapping` 为准，最终可解析版本再用官方仓库元数据确认。
4. 文档结论确认后再修改代码，避免基于过期 API、旧博客或记忆直接实现。
5. 完成开发后运行对应的 Gradle 构建与测试命令，再提交变更。

## 项目结构与模块组织

按照 Android 官方最新架构与模块化文档，项目结构以分层清晰、单一职责、高内聚低耦合为目标。当前仓库保持单模块 `app`，目录规划按层和功能组织，后续在规模增长后平滑升级为多模块。

- `app/` 作为应用入口模块，负责 `AndroidManifest.xml`、应用启动、导航图装配、依赖注入装配和最终打包。
- `app/src/main/java/com/smartisanos/music/ui/<feature>/` 放页面级 UI，按功能拆分 `PlaylistScreen`、`PlaybackScreen`、`SongsScreen` 这类界面，以及对应的 `*UiState`、`*ViewModel` 和局部 Composable。
- `app/src/main/java/com/smartisanos/music/ui/components/`、`ui/theme/`、`ui/navigation/` 放共享 UI 组件、主题、导航宿主和路由定义。
- `app/src/main/java/com/smartisanos/music/data/<domain>/` 作为数据层目标位置，承载 `*Repository`、`*LocalDataSource`、`*RemoteDataSource`、模型与映射逻辑。仓库负责对外暴露应用数据，并作为单一数据来源。
- `app/src/main/java/com/smartisanos/music/domain/<feature>/` 作为可选领域层，承载复用度高或复杂度高的 `*UseCase`。
- `app/src/main/java/com/smartisanos/music/core/` 作为共享基础层目标位置，放设计系统、媒体契约、通用模型、工具扩展和跨功能公共能力。
- 依赖方向保持 `ui -> domain -> data`；简单场景可由 `ui -> data` 直接完成。UI 通过仓库或 UseCase 读取数据，数据库、网络、MediaStore、权限与 Media3 系统交互统一收敛到数据层或平台封装层。
- 类命名遵循官方建议：UI 状态使用 `功能 + UiState`，仓库使用 `数据类型 + Repository`，数据源使用 `数据类型 + LocalDataSource` 或 `数据类型 + RemoteDataSource`。例如 `PlaybackUiState`、`AudioRepository`、`AudioLocalDataSource`。
- 当功能数量、构建时长或协作人数提升后，按功能和共享能力拆成 `:app`、`:core:designsystem`、`:core:model`、`:core:media`、`:data:library`、`:feature:playlist`、`:feature:album`、`:feature:artist`、`:feature:songs`、`:feature:playback`。纯 Kotlin 共享逻辑优先放 Kotlin library module，带资源或 Manifest 的共享能力放 Android library module。

## 代码风格与命名规范

使用 Kotlin 官方风格和 4 空格缩进，交给 Android Studio 格式化并整理 import。类、Composable、Screen、Service 使用 `PascalCase`，函数与属性使用 `camelCase`，资源文件使用小写下划线命名，例如 `tabbar_album_selector.xml`。新增页面、状态与控制器优先按功能归档到对应包，例如 `ui/playback/`、`playback/`。当前仓库没有 `ktlint` 或 `detekt` 配置，格式一致性依赖 IDE 与代码评审。

## 测试规范

纯逻辑优先写在 `app/src/test`，播放器交互、权限流程、导航状态和 Compose 界面行为放在 `app/src/androidTest`。测试类名使用 `*Test` 或 `*InstrumentedTest`，与被测包结构保持一致。仓库当前没有强制覆盖率阈值，新功能和行为修复应补充最小可验证测试。

## 配置与安全提示

`local.properties` 只保留本机配置，避免提交 SDK 路径。当前应用配置为 `minSdk 31`、`targetSdk 36`。依赖版本默认跟随官方最新稳定版，优先保证 Kotlin 与 Compose 处于最新版本，再同步检查 `gradle/libs.versions.toml` 与 `app/build.gradle.kts`。AndroidX 与 Compose 的具体版本以 Google Maven 元数据为最终发布依据，Kotlin 版本以 JetBrains 官方发布页与 Maven 元数据共同确认。
