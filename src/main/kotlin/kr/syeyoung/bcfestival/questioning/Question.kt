package kr.syeyoung.bcfestival.questioning

import java.time.Instant


data class Question(
    val id: String,
    val start: Instant = Instant.now(),
    var end: Instant? = null,
    val question: String,
    var answers: MutableMap<String, String> = hashMapOf(),
    var grade: MutableMap<String, Boolean> = hashMapOf(),
    var lastAnswer: MutableMap<String, Instant> = hashMapOf(),
    var gradingComp: Boolean = false,
    var correctPoints: Int,
    var incorrectPoints: Int,
) {
    val answersButList : List<QuestionData>
        get() {
            return answers.map { QuestionData(it.key, it.value, grade[it.key] ?: false) }
        }
}

data class QuestionData(val name: String, val choice: String, val correct: Boolean);