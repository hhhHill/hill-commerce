export class PaymentRequestError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "PaymentRequestError";
    this.status = status;
  }
}
