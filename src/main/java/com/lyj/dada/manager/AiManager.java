package com.lyj.dada.manager;

import com.lyj.dada.config.AiConfig;
import com.lyj.dada.exception.BusinessException;
import com.lyj.dada.exception.ErrorCode;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import jakarta.annotation.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class AiManager {

    @Resource
    private AiConfig aiConfig;

    private static final float STABLE_TEMPERATURE = 0.2f;
    private static final float UNSTABLE_TEMPERATURE = 0.9f;

    public String doSyncStableRequest(String systemMessage, String userMessage) {
        return doRequest(systemMessage, userMessage, STABLE_TEMPERATURE);
    }

    public String doSyncUnstableRequest(String systemMessage, String userMessage) {
        return doRequest(systemMessage, userMessage, UNSTABLE_TEMPERATURE);
    }

    public String doSyncRequest(String systemMessage, String userMessage, Float temperature) {
        if (temperature == null) {
            temperature = STABLE_TEMPERATURE;
        }
        return doRequest(systemMessage, userMessage, temperature);
    }

    private String doRequest(String systemMessage, String userMessage, Float temperature) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> body = new HashMap<>();
            body.put("model", aiConfig.getModel());
            body.put("temperature", temperature);

            List<Map<String, String>> messages = new ArrayList<>();

            Map<String, String> system = new HashMap<>();
            system.put("role", "system");
            system.put("content", systemMessage);

            Map<String, String> user = new HashMap<>();
            user.put("role", "user");
            user.put("content", userMessage);

            messages.add(system);
            messages.add(user);

            body.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(aiConfig.getApiKey());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiConfig.getUrl(),
                    entity,
                    Map.class
            );

            Map result = response.getBody();

            List choices = (List) result.get("choices");
            Map choice = (Map) choices.get(0);
            Map message = (Map) choice.get("message");

            return message.get("content").toString();

        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 调用失败");
        }
    }

    public Flowable<String> doStreamRequest(String systemMessage, String userMessage) {
        return Flowable.create(emitter -> {
            try {
                RestTemplate restTemplate = new RestTemplate();

                Map<String, Object> body = new HashMap<>();
                body.put("model", aiConfig.getModel());
                body.put("stream", true);

                List<Map<String, String>> messages = new ArrayList<>();

                Map<String, String> system = new HashMap<>();
                system.put("role", "system");
                system.put("content", systemMessage);

                Map<String, String> user = new HashMap<>();
                user.put("role", "user");
                user.put("content", userMessage);

                messages.add(system);
                messages.add(user);

                body.put("messages", messages);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(aiConfig.getApiKey());

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

                ResponseEntity<String> response = restTemplate.postForEntity(
                        aiConfig.getUrl(),
                        entity,
                        String.class
                );

                emitter.onNext(response.getBody());
                emitter.onComplete();

            } catch (Exception e) {
                emitter.onError(e);
            }
        }, BackpressureStrategy.BUFFER);
    }


}