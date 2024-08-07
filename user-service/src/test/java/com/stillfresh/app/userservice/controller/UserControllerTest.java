package com.stillfresh.app.userservice.controller;

import com.stillfresh.app.userservice.model.User;
import com.stillfresh.app.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class UserControllerTest {

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testRegisterUser() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password");

        when(userService.saveUser(any(User.class))).thenReturn(user);

        ResponseEntity<String> responseEntity = userController.registerUser(user);

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertEquals(user, responseEntity.getBody());
    }

    @Test
    public void testLoginUser_Success() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password");

        when(userService.loginUser(any(User.class))).thenReturn("Login successful");

        ResponseEntity<String> responseEntity = userController.loginUser(user);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("Login successful", responseEntity.getBody());
    }

    @Test
    public void testLoginUser_Failure() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password");

        when(userService.loginUser(any(User.class))).thenReturn("Invalid credentials");

        ResponseEntity<String> responseEntity = userController.loginUser(user);

        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertEquals("Invalid credentials", responseEntity.getBody());
    }
}
