package com.caspar.mapper;

import com.caspar.entity.DormBuilding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DormBuildingMapper {
    List<DormBuilding> selectAll();

    DormBuilding findById(@Param("id") Long id);

    DormBuilding findByCode(@Param("code") String code);

    DormBuilding findByName(@Param("name") String name);

    DormBuilding findByManagerId(@Param("managerId") Long managerId);

    int insert(DormBuilding building);

    int update(DormBuilding building);

    int clearManagerBinding(@Param("managerId") Long managerId);
}
