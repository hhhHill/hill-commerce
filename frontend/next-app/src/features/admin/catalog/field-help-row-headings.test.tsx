// @vitest-environment jsdom

import { describe, it, expect } from "vitest";
import { renderToStaticMarkup } from "react-dom/server";

import { FieldHelpRowHeadings } from "./field-help-row-headings";

describe("FieldHelpRowHeadings", () => {
  it("renders visible field labels and help triggers for repeated editor fields", () => {
    const markup = renderToStaticMarkup(
      <FieldHelpRowHeadings
        items={[
          { field: "detailImageUrl", label: "详情图 URL" },
          { field: "detailImageSortOrder", label: "详情图排序" }
        ]}
        page="productEditor"
      />
    );

    expect(markup).toMatch(/详情图 URL/);
    expect(markup).toMatch(/详情图排序/);
    expect(markup).toContain("?");
  });
});
