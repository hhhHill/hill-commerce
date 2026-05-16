package com.hillcommerce.modules.logging.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hillcommerce.modules.logging.entity.OperationLogEntity;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLogEntity> {
}
