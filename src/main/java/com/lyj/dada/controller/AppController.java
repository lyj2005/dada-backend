package com.lyj.dada.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.lyj.dada.annotation.AuthCheck;
import com.lyj.dada.common.*;
import com.lyj.dada.constant.UserConstant;
import com.lyj.dada.exception.BusinessException;
import com.lyj.dada.exception.ErrorCode;
import com.lyj.dada.exception.ThrowUtils;
import com.lyj.dada.model.dto.app.AppAddRequest;
import com.lyj.dada.model.dto.app.AppEditRequest;
import com.lyj.dada.model.dto.app.AppQueryRequest;
import com.lyj.dada.model.dto.app.AppUpdateRequest;
import com.lyj.dada.model.entity.App;
import com.lyj.dada.model.entity.User;
import com.lyj.dada.model.enums.ReviewStatusEnum;
import com.lyj.dada.model.vo.AppVO;
import com.lyj.dada.service.AppService;
import com.lyj.dada.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 应用接口
 */
@RestController
@RequestMapping("/app")
@Slf4j
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;



    // region 增删改查代码

    /**
     * 创建应用
     *
     * @param appAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        //1. 校验
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        //2.  在此处将实体类和 DTO 进行转换
        App app = new App();
        BeanUtils.copyProperties(appAddRequest, app);
        //3.  数据校验
        appService.validApp(app, true);
        //4.  填充app信息
        User loginUser = userService.getLoginUser(request);
        app.setUserId(loginUser.getId());
        app.setReviewStatus(ReviewStatusEnum.REVIEWING.getValue());
        //5.  写入数据库
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        //6.  返回新写入的数据 id
        long newAppId = app.getId();
        return ResultUtils.success(newAppId);
    }

    /**
     * 删除应用
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        //1. 校验
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2. 获取当前登录用户
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        //3.  判断应用是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        //4、  仅本人或管理员可删除
        if (!oldApp.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //5.  删除数据库
        boolean result = appService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        //6. 返回结果
        return ResultUtils.success(true);
    }

    /**
     * 更新应用（仅管理员可用）
     *
     * @param appUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest) {
        //1. 校验
        if (appUpdateRequest == null || appUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2. 在此处将实体类和 DTO 进行转换
        App app = new App();
        BeanUtils.copyProperties(appUpdateRequest, app);
        //3.  数据校验
        appService.validApp(app, false);
        //4.  判断应用是否存在
        long id = appUpdateRequest.getId();
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        //5.  更新数据库
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        //6. 返回结果
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取应用（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(long id, HttpServletRequest request) {
        //1. 校验
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        //2.  查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        //3.  返回封装类
        return ResultUtils.success(appService.getAppVO(app, request));
    }

    /**
     * 分页获取应用列表（仅管理员可用）
     *
     * @param appQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<App>> listAppByPage(@RequestBody AppQueryRequest appQueryRequest) {
        //1. 获取分页信息
        long current = appQueryRequest.getCurrent();
        long size = appQueryRequest.getPageSize();
        //2.  查询数据库
        Page<App> appPage = appService.page(new Page<>(current, size),
                appService.getQueryWrapper(appQueryRequest));
        //3. 返回结果
        return ResultUtils.success(appPage);
    }

    /**
     * 分页获取应用列表（封装类）
     *
     * @param appQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<AppVO>> listAppVOByPage(@RequestBody AppQueryRequest appQueryRequest,
                                                     HttpServletRequest request) {
        //1. 获取分页信息
        long current = appQueryRequest.getCurrent();
        long size = appQueryRequest.getPageSize();
        //2.  限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        //3. 只能看到已过审的应用
        appQueryRequest.setReviewStatus(ReviewStatusEnum.PASS.getValue());
        //4.  查询数据库
        Page<App> appPage = appService.page(new Page<>(current, size),
                appService.getQueryWrapper(appQueryRequest));
        //5. 返回封装类
        return ResultUtils.success(appService.getAppVOPage(appPage, request));
    }

    /**
     * 分页获取当前登录用户创建的应用列表
     *
     * @param appQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest,
                                                       HttpServletRequest request) {
        //1. 校验
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        //2.  补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        appQueryRequest.setUserId(loginUser.getId());
        long current = appQueryRequest.getCurrent();
        long size = appQueryRequest.getPageSize();
        //3.  限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        //4.  查询数据库
        Page<App> appPage = appService.page(new Page<>(current, size),
                appService.getQueryWrapper(appQueryRequest));
        //5.  返回封装类
        return ResultUtils.success(appService.getAppVOPage(appPage, request));
    }

    /**
     * 编辑应用（给用户使用）
     *
     * @param appEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editApp(@RequestBody AppEditRequest appEditRequest, HttpServletRequest request) {
        //1. 校验
        if (appEditRequest == null || appEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2.  在此处将实体类和 DTO 进行转换
        App app = new App();
        BeanUtils.copyProperties(appEditRequest, app);
        //3.  数据校验
        appService.validApp(app, false);
        User loginUser = userService.getLoginUser(request);
        //4.  判断应用是否存在
        long id = appEditRequest.getId();
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        //5.  仅本人或管理员可编辑
        if (!oldApp.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //6.  重置审核状态
        app.setReviewStatus(ReviewStatusEnum.REVIEWING.getValue());
        //7.  更新数据库
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        //8. 返回结果
        return ResultUtils.success(true);
    }

    // endregion




    /**
     * 应用审核
     *
     * @param reviewRequest
     * @param request
     * @return
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doAppReview(@RequestBody ReviewRequest reviewRequest, HttpServletRequest request) {
        //1. 校验
        ThrowUtils.throwIf(reviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = reviewRequest.getId();
        Integer reviewStatus = reviewRequest.getReviewStatus();
        //2. 校验
        ReviewStatusEnum reviewStatusEnum = ReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //3. 判断应用是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        //4. 检查 已是该状态
        if (oldApp.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        //5.  更新审核状态
        User loginUser = userService.getLoginUser(request);
        App app = new App();
        app.setId(id);
        app.setReviewStatus(reviewStatus);
        app.setReviewMessage(reviewRequest.getReviewMessage());
        app.setReviewerId(loginUser.getId());
        app.setReviewTime(new Date());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        //6. 返回结果
        return ResultUtils.success(true);
    }
}
