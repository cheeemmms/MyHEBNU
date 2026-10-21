# ea-api.md · 教务系统对接

- **用途**：记录登录流程、请求门控、各业务接口的参数/响应、会话与错误处理，作为对接教务系统的唯一技术参考。
- **阅读时机**：新增或修改教务接口、排查登录/权限/HTML 错误、理解 901 与自动重登。
- **最后更新**：2026-10-21
- **状态**：稳定（已由 HAR 抓包与真机日志验证；标注“待验证”的除外）

## 目录

- [基础信息](#基础信息)
- [登录与会话](#登录与会话)
- [请求门控与伪装](#请求门控与伪装)
- [学期、校区与编码](#学期校区与编码)
- [业务接口](#业务接口)
- [响应结构](#响应结构)
- [错误处理](#错误处理)
- [逆向证据与已知缺口](#逆向证据与已知缺口)

## 基础信息

| 项 | 值 |
| --- | --- |
| 教务基础 URL | `http://jwgl.hebtu.edu.cn/`（明文 HTTP） |
| 登录页 | `http://jwgl.hebtu.edu.cn/xtgl/login_slogin.html?ydType=0` |
| CAS 域名 | `http://cas.hebtu.edu.cn/`（`CasApi`/CAS Retrofit 仍存在，但**当前登录链路不依赖**） |
| 协议 | 全部 Form 表单 + JSON/HTML 响应；主 OkHttp `followRedirects=false`，重定向手动处理 |
| 超时 | 连接/读取/写入各 15 秒 |
| 请求头 | `User-Agent`（Chrome Mobile）、`X-Requested-With: XMLHttpRequest`、`Accept: */*`、`Origin`、按模块生成的 `Referer` |

## 登录与会话

### 自定义登录流程（`AuthRepository.performLogin`）

```text
Step 0  POST /xtgl/login_logoutAccount.html       仅首次尝试；验证码重试时跳过
Step 1  GET  /xtgl/login_slogin.html              解析隐藏域 csrftoken（Jsoup）
Step 2  GET  /xtgl/login_getPublicKey.html?time=  { modulus, exponent }（Base64）
Step 3  RSA 加密密码
Step 4  POST /xtgl/login_slogin.html?time=        csrftoken + yhm + mm + yzm?
         ├─ 302 → 成功；Cookie 已由 CookieJar 保存
         │        再 GET login_slogin.html 走完重定向链
         │        保存加密凭证、is_logged_in=true、清除过期标记
         └─ 200 → 失败；解析 #tips / .alert-danger 文案
                  若响应含 yzmDiv / id="yzm" → 需要验证码
```

- **密码加密**：`base64(modulus) → hex → BigInteger → RSAPublicKey → encrypt → hex → base64`（`CryptoUtil`）。
- **验证码**：`GET /kaptcha?time=`，必须用与登录相同的 `loginClient`（共享 CookieJar），否则验证码与登录会话不匹配。
- **凭证保存**：`CredentialManager`（EncryptedSharedPreferences）；`autoLogin()` 在会话过期时用其重新登录。
- **会话校验**：`GET /xtgl/index_initMenu.html?jsdm=xs`；若 302 到 `login_slogin` 则视为失效并 `logout()`。
- **登出**：清凭证、WebView Cookie、CookieJar、`is_logged_in`；不清课表/成绩缓存。

> 旧版文档中的 CAS `getPubKey → cas/login → zfiotlogin → ticketlogin` 流程已被自定义直登取代；不要再按 CAS 流程改代码。

### 会话失效信号

| 信号 | 处理 |
| --- | --- |
| `302` 且 `Location` 含 `cas/login` 或 `login_slogin` | 置 `sessionExpired=true` |
| `401` / `403` | 同上 |
| **`901`**（ZFSOFT 自定义码，非标准 HTTP） | 同上；详见 [../diagnostics/http901-session-expiry.md](../diagnostics/http901-session-expiry.md) |

`MainActivity` 观察 `AuthRepository.sessionExpired` → `autoLogin()`；成功无感恢复，失败跳登录页并提示。

## 请求门控与伪装

ZFSOFT 会拒绝非浏览器 AJAX。每个业务模块的请求必须按三步序列：

```text
① POST /xtgl/index_cxBczjsygnmk.html?gnmkdm=index   body: gndm={模块代码}
② GET  {模块页面}.html                              建立该功能的浏览器上下文
③ POST {模块数据 API}.html                          取数据
```

缺第②步 → 数据 API 返回 `text/html` 的“无功能权限”（HTTP 200），客户端用 HTML 守卫识别并报错。

| 模块 | gndm | 页面 | 数据 API |
| --- | --- | --- | --- |
| 课表 | `N2151` | `/kbcx/xskbcx_cxXskbcxIndex.html` | `/kbcx/xskbcx_cxXsgrkb.html` |
| 成绩 | `N305007` | `/cjcx/cjcx_cxXsKcList.html`（Repository 实际加载） | `/cjcx/cjcx_cxXsKcList.html` |
| 空教室 | `N2155` | `/cdjy/cdjy_cxKxcdlb.html` | `/cdjy/cdjy_cxKxcdlb.html` |
| 考试 | `N358105` | `/kwgl/kscx_cxXsksxxIndex.html` | `/kwgl/kscx_cxXsksxxIndex.html` |
| 周次 | `N2154` | `/kbcx/xskbcxZccx_cxXskbcxIndex.html` | `/kbcx/xskbcxZccx_cxZcByXnxq.html` |

> 注意：`AuthInterceptor` 为 `/cjcx/` 请求统一设置 `Referer=http://host/cjcx/cjcx_cxXskbcxIndex.html?...`，而 Repository 加载的页面是 `cjcx_cxXsKcList.html`。当前可工作，但属于历史遗留；修改成绩链路时先验证 Referer 是否仍是必需值。

### 请求头要求

| 请求头 | 值 | 说明 |
| --- | --- | --- |
| `User-Agent` | Chrome Mobile UA（`AuthInterceptor.UA`） | 绝不能暴露 `okhttp` |
| `X-Requested-With` | `XMLHttpRequest` | 教务用此判断 AJAX |
| `Accept` | `*/*` | 不能只写 JSON |
| `Origin` | `http://{host}` | 同域 POST 也需携带 |
| `Referer` | 对应功能页 URL | 错模块 Referer → “无功能权限” |

### Cookie 规则

- 使用 `JSESSIONID` + `jw` 双 Cookie（另有 `rememberMe`）。
- `saveFromResponse()` 必须**按 host + name 合并**，不能整体替换：302 响应可能只回 `jw`，替换会丢掉 `JSESSIONID`。
- `CookieManager.getCookie()` 必须传完整 URL（`http://host/`）而非裸域名。
- 登录成功后建议走完重定向链再发起业务请求，确保 Cookie 完整。

## 学期、校区与编码

| 参数 | 值 | 含义 |
| --- | --- | --- |
| `xnm` | `2025` 等 | 学年，`2025` = 2025–2026 |
| `xqm` | `3` | 第一学期（秋，9 月–次年 1 月） |
| `xqm` | `12` | 第二学期（春，2 月–7 月） |
| `xqm` | `16` | 第三学期 / 小学期（教务下拉已内置） |
| `xqh_id` | `2` | 老校区 |
| `xqh_id` | `4` | 裕华校区（默认） |

- 下一学期推算：`(y,3) → (y,12) → (y,16) → (y+1,3)`。
- 当前学期识别：`guessCurrentSemester()` 按月份判断；6 月 20 日后至 7 月为小学期 `16`，8–12 月为秋 `3`，其余为春 `12`；再由 N2154 校验兜底。
- 学期下拉是服务器渲染的 HTML `<select>`，无独立 JSON 接口；不要尝试解析 HTML 取列表，使用固定枚举更稳。

## 业务接口

### 课表

| 接口 | 方法/路径 | 关键参数 | 返回 |
| --- | --- | --- | --- |
| 页面 | `GET /kbcx/xskbcx_cxXskbcxIndex.html` | `gnmkdm=N2151&layout=default` | HTML |
| 课表数据 | `POST /kbcx/xskbcx_cxXsgrkb.html` | `xnm, xqm, kzlx=ck, xsdm, kclbdm, kclxdm` | JSON |
| 节次时间 | `POST /kbcx/xskbcx_cxRjc.html` | `xnm, xqm, xqh_id` | JSON 数组 `[{jcmc,qssj,jssj}]` |
| 周次映射 | `POST /kbcx/xskbcxZccx_cxZcByXnxq.html` | `xnm, xqm` | 周次→日期（HTML/JSON，按原始文本解析） |

课表 JSON 核心字段：`xsxx`（学生信息，含 `XM`）、`xqjmcMap`、`xsbjList`、`kbList`、`sjkList`。
`kbList` 每条：`kcmc`（课程）、`xm`/`jsxm`（教师）、`cdmc`（教室）、`xqj`（1–7）、`jcs`（如 `3-5`）、`zcd`（如 `1-18周`）、`kclb`（类别）。

### 成绩

| 接口 | 方法/路径 | 关键参数 | 返回 |
| --- | --- | --- | --- |
| 页面 | `GET /cjcx/cjcx_cxXsKcList.html` | `gnmkdm=N305007&layout=default` | HTML |
| 学期成绩 | `POST /cjcx/cjcx_cxXsKcList.html` | `xnm, xqm, queryModel.showCount=1000, queryModel.currentPage=1` | JSON `items[]` |
| 成绩明细 | `POST /cjcx/cjcx_cxXsXmcjList.html` | `xnm, xqm, jxb_id` | JSON `items[]` |

- 列表字段：`kcmc, kch, xf, zpcj, jxbmc, kkbmmc, xnmmc, xqmmc, jxb_id`。
- 明细字段：`xmblmc`（子项名+权重，如“课堂表现(20%)”）、`xmcj`（得分）。
- **必须显式传 `queryModel.showCount`**：ZFSOFT 列表默认只返首页约 10 条（空教室曾踩坑 #7a，成绩 #27 同因）。

### 空教室

| 接口 | 方法/路径 | 关键参数 | 返回 |
| --- | --- | --- | --- |
| 页面 | `GET /cdjy/cdjy_cxKxcdlb.html` | `gnmkdm=N2155` | HTML |
| 校区/楼栋/节次 | `GET /cdjy/cdjy_cxXqjc.html` | `xqh_id, xnm, xqm` | JSON `lhList`, `jcList` |
| 空教室查询 | `POST /cdjy/cdjy_cxKxcdlb.html` | `xqh_id, xnm, xqm, cdlb_id, lh, zcd, xqj, jcd, queryModel.showCount, queryModel.currentPage` | JSON `items[]`, `totalPage` |
| 周次描述 | `POST /pkglcommon/common_cxZcdesc.html` | `xnm, xqm, zcd`（bitmask） | 文本 |
| 节次描述 | `POST /pkglcommon/common_cxJcdesc.html` | `jc`（bitmask） | 文本 |

- `zcd`、`jcd` 是位掩码：如 `8192` = 第 14 周，`1024` = 第 11 节。
- 结果字段：`cdmc, cdbh, lh, lch, zws, cdlbmc, jgmc, xqmc, jxlmc, cdjylx, kszws1`。

### 考试

| 接口 | 方法/路径 | 关键参数 | 返回 |
| --- | --- | --- | --- |
| 考试列表 | `POST /kwgl/kscx_cxXsksxxIndex.html?doType=query` | `xnm, xqm, ksmcdmb_id, kch, kc, ksrq, kkbm_id` | JSON `items[]` |

- 字段：`kcmc, kssj`（如 `2026-07-15(08:30-10:30)`）、`cdmc, zwh, ksmc, sksj, kkxy, bj, xm, sjbh, cdxqmc, jsxx, ksfs`。
- 分页：当前未传 `queryModel.showCount`/`currentPage`；用户实测未出现问题（2026-10-21），暂不修改。若将来考试条数异常偏少，再回头补分页参数。

### 登录相关

| 接口 | 方法/路径 | 说明 |
| --- | --- | --- |
| 登出旧账号 | `POST /xtgl/login_logoutAccount.html` | 仅首次登录尝试；验证码重试跳过 |
| 登录页/CSRF | `GET /xtgl/login_slogin.html` | 解析 `input[name=csrftoken]` |
| RSA 公钥 | `GET /xtgl/login_getPublicKey.html?time=` | `{modulus, exponent}` Base64 |
| 登录 | `POST /xtgl/login_slogin.html?time=` | 302 成功 / 200 失败 |
| 验证码 | `GET /kaptcha?time=` | 与登录共享 CookieJar |
| 会话校验 | `GET /xtgl/index_initMenu.html?jsdm=xs` | 302 到登录页 = 失效 |

## 响应结构

### 课表 `xskbcx_cxXsgrkb.html`

```json
{
  "xsxx": { "XM": "姓名", "BJMC": "班级", "ZYMC": "专业", "XH": "学号", "XNM": "2025" },
  "xqjmcMap": { "1": "星期一", "7": "星期日" },
  "kbList": [
    { "kcmc": "课程", "xm": "教师", "cdmc": "教室", "xqj": "1",
      "jcs": "3-5", "zcd": "1-18周", "kclb": "理论" }
  ]
}
```

### 成绩

```json
{ "items": [
  { "kcmc": "课程", "kch": "代码", "xf": "3.0", "zpcj": "92",
    "jxbmc": "教学班", "kkbmmc": "学院", "xnmmc": "2025-2026",
    "xqmmc": "1", "jxb_id": "..." }
] }
```

```json
{ "items": [
  { "xmblmc": "课堂表现(20%)", "xmcj": "100" },
  { "xmblmc": "期末考试(50%)", "xmcj": "86" }
] }
```

### 空教室

```json
{
  "lhList": [ { "XQH_ID": "4", "JXLDM": "01", "JXLMC": "公共教学楼A座" } ],
  "jcList": [ { "jcmc": "1", "qssj": "08:00", "jssj": "08:40" } ],
  "items": [ { "cdmc": "A101", "lch": "1", "zws": "60", "cdlbmc": "多媒体教室" } ],
  "totalPage": 3
}
```

## 错误处理

| 场景 | 表现/处理 |
| --- | --- |
| 会话过期 | 302/401/403/901 → `sessionExpired` → autoLogin；失败回登录页 |
| 门控失败 | 返回 HTML“无功能权限” → Repository 检测 `<!doctype`/`<html`/“无功能权限” → `Result.failure` |
| 网络超时 | 15 秒超时 → `Result.failure`；UI 显示错误/缓存 |
| 接口 500 | `Result.failure("HTTP 5xx")`，UI 提示重试 |
| 解析失败 | 记录 `MyHEBNU` 日志，返回空列表或失败；不崩溃 |
| 列表分页 | 显式 `queryModel.showCount`；空教室还有 `currentPage/totalPage` |
| 考试页 901 | 2026-08-30 前会无限 901；现由拦截器识别并自愈，见 [../diagnostics/http901-session-expiry.md](../diagnostics/http901-session-expiry.md) |

## 逆向证据与已知缺口

- **抓包证据**：`mitm/`、`HAR/`（gitignore，未入库）；2026-06-05 用 mitmproxy 对比手机浏览器与 App；2026-07-17 解析 9 个 HAR 确认学期下拉为 HTML `<select>`、`16` 已内置；2026-08-30 用 adb 日志确认 901 根因。
- **已知缺口**：
  1. 成绩全量刷新只查 `3/12`，未覆盖 `16`（小学期未开始，待验证）。
  2. `CasApi` 与 CAS Retrofit 仍在 DI 中，但登录流程未使用；清理前需确认无其他调用。
  3. 明文 HTTP + `HttpLoggingInterceptor.Level.BODY` 会在日志中输出 Cookie；发布构建应关闭 BODY，详见 [security.md](security.md)。
