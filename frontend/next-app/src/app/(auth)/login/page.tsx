import LoginForm from "@/app/(auth)/login/login-form";

type LoginPageProps = {
  searchParams?: Promise<{
    next?: string;
    email?: string;
  }>;
};

export default async function LoginPage({ searchParams }: LoginPageProps) {
  const resolvedSearchParams = (await searchParams) ?? {};
  const nextPath = resolvedSearchParams.next ?? "/account";
  const initialEmail = resolvedSearchParams.email ?? "";

  return (
    <main className="min-h-screen px-6 py-12">
      <section className="mx-auto grid max-w-5xl gap-8 rounded-[32px] border border-black/10 bg-[var(--surface)] p-8 shadow-[0_20px_60px_rgba(74,42,18,0.08)] md:grid-cols-[1.1fr_0.9fr] md:p-12">
        <div className="flex flex-col justify-between gap-6">
          <div className="space-y-4">
            <span className="w-fit rounded-full bg-[var(--accent)] px-4 py-1 text-sm font-semibold text-white">
              Session Login
            </span>
            <h1 className="text-4xl font-bold tracking-tight">登录你的 hill-commerce 账号</h1>
            <p className="max-w-xl text-base leading-7 text-black/70">
              当前版本采用基于 Session 的登录态。登录成功后，你可以访问受保护的账户页和后台示例页。
            </p>
          </div>
          <div className="rounded-[24px] border border-[var(--accent)]/15 bg-white/70 p-5 text-sm leading-6 text-black/65">
            <p>管理员测试账号：</p>
            <p className="font-medium text-[var(--foreground)]">admin@hill-commerce.local / Admin@123456</p>
          </div>
        </div>

        <LoginForm initialEmail={initialEmail} nextPath={nextPath} />
      </section>
    </main>
  );
}
