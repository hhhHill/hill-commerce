"use client";

import { useState } from "react";

type CredentialRowProps = {
  label: string;
  value: string;
};

function CredentialRow({ label, value }: CredentialRowProps) {
  const [copied, setCopied] = useState(false);

  function handleCopy() {
    navigator.clipboard.writeText(value).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    });
  }

  return (
    <div className="flex items-center justify-between gap-3 py-1.5">
      <span className="min-w-0 text-sm text-black/65">
        {label}：<span className="font-medium text-[var(--foreground)]">{value}</span>
      </span>
      <button
        className="shrink-0 rounded-full border border-black/10 px-3 py-1 text-xs font-medium transition hover:border-[var(--accent)] hover:text-[var(--accent)]"
        type="button"
        onClick={handleCopy}
      >
        {copied ? "已复制" : "复制"}
      </button>
    </div>
  );
}

type TestAccount = {
  role: string;
  email: string;
  password: string;
};

const TEST_ACCOUNTS: TestAccount[] = [
  {
    role: "管理员",
    email: "admin@hill-commerce.local",
    password: "Admin@123456"
  },
  {
    role: "商家",
    email: "2250444016@qq.com",
    password: "11111111"
  }
];

export function TestCredentials() {
  return (
    <div className="flex flex-col gap-4 border-b border-[#f0f0f0] bg-white p-5">
      {TEST_ACCOUNTS.map((account) => (
        <div key={account.role}>
          <p className="text-sm font-semibold text-[var(--foreground)]">{account.role}测试账号：</p>
          <div className="mt-1 divide-y divide-[#f5f5f5]">
            <CredentialRow label="邮箱" value={account.email} />
            <CredentialRow label="密码" value={account.password} />
          </div>
        </div>
      ))}
    </div>
  );
}
