import type { NextRequest } from "next/server";

import { proxyBackendRequest } from "@/lib/auth/proxy";

export async function POST(request: NextRequest) {
  const body = await request.json();

  return proxyBackendRequest({
    method: "POST",
    pathname: "/api/storefront/view-log",
    body
  });
}
