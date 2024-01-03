package kr.syeyoung.bcfestival.points

import kr.syeyoung.bcfestival.questioning.QuestionService
import kr.syeyoung.bcfestival.users.UserService
import kr.syeyoung.bcfestival.voting.VoteService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/point")
class PointController {
    @Autowired
    lateinit var pointService: PointService;
    @Autowired
    lateinit var userService: UserService;

    @Autowired
    lateinit var voteService: VoteService;
    @Autowired
    lateinit var questionService: QuestionService;

    @GetMapping("/")
    suspend fun getPoints(): Map<String, Int> {
        val times = getTimes()
        val points = pointService.getPoints()

        return points
            .toSortedMap(Comparator
                .comparingInt<String?> { points[it] ?: 0 }
                .reversed()
                .thenComparingLong { times[it] ?: 0 }
                .thenComparing(String::compareTo))
    }

    data class AddPointRequest(val name: String, val point: Int)

    @PostMapping("/inc")
    suspend fun addPoint(@RequestBody request: AddPointRequest) {
        if (!userService.getUsers().contains(request.name)) throw IllegalStateException("User not registered")
        return pointService.appendPoint(request.name, request.point)
    }

    @PostMapping("/zero")
    suspend fun reset() {
        pointService.reset()
    }

    @GetMapping("/time")
    suspend fun getTimes(): Map<String, Long> {
        val total = voteService.historicVotes.filter { it.value.end != null }.map {
            it.value.lastAnswer.map { ans -> ans.key to ans.value.toEpochMilli() - it.value.start.toEpochMilli() }.toMap()
        } + questionService.historicQuestions.filter { it.value.end != null }.map {
            it.value.lastAnswer.map { ans -> ans.key to ans.value.toEpochMilli() - it.value.start.toEpochMilli() }.toMap()
        } + listOf( userService.getUsers().map { it to 0L }.toMap() )


        return total.flatMap { it.entries }
            .groupBy { it.key }
            .map { time -> time.key to time.value.sumOf { it.value } / 1000 }
            .toMap()
    }
}