import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TestAmapLogic {
    public static void main(String[] args) throws Exception {
        String query = "";
        String campus = null;

        List<String> expandedTerms = expandKeywords(query);
        System.out.println("expandedTerms: " + expandedTerms);

        String primaryTerm = expandedTerms.isEmpty() ? query : expandedTerms.get(0);
        String textKeyword = buildSearchKeyword(primaryTerm, campus);
        System.out.println("textKeyword: " + textKeyword);
        System.out.println("Encoded keywords=" + URLEncoder.encode(textKeyword, "UTF-8"));
        
        System.out.println("---");
        
        List<String> aroundTerms = expandedTerms.subList(0, Math.min(expandedTerms.size(), 2));
        for (String term : aroundTerms) {
            String aroundKeyword = buildAroundKeyword(term);
            System.out.println("aroundKeyword: " + aroundKeyword);
            System.out.println("Encoded keywords=" + URLEncoder.encode(aroundKeyword, "UTF-8"));
        }
    }

    private static Map<String, List<String>> KEYWORD_EXPANSION = Map.of(
            "食堂", List.of("食堂", "学生食堂", "餐厅"),
            "宿舍", List.of("宿舍", "学生公寓", "公寓"),
            "图书馆", List.of("图书馆", "阅览室"),
            "教学楼", List.of("教学楼", "理化大楼", "教室"),
            "西门", List.of("西门"),
            "东门", List.of("东门"),
            "南门", List.of("南门"),
            "北门", List.of("北门")
    );

    public static List<String> expandKeywords(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String trimmed = query.trim();
        for (Map.Entry<String, List<String>> entry : KEYWORD_EXPANSION.entrySet()) {
            if (trimmed.equals(entry.getKey()) || trimmed.contains(entry.getKey())) {
                List<String> result = new ArrayList<>();
                result.add(trimmed);
                for (String val : entry.getValue()) {
                    if (!val.equals(trimmed)) {
                        result.add(val);
                    }
                }
                return result;
            }
        }
        return List.of(trimmed);
    }
    
    private static Map<String, String> CAMPUS_SHORT_NAMES = Map.of(
            "东校区", "东区",
            "西校区", "西区",
            "南校区", "南区",
            "中校区", "中区",
            "高新校区", "高新区"
    );

    public static String buildSearchKeyword(String query, String campus) {
        String campusShort = campus != null ? CAMPUS_SHORT_NAMES.getOrDefault(campus, campus) : null;
        StringBuilder sb = new StringBuilder("中科大");
        if (campusShort != null) {
            sb.append(campusShort);
        }
        if (query != null && !query.isBlank()) {
            sb.append(query.trim());
        }
        return sb.toString().trim();
    }
    
    public static String buildAroundKeyword(String query) {
        return query != null ? query.trim() : "";
    }
}
