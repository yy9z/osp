package com.caspar.agent.tool;

import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import com.caspar.entity.dto.SecondhandGoodsVO;
import com.caspar.mapper.SecondhandGoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 二手商品搜索工具：根据关键词、价格上限等条件搜索推荐商品。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecondHandTool implements AgentTool {

    private final SecondhandGoodsMapper goodsMapper;

    @Override
    public String getName() {
        return "secondhand_search";
    }

    @Override
    public ToolResult execute(ToolArgs args) {
        Map<String, Object> params = args.getParams();

        String keyword   = getString(params, "keyword");
        String category  = getString(params, "category");
        String sortPref  = getString(params, "sort_preference");

        // 价格过滤：从槽位中读取，支持 max_price 字段
        BigDecimal maxPrice = null;
        Object maxPriceRaw = params.get("max_price");
        if (maxPriceRaw != null && !maxPriceRaw.toString().isBlank()) {
            try {
                maxPrice = new BigDecimal(maxPriceRaw.toString());
            } catch (NumberFormatException ignored) {}
        }

        // 排序映射
        String sort = "smart";
        if ("price_asc".equals(sortPref)) sort = "price_asc";
        else if ("price_desc".equals(sortPref)) sort = "price_desc";
        else if ("latest".equals(sortPref) || "create_time_desc".equals(sortPref)) sort = "latest";

        List<SecondhandGoodsVO> goods = goodsMapper.selectList(
                category, keyword, sort, 0, 6
        );

        // 若有价格上限则在内存过滤（数据库层已尽量用索引）
        if (maxPrice != null) {
            BigDecimal finalMaxPrice = maxPrice;
            goods = goods.stream()
                    .filter(g -> g.getPrice() != null && g.getPrice().compareTo(finalMaxPrice) <= 0)
                    .toList();
        }

        if (goods.isEmpty()) {
            return ToolResult.ok("未找到符合条件的二手商品。", List.of());
        }

        // 生成推荐理由（写入专用字段，不污染 category）
        for (SecondhandGoodsVO g : goods) {
            g.setRecommendReason(buildRecommendReason(g, maxPrice));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", goods.size());
        data.put("items", goods);

        String summary = String.format("为您找到 %d 件相关二手商品。", goods.size());
        return ToolResult.ok(summary, data);
    }

    private String buildRecommendReason(SecondhandGoodsVO g, BigDecimal maxPrice) {
        if (maxPrice != null && g.getPrice() != null) {
            int pct = maxPrice.subtract(g.getPrice()).multiply(BigDecimal.valueOf(100))
                    .divide(maxPrice, 0, java.math.RoundingMode.HALF_UP).intValue();
            if (pct > 0) return "价格低于预算 " + pct + "%";
        }
        return "近期发布";
    }

    private String getString(Map<String, Object> params, String key) {
        Object v = params.get(key);
        return v == null || v.toString().isBlank() ? null : v.toString();
    }
}
