export type SessionUserRole = "CUSTOMER" | "SALES" | "ADMIN";

export type SessionUser = {
  email: string;
  nickname: string;
  roles: SessionUserRole[];
};

export type AuthErrorResponse = {
  message?: string;
};
