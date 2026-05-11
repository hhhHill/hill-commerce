import test from "node:test";
import assert from "node:assert/strict";

import { getFieldHelp } from "./field-help";

test("provides static help copy for category form fields", () => {
  const nameHelp = getFieldHelp("category", "name");
  const statusHelp = getFieldHelp("category", "status");

  assert.equal(nameHelp?.title, "分类名称");
  assert.match(nameHelp?.description ?? "", /前台展示/);
  assert.equal(statusHelp?.title, "状态");
  assert.match(statusHelp?.description ?? "", /停用/);
});

test("provides static help copy for product list filters", () => {
  const keywordHelp = getFieldHelp("productList", "keyword");
  const categoryHelp = getFieldHelp("productList", "categoryId");

  assert.equal(keywordHelp?.title, "关键词");
  assert.match(keywordHelp?.description ?? "", /商品名称/);
  assert.equal(categoryHelp?.title, "分类");
  assert.match(categoryHelp?.description ?? "", /一级分类/);
});

test("provides static help copy for product editor fields", () => {
  const spuCodeHelp = getFieldHelp("productEditor", "spuCode");
  const salesAttributeValuesHelp = getFieldHelp("productEditor", "salesAttributeValues");

  assert.equal(spuCodeHelp?.title, "SPU 编码");
  assert.match(spuCodeHelp?.description ?? "", /留空/);
  assert.equal(salesAttributeValuesHelp?.title, "销售属性值");
  assert.match(salesAttributeValuesHelp?.description ?? "", /SKU/);
});
