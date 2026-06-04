package com.catify.catify_api.controller;

import com.catify.catify_api.dto.request.chat.ChatRequest;
import com.catify.catify_api.dto.response.chat.ChatResponse;
import com.catify.catify_api.service.CatChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cat")
public class ChatController {

    private final CatChatService catChatService;

    public ChatController(CatChatService catChatService) {
        this.catChatService = catChatService;
    }

    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> ask(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(catChatService.ask(request));
    }
}

