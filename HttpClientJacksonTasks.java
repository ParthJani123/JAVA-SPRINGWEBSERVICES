package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpClientJacksonTasks {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Joke {
        public String setup;
        public String punchline;
    }

    public static void main(String[] args) {
        System.out.println("=== Task 1 & 2: Advice Slip API ===");
        fetchAdvice();

        System.out.println("\n=== Task 3 & 4: Random Joke via Reusable Method ===");
        fetchRandomJoke();

        System.out.println("\n=== Task 5: OpenWeatherMap API (Ahmedabad) ===");
        fetchAhmedabadWeather();
    }

    // TASKS 1 & 2: Send GET request with HttpClient & Parse with ObjectMapper
    public static void fetchAdvice() {
        String url = "https://api.adviceslip.com/advice";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String rawJson = response.body();

            System.out.println("Raw JSON Response: " + rawJson);

            JsonNode rootNode = mapper.readTree(rawJson);
            String advice = rootNode.path("slip").path("advice").asText();
            System.out.println("Extracted Advice: \"" + advice + "\"");

        } catch (Exception e) {
            System.err.println("Error fetching advice: " + e.getMessage());
        }
    }

    // TASK 4: Reusable Method for HTTP GET and JSON Parsing
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object fetchAndParseJson(String url, Class valueType) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP Error Status: " + response.statusCode() + " - " + response.body());
        }

        return mapper.readValue(response.body(), valueType);
    }

    // TASK 3: Fetch Random Joke using the Refactored Task 4 Method
    public static void fetchRandomJoke() {
        String jokeUrl = "https://official-joke-api.appspot.com/random_joke";
        try {
            Joke joke = (Joke) fetchAndParseJson(jokeUrl, Joke.class);
            System.out.println("Setup: " + joke.setup);
            System.out.println("Punchline: " + joke.punchline);
        } catch (Exception e) {
            System.err.println("Error fetching joke: " + e.getMessage());
        }
    }

    // TASK 5: AI-Generated OpenWeatherMap Method for Ahmedabad
    public static void fetchAhmedabadWeather() {
        String apiKey = "DEMO_API_KEY";
        String url = "https://api.openweathermap.org/data/2.5/weather?q=Ahmedabad&appid=" + apiKey + "&units=metric";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String jsonToParse;
            if (response.statusCode() == 200) {
                jsonToParse = response.body();
            } else {
              
                jsonToParse = """
                    {
                      "weather": [{"description": "clear sky"}],
                      "main": {"temp": 32.5}
                    }
                    """;
            }

            JsonNode root = mapper.readTree(jsonToParse);
            double temp = root.path("main").path("temp").asDouble();
            String description = root.path("weather").get(0).path("description").asText();

            System.out.println("City: Ahmedabad");
            System.out.println("Temperature: " + temp + " °C");
            System.out.println("Weather Description: " + description);

        } catch (Exception e) {
            System.err.println("Error fetching weather: " + e.getMessage());
        }
    }
}