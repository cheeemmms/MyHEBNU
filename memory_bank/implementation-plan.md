# implementation-plan.md · 实施计划

- **用途**：记录当前里程碑、未完成任务、验证方式与风险，让下一次会话能无缝接续。
- **阅读时机**：排期、接续未完成的活、确认验收标准、复盘进度。
- **最后更新**：2026-10-21
- **状态**：稳定（当前主线：Phase 8 收尾）

## 目录

- [里程碑总览](#里程碑总览)
- [任务清单](#任务清单)
- [验证方式](#验证方式)
- [风险与缓解](#风险与缓解)
- [未决问题](#未决问题)
- [历史记录](#历史记录)

状态口径：**待办** = 未开始；**进行中** = 已动手但未验证；**完成** = 已按验证方式确认；**搁置** = 主动暂停。

## 里程碑总览

| 里程碑 | 目标 | 状态 | 完成判定 |
| --- | --- | --- | --- |
| Phase 0–7 | 侦察、骨架、认证、课表、成绩、空教室、考试、Widget + 提醒 | 完成 | v1.0.0–v1.3.x 已发布并真机验收 |
| v1.4.0 | 学期设置、考试学期收敛、课表表头月日 | 完成 | 2026-09-04 发布 |
| v1.5.0 | 成绩/课表缓存优先、悬浮刷新、节次起止时间 | 完成 | 2026-09-13 发布，versionCode 7 |
| M8 | Phase 8：国际化、无障碍、错误处理、离线、性能 | 待办 | 全部子项验收 + 版本发布 |
| M9 | 发布前工程收尾 | 待办 | 许可证/README/日志/安全项对齐 |

> 发布历史与每次发布的校验信息见 [history/change-log.md](history/change-log.md) 与仓库根 [CHANGELOG.md](../CHANGELOG.md)。

## 任务清单

### P0 · 安全项（2026-10-21 暂时搁置）

| # | 任务 | 状态 | 阻塞 | 关联文件 |
| --- | --- | --- | --- | --- |
| 1 | **发布构建关闭 BODY 日志并脱敏**：`HttpLoggingInterceptor.Level.BODY` 改为仅 Debug 或 `NONE`；移除完整 Cookie / `csrftoken` 日志 | 搁置 | 无 | `di/NetworkModule.kt`、`PersistentCookieJar.kt`、`AuthRepository.kt` |
| 2 | **Cookie 持久化加密**：`SessionManager` 从普通 SharedPreferences 迁到 EncryptedSharedPreferences，或改为不持久化 | 搁置 | 需评估自动登录体验 | `data/remote/SessionManager.kt` |
| 3 | **备份规则**：`allowBackup=true` 增加 `dataExtractionRules`/`fullBackupContent`，排除凭证、Cookie 与偏好 | 搁置 | 无 | `AndroidManifest.xml`、`res/xml/` |
| 4 | **收紧明文配置**：`network_security_config` 的 `base-config` 改为 `false`，仅放行教务域名；评估去掉 `usesCleartextTraffic` | 搁置 | 需确认教务是否支持 HTTPS | `res/xml/network_security_config.xml`、`AndroidManifest.xml` |
| 5 | **权限最小化**：移除未使用的 `SCHEDULE_EXACT_ALARM` | 搁置 | 无 | `AndroidManifest.xml` |

> 安全项完整说明与威胁模型见 [architecture/security.md](architecture/security.md)。2026-10-21 用户决定暂时搁置，恢复时按该文档的优先级执行。

### P1 · Phase 8 收尾

| # | 任务 | 状态 | 验收标准 | 关联文件 |
| --- | --- | --- | --- | --- |
| 6 | Phase 8.1 全量国际化 | **搁置**（2026-10-21 用户决定） | 恢复时：`values-en` 与默认条目一致；界面无硬编码中文；中英切换后主要页面无截断 | `res/values*/strings.xml`、各 Screen |
| 7 | Phase 8.2 无障碍 | 待办 | TalkBack 可读完主要操作；大字体不截断；对比度符合 WCAG AA；Glance 限制有说明 | 各 Screen、`ui/theme/` |
| 8 | Phase 8.3 错误处理审计 | 待办 | 所有网络失败有可读文案 + 重试；会话失效统一走 autoLogin；无裸 `HTTP xxx` 直出 | `data/repository/*`、各 ViewModel |
| 9 | Phase 8.4 离线体验 | 待办 | 考试页离线可读 Room 缓存；空教室无网有明确提示；首页/课表/成绩断网可用 | `ExamViewModel`、`RoomViewModel` |
| 10 | Phase 8.5 性能 | 待办 | 冷启动首页首帧 < 500ms（中端机）；滚动无明显掉帧；有可复现的测量记录 | `HomeViewModel`、`ScheduleScreen`、`WeekViewGrid` |

### P2 · 工程与一致性

| # | 任务 | 状态 | 说明 |
| --- | --- | --- | --- |
| 11 | 小学期成绩接入调研 | 待验证（暂不实施） | 小学期未开始，接口/展示方式未知。触发条件：教务网页或成绩接口出现 `16` 学期/小学期成绩入口；届时先用 HAR/网页确认请求参数与展示结构，再决定是否把 `SEMESTER_TERMS` 扩为 `[3,12,16]`。当前不猜测、不改代码 |
| 12 | 测试与 CI | 待办 | 当前无测试源码/CI；至少为 GPA 计算、周次解析、日期工具加 JVM 单测，并接入 GitHub Actions |
| 13 | Room schema 导出 | 待办 | `exportSchema=false`，无法 review 迁移；改为导出到 `schemas/` |
| 14 | 清理未使用依赖 | 待办 | `navigation-compose` 未使用；`CasApi`/CAS Retrofit 未参与登录链路 |
| 15 | 课表刷新事务化 | 待办 | `deleteBySemester` + `upsertAll` 改为 DAO `@Transaction replaceSemester` |
| 16 | README 同步 | 待办 | 变更日志/许可证/版本徽章已更新；剩架构图与技术栈表旧值（如 Compose BOM 年代、旧类名） |
| 17 | 通知权限引导 | 待办 | Android 13+ 未授权时提醒静默失败；设置页增加引导入口 |

### 已完成 / 已关闭

| 项 | 结果 | 日期 |
| --- | --- | --- |
| 统一许可证 | 统一为 **AGPL-3.0-only**，补 `LICENSE`，README 徽章同步，应用内 About 原本即 AGPL | 2026-10-21 |
| 考试接口分页 | 用户实测未出现问题，关闭该项；代码保持现状 | 2026-10-21 |
| 代理端口 | `gradle.properties` 对齐本机实际监听 `127.0.0.1:7897` | 2026-10-21 |
| 课表刷新长按 | 已证实课表页未接长按；但课表短按 `refreshSchedule()` 已是完整刷新（重新探测学期/周次/节次/课程），长按无独立语义。已修正 `CHANGELOG.md` 与应用内 `changelog_content`，明确“仅成绩页支持长按” | 2026-10-21 |

## 验证方式

> 项目既有约定：**构建与真机验收由用户在 Android Studio 完成**，代理不自动执行 Gradle。下表「最近结果」如实记录，不代替验收。

| 验证项 | 命令/步骤 | 期望结果 | 最近一次结果 |
| --- | --- | --- | --- |
| 代理端口 | `Test-NetConnection 127.0.0.1 -Port 7897` | True | 2026-10-21：True；`gradle.properties` 已对齐 |
| Debug 构建 | `.\gradlew.bat assembleDebug` | `BUILD SUCCESSFUL`；`app/build/outputs/apk/debug/MyHEBNU-v1.5.0-debug.apk` | 未执行（2026-10-21 文档整理；按约定由用户构建） |
| Release 构建 | `.\gradlew.bat assembleRelease` | 签名 APK；包名 `com.myhebnu`；versionCode 7 | 未执行（同上） |
| 单元测试 | `.\gradlew.bat testDebugUnitTest` | 通过（当前无测试源码） | 未执行 |
| 静态检查 | `.\gradlew.bat lintDebug` | 无 error 级问题 | 未执行 |
| 安装到设备 | `adb install -r app\build\outputs\apk\debug\MyHEBNU-v1.5.0-debug.apk` | 安装成功（debug 包名 `com.myhebnu.debug`） | 未执行（当前无连接设备） |
| 冷启动 | 清后台后启动，观察首页 | 无白闪；首页直接出卡片；无网络等待 | 未执行 |
| 课表缓存优先 | 断网后进课表 | 立即显示缓存；不出现加载圈长期停留 | 未执行 |
| 会话自愈 | 使会话失效后进考试/成绩页 | 自动重登，不出现 HTTP 901/302 死循环 | 未执行 |
| Widget | 添加 2×2/4×2/4×4 | 显示正确；跨日/周末状态正确；节次时间与 App 一致 | 未执行（需真机） |
| 提醒 | 造一节 15 分钟内开始的课 | 收到通知且仅一次 | 未执行（需真机） |
| 日志 | `adb logcat -s MyHEBNU:*` | 关键链路有日志；无敏感信息（发布前需关闭 BODY） | 未执行 |

## 风险与缓解

| 风险 | 影响 | 缓解 |
| --- | --- | --- |
| 教务改版/改编码 | 接口或学期码失效 | 接口/编码集中；保留 HAR 抓包方法；改动前先抓包确认 |
| 明文 HTTP 与 BODY 日志 | 密码/ Cookie/会话泄露 | 发布构建关闭 BODY；收紧 network config；不分享未脱敏日志；详见 [architecture/security.md](architecture/security.md) |
| Cookie 普通存储 | 设备被 root/备份时泄露 | EncryptedSharedPreferences 或减少持久化 |
| 小学期 `16` 成绩未接入 | 若小学期成绩有独立展示，可能不显示 | 待小学期开始/接口暴露后验证；不按当前缺陷处理，也不预先猜测接口 |
| 无自动化测试 | 改动回归成本高 | 先补 GPA/日期/解析单测，再考虑 CI |
| 手工构建依赖用户 | 代理无法独立验证 | 每轮交付附「用户验收步骤」；构建结果写回本文件 |
| MIUI 差异 | Widget/通知在部分机型异常 | 保留资源化写法；真机回归；参考 [diagnostics/glance-miui-resources.md](diagnostics/glance-miui-resources.md) |

## 未决问题

1. **小学期 `16` 成绩**：小学期尚未开始，接口/展示方式未知；待教务暴露后再决定是否把 `SEMESTER_TERMS` 扩为 `[3,12,16]`。
2. **i18n 范围（已搁置）**：用户 2026-10-21 决定暂缓；恢复时需明确是全部界面英文，还是仅系统文案/通知。
3. **Cookie 加密 vs 自动登录**：加密迁移可能影响已安装用户的会话恢复，需要迁移策略。
4. **考试页是否改 cache-first**：与 Phase 8.4 一起定。
5. **课表刷新长按**：已证实课表未接长按；需确认是需求缺口还是仅成绩需要长按。
6. **Phase 8 是否单独发版**：作为 v1.6.0 还是并入 2.0.0（当前无版本规划）。

## 历史记录

- **2026-10-21**：本文件由旧 `implementation-plan.md`（Phase 0–8 + Batch 1–5 的完整历史计划，427 行）重写为「当前路线图」。已完成阶段的交付物与决策记录压缩到 [history/change-log.md](history/change-log.md)，原始细节保留在 Git 历史与 `progress.md` 旧版本（`git log -- memory-bank/progress.md`）。
- **2026-10-21（第二轮）**：代理对齐 7897；许可证统一为 AGPL-3.0 并补 `LICENSE`；关闭考试分页项（用户实测正常）；小学期成绩改为“待验证/暂不实施”；i18n 标记搁置；新增安全项 P0 与 [architecture/security.md](architecture/security.md)。
- **2026-06-04 ~ 2026-09-13**：Phase 0–7 与 Batch 1–10 全部完成；v1.0.0–v1.5.0 依次发布。详见变更日志。
