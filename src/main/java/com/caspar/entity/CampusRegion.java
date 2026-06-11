package com.caspar.entity;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.time.LocalDateTime;

public class CampusRegion {
    private Long id;
    private String name;
    private String type;
    
    @JsonRawValue
    private String geojson;
    
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGeojson() {
        return geojson;
    }

    @JsonSetter
    public void setGeojson(String geojson) {
        this.geojson = geojson;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
