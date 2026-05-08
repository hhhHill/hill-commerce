import { NextResponse } from "next/server";

import { getBackendBaseUrl } from "@/lib/config";

type ProxyOptions = {
  method: "GET" | "POST";
  pathname: string;
  cookieHeader?: string | null;
  body?: unknown;
  clearSessionCookie?: boolean;
};

export async function proxyAuthRequest(options: ProxyOptions): Promise<NextResponse> {
  const backendResponse = await fetch(`${getBackendBaseUrl()}${options.pathname}`, {
    method: options.method,
    headers: buildHeaders(options.cookieHeader, options.body),
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    cache: "no-store",
    redirect: "manual"
  });

  const bodyText = await backendResponse.text();
  const response = new NextResponse(bodyText, {
    status: backendResponse.status,
    headers: {
      "content-type": backendResponse.headers.get("content-type") ?? "application/json"
    }
  });

  const setCookieHeader = backendResponse.headers.get("set-cookie");
  if (setCookieHeader) {
    response.headers.set("set-cookie", setCookieHeader);
  }

  if (options.clearSessionCookie) {
    response.cookies.set("JSESSIONID", "", {
      expires: new Date(0),
      httpOnly: true,
      path: "/",
      sameSite: "lax"
    });
  }

  return response;
}

function buildHeaders(cookieHeader?: string | null, body?: unknown): HeadersInit {
  const headers: Record<string, string> = {};

  if (cookieHeader) {
    headers.cookie = cookieHeader;
  }

  if (body !== undefined) {
    headers["content-type"] = "application/json";
  }

  return headers;
}
