export type SessionUserRole = "CUSTOMER" | "MERCHANT" | "ADMIN";

export type SessionUser = {
  email: string;
  nickname: string;
  roles: SessionUserRole[];
};

export type AuthErrorResponse = {
  message?: string;
};
