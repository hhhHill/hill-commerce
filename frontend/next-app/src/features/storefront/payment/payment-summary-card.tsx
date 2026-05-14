import type { PaymentOrder } from "@/lib/payment/types";

type PaymentSummaryCardProps = {
  payment: PaymentOrder;
};

export function PaymentSummaryCard({ payment }: PaymentSummaryCardProps) {
  return (
    <article className="rounded-[30px] border border-black/10 bg-white/90 p-6 shadow-[0_18px_50px_rgba(74,42,18,0.08)]">
      <span className="rounded-full bg-[var(--surface)] px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-black/50">
        Payment
      </span>
      <h1 className="mt-3 text-4xl font-semibold tracking-tight">{payment.orderNo}</h1>
      <p className="mt-2 text-sm leading-7 text-black/65">支付阶段只承接已经创建完成的订单。你可以在这里查看支付状态、截止时间和当前支付尝试。</p>

      <dl className="mt-6 grid gap-4 rounded-[24px] bg-[var(--surface)] p-4">
        <Metric label="订单状态" value={renderStatus(payment.orderStatus)} />
        <Metric label="应付金额" value={formatPrice(payment.payableAmount)} />
        <Metric label="支付截止" value={formatDateTime(payment.paymentDeadlineAt)} />
        <Metric label="当前尝试" value={payment.currentAttempt ? renderAttemptStatus(payment.currentAttempt.paymentStatus) : "尚未创建"} />
      </dl>
    </article>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-4 border-b border-black/6 pb-3 last:border-b-0 last:pb-0">
      <dt className="text-sm text-black/50">{label}</dt>
      <dd className="text-right text-lg font-semibold">{value}</dd>
    </div>
  );
}

function formatPrice(value: number) {
  return `¥${value.toFixed(2)}`;
}

function formatDateTime(value: string | null) {
  if (!value) {
    return "未设置";
  }

  return new Date(value).toLocaleString("zh-CN", {
    hour12: false
  });
}

function renderStatus(status: string) {
  switch (status) {
    case "PENDING_PAYMENT":
      return "待支付";
    case "PAID":
      return "已支付";
    case "CANCELLED":
      return "已取消";
    case "CLOSED":
      return "已关闭";
    default:
      return status;
  }
}

function renderAttemptStatus(status: string) {
  switch (status) {
    case "INITIATED":
      return "待处理";
    case "FAILED":
      return "已失败";
    case "SUCCESS":
      return "已成功";
    case "CLOSED":
      return "已关闭";
    default:
      return status;
  }
}
