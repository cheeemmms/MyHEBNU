# glance-miui-resources.md · Glance + MIUI 资源解析兼容性

- **用途**：记录 Glance 1.1.1 在 MIUI/HyperOS 上因 RemoteViews 资源解析导致的 Widget 崩溃与最终修法。
- **阅读时机**：改 Widget 尺寸/颜色/圆角/间距后出现 `Resource ID #0x...` 崩溃；更换 Glance 版本；适配 HyperOS。
- **最后更新**：2026-10-21
- **状态**：已解决（3 种 Widget 在小米 15 / Android 16 上显示与跳转正常）

## 目录

- [结论](#结论)
- [最终解决方案](#最终解决方案)
- [错诊复盘](#错诊复盘)
- [真机诊断时间线](#真机诊断时间线)
- [运行机制](#运行机制)
- [Glance 1.1.1 可用 API](#glance-111-可用-api)
- [不受影响的改动](#不受影响的改动)

## 结论

**Glance 1.1.1 在代码层面完全可用。** 早期关于 `provideContent` 不存在、`Dimension.Dp` 不可构造、`sp` 类型不匹配的判断均为误诊——经 `javap` 反编译确认 API 完整，代码可编译。

真正的阻塞原因是 **MIUI 的 RemoteViews 反射执行器**：它把 Glance 生成的所有 Int 参数当作 `@DimenRes`/`@ColorRes` 资源 ID，去 `Resources.getDimension()`/`getColor()` 查表，而不是像 AOSP 那样在失败时回退原始值。

## 最终解决方案

**把每一处数值——颜色、尺寸、间距、圆角——都定义为 Android 资源，代码统一用资源引用。**

| 资源 | 内容 |
| --- | --- |
| `res/values/colors.xml` | 9 个表面/文字色 + 6 个课程色桶，全部 `ResourceColorProvider(R.color.xxx)` |
| `res/values/dimens.xml` | `widget_dp_N` 覆盖所有 dp 值（1,2,4,6,8,10,12,14,16,28,32,36,40,42,56） |
| `res/drawable/widget_preview_*.xml` / PNG | Widget 选择器预览图，防 MIUI 桌面超时 |

涉及 Kotlin 文件：

| 文件 | 关键改动 |
| --- | --- |
| `widget/ScheduleWidgetCommon.kt` | `widgetXxx(): Int` → `ResourceColorProvider`；`courseColorResource()` 替代动态色 |
| `widget/ScheduleWidgetData.kt` | 增加 `MyHEBNU-Widget` 全链路日志 |
| `ScheduleMicroWidget.kt` / `ScheduleMediumWidget.kt` / `ScheduleLargeListWidget.kt` | 所有 `size/width/height/padding/cornerRadius(Int)` → `R.dimen.widget_dp_N`；颜色 provider 直接传资源 |
| `res/xml/schedule_widget_*.xml` | 加 `previewImage`，防 MIUI 桌面 Glance 延迟触发超时 |

## 错诊复盘

此前记录「`provideContent` 不存在于 Glance 1.1.1」——经 `javap` 验证是错误判断。`provideContent` 是 `androidx.glance.appwidget` 的扩展函数，import 合法。

5 轮编译失败的真正原因可能是：

1. 代理未运行（当时 `gradle.properties` 指向 `127.0.0.1:7892`，端口拒绝连接），依赖无法解析。
2. 当时代码使用了与当前不同的 API 写法。

没发现真问题的原因：代码从未成功部署到真机。RemoteViews 膨胀失败只在运行时暴露，编译期完全沉默。

## 真机诊断时间线

| 轮次 | 修复目标 | logcat 错误 | 结果 |
| --- | --- | --- | --- |
| 1 | 原始代码（padding Int 字面量） | `Resource ID #0xc` → `PaddingKt.toDp` | ❌ |
| 2 | padding → `R.dimen.widget_pad_*` | `Resource ID #0xfffff7ff` → `RemoteViews$ResourceReflectionAction`（颜色） | ❌ |
| 3 | 颜色 → `ResourceColorProvider` | `Resource ID #0x1c` → `SetViewOutlinePreferredRadiusAction`（圆角 28） | ❌ |
| 4 | cornerRadius → `R.dimen.widget_dp_*` | `Resource ID #0x8` → `LayoutParamAction.getPixelSize`（size/width/height 8） | ❌ |
| 5 | **全量：所有 size/width/height/padding/cornerRadius 都用 `R.dimen.widget_dp_N`** | **零错误** | ✅ |

## 运行机制

Glance 的 `FixedColorProvider` / `Dimension.Dp` 在 RemoteViews 中编码为 Action 的 Int 参数。MIUI 的反射执行器对这些 Int 参数调用 `Resources.getDimension(int)` / `getColor(int)`：

- **AOSP**：先尝试资源解析 → 失败 → fallback 返回原始值。
- **MIUI**：`MiuiResourcesImpl.getValue()` 直接抛 `NotFoundException`，没有 fallback。

当使用 `R.color.xxx` / `R.dimen.widget_dp_N` 时，Int 值是合法资源 ID（package=0x7F），MIUI 能成功解析。因此「资源化」既是兼容手段，也是当前项目 Widget 的强制约定。

## Glance 1.1.1 可用 API

| API | 状态 | 备注 |
| --- | --- | --- |
| `provideContent { }` | ✅ | 扩展函数，`androidx.glance.appwidget` 包 |
| `TextStyle(fontSize = 12.sp)` | ✅ | `fontSize` 类型是 Compose `TextUnit` |
| `ColorProvider(Int)` | ✅（已弃用） | FixedColorProvider，在 MIUI 上有兼容问题 |
| `ResourceColorProvider(Int)` | ✅ | **推荐**，构造参数为 `@ColorRes Int` |
| `size/width/height(Int)` | ✅ | 但在 MIUI 上 Int 被当资源 ID |
| `size/width/height(@DimenRes Int)` | ✅ | **用 `R.dimen.*`** |
| `padding(Int)` | ✅ | 但在 MIUI 上 Int 被当资源 ID |
| `cornerRadius(Dp)` | ✅ | 但内部 Int 在 MIUI 上被当资源 ID |
| `cornerRadius(@DimenRes Int)` | ✅ | **用 `R.dimen.*`** |
| `GlanceAppWidgetManager.getGlanceIds()` | ✅ | suspend |
| `widget.update(context, id)` | ✅ | 内部方法，在协程中调用 |

## 不受影响的改动

- `previewImage`：有帮助（选择器缩略图 + 占位），但非核心修复。
- 诊断日志：保留以支持后续排查（TAG `MyHEBNU-Widget`）。
- `courseColorResource()` 6 色桶：替代动态 HSL 生成；限制（不跟随 App 自定义主题色）见 [widget-and-reminders.md](../architecture/widget-and-reminders.md)。

> 关联：[widget-and-reminders.md](../architecture/widget-and-reminders.md) · [tech-stack.md](../tech-stack.md)
