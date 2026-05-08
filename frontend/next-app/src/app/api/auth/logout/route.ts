import { proxyAuthRequest } from "@/lib/auth/proxy";

export async function POST(request: Request) {
  return proxyAuthRequest({
    method: "POST",
    pathname: "/api/auth/logout",
    cookieHeader: request.headers.get("cookie"),
    clearSessionCookie: true
  });
}
