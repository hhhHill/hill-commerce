export class CartRequestError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "CartRequestError";
    this.status = status;
  }
}
