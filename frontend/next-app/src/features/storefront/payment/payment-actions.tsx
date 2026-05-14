type PaymentActionsProps = {
  canCreateAttempt: boolean;
  canActOnAttempt: boolean;
  busy: boolean;
  onCreateAttempt: () => void;
  onSucceed: () => void;
  onFail: () => void;
};

export function PaymentActions({
  canCreateAttempt,
  canActOnAttempt,
  busy,
  onCreateAttempt,
  onSucceed,
  onFail
}: PaymentActionsProps) {
  if (canActOnAttempt) {
    return (
      <div className="flex flex-col gap-3">
        <button
          className="rounded-full bg-[var(--accent)] px-5 py-3 text-sm font-semibold text-white"
          disabled={busy}
          type="button"
          onClick={onSucceed}
        >
          {busy ? "处理中..." : "模拟支付成功"}
        </button>
        <button
          className="rounded-full border border-black/10 px-5 py-3 text-sm font-medium"
          disabled={busy}
          type="button"
          onClick={onFail}
        >
          {busy ? "处理中..." : "模拟支付失败"}
        </button>
      </div>
    );
  }

  if (canCreateAttempt) {
    return (
      <button
        className="rounded-full bg-[var(--accent)] px-5 py-3 text-sm font-semibold text-white"
        disabled={busy}
        type="button"
        onClick={onCreateAttempt}
      >
        {busy ? "处理中..." : "创建支付尝试"}
      </button>
    );
  }

  return null;
}
