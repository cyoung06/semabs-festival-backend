package kr.syeyoung.bcfestival.mediaplayback

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository


@Document
data class EventMedia(
    val id: String,
    var name: String,
    var location: String,
    val events: MutableList<Event>
)
@Repository
interface EventMediaReactiveRepository :
    ReactiveMongoRepository<EventMedia, String>

data class Event(
    var id: String,
    val timestamp: Long,
    val data: EventData,
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(VoteBeginEventData::class, name="voteBegin"),
    JsonSubTypes.Type(VoteEndEventData::class, name="voteEnd"),
    JsonSubTypes.Type(NextMediaEventData::class, name="nextMedia"),
    JsonSubTypes.Type(SwitchScene::class, name="switchScene"),
    JsonSubTypes.Type(Dummy::class, name="dummy"),
    JsonSubTypes.Type(Pause::class, name="pause"),
    JsonSubTypes.Type(QuestionStartEventData::class, name="questionBegin"),
    JsonSubTypes.Type(QuestionEndEventData::class, name="questionEnd")
)
interface EventData {}

data class VoteBeginEventData(
    val options: List<String>
): EventData

data class VoteEndEventData(
    val points: List<Int>,
    val nextMedia: List<String?>?
): EventData

data class NextMediaEventData(
    val mediaId: String
): EventData

data class SwitchScene(
    val sceneId: String
): EventData

data class QuestionStartEventData(
    val question: String,
    val correctPoints: Int,
    val incorrectPoints: Int
): EventData
class QuestionEndEventData: EventData

class Pause: EventData

class Dummy: EventData