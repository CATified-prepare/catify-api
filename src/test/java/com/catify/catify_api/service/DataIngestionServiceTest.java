package com.catify.catify_api.service;

import com.catify.catify_api.dto.request.ingestion.PyqIngestionRequest;
import com.catify.catify_api.dto.request.ingestion.Question;
import com.catify.catify_api.dto.request.ingestion.QuestionGroup;
import com.catify.catify_api.dto.response.ingestion.PyqIngestionResponse;
import com.catify.catify_api.exception.CatifyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.List;

import static com.catify.catify_api.constant.CatifyConstants.CatifyGenericConstants.DATA_INGESTION_SUCCESS_RESPONSE;
import static com.catify.catify_api.constant.CatifyConstants.CatifyGenericConstants.DIFFICULTY;
import static com.catify.catify_api.constant.CatifyConstants.CatifyGenericConstants.SECTION;
import static com.catify.catify_api.constant.CatifyConstants.CatifyGenericConstants.SUBTOPIC;
import static com.catify.catify_api.constant.CatifyConstants.CatifyGenericConstants.TOPIC;
import static com.catify.catify_api.constant.CatifyConstants.CatifyGenericConstants.TYPE;
import static com.catify.catify_api.constant.CatifyConstants.ErrorMessageConstants.EMPTY_REQUEST_RECEIVED_RESPONSE_MESSAGE;
import static com.catify.catify_api.constant.CatifyConstants.ErrorMessageConstants.ERROR_CODE_400;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DataIngestionServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Test
    void ingestPyqShouldRejectNullRequest() {
        DataIngestionService service = new DataIngestionService(vectorStore);

        assertThatThrownBy(() -> service.ingestPyq(null))
                .isInstanceOf(CatifyException.class)
                .satisfies(ex -> {
                    CatifyException catifyException = (CatifyException) ex;
                    assertThat(catifyException.getCode()).isEqualTo(ERROR_CODE_400);
                    assertThat(catifyException.getMessage()).isEqualTo(EMPTY_REQUEST_RECEIVED_RESPONSE_MESSAGE);
                    assertThat(catifyException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void ingestPyqShouldRejectEmptyQuestionGroups() {
        DataIngestionService service = new DataIngestionService(vectorStore);

        PyqIngestionRequest request = new PyqIngestionRequest();
        request.setQuestionGroups(List.of());

        assertThatThrownBy(() -> service.ingestPyq(request))
                .isInstanceOf(CatifyException.class)
                .satisfies(ex -> {
                    CatifyException catifyException = (CatifyException) ex;
                    assertThat(catifyException.getCode()).isEqualTo(ERROR_CODE_400);
                    assertThat(catifyException.getMessage()).isEqualTo(EMPTY_REQUEST_RECEIVED_RESPONSE_MESSAGE);
                    assertThat(catifyException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void ingestPyqShouldConvertGroupsIntoVectorStoreDocuments() throws Exception {
        DataIngestionService service = new DataIngestionService(vectorStore);

        Question firstQuestion = new Question();
        firstQuestion.setQno("1");
        firstQuestion.setType("mcq");
        firstQuestion.setDifficulty("easy");
        firstQuestion.setProblem("What is 2 + 2?");
        firstQuestion.setOptions(List.of("1", "2", "4"));
        firstQuestion.setAns("4");

        Question secondQuestion = new Question();
        secondQuestion.setQno("2");
        secondQuestion.setType("subjective");
        secondQuestion.setDifficulty("medium");
        secondQuestion.setProblem("Explain CAT syllabus.");
        secondQuestion.setOptions(List.of());
        secondQuestion.setAns("");

        QuestionGroup group = new QuestionGroup();
        group.setSection("Quant");
        group.setTopic("Arithmetic");
        group.setSubtopic("Numbers");
        group.setContext("Use basic arithmetic.");
        group.setQuestions(List.of(firstQuestion, secondQuestion));

        PyqIngestionRequest request = new PyqIngestionRequest();
        request.setQuestionGroups(List.of(group));

        PyqIngestionResponse response = service.ingestPyq(request);

        assertThat(response.message()).isEqualTo(DATA_INGESTION_SUCCESS_RESPONSE);
        assertThat(response.documentsIngested()).isEqualTo(2);

        ArgumentCaptor<List<Document>> documentCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(documentCaptor.capture());

        List<Document> documents = documentCaptor.getValue();
        assertThat(documents).hasSize(2);

        Document firstDocument = documents.getFirst();
        assertThat(firstDocument.getMetadata())
                .containsEntry(SECTION, "Quant")
                .containsEntry(TOPIC, "Arithmetic")
                .containsEntry(SUBTOPIC, "Numbers")
                .containsEntry(TYPE, "mcq")
                .containsEntry(DIFFICULTY, "easy");
        assertThat(extractDocumentText(firstDocument))
                .contains("Section: Quant | Topic: Arithmetic | Subtopic: Numbers")
                .contains("Type: mcq | Difficulty: easy")
                .contains("Context: Use basic arithmetic.")
                .contains("Question : What is 2 + 2?")
                .contains("A. 1")
                .contains("B. 2")
                .contains("C. 4")
                .contains("Answer: 4");

        Document secondDocument = documents.get(1);
        assertThat(secondDocument.getMetadata())
                .containsEntry(SECTION, "Quant")
                .containsEntry(TOPIC, "Arithmetic")
                .containsEntry(SUBTOPIC, "Numbers")
                .containsEntry(TYPE, "subjective")
                .containsEntry(DIFFICULTY, "medium");
        assertThat(extractDocumentText(secondDocument))
                .contains("Type: subjective | Difficulty: medium")
                .contains("Question : Explain CAT syllabus.");
    }

    private static String extractDocumentText(Document document) throws IllegalAccessException {
        for (Field field : document.getClass().getDeclaredFields()) {
            if (field.getType() == String.class) {
                field.setAccessible(true);
                Object value = field.get(document);
                if (value != null) {
                    String text = value.toString();
                    if (text.contains("Section: ") || text.contains("Question : ")) {
                        return text;
                    }
                }
            }
        }
        throw new IllegalStateException("Unable to locate document text field");
    }
}
