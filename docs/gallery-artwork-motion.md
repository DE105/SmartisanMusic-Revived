# 专辑封面预览动画

日期：2026-09-07。用户录屏展示了音乐当前封面预览的弧线移动，并提供 Gallery 8.0.2 APK 作为目标效果参考。本次仅修改专辑详情中的封面放大/缩回动画，保留封面加载缓存、来源页面、背景点击关闭及系统返回行为。

## 参考依据

APK SHA-256：`3331f2ada226fc2ce1783bc883a59840fd885c054c2cf5dd81dea4d3f9d8876d`。

相册 `PageAnimation` 的 `GOTO_PHOTO_VIEW` 使用 300ms 图片动画与黑色背景淡入；对应的图片动画先在约 175ms 内以 cubic-out 曲线达到 1.1 倍插值进度，再用 125ms 线性收稳到 1。初始中心裁剪在前 50ms 内解除。返回相册的 `playPhotoViewToSmallAnim` 使用 200ms cubic-out 缩回，并让背景淡出。

APK 中的私有框架资源使 apktool 无法完整解码；上述参数来自可核对的 dex 动画代码，不依赖缺失资源推测。没有复制相册代码或素材，也没有引入 Smartisan 私有接口。参考 APK 和录屏保留在用户提供的位置，反编译输出只放临时目录，这些文件都不进入仓库。

## 实现

- 移除旧的贝塞尔弧线和 420ms 打开时长，改用相册式直线缩放及轻微回弹。
- `GalleryArtworkMotion` 用 Compose 动画分别驱动几何进度、背景透明度和裁剪释放；中途关闭从当前值继续，不跳回初始位置。
- `galleryArtworkFrame` 计算缩略图的中心裁剪起点和预览窗口中的等比适配终点，横图、竖图均不拉伸。
- 起点从真正绘制图片的节点测量，排除封面外框留白。来源和顶层预览都读取屏幕坐标，并减去绘制区域的实际原点，不混用坐标，也不猜测缺失起点。
- 预览完成布局后才开始移动和隐藏来源图片。使用已有受限封面缓存与异步加载，预览解码目标沿用窗口短边尺寸。

## 缩回后的闪动修复

用户后续录屏显示封面归位后闪动。原实现使用独立 Popup，移除窗口后才在 onDispose 中恢复缩略图，两个窗口的画面无法作为同一次更新提交。

现由应用根部的 `AlbumArtworkBrowserHost` 在同一窗口内承载预览，来源页通过局部控制器发起预览。缩回完成时先恢复来源，再移除预览，统一在应用画面中提交；取消和来源页离开也会释放临时隐藏状态，恢复回调只执行一次。预览期间屏蔽下层无障碍节点，返回由预览层处理。没有增加延时、空白遮罩或修改相册动画节奏。

## 验证

单元测试检查方形、横向和纵向图片的起止边界、等比裁剪、直线路径及回弹落点。设备用例覆盖打开后收稳、打开途中关闭、关闭系统动画后的最终状态；设备测试仅编译，实际效果由用户验证。

归位闪动修复后，`testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest assembleRelease` 全部通过：178 项单元测试零失败，Lint 为 0 错误、63 条警告、1 条提示。设备测试 APK 共 76 项用例，本轮未执行。新增用例检查同一窗口、交接时来源或预览至少一方存在、来源移除时取消预览，以及恢复回调不重复。`git diff --check` 通过。

2026-09-07 查阅 [LayoutCoordinates](https://developer.android.com/reference/kotlin/androidx/compose/ui/layout/LayoutCoordinates) 和 [PopupPositionProvider](https://developer.android.com/reference/kotlin/androidx/compose/ui/window/PopupPositionProvider)，核对屏幕坐标与 Popup 定位的区别。未新增或升级依赖。
