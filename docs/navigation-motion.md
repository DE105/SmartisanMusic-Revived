# 统一导航与专辑动效

更新日期：2026-09-07。按用户要求，层级页面采用普通返回，顶部标题参考锤子便签的分阶段动画。界面、图标、配色和数据语义继续使用音乐现有实现；验证由主机自动检查与用户实测分别完成。

## 便签参考

参考为用户提供的 Notes 7.3.1（versionCode 29）及约 4.84 秒操作录屏。APK SHA-256：`84544e1756f8f0f34a37ec9ed3eddfe92530e4afd442365eac76041652613fbc`。

`IActionBar.List2Detail` / `Detail2List` 与 `makeAlphaAnimation` / `makeButtonSlideAnim` 可核对以下时序：

| 阶段 | 进入详情 | 返回上层 |
| --- | --- | --- |
| 旧标题与普通图标 | 0–150ms 淡出 | 0–150ms 减速淡出 |
| 新标题与普通图标 | 150–300ms 淡入 | 150–300ms 淡入 |
| 返回箭头 | 100ms 后开始，150ms 淡入、200ms 从右侧移入 | 150ms 内淡出，同时用 300ms 向右退 |
| 页面主体 | 300ms 横移 | 300ms 横移 |

便签使用按钮自身宽度作为移动距离，音乐以现有图标及边距换算对应槽位距离。录屏可观察到标题背景固定、图标分阶段出现消失。这里独立实现 Compose 状态与时序，没有复制便签的 Java/XML、图片或私有接口；APK、反编译输出和用户录屏均未加入仓库。

## 复用

- [SmartisanNavigationMotion](../app/src/main/java/com/smartisan/music/ui/navigation/SmartisanNavigationMotion.kt) 统一持有页面位置、父标题透明度、详情标题透明度、返回箭头透明度与位移。每条动画可取消并从当前值继续，时序使用 Compose 帧时钟与系统动画倍率。
- [SmartisanPageStack](../app/src/main/java/com/smartisan/music/ui/navigation/SmartisanPageStack.kt) 接收主/子页的标题和内容槽位；固定标题区与内容区共享同一个动效状态。原页面中的内置标题由局部配置隐藏，避免重复标题与 Insets。
- “更多 → 设置”属于整页覆盖，使用 `SmartisanFullScreenTransition`：标题和内容一起从底部进入，关闭时一起向下退出。该组件隔离标题槽位与层级动效上下文，完整退出后才释放内容。
- “更多 → 风格/文件夹”等保留主壳的横向层级切换，通过 `PageStackTransition(projectTitles = true)` 把页面已有标题放入固定顶层槽位。页面原处保留等高占位，只有内容横移；标题不是截图，也没有第二份页面或业务状态。设置全屏展示期间仅暂停固定槽位的绘制，保留标题注册，在页面原处显示标题；播放列表添加模式使用自身标题。
- 设置内部的二级页继续共用固定的 SettingsTitleStack，使用便签式标题过渡。设置目的地状态由 MorePage 持有，SettingsPage 消费同一状态，不复制设置存储。
- 设置首页与二级页的标题阴影由 SettingsTitleStack 在转场裁剪区外统一绘制，标题层高于正文；阴影不随标题槽位淡出，也不挤占正文布局。
- 已经分离标题和内容的专辑、艺术家、文件夹、流派、播放列表继续使用 `TitleBarTransition` 和 `PageStackTransition`，二者复用上述时序。嵌套标题继承父级动效状态；全透明图标不参与点击，过渡中不会接受重复操作。
- 系统返回使用 AndroidX `BackHandler`；它和按钮修改相同页面状态。已移除所有预测进度、退出消费标记及跨页面透传。Manifest 的 `enableOnBackInvokedCallback=false` 关闭系统预测性动画。设置、搜索、播放的整页覆盖使用纵向收起，不混入层级返回的手势进度。

新层级页可直接提供 `secondaryKey`、`primaryTitle`、`secondaryTitle`、`primaryContent`、`secondaryContent` 五个槽位接入 `SmartisanPageStack`；页面拥有导航状态，动效组件只保留退场画面，不负责业务请求。

## 专辑切换

对照迁移前提交 `acbf63af^` 中的专辑与艺术家切换代码，恢复不对称节奏：列表变网格时每项 300ms 淡入、间隔约 40ms；网格变列表时封面约 150ms 缩小移动、按项约 10ms 错开。使用现有 Compose `SharedTransitionLayout`、`sharedBounds` 与 `SharedContentConfig`，按稳定专辑 ID 匹配，起止位置由实际布局提供。未匹配到目标可见行的封面正常淡出，避免猜测屏外坐标。

布局切换前按稳定条目定位滚动锚点；动画中途反向保留目标的现有测量，不重复跳动滚动位置。共享边界只在回列表时启用，正在运行的过渡可以连续反向。切换期暂停列表滚动和条目动作；离页时移除动效内容。没有引入 View/XML 布局或新依赖。

专辑首次入场由 `AlbumCollectionEntrance` 的等待、播放、完成三个阶段管理：页面可见且非空首屏条目已完成布局后才启动。横铺按 300ms/项、40ms 间隔依次展开，列表按列表入场节奏显现；后台预热和空数据不会消耗这次入场。普通切回及元数据更新不重播；中途离开或切换布局则结束初次入场，避免两套动画叠加。

## 转场边界修正

用户后续录屏显示“更多 → 风格/文件夹”的标题随整页横移；文字反馈明确要求设置入口是自下而上的全屏转场。现区分“整页覆盖”和“标题固定的层级切换”，不再把固定标题的横向模板套到设置入口。

## 设置退出闪动修复

后续录屏逐帧确认：退出尾帧中“更多”标题短暂空白，底部播放条重新播放入场。原因是设置结束时固定标题重新注册，以及底栏条件分支重新创建了播放条动画状态。

现在固定标题注册贯穿设置展示和退出，只切换原处/顶层绘制；标题恢复无需再等一次挂载副作用。底栏采用保留组合、暂停放置的方式隐藏，隐藏时不绘制、不接受输入，也不暴露子级无障碍节点；恢复时使用原播放条状态，不再重复入场。没有用额外延时掩盖尾帧问题。新增用例验证标题注册身份和底栏子组件生命周期在反复显隐中保持不变。

## 验证

艺术家一级 Tab 的列表取消逐项入场，切入直接显示内容。共用列表入场组件的初始透明度在创建状态时确定，不再先显示、再通过 LaunchedEffect 重置为透明；新增首帧及首次激活的回归用例。没有额外遮罩、延时或针对设备的判断。

统一转场与设置退出修复阶段执行 `./gradlew testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest assembleRelease` 通过。171 项单元测试零失败；设备测试 APK 包含 66 项用例，均仅完成构建、本轮未运行。Lint 为 0 错误、63 条警告、1 条提示，`git diff --check` 与文档相对链接检查通过。新增的 Manifest 提示指出关闭预测性动画的属性只在 API 33+ 生效，低版本忽略该属性，继续使用普通返回。

新增边界用例检查全屏标题与正文的共同纵向位移、层级标题固定且正文横移、以及全屏组件不注册到外层标题槽位。设备用例覆盖标题时序、箭头位移、动画反向、零动画倍率、按钮/系统返回、固定标题槽位，以及专辑快速切换、滚动定位和切换期间数据移除。只使用合成内容，用户负责真机安装及手感验收；本轮已停止模拟器，没有执行或声称通过设备用例。

## 官方 API

2026-09-07 查阅：

- [共享元素与边界](https://developer.android.com/develop/ui/compose/animation/shared-elements) 及 [定制共享过渡](https://developer.android.com/develop/ui/compose/animation/shared-elements/customize)：匹配键、边界缩放、动态启用与中断。
- [普通返回与关闭预测性动画](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture#opt-out)：关闭系统预测性动画后，AndroidX OnBackPressedCallback / BackHandler 继续工作。

艺术家闪动修复后执行 `testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest` 通过，171 项单元测试零失败；设备测试 APK 共 68 项用例，本轮未运行。新增首帧用例覆盖直接挂载和已挂载列表首次激活。

专辑加载入场按横铺/竖向分别实现后，同组主机检查通过，171 项单元测试零失败，设备测试 APK 共 71 项用例（未运行）。新增用例覆盖等待数据不消耗入场、横铺顺序展开、列表独立节奏和中断后恢复可见。

## 第二阶段提交记录

2026-09-07，用户确认 XML → Compose 第二阶段基本完成。本阶段收尾覆盖统一导航、全屏与层级转场边界、专辑共享边界和加载入场、列表拖拽、控件动画、标题阴影及首帧闪动修复。提交前对最终代码执行 `testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest assembleRelease` 全部通过：171 项单元测试零失败，Lint 0 错误、63 条警告、1 条提示；71 项设备用例完成编译，未作为本阶段自动实测通过数量。
