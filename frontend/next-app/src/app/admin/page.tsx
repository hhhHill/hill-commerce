import { redirect } from "next/navigation";

import { requireRole } from "@/lib/auth/server";

export default async function AdminPage() {
  await requireRole(["ADMIN", "MERCHANT"], "/admin");

  redirect("/admin/products");
}
