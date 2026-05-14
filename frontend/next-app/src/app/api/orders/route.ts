import { proxyBackendRequest } from "@/lib/auth/proxy";

export async function GET(request: Request) {
  const url = new URL(request.url);
  return proxyBackendRequest({
    method: "GET",
    pathname: "/api/orders",
    search: url.search,
    cookieHeader: request.headers.get("cookie")
  });
}

export async function POST(request: Request) {
  return proxyBackendRequest({
    method: "POST",
    pathname: "/api/orders",
    cookieHeader: request.headers.get("cookie")
  });
}
