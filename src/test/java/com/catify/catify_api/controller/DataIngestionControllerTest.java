package com.catify.catify_api.controller;

import com.catify.catify_api.dto.response.ingestion.PyqIngestionResponse;
import com.catify.catify_api.service.DataIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DataIngestionControllerTest {

    @Mock
    private DataIngestionService dataIngestionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DataIngestionController(dataIngestionService)).build();
    }

    @Test
    void ingestPyqShouldReturnCreatedResponse() throws Exception {
        when(dataIngestionService.ingestPyq(any())).thenReturn(new PyqIngestionResponse("PYQ data ingested successfully", 2));

        mockMvc.perform(post("/api/v1/ingest/pyq")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionGroups": [
                                    {
                                      "section": "Quant",
                                      "topic": "Arithmetic",
                                      "subtopic": "Numbers",
                                      "context": "Use basic arithmetic.",
                                      "questions": [
                                        {
                                          "qno": "1",
                                          "type": "mcq",
                                          "difficulty": "easy",
                                          "problem": "What is 2 + 2?",
                                          "options": ["1", "2", "4"],
                                          "ans": "4"
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("PYQ data ingested successfully"))
                .andExpect(jsonPath("$.documentsIngested").value(2));
    }
}
