package com.sliitreserve.api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Favicon endpoint handler
 * Prevents 401/500 errors from missing favicon.ico file
 */
@RestController
public class FaviconController {
    /**
     * Handle favicon.ico requests - returns 204 No Content
     */
    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }
}
