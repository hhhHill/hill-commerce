package com.hillcommerce.modules.user.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hillcommerce.modules.user.entity.RoleEntity;

@Mapper
public interface RoleMapper extends BaseMapper<RoleEntity> {
}
