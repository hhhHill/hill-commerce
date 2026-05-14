import type { PaymentAttempt } from "@/lib/payment/types";

type PaymentAttemptCardProps = {
  attempt: PaymentAttempt;
};

export function PaymentAttemptCard({ attempt }: PaymentAttemptCardProps) {
  return (
    <article className="rounded-[30px] border border-black/10 bg-white/90 p-6 shadow-[0_18px_50px_rgba(74,42,18,0.08)]">
      <h2 className="text-2xl font-semibold tracking-tight">当前支付尝试</h2>
      <dl className="mt-5 grid gap-4 rounded-[24px] bg-[var(--surface)] p-4">
        <Metric label="支付流水号" value={attempt.paymentNo} />
        <Metric label="支付方式" value={attempt.paymentMethod} />
        <Metric label="尝试状态" value={renderAttemptStatus(attempt.paymentStatus)} />
        <Metric label="发起时间" value={formatDateTime(attempt.requestedAt)} />
        <Metric label="支付时间" value={formatDateTime(attempt.paidAt)} />
        <Metric label="关闭时间" value={formatDateTime(attempt.closedAt)} />
      </dl>

      {attempt.failureReason ? (
        <div className="mt-5 rounded-[22px] border border-amber-200 bg-amber-50 px-4 py-4 text-sm text-amber-800">
          最近失败原因：{attempt.failureReason}
        </div>
      ) : null}
    </article>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-4 border-b border-black/6 pb-3 last:border-b-0 last:pb-0">
      <dt className="text-sm text-black/50">{label}</dt>
      <dd className="text-right text-base font-semibold">{value}</dd>
    </div>
  );
}

function formatDateTime(value: string | null) {
  if (!value) {
    return "未发生";
  }

  return new Date(value).toLocaleString("zh-CN", {
    hour12: false
  });
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
