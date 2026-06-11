package com.caspar.service.impl;

import com.caspar.common.PageResult;
import com.caspar.entity.LostFound;
import com.caspar.entity.LostFoundClaim;
import com.caspar.entity.dto.LostFoundMatchVO;
import com.caspar.entity.dto.LostFoundPublishDTO;
import com.caspar.entity.dto.LostFoundVO;
import com.caspar.mapper.LostFoundMapper;
import com.caspar.service.LostFoundService;
import com.caspar.util.PaginationUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class LostFoundServiceImpl implements LostFoundService {

    @Autowired
    private LostFoundMapper lostFoundMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "lostFoundList", allEntries = true),
            @CacheEvict(cacheNames = "lostFoundDetail", allEntries = true),
            @CacheEvict(cacheNames = "lostFoundMyList", allEntries = true),
            @CacheEvict(cacheNames = "lostFoundMatches", allEntries = true)
    })
    public Long publish(Long publisherId, LostFoundPublishDTO publishDTO) {
        if (publishDTO.getType() == null || publishDTO.getType().isEmpty()) {
            throw new IllegalArgumentException("请选择类型(LOST/FOUND)");
        }
        if (publishDTO.getTitle() == null || publishDTO.getTitle().isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (publishDTO.getCategory() == null || publishDTO.getCategory().isEmpty()) {
            throw new IllegalArgumentException("请选择分类");
        }

        LostFound lostFound = new LostFound();
        lostFound.setType(publishDTO.getType());
        lostFound.setTitle(publishDTO.getTitle());
        lostFound.setDescription(publishDTO.getDescription());
        lostFound.setCategory(publishDTO.getCategory());
        lostFound.setLocation(publishDTO.getLocation());
        lostFound.setReward(publishDTO.getReward());
        if (publishDTO.getImages() != null && !publishDTO.getImages().isEmpty()) {
            try {
                lostFound.setImages(objectMapper.writeValueAsString(publishDTO.getImages()));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("图片数据格式错误");
            }
        }
        lostFound.setStatus("OPEN");
        lostFound.setPublisherId(publisherId);
        lostFound.setCreateTime(LocalDateTime.now());

        lostFoundMapper.insert(lostFound);
        return lostFound.getId();
    }

    @Override
    @Cacheable(cacheNames = "lostFoundList",
            key = "T(String).format('%s:%s:%s:%s:%s', #page, #size, #type, #category, #keyword)")
    public PageResult<LostFoundVO> getList(Integer page, Integer size, String type, String category, String keyword) {
        int safePage = PaginationUtils.safePage(page);
        int safeSize = PaginationUtils.safeSize(size);
        int offset = PaginationUtils.offset(safePage, safeSize);

        List<LostFoundVO> records = lostFoundMapper.selectList(type, category, keyword, offset, safeSize);
        Long total = lostFoundMapper.count(type, category, keyword);
        return new PageResult<>(records, total, safePage, safeSize);
    }

    @Override
    @Cacheable(cacheNames = "lostFoundDetail", key = "#id", unless = "#result == null")
    public LostFoundVO getDetail(Long id) {
        return lostFoundMapper.findDetailById(id);
    }

    @Override
    @Cacheable(cacheNames = "lostFoundMyList",
            key = "T(String).format('%s:%s:%s:%s', #publisherId, #page, #size, #type)")
    public PageResult<LostFoundVO> getMyList(Long publisherId, Integer page, Integer size, String type) {
        int safePage = PaginationUtils.safePage(page);
        int safeSize = PaginationUtils.safeSize(size);
        int offset = PaginationUtils.offset(safePage, safeSize);

        List<LostFoundVO> records = lostFoundMapper.selectMyList(publisherId, type, offset, safeSize);
        Long total = lostFoundMapper.countMy(publisherId, type);
        return new PageResult<>(records, total, safePage, safeSize);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "lostFoundList", allEntries = true),
            @CacheEvict(cacheNames = "lostFoundDetail", allEntries = true),
            @CacheEvict(cacheNames = "lostFoundMyList", allEntries = true),
            @CacheEvict(cacheNames = "lostFoundMatches", allEntries = true)
    })
    public boolean claim(Long id, Long claimerId, String message) {
        LostFound lostFound = lostFoundMapper.findById(id);
        if (lostFound == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        if (!"OPEN".equals(lostFound.getStatus())) {
            throw new IllegalArgumentException("帖子状态不允许认领");
        }
        if (lostFoundMapper.countClaimsByLostFoundIdAndClaimerId(id, claimerId) > 0) {
            throw new IllegalArgumentException("您已提交过认领申请");
        }

        LostFoundClaim claim = new LostFoundClaim();
        claim.setLostfoundId(id);
        claim.setClaimerId(claimerId);
        claim.setMessage(message);
        claim.setStatus("PENDING");
        claim.setCreateTime(LocalDateTime.now());

        lostFoundMapper.insertClaim(claim);
        return true;
    }

    @Override
    public List<LostFoundClaim> getClaims(Long lostfoundId) {
        return lostFoundMapper.selectClaimsByLostFoundId(lostfoundId);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "lostFoundList", allEntries = true),
            @CacheEvict(cacheNames = "lostFoundDetail", allEntries = true),
            @CacheEvict(cacheNames = "lostFoundMyList", allEntries = true),
            @CacheEvict(cacheNames = "lostFoundMatches", allEntries = true)
    })
    public boolean delete(Long id, Long publisherId) {
        LostFound lostFound = lostFoundMapper.findById(id);
        if (lostFound == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        if (!lostFound.getPublisherId().equals(publisherId)) {
            throw new IllegalArgumentException("无权限操作");
        }
        return lostFoundMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "lostFoundList", allEntries = true),
            @CacheEvict(cacheNames = "lostFoundDetail", allEntries = true),
            @CacheEvict(cacheNames = "lostFoundMyList", allEntries = true),
            @CacheEvict(cacheNames = "lostFoundMatches", allEntries = true)
    })
    public boolean resolve(Long id, Long publisherId) {
        LostFound lostFound = lostFoundMapper.findById(id);
        if (lostFound == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        if (!lostFound.getPublisherId().equals(publisherId)) {
            throw new IllegalArgumentException("无权限操作");
        }
        if (!"OPEN".equals(lostFound.getStatus())) {
            throw new IllegalArgumentException("帖子状态不允许标记");
        }
        lostFoundMapper.updateStatus(id, "RESOLVED");
        return true;
    }

    @Override
    @Cacheable(cacheNames = "lostFoundMatches",
            key = "T(String).format('%s:%s', #id, #limit)")
    public List<LostFoundMatchVO> getSmartMatches(Long id, Integer limit) {
        LostFound source = lostFoundMapper.findById(id);
        if (source == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        if (!"OPEN".equalsIgnoreCase(source.getStatus())) {
            return List.of();
        }

        int safeLimit = normalizeLimit(limit);
        String oppositeType = "LOST".equalsIgnoreCase(source.getType()) ? "FOUND" : "LOST";

        List<LostFoundVO> candidates = lostFoundMapper.selectAllList(
                oppositeType,
                null,
                null,
                "OPEN",
                0,
                200
        );

        return candidates.stream()
                .filter(candidate -> !Objects.equals(candidate.getId(), source.getId()))
                .map(candidate -> buildMatch(source, candidate))
                .filter(match -> match.getMatchScore() >= 35)
                .sorted(Comparator
                        .comparing(LostFoundMatchVO::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(LostFoundMatchVO::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .toList();
    }

    private LostFoundMatchVO buildMatch(LostFound source, LostFoundVO candidate) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (sameText(source.getCategory(), candidate.getCategory())) {
            score += 30;
            reasons.add("分类一致");
        }

        int titleScore = weightedScore(textSimilarity(source.getTitle(), candidate.getTitle()), 25);
        if (titleScore > 0) {
            score += titleScore;
            reasons.add("标题关键词相似");
        }

        int descScore = weightedScore(textSimilarity(source.getDescription(), candidate.getDescription()), 20);
        if (descScore > 0) {
            score += descScore;
            reasons.add("描述信息相似");
        }

        int locationScore = weightedScore(textSimilarity(source.getLocation(), candidate.getLocation()), 20);
        if (locationScore > 0) {
            score += locationScore;
            reasons.add("地点信息接近");
        }

        int timeScore = timeProximityScore(source.getCreateTime(), candidate.getCreateTime());
        if (timeScore > 0) {
            score += timeScore;
            reasons.add("发布时间接近");
        }

        LostFoundMatchVO match = new LostFoundMatchVO();
        match.setId(candidate.getId());
        match.setType(candidate.getType());
        match.setTitle(candidate.getTitle());
        match.setCategory(candidate.getCategory());
        match.setLocation(candidate.getLocation());
        match.setStatus(candidate.getStatus());
        match.setPublisherName(candidate.getPublisherName());
        match.setPublisherPhone(candidate.getPublisherPhone());
        match.setImages(candidate.getImages());
        match.setCreateTime(candidate.getCreateTime());
        match.setMatchScore(Math.min(score, 100));
        match.setMatchReasons(reasons.stream().limit(4).toList());
        return match;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 5;
        }
        return Math.min(limit, 20);
    }

    private int weightedScore(double similarity, int weight) {
        if (similarity <= 0) {
            return 0;
        }
        return (int) Math.round(similarity * weight);
    }

    private int timeProximityScore(LocalDateTime sourceTime, LocalDateTime candidateTime) {
        if (sourceTime == null || candidateTime == null) {
            return 0;
        }
        long diffDays = Math.abs(Duration.between(sourceTime, candidateTime).toDays());
        if (diffDays <= 1) return 10;
        if (diffDays <= 3) return 8;
        if (diffDays <= 7) return 5;
        if (diffDays <= 14) return 2;
        return 0;
    }

    private boolean sameText(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private double textSimilarity(String source, String candidate) {
        Set<String> sourceTokens = tokenize(source);
        Set<String> candidateTokens = tokenize(candidate);
        if (sourceTokens.isEmpty() || candidateTokens.isEmpty()) {
            return 0;
        }

        Set<String> intersection = new HashSet<>(sourceTokens);
        intersection.retainAll(candidateTokens);
        if (intersection.isEmpty()) {
            return 0;
        }

        Set<String> union = new HashSet<>(sourceTokens);
        union.addAll(candidateTokens);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Set.of();
        }

        String text = rawText.toLowerCase(Locale.ROOT).trim();
        Set<String> tokens = new HashSet<>();

        String[] parts = text.split("[^\\p{IsHan}a-zA-Z0-9]+");
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            tokens.add(part);
            for (int i = 0; i < part.length() - 1; i++) {
                tokens.add(part.substring(i, i + 2));
            }
        }
        return tokens;
    }
}
