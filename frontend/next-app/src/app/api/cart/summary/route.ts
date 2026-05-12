import { proxyBackendRequest } from "@/lib/auth/proxy";

export async function GET(request: Request) {
  return proxyBackendRequest({
    method: "GET",
    pathname: "/api/cart/summary",
    cookieHeader: request.headers.get("cookie")
  });
}
