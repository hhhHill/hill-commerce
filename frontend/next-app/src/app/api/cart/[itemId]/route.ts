import { proxyBackendRequest } from "@/lib/auth/proxy";

type RouteContext = {
  params: Promise<{
    itemId: string;
  }>;
};

export async function PUT(request: Request, { params }: RouteContext) {
  const { itemId } = await params;
  const body = await request.json();

  return proxyBackendRequest({
    method: "PUT",
    pathname: `/api/cart/${itemId}`,
    cookieHeader: request.headers.get("cookie"),
    body
  });
}

export async function DELETE(request: Request, { params }: RouteContext) {
  const { itemId } = await params;

  return proxyBackendRequest({
    method: "DELETE",
    pathname: `/api/cart/${itemId}`,
    cookieHeader: request.headers.get("cookie")
  });
}
