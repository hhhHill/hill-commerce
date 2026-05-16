package com.hillcommerce.modules.logging.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hillcommerce.modules.logging.entity.LoginLogEntity;

@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLogEntity> {
}
