package com.lyj.dada.scoring;

import com.lyj.dada.model.entity.App;
import com.lyj.dada.model.entity.UserAnswer;

import java.util.List;

/**
 * 策略接口
 */
public interface ScoringStrategy {

    /**
     * 执行评分
     *
     * @param choices
     * @param app
     * @return
     * @throws Exception
     */
    UserAnswer doScore(List<String> choices, App app) throws Exception;
}