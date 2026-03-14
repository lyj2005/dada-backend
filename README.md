# dada - AI 答题应用平台

> 作者：[lyj](https://github.com/)



## 项目简介

### 项目介绍

基于 Vue 3 + Spring Boot + Redis + ChatGLM AI + RxJava + SSE 的 **AI 答题应用平台。**

用户可以基于 AI 快速制作并发布多种答题应用，支持检索和分享应用、在线答题并基于评分算法或 AI 得到回答总结；管理员可以审核应用、集中管理整站内容，并进行统计分析。



## 技术选型

### 后端

- Java Spring Boot 开发框架
- 存储层：MySQL 数据库 + Redis 缓存
- MyBatis-Plus 及 MyBatis X 自动生成
- Redisson 分布式锁
- Caffeine 本地缓存
- ⭐️ 基于 ChatGLM 大模型的通用 AI 能力
- ⭐️ RxJava 响应式框架 + 线程池隔离 
- ⭐️ SSE 服务端推送
- ⭐️ Shardingsphere 分库分表
- ⭐️ 幂等设计 + 分布式 ID 雪花算法
- ⭐️ 策略模式


### 开发工具

- 前端 IDE：VsCode
- 后端 IDE：JetBrains IDEA
- [CodeGeeX 智能编程助手](https://codegeex.cn/)

