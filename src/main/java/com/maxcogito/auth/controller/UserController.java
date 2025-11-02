package com.maxcogito.auth.controller;

import com.maxcogito.auth.domain.User;
import com.maxcogito.auth.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/test")
public class UserController {
    private final UserService userService;

    Logger log = LoggerFactory.getLogger(UserController.class);

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(path = "/users",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<User>> getAllUsers() {
        log.debug("REST request to get all Users");
        List<User> users = userService.findAll();

        return ResponseEntity.ok(users);
    }

    @GetMapping(path= "/user/{username}",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> getUser(@PathVariable String username) {
        log.debug("REST request to get single User");
        Optional<User> user = userService.findByUsernameOrEmail(username);
        return ResponseEntity.ok(user.get());
    }
}
