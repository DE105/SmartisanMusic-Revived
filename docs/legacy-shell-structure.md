# Legacy Shell 结构约定

本工程当前仍是单 `:app` 模块。按照 Android 官方推荐架构，代码先保持 UI 层、数据层和播放服务层分离；legacy View 壳只负责渲染状态和上抛事件，不承载播放、扫描、收藏、权限或 Media3 业务链路。

参考资料：

- Android app architecture: https://developer.android.com/topic/architecture
- UI layer: https://developer.android.com/topic/architecture/ui-layer
- Android modularization: https://developer.android.com/topic/modularization
- Common modularization patterns: https://developer.android.com/topic/modularization/patterns

## 当前包边界

- `data/*`：Repository、Room、持久化配置和业务数据源。
- `playback/*`：Media3、播放服务、本地媒体库、播放队列、封面和歌词解析。
- `ui/*`：屏幕、legacy View 壳、Compose 桥接、弹窗和控件。
- `ui/shell`：Smartisan Music 8.1.0 legacy 主壳入口、页面栈、跨页面覆盖层和 shell 级桥接。
- `ui/shell/songs`：歌曲页、歌曲 adapter、歌曲排序与分组模型。
- `ui/shell/tabs`：底部 Tab 和 Tab 内容调度。
- `ui/shell/titlebar`：Smartisan 标题栏桥接、主标题栏、搜索详情标题栏。
- `ui/shell/search`：搜索覆盖层与搜索二级页。
- `ui/shell/playback`：底部播放条和外部音频启动项到 MediaItem 的 UI 桥接。
- `ui/shell/library`：legacy shell 读取本地 library 媒体列表的 UI state。
- `ui/shell/dialogs`：shell 级确认弹窗。
- `ui/shell/LegacyPlaylist*.kt`：播放列表入口、标题栏、根列表、歌曲列表和弹窗，按文件前缀归组。
- `ui/shell/LegacyArtist*.kt`：艺术家入口、标题栈、概览、二级页和专辑切换动画，按文件前缀归组。
- `ui/shell/LegacyMore*.kt`：更多页入口和文件夹页，按文件前缀归组。

## 拆分原则

- 页面入口文件只保留状态编排、页面栈和事件转发；View 创建、adapter、排序模型、弹窗和标题栏各自独立。
- 与具体页面强绑定的类放在对应页面包；跨页面复用的 legacy 控件或转场再放回 `ui/shell` 或专门子包。
- 新拆出的类型默认使用 `private`；只有跨文件调用时才提升为 `internal`。
- 不为拆文件引入新架构，不把播放、扫描、收藏、权限或 Media3 逻辑搬进 legacy UI。
- 单个 legacy UI 文件超过约 800 行时应继续拆分；超过 1200 行时不再追加新职责，优先拆 adapter、dialog、titlebar、sort/model、animation。

## 后续拆分队列

当前已完成 `LegacyPortMainShell.kt`、播放列表页、艺术家页和更多页的首轮大文件拆分。后续若继续整理，应按体量和风险优先处理：

1. `ui/playback/PlaybackScreen.kt`：当前仍是项目内最大 Kotlin 文件，应按播放页控件、搓碟控制、状态映射和弹窗继续拆分。
2. `LegacyPlaylistTrackPages.kt`：继续拆出 detail/add 子页、track adapter、sort/model。
3. `LegacyMoreFolderPage.kt`：继续拆出 folder root/detail、folder adapter、folder sort/model。
4. `LegacyPortAlbumPage.kt`：拆出 album overview、list/grid adapter、view-switch animator。
5. `GlobalSearchScreen.kt`：搜索 UI 后续增长时拆出结果分组、详情入口和 adapter/state 映射。
