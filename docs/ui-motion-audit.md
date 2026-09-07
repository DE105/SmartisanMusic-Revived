# 动效核查记录

日期：2026-09-06；2026-09-07 按最新反馈修订。用户授权在保留界面和操作语义的前提下采用自然、可中断的 Compose 动效，不要求逐帧复制旧 View 动画。本次依据当前源码、迁移前实现的既有语义与官方 API 核查，未读取原 APK 或 reverse 材料。

## 确认并修复

| 范围 | 问题与处理 |
| --- | --- |
| 专辑/艺术家列表与网格 | 恢复网格逐项显现和封面缩小落入列表；共享边界使用稳定 ID 和 Compose 实际测量，中断保留当前边界，移除整页淡出淡入。 |
| 播放列表/队列拖拽 | 让位采用无回弹弹簧，落位后立即清除旧位移，取消时同步归位；保留小数指针位置，失效状态不再卡在 settling。 |
| 普通返回 | 9 月 7 日依用户要求移除预测性返回及全部进度参数；BackHandler 和标题按钮修改相同页面状态，再播放统一便签风格转场。 |
| 横向页面 | 同一页面退出过程中重开不再 snapTo(0) 跳回屏外；位移在 graphicsLayer 中读取，减少逐帧重组与整数取整。 |
| 设置入口与层级边界 | 进入设置时标题/正文整体自下而上，退出向下；只有内部层级及更多溢出目的地固定标题、正文横移。设置底栏避让跟随内容实际挂载状态。 |
| 标题与按钮 | 使用便签参考的分阶段图标退场、返回箭头横移与延迟入场；完全透明的槽位不参与点击，过渡期间动作不可重复提交。 |
| 底部弹层 | 遮罩与面板分别动画，避免父层透明度再乘到面板上造成意外变透明；保留内容到退出结束。 |
| 资料库列表 | 每个挂载页面只做一次入场；元数据或顺序更新不再重放整列淡入，离开时结束未完成入场。 |
| 开关 | 连续点击以尚未提交的目标值反向，避免 20ms 回调窗口内重复朝同一侧运动。 |
| 唱针 | 切歌时不再跳过目标角度更新，暂停切歌也能落到新位置。 |
| 唱盘 | 从当前角度逐帧累加，不再用旧锚点覆盖手动旋转；角度保持有界，并在生命周期停止时暂停自动帧循环。 |
| 睡眠滚轮 | 以 AnimationState 和 Android 样条衰减替换 Scroller，惯性、吸附与取消统一使用 Compose 帧时钟；缩短落位拖尾。 |
| 文件夹编辑 | 隐藏行展开与编辑控件同时开始动画，移除人为 200ms 等待；资料库排除的提交语义不变。 |

## 已读保留

搜索覆盖层、播放页外层展开使用 AnimatedVisibility，底部播放条和封面浏览器保留可取消的 Animatable；字母快捷栏、滑删、滚动条、歌词跟随保留现有手势/任务取消路径。本轮未发现需要改写这些路径的明确依据，不以静态阅读声称真机表现已经通过。播放入口的分段入场和文件夹眼睛资源动画继续保留，待设备反馈再判断节奏。

未改变 Media3、Room、DataStore、队列提交协议、文件删除、收藏或播放列表持久化。没有升级依赖，也没有将通用导航框架或另一套播放器引入项目。

## 验证边界

9 月 6 日阶段执行 `./gradlew testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest assembleRelease` 通过；171 项单元测试零失败，设备测试 APK 包含 58 项用例。Lint 为 0 错误、62 条警告、2 条提示，`git diff --check` 通过。58 项设备用例当时未执行。9 月 7 日统一导航与专辑共享边界的最新检查见 [统一导航动效](navigation-motion.md)。

设备回归覆盖专辑快速反向、拖拽取消、页面重开、0 倍/3 倍动画下的退出生命周期、普通返回与统一标题分段时序、透明标题层、列表入场更新、开关快速反向、唱盘角度和滚轮中断，以及弹层面板像素。用例使用合成数据和测试宿主 Activity。

本轮 ADB 未连接设备，新增及修改的 instrumentation 只能确认编译结果，不能沿用此前 37 项真机通过记录作为此次结论。仍需实际回归系统返回与标题返回、连续开关页面/弹窗、暂停切歌、搓碟后恢复、滚轮高速滑动与打断、前后台切换、不同刷新率和系统动画倍率；未测量帧耗时，不声称已达到某个帧率。

## 官方依据

以下资料于 2026-09-06 查阅，API 同时核对当前缓存中的 AndroidX Activity 1.13.0 和 Compose 1.11.2 源码：

- [手动访问预测性返回进度](https://developer.android.com/develop/ui/compose/system/predictive-back-progress)：收集进度、处理取消和完成。
- [动画组件与修饰符](https://developer.android.com/develop/ui/compose/animation/composables-modifiers)：退出后移除时机及子层 animateEnterExit。
- [Compose 动画指南](https://developer.android.com/develop/ui/compose/animation/quick-guide)：状态驱动、可中断动画与绘制阶段读取。
- [AnimationState](https://developer.android.com/reference/kotlin/androidx/compose/animation/core/AnimationState)：衰减、吸附与动画作用域中的取消。
- [生命周期与协程](https://developer.android.com/topic/libraries/architecture/coroutines#repeatonlifecycle)：唱盘自动帧循环按 STARTED 状态启动和取消。
