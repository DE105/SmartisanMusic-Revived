# 锤子音乐复刻 (Smartisan Music Revived)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![AGP](https://img.shields.io/badge/AGP-9.2.0-3DDC84?logo=android)](https://developer.android.com/build)
[![API](https://img.shields.io/badge/minSdk-31-3DDC84?logo=android)](https://developer.android.com/about/versions/12)
[![License](https://img.shields.io/badge/License-NonCommercial-lightgrey)](./LICENSE)

> “这是为你们做的。”

Smartisan Music 是 Smartisan OS 里我一直很喜欢的系统应用。

Smartisan OS 早已退出历史舞台，原版音乐播放器也留在了旧 Android 里。但那套黑胶唱盘、唱针拖拽、搓碟、光影和小动画，放到现在还是很难被别的播放器替代。所以我想把它带回现代 Android 上继续欣赏使用。

## 现在做到哪了？

这一版已经到了一个我愿意长期使用的状态。

主界面和播放页经过了很多轮逆向、对照和真机调整，整体观感、页面层级、动画/动效和主要交互都到了我比较满意的状态。

主要页面、底部播放条、搜索、弹窗、黑胶唱盘、唱针、搓碟、歌词 / 控制区、播放队列展开和队列拖拽排序等等都已经完善。本地音乐扫描、后台播放、收藏、播放列表、播放统计和外部音频打开也都已经接上现代播放链路。

后面如果继续改，主要就是维护稳定性、继续优化细节和性能，以及修复我还没发现的 Bug。

## 真机截图

<p align="center">
  <img src="docs/images/device-shot-01.jpg" width="220" />
  <img src="docs/images/device-shot-02.jpg" width="220" />
  <img src="docs/images/device-shot-03.jpg" width="220" />
</p>
<p align="center">
  <img src="docs/images/device-shot-04.jpg" width="220" />
  <img src="docs/images/device-shot-05.jpg" width="220" />
  <img src="docs/images/device-shot-06.jpg" width="220" />
</p>

## 为什么又重写了一版？

这个项目一开始是一个纯 Jetpack Compose 版本，当时以 Smartisan OS 6.8.0（坚果 R1）作为复刻基准。这段历史现在保存在 `archive/6.8.0-compose` 分支里。

那一版不是不能用，播放链路和主要功能都能跑。但如果目标是真正 1:1 复刻 Smartisan Music，它始终差一点。锤子音乐很多细节不是“把元素画在同样的位置”就完了，它依赖旧 View/XML 体系里的测量、阴影、selector、列表分层、文本排版、按压状态和动画节奏。

Compose 版虽然能跑，但很多细节始终达不到我满意的程度。

所以才有了现在的 8.1.0 legacy View 版本。

现在这版用 legacy View 壳保住视觉事实：能按原版 XML、drawable、dimens、selector、anim 和控件结构做的，就尽量按原版做。播放、扫描、收藏、队列、设置和后台服务则继续使用现代 Android 技术栈。

## 为什么用 People-11 的 8.1.0？

当前视觉基准来自 People-11 的 [SmartisanOS_APP_Port](https://github.com/People-11/SmartisanOS_APP_Port/) 项目里的 `Music_8.1.0.apk`。

Smartisan OS 的系统应用不是普通 Android APK。它们依赖很多私有框架和系统组件，比如数据库结构、Telephony Intent、双卡逻辑、弹窗、动效、样式、验证码识别、系统资源引用等。很多东西只拆音乐 APK 是看不全的，因为真正的逻辑和样式在系统框架里。

People-11 做移植时已经手动补了大量缺口，让这些原版应用能在非 Smartisan 系统上尽可能跑起来。音乐 8.1.0 也额外支持了 MediaSession，优化过媒体扫描效率和播放队列展开效果。这个工作量很大，也让它成为一个非常可靠的视觉和交互基准。

我又逆向解包了 Smartisan Music 8.1.0 APK 和部分 Smartisan 系统组件，用来对照 XML、drawable、values、anim、私有控件引用和系统资源关系。People-11 的移植版提供接近可运行原版的事实基准，本仓库则在这个基准上重建现代播放链路和功能体验。

所以这里必须特别感谢 People-11 的付出，同时明确标注这个基准来源。

## 和 People-11 移植版有什么区别？

People-11 做的是 Smartisan OS APP 移植，尽量让原版 APK 本身在其他系统上原汁原味地继续运行。

当前仓库做的是锤子音乐复刻。UI 尽量贴近 8.1.0 原版，但播放链路、媒体扫描、队列、收藏、设置、数据持久化和后台服务都是重新用现代 Android 技术栈写的，所以后续可以继续维护，也可以在不破坏原版味道的前提下补新功能。比如移植版里受系统库限制没法完整保留的搓碟，在这里就可以接到新的播放链路里继续做。

没有 People-11 的移植版，也就没有我们现在这个版本。

## 用了什么？

| 类别 | 技术                                                 |
| ---- | ---------------------------------------------------- |
| 构建 | Android Gradle Plugin `9.2.0`                      |
| 语言 | Kotlin `2.3.21`                                    |
| UI   | legacy View 壳 + Jetpack Compose 桥接                |
| 播放 | Media3 `1.10.0`                                    |
| 存储 | Room `2.8.4` + DataStore Preferences `1.2.1`     |
| SDK  | `minSdk 31` / `targetSdk 36` / `compileSdk 36` |

## 工程大概长这样

```text
.
├── app/
│   └── src/main/
│       ├── java/com/smartisanos/music/
│       │   ├── data/       # Room、DataStore、Repository
│       │   ├── playback/   # Media3 播放服务、本地媒体库、队列、封面和歌词
│       │   └── ui/
│       │       ├── shell/   # 8.1.0 legacy 主壳、页面、转场和弹窗
│       │       ├── playback/# 播放页、唱盘、搓碟、弹层和控制区
│       │       └── widgets/ # 为复刻补的旧 View / shim 控件
│       └── res/             # 8.1.0 迁移资源和现代 Android 资源
├── docs/
├── reverse/
└── gradle/
```

## 免责声明

本项目与字节跳动无关，仅为个人兴趣驱动的非官方复刻。

- Smartisan OS 及相关视觉设计的知识产权归原权利人所有。
- 8.1.0 APK 资源文件仅供学习研究，版权归原权利人所有。
- 原创代码仅限非商业用途。

## 许可证

原创代码仅限非商业用途。APK 资源版权归原权利人所有，仅供学习研究。

感谢 [People-11](https://github.com/People-11/SmartisanOS_APP_Port/) 的移植工作。

---

复刻它不只是怀旧，是希望打开它的那一刻，你也能感到和我一样的愉悦。
