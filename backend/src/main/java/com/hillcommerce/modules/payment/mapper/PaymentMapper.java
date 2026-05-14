package com.hillcommerce.modules.payment.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hillcommerce.modules.payment.entity.PaymentEntity;

@Mapper
public interface PaymentMapper extends BaseMapper<PaymentEntity> {
}
