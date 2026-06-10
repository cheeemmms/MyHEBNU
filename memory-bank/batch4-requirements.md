# Batch 4 需求讨论 — 用户反馈记录

> 记录日期: 2026-06-07 | 来源: 真机验证 + 用户讨论

---

## 1. 首页问候语

- 顶部显示 **"早上/中午/下午/晚上好，xxx"**
- 姓名来源：考试 API 的 `xm` 字段 或 `GET /xtgl/index_cxYhxxIndex.html` 用户信息端点
- 技术确认：姓名可通过现有接口获取，首次登录后缓存到 UserPreferences
- 时段划分：早上 6-12 / 中午 12-14 / 下午 14-18 / 晚上 18-次日6

## 2. "下一节课"卡片

- 显示：课程名称、教室位置、任课老师
- 如果**今天课程已上完** → 显示 "今日课程已结束"
- 如果**今天周末** → 显示 "今天周末，好好休息吧~"
- 数据来源：ScheduleViewModel 中已有的当天课程列表
- 点击 → 跳转课表周视图

## 3. 空教室卡片

- 无动态数据，但需要展示内容来丰富页面
- 待讨论：展示什么？快速入口？最近查询记录？校区/楼栋快捷方式？
- 点击 → 跳转空教室筛选页面

## 4. "下一场考试"卡片

- 显示：考试时间、地点、座位号、距今时间
- 如果**无考试** → 显示 "暂无考试安排"
- 数据来源：ExamViewModel 中按日期排序的考试列表（取最近一场）
- 点击 → 跳转考试列表页

## 5. 抽屉 + 设置 + 捐赠

- 左上角**保留抽屉**，增加**设置入口**
- 未来功能：捐赠功能、鸣谢列表
- 捐赠后解锁：**自定义背景** + **自由主题色彩**
- 技术澄清：MD3 原生支持 Dynamic Color（跟随系统壁纸），但"用户自选任意 seed color"需要自行实现 color picker → 生成 tonal palette

## 6. 捐赠功能设计

- 设置页内有 **"高级功能"二级菜单**
- 进入后：顶部一个**开关**
- 下方说明文字：**"我已诚信捐赠，启用高级功能"**
- 开启后弹出**高级功能设置菜单**（自定义背景/主题色等）

## 7. HTTP 302 Bug（已定位，待修复）

- **现象**：登录后一段时间，访问考试和成绩时 APP 报 HTTP 302，重试无效
- **根因**：`AuthInterceptor` 检测到 session 过期后设置 `_sessionExpired = true`，但**整个 App 没有任何地方观察此 StateFlow**。Session 被静默忽略，CAS cookie 失效后重试永远失败
- **影响范围**：所有需要会话的 API（课表/成绩/空教室/考试）
- **修复方向**：MainActivity 层观察 `sessionExpired` → 触发重新登录流程
- **优先级**：P0（阻塞使用）

---

## 相关记忆

- [[教务系统逆向分析]] — API 端点文档
- [[design-document]] — v1.1 单首页原始设计
- [[progress]] — 当前进度 Batch 3 完成

---

## 8. Widget 前端改进需求（2026-06-10）

### 8.1 LargeGrid Widget 已删除

原来 4 种 Widget 精简为 3 种：Micro (2×2)、Medium (4×2)、LargeList (4×4)。

### 8.2 跨日显示明日课程 ✅ 已完成 (2026-06-10)

**规则**：

| 场景 | 条件 | Widget 显示 |
|------|------|-------------|
| 今日课程已全部结束 | `nextCourseIndex < 0` 且 `now > 19:00` | 明日课程 |
| 今日周日 | `dayOfWeek == 7` 且 `now > 19:00` | 明日周一课程 |
| 今日周六 | 时间 > 19:00 | 明日周日 → 显示"周末愉快" |
| 今日周五+课程结束 | 时间 > 19:00 | 明日周六 → 显示"周末愉快" |

**实现要点**：
- `loadDaySchedule()` 需支持 `targetDayOfWeek` 参数
- Widget 标题行需显示 "明天 周一" 区分当天和次日

### 8.3 深色模式 ✅ 已完成 (2026-06-10)

- `res/values-night/colors.xml` — 新建, 15 色 MD3 Dark Scheme
- `ResourceColorProvider` 自动按系统深/浅模式切换
- 零代码改动. 编译通过.

### 8.4 分钟级更新（已放弃）

管理员决定保持 XML `updatePeriodMillis=3600000`（1 小时），不引入 AlarmManager。

### 8.5 Widget 复用 App 主题色（已放弃）

管理员决定不扩颜色桶，保持现有 `colors.xml` 9 基础色 + 6 课程色。

### 8.6 Widget 前端精修 — Session 执行计划

> 决议日期: 2026-06-10 | 路线: Widget 样式精修 → HyperOS 适配 → 深色模式

#### 开发路线

```
Session 1           Session 2           Session 3           后续
Micro 居中          Medium 时间列        HyperOS 对标检查   深色模式
+ Medium 初修       + 间距调整           + 逐个调整          + 明日课程逻辑
                                        (可能多 session)
```

#### Session 1: Micro 居中 + Medium 紧急修复 ✅ 已完成 (2026-06-10)

| # | 任务 | 文件 | 改动 | 实施 |
|---|------|------|------|------|
| S1-1 | Micro HasCourses 内容下移 | `ScheduleMicroWidget.kt` | Column 顶部+底部各加 `Spacer(Modifier.defaultWeight())` 实现垂直居中 | 双 `defaultWeight()` 方案 (自适应 widget 缩放) |
| S1-2 | Medium 时间列宽度修复 (09:45→09:\\n45) | `ScheduleMediumWidget.kt` | `width(32)` → `width(42)` (`R.dimen.widget_dp_42` 已存在) | 1 行改 |
| S1-3 | Medium 标题与课程间距加大 | `ScheduleMediumWidget.kt` | `Spacer(height=6)` → `Spacer(height=10)` | 1 行改 |
| S1-4 | LargeList 确认无需改动 | `ScheduleLargeListWidget.kt` | 目视审计: 36dp 时间列 + 12sp 字号 + 8dp 间距均合适 | 零改动 |

**实际改动量**: 2 files, 4 lines. 编译零错误通过.

#### Session 2: Medium 信息层级 + 间距 ✅ 已完成 (2026-06-10)

| # | 任务 | 文件 | 改动 | 实施 |
|---|------|------|------|------|
| S2-1 | Medium 课程行间距增大 | `ScheduleMediumWidget.kt` | 课程行间 `Spacer(height=6)` → `8dp` | 1 行改 |
| S2-2 | Medium 课程卡片内间距微调 | `ScheduleMediumWidget.kt` | 审计: 非对称 6dp/8dp 是有意设计（色条靠近时间），保持 | 零改动 |
| S2-3 | Medium 剩余课程提示行间距 | `ScheduleMediumWidget.kt` | `Spacer(height=4)` → `8dp`，与 S2-1 统一 | 1 行改 |

**实际改动量**: 1 file, 2 lines. 编译零错误通过. 最终垂直节奏: 10→8→8.

#### Session 3: HyperOS 对标检查 ✅ 已完成 (2026-06-10)

| # | 任务 | 状态 | 实施 |
|---|------|------|------|
| HS-4 | `initialLayout` 加载态布局 | ✅ | 新建 `res/layout/widget_loading.xml` + 3 Widget XML |
| HS-5 | `miuiWidgetVersion` meta-data | ✅ | `AndroidManifest.xml` +3 行 |
| HS-6 | Widget 命名规范化 | ✅ | 3 标签 → `MyHEBNU·课表` |
| HS-1+2 | 设计规范审计 | ✅ | 圆角/字号/对比度/触摸区 全合规 |
| HS-8 | 无障碍评估 | ✅ | Glance 1.1.1 限制，已穷尽 |
| HS-7 | App 内调起 Widget | ❌ 放弃 | `requestPinAppWidget` 需小米审核 |
| HS-3 | 真实预览图 | ⏳ | 延后（需真机截取 PNG） |
| HS-9 | 深色模式 | ⏳ | 延后（独立 Session） |

---

## 相关记忆

- [[design-document]] — v1.1 单首页原始设计
- [[progress]] — 当前进度 Widget 前端精修阶段
- [[glance-api-research]] — MIUI RemoteViews 资源解析机制
