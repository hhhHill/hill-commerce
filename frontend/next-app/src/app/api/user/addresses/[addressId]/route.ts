import { proxyBackendRequest } from "@/lib/auth/proxy";

type RouteContext = {
  params: Promise<{
    addressId: string;
  }>;
};

export async function PUT(request: Request, { params }: RouteContext) {
  const { addressId } = await params;
  const body = await request.json();

  return proxyBackendRequest({
    method: "PUT",
    pathname: `/api/user/addresses/${addressId}`,
    cookieHeader: request.headers.get("cookie"),
    body
  });
}

export async function DELETE(request: Request, { params }: RouteContext) {
  const { addressId } = await params;

  return proxyBackendRequest({
    method: "DELETE",
    pathname: `/api/user/addresses/${addressId}`,
    cookieHeader: request.headers.get("cookie")
  });
}
