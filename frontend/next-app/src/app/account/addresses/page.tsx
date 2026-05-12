import Link from "next/link";

import { AddressBook } from "@/features/storefront/cart/address-book";
import { SearchForm } from "@/features/storefront/catalog/search-form";
import { requireUser } from "@/lib/auth/server";
import { getServerUserAddresses } from "@/lib/cart/server";

export default async function AddressManagementPage() {
  await requireUser("/account/addresses");
  const addresses = await getServerUserAddresses();

  return (
    <main className="min-h-screen px-6 py-10">
      <div className="mx-auto flex max-w-6xl flex-col gap-8">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href="/cart">
            返回购物车
          </Link>
          <SearchForm className="w-full max-w-md" />
        </div>

        <section className="space-y-3">
          <span className="rounded-full bg-[var(--surface)] px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-black/50">
            Addresses
          </span>
          <h1 className="text-4xl font-semibold tracking-tight">收货地址管理</h1>
          <p className="max-w-3xl text-sm leading-7 text-black/65">只要存在地址，系统就会保证且只保证一条默认地址。删除默认地址后，也会自动补出新的默认地址。</p>
        </section>

        <AddressBook addresses={addresses} />
      </div>
    </main>
  );
}
