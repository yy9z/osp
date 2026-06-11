package com.caspar.agent.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UstcCampusResolver {

    private static final double AMBIGUOUS_DISTANCE_METERS = 1200;
    private static final List<String> USTC_MARKERS = List.of("中国科学技术大学", "中国科大", "ustc");

    private static final List<Campus> CAMPUSES = List.of(
            new Campus("东校区", 31.8369, 117.2708, List.of("东校区", "东区", "东")),
            new Campus("西校区", 31.8414, 117.2579, List.of("西校区", "西区", "西")),
            new Campus("南校区", 31.8256, 117.2840, List.of("南校区", "南区", "南")),
            new Campus("中校区", 31.8462, 117.2655, List.of("中校区", "中区", "中")),
            new Campus("高新校区", 31.8518, 117.1413, List.of("高新校区", "高新区", "高新"))
    );

    public String normalizeCampus(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String text = input.trim();

        // 1) 先匹配强别名（如“南校区”“西区”），避免“南校区东门”被单字“东”误判为东校区
        String strongMatch = CAMPUSES.stream()
                .filter(campus -> campus.aliases.stream()
                        .filter(this::isStrongCampusAlias)
                        .anyMatch(text::contains))
                .map(Campus::name)
                .findFirst()
                .orElse(null);
        if (strongMatch != null) {
            return strongMatch;
        }

        // 2) 再允许单字别名，但仅在“完全相等”时生效（例如用户仅输入“东”）
        return CAMPUSES.stream()
                .filter(campus -> campus.aliases.stream()
                        .filter(alias -> !isStrongCampusAlias(alias))
                        .anyMatch(text::equals))
                .map(Campus::name)
                .findFirst()
                .orElse(null);
    }

    public String inferCampusFromText(String text) {
        return normalizeCampus(text);
    }

    public String inferCampusFromPlaceText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.trim();
        return CAMPUSES.stream()
                .filter(campus -> campus.aliases.stream()
                        .filter(this::isStrongCampusAlias)
                        .anyMatch(normalized::contains))
                .map(Campus::name)
                .findFirst()
                .orElse(null);
    }

    public CampusDecision decideCampus(String explicitCampus, Double userLat, Double userLng) {
        String normalized = normalizeCampus(explicitCampus);
        if (normalized != null) {
            return CampusDecision.resolved(normalized, "已按您指定的校区进行匹配");
        }
        if (userLat == null || userLng == null) {
            return CampusDecision.clarify(defaultCampusOptions(), "中科大有多个校区，请问您想去哪个校区？");
        }

        List<CampusDistance> distances = CAMPUSES.stream()
                .map(campus -> new CampusDistance(campus, distanceMeters(userLat, userLng, campus.lat, campus.lng)))
                .sorted(Comparator.comparingDouble(CampusDistance::distance))
                .toList();

        if (distances.isEmpty()) {
            return CampusDecision.clarify(defaultCampusOptions(), "中科大有多个校区，请问您想去哪个校区？");
        }

        CampusDistance nearest = distances.get(0);
        if (distances.size() == 1) {
            return CampusDecision.resolved(nearest.campus.name, "已按您当前位置优先匹配到最近校区");
        }

        CampusDistance second = distances.get(1);
        if (Math.abs(second.distance - nearest.distance) <= AMBIGUOUS_DISTANCE_METERS) {
            return CampusDecision.clarify(List.of(nearest.campus.name, second.campus.name), "您离多个校区距离接近，请问您想去哪个校区？");
        }

        return CampusDecision.resolved(nearest.campus.name, "已按您当前位置优先匹配到最近校区");
    }

    public String preferCampus(Double userLat, Double userLng) {
        if (userLat == null || userLng == null) {
            return null;
        }
        return CAMPUSES.stream()
                .min(Comparator.comparingDouble(campus -> distanceMeters(userLat, userLng, campus.lat, campus.lng)))
                .map(Campus::name)
                .orElse(null);
    }

    public String buildSearchKeyword(String query, String campus) {
        String normalizedCampus = normalizeCampus(campus);
        if (query == null || query.isBlank()) {
            return "中国科学技术大学" + (normalizedCampus != null ? " " + normalizedCampus : "");
        }
        String trimmed = query.trim();
        if (containsUstcMarker(query)) {
            // 查询词里已有科大标记时，仍尽量补充校区，避免“图书馆”等跨校区词条命中不稳定
            if (normalizedCampus != null && inferCampusFromText(trimmed) == null) {
                return (trimmed + " " + normalizedCampus).trim();
            }
            return trimmed;
        }
        StringBuilder sb = new StringBuilder("中国科学技术大学");
        if (normalizedCampus != null) {
            sb.append(" ").append(normalizedCampus);
        }
        if (query != null && !query.isBlank()) {
            sb.append(" ").append(trimmed);
        }
        return sb.toString().trim();
    }

    public List<String> defaultCampusOptions() {
        return CAMPUSES.stream().map(Campus::name).toList();
    }

    public Optional<String> detectCampusFromAddress(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }
        if (!containsUstcMarker(address)) {
            return Optional.empty();
        }
        return CAMPUSES.stream()
                .filter(campus -> campus.aliases.stream()
                        .filter(this::isStrongCampusAlias)
                        .anyMatch(address::contains))
                .map(Campus::name)
                .findFirst();
    }

    public boolean containsUstcMarker(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.trim().toLowerCase();
        return USTC_MARKERS.stream().anyMatch(normalized::contains);
    }

    public boolean looksLikeUstcPlace(String name, String address) {
        if (containsUstcMarker(name) || containsUstcMarker(address)) {
            return true;
        }
        if (detectCampusFromAddress(address).isPresent()) {
            return true;
        }
        return inferCampusFromPlaceText(name) != null;
    }

    private boolean isStrongCampusAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return false;
        }
        String trimmed = alias.trim();
        return trimmed.length() >= 2;
    }

    public double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private record Campus(String name, double lat, double lng, List<String> aliases) {
    }

    private record CampusDistance(Campus campus, double distance) {
    }

    @Getter
    public static class CampusDecision {
        private final boolean clarificationRequired;
        private final String campus;
        private final String message;
        private final List<String> options;

        private CampusDecision(boolean clarificationRequired, String campus, String message, List<String> options) {
            this.clarificationRequired = clarificationRequired;
            this.campus = campus;
            this.message = message;
            this.options = options;
        }

        public static CampusDecision resolved(String campus, String message) {
            return new CampusDecision(false, campus, message, List.of());
        }

        public static CampusDecision clarify(List<String> options, String message) {
            List<String> normalized = options.stream().filter(Objects::nonNull).distinct().toList();
            return new CampusDecision(true, null, message, normalized);
        }
    }
}
