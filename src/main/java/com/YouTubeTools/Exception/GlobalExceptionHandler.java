package com.YouTubeTools.Exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception: ", e);

        // For API requests (JSON)
        if (request.getHeader("Content-Type") != null &&
                request.getHeader("Content-Type").contains("application/json")) {

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", true);
            errorResponse.put("message", "An internal server error occurred");
            errorResponse.put("details", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }

        // For web requests (HTML)
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("error", "An unexpected error occurred");
        mav.addObject("message", e.getMessage());
        mav.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return mav;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("Invalid argument: ", e);

        if (request.getHeader("Content-Type") != null &&
                request.getHeader("Content-Type").contains("application/json")) {

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", true);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }

        ModelAndView mav = new ModelAndView("error");
        mav.addObject("error", e.getMessage());
        mav.addObject("status", HttpStatus.BAD_REQUEST.value());
        return mav;
    }
}