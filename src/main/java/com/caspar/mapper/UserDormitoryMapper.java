package com.caspar.mapper;

import com.caspar.entity.UserDormitory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserDormitoryMapper {
    UserDormitory findByUserId(@Param("userId") Long userId);

    int insert(UserDormitory userDormitory);

    int update(UserDormitory userDormitory);

    int deleteByUserId(@Param("userId") Long userId);

    int updateBuildingAndCampus(@Param("oldBuilding") String oldBuilding,
                                @Param("newBuilding") String newBuilding,
                                @Param("campus") String campus);
}
