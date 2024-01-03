package kr.syeyoung.bcfestival.questioning

import kr.syeyoung.bcfestival.points.PointService
import kr.syeyoung.bcfestival.users.UserService
import kr.syeyoung.bcfestival.users.UserUpdateEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class QuestionService {

    val historicQuestions: MutableMap<String, Question> = hashMapOf();
    var currentVoting: Question? = null;
    var lastQuestion: Question? = null;

    @Autowired
    lateinit var userService: UserService

    suspend fun createQuestion(question: String, correctPt: Int, incorrectPt: Int): Question {
        if (currentVoting != null) throw IllegalStateException("There is a question on-going")
        val currentQuestion = Question(
            UUID.randomUUID().toString(),
            question = question,
            correctPoints = correctPt,
            incorrectPoints = incorrectPt
        )
        userService.getUsers().forEach { currentQuestion.answers[it] = ""; currentQuestion.grade[it] = false; currentQuestion.lastAnswer[it] = currentQuestion.start }

        this.currentVoting = currentQuestion
        this.lastQuestion = currentQuestion
        historicQuestions.put(currentQuestion.id, currentQuestion)
        return currentQuestion
    }

    suspend fun reviveQuestion(questionId: String): Question {
        if (currentVoting != null) throw IllegalStateException("There is a question on-going")

        val newQuestion = historicQuestions[questionId] ?: throw IllegalArgumentException("No question with id that")
        newQuestion.end = null
        if (newQuestion.gradingComp)
            unfinishGrade(newQuestion.id)
        this.currentVoting = newQuestion
        this.lastQuestion = newQuestion
        return newQuestion
    }

    @EventListener
    fun onEvent(userUpdateEvent: UserUpdateEvent) {
        val question = currentVoting;
        if (question == null) return
        question.answers = question.answers.filterKeys { userUpdateEvent.newUsers.contains(it) }.toMutableMap()
        question.grade = question.grade.filterKeys { userUpdateEvent.newUsers.contains(it) }.toMutableMap()
        for (newUser in userUpdateEvent.newUsers) {
            question.answers.putIfAbsent(newUser, "")
            question.grade.putIfAbsent(newUser, false)
        }
    }

    suspend fun endQuestion(): Question {
        val question = currentVoting ?: throw IllegalStateException("There isn't a question on-going")
        question.end = Instant.now()
        this.currentVoting = null
        return question
    }

    suspend fun getQuestions(): List<Question> {
        return historicQuestions.values.toList()
    }

    suspend fun answer(name: String, option: String) {
        val question = currentVoting ?: throw IllegalStateException("There isn't a question on-going")
        question.answers.put(name, option)
        question.grade.put(name, false)
        question.lastAnswer.put(name, Instant.now())
    }

    suspend fun grade(id: String, name: String, correct: Boolean) {
        val question = historicQuestions[id] ?: throw IllegalStateException("Can't grade unfinished or nonexistant question")
        if (question.gradingComp) throw IllegalStateException("Can't grade graded question")
        question.grade[name] = correct
    }

    @Autowired
    lateinit var pointService: PointService;

    suspend fun finishGrade(id: String) {
        val question = historicQuestions[id] ?: throw IllegalStateException("Can't grade unfinished or nonexistant question")
        if (question.gradingComp) throw IllegalStateException("Grading has been finished for this question")
        question.grade.forEach { (player, correct) ->
            pointService.appendPoint(player, if(correct) question.correctPoints else question.incorrectPoints)
        }
        question.gradingComp = true;
    }

    suspend fun unfinishGrade(id: String) {
        val question = historicQuestions[id] ?: throw IllegalStateException("Can't grade unfinished or nonexistant question")
        if (!question.gradingComp) throw IllegalStateException("Grading has been un-finished for this question")
        question.grade.forEach { (player, correct) ->
            pointService.appendPoint(player, if(correct) -question.correctPoints else -question.incorrectPoints)
        }
        question.gradingComp = false
    }

    suspend fun getCurrentQuestion(): Question? {
        return currentVoting;
    }
}