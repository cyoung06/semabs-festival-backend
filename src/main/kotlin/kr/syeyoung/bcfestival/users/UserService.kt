package kr.syeyoung.bcfestival.users

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

data class UserUpdateEvent(val newUsers: Set<String>)

@Service
class UserService {
    val usernames: MutableSet<String> = linkedSetOf();

    @Autowired
    lateinit var  eventHandler: ApplicationEventPublisher;

    suspend fun appendUser(username: String) {
        usernames.add(username)

        eventHandler.publishEvent(UserUpdateEvent(usernames))
    }

    suspend fun removeUser(username: String) {
        usernames.remove(username)

        eventHandler.publishEvent(UserUpdateEvent(usernames))
    }

    suspend fun resetUsers() {
        usernames.clear()
        eventHandler.publishEvent(UserUpdateEvent(usernames))
    }

    suspend fun getUsers(): Set<String> {
        return usernames;
    }
}