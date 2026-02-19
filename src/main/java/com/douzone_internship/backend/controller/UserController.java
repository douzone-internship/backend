package com.douzone_internship.backend.controller;

import com.douzone_internship.backend.auth.CurrentUser;
import com.douzone_internship.backend.auth.CustomUserDetails;
import com.douzone_internship.backend.service.UserService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
@Getter
@Setter
public class UserController {

    private final UserService userService;

    public ResponseEntity<?> getUserFavorite(@CurrentUser CustomUserDetails userDetails) {
        return userService.getUserFavorite(userDetails.getUserId());
    }
}
