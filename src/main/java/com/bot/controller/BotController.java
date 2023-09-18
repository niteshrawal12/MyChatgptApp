package com.bot.controller;

import com.bot.dto.GPTRequest;
import com.bot.dto.GPTResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/bot")
public class BotController {
    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String api;
    @Autowired
    private final RestTemplate restTemplate;

    public BotController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/chat")
    public ResponseEntity<String> chat(@RequestParam("prompt") String prompt) {
        try {
            // Validate the prompt
            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Prompt cannot be empty.");
            }

            GPTRequest request = new GPTRequest(model, prompt);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(api); // Replace with your API key

            HttpEntity<GPTRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<GPTResponse> responseEntity = restTemplate.exchange(
                    api,
                    HttpMethod.POST,
                    entity,
                    GPTResponse.class
            );

            GPTResponse gptResponse = responseEntity.getBody();

            if (gptResponse != null && !gptResponse.getChoices().isEmpty()) {
                String message = gptResponse.getChoices().get(0).getMessage().getContent();
                return ResponseEntity.ok(message);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to get a valid response from OpenAI.");
            }
        } catch (HttpClientErrorException.BadRequest badRequestException) {
            // Handle 400 Bad Request from the OpenAI API
            return ResponseEntity.badRequest().body("Bad Request: " + badRequestException.getResponseBodyAsString());
        } catch (Exception e) {
            // Handle other exceptions, e.g., network issues, timeouts
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error communicating with the OpenAI API.");
        }
    }
}
