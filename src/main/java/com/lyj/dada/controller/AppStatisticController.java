package com.lyj.dada.controller;


import com.lyj.dada.common.BaseResponse;
import com.lyj.dada.common.ResultUtils;
import com.lyj.dada.exception.ErrorCode;
import com.lyj.dada.exception.ThrowUtils;
import com.lyj.dada.mapper.UserAnswerMapper;
import com.lyj.dada.model.dto.statistic.AppAnswerCountDTO;
import com.lyj.dada.model.dto.statistic.AppAnswerResultCountDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * App 统计分析接口
 */
@RestController
@RequestMapping("/app/statistic")
@Slf4j
public class AppStatisticController {

    @Resource
    private UserAnswerMapper userAnswerMapper;

    /**
     * 热门应用及回答数统计（top 10）
     *
     * @return
     */
    @GetMapping("/answer_count")
    public BaseResponse<List<AppAnswerCountDTO>> getAppAnswerCount() {
        //1. 返回结果
        return ResultUtils.success(userAnswerMapper.doAppAnswerCount());
    }

    /**
     * 某应用回答结果分布统计
     *
     * @param appId
     * @return
     */
    @GetMapping("/answer_result_count")
    public BaseResponse<List<AppAnswerResultCountDTO>> getAppAnswerResultCount(Long appId) {
        //1. 校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR);
        //2. 返回结果
        return ResultUtils.success(userAnswerMapper.doAppAnswerResultCount(appId));
    }
}
