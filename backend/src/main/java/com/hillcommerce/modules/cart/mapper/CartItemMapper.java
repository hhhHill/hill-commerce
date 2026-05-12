package com.hillcommerce.modules.cart.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hillcommerce.modules.cart.entity.CartItemEntity;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItemEntity> {
}
