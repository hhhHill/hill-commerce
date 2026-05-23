// @vitest-environment jsdom

import { describe, it, expect } from "vitest";
import { renderToStaticMarkup } from "react-dom/server";

import { SkuGridLayout } from "./sku-grid-layout";

describe("SkuGridLayout", () => {
  it("renders aligned sku headers including the combination column", () => {
    const markup = renderToStaticMarkup(
      <SkuGridLayout header="header" rows={<div>row</div>} />
    );

    expect(markup).toMatch(/header/);
    expect(markup).toMatch(/row/);
    expect(markup).toMatch(/xl:grid-cols-\[minmax\(180px,1\.2fr\)_minmax\(180px,1fr\)_minmax\(120px,0\.8fr\)_minmax\(120px,0\.8fr\)_minmax\(150px,0\.9fr\)_140px\]/);
  });
});
