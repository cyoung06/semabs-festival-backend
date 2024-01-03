package kr.syeyoung.bcfestival.voting

import kr.syeyoung.bcfestival.users.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/vote")
class VotingController {

    @Autowired
    lateinit var voteService: VoteService;
    @Autowired
    lateinit var userService: UserService;

    data class VoteBeginRequest(val options: List<String>)
    @PostMapping("/begin")
    suspend fun beginVote(@RequestBody request: VoteBeginRequest): Vote {
        return voteService.createVote(request.options)
    }

    @PostMapping("/close")
    suspend fun closeVote(): Vote {
        return voteService.endVote()
    }


    data class VotingRequest(val index: Int, val name: String)

    @PostMapping("/vote")
    suspend fun doVote(@RequestBody request: VotingRequest) {
        if (!userService.getUsers().contains(request.name)) throw IllegalStateException("User not registered")
        return voteService.vote(request.name, request.index)
    }

    @GetMapping("/")
    suspend fun getCurrentVote(): Vote {
        return voteService.getCurrentVote() ?: throw  ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @GetMapping("/dump")
    suspend fun dumpAll(): List<Vote> {
        return voteService.getVotes()
    }

    @GetMapping("/last")
    suspend fun getLastVote(): Vote {
        return voteService.lastVote ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

}