import type { NextRequest } from "next/server";

import { proxyBackendRequest } from "@/lib/auth/proxy";

type Params = {
  params: Promise<{
    productId: string;
  }>;
};

export async function GET(request: NextRequest, context: Params) {
  const { productId } = await context.params;

  return proxyBackendRequest({
    method: "GET",
    pathname: `/api/products/${productId}`,
    search: request.nextUrl.search
  });
}
