import { proxyBackendRequest } from "@/lib/auth/proxy";

export async function POST(request: Request) {
  return proxyBackendRequest({
    method: "POST",
    pathname: "/api/orders",
    cookieHeader: request.headers.get("cookie")
  });
}
