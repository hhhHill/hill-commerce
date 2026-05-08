"use client";

import type { AuthErrorResponse, SessionUser } from "@/lib/auth/types";

type RegisterInput = {
  email: string;
  password: string;
  nickname: string;
};

type LoginInput = {
  email: string;
  password: string;
};

export async function registerWithEmail(input: RegisterInput): Promise<SessionUser> {
  return sendAuthRequest<SessionUser>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(input)
  });
}

export async function loginWithEmail(input: LoginInput): Promise<SessionUser> {
  return sendAuthRequest<SessionUser>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(input)
  });
}

export async function logoutCurrentUser(): Promise<void> {
  await sendAuthRequest("/api/auth/logout", {
    method: "POST"
  });
}

async function sendAuthRequest<T>(input: RequestInfo, init: RequestInit): Promise<T> {
  const response = await fetch(input, {
    ...init,
    credentials: "include",
    headers: {
      "content-type": "application/json",
      ...(init.headers ?? {})
    }
  });

  if (!response.ok) {
    const payload = (await safeJson(response)) as AuthErrorResponse | null;
    throw new Error(payload?.message ?? "请求失败，请稍后重试");
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

async function safeJson(response: Response): Promise<unknown | null> {
  try {
    return await response.json();
  } catch {
    return null;
  }
}
