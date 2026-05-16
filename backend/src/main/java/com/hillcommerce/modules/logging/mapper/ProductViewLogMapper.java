package com.hillcommerce.modules.logging.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hillcommerce.modules.logging.entity.ProductViewLogEntity;

@Mapper
public interface ProductViewLogMapper extends BaseMapper<ProductViewLogEntity> {
}
