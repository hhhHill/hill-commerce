-- Upsert the 10 fixed categories first
INSERT INTO product_categories (name, sort_order, status) VALUES
('手机数码', 0, 'ENABLED'),
('家用电器', 1, 'ENABLED'),
('服饰鞋包', 2, 'ENABLED'),
('美妆个护', 3, 'ENABLED'),
('家居生活', 4, 'ENABLED'),
('食品饮料', 5, 'ENABLED'),
('母婴玩具', 6, 'ENABLED'),
('运动户外', 7, 'ENABLED'),
('汽车用品', 8, 'ENABLED'),
('其他分类', 9, 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Cleanup all data tied to non-fixed categories

DELETE pvl
  FROM product_view_logs pvl
  JOIN product_categories pc ON pvl.category_id = pc.id
 WHERE pc.name NOT IN ('手机数码','家用电器','服饰鞋包','美妆个护','家居生活','食品饮料','母婴玩具','运动户外','汽车用品','其他分类');

DELETE ci
  FROM cart_items ci
  JOIN product_skus sku ON ci.sku_id = sku.id
  JOIN products p ON sku.product_id = p.id
  JOIN product_categories pc ON p.category_id = pc.id
 WHERE pc.name NOT IN ('手机数码','家用电器','服饰鞋包','美妆个护','家居生活','食品饮料','母婴玩具','运动户外','汽车用品','其他分类');

DELETE oi
  FROM order_items oi
  JOIN product_skus sku ON oi.sku_id = sku.id
  JOIN products p ON sku.product_id = p.id
  JOIN product_categories pc ON p.category_id = pc.id
 WHERE pc.name NOT IN ('手机数码','家用电器','服饰鞋包','美妆个护','家居生活','食品饮料','母婴玩具','运动户外','汽车用品','其他分类');

DELETE psav
  FROM product_sales_attribute_values psav
  JOIN product_sales_attributes psa ON psa.id = psav.sales_attribute_id
  JOIN products p ON p.id = psa.product_id
  JOIN product_categories pc ON p.category_id = pc.id
 WHERE pc.name NOT IN ('手机数码','家用电器','服饰鞋包','美妆个护','家居生活','食品饮料','母婴玩具','运动户外','汽车用品','其他分类');

DELETE pa
  FROM product_attribute_values pa
  JOIN products p ON p.id = pa.product_id
  JOIN product_categories pc ON p.category_id = pc.id
 WHERE pc.name NOT IN ('手机数码','家用电器','服饰鞋包','美妆个护','家居生活','食品饮料','母婴玩具','运动户外','汽车用品','其他分类');

DELETE pi
  FROM product_images pi
  JOIN products p ON p.id = pi.product_id
  JOIN product_categories pc ON p.category_id = pc.id
 WHERE pc.name NOT IN ('手机数码','家用电器','服饰鞋包','美妆个护','家居生活','食品饮料','母婴玩具','运动户外','汽车用品','其他分类');

DELETE sku
  FROM product_skus sku
  JOIN products p ON sku.product_id = p.id
  JOIN product_categories pc ON p.category_id = pc.id
 WHERE pc.name NOT IN ('手机数码','家用电器','服饰鞋包','美妆个护','家居生活','食品饮料','母婴玩具','运动户外','汽车用品','其他分类');

DELETE psa
  FROM product_sales_attributes psa
  JOIN products p ON p.id = psa.product_id
  JOIN product_categories pc ON p.category_id = pc.id
 WHERE pc.name NOT IN ('手机数码','家用电器','服饰鞋包','美妆个护','家居生活','食品饮料','母婴玩具','运动户外','汽车用品','其他分类');

DELETE p
  FROM products p
  JOIN product_categories pc ON p.category_id = pc.id
 WHERE pc.name NOT IN ('手机数码','家用电器','服饰鞋包','美妆个护','家居生活','食品饮料','母婴玩具','运动户外','汽车用品','其他分类');

DELETE FROM product_categories
 WHERE name NOT IN ('手机数码','家用电器','服饰鞋包','美妆个护','家居生活','食品饮料','母婴玩具','运动户外','汽车用品','其他分类');
