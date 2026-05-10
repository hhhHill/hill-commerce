const STATUS_CODES_WITHOUT_BODY = new Set([101, 103, 204, 205, 304]);

export function getProxyResponseBody(status: number, bodyText: string): string | null {
  if (STATUS_CODES_WITHOUT_BODY.has(status)) {
    return null;
  }

  return bodyText;
}
