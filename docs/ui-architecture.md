# UI 架构

更新日期：2026-09-07。应用 UI 已完整使用 Jetpack Compose；当前视觉基线来自迁移前经过校准的实现。工程保持单 `:app` 模块，源码根包为 `com.smartisan.music`，applicationId 为 `app.smartisanmusic.revived`。迁移范围、验证状态与待验收项见 [Compose 迁移记录](compose-migration.md)。

## 状态与系统边界

[MainActivity](../app/src/main/java/com/smartisan/music/MainActivity.kt) 负责窗口、权限、外部音频入口和 Compose 宿主。[MusicAppShell](../app/src/main/java/com/smartisan/music/ui/shell/MusicAppShell.kt) 协调目的地、编辑态、页面栈及覆盖层。具体页面消费已有媒体与设置状态，通过回调或 controller 发送用户意图。

| 所有者 | 职责 | UI 的接入方式 |
| --- | --- | --- |
| [playback](../app/src/main/java/com/smartisan/music/playback/) | Media3 服务、队列、播放位置、会话恢复、音频与歌词解析 | 使用现有 Player/controller 和 session command；页面不创建第二个播放器 |
| [data](../app/src/main/java/com/smartisan/music/data/) | Room、DataStore、资料库索引、收藏、播放列表和设置 | 订阅已有 Repository / Flow，写入仍经原业务入口 |
| [launcher](../app/src/main/java/com/smartisan/music/launcher/) | 应用图标 alias 切换 | 以系统组件状态确认选择，不另存一份图标状态 |
| [ui/library](../app/src/main/java/com/smartisan/music/ui/library/) | 资料库 UI state、共用列表和专辑展示 | 将已有媒体映射为页面数据，不维护第二套媒体索引 |
| [ui/shell](../app/src/main/java/com/smartisan/music/ui/shell/) | 全局页面编排、Tab、标题栈、播放条和跨页面覆盖层 | 持有导航与临时编辑状态，转发页面事件 |

Room schema、DataStore key、稳定媒体 ID、队列顺序、当前项及恢复协议不因 UI 迁移而改变。删除媒体继续通过系统授权协调器执行；确认界面只表达用户意图，授权结果仍走原有链路。

## 页面与组件

页面按功能归组，`shell` 保留跨页面编排。迁移中有 41 个文件从主壳目录归入功能包；文件、类型和函数采用当前职责命名，移除 `Legacy` / `LegacyPort` 前缀。

| 包 | 当前入口与职责 |
| --- | --- |
| [ui/songs](../app/src/main/java/com/smartisan/music/ui/songs/) | `SongsPage`、歌曲行、排序分组、滑动删除、可拉出网格的字母快捷栏 |
| [ui/album](../app/src/main/java/com/smartisan/music/ui/album/) | `AlbumPage`、`AlbumDetailPage` 与专辑模型 |
| [ui/artist](../app/src/main/java/com/smartisan/music/ui/artist/) | 艺术家概览、专辑/全部歌曲子页、标题栈与名称拆分 |
| [ui/folder](../app/src/main/java/com/smartisan/music/ui/folder/)、[ui/genre](../app/src/main/java/com/smartisan/music/ui/genre/) | 文件夹排除、刷新、删除与歌曲详情；流派浏览与详情 |
| [ui/loved](../app/src/main/java/com/smartisan/music/ui/loved/)、[ui/playlist](../app/src/main/java/com/smartisan/music/ui/playlist/) | 收藏、播放列表根页、歌曲编辑、排序和选择弹层 |
| [ui/search](../app/src/main/java/com/smartisan/music/ui/search/) | 全局搜索、结果分组、历史和详情覆盖层 |
| [ui/more](../app/src/main/java/com/smartisan/music/ui/more/)、[ui/settings](../app/src/main/java/com/smartisan/music/ui/settings/) | 动态溢出入口、设置及图标/主题选择 |
| [ui/playback](../app/src/main/java/com/smartisan/music/ui/playback/) | 播放页、黑胶/歌词舞台、唱针与搓碟、播放队列、音量、音效和睡眠定时 |
| [ui/artwork](../app/src/main/java/com/smartisan/music/ui/artwork/) | `AlbumArtworkLoader`、共享 LRU、请求合并及封面浏览转场 |
| [ui/components](../app/src/main/java/com/smartisan/music/ui/components/) | Smartisan 标题、资源 Painter、弹层、开关、评分、滚动条、滑选和拖拽 |
| [ui/navigation](../app/src/main/java/com/smartisan/music/ui/navigation/)、[ui/theme](../app/src/main/java/com/smartisan/music/ui/theme/) | 目的地/导航配置模型与主题接入 |

[PageStackTransition](../app/src/main/java/com/smartisan/music/ui/shell/PageStackTransition.kt) 与 [PlaybackTransition](../app/src/main/java/com/smartisan/music/ui/shell/PlaybackTransition.kt) 分别承载普通层级页面平移与播放页整页覆盖；不再跟随返回手势进度。[tabs](../app/src/main/java/com/smartisan/music/ui/shell/tabs/) 负责目的地调度和导航编辑；[titlebar](../app/src/main/java/com/smartisan/music/ui/shell/titlebar/) 负责主标题与标题栈；[shell/playback](../app/src/main/java/com/smartisan/music/ui/shell/playback/) 保留底部播放条和外部音频到 MediaItem 的桥接。这里没有引入另一套通用导航状态源。

## 图形、交互与生命周期

2026-09-07 调整：专辑与艺术家采用 `SharedTransitionLayout` / `sharedBounds`，网格封面依次显现，切回列表时以稳定条目 ID 匹配封面并缩放落位。浏览锚点在目标布局显示前定位，中途反向沿已测量边界继续。播放列表与队列拖拽的让位、取消和落位继续使用 Compose 动画，数据顺序仍由原回调提交。

布局、列表、弹层和交互节点由 Compose 构建。原有 `res/layout`、UI View 子类、adapter 和 framework shim 已移除；`drawable` / `color` XML 是图形资源，继续保留。[attrs.xml](../app/src/main/res/values/attrs.xml) 中的 `SmartisanScrollbar` 仅声明系统滚动条主题属性，由 AAPT 生成正确的索引顺序，不承载 View 布局。页面使用自定义 Smartisan 组件呈现现有视觉。

[SmartisanDrawablePainter](../app/src/main/java/com/smartisan/music/ui/components/SmartisanDrawablePainter.kt) 保留 selector、ColorStateList、9-patch、配置变体及 Drawable 回调生命周期。`smartisanTextSize` 按资源解析后的像素转换字号，正文明确设置 font padding。[SmartisanTitleText](../app/src/main/java/com/smartisan/music/ui/components/SmartisanTitleText.kt) 使用公开 TextPaint 和文字布局 API 维持伪粗体及基线。

背景统一使用 [smartisanPainterBackground](../app/src/main/java/com/smartisan/music/ui/components/SmartisanPainterBackground.kt) 在 `drawBehind` 中按已测量尺寸绘制。`Modifier.paint` 即使关闭固有尺寸仍会改变父约束，不能用于自适应高度的底栏、列表或弹层背景。装饰阴影显式声明资源高度，不通过图片宽度缩放决定容器高度。

Compose 绘制阶段仍可调用 Android 的公开 `Canvas` / `Drawable` / `TextPaint`；[睡眠定时滚轮](../app/src/main/java/com/smartisan/music/ui/playback/PlaybackSleepTimerPicker.kt) 使用 Compose `AnimationState`、Android 样条衰减和吸附动画，不再依赖 `Scroller` 的墙钟时间。系统窗口、点击音、触觉及权限桥接仍使用适用的公开平台 API。UI 测试中的原生标题栏和 Drawable View 只用于对照，不进入生产页面。

普通层级返回统一使用 [SmartisanNavigationMotion](../app/src/main/java/com/smartisan/music/ui/navigation/SmartisanNavigationMotion.kt)，同一套 300ms 时序驱动页面和标题各槽位。[SmartisanPageStack](../app/src/main/java/com/smartisan/music/ui/navigation/SmartisanPageStack.kt) 提供固定标题区与整宽内容区，供分离标题的层级页复用；已有标题与内容分离的专辑、艺术家、文件夹、流派和播放列表分别复用 `TitleBarTransition` 与 `PageStackTransition`。标题图标使用继承的局部动效状态，嵌套标题不会覆盖父级淡出。页面业务与存储不归动效组件所有。

AndroidX `BackHandler` 仅处理已确认的返回，与标题返回按钮汇入同一页面状态；Manifest 关闭系统预测性返回动画，生产 UI 已移除预测进度、消费标记与相关参数。设置/播放/搜索整页覆盖层使用纵向展开收起，区别于层级页的横向返回。复用方式、便签参考及测试边界见 [统一导航动效](navigation-motion.md)。

列表使用稳定项键和独立滚动状态。按压、编辑、滑删、滑选和拖拽各有明确手势所有者；边缘自动滚动跳过不可选择的标题/页脚。隐藏页面不应接受点击或暴露重复的可访问节点。动画和后台工作绑定 Composition 生命周期，不能为离开的页面留下运行中的任务或 Drawable 回调。

封面加载沿用原 LRU：已有缓存可先显示，同一加载器内相同请求合并，媒体解析和解码在 IO 线程执行；离开页面取消所属任务。列表、队列和转场复用加载入口，不复制解码流程。主题资源仍通过 `values-night` / `drawable-night` 提供，[夜间资源生成脚本](../tools/generate_night_drawables.py) 维护对应输入和输出。

专辑封面预览由 `GalleryArtworkMotion` 驱动直线缩放、轻微回弹和背景透明度；几何计算保留原始图片比例与缩略图裁剪。`AlbumArtworkBrowserHost` 在应用同一窗口顶层绘制，来源页只提交请求并持有来源标识；移除来源页时取消对应预览。缩回完成时先恢复缩略图、再移除预览，避免跨窗口交接出现空帧。参数依据和验证边界见 [封面预览动画](gallery-artwork-motion.md)。

## 后续维护规则

- 新页面放入最窄的功能包；跨页面共用的视觉或手势组件才放入 `ui/components`。避免将页面细节再次堆回 `MusicAppShell`。
- UI 只保留渲染、临时交互和事件协调。播放、媒体扫描、收藏、设置和持久化继续由现有业务层拥有。
- 新类型默认 `private`，有跨文件调用再使用 `internal`；不为拆文件引入新的模块或状态源。
- 视觉调整以已校准实现、既有资源和设备反馈为依据，保留尺寸、时序、按压、触摸和字体语义。原版没有的能力延续相邻页面的视觉语言。
- 删除资源时同时检查 main/test/androidTest、Manifest、全部配置下的 XML 依赖及生成脚本；不要只根据文件名或单次 Lint 报告判断。
- 验证按风险选择；自动构建与设备验收分别记录。完整清单见 [迁移记录](compose-migration.md#验证与待验收)。

设置入口通过 `SmartisanFullScreenTransition` 让标题与正文整体移动，内部二级页再使用固定标题转场。主壳的“更多”溢出目的地通过 `ProjectedNavigationTitle` 注册已有标题，保留页面原处高度、在主壳顶层绘制同一个实时标题槽位；不复制页面、状态或资源解码。渲染槽位独立重组，注册以所有者标识清理，避免退场页面清掉新页面标题。全屏设置与播放列表添加模式停用外层固定标题。

设置退出恢复主壳时，标题槽位保持注册，仅切换原处/顶层渲染。底栏通过 `retainedChromeVisibility` 暂停放置与子级无障碍暴露，保留播放条的组合和动画状态；避免尾帧重新挂载造成标题空白与播放条重复入场。
