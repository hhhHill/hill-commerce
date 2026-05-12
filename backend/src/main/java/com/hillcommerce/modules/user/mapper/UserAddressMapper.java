package com.hillcommerce.modules.user.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hillcommerce.modules.user.entity.UserAddressEntity;

@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddressEntity> {
}
