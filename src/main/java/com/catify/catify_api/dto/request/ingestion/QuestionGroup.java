package com.catify.catify_api.dto.request.ingestion;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuestionGroup {

    String section;

    Integer year;

    String topic;

    String subtopic;

    String context;

    List<Question> questions;

}