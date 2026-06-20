const DEFAULT_TIMEOUT_MS = 5000;

type FetchWithTimeoutInit = RequestInit & {
  timeoutMs?: number;
};

export async function fetchWithTimeout(input: RequestInfo | URL, init: FetchWithTimeoutInit = {}): Promise<Response> {
  const { timeoutMs = DEFAULT_TIMEOUT_MS, ...requestInit } = init;

  return fetch(input, {
    ...requestInit,
    signal: AbortSignal.timeout(timeoutMs)
  });
}

export function isTimeoutError(error: unknown): boolean {
  return error instanceof Error && (error.name === "TimeoutError" || error.name === "AbortError");
}
