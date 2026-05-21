package com.hillcommerce.modules.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hillcommerce.modules.admin.entity.ShopEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ShopMapper extends BaseMapper<ShopEntity> {
    @Select("SELECT * FROM shops WHERE owner_id = #{ownerId}")
    ShopEntity findByOwnerId(Long ownerId);

    @Select("SELECT * FROM shops WHERE slug = #{slug}")
    ShopEntity findBySlug(String slug);
}
