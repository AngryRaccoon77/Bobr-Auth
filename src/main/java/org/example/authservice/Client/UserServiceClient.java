package org.example.authservice.Client;

import org.example.userservice.ui.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", url = "http://localhost:8083/api/users")
public interface UserServiceClient {

    @PostMapping
    UserDTO createUser(@RequestBody UserDTO userDTO);

    @GetMapping("/email/{email}")
    UserDTO getUserByEmail(@PathVariable String email);
}
