package com.catify.catify_api.dto.request.ingestion;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class PyqIngestionRequest {

    List<QuestionGroup> questionGroups;

}