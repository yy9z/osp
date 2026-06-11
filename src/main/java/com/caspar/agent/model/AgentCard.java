package com.caspar.agent.model;

import lombok.Data;

@Data
public class AgentCard {
    /** 卡片类型: REPAIR_ORDER / PRODUCT / LOST_FOUND / ROUTE / MESSAGE */
    private String type;
    /** 卡片数据（与前端约定的结构） */
    private Object data;

    public static AgentCard of(String type, Object data) {
        AgentCard card = new AgentCard();
        card.setType(type);
        card.setData(data);
        return card;
    }
}
