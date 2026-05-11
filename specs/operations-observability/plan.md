# Implementation Plan: operations-observability

**Feature**: `operations-observability`  
**Status**: active  
**Parent Baseline**: `specs/hill-commerce-mvp/plan.md`

## Summary

把日志、通知、统计与联调上线准备收敛为一个偏运维主题，适合作为 MVP 尾部阶段统一推进。

## Technical Boundaries

- 登录、操作、浏览日志依赖业务事件落点
- 邮件能力依赖通知模块和本地邮件调试设施
- Dashboard 以聚合视图为主
- 联调与异常页不引入新业务规则

## Risks

- 日志埋点遗漏
- 邮件链路与业务事件脱节
- 联调阶段混入新需求
