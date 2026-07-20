package com.myhebnu.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.local.db.entity.CourseEntity
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.repository.PeriodTime
import com.myhebnu.data.repository.ScheduleRepository
import com.myhebnu.ui.theme.ColorPreset
import com.myhebnu.ui.theme.builtInPresets
import com.myhebnu.ui.theme.findPresetById
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class ScheduleUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isCached: Boolean = false,
    val courses: List<CourseEntity> = emptyList(),
    val filteredCourses: List<CourseEntity> = emptyList(),  // 按周过滤后的课程
    val currentWeek: Int = 1,
    val displayWeek: Int = 1,
    val semesterYear: String = "2025",
    val semesterTerm: String = "12",
    val error: String? = null,
    // Current active course (highlighted)
    val activeCourseId: String? = null,
    // Day labels
    val dayLabels: List<String> = listOf("一", "二", "三", "四", "五"),
    // Period labels and time ranges
    val periodLabels: List<PeriodInfo> = emptyList(),
    // Course tonal palettes (name → palette)
    val coursePalettes: Map<String, com.myhebnu.ui.theme.CourseTonalPalette> = emptyMap(),
    // Course detail BottomSheet
    val selectedCourse: CourseEntity? = null,
    // #29 下学期课表悬浮面板（三态复用同一 MD3 卡片；不持久化，每次进页重算）
    val panelMode: SemesterPanelMode = SemesterPanelMode.NONE,
    val nextTermLabel: String = "",
    val queryingNextSemester: Boolean = false,
    val nextSemesterCoursesLoaded: Boolean = false,
    val nextSemesterUnavailable: Boolean = false,
    val lastWeek: Int = 20
)

/**
 * #29 悬浮面板三态。同一 MD3 卡片组件按此切换文案与按钮，保持视觉一致：
 * - [DISCOVERY]  学期末窗口内、下学期未缓存、看本学期 → 「本学期已结束」+ 暂时忽略 / 查询课表
 * - [REENTRY]    下学期已缓存、看本学期            → 「下学期课表已公布，是否查看？」+ 暂时忽略 / 查看
 * - [BACK]       正在查看下学期                   → 「正在查看下学期课表」+ 返回本学期
 * - [NONE]       不显示
 */
enum class SemesterPanelMode { NONE, DISCOVERY, REENTRY, BACK }

data class PeriodInfo(
    val label: String,         // "1-2", "3-4", etc.
    val startPeriod: Int,      // 1
    val endPeriod: Int,        // 2
    val startTime: String,     // "08:00"
    val endTime: String,       // "09:40"
    val timeRange: String      // "08:00-09:40" (kept for backward compat)
)

// Day labels for the schedule grid. Weekend columns are shown only when the
// user enables "显示周末列"; otherwise only weekdays are displayed.
private val WEEKDAY_DAY_LABELS = listOf("一", "二", "三", "四", "五")
private val FULL_DAY_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: ScheduleRepository,
    private val preferences: UserPreferences,
    private val widgetUpdateManager: com.myhebnu.widget.WidgetUpdateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    // Separate Flow to drive combine — avoids potential races when _uiState
    // is updated concurrently from the outer coroutine and the combine collector.
    private val _displayWeek = MutableStateFlow(1)

    // #29 展示学期驱动流——默认=detect 结果；查询/查看下学期时切换。
    // 全程不碰全局 currentSemester（仅 detectAndApplySemester 写入）。
    private val _viewYear = MutableStateFlow("2025")
    private val _viewTerm = MutableStateFlow("12")

    // #29 真实本学期（detect 结果）——与展示学期区分；viewingNext = 二者不一致。
    private val _currentYear = MutableStateFlow("2025")
    private val _currentTerm = MutableStateFlow("12")
    // 真实当前周（切到下学期展示时用 0 占位，返回本学期时恢复）。
    private var realCurrentWeek: Int = 1
    // 学期末窗口是否处于活动状态（loadInitialData 依末周周日+估算开学日算好）。
    private var endWindowActive: Boolean = false
    // 「暂时忽略」为会话内内存态——每次进页 onScheduleEntered() 重置为 false。
    private var panelDismissedThisSession: Boolean = false

    // Reactive color preferences for course card palettes
    private data class ColorPrefs(
        val seedHue: Float?,
        val isDark: Boolean
    )

    private data class CombinedSchedule(
        val allCourses: List<CourseEntity>,
        val filtered: List<CourseEntity>,
        val colorPrefs: ColorPrefs,
        val dayLabels: List<String>
    )

    private val colorPrefsFlow: Flow<ColorPrefs> = combine(
        preferences.useCustomColors,
        preferences.activePresetId,
        preferences.customPresetsJson,
        preferences.themeMode
    ) { useCustom, presetId, presetsJson, themeMode ->
        val seedHue = if (useCustom && presetId != null) {
            findPresetById(presetId, parsePresetsJson(presetsJson))?.seedHue
        } else null
        val isDark = when (themeMode) {
            "dark" -> true
            "light" -> false
            else -> _systemIsDark.value  // "system" → follow device setting
        }
        ColorPrefs(seedHue = seedHue, isDark = isDark)
    }

    // System dark mode state — updated from composable layer
    private val _systemIsDark = MutableStateFlow(false)

    fun updateSystemDarkMode(isDark: Boolean) {
        _systemIsDark.value = isDark
    }

    private val today: LocalDate = LocalDate.now()
    private val currentTime: LocalTime = LocalTime.now()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // ① 探测/切换学期
            val (year, term) = detectAndApplySemester()
            _viewYear.value = year
            _viewTerm.value = term
            _currentYear.value = year
            _currentTerm.value = term

            // ② 从 N2154 API 获取周次日期映射
            val weekMappingResult = repository.fetchWeekDateMapping(year, term)

            // ③ 自动计算当前周
            val today = LocalDate.now()
            val autoWeek = weekMappingResult.fold(
                onSuccess = { mapping ->
                    mapping.entries.find { (_, dateRange) ->
                        val (start, end) = parseDateRange(dateRange)
                        today in start..end
                    }?.key ?: preferences.currentWeek.first()
                },
                onFailure = { preferences.currentWeek.first() }
            )
            preferences.setCurrentWeek(autoWeek)
            realCurrentWeek = autoWeek

            // 实际末周（用于触发"下学期"提示，稳健于硬编码20周）
            val lastWeek = weekMappingResult.fold(
                onSuccess = { mapping -> mapping.keys.maxOrNull() ?: 20 },
                onFailure = { 20 }
            )

            // Fetch real period time table from API (fallback = hardcoded 13-period table)
            val periods = repository.fetchPeriods(year, term)
            val periodLabels = toPeriodInfos(periods)

            // Sync both displayWeek sources
            _displayWeek.value = autoWeek
            _uiState.update {
                it.copy(
                    semesterYear = year, semesterTerm = term,
                    currentWeek = autoWeek, displayWeek = autoWeek,
                    periodLabels = periodLabels,
                    lastWeek = lastWeek
                )
            }

            // Check cache first
            val cached = repository.hasCachedData(year, term)
            if (cached) {
                _uiState.update { it.copy(isCached = true) }
            }

            // ④ combine: Room 课程 + 独立的 displayWeek Flow → 自动过滤
            // 使用独立的 _displayWeek 而不是 _uiState.map{}，避免
            // StateFlow 合并更新时潜在的竞态导致 combine 错过触发信号
            // ④ combine: Room 课程 + displayWeek + 色彩偏好 + 周末列开关 → 自动过滤 + 课程色相感知 seedHue
            viewModelScope.launch {
                combine(
                    _viewYear.combine(_viewTerm) { y, t -> y to t }
                        .flatMapLatest { (y, t) -> repository.observeSchedule(y, t) },
                    _displayWeek,
                    colorPrefsFlow,
                    preferences.showWeekendColumns
                ) { allCourses, week, colorPrefs, showWeekend ->
                    CombinedSchedule(
                        allCourses = allCourses,
                        filtered = filterCoursesByWeek(allCourses, week),
                        colorPrefs = colorPrefs,
                        dayLabels = if (showWeekend) FULL_DAY_LABELS else WEEKDAY_DAY_LABELS
                    )
                }.collect { combined ->
                    _uiState.update {
                        it.copy(
                            courses = combined.allCourses,
                            filteredCourses = combined.filtered,
                            coursePalettes = buildCoursePalettes(
                                combined.allCourses,
                                seedOffset = combined.colorPrefs.seedHue ?: 0f,
                                isDark = combined.colorPrefs.isDark
                            ),
                            dayLabels = combined.dayLabels,
                            isLoading = false,
                            activeCourseId = findActiveCourse(combined.filtered, it.displayWeek)
                        )
                    }
                }
            }

            // ⑤ 刷新课表 (后台，不阻塞 UI)
            refreshSchedule()

            // #29 学期末窗口：今天 ≥ 末周周日 且 今天 < 估算下学期开学日 → 窗口活动
            // 面板可见性由 computeAndApplyPanelMode() 综合窗口态 + 下学期缓存 + 展示学期得出。
            val lastWeekSunday: LocalDate = weekMappingResult.fold(
                onSuccess = { mapping ->
                    val rangeStr = mapping[lastWeek] ?: ""
                    val parts = rangeStr.split("/")
                    if (parts.size == 2) try { LocalDate.parse(parts[1]) } catch (_: Exception) { today } else today
                },
                onFailure = { today }
            )
            val nextTermStartEst = estimateNextTermStartDate(year, term)
            endWindowActive = today >= lastWeekSunday && today < nextTermStartEst
            computeAndApplyPanelMode()
        }
    }

    fun refreshSchedule() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            val state = _uiState.value
            val result = repository.refreshSchedule(state.semesterYear, state.semesterTerm)
            result.fold(
                onSuccess = {
                    // Room Flow will automatically emit updated data
                    _uiState.update { it.copy(isRefreshing = false, isCached = true) }
                    // Refresh all widget instances
                    widgetUpdateManager.updateAll()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            error = if (it.isCached) {
                                null // Don't show error if we have cached data
                            } else {
                                e.message ?: "Failed to load schedule"
                            }
                        )
                    }
                }
            )
        }
    }

    fun goToPreviousWeek() {
        val current = _uiState.value.displayWeek
        if (current > 1) {
            setDisplayWeek(current - 1)
        }
    }

    fun goToNextWeek() {
        val current = _uiState.value.displayWeek
        val maxWeek = _uiState.value.lastWeek
        if (current < maxWeek) {
            setDisplayWeek(current + 1)
        }
    }

    fun goToCurrentWeek() {
        val target = _uiState.value.currentWeek
        // 看下学期时 currentWeek=0（无"本周"概念），回退到第 1 周。
        setDisplayWeek(if (target > 0) target else 1)
    }

    // ---- #29 学期末悬浮面板方法 ----

    private fun toPeriodInfos(periods: List<PeriodTime>): List<PeriodInfo> = periods.map { pt ->
        PeriodInfo(
            label = pt.period.toString(),
            startPeriod = pt.period,
            endPeriod = pt.period,
            startTime = pt.startTime,
            endTime = pt.endTime,
            timeRange = "${pt.startTime}-${pt.endTime}"
        )
    }

    /** 估算下一学期开学日期（用于自动收起面板）。 */
    private fun estimateNextTermStartDate(year: String, term: String): LocalDate {
        val y = year.toIntOrNull() ?: LocalDate.now().year
        return when (term) {
            "3"  -> LocalDate.of(y + 1, 2, 20)   // 秋→春，约2月下旬
            "12" -> LocalDate.of(y + 1, 8, 25)   // 春→秋，约8月下旬
            "16" -> LocalDate.of(y + 1, 8, 25)   // 小学期→秋
            else -> LocalDate.of(y + 1, 2, 20)
        }
    }

    /** 构建下学期可读标签，如 "2026年春季学期"。 */
    private fun buildNextTermLabel(year: String, term: String): String {
        val y = (year.toIntOrNull() ?: 0) + 1
        return when (term) {
            "3"  -> "${y}年春季学期"
            "12" -> "${y}年秋季学期"
            "16" -> "${y}年秋季学期"
            else -> "${y}年新学期"
        }
    }

    // ---- #29 下学期课表悬浮面板（三态，内存态，每次进页重算） ----

    /** 每次进入课表页调用：重置「暂时忽略」会话态并重算面板（满足"本次忽略、下次仍弹"）。 */
    fun onScheduleEntered() {
        panelDismissedThisSession = false
        computeAndApplyPanelMode()
    }

    /** Snackbar 消费「下学期未公布」标记，避免重复提示。 */
    fun consumeNextSemesterUnavailable() {
        _uiState.update { it.copy(nextSemesterUnavailable = false) }
    }

    /** 用户点击「暂时忽略」——仅隐藏本次进入（会话内存态，下次进页由 onScheduleEntered 重置）。 */
    fun dismissSemesterEndPanel() {
        panelDismissedThisSession = true
        computeAndApplyPanelMode()
    }

    /** 用户点击「查询课表」(DISCOVERY) / 「查看」(REENTRY)——先联网探测，确认有课表再切到下学期展示。 */
    fun queryNextSemesterSchedule() {
        viewModelScope.launch {
            _uiState.update { it.copy(queryingNextSemester = true) }
            val (ny, nt) = nextSemester(_viewYear.value, _viewTerm.value)
            // 先探测：有课表才切换视图；无课表保持本学期，由 Snackbar 提示「暂无新课表」。
            val result = repository.refreshSchedule(ny, nt)
            val hasCourses = repository.observeSchedule(ny, nt).first().isNotEmpty()
            _uiState.update {
                it.copy(
                    queryingNextSemester = false,
                    nextSemesterCoursesLoaded = result.isSuccess && hasCourses,
                    nextSemesterUnavailable = !(result.isSuccess && hasCourses)
                )
            }
            if (result.isSuccess && hasCourses) {
                switchDisplaySemester(ny, nt, isCurrent = false)
            }
            computeAndApplyPanelMode()
        }
    }

    /** 用户点击「查看」(REENTRY)——下学期已缓存，仅切展示学期（不发网络请求）。 */
    fun viewNextSemester() {
        viewModelScope.launch {
            val (ny, nt) = nextSemester(_currentYear.value, _currentTerm.value)
            switchDisplaySemester(ny, nt, isCurrent = false)
            computeAndApplyPanelMode()
        }
    }

    /** 用户点击「返回本学期」(BACK)——切回真实本学期展示。 */
    fun viewCurrentSemester() {
        viewModelScope.launch {
            switchDisplaySemester(_currentYear.value, _currentTerm.value, isCurrent = true)
            computeAndApplyPanelMode()
        }
    }

    /**
     * 切换展示学期（不碰全局 currentSemester）。
     * isCurrent=true 恢复真实当前周；=false（看下学期）置 currentWeek=0，
     * 使 findActiveCourse 的 displayWeek≠currentWeek 守卫生效，避免误高亮下学期课程。
     */
    private suspend fun switchDisplaySemester(year: String, term: String, isCurrent: Boolean) {
        _viewYear.value = year
        _viewTerm.value = term
        val periods = repository.fetchPeriods(year, term)
        _displayWeek.value = 1
        _uiState.update {
            it.copy(
                semesterYear = year,
                semesterTerm = term,
                displayWeek = 1,
                currentWeek = if (isCurrent) realCurrentWeek else 0,
                periodLabels = toPeriodInfos(periods)
            )
        }
    }

    /**
     * 综合：窗口态 + 下学期是否已缓存 + 当前展示学期 → 决定面板 mode。
     * 不持久化；每次进页 onScheduleEntered() 与学期切换后置位重算。
     */
    private fun computeAndApplyPanelMode() {
        viewModelScope.launch {
            val viewingNext = _viewYear.value != _currentYear.value
                || _viewTerm.value != _currentTerm.value
            val (ny, nt) = nextSemester(_currentYear.value, _currentTerm.value)
            val nextCached = repository.hasCachedData(ny, nt)
            val mode = if (panelDismissedThisSession) {
                SemesterPanelMode.NONE
            } else when {
                viewingNext -> SemesterPanelMode.BACK
                nextCached -> SemesterPanelMode.REENTRY
                endWindowActive -> SemesterPanelMode.DISCOVERY
                else -> SemesterPanelMode.NONE
            }
            _uiState.update {
                it.copy(
                    panelMode = mode,
                    nextTermLabel = buildNextTermLabel(_currentYear.value, _currentTerm.value)
                )
            }
        }
    }

    /** [3,12,16] 顺序取下一学期，16→次年3。 */
    private fun nextSemester(year: String, term: String): Pair<String, String> {
        val y = year.toIntOrNull() ?: LocalDate.now().year
        return when (term) {
            "3"  -> y.toString() to "12"
            "12" -> (y + 1).toString() to "3"
            "16" -> (y + 1).toString() to "3"
            else -> y.toString() to "12"
        }
    }

    private fun setDisplayWeek(week: Int) {
        // Update the independent Flow FIRST — this triggers combine re-evaluation
        _displayWeek.value = week
        // Then update the UI state (activeCourseId will be overridden by combine result)
        _uiState.update {
            it.copy(
                displayWeek = week,
                activeCourseId = findActiveCourse(it.filteredCourses, week)
            )
        }
    }

    /**
     * Find the course that is currently active based on day of week and time.
     */
    private fun findActiveCourse(courses: List<CourseEntity>, displayWeek: Int): String? {
        val state = _uiState.value
        if (displayWeek != state.currentWeek) return null

        val todayDayOfWeek = today.dayOfWeek.value // Mon=1 ... Sun=7

        for (course in courses) {
            if (course.dayOfWeek != todayDayOfWeek) continue
            if (displayWeek !in course.startWeek..course.endWeek) continue

            // Find the period info for this course's time range
            val periodInfo = state.periodLabels.find {
                it.startPeriod <= course.startPeriod && it.endPeriod >= course.endPeriod
                    || it.startPeriod <= course.startPeriod && it.endPeriod >= course.startPeriod
            }
            if (periodInfo != null) {
                // Parse the time range
                val times = periodInfo.timeRange.split("-")
                if (times.size == 2) {
                    try {
                        val startTime = LocalTime.parse(times[0], DateTimeFormatter.ofPattern("HH:mm"))
                        val endTime = LocalTime.parse(times[1], DateTimeFormatter.ofPattern("HH:mm"))
                        if (currentTime >= startTime && currentTime <= endTime) {
                            return course.id
                        }
                    } catch (_: Exception) {}
                }
            }
        }
        return null
    }

    /**
     * 根据手机日期猜测当前学期。
     *
     * 学年划分（河北师大）：秋(9–1月)=第一学期 xqm=3；春(2–7月)=第二学期 xqm=12；
     * 小学期(6月下旬–7月)=第三学期 xqm=16。
     *   month ∈ [8,12]        → xqm="3",  xnm=当年
     *   month == 7 或 6月≥20  → xqm="16", xnm=当年-1
     *   其余(1–6.19)          → xqm="12", xnm=当年-1
     * 注：guess 仅作初猜，detectAndApplySemester 会用 N2154 接口校验，
     * 若小学期无课表数据则自动回退到春学期，不会误切。
     */
    private fun guessCurrentSemester(): Pair<String, String> {
        val now = LocalDate.now()
        val m = now.monthValue
        val d = now.dayOfMonth
        return when {
            m in 8..12 -> now.year.toString() to "3"
            m == 7 || (m == 6 && d >= 20) -> (now.year - 1).toString() to "16"
            else -> (now.year - 1).toString() to "12"
        }
    }

    /**
     * Detect the current semester. Compares the phone-date guess against
     * the stored semester. If they differ, validates the guess via the
     * N2154 API. Switches only when the new semester is confirmed active.
     */
    private suspend fun detectAndApplySemester(): Pair<String, String> {
        val storedYear = preferences.currentSemesterYear.first()
        val storedTerm = preferences.currentSemesterTerm.first()
        val guessed = guessCurrentSemester()

        // Same → no switch needed
        if (storedYear == guessed.first && storedTerm == guessed.second) {
            return storedYear to storedTerm
        }

        // Different → validate via API
        val result = repository.fetchWeekDateMapping(guessed.first, guessed.second)
        return if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
            preferences.setCurrentSemester(guessed.first, guessed.second)
            android.util.Log.w("MyHEBNU", "Semester switched: $storedYear-$storedTerm → ${guessed.first}-${guessed.second}")
            guessed
        } else {
            android.util.Log.w("MyHEBNU", "Semester guess ${guessed.first}-${guessed.second} invalid (break?), keeping $storedYear-$storedTerm")
            storedYear to storedTerm
        }
    }

    /**
     * Filter courses that are active in the given week.
     * Checks week range AND odd/even week restriction.
     */
    private fun filterCoursesByWeek(
        courses: List<CourseEntity>, week: Int
    ): List<CourseEntity> {
        val isOdd = (week % 2 == 1)
        return courses.filter { course ->
            week in course.startWeek..course.endWeek &&
            when (course.oddEven) {
                1 -> isOdd
                2 -> !isOdd
                else -> true
            }
        }
    }

    /**
     * Parse a date range string like "2025-09-08/2025-09-14" into a pair of LocalDates.
     */
    private fun parseDateRange(range: String): Pair<LocalDate, LocalDate> {
        val parts = range.split("/")
        return if (parts.size == 2) {
            try {
                LocalDate.parse(parts[0]) to LocalDate.parse(parts[1])
            } catch (_: Exception) {
                LocalDate.now() to LocalDate.now()
            }
        } else {
            LocalDate.now() to LocalDate.now()
        }
    }

    /** Build tonal palettes for all courses, optionally rotated by [seedOffset]. */
    private fun buildCoursePalettes(
        courses: List<CourseEntity>,
        seedOffset: Float = 0f,
        isDark: Boolean = false
    ): Map<String, com.myhebnu.ui.theme.CourseTonalPalette> {
        val names = courses.map { it.courseName }.distinct()
        val hues = com.myhebnu.ui.theme.assignCourseHues(names, seedOffset)
        return names.associateWith { name ->
            com.myhebnu.ui.theme.coursePaletteForHue(hues[name] ?: 0f, isDark)
        }
    }

    fun selectCourse(course: CourseEntity?) {
        _uiState.update { it.copy(selectedCourse = course) }
    }

    // ============================================================
    // JSON helpers for color preset parsing
    // ============================================================

    private fun parsePresetsJson(json: String): List<ColorPreset> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ColorPreset(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    seedHue = obj.getDouble("seedHue").toFloat(),
                    isBuiltIn = false
                )
            }
        } catch (e: Exception) { emptyList() }
    }
}
