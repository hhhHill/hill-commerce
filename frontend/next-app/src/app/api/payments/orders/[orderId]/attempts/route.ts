import { proxyBackendRequest } from "@/lib/auth/proxy";

type RouteContext = {
  params: Promise<{
    orderId: string;
  }>;
};

export async function POST(request: Request, context: RouteContext) {
  const { orderId } = await context.params;

  return proxyBackendRequest({
    method: "POST",
    pathname: `/api/payments/orders/${orderId}/attempts`,
    cookieHeader: request.headers.get("cookie")
  });
}
