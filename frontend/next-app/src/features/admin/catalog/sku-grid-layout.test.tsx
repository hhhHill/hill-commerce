import test from "node:test";
import assert from "node:assert/strict";
import { renderToStaticMarkup } from "react-dom/server";

import { SkuGridLayout } from "./sku-grid-layout";

test("renders aligned sku headers including the combination column", () => {
  const markup = renderToStaticMarkup(
    <SkuGridLayout
      header="header"
      rows={<div>row</div>}
    />
  );

  assert.match(markup, /header/);
  assert.match(markup, /row/);
  assert.match(markup, /xl:grid-cols-\[minmax\(180px,1\.2fr\)_minmax\(180px,1fr\)_minmax\(120px,0\.8fr\)_minmax\(120px,0\.8fr\)_minmax\(150px,0\.9fr\)_140px\]/);
});
