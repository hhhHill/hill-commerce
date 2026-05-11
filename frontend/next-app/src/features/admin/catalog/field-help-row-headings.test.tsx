import test from "node:test";
import assert from "node:assert/strict";
import { renderToStaticMarkup } from "react-dom/server";

import { FieldHelpRowHeadings } from "./field-help-row-headings";

test("renders visible field labels and help triggers for repeated editor fields", () => {
  const markup = renderToStaticMarkup(
    <FieldHelpRowHeadings
      items={[
        { field: "detailImageUrl", label: "详情图 URL" },
        { field: "detailImageSortOrder", label: "详情图排序" }
      ]}
      page="productEditor"
    />
  );

  assert.match(markup, /详情图 URL/);
  assert.match(markup, /详情图排序/);
  assert.ok(markup.includes("?"));
});
