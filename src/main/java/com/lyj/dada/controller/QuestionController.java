package com.lyj.dada.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.lyj.dada.annotation.AuthCheck;
import com.lyj.dada.common.BaseResponse;
import com.lyj.dada.common.DeleteRequest;
import com.lyj.dada.common.ResultUtils;
import com.lyj.dada.constant.UserConstant;
import com.lyj.dada.exception.BusinessException;
import com.lyj.dada.exception.ErrorCode;
import com.lyj.dada.exception.ThrowUtils;
import com.lyj.dada.manager.AiManager;
import com.lyj.dada.model.dto.question.*;
import com.lyj.dada.model.entity.App;
import com.lyj.dada.model.entity.Question;
import com.lyj.dada.model.entity.User;
import com.lyj.dada.model.enums.AppTypeEnum;
import com.lyj.dada.model.vo.QuestionVO;
import com.lyj.dada.service.AppService;
import com.lyj.dada.service.QuestionService;
import com.lyj.dada.service.UserService;
import com.zhipu.oapi.service.v4.model.ModelData;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 题目接口
 */
@RestController
@RequestMapping("/question")
@Slf4j
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserService userService;

    @Resource
    private AppService appService;

    @Resource
    private AiManager aiManager;

    @Resource
    private Scheduler vipScheduler;



    // region 增删改查代码

    /**
     * 创建题目
     *
     * @param questionAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {

        //1. 校验
        ThrowUtils.throwIf(questionAddRequest == null, ErrorCode.PARAMS_ERROR);
        //2.  在此处将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        List<QuestionContentDTO> questionContentDTO = questionAddRequest.getQuestionContent();
        question.setQuestionContent(JSONUtil.toJsonStr(questionContentDTO));
        //3.  数据校验
        questionService.validQuestion(question, true);
        //4.  填充默认值
        User loginUser = userService.getLoginUser(request);
        question.setUserId(loginUser.getId());
        //5.   写入数据库
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        //6.  返回新写入的数据 id
        long newQuestionId = question.getId();
        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除题目
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        //1. 校验
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        //2.  判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        //3.  仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //4.  操作数据库
        boolean result = questionService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        //5. 返回结果
        return ResultUtils.success(true);
    }

    /**
     * 更新题目（仅管理员可用）
     *
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        //1. 校验
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2.  在此处将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        List<QuestionContentDTO> questionContentDTO = questionUpdateRequest.getQuestionContent();
        question.setQuestionContent(JSONUtil.toJsonStr(questionContentDTO));
        //3.  数据校验
        questionService.validQuestion(question, false);
        //4.  判断是否存在
        long id = questionUpdateRequest.getId();
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        //5.  操作数据库
        boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        //6. 返回结果
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取题目（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(long id, HttpServletRequest request) {
        //1. 校验
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        //2.  查询数据库
        Question question = questionService.getById(id);
        ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR);
        //3.  获取封装类
        return ResultUtils.success(questionService.getQuestionVO(question, request));
    }

    /**
     * 分页获取题目列表（仅管理员可用）
     *
     * @param questionQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
       //1. 获取分页信息
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        //2.  查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        //3. 返回结果
        return ResultUtils.success(questionPage);
    }

    /**
     * 分页获取题目列表（封装类）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                               HttpServletRequest request) {

        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 分页获取当前登录用户创建的题目列表
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        questionQueryRequest.setUserId(loginUser.getId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 编辑题目（给用户使用）
     *
     * @param questionEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest, HttpServletRequest request) {
        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 在此处将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);
        List<QuestionContentDTO> questionContentDTO = questionEditRequest.getQuestionContent();
        question.setQuestionContent(JSONUtil.toJsonStr(questionContentDTO));
        // 数据校验
        questionService.validQuestion(question, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = questionEditRequest.getId();
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestion.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion




    // region AI 生成题目功能代码



    private static final String GENERATE_QUESTION_SYSTEM_MESSAGE = "你是一位严谨的出题专家，我会给你如下信息：\n" +
            "```\n" +
            "应用名称，\n" +
            "【【【应用描述】】】，\n" +
            "应用类别，\n" +
            "要生成的题目数，\n" +
            "每个题目的选项数\n" +
            "```\n" +
            "\n" +
            "请你根据上述信息，按照以下步骤来出题：\n" +
            "1. 要求：题目和选项尽可能地短，题目不要包含序号，每题的选项数以我提供的为主，题目不能重复\n" +
            "2. 严格按照下面的 json 格式输出题目和选项\n" +
            "```\n" +
            "[{\"options\":[{\"value\":\"选项内容\",\"key\":\"A\"},{\"value\":\"\",\"key\":\"B\"}],\"title\":\"题目标题\"}]\n" +
            "```\n" +
            "title 是题目，options 是选项，每个选项的 key 按照英文字母序（比如 A、B、C、D）以此类推，value 是选项内容\n" +
            "3. 检查题目是否包含序号，若包含序号则去除序号\n" +
            "4. 返回的题目列表格式必须为 JSON 数组";



    /**
     * 构建生成题目的用户消息
     *
     * @param app
     * @param questionNumber
     * @param optionNumber
     * @return
     */
    private String getGenerateQuestionUserMessage(App app, int questionNumber, int optionNumber) {
        //1. 构建用户信息
        StringBuilder userMessage = new StringBuilder();
        userMessage.append(app.getAppName()).append("\n");
        userMessage.append(app.getAppDesc()).append("\n");
        userMessage.append(AppTypeEnum.getEnumByValue(app.getAppType()).getText() + "类").append("\n");
        userMessage.append(questionNumber).append("\n");
        userMessage.append(optionNumber);
        //2. 返回结果
        return userMessage.toString();
    }

    /**
     * AI生成题目(普通版）
     * @param aiGenerateQuestionRequest
     * @return
     */
    @PostMapping("/ai_generate")
    public BaseResponse<List<QuestionContentDTO>> aiGenerateQuestion(
            @RequestBody AiGenerateQuestionRequest aiGenerateQuestionRequest) {
        //1. 校验
        ThrowUtils.throwIf(aiGenerateQuestionRequest == null, ErrorCode.PARAMS_ERROR);
        //2.  获取参数
        Long appId = aiGenerateQuestionRequest.getAppId();
        int questionNumber = aiGenerateQuestionRequest.getQuestionNumber();
        int optionNumber = aiGenerateQuestionRequest.getOptionNumber();
        //3.  获取应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        //4. 封装 Prompt
        String userMessage = getGenerateQuestionUserMessage(app, questionNumber, optionNumber);
        //5. AI 生成
        String result = aiManager.doSyncRequest(GENERATE_QUESTION_SYSTEM_MESSAGE, userMessage, null);
        //6.  截取需要的 JSON 信息
        int start = result.indexOf("[");
        int end = result.lastIndexOf("]");
        String json = result.substring(start, end + 1);
        List<QuestionContentDTO> questionContentDTOList = JSONUtil.toList(json, QuestionContentDTO.class);
        //7. 返回结果
        return ResultUtils.success(questionContentDTOList);
    }

    /**
     * AI生成题目 SSE版
     * @param aiGenerateQuestionRequest
     * @param request
     * @return
     */
    @GetMapping("/ai_generate/sse")
    public SseEmitter aiGenerateQuestionSSE(AiGenerateQuestionRequest aiGenerateQuestionRequest, HttpServletRequest request) {
        //1. 参数校验
        ThrowUtils.throwIf(aiGenerateQuestionRequest == null, ErrorCode.PARAMS_ERROR);
        //2.  获取参数
        Long appId = aiGenerateQuestionRequest.getAppId();
        int questionNumber = aiGenerateQuestionRequest.getQuestionNumber();
        int optionNumber = aiGenerateQuestionRequest.getOptionNumber();
        //3.  获取应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        //4. 封装 Prompt
        String userMessage = getGenerateQuestionUserMessage(app, questionNumber, optionNumber);
        //5.  建立 SSE 连接对象，0 表示永不超时
        SseEmitter sseEmitter = new SseEmitter(0L);
        //6. AI 生成，SSE 流式返回
        Flowable<ModelData> modelDataFlowable = aiManager.doStreamRequest(GENERATE_QUESTION_SYSTEM_MESSAGE, userMessage, null);
        //7. 左括号计数器，除了默认值外，当回归为 0 时，表示左括号等于右括号，可以截取
        AtomicInteger counter = new AtomicInteger(0);
        //8. 拼接完整题目
        StringBuilder stringBuilder = new StringBuilder();

        //9. 获取登录用户
        User loginUser = userService.getLoginUser(request);
        //10. 默认全局线程池 --  改为vip隔离线程池
        Scheduler scheduler = Schedulers.io();
        if ("vip".equals(loginUser.getUserRole())) {
            scheduler = vipScheduler;
        }
        modelDataFlowable
                .observeOn(scheduler)
                .map(modelData -> modelData.getChoices().get(0).getDelta().getContent())
                .map(message -> message.replaceAll("\\s", ""))
                .filter(StrUtil::isNotBlank)
                .flatMap(message -> {
                    List<Character> characterList = new ArrayList<>();
                    for (char c : message.toCharArray()) {
                        characterList.add(c);
                    }
                    return Flowable.fromIterable(characterList);
                })
                .doOnNext(c -> {
                    // 如果是 '{'，计数器 + 1
                    if (c == '{') {
                        counter.addAndGet(1);
                    }
                    if (counter.get() > 0) {
                        stringBuilder.append(c);
                    }
                    if (c == '}') {
                        counter.addAndGet(-1);
                        if (counter.get() == 0) {
                            // 可以拼接题目，并且通过 SSE 返回给前端
                            sseEmitter.send(JSONUtil.toJsonStr(stringBuilder.toString()));
                            // 重置，准备拼接下一道题
                            stringBuilder.setLength(0);
                        }
                    }
                })
                .doOnError((e) -> log.error("sse error", e))
                .doOnComplete(sseEmitter::complete)
                .subscribe();
        return sseEmitter;
    }

    /**
     * AI 生成题目 隔离线程池版
     * @param aiGenerateQuestionRequest
     * @param isVip
     * @return
     */
    // 仅测试隔离线程池使用
    @Deprecated
    @GetMapping("/ai_generate/sse/test")
    public SseEmitter aiGenerateQuestionSSETest(AiGenerateQuestionRequest aiGenerateQuestionRequest,
                                                boolean isVip) {
        //1. 参数校验
        ThrowUtils.throwIf(aiGenerateQuestionRequest == null, ErrorCode.PARAMS_ERROR);
        //2. 获取参数
        Long appId = aiGenerateQuestionRequest.getAppId();
        int questionNumber = aiGenerateQuestionRequest.getQuestionNumber();
        int optionNumber = aiGenerateQuestionRequest.getOptionNumber();
        //3.  获取应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        //4.  封装 Prompt
        String userMessage = getGenerateQuestionUserMessage(app, questionNumber, optionNumber);
        //5.  建立 SSE 连接对象，0 表示永不超时
        SseEmitter sseEmitter = new SseEmitter(0L);
        //6.  AI 生成，SSE 流式返回
        Flowable<ModelData> modelDataFlowable = aiManager.doStreamRequest(GENERATE_QUESTION_SYSTEM_MESSAGE, userMessage, null);
        //7. 左括号计数器，除了默认值外，当回归为 0 时，表示左括号等于右括号，可以截取。利用原子类，异步。
        AtomicInteger counter = new AtomicInteger(0);
        //8. 拼接完整题目
        StringBuilder stringBuilder = new StringBuilder();
        //9. 默认全局线程池  --  改为使用隔离的线程池
        Scheduler scheduler = Schedulers.single();
        if (isVip) {
            scheduler = vipScheduler;
        }
        //10. sse操作过程
        modelDataFlowable
                .observeOn(scheduler)//改为使用隔离的线程池
                .map(modelData -> modelData.getChoices().get(0).getDelta().getContent())
                .map(message -> message.replaceAll("\\s", ""))
                .filter(StrUtil::isNotBlank)
                .flatMap(message -> {
                    List<Character> characterList = new ArrayList<>();
                    for (char c : message.toCharArray()) {
                        characterList.add(c);
                    }
                    return Flowable.fromIterable(characterList);
                })
                .doOnNext(c -> {
                    // 如果是 '{'，计数器 + 1
                    if (c == '{') {
                        counter.addAndGet(1);
                    }
                    // 如果计数器大于 0，{}中间部分，则将字符添加到 StringBuilder 中
                    if (counter.get() > 0) {
                        stringBuilder.append(c);
                    }
                    // 如果是 '}'，计数器 - 1，开始sse传输。
                    if (c == '}') {
                        counter.addAndGet(-1);
                        if (counter.get() == 0) {
                            // ①输出当前线程的名称
                            System.out.println(Thread.currentThread().getName());
                            // ②模拟普通用户阻塞
                            if (!isVip) {
                                Thread.sleep(10000L);
                            }
                            // ③可以拼接题目，并且通过 SSE 返回给前端
                            sseEmitter.send(JSONUtil.toJsonStr(stringBuilder.toString()));
                            // ④重置，准备拼接下一道题
                            stringBuilder.setLength(0);
                        }
                    }
                })
                .doOnError((e) -> log.error("sse error", e))
                .doOnComplete(sseEmitter::complete)
                .subscribe();

        // 11. 返回结果 SseEmitter 对象
        return sseEmitter;
    }

    // endregion




}

















