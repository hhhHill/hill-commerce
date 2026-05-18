import { cookies } from "next/headers";
import type { NextRequest } from "next/server";

import { proxyBackendRequest } from "@/lib/auth/proxy";

type Params = {
  params: Promise<{
    path: string[];
  }>;
};

export async function GET(request: NextRequest, context: Params) {
  return proxyAdminRequest(request, context, "GET");
}

export async function POST(request: NextRequest, context: Params) {
  return proxyAdminRequest(request, context, "POST");
}

export async function PUT(request: NextRequest, context: Params) {
  return proxyAdminRequest(request, context, "PUT");
}

export async function DELETE(request: NextRequest, context: Params) {
  return proxyAdminRequest(request, context, "DELETE");
}

async function proxyAdminRequest(request: NextRequest, context: Params, method: "GET" | "POST" | "PUT" | "DELETE") {
  const { path } = await context.params;
  const cookieStore = await cookies();
  const cookieHeader = cookieStore
    .getAll()
    .map(({ name, value }) => `${name}=${value}`)
    .join("; ");
  const body = method === "GET" || method === "DELETE" ? undefined : await safeBody(request);

  return proxyBackendRequest({
    method,
    pathname: `/api/admin/${path.join("/")}`,
    search: request.nextUrl.search,
    cookieHeader,
    body
  });
}

async function safeBody(request: NextRequest): Promise<unknown> {
  const contentLength = request.headers.get("content-length");
  if (!contentLength || contentLength === "0") {
    return undefined;
  }
  const contentType = request.headers.get("content-type") ?? "";
  if (contentType.includes("multipart/form-data")) {
    return await request.formData();
  }
  try {
    return await request.json();
  } catch {
    return undefined;
  }
}
