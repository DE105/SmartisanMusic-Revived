# 列表选中样式

日期：2026-09-07。用户希望补回蓝色选中反馈，并授权根据当前界面判断适用范围。

## 依据与范围

现有 `listview_selector.xml` 中的蓝色位图 `list_item_bgwithoutphoto_down` 只对应 pressed/focused。迁移前 Git 中的 `LegacySongsAdapter` 又把多选交给独立勾选框，因此不能据此认定原版每一种列表都有常驻蓝底。本次按当前 UI 语义补充统一的多选行高亮，不声称逐项复现原 APK 的所有状态。

| 场景 | 处理 |
| --- | --- |
| 歌曲、收藏、播放列表及其歌曲、专辑竖向列表、文件夹目录与歌曲多选 | 勾选框之外增加整行浅蓝底；深色模式使用暗蓝底。只有编辑且已选中时启用。 |
| 普通浏览行的按下与焦点 | 保留现有深蓝按下/焦点资源；不新增点击后常驻的选中状态。 |
| 专辑网格 | 保留现有勾选遮罩，不给封面本身染色。 |
| 当前播放歌曲、播放队列 | 保留播放图标、文字或专用队列样式，不和批量选择混用。 |
| 主题、应用图标、开关等设置 | 保留单选标记、勾选和开关自身提示，不将所有设置行涂蓝。 |

## 实现

原有选择集合与回调保持不变。六处多选行把 `editMode && selected/checked` 传给 Painter 的 activated 状态，专辑列表通过共享 `LibrarySummaryRow` 传递。选择取消、退出编辑、行复用后，背景都从当前状态直接计算，不缓存一份“高亮项”。

`listview_selector` 的优先级为按下、焦点、多选、普通。多选色为日间 `#E5EEFF`、夜间 `#26384F`，放在专用资源文件中，保留原文字颜色和布局。深蓝按下背景仍是原有位图资源。

设备用例检查歌曲行与共享专辑行的实际背景像素：点选后蓝底持续、退出编辑后清除、当前播放不导致整行多选高亮，以及日间/夜间资源解析。用例使用合成数据；设备效果由用户实测。

`testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest assembleRelease` 全部通过：178 项单元测试零失败，Lint 0 错误、63 条警告、1 条提示。设备测试 APK 共 78 项用例，本轮未运行。`git diff --check` 通过。

## 普通点击反馈修复（2026-09-07）

用户在原版锤子手机确认：“更多”的目的地入口与歌曲列表普通点击也应有蓝色反馈。当前行已使用蓝色按压资源，但 `collectIsPressedAsState()` 会遗漏同一帧内结束的快速点击。因此列表行改为直接收集 Press/Release/Cancel：正常释放后按公开 `ViewConfiguration.getPressedStateDuration()` 保留短暂反馈，并通过帧时钟确保有绘制机会；滑动取消立即清除，新按压取消上一轮释放任务，离开组合时任务随生命周期取消。播放与导航回调仍即时执行。

接入范围为更多入口、歌曲、收藏、播放列表及其歌曲、文件夹目录和共享资料库汇总行。继续复用已有蓝色资源及图标/文字状态，不新增常驻的普通点击选中项。

官方资料查阅日期：2026-09-07：[Compose 交互与同帧状态合并](https://developer.android.com/develop/ui/compose/touch-input/user-interactions/handling-interactions)、[ViewConfiguration 按压反馈时长](https://developer.android.com/reference/android/view/ViewConfiguration#getPressedStateDuration())。

新增设备测试覆盖同帧点击可见及自动恢复、连续点击不会被旧释放任务清除、滑动取消立即清除。自动验证与真机验收分开：测试 APK 仅构建；仍需用户确认更多入口跳转时的蓝底、歌曲快点/长按/滚动取消及深浅色效果。

### 原版 APK 核对

同日经用户授权读取坚果 R1 上的音乐 7.2.0.3 APK 及其配套 VDEX，并与本地 8.1.0 基准交叉核对。`more_fragment_layout.xml` 与歌曲列表 `lv_tracks.xml` 使用 `listview_selector`；该 selector 的 pressed/focused 项指向 `list_item_bgwithoutphoto_down`，默认透明。`MoreFragment.onItemClick()` 只负责页面跳转，更多入口 adapter 没有保存常驻选中项；列表使用系统按压处理。8.1.0 的对应入口保持相同语义，歌曲行还直接使用该 selector 作为背景。这些证据确认本轮补的是普通点击反馈，不能把先前新增的多选浅蓝底当成原版普通点击样式。

原版正常页面已通过 ADB 查看，但未取得可用的按压过程截图或录像，不能据此声称已测得原机反馈时长。实现使用公开平台时长；复刻版本仍待真机验收。APK、提取代码及设备画面仅留临时目录，不进入提交。

本轮 `testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest` 通过，178 项单元测试零失败；补充快速点击实际蓝底像素断言后再次通过测试 APK 构建与 Lint。未执行设备测试，未安装、卸载或清除手机上的应用。

## 全局点击状态核查（2026-09-07）

用户要求扩展检查后，按所有 `clickable`、`combinedClickable`、`selectable`、`toggleable` 入口及自定义触摸入口核查，普通控件统一接入 `collectSmartisanPressedAsState()`。除快点漏显外，还补齐了原版按压文字与子图标的联动；松开后仍使用各页面已有语义色。核对使用 8.1.0 的 selector、布局与代码，R1 提取的 7.2.0.3 作为交叉参考；没有把新版平台能力改回 OEM 实现。

| 范围 | 核查与处理 |
| --- | --- |
| 歌曲、收藏、播放列表、文件夹、艺术家、流派、专辑列表及专辑内歌曲 | 蓝色行背景保留快点反馈；标题、副标题、时长按原版白色文字 selector 联动；播放标记、箭头、拖动图标及更多图标传递按压状态。对应 `item_track_list`、`item_saved_songs`、`artist_listview_items_text`、`playlist_normal_item` 等布局。 |
| 搜索结果、播放队列、加入播放列表选择器 | 复用各自已有蓝色背景；搜索文字、列表选择器文字与箭头同步反馈，队列保留其已有播放项和评分语义。对应 `search_result_item_layout`、`item_listview_playlist_picker` 和队列布局。 |
| 更多、设置、主题、应用图标、音效预设 | 分组行沿用原背景及文字 selector；补齐单选标记的 pressed/enabled 状态，不把普通按压当成配置选中。 |
| 新建播放列表、删除/重命名操作、专辑操作按钮、弹窗按钮、操作菜单、排序弹出菜单 | 沿用各自按压背景、图标与文字；新建入口图标及排序单选标记补传按压状态。 |
| 标题栏、底部 Tab、播放条按钮、播放页控制按钮 | 使用现有图标按压变体；循环、随机等直接使用当前模式图标的控件继续以模式状态反馈。 |
| 专辑网格、搜索历史标签、搜索输入框清除、搜索关闭、字母展开面板 | 分别保留封面遮罩、标签按压位图、图标变体与字母格样式，使快速点击可见。 |
| 排序分段标签、开关、滑块、评分、歌词/唱盘手势、拖拽与滑选 | 检查其独立状态流；排序在按下时切换激活项，其他控件已有连续手势或状态动画，不用短暂按钮反馈替代它们。 |
| 专辑大图入口、播放条整体入口、单张资源的清除图标、弹层遮罩与触摸拦截区域 | 保留现有页面打开、关闭或清除语义；未发现可直接沿用的独立按压资源时不新增蓝底。 |

共享实现位于 `SmartisanPressFeedback.kt`，源代码中已无直接用 `collectIsPressedAsState()` 驱动普通控件外观的遗漏。设备测试包括同帧 Press/Release、连续点击、取消、真实 LazyColumn 歌曲行的蓝底/白字与恢复，以及播放按钮使用自己的按压位图。深浅色均有歌曲行用例。测试 APK 构建只验证可编译，以上新增设备用例仍未执行。

待用户验收：各类页面快速点击与长按、滑动取消、快速连续点击、弹层关闭、深浅色切换，以及按压后播放状态/多选状态/设置标记正常恢复。

全局修复后再次运行 `testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest`，全部通过（178 项单元测试零失败）。`git diff --check` 通过；新增设备用例仅完成编译。
