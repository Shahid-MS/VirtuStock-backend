package com.virtu_stock.Exceptions;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.stereotype.Component;

import com.virtu_stock.Service.MailService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    private final MailService mailService;

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {

        System.err.println("ASYNC ERROR in method: " + method.getName());
        System.err.println("Error: " + ex.getMessage());

        // Extract user email from last parameter (if async method passed it)
        String userEmail = null;
        if (params != null && params.length > 0) {
            userEmail = String.valueOf(params[params.length - 1]);
        }

        if (userEmail != null) {

            Map<String, Object> error = new HashMap<>();
            error.put("title", "Async Task Failed");
            error.put("message", ex.getMessage());
            error.put("details", "Occurred in async method: " + method.getName());

            try {
                mailService.sendAsyncErrorMail(userEmail, error);
            } catch (Exception mailException) {
                System.err.println("Failed to send async error mail: " + mailException.getMessage());
            }
        }
    }
}
