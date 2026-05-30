package com.catify.catify_api.dto.request.ingestion;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Question {

    String qno;

    String type;

    String difficulty;

    String problem;

    List<String> options;

    String ans;
}