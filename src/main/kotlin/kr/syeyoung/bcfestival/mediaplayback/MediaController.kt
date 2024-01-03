package kr.syeyoung.bcfestival.mediaplayback

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/eventmedia")
class MediaController {
    @Autowired
    lateinit var eventMgmtService: EventMgmtService;
    @Autowired
    lateinit var festivalService: FestivalService

    @GetMapping("/")
    suspend fun getAll(): List<EventMedia> {
        return eventMgmtService.eventMediaReactiveRepository.findAll().asFlow().toList()
    }

    data class MediaCreationRequest(val name: String)

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], value = ["/"])
    suspend fun createMedia(@RequestPart("media") filePart: FilePart, @RequestPart("request") mediaCreationRequest: MediaCreationRequest): EventMedia {
        return eventMgmtService.uploadMedia(filePart, mediaCreationRequest.name)
    }

    @PutMapping("/{id}/media", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun updateMedia(@PathVariable("id") mediaId: String, @RequestPart("media") filePart: FilePart): EventMedia {
        return eventMgmtService.updateMedia(filePart, mediaId)
    }


    @GetMapping("/{id}/media")
    suspend fun download(@PathVariable("id") mediaId: String,): ResponseEntity<Resource> {
        return ResponseEntity.ofNullable(eventMgmtService.downloadMedia(mediaId))
    }

    @GetMapping("/{id}")
    suspend fun get(@PathVariable("id") mediaId: String): EventMedia {
        return eventMgmtService.getMedia(mediaId)
    }
    @DeleteMapping("/{id}")
    suspend fun delete(@PathVariable("id") mediaId: String) {
        return eventMgmtService.deleteMedia(mediaId)
    }

    @PutMapping("/{id}/name")
    suspend fun changeName(@PathVariable("id") mediaId: String, @RequestBody mediaCreationRequest: MediaCreationRequest): EventMedia {
        return eventMgmtService.renameMedia(mediaId, mediaCreationRequest.name)
    }


    @PostMapping("/{id}/begin")
    suspend fun beginFrom(@PathVariable("id") mediaId: String) {
        return festivalService.begin(mediaId)
    }
    @PostMapping("/{id}/beginForce")
    suspend fun beginFromForce(@PathVariable("id") mediaId: String) {
        return festivalService.beginForce(mediaId)
    }

    @PostMapping("/{id}/events")
    suspend fun addEvent(@PathVariable("id") mediaId: String, @RequestBody event: Event): Event {
        return eventMgmtService.addEventToMedia(mediaId, event)
    }
    @DeleteMapping("/{id}/events/{eventid}")
    suspend fun removeEvent(@PathVariable("id") mediaId: String,@PathVariable("eventid") eventId: String): EventMedia {
        return eventMgmtService.deleteEventFromMedia(mediaId, eventId)
    }
}