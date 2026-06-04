package com.myhebnu.domain

/**
 * A single course grade entry for a semester.
 */
data class Grade(
    val courseName: String,         // kcmc
    val courseCode: String,         // kch
    val credit: Float,              // xf
    val score: String,              // zpcj - final composite score
    val scoreValue: Float?,         // parsed numeric score for GPA calc
    val classId: String,            // jxb_id - for detail query
    val teachingClassName: String,  // jxbmc
    val department: String,         // kkbmmc
    val semesterYear: String,       // xnm
    val semesterTerm: String,       // xqm
    val semesterName: String        // "2025-2026-1"
)

/**
 * A sub-item within a course grade (e.g. 平时作业(30%)=86).
 */
data class GradeSubItem(
    val name: String,               // xmblmc - e.g. "课堂表现(20%)"
    val score: String               // xmcj - e.g. "100"
)

/**
 * Aggregated grade data for a single semester.
 */
data class SemesterGrades(
    val semesterYear: String,
    val semesterTerm: String,
    val semesterName: String,
    val courses: List<Grade>,
    val gpa4: Float,                // GPA on 4.0 scale
    val gpa5: Float,                // GPA on 5.0 scale
    val weightedAvg: Float          // Weighted percentage average
)

/**
 * GPA calculation strategies.
 */
enum class GpaStrategy(val label: String) {
    SCALE_4_0("4.0制"),
    SCALE_5_0("5.0制"),
    WEIGHTED_PERCENTAGE("百分制加权");

    companion object {
        fun fromLabel(label: String): GpaStrategy =
            entries.find { it.label == label } ?: SCALE_4_0
    }
}

/**
 * Stateless GPA calculator.
 */
object GpaCalculator {

    fun calculate(courses: List<Grade>, strategy: GpaStrategy): Float {
        val scored = courses.filter { it.scoreValue != null && it.credit > 0 }
        if (scored.isEmpty()) return 0f

        return when (strategy) {
            GpaStrategy.SCALE_4_0 -> {
                val totalPoints = scored.sumOf { it.scoreTo4Scale() * it.credit.toDouble() }
                val totalCredits = scored.sumOf { it.credit.toDouble() }
                (totalPoints / totalCredits).toFloat()
            }
            GpaStrategy.SCALE_5_0 -> {
                val totalPoints = scored.sumOf { it.scoreTo5Scale() * it.credit.toDouble() }
                val totalCredits = scored.sumOf { it.credit.toDouble() }
                (totalPoints / totalCredits).toFloat()
            }
            GpaStrategy.WEIGHTED_PERCENTAGE -> {
                val totalWeighted = scored.sumOf { (it.scoreValue!! * it.credit).toDouble() }
                val totalCredits = scored.sumOf { it.credit.toDouble() }
                (totalWeighted / totalCredits).toFloat()
            }
        }
    }

    private fun Grade.scoreTo4Scale(): Double {
        val s = scoreValue ?: return 0.0
        return when {
            s >= 90 -> 4.0
            s >= 85 -> 3.7
            s >= 82 -> 3.3
            s >= 78 -> 3.0
            s >= 75 -> 2.7
            s >= 72 -> 2.3
            s >= 68 -> 2.0
            s >= 64 -> 1.5
            s >= 60 -> 1.0
            else -> 0.0
        }
    }

    private fun Grade.scoreTo5Scale(): Double {
        val s = scoreValue ?: return 0.0
        return when {
            s >= 90 -> 5.0
            s >= 85 -> 4.5
            s >= 80 -> 4.0
            s >= 75 -> 3.5
            s >= 70 -> 3.0
            s >= 65 -> 2.5
            s >= 60 -> 2.0
            else -> 0.0
        }
    }
}
