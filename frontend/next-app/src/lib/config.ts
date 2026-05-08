const DEFAULT_BACKEND_BASE_URL = "http://localhost:8080";

export function getBackendBaseUrl(): string {
  return process.env.HILL_BACKEND_URL ?? process.env.NEXT_PUBLIC_HILL_BACKEND_URL ?? DEFAULT_BACKEND_BASE_URL;
}
