# Glance 1.1.1 API 逆向研究与实施备忘

> 创建日期: 2026-06-08 | 状态: 编译受阻，待升级 Glance 或降级实现方案

---

## 1. 背景

Phase 7.1 目标为实现 4 种尺寸的课表桌面 Widget（微型 2×2 / 横条 4×2 / 日历网格 4×4 / 垂直列表 4×4）。当前 `glance-appwidget:1.1.1` 已作为依赖引入，但 API 与最新文档存在显著差异，导致 4 轮编译均失败。

---

## 2. 已创建的文件清单

### 共享层（已就绪，无编译问题）
| 文件 | 状态 | 说明 |
|------|------|------|
| `ScheduleWidgetEntryPoint.kt` | ✅ | Hilt `@EntryPoint` 桥接 `AppDatabase` + `UserPreferences` |
| `ScheduleWidgetData.kt` | ✅ | 数据加载层：`loadDaySchedule()` + `DayScheduleState` 状态模型 + 周/奇偶过滤 |
| `ScheduleWidgetCommon.kt` | ✅ | 色彩定义(MD3 色板 ARGB Int)、HSL→RGB、`navigateIntent()`、`updateAllWidgets()` |

### Widget UI 层（已创建但编译受阻）
| 文件 | 状态 | 受阻于 |
|------|------|--------|
| `ScheduleMicroWidget.kt` | ⚠️ | `provideContent` 不存在、`Dimension.Dp` 不可构造 |
| `ScheduleMediumWidget.kt` | ⚠️ | 同上 |
| `ScheduleLargeGridWidget.kt` | ⚠️ | 同上 + `SpaceBetween` 不确定 |
| `ScheduleLargeListWidget.kt` | ⚠️ | 同上 |
| `GridNavReceiver.kt` | ⚠️ | 依赖 `ScheduleLargeGridWidget().updateAll()` |

### 基础设施层
| 文件 | 状态 | 说明 |
|------|------|------|
| `ScheduleWidgetReceiver.kt` | ✅ | 4 个 `GlanceAppWidgetReceiver` 子类 |
| `ScheduleWidgetWorker.kt` | ⚠️ | 依赖 `updateAllWidgets()` → `GlanceAppWidgetManager` |
| `WidgetUpdateManager.kt` | ⚠️ | 同上 |
| 4 × XML configs | ✅ | 已创建 |
| `AndroidManifest.xml` | ✅ | 4 个 receiver + GridNavReceiver + deep link |
| `MainActivity.kt` | ✅ | `onNewIntent` + `pendingNavigation` Channel |

### 删除的文件
- `schedule_widget.xml`（旧占位 XML 布局）
- `widget_background.xml`（旧 drawable）
- `schedule_widget_info.xml`（被 4 个新 XML 替代）

---

## 3. Glance 1.1.1 API 反编译发现

通过 `javap` 对 Glance 1.1.1 的 `glance-1.1.1-api.jar` 和 `glance-appwidget-1.1.1-runtime.jar` 进行完整反编译，关键发现：

### 3.1 `provideContent` 不存在于 GlanceAppWidget

| 预期 API（Glance 1.2+） | Glance 1.1.1 实际 API |
|---|---|
| `GlanceAppWidget.provideContent { ... }` | **不存在此方法** |
| 在 `provideGlance()` 内调用 `provideContent` 提供 UI | 实际通过 `ContentReceiver` 接口的 `provideContent` 方法（CoroutineContext Element） |

**发现的替代机制**：
- `AppWidgetUtilsKt.runGlance()` 返回 `Flow<@Composable () -> Unit>` — 这是 Glance 运行时获取 Composable 内容的内部机制
- `ContentReceiver` 是一个 `CoroutineContext.Element`，其 `provideContent(Composer, Continuation)` 方法接收 Composable lambda

**阻塞结论**：`provideContent` 在 Glance 1.1.1 中不是 `GlanceAppWidget` 的成员函数，而是通过 CoroutineContext 机制传递。当前代码的调用模式在 1.1.1 中不可用。

### 3.2 `Dimension.Dp` 不可公开构造

| 预期 API | Glance 1.1.1 实际 API |
|---|---|
| `12.dp` 或 `Dp(12f)` 创建 dp 值 | `Dimension.Dp(Float, DefaultConstructorMarker)` — **私有构造函数** |
| `width(12.dp)` / `cornerRadius(28.dp)` | 仅 `width(Dp)` 或 `width(Int)` 两个重载 |

**已验证不存在的内容**：
- ❌ `androidx.glance.unit.Color` — 不存在此独立类
- ❌ `androidx.glance.unit.dp` 扩展属性 — 不存在
- ❌ `androidx.glance.unit.sp` 扩展属性 — 不存在
- ❌ `DimensionKt` 文件 — 不存在，无法通过顶层函数构造 Dp

**存在的重载**（来源于 `SizeModifiersKt` 反编译）：
```
width-3ABfNKs(GMod, float)     // mangled name → Kotlin 层解析为 width(Dp)
width(GMod, int)               // pixel overload
height-3ABfNKs(GMod, float)    // → height(Dp)
height(GMod, int)              // pixel overload
size-3ABfNKs(GMod, float)      // → size(Dp)
size(GMod, int)                // pixel overload
size-VpY3zN4(GMod, float, float) // → size(Dp, Dp)
size(GMod, int, int)           // pixel overload
```

**阻塞结论**：`Dp` 值无法由用户代码构造。使用 `Int`（像素值）重载虽然可编译，但不同屏幕密度下组件尺寸会严重变形。

### 3.3 `ColorProvider(Int)` — 此接口可用

| 方法 | 位置 | 签名 |
|------|------|------|
| `ColorProvider(int)` | `ColorProviderKt` (顶层函数) | `(Int) -> ColorProvider` — ARGB int → ColorProvider |
| `ColorProvider(long)` | `ColorProviderKt` (顶层函数) | `(Long) -> ColorProvider` — Color long → ColorProvider |

**结论**：`import androidx.glance.unit.ColorProvider` + `ColorProvider(0xFFXXXXXX.toInt())` 是正确的用法。这是唯一一个无需修正的 API 调用。

### 3.4 `GlanceAppWidget.updateAll(context)` — 不存在

| 预期 API | Glance 1.1.1 实际 API |
|---|---|
| `widget.updateAll(context)` | **不存在此实例方法** |

**正确方式**：通过 `GlanceAppWidgetManager`：
```kotlin
GlanceAppWidgetManager(context).getGlanceIds(widget::class.java)
    .forEach { id -> widget.update(context, id) }
```
注意：`getGlanceIds` 和 `update` 均为 suspend 函数，必须在协程作用域内调用。

### 3.5 `TextStyle.fontSize` 的 SP 单位

`TextStyle` 构造函数接受 `androidx.compose.ui.unit.TextUnit`（来自 Compose UI）。项目已有 Compose BOM 依赖，因此 `import androidx.compose.ui.unit.sp` 并使用 `12.sp` 是正确的。

---

## 4. 历次编译尝试与失败路径

| 轮次 | 方案 | 结果 | 关键错误 |
|------|------|------|----------|
| 1 | 原始 `sp(12)` / `dp(12)` 函数调用 | ❌ | `sp`/`dp` 不是函数，且无 `Color` 类 |
| 2 | `12.sp` / `12.dp` 扩展属性 + `Color(ARGB)` | ❌ | `Color` 类不存在，`dp`/`sp` 扩展属性不存在于 Glance |
| 3 | `12f` Float 字面量 + `ColorProvider(Int)` | ❌ | `width(12f)` 无 Float 重载，`ColorProvider` import 路径错误 |
| 4 | `Dimension.Dp(12f)` + `ColorProvider(Int)` | ❌ | `Dimension.Dp` 构造函数私有，`provideContent` 不存在 |
| 5 | `Int` 像素值 + `ColorProvider(Int)` | ❌ | `provideContent` 不存在，`SpaceBetween` 未解析 |

---

## 5. 可行解决方案（优先级排序）

### 方案 A：升级 Glance 到 1.2.0+（推荐）

Glance 1.2.0+ 修复了维度 API 和 `provideContent` 访问性问题。

**风险**：需确认阿里云 Maven 镜像是否有 1.2.x 版本；AGP 8.7.3 / Kotlin 2.2.21 兼容性。

### 方案 B：使用 Glance 1.1.1 的 ContentReceiver 模式

根据反编译，Glance 1.1.1 通过 `ContentReceiver`（CoroutineContext Element）接收 Composable 内容。需要深入研究 `runGlance()` 的内部机制才能正确实现。

**风险**：API 未公开文档化，属于内部实现细节，未来可能变化。

### 方案 C：退回到传统 RemoteViews + AppWidgetProvider

完全放弃 Glance，使用 Android 原生 `RemoteViews` API 实现 Widget。虽然更冗长，但 API 稳定、文档齐全。

**风险**：开发成本高（4 种尺寸需大量 RemoteViews 代码），UI 灵活度不如 Glance。

### 方案 D：像素值硬编码 + 降级 Glance

使用 `Int` 像素值重载，配合 `context.resources.displayMetrics.density` 手动 dp→px 转换，在 `provideGlance()` 中使用 `coroutineContext[ContentReceiver]?.provideContent(...)`。

**风险**：依赖未公开 API，不同设备可能表现不一致。

---

## 6. 用户需求摘要（待实施）

Widget UI 设计方案已完成并记录在计划文件中。4 种小组件的详细设计要求见 `C:\Users\yongl\.claude\plans\twinkling-puzzling-moon.md`。

---

## 7. 下一步行动

1. **决策**：管理员选择方案 A/B/C/D 之一
2. **方案 A**：修改 `libs.versions.toml` 升级 Glance → `1.2.0` 或更高，验证编译
3. **方案 A 备选**：若阿里云镜像无 1.2.x，探索 Maven Central 直连
4. **方案 A 成功后**：恢复 `provideContent` + `Dp(12f)` + `12.dp` 写法，继续编译验证
5. **编译通过后**：真机部署验证 4 种 Widget 视觉效果

---

> **关联文档**: [[progress]] | [[architecture]] | `C:\Users\yongl\.claude\plans\twinkling-puzzling-moon.md`
