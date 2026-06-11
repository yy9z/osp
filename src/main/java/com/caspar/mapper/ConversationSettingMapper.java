package com.caspar.mapper;

import com.caspar.entity.ConversationSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ConversationSettingMapper {
    
    ConversationSetting findByUserAndOther(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);
    
    int insert(ConversationSetting setting);
    
    int update(ConversationSetting setting);
    
    int upsert(ConversationSetting setting);

    int deleteByUserAndOther(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);
}
