package com.catify.catify_api.service;

import com.catify.catify_api.dto.ChatRequest;
import com.catify.catify_api.dto.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatChatService {

    private static final String SYSTEM_PROMPT = """
            You are a CAT India exam assistant named Catify.
            ONLY answer questions related to the CAT (Common Admission Test) India MBA entrance exam.
            Topics you can help with: Quantitative Aptitude, VARC (Verbal Ability & Reading Comprehension),
            DILR (Data Interpretation & Logical Reasoning), exam strategy, syllabus, cutoffs, and IIM admissions.
            If the question is NOT related to CAT exam, politely decline and say:
            "I can only help with CAT India exam questions. Please ask something related to CAT."
            Always be concise, accurate, and helpful.
            """;

    private final ChatClient chatClient;

    public CatChatService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    public ChatResponse ask(ChatRequest request) {
        String answer = chatClient.prompt()
                .user(request.question())
                .call()
                .content();

        return new ChatResponse(answer, List.of(), 0);
    }
}

