import LoginForm from "@/app/(auth)/login/login-form";
import { TestCredentials } from "@/features/storefront/auth/test-credentials";

type LoginPageProps = {
  searchParams?: Promise<{
    next?: string;
    email?: string;
  }>;
};

export default async function LoginPage({ searchParams }: LoginPageProps) {
  const resolvedSearchParams = (await searchParams) ?? {};
  const nextPath = resolvedSearchParams.next ?? "/";
  const initialEmail = resolvedSearchParams.email ?? "";

  return (
    <main className="min-h-screen px-6 py-12">
      <section className="mx-auto grid max-w-5xl gap-8 border-b border-[#f0f0f0] bg-white p-8 md:grid-cols-[1.1fr_0.9fr] md:p-12">
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
          <TestCredentials />
        </div>

        <LoginForm initialEmail={initialEmail} nextPath={nextPath} />
      </section>
    </main>
  );
}
