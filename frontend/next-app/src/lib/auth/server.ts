import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { getBackendBaseUrl } from "@/lib/config";
import type { SessionUser, SessionUserRole } from "@/lib/auth/types";

export async function getSessionUser(): Promise<SessionUser | null> {
  const cookieHeader = await getCookieHeader();
  if (!cookieHeader) {
    return null;
  }

  const response = await fetch(`${getBackendBaseUrl()}/api/auth/me`, {
    method: "GET",
    headers: {
      cookie: cookieHeader
    },
    cache: "no-store",
    redirect: "manual"
  });

  if (response.status === 401) {
    return null;
  }

  if (!response.ok) {
    throw new Error(`Failed to load current user: ${response.status}`);
  }

  return (await response.json()) as SessionUser;
}

export async function requireUser(nextPath: string): Promise<SessionUser> {
  const user = await getSessionUser();
  if (!user) {
    redirect(`/login?next=${encodeURIComponent(nextPath)}`);
  }
  return user;
}

export async function requireRole(roles: SessionUserRole[], nextPath: string): Promise<SessionUser> {
  const user = await requireUser(nextPath);
  const hasRole = user.roles.some((role) => roles.includes(role));

  if (!hasRole) {
    redirect("/forbidden");
  }

  return user;
}

async function getCookieHeader(): Promise<string> {
  const cookieStore = await cookies();

  return cookieStore
    .getAll()
    .map(({ name, value }) => `${name}=${value}`)
    .join("; ");
}
