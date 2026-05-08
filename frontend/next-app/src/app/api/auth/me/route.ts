import { proxyAuthRequest } from "@/lib/auth/proxy";

export async function GET(request: Request) {
  return proxyAuthRequest({
    method: "GET",
    pathname: "/api/auth/me",
    cookieHeader: request.headers.get("cookie")
  });
}
