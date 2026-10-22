# 项目协作约定（MyHEBNU）

> 作用范围：本仓库（`D:\Personal_file\VibeCoding\Program\My-University`）。
> 优先级低于系统、开发者与用户当场指令；与用户级 `~/.dsh/AGENTS.md` 冲突时，以用户当场指令为准。

## 构建、安装与真机验证

- **代理只负责构建与安装，不代替用户做真机点击验收。**
- 每次改动 `app/` 下的代码后：先 `gradlew.bat :app:assembleDebug` 编译通过，再 `adb install -r` 安装到已连接的真机（设备序列号以 `adb devices` 为准，当前为小米 `24129PN74C`）。
- 安装完成后**立即向用户报告**，报告内容至少包含：
  1. 本次改动涉及的页面与预期可见效果（分条列）；
  2. 建议用户重点验证的 3–5 个动作（点哪里、应该看到什么）；
  3. 已知风险或尚未验证的点；
  4. 构建结果（成功/失败）与 APK 路径 `app/build/outputs/apk/debug/MyHEBNU-v1.5.0-debug.apk`。
- **实际测试由用户完成**：用户的反馈（“通过 / 不接受 / 还有问题”）是后续调整的唯一依据；在用户明确说通过之前，不视为验收完成，也不要自行判定“效果达标”。
- 若 `adb devices` 无设备，先说明连接问题并停下等待，不要反复重试或改用其他安装方式。

## 提交

- 只有用户明确说“提交”时才 `git commit`；提交信息沿用 Conventional Commits（`feat/fix/docs/refactor` + 中文描述）。
- 提交前确认工作区不含临时脚手架（如 `app/src/debug/res/values/strings.xml` 这类调试用覆盖资源仅限本地验证）。

## 文档

- `memory_bank/` 是随仓库走的长期文档，入口是 `memory_bank/readme.md`；按其中的懒加载协议读取，只更新真正变化的部分，并刷新“最后更新”日期。
