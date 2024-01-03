package kr.syeyoung.bcfestival.points

import kr.syeyoung.bcfestival.users.UserUpdateEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class PointService {
    var pointMap: MutableMap<String, Int> = hashMapOf()


    suspend fun appendPoint(player: String, point: Int) {
        pointMap[player] = pointMap.getOrDefault(player, 0) + point;
    }

    suspend fun getPoints(): Map<String, Int> {
        return pointMap
    }

    suspend fun reset() {
        pointMap.replaceAll { t, u -> 0 }
    }



    @EventListener
    fun onEvent(userUpdateEvent: UserUpdateEvent) {
        pointMap = pointMap.filterKeys { userUpdateEvent.newUsers.contains(it) }.toMutableMap()
        for (newUser in userUpdateEvent.newUsers) {
            pointMap.putIfAbsent(newUser, 0)
        }
    }
}