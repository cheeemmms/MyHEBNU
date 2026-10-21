# widget-and-reminders.md · Widget、提醒与后台任务

- **用途**：说明桌面课表 Widget、上课/考试提醒、更新检查的实现方式与平台限制。
- **阅读时机**：修改 Widget 尺寸/样式/刷新、调整通知策略、排查后台任务不触发。
- **最后更新**：2026-10-21
- **状态**：稳定（v1.5.0；部分平台限制见文末）

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
| 微型 2×2 | `ScheduleMicroWidget` / `MicroWidgetReceiver` | 110 × 110 dp | 下一节课或明日课程；全部结束显示状态 |
| 横向 4×2 | `ScheduleMediumWidget` / `MediumWidgetReceiver` | 250 × 110 dp | 时间轴 + 色条 + 近两节课 |
| 列表 4×4 | `ScheduleLargeListWidget` / `LargeListReceiver` | 280 × 280 dp | 当天全部课程详情 |

- 注册：`AndroidManifest.xml` 三个 `receiver`，各自引用 `res/xml/schedule_widget_*.xml`。
- 配置 XML 统一：`updatePeriodMillis=3600000`（1 小时）、`initialLayout=@layout/widget_loading`、`previewImage=@drawable/widget_preview_*`、`resizeMode`、`widgetFeatures="reconfigurable"`、`description`。
- HyperOS 标记：`<meta-data android:name="miuiWidgetVersion" android:value="1" />`。
- 点击 Widget → `myhebnu://navigate/schedule` 深链回 App。

### 数据来源

`ScheduleWidgetData.loadDaySchedule()` 在 Glance 进程中通过 `ScheduleWidgetEntryPoint` 取依赖：

| 数据 | 来源 | 说明 |
| --- | --- | --- |
| 课程 | Room `ScheduleDao` | 按当前学年/学期 + 星期查询 |
| 当前周 | DataStore `current_week` | 用于过滤周次/单双周 |
| 节次时间 | DataStore `period_times_json` | App 联网成功后持久化；为空才回退 13 节硬编码表 |
| 明日课程 | Room + 下一天星期 | 19:00 后切换逻辑 |

跨日/状态规则（`DayScheduleState`）：

| 场景 | 显示 |
| --- | --- |
| 今天还有课 | 当前/下一节课 + 色条 |
| 今天课程全部结束且未到 19:00 | `AllDoneToday` |
| 19:00 后工作日 | 明日课程（标题带“明天 周X”） |
| 周五 19:00 后 / 周六 | 周末状态 |
| 周日 19:00 后 | 下周一课程 |

## Widget 数据与刷新

| 触发 | 实现 |
| --- | --- |
| 系统周期 | XML `updatePeriodMillis=3600000`（1 小时） |
| 午夜刷新 | `ScheduleWidgetWorker`，`PeriodicWorkRequest 24h`，初始延迟到当天 00:00 |
| App 内数据变化 | `WidgetUpdateManager` 调用 `GlanceAppWidget.update()` 刷新所有实例 |
| 手动 | 系统/桌面刷新（受 Launcher 限制） |

- 明确**放弃分钟级更新**（#19）：WorkManager 最小周期 15 分钟，AlarmManager 代价高；每小时 + 午夜足够。
- App 内“添加 Widget”入口已放弃（`requestPinAppWidget` 需小米审核，见 #21）。
- Widget 颜色桶固定 6 色（#17/#18），不跟随 App 自定义主题色。

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
| 上课提醒冷启动可能联网 | `ReminderManager` 注释称“内存缓存”，但 `fetchPeriods` 在冷缓存时会请求 API；Worker 约束为 `NOT_REQUIRED` | 当前离线失败会回退持久化/硬编码；如需彻底离线，可改为只读 DataStore |
| 通知权限被拒 | 提醒静默不显示 | 需在设置页引导用户开启（当前未做） |
