export type FieldHelpPage = "category" | "productList" | "productEditor";

export type FieldHelpItem = {
  title: string;
  description: string;
};

const FIELD_HELP: Record<FieldHelpPage, Record<string, FieldHelpItem>> = {
  category: {
    name: {
      title: "分类名称",
      description: "用于后台识别和前台展示这个一级分类。请填写商家和运营都能直接理解的业务名称。"
    },
    sortOrder: {
      title: "排序",
      description: "用于控制分类展示顺序。数值越小越靠前，建议按后台和前台希望看到的顺序维护。"
    },
    status: {
      title: "状态",
      description: "用于控制该分类是否还能继续被新商品选用。停用后历史商品仍保留分类归属，但新商品不应继续使用。"
    }
  },
  productList: {
    keyword: {
      title: "关键词",
      description: "用于按商品名称或 SPU 编码快速定位商品。适合在商品较多时先缩小范围再进入编辑。"
    },
    categoryId: {
      title: "分类",
      description: "用于按商品所属一级分类筛选结果。只会过滤商品归属，不会改变商品本身的分类。"
    },
    status: {
      title: "状态",
      description: "用于按草稿、已上架、已下架筛选商品。方便先看可售商品，或集中处理待上架内容。"
    }
  },
  productEditor: {
    categoryId: {
      title: "分类",
      description: "用于指定商品所属的一级分类。商品创建后会按这里的分类进入后台筛选和前台展示。"
    },
    status: {
      title: "商品状态",
      description: "用于控制当前商品是草稿、上架还是下架。状态会直接影响后台管理动作和前台是否可售。"
    },
    name: {
      title: "商品名称",
      description: "这是商品的主名称，会作为后台识别和前台核心展示名称。建议直接写用户能理解的正式商品名。"
    },
    spuCode: {
      title: "SPU 编码",
      description: "这是商品级识别编码，主要供后台管理和系统对接使用。可手工填写，留空时按系统规则自动生成。"
    },
    subtitle: {
      title: "副标题",
      description: "用于补充商品卖点或简要说明。适合写一句帮助运营和用户快速理解商品特点的补充信息。"
    },
    coverImageUrl: {
      title: "封面图 URL",
      description: "用于商品列表和详情主视觉展示。请填写可直接访问的图片地址，而不是本地文件路径。"
    },
    description: {
      title: "商品描述",
      description: "用于保存商品详情说明内容。这里存的是富文本或 HTML 源字符串，不是系统自动生成的简介。"
    },
    detailImageUrl: {
      title: "详情图 URL",
      description: "用于补充商品详情页展示内容。可维护多张图片，并按顺序控制展示先后。"
    },
    detailImageSortOrder: {
      title: "详情图排序",
      description: "用于控制详情图展示顺序。数值越小越靠前，建议和商品详情阅读顺序保持一致。"
    },
    attributeName: {
      title: "展示属性名",
      description: "用于描述不会生成 SKU 的商品展示信息，例如材质或产地。它只负责说明商品，不参与规格组合。"
    },
    attributeValue: {
      title: "展示属性值",
      description: "用于填写展示属性对应的具体内容。请和属性名一一对应，方便后台和前台统一理解。"
    },
    attributeSortOrder: {
      title: "展示属性排序",
      description: "用于控制展示属性的显示顺序。数值越小越靠前，建议按最重要的信息优先展示。"
    },
    salesAttributeName: {
      title: "销售属性名",
      description: "用于定义 SKU 差异维度，例如颜色或尺码。每个商品最多支持 2 个销售属性。"
    },
    salesAttributeValues: {
      title: "销售属性值",
      description: "用于填写某个销售属性下的可选值，多个值用逗号分隔。属性值组合会映射成不同 SKU 草稿。"
    },
    skuCode: {
      title: "SKU 编码",
      description: "用于标识具体 SKU 组合的后台编码。可手工改写，留空时由后端按规则补码。"
    },
    skuPrice: {
      title: "SKU 价格",
      description: "用于填写该 SKU 组合的实际销售价格。不同 SKU 可以有不同价格，商品起售价会取最低值。"
    },
    skuStock: {
      title: "SKU 库存",
      description: "用于填写该 SKU 当前可售库存数量。库存是 SKU 级字段，不会自动在不同组合之间共享。"
    },
    skuLowStockThreshold: {
      title: "低库存阈值",
      description: "用于定义该 SKU 被视为低库存的提醒阈值。库存低于这个值时，后台可据此触发提示或后续处理。"
    },
    skuStatus: {
      title: "SKU 状态",
      description: "用于控制某个 SKU 组合当前是否可用。即使商品存在，不可用 SKU 也不应作为正常可售组合处理。"
    }
  }
};

export function getFieldHelp(page: FieldHelpPage, field: string): FieldHelpItem | undefined {
  return FIELD_HELP[page][field];
}
