export class OrderRequestError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "OrderRequestError";
    this.status = status;
  }
}
