# MyHEBNU — 技术架构文档

> 版本: v1.0 | 创建日期: 2026-06-04 | 状态: 草稿

---

## 1. 技术栈全景图

| 层级 | 技术 | 说明 |
|------|------|------|
| **UI** | Jetpack Compose + Material3 | 声明式 UI，Material Design 3 |
| **导航** | Compose Navigation | 类型安全的导航 |
| **状态管理** | ViewModel + StateFlow | MVVM 中的 UiState 管理 |
| **依赖注入** | Hilt (Dagger) | 全应用 DI 容器 |
| **网络层** | Retrofit 2 + OkHttp 4 | HTTP 客户端 + 拦截器 |
| **认证/会话** | OkHttp CookieJar | SSO Cookie 持久化 |
| **HTML 解析** | Jsoup | 教务系统可能返回 HTML |
| **本地数据库** | Room | 课表离线缓存 |
| **键值存储** | DataStore | 偏好设置、凭证存储 |
| **加密存储** | EncryptedSharedPreferences | 敏感凭证加密 |
| **异步** | Kotlin Coroutines + Flow | 协程 + 响应式流 |
| **图表** | Vico | Compose 原生图表（成绩趋势） |
| **图片** | Coil | Compose 原生图片加载 |
| **Widget** | Glance | Jetpack Compose 风格 Widget |
| **通知** | NotificationManager + AlarmManager | 本地通知调度 |
| **最低 SDK** | API 31 (Android 12) | Material You 动态取色 |
| **构建** | Gradle KTS + Version Catalog | 声明式依赖管理 |

---

## 2. 分层架构 (MVVM + Repository)

```
┌─────────────────────────────────────────────────────┐
│                   UI Layer                           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐ │
│  │ Schedule │ │  Grade   │ │  Room    │ │  Exam  │ │
│  │  Screen  │ │  Screen  │ │  Screen  │ │ Screen │ │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └───┬────┘ │
│       │            │            │            │      │
│  ┌────┴────────────┴────────────┴────────────┴────┐ │
│  │              ViewModels (Hilt)                  │ │
│  │  ScheduleVM / GradeVM / RoomVM / ExamVM         │ │
│  │  → exposes UiState via StateFlow               │ │
│  └───────────────────────┬────────────────────────┘ │
├──────────────────────────┼──────────────────────────┤
│              Repository Layer                        │
│  ┌───────────────────────┼────────────────────────┐ │
│  │  ScheduleRepo / GradeRepo / RoomRepo / ExamRepo│ │
│  │  AuthRepo                                       │ │
│  │  → orchestrates Local + Remote data sources     │ │
│  └──────────┬─────────────────────┬───────────────┘ │
├─────────────┼─────────────────────┼─────────────────┤
│     Data Layer                                     │
│  ┌──────────┴──────────┐ ┌────────┴──────────────┐ │
│  │   Local (Room)       │ │  Remote (Retrofit)     │ │
│  │  ┌────────────────┐  │ │  ┌──────────────────┐ │ │
│  │  │ ScheduleDao    │  │ │  │ EASystemApi      │ │ │
│  │  │ CourseDao      │  │ │  │ (Retrofit        │ │ │
│  │  └────────────────┘  │ │  │  Interface)      │ │ │
│  │  ┌────────────────┐  │ │  └──────────────────┘ │ │
│  │  │ DataStore       │  │ │  ┌──────────────────┐ │ │
│  │  │ (Preferences)   │  │ │  │ CookieJar        │ │ │
│  │  └────────────────┘  │ │  │ SessionManager    │ │ │
│  └──────────────────────┘ │  └──────────────────┘ │ │
│                           └───────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

---

## 3. 包结构 (Package Structure)

```
com.myhebnu/
├── MyHebnuApplication.kt           // @HiltAndroidApp
├── MainActivity.kt                  // 唯一 Activity, Compose 宿主
├── di/                             // Hilt 依赖注入模块
│   ├── NetworkModule.kt            // Retrofit, OkHttp 提供
│   ├── DatabaseModule.kt           // Room, DAO 提供
│   └── RepositoryModule.kt         // Repository 绑定
├── data/
│   ├── local/
│   │   ├── db/
│   │   │   ├── AppDatabase.kt      // Room Database
│   │   │   ├── entity/
│   │   │   │   └── CourseEntity.kt // 课程实体
│   │   │   └── dao/
│   │   │       ├── ScheduleDao.kt
│   │   │       └── CourseDao.kt
│   │   └── preferences/
│   │       └── UserPreferences.kt  // DataStore 封装
│   ├── remote/
│   │   ├── EASystemApi.kt          // 教务系统 Retrofit 接口
│   │   ├── dto/                    // 网络响应 DTO
│   │   │   ├── ScheduleDto.kt
│   │   │   ├── GradeDto.kt
│   │   │   ├── RoomDto.kt
│   │   │   └── ExamDto.kt
│   │   └── interceptor/
│   │       ├── AuthInterceptor.kt  // 会话过期检测
│   │       └── CookieInterceptor.kt
│   └── repository/
│       ├── AuthRepository.kt
│       ├── ScheduleRepository.kt
│       ├── GradeRepository.kt
│       ├── RoomRepository.kt
│       └── ExamRepository.kt
├── domain/
│   └── model/                      // 领域模型（UI 层使用）
│       ├── Course.kt
│       ├── Grade.kt
│       ├── EmptyRoom.kt
│       ├── Exam.kt
│       └── Semester.kt
├── ui/
│   ├── theme/
│   │   ├── Theme.kt                // MD3 主题配置
│   │   ├── Color.kt                // 颜色定义
│   │   └── Type.kt                 // 字体定义
│   ├── navigation/
│   │   ├── AppNavigation.kt        // 导航图
│   │   └── DrawerContent.kt        // 侧边抽屉内容
│   ├── schedule/
│   │   ├── ScheduleScreen.kt
│   │   ├── ScheduleViewModel.kt
│   │   └── components/
│   │       ├── WeekViewGrid.kt     // 周视图网格
│   │       ├── CourseCard.kt       // 课程卡片
│   │       └── WeekSelector.kt     // 周次选择器
│   ├── grade/
│   │   ├── GradeScreen.kt
│   │   ├── GradeViewModel.kt
│   │   └── components/
│   │       ├── SemesterSection.kt
│   │       ├── GpaCard.kt
│   │       └── GradeTrendChart.kt
│   ├── room/
│   │   ├── RoomScreen.kt
│   │   ├── RoomViewModel.kt
│   │   └── components/
│   │       ├── FilterPanel.kt
│   │       └── RoomList.kt
│   ├── exam/
│   │   ├── ExamScreen.kt
│   │   ├── ExamViewModel.kt
│   │   └── components/
│   │       └── ExamCard.kt
│   └── auth/
│       ├── LoginScreen.kt
│       └── LoginViewModel.kt
├── widget/
│   └── ScheduleWidget.kt           // Glance Widget 定义
└── worker/
    └── NotificationWorker.kt       // 通知调度 WorkManager
```

---

## 4. 数据模型设计

### 4.1 Room 实体（本地缓存）

```kotlin
// 课程实体 - 仅课表缓存到本地
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String,          // 教务系统课程ID
    val name: String,                     // 课程名称
    val teacher: String,                  // 教师
    val classroom: String,                // 教室
    val dayOfWeek: Int,                   // 星期几 (1-7)
    val startPeriod: Int,                // 开始节次
    val endPeriod: Int,                   // 结束节次
    val startWeek: Int,                   // 起始教学周
    val endWeek: Int,                     // 结束教学周
    val oddEven: String?,                 // "odd"/"even"/null (单双周)
    val color: Int,                       // 课程颜色 (ARGB)
    val semesterId: String,              // 所属学期ID
    val lastUpdated: Long                 // 最后更新时间戳
)
```

### 4.2 DataStore 键值

```text
// UserPreferences
currentSemesterId: String          // 当前学期ID
currentWeek: Int                   // 当前教学周（手动设置）
semesterStartDate: String          // 学期起始日期
isLoggedIn: Boolean                // 是否已登录
selectedLanguage: String           // "zh" | "en"
isDarkMode: Boolean?               // null = 跟随系统
```

### 4.3 领域模型（UI 层）

```kotlin
data class Course(
    val id: String,
    val name: String,
    val teacher: String,
    val classroom: String,
    val dayOfWeek: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val startWeek: Int,
    val endWeek: Int,
    val oddEven: String?,
    val color: Long
)

data class Grade(
    val courseName: String,
    val courseCode: String,
    val credit: Float,
    val score: Float,                  // 综合成绩 (zpcj)
    val teachingClassName: String,      // 教学班名
    val department: String,             // 开课学院
    val semesterId: String,
    val semesterName: String,
    val classId: String,                // jxb_id — 用于查询明细
    val subItems: List<GradeSubItem>? = null  // 成绩明细（需单独请求）
)

data class GradeSubItem(
    val name: String,                   // 子项名称+权重 (xmblmc) 如"课堂表现(20%)"
    val score: String                   // 子项得分 (xmcj)
)

data class EmptyRoom(
    val roomName: String,
    val building: String,
    val floor: String?,
    val capacity: Int?,
    val venueType: String?         // 场地类别
)

data class Exam(
    val courseName: String,
    val date: LocalDate,
    val time: LocalTime?,
    val location: String,
    val seatNumber: String?,
    val examType: String,          // 期末考试/补考/重修
    val daysRemaining: Int         // 距今天数（计算属性）
)
```

---

## 5. 教务系统 API 参考（已通过 HAR 抓包确认）

> ✅ 所有接口已通过 Phase 0 HAR 抓包分析确认。基础 URL: `http://jwgl.hebtu.edu.cn`

### 5.1 学期/校区编码

| 编码 | 值 | 含义 |
|------|-----|------|
| `xnm` | `2024`, `2025` | 学年（2024=2024-2025, 2025=2025-2026） |
| `xqm` | `3` | 第1学期（秋季，9月-1月） |
| `xqm` | `12` | 第2学期（春季，2月-7月） |
| `xqm` | `16` | 暑假/小学期 |
| `xqh_id` | `2` | 校区2（老校区：第一~第六教学楼等） |
| `xqh_id` | `4` | 校区4（裕华校区：公共教学楼A~E座等） |

### 5.2 SSO 认证流程（已确认）

```
① GET  cas.hebtu.edu.cn/cas/v2/getPubKey       → 获取RSA公钥 {modulus, exponent}
② POST cas.hebtu.edu.cn/cas/login               → 提交: username, password(RSA加密), authcode, rememberMe, execution
③ 302 → jwgl.hebtu.edu.cn/sso/zfiotlogin?ticket=ST-xxx
④ 302 → jwgl.hebtu.edu.cn/sso/zfiotlogin       → 换取内部ticket
⑤ 302 → jwgl.hebtu.edu.cn/ticketlogin?uid=xxx&timestamp=xxx&verify=MD5
⑥ 302 → /xtgl/login_slogin.html                → 登录成功，Set-Cookie
⑦ 302 → /xtgl/index_initMenu.html?jsdm=xs      → 初始化菜单，进入首页
```

### 5.3 课表 API

| 接口 | 参数 | 返回 |
|------|------|------|
| `POST /kbcx/xskbcx_cxXsgrkb.html?gnmkdm=N2151` | `xnm, xqm, kzlx=ck` | **课表完整数据** |
| `POST /kbcx/xskbcx_cxRsd.html?gnmkdm=N2151` | `xnm, xqm, xqh_id` | 时段列表（上午/下午/晚上） |
| `POST /kbcx/xskbcx_cxRjc.html?gnmkdm=N2151` | `xnm, xqm, xqh_id` | 节次时间表（1-13节，含起止时间） |
| `POST /kbdy/bjkbdy_cxKbzdxsxx.html?gnmkdm=N2151` | `kbzl=xscx, doType=query` | 课表字段定义元数据 |

**核心响应结构** (`xskbcx_cxXsgrkb.html`):
```
{
  xsxx: { XM, BJMC, ZYMC, XH, NJDM_ID, XNM, XQMMC, ... },
  xqjmcMap: { "1":"星期一", ..., "7":"星期日" },
  xsbjList: [{ xsdm:"01", xsmc:"理论" }, { xsdm:"04", xsmc:"实践" }, ...],
  kbList: [
    { kcmc, xm(教师), cdmc(教室), xqj(星期1-7), jcs(节次如"3-5"),
      zcd(周次如"1-18周"), kcxszc(详细周次文本), kclb(课程类别), ... }
  ],
  sjkList: [ { kcmc, qsjsz(起止周次), qtkcgs(课程格式文本), ... } ]
}
```

### 5.4 成绩 API

| 接口 | 参数 | 返回 |
|------|------|------|
| `POST /cjcx/cjcx_cxXsKcList.html?gnmkdm=N305007` | `xnm, xqm` | **学期课程成绩列表** |
| `POST /cjcx/cjcx_cxXsXmcjList.html?gnmkdm=N305007` | `xnm, xqm, jxb_id` | **单科成绩明细（子项构成）** |
| `POST /cjcx/cjcx_cxXsKccjList.html?gnmkdm=N305007` | `xnm, xqm` | 全科成绩汇总（含子项） |

**核心响应结构** (`cxXsKcList.html`):
```
items: [{ kcmc, kch, xf(学分), zpcj(综合成绩),
          jxbmc(教学班名), kkbmmc(开课学院),
          xnmmc(学年名), xqmmc(学期名), jxb_id, ... }]
```

**成绩明细结构** (`cxXsXmcjList.html`):
```
items: [{ xmblmc:"课堂表现(20%)", xmcj:"100" },
        { xmblmc:"平时作业(30%)", xmcj:"86" },
        { xmblmc:"期末考试(50%)", xmcj:"92" },
        ... ]
```

### 5.5 空教室 API

| 接口 | 参数 | 返回 |
|------|------|------|
| `GET /cdjy/cdjy_cxXqjc.html?gnmkdm=N2155` | `xqh_id, xnm, xqm` | **校区楼栋列表 + 节次列表** |
| `POST /cdjy/cdjy_cxKxcdlb.html?doType=query&gnmkdm=N2155` | `xqh_id, xnm, xqm, cdlb_id, lh, zcd, xqj, jcd, ...` | **空教室查询结果** |
| `POST /pkglcommon/common_cxZcdesc.html?gnmkdm=N2155` | `xnm, xqm, zcd(bitmask)` | 周次描述 (如"14周") |
| `POST /pkglcommon/common_cxJcdesc.html?gnmkdm=N2155` | `jc(bitmask)` | 节次描述 (如"11-11") |

**校区楼栋响应** (`cxXqjc.html`):
```
{
  lhList: [{ XQH_ID, JXLDM(楼代码), JXLMC(楼名称) }, ...],
  jcList: [{ jcmc(节次号), qssj(开始时间), jssj(结束时间), ... }, ...]
}
```

**空教室查询参数说明**:
| 参数 | 示例 | 说明 |
|------|------|------|
| `xqh_id` | `4` | 校区ID |
| `cdlb_id` | `02` | 场地类别（02=多媒体教室） |
| `lh` | `01` | 楼号（对应JXLDM） |
| `zcd` | `8192` | 教学周bitmask（8192=第14周） |
| `xqj` | `1` 或 `1,2,3` | 星期几（可逗号组合） |
| `jcd` | `1024` 或 `1536` | 节次bitmask（1024=第11节） |

**查询结果字段**: `cdmc, cdbh, lh, lch(楼层), zws(座位数), cdlbmc(场地类别), jgmc(建筑名), xqmc(校区名), jxlmc, cdjylx, kszws1`

### 5.6 考试 API

| 接口 | 参数 | 返回 |
|------|------|------|
| `POST /kwgl/kscx_cxXsksxxIndex.html?doType=query&gnmkdm=N358105` | `xnm, xqm` | **考试安排列表** |
| `POST /ksglcommon/common_cxKsmcByXnxq.html?gnmkdm=N358105` | `xnm, xqm` | 考试名称字典 |

**响应字段**: `kcmc(课程), kssj(考试时间"2026-07-15(08:30-10:30)"), cdmc(教室), zwh(座位号), ksmc(考试类型"学期期末考试"), sksj(上课时间), kkxy(开课学院), bj(班级), xm(姓名)`

### 5.7 其他辅助接口

| 接口 | 用途 |
|------|------|
| `GET /xtgl/index_cxYhxxIndex.html` | 用户信息（HTML，含姓名/班级/专业/头像） |
| `GET /xtgl/photo_cxXszp4.html?xh_id=xxx` | 用户头像图片 |
| `POST /xtgl/index_cxWdyy.html` | 子菜单加载 |
| `POST /xtgl/index_cxBczjsygnmk.html` | 菜单点击记录（`gndm=菜单代码`，返回"操作成功！"） |
| `POST /xtgl/zdpz_cxZdpzList.html` | 字典配置数据 |
| `POST /cdjy/cdjy_cxSfkfyuy.html` | 教室是否可预约检查 |

### 5.8 Retrofit 接口设计

```kotlin
interface EASystemApi {
    // === 课表 ===
    @POST("/kbcx/xskbcx_cxXsgrkb.html")
    @FormUrlEncoded
    suspend fun getSchedule(
        @Field("xnm") year: String,
        @Field("xqm") semester: String,
        @Field("kzlx") type: String = "ck",
        @Field("xsdm") studentDept: String = "",
        @Field("kclbdm") courseCategory: String = "",
        @Field("kclxdm") courseType: String = "",
        @Query("gnmkdm") moduleCode: String = "N2151"
    ): Response<ScheduleResponse>

    @POST("/kbcx/xskbcx_cxRjc.html")
    @FormUrlEncoded
    suspend fun getPeriods(
        @Field("xnm") year: String,
        @Field("xqm") semester: String,
        @Field("xqh_id") campusId: String,
        @Query("gnmkdm") moduleCode: String = "N2151"
    ): Response<List<PeriodInfo>>

    // === 成绩 ===
    @POST("/cjcx/cjcx_cxXsKcList.html")
    @FormUrlEncoded
    suspend fun getGradeList(
        @Field("xnm") year: String,
        @Field("xqm") semester: String,
        @Query("gnmkdm") moduleCode: String = "N305007"
    ): Response<GradeListResponse>

    @POST("/cjcx/cjcx_cxXsXmcjList.html")
    @FormUrlEncoded
    suspend fun getGradeDetail(
        @Field("xnm") year: String,
        @Field("xqm") semester: String,
        @Field("jxb_id") classId: String,
        @Query("gnmkdm") moduleCode: String = "N305007"
    ): Response<GradeDetailResponse>

    // === 空教室 ===
    @GET("/cdjy/cdjy_cxXqjc.html")
    suspend fun getCampusInfo(
        @Query("xqh_id") campusId: String,
        @Query("xnm") year: String,
        @Query("xqm") semester: String,
        @Query("gnmkdm") moduleCode: String = "N2155"
    ): Response<CampusInfoResponse>

    @POST("/cdjy/cdjy_cxKxcdlb.html")
    @FormUrlEncoded
    suspend fun getEmptyRooms(
        @Field("xqh_id") campusId: String,
        @Field("xnm") year: String,
        @Field("xqm") semester: String,
        @Field("cdlb_id") venueType: String,
        @Field("lh") building: String,
        @Field("zcd") weekBitmask: String,
        @Field("xqj") dayOfWeek: String,
        @Field("jcd") periodBitmask: String,
        @Query("doType") doType: String = "query",
        @Query("gnmkdm") moduleCode: String = "N2155"
    ): Response<EmptyRoomResponse>

    // === 考试 ===
    @POST("/kwgl/kscx_cxXsksxxIndex.html")
    @FormUrlEncoded
    suspend fun getExams(
        @Field("xnm") year: String,
        @Field("xqm") semester: String,
        @Query("doType") doType: String = "query",
        @Query("gnmkdm") moduleCode: String = "N358105"
    ): Response<ExamResponse>
}
```

---

## 6. 导航与路由

```kotlin
// 顶级路由
sealed class TopLevelRoute(
    val route: String,
    val titleResId: Int,
    val icon: ImageVector
) {
    object Schedule : TopLevelRoute("schedule", R.string.schedule, Icons.Default.CalendarToday)
    object Grade    : TopLevelRoute("grade", R.string.grade, Icons.Default.Grade)
    object Room     : TopLevelRoute("room", R.string.empty_room, Icons.Default.MeetingRoom)
    object Exam     : TopLevelRoute("exam", R.string.exam, Icons.Default.Assignment)
}
```

NavigationDrawer 包含 4 个顶级路由，Schedule 为起始路由。

---

## 7. Widget 架构 (Glance)

```kotlin
class ScheduleWidget : GlanceAppWidget() {
    // 显示当天的课程
    // 从 Room 数据库读取课表数据
    // 更新频率: 手动刷新 + 每天凌晨自动更新
}
```

Widget 大小: 4x2（标准课表卡片），可扩展到 4x4

---

## 8. 通知架构

```
WorkManager (AlarmManager)
  ├── ClassReminderWorker   → 课前 15 分钟
  └── ExamReminderWorker    → 考前 1 天 + 考前 1 小时
```

- 从 Room 课表数据获取上课时间
- 从远程获取考试安排后缓存，计算提醒时间
- 通知渠道: 上课提醒、考试提醒（分类管理）

---

## 9. 错误处理策略

| 场景 | 处理 |
|------|------|
| 网络超时 (15s) | 展示缓存数据 + Snackbar "网络超时" |
| 教务系统 500 | Snackbar "教务系统异常" + 重试按钮 |
| 会话过期 (401/403) | 静默重新登录 → 成功继续 / 失败跳转登录页 |
| 解析失败 | 记录日志 + "数据格式异常" + 建议用户手动查看 |
| 无网络 | 展示缓存 + "离线模式" 提示 |
| 首次启动无缓存 | 骨架屏 → 加载成功展示 / 失败展示空状态+重试 |

---

## 10. 安全性

| 措施 | 说明 |
|------|------|
| HTTPS | OkHttp 仅允许 HTTPS 连接 |
| 凭证加密 | EncryptedSharedPreferences 存储 Cookie/Token |
| 证书固定 | SSL Pinning（若教务系统证书稳定） |
| 日志脱敏 | Release 构建禁用 HTTP 日志，避免泄露 Cookie |

---

> **关联文档**: [[design-document]] | [[implementation-plan]] | [[progress]]
