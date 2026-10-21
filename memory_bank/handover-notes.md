# handover-notes.md · 交接笔记

- **用途**：让没有上下文的新会话快速上手 MyHEBNU，并知道哪些结论已验证、哪些仍是推测。
- **阅读时机**：刚接手、长时间未参与、准备交接给他人或其他会话。
- **最后更新**：2026-10-21
- **状态**：稳定

## 目录

- [项目来源与当前状态](#项目来源与当前状态)
- [五分钟上手路径](#五分钟上手路径)
- [关键文件地图](#关键文件地图)
- [构建、运行与调试](#构建运行与调试)
- [工作流约定](#工作流约定)
- [与上游/原版的差异](#与上游原版的差异)
- [未验证的假设与风险](#未验证的假设与风险)
- [待办与坑](#待办与坑)
- [交接检查清单](#交接检查清单)
- [历史记录](#历史记录)

## 项目来源与当前状态

| 项 | 内容 |
| --- | --- |
| 来源 | 自有仓库 `https://github.com/cheeemmms/MyHEBNU.git`，非 fork |
| 分支/提交 | `main`；2026-10-21 有本地提交（记忆库/许可证/代理，见 `git log -1`）；此前发布提交 `db8580d`（2026-09-13），标签 `v1.5.0` |
| 历史规模 | 2026-06-04 起，97 次提交；`backup/2026-08-30`（`797e0ef`）保留一次回退前的未提交改动 |
| 当前可用程度 | **能构建**：2026-10-21 `assembleDebug` 通过；v1.5.0 已发布；未连接真机，功能回归待验收 |
| 最近一次验证 | 2026-10-21 `assembleDebug`：`BUILD SUCCESSFUL in 27s`，APK `MyHEBNU-v1.5.0-debug.apk`（71,289,145 B）；2026-09-13 v1.5.0 发布校验（签名 v2、证书 SHA-256 与 v1.4.0 一致、APK 7,367,150 B，来自当时记录）；本轮未连真机 |
| 工作区状态 | `gradle.properties` 有未提交改动（代理端口 7892→7897，已对齐本机）；`.workbuddy/` 未跟踪（另一工具的日志，不属于本记忆库） |

## 五分钟上手路径

1. 读 [readme.md](readme.md) 的「当前快照」与「文件清单」，确认版本与下一步。
2. 读本文件「关键文件地图」，定位要改的模块；不要通读整个 `memory_bank/`。
3. 用 Android Studio 打开项目根目录，确认 `local.properties` 中 `sdk.dir` 与签名键存在（文件已 gitignore，不会随仓库走）。
4. 启动本机代理并核对端口；`gradle.properties` 已对齐本机实际端口 `7897`。
5. 在 Android Studio 或本机终端执行 `gradlew assembleDebug`，装到 Android 12+ 真机（小米 15 / Android 16 是历史主要验证机）。
6. 首次启动：欢迎页 → 登录（学号/密码）→ 首页四卡；检查首页是否零联网出数据。
7. 改完代码后按 [tech-stack.md](tech-stack.md) 的命令自测，并把构建/真机结果写回 [implementation-plan.md](implementation-plan.md) 的验证表。

## 关键文件地图

| 想改什么 | 去哪里改 | 注意点 |
| --- | --- | --- |
| 版本号/包名/签名 | `app/build.gradle.kts` | 同步 `CHANGELOG.md`、`strings.xml` 的 `changelog_content`、Git tag |
| 入口/导航/会话门控 | `MainActivity.kt`、`ui/navigation/TopLevelRoute.kt` | 手写 backStack，不是 NavHost；新增页面要加 `when(route)` 分支 |
| 登录/自动重登 | `data/repository/AuthRepository.kt`、`data/remote/interceptor/AuthInterceptor.kt`、`CredentialManager.kt` | 901 必须继续识别；验证码重试不要先 logout |
| 课表逻辑 | `ui/schedule/ScheduleViewModel.kt`、`data/repository/ScheduleRepository.kt` | 881 行，改前先看 `_currentYear/_currentTerm` vs `_viewYear/_viewTerm` |
| 课表 UI | `ui/schedule/components/WeekViewGrid.kt`、`CourseCard.kt`、`WeekSelector.kt` | 节次列宽 5 列 36dp / 7 列 28dp；卡片高度由父层传入 |
| 成绩 | `data/repository/GradeRepository.kt`、`ui/grade/GradeViewModel.kt`、`GpaCard.kt` | 成绩页只观察 Room；网络刷新写库；GPA 公式在 `GpaCalculator` |
| 空教室 | `data/repository/RoomRepository.kt`、`ui/room/RoomViewModel.kt`、`FilterPanel.kt`、`RoomList.kt` | 三步门控 + `queryModel.showCount` + 分页 |
| 考试 | `data/repository/ExamRepository.kt`、`ui/exam/` | 每次进页联网；分页项用户实测正常（2026-10-21），暂不修改 |
| 首页 | `ui/home/HomeViewModel.kt`、`HomeScreen.kt`、`components/HomeCardPanel.kt` | 首页纯本地；不要在这里加阻塞网络 |
| 数据库 | `data/local/db/AppDatabase.kt`、`entity/`、`dao/`、`migrations/Migrations.kt` | 改表必须写 Migration；v4 当前有 3 张表 |
| DataStore | `data/local/preferences/UserPreferences.kt` | 新增键要提供默认值；跨进程读取要能被 Widget 使用 |
| Widget | `widget/ScheduleWidgetData.kt`、`widget/Schedule*Widget.kt`、`res/xml/schedule_widget_*.xml` | 尺寸/颜色/圆角一律用 `R.dimen`/`R.color`；详见 [diagnostics/glance-miui-resources.md](diagnostics/glance-miui-resources.md) |
| 提醒 | `MyHebnuApplication.kt`、`worker/ReminderManager.kt`、`worker/*Worker.kt` | 周期任务唯一名不要改；异常要消化为 `Result.success()` |
| 更新检查 | `data/repository/UpdateRepository.kt`、`ui/about/SystemUpdateScreen.kt` | 自动检查用应用内横幅，不发强制通知 |
| 主题/配色 | `ui/theme/Theme.kt`、`Color.kt`、`ui/settings/ColorThemeScreen.kt` | 自定义 seed hue 优先于动态取色；课程色由 hash + seedOffset |
| 学期/周次 | `ui/settings/SemesterSetupViewModel.kt`、`util/SemesterDateUtils.kt`、`ScheduleRepository.fetchWeekDateMapping` | 学期码 `3/12/16`；N2154 映射是权威日期来源 |

## 构建、运行与调试

| 目的 | 命令/操作 | 说明 |
| --- | --- | --- |
| 构建 Debug | `.\gradlew.bat assembleDebug` | 或 Android Studio Build；产物 `app/build/outputs/apk/debug/` |
| 构建 Release | `.\gradlew.bat assembleRelease` | 需 `local.properties` 签名四项 |
| 快速编译检查 | `.\gradlew.bat compileDebugKotlin` | 不含打包，最快发现 Kotlin 错误 |
| 单元测试 | `.\gradlew.bat testDebugUnitTest` | 当前无测试源码 |
| Lint | `.\gradlew.bat lintDebug` | 报告在 `app/build/reports/lint-results-debug.html` |
| 安装 | `adb install -r <apk>` | debug 包名 `com.myhebnu.debug`，可与正式版共存 |
| 应用日志 | `adb logcat -s MyHEBNU:*` | 登录/课表/成绩/考试均有 `MyHEBNU` 日志 |
| Widget 日志 | `adb logcat -s MyHEBNU-Widget:*` | Widget 独立进程 |
| 崩溃日志 | `adb logcat`（**不要加 tag 过滤**） | 需包含 `AndroidRuntime`；空教室闪退曾因过滤过严误诊 |
| 清除数据 | `adb shell pm clear com.myhebnu` | 会清课表/成绩/登录态；仅复现首次启动时用 |

### 调试顺序建议

1. 先看崩溃堆栈（`AndroidRuntime`），再猜原因。
2. 请求失败先看 `AuthInterceptor` 日志：Cookie 是否携带、响应码是否 302/401/403/901。
3. 数据不显示先确认：Room 是否有数据 → DataStore 学期/周次是否正确 → 是否被 HTML 守卫当错误。
4. Widget 异常先看是否用了裸 Int（应全部资源化），再看 DataStore `period_times_json`。

## 工作流约定

以下为项目既有约定（来自上一阶段协作记录），除非用户明确改变，否则沿用：

1. **任务一个一个来**：先出计划 → 用户确认 → 实施 → 用户用 Android Studio 编译 + 真机验收 → 通过后再推送。
2. **构建/验收由用户负责**：代理不自动跑 Gradle；交付时给出明确的验收步骤。
3. **推送**：`gh` 已登录 `cheeemmms`；如 git 凭据失效，用 `gh auth setup-git` 注册 credential helper，再 `git push origin main`。
4. **发布**：bump `versionCode/versionName` → 更新 `CHANGELOG.md` 与 `strings.xml` 的 `changelog_content` → 打 tag → 构建签名 APK → 校验证书一致。
5. **记忆库**：有实质改动后刷新 [readme.md](readme.md) 的当前快照与受影响文件；详细规则见 readme「维护约定」。

## 与上游/原版的差异

- 本项目是自有仓库，不存在上游 fork；无需同步上游。
- `backup/2026-08-30`（`797e0ef`）是 2026-08-30 一次 `git reset --hard v1.3.0` 前对未提交改动的备份分支。后续 main 已继续前进，**不要删除该分支**，直到确认不再需要追溯。
- 历史 `v1.3.0` 标签与名为 “bump to v1.3.0” 的提交不是同一个对象；做任何 reset/checkout 前用 `git log --oneline --decorate v1.3.0` 核对实际提交，避免再次误删改动。

## 未验证的假设与风险

| 项 | 状态 | 验证方法 |
| --- | --- | --- |
| 当前源码可构建 | **已验证**：2026-10-21 `assembleDebug` 通过 | 需要时重跑 `assembleDebug` |
| 本机代理可用 | **已验证**：7897 正在监听，项目 `gradle.properties` 已对齐 | `Test-NetConnection 127.0.0.1 -Port 7897` |
| v1.5.0 之后无回归 | **未验证**：本轮只读代码，未真机 | 按 [implementation-plan.md](implementation-plan.md) 验证表逐项验收 |
| 小学期成绩可查 | **待验证**：小学期未开始，接口/展示方式未知；待暴露后再接入 | 用小学期账号验证或查教务网页 |
| i18n 可用 | **部分，已搁置**：仍有硬编码中文；用户决定暂缓 | 切英文逐页检查 |
| 安全项 | **有风险**：明文 HTTP、Cookie 明文持久化、BODY 日志、备份暴露 | 见 [architecture/security.md](architecture/security.md) |
| README 与实际一致 | **基本一致**：版本徽章/变更日志/许可证/缓存策略/Widget 状态已更新；架构图与技术栈表仍有旧值 | 按 [implementation-plan.md](implementation-plan.md) 任务 16 修 |

> 已验证关闭：考试接口分页——用户实测未出现问题（2026-10-21），不再作为待办。

## 待办与坑

| # | 事项 | 类型 | 优先级 | 线索 |
| --- | --- | --- | --- | --- |
| 1 | 发布构建关闭 BODY 日志并脱敏 Cookie 日志 | 安全·搁置 | — | `NetworkModule.provideOkHttpClient`、`PersistentCookieJar` |
| 2 | Cookie 持久化加密或改为不持久化 | 安全·搁置 | — | `SessionManager` 普通 SharedPreferences |
| 3 | 收紧 `network_security_config`，仅放行教务域名 | 安全·搁置 | — | `res/xml/network_security_config.xml` |
| 4 | 配置备份排除/关闭 `allowBackup` | 安全·搁置 | — | `AndroidManifest.xml` |
| 5 | 移除未使用的 `SCHEDULE_EXACT_ALARM` | 安全·搁置 | — | `AndroidManifest.xml` |
| 6 | 小学期 `16` 成绩接入调研 | 待办 | 中 | 待小学期开始/接口暴露后验证；`GradeRepository.SEMESTER_TERMS` |
| 7 | 补测试/CI | 待办 | 中 | 无 `app/src/test`；JUnit 依赖空置 |
| 8 | README 架构图/技术栈表同步（其余已更新） | 待办 | 中 | README 架构总览、技术栈表 |
| 9 | 课表刷新事务化 | 坑 | 中 | `ScheduleRepository.refreshSchedule` 删后插非事务 |
| 10 | 通知权限未引导 | 坑 | 中 | `ReminderManager` 捕获 `SecurityException` 后静默 |
| 11 | 自研滚轮禁用 | 约定 | 高 | 见 [design-document.md](design-document.md) UI/UX |
| 12 | Compose 嵌套滚动 | 坑 | 高 | 见 [diagnostics/compose-nested-scroll.md](diagnostics/compose-nested-scroll.md) |
| 13 | MIUI Widget 资源化 | 坑 | 高 | 见 [diagnostics/glance-miui-resources.md](diagnostics/glance-miui-resources.md) |

> 安全项 2026-10-21 用户决定**暂时搁置**，保留记录不实施；恢复优先级见 [architecture/security.md](architecture/security.md)。
> 已完成：许可证统一为 AGPL-3.0-only 并补 `LICENSE`；考试分页项关闭；课表长按已修正为“仅成绩页支持长按”的发布说明（课表短按 `refreshSchedule()` 已是完整刷新，长按无独立语义）。完整任务见 [implementation-plan.md](implementation-plan.md)。

## 交接检查清单

- [ ] 已读 [readme.md](readme.md) 与本文
- [ ] 已确认 `local.properties`（SDK/签名）存在且未提交
- [ ] 已启动代理并确认端口 7897（项目已对齐）
- [ ] Debug 构建通过（用户执行）
- [ ] 关键路径手动验证：登录 → 首页 → 课表 → 成绩 → 空教室 → 考试
- [ ] Widget/提醒至少各验证一次（真机）
- [ ] [tech-stack.md](tech-stack.md) 中的命令可复现
- [ ] 未验证项已明确标注，未把推测写成结论
- [ ] 改动已刷新 [readme.md](readme.md) 快照与受影响文件

## 历史记录

- **2026-10-21**：按 memory_bank 规范创建本文件。项目此前没有独立交接文档，状态散落在 `progress.md` 与 README；本次把“来源/上手/地图/约定/风险/待办”集中到此，`progress.md` 的变更过程归档到 [history/change-log.md](history/change-log.md)。同日对齐代理 7897、统一许可证 AGPL-3.0、关闭考试分页、把小学期成绩改为待验证、i18n 搁置、安全项搁置（保留 [architecture/security.md](architecture/security.md)）、修正课表长按发布说明，并跑通 `assembleDebug`。
- **2026-08-30**：执行过 `git reset --hard v1.3.0` 回退；未提交改动保存在 `backup/2026-08-30`。后续 v1.3.1/v1.4.0/v1.5.0 已重新前进。
