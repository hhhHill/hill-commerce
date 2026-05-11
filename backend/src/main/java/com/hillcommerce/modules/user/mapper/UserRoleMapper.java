package com.hillcommerce.modules.user.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hillcommerce.modules.user.entity.UserRoleEntity;

/** 用户-角色关联表 Mapper，继承 MyBatis-Plus BaseMapper，无额外自定义查询。 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleEntity> {
}
