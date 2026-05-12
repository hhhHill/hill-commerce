import { proxyBackendRequest } from "@/lib/auth/proxy";

export async function GET(request: Request) {
  return proxyBackendRequest({
    method: "GET",
    pathname: "/api/products",
    search: new URL(request.url).search
  });
}
