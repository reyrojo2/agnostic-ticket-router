package com.agnosticrouter.decision;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public final class SystemOneModels {
    private SystemOneModels() {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Request(
            Object state,
            Map<String, Question> questions,
            String model,
            String lang,
            @JsonProperty("min_confidence") Double minConfidence
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Question(String type, String instructions, Object criteria) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(String model, Map<String, Answer> answers, Usage usage, Routing routing) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Answer(
            String type,
            String choice,
            Double score,
            Double noul,
            Map<String, Double> probabilities,
            Map<String, String> legend,
            Double confidence,
            @JsonProperty("answer_confidence") Double answerConfidence,
            @JsonProperty("low_confidence") Boolean lowConfidence,
            String abstention
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @JsonProperty("state_tokens") Integer stateTokens,
            @JsonProperty("state_tokens_dropped") Integer stateTokensDropped,
            Boolean truncated,
            @JsonProperty("truncated_questions") List<String> truncatedQuestions
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Routing(String model, String reason) {}
}
