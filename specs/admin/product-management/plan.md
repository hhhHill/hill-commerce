# Implementation Plan: admin-product-management

**Feature**: `admin-product-management`  
**Status**: active  

## Summary

基于当前已存在的后台分类页、商品列表页和商品编辑页骨架，重写后台商品管理规范，使其同时覆盖：

- 分类与商品规则
- 页面级操作边界
- 字段语义
- 后台可用性要求
- 静态问号帮助提示

目标不是单纯补一个 UI 小功能，而是让 `admin-product-management` 变成后续实现与验收的可靠依据。

## Current Repository Reality

- 后端已存在 `modules/product` 的 entity、mapper、service、controller 骨架
- 前端已存在 `/admin/categories`
- 前端已存在 `/admin/products`
- 前端已存在 `/admin/products/new`
- 前端已存在 `/admin/products/[id]`
- `frontend/next-app/src/features/admin/catalog` 已存在 `category-manager`、`product-list`、`product-editor` 等基础 UI 组件

当前主要问题不是“没有页面”，而是：

- 规范对页面职责定义过粗
- 字段业务语义没有被清楚写入 canonical spec
- Tooltip 规则不存在
- 验收点更偏抽象业务，而不是后台操作链路

## Technical Boundaries

- 商品管理采用聚合保存模型
- 分类和商品接口位于 `/api/admin/*`
- 图片仍保持 URL 方案
- SKU 自动补码规则在后端统一处理
- 分类状态采用固定枚举 `ENABLED` / `DISABLED`
- 商品状态采用固定枚举 `DRAFT` / `ON_SHELF` / `OFF_SHELF`
- SKU 状态采用独立枚举 `ENABLED` / `DISABLED`
- 商品状态与 SKU 状态不得共用同一校验逻辑
- 后台页面默认只允许 `SALES` 与 `ADMIN`
- 前端 tooltip 采用统一静态文案，不做状态联动
- tooltip 仅做字段解释，不承担校验逻辑
- 分类一旦关联过商品，只允许停用，不允许删除
- 商品编辑页中的 SKU 区域必须使用统一表头和统一列模板，保证字段名称、问号提示和输入框对齐

## Main Delivery Areas

### 1. Category Management Rules

- 重写分类页的字段定义、操作边界、删除/停用规则和反馈规则

### 2. Product List Rules

- 重写商品列表页的筛选规则、展示字段含义、状态操作和删除行为
- 固化关键词筛选的匹配语义、字段关系和触发方式

### 3. Product Editor Rules

- 重写商品新建/编辑页的分区结构
- 固化基础信息、图片与描述、展示属性、销售属性和 SKU 的语义与限制

### 4. Tooltip Pattern

- 统一后台关键字段的“标签右侧问号提示”模式
- 为分类页、筛选区和商品编辑页定义静态帮助文案规范

### 5. SKU Status And Layout Consistency

- 收紧 SKU 状态语义，避免把商品状态校验错误复用到 SKU
- 收紧 SKU 列表布局，保证表头、提示和输入控件结构一致

### 6. Status Enum Clarity

- 把分类状态、商品状态、SKU 状态的字面量枚举写入 canonical spec
- 避免实现、测试和验收分别使用不同口径解释“启用/停用”与“上架/下架”

### 7. Verification Path

- 定义页面级人工验收路径
- 定义字段理解性和规则一致性的验证要求

## Risks

- SKU 组合生成与已有数据同步复杂度
- 商品状态与逻辑删除边界混淆
- 前端编辑器分区与一次提交模型不一致
- 字段术语不一致，导致用户无法区分 `SPU`、销售属性、展示属性、`SKU`
- tooltip 文案与后端真实规则不一致，反而误导用户
- SKU 状态与商品状态混用，会直接导致新建或更新商品失败
- SKU 表头和输入网格不一致，会让字段理解成本明显上升并增加录入错误
- spec 未显式定义状态枚举，会导致验收口径和实现口径漂移
- 关键词搜索若未定义匹配方式和字段关系，会导致前后端实现与验收口径分叉
- 分类删除/停用边界不清，导致实现和验收口径分叉
- 商品列表页承担过多编辑职责，削弱页面边界

## Verification Notes

- 验证不能只停留在接口通过或保存成功
- 需要人工确认分类页、商品列表页和商品编辑页的字段含义是否可理解
- 需要人工确认问号图标是否出现在所有关键字段旁
- 需要人工确认 tooltip 文案是否符合“业务含义 + 规则限制”的两句式
- 需要人工确认 tooltip 文案与真实系统行为一致
- 需要人工确认分类关联商品后只能停用、不能删除
- 需要人工确认分类状态、商品状态、SKU 状态的字面量枚举与接口行为一致
- 需要人工确认关键词筛选按名称模糊匹配、`SPU` 精确匹配、`OR` 关系执行
- 需要人工确认 SKU 状态值和商品状态值没有混用
- 需要人工确认 SKU 列表在桌面布局下表头、问号提示和输入框保持列对齐
