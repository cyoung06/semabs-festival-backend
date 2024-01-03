package kr.syeyoung.bcfestival.mediaplayback

import io.obswebsocket.community.client.OBSRemoteController
import io.obswebsocket.community.client.message.request.scenes.SetCurrentProgramSceneRequest
import io.obswebsocket.community.client.message.request.scenes.SetCurrentProgramSceneRequest.SetCurrentProgramSceneRequestBuilder
import io.obswebsocket.community.client.message.response.scenes.SetCurrentProgramSceneResponse
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.*
import kr.syeyoung.bcfestival.points.PointService
import kr.syeyoung.bcfestival.questioning.QuestionService
import kr.syeyoung.bcfestival.voting.VoteService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.*


@Service
class FestivalService {

    @Autowired
    lateinit var voteService: VoteService;
    @Autowired
    lateinit var pointService: PointService;
    @Autowired
    lateinit var mediaService: MediaService;
    @Autowired
    lateinit var questionService: QuestionService;
    @Autowired
    lateinit var eventMgmtService: EventMgmtService;

    lateinit var obsRemoteController: OBSRemoteController;
    @PostConstruct
    fun setupObs() {
        obsRemoteController = OBSRemoteController.builder()
            .host(System.getenv("OBS_HOST")) // Default host
            .port(Integer.getInteger("OBS_PORT", 4455)) // Default port
            .password(System.getenv("OBS_PASSWORD")) // Provide your password here
            .connectionTimeout(3)
            .lifecycle().onConnect {
                println("CONNECTED!!")
            }.onReady {
                obsRemoteController.sendRequest<SetCurrentProgramSceneRequest,SetCurrentProgramSceneResponse>(SetCurrentProgramSceneRequest.builder().sceneName("Scene 2").build()) {
                    println(it)
                }
            }
            .onClose {
                Thread.sleep(1000)
                obsRemoteController.connect()
            }
            .and()// Seconds the client will wait for OBS to respond
            .build()
        obsRemoteController.connect()
        println(obsRemoteController)
    }

    data class State(val lastVoteId: String?, val lastNextMedia: String?, val lastScene: String?, val lastQuestionId: String?) {
        fun derive(lastVoteId: String? = null, lastNextMedia: String? = null, lastScene: String? = null, lastQuestionId: String? = null): State {
            return State(
                lastVoteId ?: this.lastVoteId,
                lastNextMedia ?: this.lastNextMedia,
                lastScene ?: this.lastScene,
                lastQuestionId ?: this.lastQuestionId
            )
        }
    }
    var states: Stack<State> = Stack()
    var currentScene = ""

    suspend fun begin(mediaId: String) {
        states.clear()
        states.push(State(null, null, null, null))
        currentScene = "default"
        mediaService.setNextPlaying(mediaId)
    }
    suspend fun beginForce(mediaId: String) {
        mediaService.setNextPlaying(mediaId)
    }
    suspend fun stop() {
        mediaService.clearScreen()
    }

    private val applicationCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    @EventListener
    fun onEventLol(event: PlaybackEvent) {
        applicationCoroutineScope.launch {
            if (event.fire) onEventOccur(event.event.id)
            else undoEventOccur(event.event.id)
        }
    }

    suspend fun onEventOccur(eventId: String) {
        val event = eventMgmtService.getEventById(eventId)
        if (event.data is VoteBeginEventData) {
            val vote = voteService.createVote((event.data as VoteBeginEventData).options)
            states.push(states.peek().derive(lastVoteId = vote.id))
        } else if (event.data is NextMediaEventData) {
            val prevMedia=  mediaService.getNextPlaying()
            mediaService.setNextPlaying((event.data as NextMediaEventData).mediaId)
            states.push(states.peek().derive(lastNextMedia = prevMedia))
        } else if (event.data is VoteEndEventData) {
            val data: VoteEndEventData = event.data as VoteEndEventData;
            val vote = voteService.endVote()

            vote.votes.forEach {
                pointService.appendPoint(it.key, data.points[it.value])
            }

            val prevMedia = mediaService.getNextPlaying()
            if (data.nextMedia != null) {
                val winningOption = vote.effectiveVotes
                    .mapIndexed { a,b -> a to b }
                    .filter { data.nextMedia[it.first] != "ignored" }
                    .maxBy { it.second }.first
                mediaService.setNextPlaying(data.nextMedia[winningOption])
            }
            states.push(states.peek().derive(lastNextMedia = prevMedia))
        } else if (event.data is SwitchScene) {
            val prevScene = currentScene
            currentScene = event.data.sceneId
            obsRemoteController.sendRequest<SetCurrentProgramSceneRequest,SetCurrentProgramSceneResponse>(SetCurrentProgramSceneRequest.builder().sceneName(event.data.sceneId).build()) {}

            states.push(states.peek().derive(lastScene = prevScene))
        } else if (event.data is Pause) {
            mediaService.pausePlaying()
        } else if (event.data is QuestionStartEventData) {
            val quest = questionService.createQuestion(event.data.question, event.data.correctPoints, event.data.incorrectPoints)
            states.push(states.peek().derive(lastQuestionId = quest.id))
        } else if (event.data is QuestionEndEventData) {
            questionService.endQuestion()
        }
    }
    suspend fun undoEventOccur(eventId: String) {
        val event = eventMgmtService.getEventById(eventId)
        if (event.data is VoteEndEventData) {
            val data: VoteEndEventData = event.data;

            val prevState = states.pop();
            mediaService.setNextPlaying(prevState.lastNextMedia)
            if (prevState.lastVoteId != null) {
                val vote = voteService.historicVotes[prevState.lastVoteId]
                vote?.votes?.forEach {
                    pointService.appendPoint(it.key, -data.points[it.value])
                }
                voteService.reviveVote(prevState.lastVoteId)
            }
        } else if (event.data is NextMediaEventData) {
            val prevState = states.pop()
            mediaService.setNextPlaying(prevState.lastNextMedia)
        } else if (event.data is VoteBeginEventData) {
            val prevState = states.pop()
            mediaService.setNextPlaying(prevState.lastNextMedia)
            if (voteService.getCurrentVote()?.id == prevState.lastVoteId)
                voteService.endVote()
        } else if (event.data is SwitchScene) {
            val prevState = states.pop()


            currentScene = prevState.lastScene!!

            obsRemoteController.sendRequest<SetCurrentProgramSceneRequest,SetCurrentProgramSceneResponse>(SetCurrentProgramSceneRequest.builder().sceneName(currentScene).build()) {}
        } else if (event.data is QuestionStartEventData) {
            val prevState = states.pop()

            mediaService.setNextPlaying(prevState.lastNextMedia)
            if (questionService.getCurrentQuestion()?.id == prevState.lastQuestionId)
                questionService.endQuestion()
        } else if (event.data is QuestionEndEventData) {
            val prevState = states.peek()
            if (prevState.lastQuestionId != null) {
                questionService.reviveQuestion(prevState.lastQuestionId)
            }
        }
    }
}