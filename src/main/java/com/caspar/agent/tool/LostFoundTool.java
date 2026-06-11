package com.caspar.agent.tool;

import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import com.caspar.entity.LostFound;
import com.caspar.entity.dto.LostFoundVO;
import com.caspar.mapper.LostFoundMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 失物招领工具：发布失物记录 + 简单相似度匹配
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LostFoundTool implements AgentTool {

    private final LostFoundMapper lostFoundMapper;

    @Override
    public String getName() {
        return "lostfound_lost";
    }

    @Override
    public ToolResult execute(ToolArgs args) {
        return doExecute(args, "LOST");
    }

    ToolResult doExecute(ToolArgs args, String type) {
        Map<String, Object> params = args.getParams();
        Long userId = args.getUserId();

        String itemName  = getString(params, "item_name");
        String color     = getString(params, "color");
        String location  = getString(params, "location");
        String desc      = getString(params, "description");

        LostFound lf = new LostFound();
        lf.setType(type);
        lf.setTitle(itemName);
        lf.setDescription(desc);
        lf.setLocation(location);
        lf.setStatus("OPEN");
        lf.setPublisherId(userId);
        lf.setCreateTime(LocalDateTime.now());
        lf.setUpdateTime(LocalDateTime.now());

        // 构造标题 = 物品名(颜色)
        String fullTitle = itemName + (color != null ? "（" + color + "）" : "");
        lf.setTitle(fullTitle);

        try {
            lostFoundMapper.insert(lf);

            // 相似度计算（设计文档公式）
            // score = 0.4*name_sim + 0.2*color_match + 0.25*location_match + 0.15*time_proximity
            String oppositeType = "LOST".equals(type) ? "FOUND" : "LOST";
            List<LostFoundVO> candidatesVO =
                    lostFoundMapper.selectList(oppositeType, null, null, 0, 20);

            List<Map<String, Object>> matched = new ArrayList<>();
            for (LostFoundVO c : candidatesVO) {
                double score = calcSimilarity(itemName, color, location, c);
                if (score >= 0.3) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getId());
                    m.put("title", c.getTitle());
                    m.put("location", c.getLocation());
                    m.put("publishTime", c.getCreateTime());
                    m.put("similarity", Math.round(score * 100) / 100.0);
                    matched.add(m);
                }
            }
            // 按相似度降序排序
            matched.sort((a, b) -> Double.compare(
                    (Double) b.get("similarity"), (Double) a.get("similarity")));
            if (matched.size() > 5) matched = matched.subList(0, 5);

            Map<String, Object> data = new HashMap<>();
            data.put("recordId", lf.getId());
            data.put("type", type);
            data.put("title", fullTitle);
            data.put("candidates", matched);

            String action = "LOST".equals(type) ? "失物" : "拾物";
            StringBuilder summary = new StringBuilder(
                    String.format("已发布%s记录：%s，", action, fullTitle));
            if (!matched.isEmpty()) {
                summary.append(String.format("系统找到 %d 条可能匹配的记录，请查看详情。", matched.size()));
            } else {
                summary.append("暂无匹配记录，我们会在有新匹配时通知您。");
            }

            return ToolResult.ok(summary.toString(), data);
        } catch (Exception e) {
            log.error("发布失物记录失败", e);
            return ToolResult.fail("发布失败：" + e.getMessage());
        }
    }

    /**
     * 相似度计算（设计文档算法）：
     * score = 0.4*name_sim + 0.2*color_match + 0.25*location_match + 0.15*time_proximity
     */
    private double calcSimilarity(String itemName, String color, String location, LostFoundVO candidate) {
        double nameSim = 0.0;
        String cName = extractName(candidate.getTitle());
        if (itemName != null && cName != null && !cName.isEmpty()) {
            if (cName.contains(itemName) || itemName.contains(cName)) {
                nameSim = 1.0;
            } else {
                // 字符级交集
                long common = itemName.chars().filter(ch ->
                        cName.indexOf(ch) >= 0).count();
                nameSim = (double) common / Math.max(itemName.length(), cName.length());
            }
        }

        double colorMatch = 0.0;
        if (color != null && candidate.getTitle() != null &&
                candidate.getTitle().contains(color)) {
            colorMatch = 1.0;
        }

        double locationMatch = 0.0;
        if (location != null && candidate.getLocation() != null) {
            String cLoc = candidate.getLocation();
            if (cLoc.contains(location) || location.contains(cLoc)) {
                locationMatch = 1.0;
            }
        }

        double timeProximity = 0.0;
        if (candidate.getCreateTime() != null) {
            long days = ChronoUnit.DAYS.between(
                    candidate.getCreateTime().toLocalDate(),
                    LocalDateTime.now().toLocalDate());
            timeProximity = Math.max(0.0, 1.0 - days / 7.0);
        }

        return 0.4 * nameSim + 0.2 * colorMatch + 0.25 * locationMatch + 0.15 * timeProximity;
    }

    private String extractName(String title) {
        if (title == null) return "";
        int idx = title.indexOf("（");
        return idx > 0 ? title.substring(0, idx) : title;
    }

    private String getString(Map<String, Object> params, String key) {
        Object v = params.get(key);
        return v == null || v.toString().isBlank() ? null : v.toString();
    }
}
