package com.example.restservice;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONObject;
import java.io.OutputStream;

import org.apache.logging.log4j.message.Message;
import org.json.JSONArray;

@RestController
public class AiController {
    private static HashMap<String, JSONObject> jsonRequester = new HashMap<>();
    private static final AtomicInteger amount = new AtomicInteger(0);
    private static HashMap<String, Boolean> idRateLimit = new HashMap<>();
    @GetMapping("/bot")
    public AiReponse api(@RequestParam(value = "text", defaultValue = "none") String text,
            @RequestParam(value = "id", defaultValue = "none") String id) {
        amount.incrementAndGet();
        if (idRateLimit.get(id) == null || !idRateLimit.get(id)) {
            idRateLimit.put(id, true);
        }else {
            return new AiReponse("ID Rate Limited Reached, Please Wait");
        }

        if (amount.get() > 10) {
            return new AiReponse("Rate Limted Reached, Please Wait");
        }
        if (text.isBlank()) {
            return new AiReponse("");
        }
        try {
            try {
                JSONObject newMessage = new JSONObject();
                newMessage.put("role", "user");
                newMessage.put("content", text);
                jsonRequester.get(id).getJSONArray("messages").put(newMessage);
            } catch (Exception e) {
                JSONObject root = new JSONObject();
                root.put("model", "java-helper:latest");
                root.put("stream", false);
                JSONObject newMessage = new JSONObject();
                newMessage.put("role", "user");
                newMessage.put("content", text);
                root.put("messages", new JSONArray().put(newMessage));
                jsonRequester.put(id, root);
            }
            // Create a URI object
            URI uri = URI.create("http://localhost:11434/api/chat");

            // Create an HttpRequest object
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers
                            .ofString(jsonRequester.get(id).toString()))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                    HttpResponse.BodyHandlers.ofString());
            System.out.println("Response Code: " + response.statusCode());
            System.out.println("Response: " + response.body());
            System.out.println(jsonRequester.get(id).toString());
            JSONObject jsonObjectos = new JSONObject(response.body());
            String content = jsonObjectos.getJSONObject("message").getString("content");
            JSONObject newMessage = new JSONObject();
            newMessage.put("role", "assistant");
            newMessage.put("content", content);
            jsonRequester.get(id).getJSONArray("messages").put(newMessage);
            amount.decrementAndGet();
            idRateLimit.put(id, false);
            return new AiReponse(content);
        } catch (Exception e) {
            amount.decrementAndGet();
            idRateLimit.put(id, false);
            return new AiReponse(e.toString());
        }
    }
}