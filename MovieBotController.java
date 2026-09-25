package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/*
 * =================================================================================
 * TASK 4: AI PROMPT & RESPONSE DOCUMENTATION
 * =================================================================================
 * PROMPT GIVEN TO AI:
 * "Write a Java method using OkHttp and Jackson ObjectMapper to send an HTTP POST 
 * request to the OpenAI chat completions API with a user's movie question, check 
 * for non-200 HTTP status codes (like 401 invalid API key), and adapt it into a 
 * Spring Boot @RestController endpoint mapped to POST /askMovieBot."
 *
 * AI'S SUGGESTED CODE SNIPPET:
 * okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonPayload, MediaType.parse("application/json"));
 * Request request = new Request.Builder()
 *     .url("https://api.openai.com/v1/chat/completions")
 *     .post(body)
 *     .addHeader("Authorization", "Bearer " + apiKey)
 *     .build();
 * Response response = client.newCall(request).execute();
 * if (!response.isSuccessful()) { return ResponseEntity.status(response.code()).body("Invalid API Key"); }
 * =================================================================================
 */

@RestController
public class MovieBotController {

    @Value("${openai.api.key:YOUR_OPENAI_API_KEY}")
    private String apiKey;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // DTO Class to map incoming JSON body: { "question": "..." }
    public static class QuestionRequest {
        public String question;

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }
    }

    // Task 2: Create POST endpoint /askMovieBot
    @SuppressWarnings("rawtypes")
    @PostMapping("/askMovieBot")
    public ResponseEntity askMovieBot(@RequestBody QuestionRequest requestDto) {

        if (requestDto == null || requestDto.question == null || requestDto.question.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"Please provide a 'question' field in your JSON body.\"}");
        }

        // Task 3 (Part A): Handle missing API key
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_OPENAI_API_KEY")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\": \"OpenAI API key is missing or invalid in application.properties.\"}");
        }

        try {
            String safeQuestion = requestDto.question.replace("\"", "\\\"");
            String jsonPayload = """
                {
                  "model": "gpt-4o-mini",
                  "messages": [
                    {"role": "system", "content": "You are a helpful movie expert bot."},
                    {"role": "user", "content": "%s"}
                  ]
                }
                """.formatted(safeQuestion);

            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    jsonPayload, MediaType.parse("application/json")
            );

            Request openAiRequest = new Request.Builder()
                    .url(OPENAI_URL)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(openAiRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                // Task 3 (Part B): Check HTTP status code and handle non-200 codes
                if (response.code() != 200) {
                    if (response.code() == 401) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body("{\"error\": \"Invalid OpenAI API key provided (HTTP 401).\"}");
                    } else if (response.code() == 429) {
                        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                .body("{\"error\": \"OpenAI API rate limit exceeded (HTTP 429).\"}");
                    } else {
                        return ResponseEntity.status(response.code())
                                .body("{\"error\": \"OpenAI API returned HTTP " + response.code() + "\"}");
                    }
                }

                // Task 2: Parse and return the OpenAI response
                JsonNode rootNode = objectMapper.readTree(responseBody);
                String answer = rootNode.path("choices").get(0).path("message").path("content").asText();

                return ResponseEntity.ok(answer);
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Server error occurred: " + e.getMessage() + "\"}");
        }
    }
}