package com.example.booking.service;

import com.example.booking.dto.request.LoginRequest;
import com.example.booking.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
