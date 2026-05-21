# Manual Verification: product-discovery

## Browse Flow

- 首页可正常打开，并展示搜索入口、分类入口和首页商品列表
- 点击首页分类卡片后，可进入对应分类商品列表页
- 点击首页商品卡片后，可进入商品详情页

## Search Flow

- 在首页、分类页或搜索页输入商品名称关键字后，可进入搜索结果页
- 搜索词前后空格不会影响结果
- 空关键词提交后进入默认搜索页，不会报错
- 搜索结果中的商品卡片与分类商品列表页使用一致展示口径

## Detail Flow

- 商品详情页展示名称、价格、可售状态、规格选项、库存提示和详情描述
- 不可售但可浏览商品可正常打开详情页，并显示明确不可售状态
- 不存在或前台不可见商品打开时返回 404 页面

## Empty And Error States

- 首页无商品时展示首页空状态
- 分类无商品时展示分类空状态
- 搜索无结果时展示搜索空状态
- 商品缺图时展示统一缺图占位

## Logging Trigger Points

- 点击分类入口时会触发 `storefront.category.enter`
- 点击商品卡片时会触发 `storefront.product.click`
- 提交搜索时会触发 `storefront.search.submit`
- 打开商品详情页时会触发 `storefront.product.view`
