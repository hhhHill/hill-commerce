import { proxyBackendRequest } from "@/lib/auth/proxy";

type RouteContext = {
  params: Promise<{
    paymentId: string;
  }>;
};

export async function POST(request: Request, context: RouteContext) {
  const { paymentId } = await context.params;

  return proxyBackendRequest({
    method: "POST",
    pathname: `/api/payments/${paymentId}/succeed`,
    cookieHeader: request.headers.get("cookie")
  });
}
