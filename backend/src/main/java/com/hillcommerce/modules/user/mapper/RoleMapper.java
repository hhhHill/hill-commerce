package com.hillcommerce.modules.user.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hillcommerce.modules.user.entity.RoleEntity;

/** 角色表 Mapper，继承 MyBatis-Plus BaseMapper，无额外自定义查询。 */
@Mapper
public interface RoleMapper extends BaseMapper<RoleEntity> {
}
