package com.caspar.mapper;

import com.caspar.entity.CampusRegion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CampusRegionMapper {
    
    @Select("SELECT * FROM campus_region")
    List<CampusRegion> findAll();
}
