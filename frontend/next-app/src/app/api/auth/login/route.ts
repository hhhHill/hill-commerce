import { proxyAuthRequest } from "@/lib/auth/proxy";

export async function POST(request: Request) {
  const body = await request.json();

  return proxyAuthRequest({
    method: "POST",
    pathname: "/api/auth/login",
    body
  });
}
