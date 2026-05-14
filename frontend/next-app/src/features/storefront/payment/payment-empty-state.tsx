import Link from "next/link";

type PaymentEmptyStateProps = {
  orderId: number;
};

export function PaymentEmptyState({ orderId }: PaymentEmptyStateProps) {
  return (
    <article className="rounded-[30px] border border-dashed border-black/15 bg-white/80 p-6 text-center shadow-[0_18px_50px_rgba(74,42,18,0.06)]">
      <h2 className="text-2xl font-semibold tracking-tight">尚未创建支付尝试</h2>
      <p className="mt-3 text-sm leading-7 text-black/65">当前订单仍处于待支付状态，但系统还没有为它创建有效支付尝试。你可以在右侧直接创建一次新的模拟支付尝试。</p>
      <div className="mt-5 flex flex-wrap justify-center gap-3">
        <Link className="rounded-full border border-black/10 px-5 py-3 text-sm font-medium" href={`/orders/${orderId}`}>
          返回订单详情
        </Link>
        <Link className="rounded-full border border-black/10 px-5 py-3 text-sm font-medium" href="/cart">
          返回购物车
        </Link>
      </div>
    </article>
  );
}
