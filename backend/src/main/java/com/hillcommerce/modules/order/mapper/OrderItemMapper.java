package com.hillcommerce.modules.order.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hillcommerce.modules.order.entity.OrderItemEntity;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemEntity> {
}
