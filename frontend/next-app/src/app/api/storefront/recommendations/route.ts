import type { NextRequest } from "next/server";

import { proxyBackendRequest } from "@/lib/auth/proxy";

export async function GET(request: NextRequest) {
  return proxyBackendRequest({
    method: "GET",
    pathname: "/api/storefront/recommendations",
    search: request.nextUrl.search,
    cookieHeader: request.headers.get("cookie")
  });
}
