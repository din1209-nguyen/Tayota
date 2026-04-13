package com.nguyendin.authservice.service;

import com.nguyendin.authservice.dto.Request.RegisterRequestDTO;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    public void register(RegisterRequestDTO registerRequestDTO) {
        System.out.println("Da dang ki");
    }
}
