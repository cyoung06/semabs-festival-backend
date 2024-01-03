package kr.syeyoung.bcfestival.mediaplayback

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/playback")
class PlaybackController {
    @Autowired
    lateinit var mediaService: MediaService;

    @PostMapping("/resume")
    suspend fun resume() {
        mediaService.resumePlaying()
    }
    @PostMapping("/pause")
    suspend fun pause() {
        mediaService.pausePlaying()
    }
    @PostMapping("/stop")
    suspend fun stop() {
        mediaService.clearScreen()
    }

    data class SeekRequest(val timestamp: Long)
    @PostMapping("/seek")
    suspend fun seek(@RequestBody seekRequest: SeekRequest) {
        mediaService.moveToCursor(seekRequest.timestamp)
    }

    @GetMapping("/time")
    suspend fun getPosition(): Long? {
        return mediaService.getPosition()
    }


    data class PlaybackStatus(val media: EventMedia? , val next: EventMedia?, val playing: Boolean, val time: Long?)
    @GetMapping("/")
    suspend fun getStatus(): PlaybackStatus {
        return PlaybackStatus(mediaService.current,mediaService.nextUp, mediaService.isPlaying(), mediaService.getPosition());
    }
}