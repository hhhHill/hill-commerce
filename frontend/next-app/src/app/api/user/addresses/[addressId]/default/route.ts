import { proxyBackendRequest } from "@/lib/auth/proxy";

type RouteContext = {
  params: Promise<{
    addressId: string;
  }>;
};

export async function PUT(request: Request, { params }: RouteContext) {
  const { addressId } = await params;

  return proxyBackendRequest({
    method: "PUT",
    pathname: `/api/user/addresses/${addressId}/default`,
    cookieHeader: request.headers.get("cookie")
  });
}
