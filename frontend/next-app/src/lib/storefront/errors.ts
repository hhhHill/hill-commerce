export class StorefrontRequestError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "StorefrontRequestError";
    this.status = status;
  }
}
