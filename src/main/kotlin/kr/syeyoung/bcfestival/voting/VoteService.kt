package kr.syeyoung.bcfestival.voting

import kr.syeyoung.bcfestival.users.UserService
import kr.syeyoung.bcfestival.users.UserUpdateEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class VoteService {
    val historicVotes: MutableMap<String, Vote> = hashMapOf();
    var currentVoting: Vote? = null;
    var lastVote: Vote? = null;

    @Autowired lateinit var userService: UserService

    suspend fun createVote(options: List<String>): Vote {
        if (currentVoting != null) throw IllegalStateException("There is a vote on-going")
        val currentVote = Vote(
            UUID.randomUUID().toString(),
            options = arrayListOf("nothing", *options.toTypedArray())
        )
        userService.getUsers().forEach { currentVote.votes[it] = 0; currentVote.lastAnswer[it] = currentVote.start }

        this.currentVoting = currentVote
        this.lastVote = currentVote
        historicVotes.put(currentVote.id, currentVote)
        return currentVote
    }

    suspend fun reviveVote(voteId: String): Vote {
        if (currentVoting != null) throw IllegalStateException("There is a vote on-going")

        val newVote = historicVotes[voteId] ?: throw IllegalArgumentException("No vote with id that")
        newVote.end = null
        this.currentVoting = newVote
        this.lastVote = newVote
        return newVote
    }

    @EventListener
    fun onEvent(userUpdateEvent: UserUpdateEvent) {
        val vote = currentVoting;
        if (vote == null) return
        vote.votes = vote.votes.filterKeys { userUpdateEvent.newUsers.contains(it) }.toMutableMap()
        for (newUser in userUpdateEvent.newUsers) {
            vote.votes.putIfAbsent(newUser, 0)
        }
    }

    suspend fun endVote(): Vote {
        val vote = currentVoting ?: throw IllegalStateException("There isn't a vote on-going")
        vote.end = Instant.now()
        this.currentVoting = null
        return vote
    }

    suspend fun getVotes(): List<Vote> {
        return historicVotes.values.toList()
    }

    suspend fun vote(name: String, option: Int) {
        val vote = currentVoting ?: throw IllegalStateException("There isn't a vote on-going")
        vote.votes.put(name, option)
        vote.lastAnswer.put(name, Instant.now())
    }

    suspend fun getCurrentVote(): Vote? {
        return currentVoting;
    }
}