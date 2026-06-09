# Glance 1.1.1 + MIUI 兼容性排查全程记录

> 最后更新: 2026-06-09 | 状态: ✅ 已解决 — 4 种 Widget 在小米15 (MIUI/Android 16) 上成功显示并跳转

---

## 1. 结论（先写重要内容）

**Glance 1.1.1 在代码层面完全可用。** 之前的 5 轮编译失败诊断（`provideContent` 不存在、`Dimension.Dp` 不可构造、`sp` 类型不匹配）是**误判**——所有 API 都可用，代码通过 `javap` 反编译字节码验证后在 Glance 1.1.1 上成功编译。

**真正的阻塞原因是 MIUI RemoteViews 膨胀器的资源解析行为**：MIUI 的 `RemoteViews` 反射执行器（`ResourceReflectionAction`、`LayoutParamAction`、`SetViewOutlinePreferredRadiusAction` 等）把 Glance 生成的**所有** Int 参数当作 `@DimenRes`/`@ColorRes` 资源 ID 去 `Resources.getDimension()`/`getColor()` 查表，而非像 AOSP 那样 fallback 回原始值。

### 最终解决方案

**把每一处数值——颜色、尺寸、间距、圆角——都定义为 Android 资源（`R.color.*` / `R.dimen.*`），代码中统一使用资源引用。**

| 资源文件 | 内容 |
|---------|------|
| `res/values/colors.xml` | 9 个表面/文字色 + 6 个课程色桶，全部 `ResourceColorProvider(R.color.xxx)` |
| `res/values/dimens.xml` | `widget_dp_N` 系列覆盖所有 dp 值（1,2,4,6,8,10,12,14,16,28,32,36,40,42,56） |
| `res/drawable/widget_preview_*.xml` | 3 个 XML Shape 预览图，`previewImage` 占位 |

### 修改的 Kotlin 文件

| 文件 | 关键改动 |
|------|---------|
| `ScheduleWidgetCommon.kt` | 9 个 `widgetXxx(): Int` → `ResourceColorProvider`；`courseColorResource()` 替代 `courseColorFromHueInt()` |
| `ScheduleWidgetData.kt` | 新增全链路诊断日志 `MyHEBNU-Widget` |
| 4 个 `ScheduleXxxWidget.kt` | `ColorProvider(xxx)` → `xxx`；所有 `size/width/height/padding/cornerRadius(Int)` → `(R.dimen.widget_dp_N)`；新增 `import com.myhebnu.R` |
| `res/xml/schedule_widget_*.xml` | 加 `previewImage` 防止 MIUI 桌面超时 |

---

## 2. 错诊复盘

### 之前 5 轮编译失败的真正原因

此前文档记录 `provideContent` 不存在于 Glance 1.1.1——**经 `javap` 反编译验证，此判断错误**。`provideContent` 是 `GlanceAppWidget` 的扩展函数，定义在 `GlanceAppWidget.kt` 中，`import androidx.glance.appwidget.provideContent` 完全合法。

那 5 轮编译失败的真正原因可能是：
1. **代理未运行**（`gradle.properties` 中 `127.0.0.1:7892` 端口拒绝连接）导致依赖无法解析
2. **代码版本不符**——当时代码可能使用了与当前不同的 API 写法

### 为什么没发现真问题

因为代码**从未成功部署到真机**。5 轮尝试全部花在"修编译错误"上，等到编译通过已是今天的 `ResourceColorProvider` 修复。RemoteViews 膨胀失败只能通过真机 logcat 发现——它在编译期完全沉默。

教训与 [[progress.md]] 中"空教室闪退"诊断完全一致：**没看运行时堆栈就开始猜测**。

---

## 3. 真机诊断时间线

| 轮次 | 修复目标 | logcat 错误 | 结果 |
|------|---------|------------|------|
| 1 | 原始代码（padding Int 字面量） | `Resource ID #0xc` → `PaddingKt.toDp` | ❌ |
| 2 | padding → `R.dimen.widget_pad_*` | `Resource ID #0xfffff7ff` → `RemoteViews$ResourceReflectionAction`（颜色）| ❌ |
| 3 | 颜色 → `ResourceColorProvider` | `Resource ID #0x1c` → `SetViewOutlinePreferredRadiusAction`（圆角 28）| ❌ |
| 4 | cornerRadius → `R.dimen.widget_dp_*` | `Resource ID #0x8` → `LayoutParamAction.getPixelSize`（size/width/height 8）| ❌ |
| 5 | **全量：所有 size/width/height/padding/cornerRadius 都用 `R.dimen.widget_dp_N`** | **零错误** | ✅ |

---

## 4. 运行机制

### 为什么 `R.xxx` 资源引用能通过而裸值不行

Glance 的 `FixedColorProvider` / `Dimension.Dp` 在 RemoteViews 中编码为 Action 的 Int 参数。MIUI 的反射执行器对这些 Int 参数调用 `Resources.getDimension(int)` / `Resources.getColor(int)`：

- **AOSP**：先尝试资源解析 → 失败 → fallback 返回原始值
- **MIUI**：`MiuiResourcesImpl.getValue()` 直接抛 `NotFoundException`，没有 fallback

当使用 `R.color.xxx` / `R.dimen.widget_dp_N` 时，Int 值是合法的资源 ID（package=0x7F），MIUI 能成功解析。

### Glance 1.1.1 实际可用 API（已验证）

| API | 状态 | 备注 |
|-----|------|------|
| `provideContent { }` | ✅ | 扩展函数，`androidx.glance.appwidget` 包 |
| `TextStyle(fontSize = 12.sp)` | ✅ | `fontSize` 类型是 Compose UI 的 `TextUnit` |
| `ColorProvider(Int)` | ✅ （已弃用）| FixedColorProvider，在 MIUI 上有兼容问题 |
| `ResourceColorProvider(Int)` | ✅ | **推荐的替代方案**，构造参数为 `@ColorRes Int` |
| `size/width/height(Int)` | ✅ | 但在 MIUI 上 Int 被当资源 ID |
| `size/width/height(@DimenRes Int)` | ✅ | **用 `R.dimen.*` 传入** |
| `padding(Int)` | ✅ | 但在 MIUI 上 Int 被当资源 ID |
| `cornerRadius(Dp)` | ✅ | 但在 MIUI 上内部 Int 被当资源 ID |
| `cornerRadius(@DimenRes Int)` | ✅ | **用 `R.dimen.*` 传入** |
| `GlanceAppWidgetManager.getGlanceIds()` | ✅ | suspend 函数 |
| `widget.update(context, id)` | ✅ | 内部方法，在协程中调用 |

---

## 5. 不影响此结论的改动

- `previewImage` — 有帮助（Widget 选择器缩略图+占位），但非核心修复
- 诊断日志 — 保留以支持后续调试
- `courseColorResource()` 6 色桶 — 替代动态 HSL 色生成

---

> **关联文档**: [[progress]] | [[architecture]]
