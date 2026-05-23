import { describe, it, expect } from "vitest";

import { getFieldHelp } from "./field-help";

describe("getFieldHelp", () => {
  it("provides static help copy for category form fields", () => {
    const nameHelp = getFieldHelp("category", "name");
    const statusHelp = getFieldHelp("category", "status");

    expect(nameHelp?.title).toBe("分类名称");
    expect(nameHelp?.description ?? "").toMatch(/前台展示/);
    expect(statusHelp?.title).toBe("状态");
    expect(statusHelp?.description ?? "").toMatch(/停用/);
  });

  it("provides static help copy for product list filters", () => {
    const keywordHelp = getFieldHelp("productList", "keyword");
    const categoryHelp = getFieldHelp("productList", "categoryId");

    expect(keywordHelp?.title).toBe("关键词");
    expect(keywordHelp?.description ?? "").toMatch(/商品名称/);
    expect(categoryHelp?.title).toBe("分类");
    expect(categoryHelp?.description ?? "").toMatch(/一级分类/);
  });

  it("provides static help copy for product editor fields", () => {
    const spuCodeHelp = getFieldHelp("productEditor", "spuCode");
    const salesAttributeValuesHelp = getFieldHelp("productEditor", "salesAttributeValues");

    expect(spuCodeHelp?.title).toBe("SPU 编码");
    expect(spuCodeHelp?.description ?? "").toMatch(/留空/);
    expect(salesAttributeValuesHelp?.title).toBe("销售属性值");
    expect(salesAttributeValuesHelp?.description ?? "").toMatch(/SKU/);
  });
});
