package com.catify.catify_api.service;

import com.catify.catify_api.dto.request.ingestion.PyqIngestionRequest;
import com.catify.catify_api.dto.request.ingestion.Question;
import com.catify.catify_api.dto.request.ingestion.QuestionGroup;
import com.catify.catify_api.dto.response.ingestion.PyqIngestionResponse;
import com.catify.catify_api.exception.CatifyException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.catify.catify_api.constant.CatifyConstants.CatifyGenericConstants.*;
import static com.catify.catify_api.constant.CatifyConstants.ErrorMessageConstants.EMPTY_REQUEST_RECEIVED_RESPONSE_MESSAGE;
import static com.catify.catify_api.constant.CatifyConstants.ErrorMessageConstants.ERROR_CODE_400;

@Service
@AllArgsConstructor
@Slf4j
public class DataIngestionService {

    private final VectorStore vectorStore;

    public PyqIngestionResponse ingestPyq(PyqIngestionRequest pyqIngestionRequest) {
        if (pyqIngestionRequest == null
                || pyqIngestionRequest.getQuestionGroups() == null
                || pyqIngestionRequest.getQuestionGroups().isEmpty()) {
            throw new CatifyException(ERROR_CODE_400, EMPTY_REQUEST_RECEIVED_RESPONSE_MESSAGE, HttpStatus.BAD_REQUEST);
        }

        List<Document> documents = pyqIngestionRequest.getQuestionGroups().stream()
                .flatMap(questionGroup -> buildDocuments(questionGroup).stream())
                .toList();

        vectorStore.add(documents);
        log.info("Ingested {} documents into vector store", documents.size());

        return new PyqIngestionResponse(DATA_INGESTION_SUCCESS_RESPONSE, documents.size());
    }

    private List<Document> buildDocuments(QuestionGroup questionGroup) {
        String header = buildHeader(questionGroup);
        String context = buildContext(questionGroup);

        return questionGroup.getQuestions().stream()
                .map(question -> buildQuestionDocument(questionGroup, question, header, context))
                .toList();
    }

    private Document buildQuestionDocument(QuestionGroup questionGroup, Question question, String header, String context) {
        StringBuilder sb = new StringBuilder();

        sb.append(header);
        sb.append("Type: ").append(question.getType())
                .append(" | Difficulty: ").append(question.getDifficulty())
                .append("\n");

        if (!context.isEmpty()) {
            sb.append(context.replace("\n", ""));
        }

        sb.append("\nQuestion ").append(": ")
                .append(question.getProblem().replace("\n", ""))
                .append("\n");

        List<String> options = question.getOptions();
        if (options != null && !options.isEmpty()) {
            sb.append("\nOptions:\n");
            char label = 'A';
            for (String option : options) {
                sb.append(label++).append(". ").append(option).append("\n");
            }
        }

        if (question.getAns() != null && !question.getAns().isBlank()) {
            sb.append("\nAnswer: ").append(question.getAns()).append("\n");
        }

        Map<String, Object> metadata = buildMetadata(questionGroup, question);

        return new Document(sb.toString(), metadata);
    }

    private String buildHeader(QuestionGroup questionGroup) {
        return "Section: " + questionGroup.getSection() +
                " | Topic: " + questionGroup.getTopic() +
                " | Subtopic: " + questionGroup.getSubtopic() + "\n";
    }

    private String buildContext(QuestionGroup questionGroup) {
        if (questionGroup.getContext() == null || questionGroup.getContext().isBlank()) {
            return "";
        }
        return "Context: " + questionGroup.getContext() + "\n";
    }

    private Map<String, Object> buildMetadata(QuestionGroup questionGroup, Question question) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(SECTION,    safeLower(questionGroup.getSection()));
        metadata.put(TOPIC,      safeLower(questionGroup.getTopic()));
        metadata.put(SUBTOPIC,   safeLower(questionGroup.getSubtopic()));
        metadata.put(TYPE,       safeLower(question.getType()));
        metadata.put(DIFFICULTY, safeLower(question.getDifficulty()));
        metadata.put(YEAR,       questionGroup.getYear());

        return metadata;
    }
    private static String safeLower(String s) {
        return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
    }

}