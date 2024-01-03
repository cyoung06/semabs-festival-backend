package kr.syeyoung.bcfestival.voting

import java.time.Instant

data class Vote(
    val id: String,
    val start: Instant = Instant.now(),
    var end: Instant? = null,
    val options: List<String>,
    var votes: MutableMap<String, Int> = hashMapOf(),
    var lastAnswer: MutableMap<String, Instant> = hashMapOf(),
) {
    val effectiveVotes : List<Int>
        get() {
            val realVotes = votes.values.groupBy { i -> i }.map { it.key to it.value.size }.toMap()

            return options.indices.map {
                realVotes.getOrDefault(it, 0)
            }
        }
    val votesButList : List<VoteData>
        get() {
            return votes.map { VoteData(it.key, it.value) }
        }


}

data class VoteData(val name: String, val choice: Int);