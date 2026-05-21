package com.hillcommerce.modules.product.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hillcommerce.modules.product.entity.ProductEntity;

@Mapper
public interface ProductMapper extends BaseMapper<ProductEntity> {

    @Select("""
        select *
        from products
        where deleted = 0
          and status = 'ON_SHELF'
          and match(name, subtitle) against(#{keyword} in boolean mode)
        order by match(name, subtitle) against(#{keyword} in boolean mode) desc
        """)
    List<ProductEntity> searchByKeyword(@Param("keyword") String keyword);
}
