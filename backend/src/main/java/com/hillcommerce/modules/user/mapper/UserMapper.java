package com.hillcommerce.modules.user.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hillcommerce.modules.user.entity.UserEntity;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    @Select("""
        select r.code
        from roles r
        join user_roles ur on ur.role_id = r.id
        where ur.user_id = #{userId}
        order by r.id
        """)
    List<String> findRoleCodesByUserId(Long userId);
}
