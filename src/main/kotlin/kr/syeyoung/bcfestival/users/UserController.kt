package kr.syeyoung.bcfestival.users

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/user")
class UserController {
    @Autowired
    lateinit var userService: UserService;

    @GetMapping("/")
    suspend fun getUsers(): Set<String> {
        return userService.getUsers()
    }

    @PostMapping("/")
    suspend fun createUser(@RequestBody name: String) {
        return userService.appendUser(name)
    }
    @PostMapping("/delete")
    suspend fun deleteUser(@RequestBody name: String) {
        return userService.removeUser(name)
    }
    @DeleteMapping("/")
    suspend fun reset() {
        return userService.resetUsers()
    }
}