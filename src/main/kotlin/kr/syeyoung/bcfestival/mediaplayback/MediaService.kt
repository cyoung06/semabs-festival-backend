package kr.syeyoung.bcfestival.mediaplayback

import com.alibaba.fastjson.JSONObject
import io.github.macfja.mpv.communication.handling.NamedEventHandler
import io.github.macfja.mpv.communication.handling.PropertyObserver
import io.github.macfja.mpv.wrapper.Shorthand
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.lang.Exception
import java.lang.Runnable
import java.math.BigDecimal
import java.nio.file.Path
import java.util.*


@Service
class MediaService  {
    lateinit var mpv: Shorthand;

    private val applicationCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)



    @PostConstruct
    fun construct() {
        println("Connecting to ${System.getenv("MPV_SOCKET")}")
        mpv = Shorthand(RealMPVService(System.getenv("MPV_SOCKET")))

        mpv.registerPropertyChange(TimeChanged())
        mpv.registerPropertyChange(MediaChanged())
        mpv.registerPropertyChange(PauseChanged())
        mpv.registerEvent(MediaEnded())

        applicationCoroutineScope.launch {
            clearScreen()
        }
    }

    @Autowired
    lateinit var eventMgmtService: EventMgmtService;
    @Autowired
    lateinit var applicationEventPublisher: ApplicationEventPublisher;

    var nextUp: EventMedia? = null;
    var current: EventMedia? = null;

    suspend fun setNextPlaying(eventMedia: String?) {
        if (nextUp?.id == eventMedia) return
        nextUp = eventMedia?.let { eventMgmtService.getMedia(it) }

        val next = nextUp
        withContext(Dispatchers.IO) {
            mpv.sendCommand("playlist-clear", emptyList())
            if (next != null)
                mpv.addMedia(Path.of(next.location).toAbsolutePath().toString(), true)
        }
    }
    suspend fun getNextPlaying(): String? {
        return nextUp?.id;
    }
    suspend fun pausePlaying() {
        withContext(Dispatchers.IO) {
            mpv.pause()
        }
    }
    suspend fun resumePlaying() {
        withContext(Dispatchers.IO) {
            mpv.play()
        }
    }
    suspend fun isPlaying(): Boolean {
        return !lastPaused;
    }
    suspend fun clearScreen() {
        withContext(Dispatchers.IO) {
            mpv.sendCommand("stop", emptyList())
        }

        current = null
        nextUp = null
        events.clear()
        lastEventFire = -100
    }
    suspend fun moveToCursor(timestamp: Long) {
        withContext(Dispatchers.IO) {
            mpv.sendCommand("seek", listOf(String.format("%.2f", timestamp/1000.0), "absolute+exact"))
        }

        if (events.isNotEmpty()) {
            var eventa: Event = events.peek()
            while (eventa.timestamp > timestamp) {
                events.pop()
                applicationEventPublisher.publishEvent(PlaybackEvent(false, eventa))

                if (events.isNotEmpty()) {
                    eventa = events.peek()
                } else {
                    break;
                }
            }
        }
    }
    suspend fun getPosition(): Long? {
        return lastTime;
//        return withContext(Dispatchers.IO) {
//            val value = mpv.getProperty("time-pos/full")
//            if (value == null) lastTime;
//            else (JSONObject.parseObject(value).getDoubleValue("data") * 1000).toLong()
//        }
    }

    val events: Stack<Event> = Stack()
    var lastEventFire: Long = -100
    var lastTime: Long = 0
    var lastPaused: Boolean = false;

    inner class MediaEnded : NamedEventHandler("end-file") {
        override fun doHandle(p0: JSONObject?): Runnable {
            println("Current playback ended")
            val currentMedia = current;

            if (currentMedia != null) {
                val toFire = currentMedia.events.filter {
                    lastEventFire + 1 < it.timestamp
                }

                toFire.forEach {
                    events.push(it)
                    applicationEventPublisher.publishEvent(PlaybackEvent(true, it))
                }
            }
            current = null;


            return Runnable {  };
        }
    }
    inner class MediaChanged: PropertyObserver("filename/no-ext") {
        override fun changed(p0: String?, data: Any?, id: Int?) {
            println("New File ${data}")

            if ((data as String).startsWith(nextUp?.id ?: "asdjlaskjdlaskd")) {
                current = nextUp;
            } else {
                current = null
                println("WHAT THE FU??? $data playing??")
            }
            nextUp = null
            events.clear()
            lastEventFire = -100
        }
    }

    inner class PauseChanged: PropertyObserver("pause") {
        override fun changed(p0: String?, p1: Any?, p2: Int?) {
            lastPaused = p1 as Boolean;
        }

    }

    inner class TimeChanged: PropertyObserver("time-pos/full") {
        override fun changed(p0: String?, data: Any?, id: Int?) {
            val newTime = ((data as BigDecimal).toDouble() * 1000).toLong()
            println(newTime)
            lastTime = newTime;
            val currentMedia = current;
            if (currentMedia == null) return
            // fire event
            if (newTime >= lastEventFire) {
                val toFire = currentMedia.events.filter {
                    it.timestamp in (lastEventFire + 1)..newTime
                }
                lastEventFire = newTime

                toFire.forEach {
                    events.push(it)
                    applicationEventPublisher.publishEvent(PlaybackEvent(true, it))
                }
            } else {
                if (events.isNotEmpty()) {
                    var eventa = events.peek()

                    while (eventa.timestamp > newTime) {
                        events.pop()
                        applicationEventPublisher.publishEvent(PlaybackEvent(false, eventa))

                        if (events.isNotEmpty()) {
                            eventa = events.peek();
                        } else {
                            break
                        }
                    }
                }
                lastEventFire = newTime
            }

        }
    }


}