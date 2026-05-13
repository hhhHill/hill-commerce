import { proxyBackendRequest } from "@/lib/auth/proxy";

type RouteContext = {
  params: Promise<{
    orderId: string;
  }>;
};

export async function GET(request: Request, context: RouteContext) {
  const { orderId } = await context.params;

  return proxyBackendRequest({
    method: "GET",
    pathname: `/api/orders/${orderId}`,
    cookieHeader: request.headers.get("cookie")
  });
}
