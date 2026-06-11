package com.caspar.agent.tool;

import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import com.caspar.entity.SecondhandGoods;
import com.caspar.entity.enums.GoodsStatus;
import com.caspar.mapper.SecondhandGoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 二手商品发布工具：通过 Agent 自然语言发布二手商品。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecondHandPublishTool implements AgentTool {

    private final SecondhandGoodsMapper goodsMapper;

    @Override
    public String getName() {
        return "secondhand_publish";
    }

    @Override
    public ToolResult execute(ToolArgs args) {
        Map<String, Object> params = args.getParams();
        Long userId = args.getUserId();

        String title = (String) params.getOrDefault("title", "");
        if (title.isBlank()) {
            return ToolResult.builder()
                    .success(false)
                    .errorMessage("商品名称不能为空")
                    .build();
        }

        String description = (String) params.getOrDefault("description", "");
        String priceStr = String.valueOf(params.getOrDefault("price", "0"));
        String category = (String) params.getOrDefault("category", "OTHER");

        BigDecimal price;
        try {
            price = new BigDecimal(priceStr);
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new NumberFormatException("price negative");
            }
        } catch (NumberFormatException e) {
            return ToolResult.builder()
                    .success(false)
                    .errorMessage("价格格式不正确，请输入有效数字")
                    .build();
        }

        // 标准化分类
        String normalizedCategory = normalizeCategory(category);

        SecondhandGoods goods = new SecondhandGoods();
        goods.setTitle(title);
        goods.setDescription(description.isBlank() ? title : description);
        goods.setPrice(price);
        goods.setCategory(normalizedCategory);
        goods.setCondition("GOOD");
        goods.setStatus(GoodsStatus.PENDING); // 发布后待审核
        goods.setSellerId(userId);
        goods.setViewCount(0);
        goods.setCreateTime(LocalDateTime.now());
        goods.setUpdateTime(LocalDateTime.now());

        try {
            goodsMapper.insert(goods);
            return ToolResult.builder()
                    .success(true)
                    .summary(String.format("商品《%s》已提交，定价 %.2f 元，等待管理员审核后即可上架。", title, price))
                    .data(Map.of(
                            "id", goods.getId() != null ? goods.getId() : 0,
                            "title", title,
                            "price", price,
                            "status", "AUDITING"
                    ))
                    .build();
        } catch (Exception e) {
            log.error("发布二手商品失败", e);
            return ToolResult.builder()
                    .success(false)
                    .errorMessage("发布失败：" + e.getMessage())
                    .build();
        }
    }

    private String normalizeCategory(String raw) {
        if (raw == null) return "OTHER";
        String lower = raw.toLowerCase();
        if (lower.contains("电子") || lower.contains("数码") || lower.contains("electronics")) return "ELECTRONICS";
        if (lower.contains("书") || lower.contains("book")) return "BOOKS";
        if (lower.contains("衣") || lower.contains("服") || lower.contains("clothing")) return "CLOTHING";
        if (lower.contains("运动") || lower.contains("sport")) return "SPORTS";
        if (lower.contains("日用") || lower.contains("生活") || lower.contains("daily")) return "DAILY";
        return "OTHER";
    }
}
