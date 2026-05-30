package com.catify.catify_api.controller;

import com.catify.catify_api.dto.request.ingestion.PyqIngestionRequest;
import com.catify.catify_api.dto.response.ingestion.PyqIngestionResponse;
import com.catify.catify_api.service.DataIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ingest")
@RequiredArgsConstructor
public class DataIngestionController {

    private final DataIngestionService dataIngestionService;

    @PostMapping(value = "/pyq")
    public ResponseEntity<PyqIngestionResponse> ingestPyq(@RequestBody PyqIngestionRequest pyqIngestionRequest){
        PyqIngestionResponse pyqIngestionResponse = dataIngestionService.ingestPyq(pyqIngestionRequest);
        return new ResponseEntity<>(pyqIngestionResponse, HttpStatus.CREATED);
    }
}
