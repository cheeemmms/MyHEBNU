# compose-nested-scroll.md · Compose 嵌套滚动崩溃复盘

- **用途**：记录空教室查询闪退（项目最顽固 Bug）的根因、修复文件与可复用排查清单。
- **阅读时机**：Compose 出现 `Vertically scrollable component was measured with an infinity maximum height`、点击后崩溃/卡死；新增滚动容器时。
- **最后更新**：2026-10-21
- **状态**：已解决（Batch 1，2026-06-05）；规则仍强制生效

## 目录

- [结论](#结论)
- [为什么花了这么长时间](#为什么花了这么长时间)
- [实际根因（三层）](#实际根因三层)
- [修复文件汇总](#修复文件汇总)
- [可复用检查清单](#可复用检查清单)
- [当前强制规则](#当前强制规则)

## 结论

「空教室点击查询 → 闪退」的主因不在数据层，而在 **Compose 布局层的嵌套垂直滚动容器**：

```text
RoomScreen: Column(Modifier.verticalScroll(...))   ← 外层垂直滚动
    └── RoomList: LazyColumn(...)                   ← 内层垂直滚动
```

Compose 给内层 `LazyColumn` 传入无限最大高度约束，measure 阶段抛：

```text
java.lang.IllegalStateException: Vertically scrollable component was measured
with an infinity maximum height constraints, which is disallowed.
```

## 为什么花了这么长时间

8 次提交、4 轮 logcat 分析，从数据层猜到 Compose 布局才定位。

| 失误 | 表现 | 正确做法 |
| --- | --- | --- |
| 隧道视野 | 看到“点击查询→闪退”，始终认定崩溃在数据层（网络/JSON/门控），反复改 `RoomRepository` | 先看 crash 堆栈，再猜原因；`LazyColumn` 相关错误不可能是 JSON 解析导致 |
| logcat 过滤过严 | 只过滤 `tag:MyHEBNU`，FATAL EXCEPTION（tag:AndroidRuntime）被排除，零堆栈盲猜 3 轮 | crash 诊断**不加 tag 过滤**，至少同时包含 `AndroidRuntime` + `MyHEBNU` |
| 修复不彻底 | 修了 `RoomScreen` 的 `LazyColumn`，未检查子组件 `RoomList` 里的另一个 `LazyColumn`，同一 bug 藏两处 | 修嵌套滚动时全局 `grep LazyColumn` |
| “闪退”的歧义 | 以为是按钮 `onClick` 逻辑崩溃，实际是点击触发状态变更后的**重组/渲染**阶段崩溃 | 看 stack trace 是 `measure` 还是 `onClick` |
| 空数据误导 | 空数据时 `LazyColumn` 不渲染，不崩溃，制造“数据层没问题”的假象 | 区分“有数据崩/空数据不崩”——条件渲染触发的布局崩溃特征 |

## 实际根因（三层）

1. **Compose 布局层（主因）**：`RoomScreen` 的 `Column(verticalScroll)` 内嵌 `RoomList` 的 `LazyColumn`，measure 阶段抛 `IllegalStateException`。
2. **数据层（次因）**：`getCampusInfo()` 缺少页面加载步骤，教务返回 302 拒绝，楼栋列表为空。
3. **API 层（次因）**：`registerMenuClick` 的 `gnmkdm` 默认空串应为 `"index"`；返回类型 `Response<String>` 让 Gson 去解析文本“操作成功！”失败。

## 修复文件汇总

| 层 | 文件 | 改动 |
| --- | --- | --- |
| Compose | `ui/room/RoomScreen.kt` | 外层 `LazyColumn` → `Column(verticalScroll)`（与子组件二选一） |
| Compose | `ui/room/components/RoomList.kt` | 子组件 `LazyColumn` → `Column + forEach` |
| API | `data/remote/EASystemApi.kt` | 新增 `loadRoomPage()`；`registerMenuClick` 的 `gnCode` 默认 `""→"index"`；相关返回类型 `String/JsonObject → ResponseBody` |
| 数据 | `data/repository/RoomRepository.kt` | `readJsonBody()` 绕过 Gson；`getCampusInfo()` 加三步序列；`queryEmptyRooms()` 加三步序列 + HTML 守卫 + 全链路日志 |
| UI | `ui/room/RoomViewModel.kt` | 校区标签修正；星期/节次必填校验；新增学年/学期 setter |
| UI | `ui/room/components/FilterPanel.kt` | 新增学年/学期下拉；下拉选中后自动关闭 |

## 可复用检查清单

遇到“操作后闪退”：

1. **先看 stack trace**，不是先猜原因。
2. logcat **不加 tag 过滤**（或至少包含 `AndroidRuntime`）。
3. 在堆栈中搜索 `LazyColumn`、`LazyRow`、`scroll`、`measure`；若在 measure 阶段崩溃，优先怀疑布局约束。
4. 全局 `grep -rn "LazyColumn\|LazyRow\|verticalScroll\|horizontalScroll" app/src/ --include="*.kt"`，排查所有嵌套。
5. 区分“空数据不崩/有数据崩”——条件渲染触发的布局崩溃特征。
6. 修完后回归：同一页面有数据/无数据、长列表、分页、下拉刷新各走一遍。

## 当前强制规则

**Compose 不允许在任何垂直滚动容器（`Column.verticalScroll`、`LazyColumn`）内嵌套另一个垂直滚动容器（`LazyColumn`、`LazyVerticalGrid`）。**

| 场景 | 正确做法 | 错误做法 |
| --- | --- | --- |
| Screen 根布局 | `Column(verticalScroll)` 或 `LazyColumn` 二选一 | 外层 verticalScroll + 内层 LazyColumn |
| Screen 已垂直滚动，嵌入列表 | 子组件用 `Column + forEach` | 子组件继续用 `LazyColumn` |
| 水平滚动嵌在垂直滚动里 | ✅ 安全（方向不同） | — |
| Scaffold 中 | `LazyColumn(fillMaxSize())` 安全（Scaffold 给有限约束） | `AnimatedContent` 子级直接用 LazyColumn（无有限约束） |

> 关联：[modules-and-ui.md](../architecture/modules-and-ui.md) · [data-and-storage.md](../architecture/data-and-storage.md)（空教室接口）
