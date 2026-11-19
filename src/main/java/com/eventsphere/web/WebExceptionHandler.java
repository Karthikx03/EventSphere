package com.eventsphere.web;

import com.eventsphere.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Shows a friendly 404 page when a URL points at something that doesn't exist,
 * including malformed ids such as /events/abc. Never exposes exception details.
 */
@ControllerAdvice(basePackages = "com.eventsphere.web")
public class WebExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("message", "Sorry, we couldn't find that event or booking.");
        return "error/404";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleBadUrl(MethodArgumentTypeMismatchException ex, Model model) {
        model.addAttribute("message", "That page does not exist.");
        return "error/404";
    }
}
