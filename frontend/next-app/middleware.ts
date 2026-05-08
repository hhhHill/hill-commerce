import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const AUTH_PAGES = new Set(["/login", "/register"]);
const PROTECTED_PAGES = new Set(["/account", "/admin"]);

export function middleware(request: NextRequest) {
  const { pathname, search } = request.nextUrl;
  const hasSessionCookie = Boolean(request.cookies.get("JSESSIONID")?.value);

  if (PROTECTED_PAGES.has(pathname) && !hasSessionCookie) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("next", `${pathname}${search}`);
    return NextResponse.redirect(loginUrl);
  }

  if (AUTH_PAGES.has(pathname) && hasSessionCookie) {
    return NextResponse.redirect(new URL("/account", request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/login", "/register", "/account", "/admin"]
};
