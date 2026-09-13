package com.myhebnu.ui.grade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.repository.GradeRepository
import com.myhebnu.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GradeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,          // 手动刷新中（悬浮图标转圈）
    val hasCache: Boolean = false,              // 本地已有成绩 → 显示悬浮刷新图标
    val error: String? = null,
    val warningMessage: String? = null,         // snackbar warning when refresh fails but cache exists
    val semesters: List<SemesterGrades> = emptyList(),
    val currentStrategy: GpaStrategy = GpaStrategy.WEIGHTED_PERCENTAGE,
    val expandedSemester: String? = null,       // semesterName to expand
    val expandedCourse: String? = null,         // classId whose detail is loading/shown
    val courseDetails: Map<String, List<GradeSubItem>> = emptyMap(),
    val loadingDetail: Boolean = false
)

@HiltViewModel
class GradeViewModel @Inject constructor(
    private val repository: GradeRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(GradeUiState())
    val uiState: StateFlow<GradeUiState> = _uiState.asStateFlow()

    // 首次拿到数据时自动展开最新学期；之后不再重置，避免覆盖用户的手动折叠。
    private var initialExpansionApplied = false
    // 最近一次写入首页缓存的学期，避免 Room 每次发射都写 DataStore。
    private var lastHomeCacheSemester: String? = null

    init {
        // 首帧即进入加载态，避免本地成绩到达前闪出"无成绩"
        _uiState.update { it.copy(isLoading = true) }
        observeCachedGrades()
    }

    /**
     * 持续观察本地缓存（Room），构建按学期分组的展示数据。
     * 纯本地读取，零网络——成绩页的数据来源。
     */
    private fun observeCachedGrades() {
        viewModelScope.launch {
            repository.observeCachedGrades().collect { grades ->
                val semesters = buildSemesters(grades)
                val newest = semesters.firstOrNull()

                val newExpanded = if (grades.isNotEmpty() && !initialExpansionApplied) {
                    initialExpansionApplied = true
                    newest?.semesterName
                } else null

                _uiState.update {
                    it.copy(
                        // 无数据且正在加载 → 保持 loading，避免闪出"无成绩"
                        isLoading = if (grades.isEmpty()) it.isLoading else false,
                        hasCache = grades.isNotEmpty(),
                        error = if (grades.isNotEmpty()) null else it.error,
                        semesters = semesters,
                        expandedSemester = newExpanded ?: it.expandedSemester
                    )
                }

                // 最新学期加权均分写入本地缓存，供首页展示（与成绩页同一公式）。
                if (newest != null && newest.semesterName != lastHomeCacheSemester) {
                    lastHomeCacheSemester = newest.semesterName
                    preferences.setHomeWeightedAvg(newest.weightedAvg)
                    preferences.setHomeWeightedAvgSemester(newest.semesterName)
                }
            }
        }
    }

    private fun buildSemesters(grades: List<Grade>): List<SemesterGrades> =
        grades.groupBy { it.semesterName }
            .map { (name, group) ->
                val courses = group.sortedByDescending { it.scoreValue ?: 0f }
                SemesterGrades(
                    semesterYear = courses.firstOrNull()?.semesterYear ?: "",
                    semesterTerm = courses.firstOrNull()?.semesterTerm ?: "",
                    semesterName = name,
                    courses = courses,
                    gpa4 = GpaCalculator.calculate(courses, GpaStrategy.SCALE_4_0),
                    gpa5 = GpaCalculator.calculate(courses, GpaStrategy.SCALE_5_0),
                    weightedAvg = GpaCalculator.calculate(courses, GpaStrategy.WEIGHTED_PERCENTAGE)
                )
            }
            .sortedByDescending { it.semesterName }

    /**
     * 进入成绩页：本地已有成绩 → 直接展示，不发任何网络请求；
     * 首次使用（无本地成绩）→ 拉取所有学期。
     */
    fun loadAllGrades() {
        viewModelScope.launch {
            if (repository.hasCache()) return@launch
            _uiState.update { it.copy(isLoading = true, error = null) }
            runRefresh { repository.refreshAllSemesters() }
        }
    }

    /** 短按悬浮刷新图标：刷新最近一个有成绩的学期 + 当前学期。 */
    fun refreshLatestGrades() {
        viewModelScope.launch {
            runRefresh { repository.refreshLatestSemester() }
        }
    }

    /** 长按悬浮刷新图标：强制拉取所有学期。 */
    fun forceRefreshAllGrades() {
        viewModelScope.launch {
            runRefresh { repository.refreshAllSemesters() }
        }
    }

    private suspend fun runRefresh(block: suspend () -> Result<Unit>) {
        _uiState.update { it.copy(isRefreshing = true, error = null, warningMessage = null) }
        block().fold(
            onSuccess = {
                // Room Flow 会自动发射新数据
                _uiState.update { it.copy(isRefreshing = false, isLoading = false) }
            },
            onFailure = { e ->
                val hasData = _uiState.value.semesters.isNotEmpty()
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        isLoading = false,
                        error = if (hasData) null else (e.message ?: "加载成绩失败"),
                        warningMessage = if (hasData) {
                            e.message ?: "刷新失败，显示的是上次的数据"
                        } else null
                    )
                }
            }
        )
    }

    fun clearWarning() {
        _uiState.update { it.copy(warningMessage = null) }
    }

    fun toggleSemesterExpanded(semesterName: String) {
        _uiState.update {
            if (it.expandedSemester == semesterName) {
                it.copy(expandedSemester = null)
            } else {
                it.copy(expandedSemester = semesterName)
            }
        }
    }

    fun loadGradeDetail(classId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loadingDetail = true, expandedCourse = classId) }

            // Find the semester for this course
            val state = _uiState.value
            var year = ""
            var term = ""
            for (sem in state.semesters) {
                val course = sem.courses.find { it.classId == classId }
                if (course != null) {
                    year = course.semesterYear
                    term = course.semesterTerm
                    break
                }
            }

            val result = repository.getGradeDetail(year, term, classId)
            result.fold(
                onSuccess = { details ->
                    _uiState.update {
                        it.copy(
                            loadingDetail = false,
                            courseDetails = it.courseDetails + (classId to details)
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(loadingDetail = false) }
                }
            )
        }
    }

    fun dismissDetail() {
        _uiState.update { it.copy(expandedCourse = null) }
    }

    fun setStrategy(strategy: GpaStrategy) {
        _uiState.update { it.copy(currentStrategy = strategy) }
    }
}
