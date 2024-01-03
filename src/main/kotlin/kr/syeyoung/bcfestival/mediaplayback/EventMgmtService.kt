package kr.syeyoung.bcfestival.mediaplayback

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.FileUrlResource
import org.springframework.core.io.Resource
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Path
import java.util.UUID

@Service
@EnableReactiveMongoRepositories
class EventMgmtService {
    @Autowired
    lateinit var eventMediaReactiveRepository: EventMediaReactiveRepository;

    suspend fun getEventById(eventId: String): Event {
        val (media, event) = eventMediaReactiveRepository.findAll()
            .asFlow()
            .flatMapConcat { media -> media.events.map { media to it }.asFlow() }
            .filter { it.second.id == eventId }.first()

        return event
    }

    suspend fun downloadMedia(mediaId: String): Resource {
        val loc = getMedia(mediaId).location
        return FileSystemResource(Path.of(loc))
    }

    suspend fun addEventToMedia(mediaId: String, event: Event) : Event {
        val media = getMedia(mediaId)
        event.id = UUID.randomUUID().toString()
        media.events.add(event)
        eventMediaReactiveRepository.save(media).awaitSingle()
        return event;
    }
    suspend fun deleteEventFromMedia(mediaId: String, eventId: String): EventMedia {
        val media = getMedia(mediaId)
        media.events.removeIf { it.id == eventId }
        return eventMediaReactiveRepository.save(media).awaitSingle()
    }

    suspend fun getMedia(mediaId: String): EventMedia {
        return eventMediaReactiveRepository.findById(mediaId).awaitSingleOrNull() ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
    suspend fun uploadMedia(filePart: FilePart, name :String): EventMedia {
        val uuid = UUID.randomUUID()
        var ext = filePart.filename().split(".").last()
        if (!filePart.filename().contains("."))
            ext = "unk"
        var filename = "$uuid-${System.currentTimeMillis()}.$ext"
        filePart.transferTo(Path.of("media",filename)).awaitSingleOrNull()

        val media = EventMedia(
            id = uuid.toString(),
            name= name,
            location = "media/$filename",
            events = arrayListOf()
        )
        return eventMediaReactiveRepository.save(media).awaitSingle()
    }

    suspend fun updateMedia(filePart: FilePart, mediaId: String): EventMedia {
        var media = getMedia(mediaId);

        var ext = filePart.filename().split(".").last()
        if (!filePart.filename().contains("."))
            ext = "unk"
        var filename = "${media.id}-${System.currentTimeMillis()}.$ext"
        filePart.transferTo(Path.of("media",filename)).awaitSingleOrNull()
        media.location = "media/$filename"
        return eventMediaReactiveRepository.save(media).awaitSingle()
    }

    suspend fun renameMedia(mediaId: String, name: String): EventMedia {
        return getMedia(mediaId).let {
            it.name = name
            eventMediaReactiveRepository.save(it).awaitSingle()
        }
    }
    suspend fun deleteMedia(mediaId: String) {
        eventMediaReactiveRepository.deleteById(mediaId).awaitSingleOrNull()
    }
}