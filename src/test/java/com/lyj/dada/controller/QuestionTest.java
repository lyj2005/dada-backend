package com.lyj.dada.controller;

import com.lyj.dada.model.dto.question.AiGenerateQuestionRequest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class QuestionTest {

    @Resource
    private QuestionController questionController;

    @Test
    void aiGenerateQuestionSSEVIPTest() throws InterruptedException {
        //1. 设置参数
        AiGenerateQuestionRequest request = new AiGenerateQuestionRequest();
        request.setAppId(3L);
        request.setQuestionNumber(10);
        request.setOptionNumber(2);

        //2. 测试调用
        questionController.aiGenerateQuestionSSETest(request, false);
        questionController.aiGenerateQuestionSSETest(request, false);
        questionController.aiGenerateQuestionSSETest(request, true);

        Thread.sleep(1000000L);


    }

}
