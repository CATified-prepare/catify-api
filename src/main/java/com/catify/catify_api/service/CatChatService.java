package com.catify.catify_api.service;

import com.catify.catify_api.dto.AiMetadata;
import com.catify.catify_api.dto.ChatRequest;
import com.catify.catify_api.dto.ChatResponse;
import com.catify.catify_api.exception.CatifyException;
import com.catify.catify_api.mapper.AiMetadataMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static com.catify.catify_api.constant.CatifyConstants.AI_EMPTY_RESPONSE_MESSAGE;
import static com.catify.catify_api.constant.CatifyConstants.ERROR_CODE_500;

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

    private final AiMetadataMapper aiMetadataMapper;

    public CatChatService(ChatClient.Builder builder, AiMetadataMapper aiMetadataMapper) {
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
        this.aiMetadataMapper = aiMetadataMapper;
    }

    public ChatResponse ask(ChatRequest request) {
        ChatClientResponse response = chatClient.prompt()
                .user(request.question())
                .call()
                .chatClientResponse();

        var chatResponse = response.chatResponse();

        String answer = chatResponse == null ? null : Objects.requireNonNull(chatResponse.getResult()).getOutput().getText();

        if (answer == null || answer.isEmpty()) {
            throw new CatifyException(ERROR_CODE_500, AI_EMPTY_RESPONSE_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        AiMetadata ai = aiMetadataMapper.toAiMetadata(chatResponse.getMetadata());
        int tokensUsed = ai != null && ai.totalTokens() != null ? ai.totalTokens() : 0;

        return new ChatResponse(answer, List.of(), tokensUsed, ai);
    }
}
