# data-and-storage.md · 数据与存储

- **用途**：记录本地数据模型、缓存写入规则、DataStore 键、凭证/Cookie 存储与迁移方式。
- **阅读时机**：改实体/表结构、排查缓存不更新或迁移失败、处理登录态与敏感数据。
- **最后更新**：2026-10-30
- **状态**：稳定（Room v6，含 wishlist 表）

> 凭证/Cookie/备份/日志的安全评估见 [security.md](security.md)。

## 目录

- [存储总览](#存储总览)
- [Room 数据库](#room-数据库)
- [DataStore 偏好](#datastore-偏好)
- [凭证与 Cookie](#凭证与-cookie)
- [缓存一致性规则](#缓存一致性规则)
- [迁移与版本](#迁移与版本)
- [已知风险](#已知风险)

## 存储总览

| 存储 | 位置/实现 | 内容 | 是否跨进程可读 |
| --- | --- | --- | --- |
| Room `AppDatabase` | `data/local/db/`，数据库版本 6 | 课程、考试、成绩缓存、想看（wishlist） | 是（Widget 通过 EntryPoint 读） |
| DataStore Preferences | `user_preferences`（`Context.dataStore`） | 学期、主题、节次时间表、首页均分、提醒去重等 | 是（Widget/Worker 读） |
| EncryptedSharedPreferences | `myhebnu_credentials` | 学号 + 密码（AES256_GCM / AES256_SIV） | 否（仅 App 进程） |
| SharedPreferences | `myhebnu_session` | 教务 Cookie 键值 + 最后保存时间 | 否 |
| 内存 | Repository / ViewModel | 节次缓存、列表状态 | 否 |

- 应用私有目录，未使用外部存储；`android:allowBackup="true"`（见风险）。
- 登出只清除凭证、Cookie 与登录标记，**不清 Room/DataStore 中的课表/成绩/偏好**。

## Room 数据库

`AppDatabase`（`version = 5`，`exportSchema = false`）注册 3 个实体、3 个 DAO。

### courses（课表缓存）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | String PK | `year-term-课程名-星期-节次-周次文本` 拼接 |
| `courseName` / `teacher` / `classroom` | String | 课程名（kcmc）/ 教师（xm/jsxm）/ 教室（cdmc） |
| `dayOfWeek` | Int | 1–7（xqj） |
| `startPeriod` / `endPeriod` | Int | 起止节次；有位掩码时由 `oldjc` 反推，否则由 `jcs` 如 `3-5` 解析 |
| `startWeek` / `endWeek` | Int | 起止教学周；有位掩码时由 `oldzc` 反推，否则由 `zcd` 文本解析 |
| `oddEven` | Int | 0=全周，1=单周，2=双周；**新数据不再依赖**，仅老数据（`weekBits=0`）兜底 |
| `weekBits` / `periodBits` | Int | 周次/节次位掩码（bit0=第 1 周/第 1 节），来自教务 `oldzc`/`oldjc`；0 表示未采集 |
| `weekText` / `category` | String | 原始周次文本 / 课程类别（kclb） |
| `color` | Int | 课程名 hash 得到的稳定 hue（0–359），非最终 ARGB |
| `semesterYear` / `semesterTerm` / `semesterName` | String | 学年、学期码、展示名 |
| `lastUpdated` | Long | 当前课表写入未显式赋值（默认 0）；过期判断不依赖它 |

DAO：`getScheduleBySemester`（Flow）、`getCoursesByDay`、`upsertAll`、`deleteBySemester`、`getAvailableSemesters`。

### exams（考试缓存）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | String PK | `year-term-sjbh`，无 `sjbh` 时用课程名+日期兜底 |
| `courseName` / `location` / `campus` / `seatNumber` | String | 课程、教室、校区、座位号 |
| `examDate` | String | `yyyy-MM-dd` |
| `startTime` / `endTime` | String? | `HH:mm`，可能为空 |
| `examType` / `department` / `className` / `teacherInfo` / `courseSchedule` / `examMethod` | String | 考试类型、开课学院、班级、教师、上课时间、考试方式 |
| `semesterYear` / `semesterTerm` | String | 学期；联合索引 |
| `lastUpdated` | Long | 写入时间戳 |

DAO：`observeExams`（Flow）、`getExamsBySemester`、`replaceSemester`（`@Transaction` 删后插）。

### grades（成绩缓存，v1.5.0 新增）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | String PK | `year-term-jxb_id` |
| `courseName` / `courseCode` | String | 课程名 / 课程号 |
| `credit` | Float | 学分 |
| `score` | String | 综合成绩原文（可能为“优秀/合格”等） |
| `scoreValue` | Float? | 解析出的数值分，未出分/非数值为 null，用于 GPA |
| `classId` | String | `jxb_id`，查成绩明细用 |
| `teachingClassName` / `department` | String | 教学班名 / 开课学院 |
| `semesterYear` / `semesterTerm` / `semesterName` | String | 学期 |
| `lastUpdated` | Long | 写入时间戳 |

DAO：`observeAll`（Flow，驱动成绩页）、`getAll`、`count`、`getBySemester`、`replaceSemester`。成绩明细（子项）**不落库**，每次点开课程时请求。

## DataStore 偏好

`UserPreferences` 使用 Preferences DataStore（名称 `user_preferences`）。按主题分组：

### 学期与周次

| 键 | 默认值 | 说明 |
| --- | --- | --- |
| `current_semester_year` | `"2025"` | 学年，如 `2025` = 2025–2026 |
| `current_semester_term` | `"12"` | `3` 秋 / `12` 春 / `16` 小学期 |
| `current_week` | `1` | 当前教学周；假期可为 0 |
| `semester_start_date` / `semester_end_date` | `""` | 学期起止日（学期设置写入） |
| `semester_manually_set` | `false` | 用户是否手动设置过 |
| `last_week` | `20` | 末周（由 N2154 映射更新） |
| `campus_id` | `"4"` | 校区（4=裕华，2=老校区） |

### 身份与界面

| 键 | 默认值 | 说明 |
| --- | --- | --- |
| `is_logged_in` | `false` | 登录标记（与 Cookie 共同判断） |
| `student_name` | `""` | 首页问候语用的姓名 |
| `is_first_launch` | `true` | 欢迎页标记 |
| `selected_language` | `"zh"` | 预留语言项 |
| `theme_mode` | `"system"` | `light` / `dark` / `system` |
| `is_dark_mode` | `false` | 旧键，当前主题以 `theme_mode` 为准 |

### 高级功能与主题

| 键 | 默认值 | 说明 |
| --- | --- | --- |
| `advanced_enabled` | `false` | “诚信捐赠”开关，解锁自由主题色/周末列 |
| `use_custom_colors` | `false` | 是否启用自定义主题 |
| `active_preset_id` | `null` | 当前预设 id |
| `custom_presets_json` | `"[]"` | 自定义预设 JSON |
| `show_weekend_columns` | `false` | 课表是否显示周六/日 |

### 缓存与后台

| 键 | 默认值 | 说明 |
| --- | --- | --- |
| `period_times_json` | `""` | 真实节次表 JSON，供首页/Widget/Worker 共用 |
| `home_weighted_avg` | `0f` | 首页“本学期加权均分”缓存 |
| `home_weighted_avg_semester` | `""` | 上述均分对应学期（为空则不显示） |
| `sent_reminders` | `""` | 已发送提醒去重集合（按日期清理，上限 500） |
| `auto_check_update` | `true` | 启动自动检查更新 |
| `available_update_version` | `""` | 首页更新横幅使用 |

## 凭证与 Cookie

| 项 | 实现 | 备注 |
| --- | --- | --- |
| 学号/密码 | `CredentialManager` → EncryptedSharedPreferences（AES256_GCM 主密钥，AES256_SIV/AES256_GCM） | 仅自动重登时在内存解密 |
| 登录 Cookie | `PersistentCookieJar` → `SessionManager`（普通 SharedPreferences，名称 `myhebnu_session`） | 保存 `JSESSIONID`、`jw`、`rememberMe` 等；7 天有效期判断 |
| Cookie 合并 | `saveFromResponse()` 按 host + name 合并，不整体替换 | 302 只回 `jw` 时会覆盖丢失 `JSESSIONID`，故必须合并 |
| 登出 | `AuthRepository.logout()` 清凭证、WebView Cookie、CookieJar、`is_logged_in` | 不清理课表/成绩缓存 |
| 自动重登 | `autoLogin()` 用加密凭证重新 `performLogin()` | 失败且疑似密码错误时清空凭证 |

## 缓存一致性规则

1. **按学期隔离**：课程/考试/成绩都以 `(semesterYear, semesterTerm)` 分区；刷新只影响当前学期。
2. **先删后插的原子性**：考试/成绩用 DAO 的 `@Transaction replaceSemester()`；课表是 `deleteBySemester()` + `upsertAll()` 两步（不在同一事务）。
3. **课表过期判定**：不看 `lastUpdated`，而是看“今天是否已超过末周周日 + 14 天”；超过则进入联网校准路径。
4. **成绩页数据源**：始终观察 Room；网络刷新只负责写库，UI 不直接消费网络结果。
5. **考试页数据源**：每次进页联网，成功后写库；首页只读库。因此考试页在离线时可能显示错误，而首页仍显示旧缓存。
6. **节次表单一来源**：联网成功后写 `period_times_json`；首页/Widget/Worker 优先读它，失败才回退 `fallbackPeriods()` 的 13 节硬编码表。

## 迁移与版本

| 版本 | 迁移 | 内容 |
| --- | --- | --- |
| 1 → 2 | `MIGRATION_1_2` | `courses` 增加 `oddEven INTEGER NOT NULL DEFAULT 0` |
| 2 → 3 | `MIGRATION_2_3` | 新建 `exams` 表 + `(semesterYear, semesterTerm)` 索引 |
| 3 → 4 | `MIGRATION_3_4` | 新建 `grades` 表 + `(semesterYear, semesterTerm)` 索引 |
| 4 → 5 | `MIGRATION_4_5` | `courses` 增加 `weekBits`、`periodBits`（`INTEGER NOT NULL DEFAULT 0`） |
| 5 → 6 | `MIGRATION_5_6` | 新建 `wishlist` 表（想看书目快照：bibId 主键 / title / author / publisher / pubYear / isbn / coverUrl / addedAt） |

- `exportSchema = false`，仓库中没有 schema JSON；改表时需手写 Migration 并真机验证升级安装。
- `DatabaseModule` 显式注册 5 个 Migration；新增版本时同步修改 `AppDatabase.version` 与迁移列表。
- 发布包升级兼容性另见 [../history/change-log.md](../history/change-log.md) 的 v1.5.0 校验记录。

### wishlist 表（2026-10-30 新增）

- **存书籍快照而非只存 bibId**：列表页离线可读；封面地址是第三方图床临时链接，不回写更新。
- `WishlistDao`：三个 Flow 读（observeAll / observeCount / observeIsWished）+ `upsert`（REPLACE，查与写同事务防连点）+ `insertIgnoringDuplicates`（IGNORE，备份导入用，幂等）+ `getAll` + `deleteById`。
- `WishlistRepository` 之上还有 `WishlistBackup`（JSON 备份，带 `version` 字段；反序列化走全可空中间结构防坏文件）。
- 封面解码键分层：列表小图 `book-cover-<bibId>`、详情大图 `book-cover-lg-<bibId>`（见 `ui/library/BookCovers.kt`），与 Room 无关但同属想看功能的数据面。

## 已知风险

| 风险 | 说明 | 建议 |
| --- | --- | --- |
| Cookie 未加密 | `SessionManager` 用普通 SharedPreferences 存 `JSESSIONID/jw`；类注释“加密存储”与实现不符 | 迁移到 EncryptedSharedPreferences 或不再持久化；详见 [security.md](security.md) |
| `allowBackup=true` 且无备份规则 | 系统备份可能带走 DataStore/SharedPreferences（含加密凭证与 Cookie 文件） | 增加 `dataExtractionRules`/`fullBackupContent` 排除敏感文件；详见 [security.md](security.md) |
| 课表刷新非事务 | `deleteBySemester` 后 `upsertAll` 若失败，当前学期缓存会短暂为空 | 改为 DAO `@Transaction replaceSemester` |
| 考试页离线体验差 | 联网优先，失败不自动回退 Room | 可改为 cache-first（与课表/成绩一致） |
| 小学期成绩未接入 | `SEMESTER_TERMS` 仅 `[3,12]`；小学期尚未开始，接口/展示未知 | 待教务暴露后再验证，目前不按缺陷处理 |
