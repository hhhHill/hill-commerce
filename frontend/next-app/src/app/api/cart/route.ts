import { proxyBackendRequest } from "@/lib/auth/proxy";

export async function GET(request: Request) {
  return proxyBackendRequest({
    method: "GET",
    pathname: "/api/cart",
    cookieHeader: request.headers.get("cookie")
  });
}

export async function POST(request: Request) {
  const body = await request.json();

  return proxyBackendRequest({
    method: "POST",
    pathname: "/api/cart",
    cookieHeader: request.headers.get("cookie"),
    body
  });
}
