import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { AdminProductLogCenter } from "@/features/admin/logs/admin-product-log-center";
import { requireRole } from "@/lib/auth/server";
import { getServerProductLogs } from "@/lib/admin/server";

type AdminProductLogsPageProps = {
  searchParams: Promise<{
    actionType?: string;
    productName?: string;
    spuCode?: string;
    operatorUserId?: string;
    page?: string;
  }>;
};

export default async function AdminProductLogsPage({
  searchParams,
}: AdminProductLogsPageProps) {
  const user = await requireRole(["ADMIN", "MERCHANT"], "/admin/product-logs");
  const query = await searchParams;
  const page = Number(query.page) || 1;

  const result = await getServerProductLogs({
    actionType: query.actionType,
    productName: query.productName,
    spuCode: query.spuCode,
    operatorUserId: query.operatorUserId,
    page,
    size: 20,
  });

  return (
    <AdminShell
      title="商品日志"
      description="追踪所有商品的创建、编辑、上下架记录，支持按操作类型和商品筛选。"
      user={user}
    >
      <AdminProductLogCenter
        result={result}
        filters={{
          actionType: query.actionType,
          productName: query.productName,
          spuCode: query.spuCode,
          operatorUserId: query.operatorUserId,
          page: query.page,
        }}
      />
    </AdminShell>
  );
}
