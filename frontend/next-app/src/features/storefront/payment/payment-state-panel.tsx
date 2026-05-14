import Link from "next/link";

type PaymentStatePanelProps = {
  orderId: number;
  orderStatus: string;
};

export function PaymentStatePanel({ orderId, orderStatus }: PaymentStatePanelProps) {
  return (
    <article className="rounded-[30px] border border-black/10 bg-white/90 p-6 shadow-[0_18px_50px_rgba(74,42,18,0.08)]">
      <h2 className="text-2xl font-semibold tracking-tight">当前支付状态</h2>
      <div className="mt-5 rounded-[24px] bg-[var(--surface)] p-5">
        <p className="text-lg font-semibold">{renderStateHeadline(orderStatus)}</p>
        <p className="mt-3 text-sm leading-7 text-black/65">{renderStateDescription(orderStatus)}</p>
      </div>

      <div className="mt-6 flex flex-col gap-3">
        <Link className="rounded-full bg-[var(--accent)] px-5 py-3 text-center text-sm font-semibold text-white" href={`/orders/${orderId}`}>
          查看订单详情
        </Link>
        <Link className="rounded-full border border-black/10 px-5 py-3 text-center text-sm font-medium" href={`/orders/${orderId}/result`}>
          返回订单结果页
        </Link>
      </div>
    </article>
  );
}

function renderStateHeadline(status: string) {
  switch (status) {
    case "PAID":
      return "订单已支付";
    case "CANCELLED":
      return "订单已取消";
    case "CLOSED":
      return "订单已关闭";
    default:
      return `当前状态：${status}`;
  }
}

function renderStateDescription(status: string) {
  switch (status) {
    case "PAID":
      return "支付已经完成，这笔订单不再需要新的支付尝试。";
    case "CANCELLED":
      return "这笔订单已被用户主动取消，支付入口已经失效。";
    case "CLOSED":
      return "这笔订单因超时未支付而被系统关闭，支付入口已经失效。";
    default:
      return "当前订单已经不处于可支付状态。";
  }
}
