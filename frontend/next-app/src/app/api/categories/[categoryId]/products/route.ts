import type { NextRequest } from "next/server";

import { proxyBackendRequest } from "@/lib/auth/proxy";

type Params = {
  params: Promise<{
    categoryId: string;
  }>;
};

export async function GET(request: NextRequest, context: Params) {
  const { categoryId } = await context.params;

  return proxyBackendRequest({
    method: "GET",
    pathname: `/api/categories/${categoryId}/products`,
    search: request.nextUrl.search
  });
}
