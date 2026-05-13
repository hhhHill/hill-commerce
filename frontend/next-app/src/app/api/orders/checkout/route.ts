import { proxyBackendRequest } from "@/lib/auth/proxy";

export async function GET(request: Request) {
  return proxyBackendRequest({
    method: "GET",
    pathname: "/api/orders/checkout",
    cookieHeader: request.headers.get("cookie")
  });
}
