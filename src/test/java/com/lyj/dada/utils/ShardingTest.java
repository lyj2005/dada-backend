package com.lyj.dada.utils;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lyj.dada.model.entity.UserAnswer;
import com.lyj.dada.service.UserAnswerService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ShardingTest {

        @Resource
        private UserAnswerService userAnswerService;

        @Test
        void test() {

            UserAnswer userAnswer1 = new UserAnswer();

            userAnswer1.setAppId(1L);
            userAnswer1.setUserId(1L);
            userAnswer1.setChoices("1");
            userAnswerService.save(userAnswer1);

            UserAnswer userAnswer2 = new UserAnswer();
            userAnswer2.setAppId(2L);
            userAnswer2.setUserId(1L);
            userAnswer2.setChoices("2");
            userAnswerService.save(userAnswer2);

            UserAnswer userAnswerOne = userAnswerService.getOne(Wrappers.lambdaQuery(UserAnswer.class).eq(UserAnswer::getAppId, 1L).last("limit 1"));
            System.out.println(JSONUtil.toJsonStr(userAnswerOne));

            UserAnswer userAnswerTwo = userAnswerService.getOne(Wrappers.lambdaQuery(UserAnswer.class).eq(UserAnswer::getAppId, 2L).last("limit 1"));
            System.out.println(JSONUtil.toJsonStr(userAnswerTwo));
        }

}
