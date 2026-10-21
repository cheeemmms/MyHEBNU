# memory_bank · 记忆库总纲

- **用途**：MyHEBNU 项目记忆库的唯一入口，承载懒加载协议、文件清单与当前快照。
- **阅读时机**：任何会话开始处理本项目时，先只读本文件。
- **最后更新**：2026-10-21
- **状态**：稳定

## 目录

- [懒加载协议](#懒加载协议对所有会话生效) — 读取顺序与边界
- [当前快照](#当前快照) — 项目类型、版本、下一步、阻塞
- [文件清单](#文件清单) — 每个文件「何时读」
- [维护约定](#维护约定) — 更新规则
- [历史记录](#历史记录) — 记忆库自身的结构调整

## 懒加载协议（对所有会话生效）

1. 开始任务时**只读本文件**：「当前快照」+「文件清单」通常已足够定位工作。
2. 需要细节时，按「文件清单」的「何时读」一列，按需打开 **1–2 个**具体文件。
3. 不要一次性读取整个 `memory_bank/`，也不要为了「了解全貌」而全量读取。
4. 拆分文件的唯一理由是**主题分化**（一个文件承载多个可独立引用的主题）。不设行数硬上限，不为满足篇幅指标而拆。
5. 内容较长的文件必须在开头提供**小节目录**（锚点 + 一句话说明），让读者直接跳到相关小节。

## 当前快照

| 项目 | 内容 |
| --- | --- |
| 项目 | **MyHEBNU** — 河北师范大学非官方 Android 教务助手（包名 `com.myhebnu`） |
| 项目类型 | 既有仓库 · 接手维护（GitHub `cheeemmms/MyHEBNU`，97 次提交，2026-06-04 起） |
| 当前版本 | **v1.5.0**（versionCode 7，2026-09-13 发布）；2026-10-21 有本地文档/配置提交 |
| 当前阶段 | Phase 0–7（侦察、骨架、认证、课表、成绩、空教室、考试、Widget+提醒）已交付；**Phase 8「打磨/收尾」未完成** |
| 最近一次实质进展 | v1.5.0：成绩/课表缓存优先、悬浮刷新、节次起止时间；2026-10-21 记忆库重整、代理对齐 7897、许可证统一 AGPL-3.0、安全项盘点并搁置；**assembleDebug 构建通过** |
| 下一步 | 见 [implementation-plan.md](implementation-plan.md)：Android Studio 真机验收；Phase 8 收尾（i18n 已搁置）、README 架构图同步、小学期成绩待验证 |
| 已知阻塞 | 无代码/构建阻塞；代理已对齐 `7897`，`assembleDebug` 已通过；当前未连接真机，真机验收由用户完成 |
| 最后更新 | 2026-10-21 |

## 文件清单

### 核心文件

| 文件 | 内容 | 何时读 | 最后更新 |
| --- | --- | --- | --- |
| [architecture.md](architecture.md) | 系统全景、模块边界、关键架构决策与子文档导航 | 改结构、找代码位置、评估影响面 | 2026-10-21 |
| [design-document.md](design-document.md) | 产品目标、用户场景、功能需求与 UI/UX 规范 | 讨论需求、判断「应该做成什么样」 | 2026-10-21 |
| [implementation-plan.md](implementation-plan.md) | 当前里程碑、未完成任务、验证方式与风险 | 排期、接续未完成的活、确认验收标准 | 2026-10-21 |
| [tech-stack.md](tech-stack.md) | 技术栈、版本矩阵、构建/运行命令、本机环境与编码规范 | 配环境、跑构建测试、加依赖、判断该用什么写法 | 2026-10-21 |
| [handover-notes.md](handover-notes.md) | 上手路径、关键文件地图、工作流约定、坑与未决问题 | 刚接手、久未参与、准备交接 | 2026-10-21 |

> 项目按「既有仓库 · 接手维护」维护，因此补齐了 `handover-notes.md`；`design-document.md` 与 `implementation-plan.md` 是新建期留下的长期资产，继续保留并维护。

### 架构子文档

| 文件 | 内容 | 何时读 | 最后更新 |
| --- | --- | --- | --- |
| [architecture/modules-and-ui.md](architecture/modules-and-ui.md) | 包结构、模块职责、导航与页面状态、主题与 DI | 新增页面/模块、改导航或主题 | 2026-10-21 |
| [architecture/data-and-storage.md](architecture/data-and-storage.md) | Room 实体/DAO/迁移、DataStore 键、凭证与 Cookie、缓存策略 | 改数据模型、排查缓存/迁移问题 | 2026-10-21 |
| [architecture/ea-api.md](architecture/ea-api.md) | 教务系统认证、请求门控、课程/成绩/空教室/考试接口与响应结构 | 对接教务、排查请求失败、扩展接口 | 2026-10-21 |
| [architecture/widget-and-reminders.md](architecture/widget-and-reminders.md) | Glance Widget、跨进程数据、WorkManager 提醒、更新检查 | 改 Widget/通知/后台任务 | 2026-10-21 |
| [architecture/security.md](architecture/security.md) | 威胁模型、明文 HTTP、Cookie/日志/备份风险与修复优先级 | 发布前安全检查、处理凭证/隐私问题 | 2026-10-21 |

### 诊断复盘

| 文件 | 内容 | 何时读 | 最后更新 |
| --- | --- | --- | --- |
| [diagnostics/http901-session-expiry.md](diagnostics/http901-session-expiry.md) | HTTP 901 会话失效的根因、日志证据与抓取方法 | 出现 901、会话/自动重登异常 | 2026-10-21 |
| [diagnostics/glance-miui-resources.md](diagnostics/glance-miui-resources.md) | Glance 1.1.1 在 MIUI/HyperOS 上的资源解析坑与修法 | 改 Widget 尺寸/颜色/圆角后异常 | 2026-10-21 |
| [diagnostics/compose-nested-scroll.md](diagnostics/compose-nested-scroll.md) | 空教室闪退的嵌套滚动根因、排查清单与教训 | 出现 Compose measure 崩溃、页面卡死 | 2026-10-21 |

### 历史归档

| 文件 | 内容 | 何时读 | 最后更新 |
| --- | --- | --- | --- |
| [history/change-log.md](history/change-log.md) | 2026-06-04 → 2026-09-13 的阶段/批次/发布变更日志 | 追溯「为什么有这个改动」、写发布说明 | 2026-10-21 |

> 仓库根目录的 [README.md](../README.md) 面向使用者，[CHANGELOG.md](../CHANGELOG.md) 面向版本发布；两者与记忆库分工不同，不互相复制。

## 维护约定

- 做完有实质影响的改动后，至少刷新本文件「当前快照」和受影响的单个文件。
- 每次更新刷新「最后更新」日期（用本机本地日期）。
- 废弃内容直接删除或移入文件末尾的「历史记录」，不要写成「已从 X 改为 Y」的补丁式叙述。
- 文件之间用相对链接互相引用；同一段内容只保留一处。
- 新增主题文件或子目录时，在本文件「文件清单」登记，并写明「何时读」。
- 只写有依据的内容。不确定的标 `待验证`，并注明需要什么才能确定，不猜测。
- 项目既有约定：**构建与真机验收由用户在 Android Studio 完成**，除非用户明确要求，代理不自动执行 Gradle 构建。

## 历史记录

- **2026-10-21**：按团队 memory_bank 规范重整。目录由 `memory-bank/` 改为 `memory_bank/`；`progress.md` 拆分为当前路线图（[implementation-plan.md](implementation-plan.md)）与变更日志（[history/change-log.md](history/change-log.md)）；`architecture.md` 拆为主题子文档；`batch4-requirements.md` 的有效需求并入 [design-document.md](design-document.md)，过程记录归档到变更日志；`HTTP901-诊断与日志采集.md` 与 `glance-api-research.md` 移入 `diagnostics/` 并规范化；新增本入口文件、`tech-stack.md`、`handover-notes.md`、`architecture/security.md`。同日对齐代理端口 7897、统一许可证为 AGPL-3.0、关闭考试分页项、将小学期成绩改为待验证、i18n 搁置、安全项搁置、修正课表长按发布说明。
