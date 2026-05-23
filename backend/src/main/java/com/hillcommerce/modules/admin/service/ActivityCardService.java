package com.hillcommerce.modules.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hillcommerce.modules.admin.dto.ActivityCardDtos.CardUpdateItem;
import com.hillcommerce.modules.admin.entity.ActivityCardEntity;
import com.hillcommerce.modules.admin.mapper.ActivityCardMapper;

@Service
public class ActivityCardService {

    private final ActivityCardMapper activityCardMapper;

    public ActivityCardService(ActivityCardMapper activityCardMapper) {
        this.activityCardMapper = activityCardMapper;
    }

    public List<ActivityCardEntity> listByPlacement(String placement) {
        return activityCardMapper.selectList(
            new LambdaQueryWrapper<ActivityCardEntity>()
                .eq(ActivityCardEntity::getPlacement, placement)
                .orderByAsc(ActivityCardEntity::getSortOrder)
        );
    }

    public List<ActivityCardEntity> listActiveByPlacement(String placement) {
        return activityCardMapper.selectList(
            new LambdaQueryWrapper<ActivityCardEntity>()
                .eq(ActivityCardEntity::getPlacement, placement)
                .eq(ActivityCardEntity::getIsActive, true)
                .orderByAsc(ActivityCardEntity::getSortOrder)
        );
    }

    @Transactional
    public void batchUpdate(List<CardUpdateItem> cards) {
        for (CardUpdateItem item : cards) {
            ActivityCardEntity entity = activityCardMapper.selectById(item.id());
            if (entity == null) {
                continue;
            }
            entity.setTitle(item.title());
            entity.setImageUrl(item.imageUrl());
            entity.setLinkUrl(item.linkUrl());
            entity.setIsActive(item.isActive());
            entity.setSortOrder(item.sortOrder());
            activityCardMapper.updateById(entity);
        }
    }
}
