package kr.syeyoung.bcfestival.questioning

import kr.syeyoung.bcfestival.users.UserService
import kr.syeyoung.bcfestival.voting.Vote
import kr.syeyoung.bcfestival.voting.VoteService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/question")
class QuestionController {
    @Autowired
    lateinit var questionService: QuestionService;
    @Autowired
    lateinit var userService: UserService;

    data class QuestionBeginRequest(val question: String, val correctPoints: Int, val incorrectPoints: Int)
    @PostMapping("/begin")
    suspend fun beginQuestion(@RequestBody request: QuestionBeginRequest): Question {
        return questionService.createQuestion(request.question, request.correctPoints, request.incorrectPoints)
    }

    @PostMapping("/close")
    suspend fun closeQuestion(): Question {
        return questionService.endQuestion()
    }


    data class AnsweringRequest(val answer: String, val name: String)

    @PostMapping("/answer")
    suspend fun doVote(@RequestBody request: AnsweringRequest) {
        if (!userService.getUsers().contains(request.name)) throw IllegalStateException("User not registered")
        return questionService.answer(request.name, request.answer)
    }

    @GetMapping("/")
    suspend fun getCurrentVote(): Question {
        return questionService.getCurrentQuestion() ?: throw  ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @GetMapping("/dump")
    suspend fun dumpAll(): List<Question> {
        return questionService.getQuestions()
    }

    @GetMapping("/last")
    suspend fun getLastVote(): Question {
        return questionService.lastQuestion ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    data class GradingRequest(val name: String, val correct: Boolean)
    @PostMapping("/{id}/grade")
    suspend fun grade(@PathVariable("id") qId: String, @RequestBody gradingRequest: GradingRequest) {
        questionService.grade(qId, gradingRequest.name, gradingRequest.correct)
    }

    @PostMapping("/{id}/finishGrade")
    suspend fun finishGrade(@PathVariable("id") qId: String) {
        questionService.finishGrade(qId)
    }

    @PostMapping("/{id}/reGrade")
    suspend fun unfinishGrade(@PathVariable("id") qId: String) {
        questionService.unfinishGrade(qId)
    }

}