# widget-and-reminders.md · Widget、提醒与后台任务

- **用途**：说明桌面课表 Widget、上课/考试提醒、更新检查的实现方式与平台限制。
- **阅读时机**：修改 Widget 尺寸/样式/刷新、调整通知策略、排查后台任务不触发。
- **最后更新**：2026-10-21
- **状态**：稳定（v1.5.0 + 2026-10-21 小组件刷新引擎/样式重做；真机已验证刷新链与横版/微型渲染，列表行数自适应/假期态待验）

## 目录

- [桌面 Widget](#桌面-widget)
- [Widget 数据与刷新](#widget-数据与刷新)
- [上课/考试提醒](#上课考试提醒)
- [更新检查](#更新检查)
- [平台限制与注意事项](#平台限制与注意事项)

## 桌面 Widget

### 三种尺寸

| 尺寸 | 类/Receiver | 尺寸（minWidth × minHeight） | 内容 |
| --- | --- | --- | --- |
| 微型 2×2 | `ScheduleMicroWidget` / `MicroWidgetReceiver` | 110 × 110 dp | 正在上/下一节课 + 状态标签；课程全结束显示状态卡 |
| 横向 4×2 | `ScheduleMediumWidget` / `MediumWidgetReceiver` | 250 × 110 dp | 时间轴 + 色条；从下一节起显示 2–4 节（随高度） |
| 列表 4×4 | `ScheduleLargeListWidget` / `LargeListReceiver` | 280 × 280 dp | 从下一节起显示 4–6 节详情（随高度） |

- 注册：`AndroidManifest.xml` 三个 `receiver`，各自引用 `res/xml/schedule_widget_*.xml`；`miuiWidgetVersion=1` 在 `<application>` 统一声明。
- 配置 XML 统一：`updatePeriodMillis=3600000`、`initialLayout=@layout/widget_loading`、`previewImage=@drawable/widget_preview_*`、`description`、`minWidth/minHeight`。**已移除 `widgetFeatures="reconfigurable"`**：App 内没有配置页，声明它只会让系统显示一个点了没反应的「配置」入口。
- 行数自适应：横版/列表按 `LocalSize.current.height` 取行数（`WidgetScheduleLogic.mediumRowsFor`: ≥330dp→4、≥210dp→3、否则 2；`largeRowsFor`: ≥440dp→6、≥350dp→5、否则 4）；`resizeMode` 保留横向 + 纵向，微型固定 2×2 不可缩放，保持单卡观感。
- 标签：`课表·微型` / `课表·横条` / `课表·列表`；旧的无用条目 `widget_large_grid_label/desc` 已删除（`values/strings.xml` 与 `values-en/strings.xml` 同步）。
- 点击 Widget → `myhebnu://navigate/schedule` 深链回 App。

### 数据来源

`ScheduleWidgetData.loadDaySchedule()` 在 Glance 进程中通过 `ScheduleWidgetEntryPoint` 取依赖：

| 数据 | 来源 | 说明 |
| --- | --- | --- |
| 课程 | Room `ScheduleDao` | 按当前学年/学期 + 目标星期查询 |
| 当前周 | `semester_start_date` / `semester_end_date` 实时计算 | `SemesterDateUtils.computeCurrentWeek`；起止日期缺失才回退 DataStore `current_week` |
| 节次时间 | DataStore `period_times_json` | App 联网成功后持久化；为空才回退 13 节硬编码表 |
| 明日课程 | Room + 下一天星期 | 19:00 后切换逻辑，周次自动 +1 |

跨日/状态规则（`DayScheduleState`），全部由 `ScheduleWidgetData.loadDaySchedule()` 计算：

| 场景 | 状态 | 显示 |
| --- | --- | --- |
| 周次算出来是 0（开学前 / 学期结束后） | `Vacation` | 「假期中 / ☀️ 好好休息吧」 |
| 今天还有课（含正在上的那节） | `HasCourses` | 从下一节（或正在上的）开始的课程 + 色条 |
| 今天上完但未到 19:00 | `AllDoneToday` | 「今日课程已结束」 |
| 工作日 19:00 后 | `HasCourses(isTomorrow=true)` | 明日课程，标题带「明天 周X」 |
| 周五 19:00 后 / 周六整天 | `Weekend` | 「周末愉快」 |
| 周六 19:00 后 / 周日整天 | `HasCourses(isTomorrow=true)` | 下周一课程 |
| 周日 19:00 后且下周无课 | `Weekend` | 周末状态 |

- 19:00 是硬编码切换点（`ScheduleWidgetData.loadDaySchedule` 里的 `LocalTime.of(19, 0)`）。
- **硬滑动**：横版/列表永远从 `nextCourseIndex` 开始展示，已上完的课不再占位；「其他 N 节课程」按剩余节数计算（不是总课数减固定行）。
- 微型状态标签优先级：`明天` > `正在上课` > `下节课`；旧实现用 `nextCourseIndex == 0` 判断「正在上课」，把课间/课前误标。

## Widget 数据与刷新

刷新引擎已从「每 24 小时一次的周期任务」改为**事件边界一次性任务链**（`WidgetTickScheduler` + `WidgetTickWorker`，均在 `com.myhebnu.widget`）：

| 触发 | 实现 |
| --- | --- |
| 事件边界 | `WidgetTickWorker` 刷新完所有实例后，用 `WidgetScheduleLogic.nextDelayMinutes()` 排下一次；边界 = 节次起止时刻 + 次日 00:00，延迟钳制在 1–360 分钟 |
| 零点必刷 | 边界集合始终包含次日 00:00，跨日状态（明日课程 / 周末 / 假期）在零点自动切换 |
| App 启动 | `MyHebnuApplication.onCreate` → `WidgetTickScheduler.start()`：取消旧周期任务 `schedule_widget_midnight` 并排下一次 |
| App 内数据变化 | `WidgetUpdateManager.updateAll()` → `GlanceAppWidget.update()` 刷新所有实例；`Mutex.tryLock()` 防止与 tick 撞车重复刷新 |
| 系统周期 | XML `updatePeriodMillis=3600000` 保留作兜底（Launcher 自己也会偶尔请求） |
| 手动 | 系统/桌面刷新（受 Launcher 限制） |

- 为什么要改：原来只有 24 小时周期任务 + 每小时系统刷新，上/下课时刻、19:00 切换点最多滞后 1 小时。一天约 30 次点对点刷新，WorkManager 完全承受得住。
- 仍然**不做分钟级轮询**（#19）：刷新只发生在事件边界，不做周期高频任务。
- `ScheduleWidgetWorker` 保留为兼容壳（旧版本升级来的设备上它可能还挂在系统里），内部同样调用 `scheduleNext`，避免升级瞬间刷新断档。
- App 内“添加 Widget”入口已放弃（`requestPinAppWidget` 需小米审核，见 #21）。
- Widget 颜色桶固定 6 色（#17/#18），不跟随 App 自定义主题色；色相实验（6 桶 → 24 档）**不采纳**。

### 视觉规范（贴近 M3，全部走资源）

| 项 | 值 |
| --- | --- |
| 圆角 | 微型/横版 20dp（`widget_dp_20`）、列表 24dp（`widget_dp_24`） |
| 背景 | 微型 `widget_surface_container_lowest`；横版 `widget_surface_container`；列表 `widget_surface_container_lowest` + 卡片 `widget_surface_container`（圆角 12dp） |
| 标题 | 微型 16sp、横版 14sp、列表 14sp，`widget_primary`；副标题 11sp，`widget_on_surface_variant` |
| 行 | 横版色条高 40dp、时间列 42dp；列表色条高 36dp、时间列 36dp |
| 空态 | 标题 16sp（列表 18sp）+ 副标题 11sp（列表 12sp）；Loading 与 NoData 文案分开 |
| 深色 | 颜色全部取 `values-night/colors.xml`；代码不再传 `isDark`（Glance 侧拿不到可靠的主题配置，参数已是死代码） |

### 真机验收记录（2026-10-21，Xiaomi 24129PN74C / HyperOS 3 / Android 17·SDK 37，debug 包）

| 项 | 结论 |
| --- | --- |
| tick 调度 | ✅ App 启动日志 `WidgetTickScheduler: next tick in N min`（当晚 145 min → 00:00），旧周期任务 `schedule_widget_midnight` 已被取消 |
| 横版标题间距 | ✅ 日期与「第N周」之间 8dp 生效 |
| 圆角 | ✅ 横版 20dp、微型 20dp 生效 |
| 硬滑动 | ✅ 21:22 全部上完 → 组件切到「明天 10月22日 周四 第6周」并从明日第一节开始 |
| 微型标签 | ✅ 修掉「明天 明天 周四」重复，显示「明天 周四」 |
| 列表 4–6 行自适应 | ⏳ 待验：当日只剩 1 节课，需在有多节课的日子或手工缩放确认 |
| 假期态 `Vacation` | ⏳ 待验：需把学期起止日期改到当前日期之外 |
| 多节课硬滑动计数 | ⏳ 待验：需 ≥2 节剩余课程，确认「其他 N 节课程」按剩余数而非总数 |

- 验收用的 debug 包在 `app/src/debug/res/values/strings.xml` 覆盖了 `app_name=MyHEBNU D`、`widget_*_label=课表·横条 D` 等，只为在同机区分 debug/release 两套组件（debug 变体 `applicationIdSuffix=".debug"` 是独立 App，桌面组件也独立）。删掉该文件即恢复原标签。

## 上课/考试提醒

### 上课提醒

| 项 | 内容 |
| --- | --- |
| 调度 | `ClassReminderWorker`，唯一周期任务 `class_reminder_periodic`，15 分钟 |
| 初始延迟 | 对齐到下一个 15 分钟整点 |
| 数据 | Room 当天课程 + 节次时间（`fetchPeriods`，缓存/持久化/兜底） |
| 条件 | 当前周在课程周次内 + 单双周匹配 + 距上课 ≤15 分钟 |
| 去重 | `UserPreferences.sent_reminders`，键 `class|{courseId}|{date}` |
| 通知 | 渠道 `class_reminder`，高重要性；BigText 显示课程/教室/教师/起止时间 |
| 点击 | 深链 `myhebnu://navigate/schedule` |

### 考试提醒

| 项 | 内容 |
| --- | --- |
| 调度 | `ExamReminderWorker`，唯一周期任务 `exam_reminder_periodic`，60 分钟 |
| 初始延迟 | 5 分钟 |
| 数据 | Room 已缓存考试（**不联网**） |
| 触发 | 考前 1 天、考前 1 小时 |
| 去重 | `sent_reminders`，按考试 id + 日期 |
| 通知 | 渠道 `exam_reminder`，高重要性 |
| 点击 | 深链 `myhebnu://navigate/exam` |

### Worker 可靠性约定

- 所有异常在 Worker 内部消化并返回 `Result.success()`，避免周期任务被系统取消。
- `ReminderManager` 统一负责查询与通知，Worker 只做调度入口。
- 通知权限：Android 13+ 需 `POST_NOTIFICATIONS`；未授权时捕获 `SecurityException` 静默失败。
- Manifest 声明了 `SCHEDULE_EXACT_ALARM`，但当前实现用 WorkManager 周期任务，**未使用精确闹钟**；该权限可按需清理。

## 更新检查

| 项 | 内容 |
| --- | --- |
| 接口 | GitHub `GET /repos/cheeemmms/MyHEBNU/releases/latest` |
| 版本比较 | 语义化 `major.minor.patch`，去 `v` 前缀 |
| 自动检查 | App 启动时若 `auto_check_update=true`，失败静默；发现新版本写 `available_update_version` → 首页横幅 |
| 手动检查 | 关于 → 系统与更新，按钮展示检查中/已最新/有新版本/失败 |
| 通知渠道 | `app_update` 已创建，但自动检查改为应用内横幅后不再强制发通知 |

## 平台限制与注意事项

| 限制 | 影响 | 应对 |
| --- | --- | --- |
| MIUI 把 Widget 的裸 Int 当资源 ID | 尺寸/颜色/圆角运行时崩溃 | 全部改用 `R.dimen.*` / `R.color.*`；详见 [../diagnostics/glance-miui-resources.md](../diagnostics/glance-miui-resources.md) |
| Glance 1.1.1 无 `contentDescription` API | Widget 无障碍能力有限 | 已评估穷尽；要提升需升级 Glance 或改用 RemoteViews |
| Widget 独立进程 | 读不到 App 内存缓存 | 共享数据必须落 Room/DataStore（节次表即因此持久化） |
| Launcher 刷新节流 | 不能保证分钟级准确 | 接受 1 小时 + 午夜刷新；跨日逻辑在刷新时计算 |
| 电池优化/Doze | 周期任务可能延后 | WorkManager 自带重试；不承诺精确到分钟 |
| 旧版残留周期任务 | 升级瞬间旧 `ScheduleWidgetWorker` 仍在系统里排着 | `WidgetTickScheduler.start()` 取消 `schedule_widget_midnight`，并保留 `ScheduleWidgetWorker` 兼容壳 |
| 调试包是另一个 App | debug 变体 `applicationIdSuffix = ".debug"`，桌面组件归属 `com.myhebnu.debug` | 真机验收要装 debug 包并在桌面加「课表·横条 D」等带 D 后缀的组件；正式版桌面的旧组件不会变 |
| 被替代的合成会抛异常 | 同一实例在 App 主动刷新与 Launcher rebind 撞车时，Glance 用新的 update 取代旧会话，旧会话抛 `LeftCompositionCancellationException` | 三个 `provideGlance` 已单独 catch `CancellationException` 并降为 debug 日志；该异常不代表刷新失败，最终 UI 由胜出的那次 update 决定 |
| 上课提醒冷启动可能联网 | `ReminderManager` 注释称“内存缓存”，但 `fetchPeriods` 在冷缓存时会请求 API；Worker 约束为 `NOT_REQUIRED` | 当前离线失败会回退持久化/硬编码；如需彻底离线，可改为只读 DataStore |
| 通知权限被拒 | 提醒静默不显示 | 需在设置页引导用户开启（当前未做） |
