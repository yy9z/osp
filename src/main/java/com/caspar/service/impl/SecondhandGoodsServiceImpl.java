package com.caspar.service.impl;

import com.caspar.common.PageResult;
import com.caspar.entity.GoodsFavorite;
import com.caspar.entity.Notification;
import com.caspar.entity.SecondhandGoods;
import com.caspar.entity.User;
import com.caspar.entity.dto.SecondhandGoodsVO;
import com.caspar.entity.dto.SecondhandPublishDTO;
import com.caspar.entity.enums.GoodsStatus;
import com.caspar.mapper.GoodsFavoriteMapper;
import com.caspar.mapper.SecondhandGoodsMapper;
import com.caspar.service.NotificationService;
import com.caspar.service.SecondhandGoodsService;
import com.caspar.service.SecondhandInnovationService;
import com.caspar.service.UserService;
import com.caspar.util.AliyunOSSOperator;
import com.caspar.util.PaginationUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 二手商品服务实现类
 */
@Service
public class SecondhandGoodsServiceImpl implements SecondhandGoodsService {

    // Refactor: 用 SLF4J Logger 替代 System.out.println，支持生产环境日志级别控制
    private static final Logger logger = LoggerFactory.getLogger(SecondhandGoodsServiceImpl.class);

    @Autowired
    private SecondhandGoodsMapper secondhandGoodsMapper;

    @Autowired
    private GoodsFavoriteMapper goodsFavoriteMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;

    @Autowired
    private SecondhandInnovationService secondhandInnovationService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Set<String> MARKET_SORT_WHITELIST = new HashSet<>(Set.of("smart", "latest", "price_asc", "price_desc"));

    @Override
    @Transactional
    public Long publish(Long sellerId, SecondhandPublishDTO publishDTO) {
        // 参数校验
        if (publishDTO.getTitle() == null || publishDTO.getTitle().isEmpty()) {
            throw new IllegalArgumentException("商品标题不能为空");
        }
        if (publishDTO.getPrice() == null || publishDTO.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("商品价格必须大于0");
        }
        if (publishDTO.getCategory() == null || publishDTO.getCategory().isEmpty()) {
            throw new IllegalArgumentException("请选择商品分类");
        }
        if (publishDTO.getCondition() == null || publishDTO.getCondition().isEmpty()) {
            throw new IllegalArgumentException("请选择商品新旧程度");
        }

        // 构建商品对象
        SecondhandGoods goods = new SecondhandGoods();
        goods.setTitle(publishDTO.getTitle());
        goods.setDescription(publishDTO.getDescription());
        goods.setPrice(publishDTO.getPrice());
        goods.setCategory(publishDTO.getCategory());
        goods.setCondition(publishDTO.getCondition());
        // 初始状态设为待审核
        goods.setStatus(GoodsStatus.PENDING);
        goods.setSellerId(sellerId);
        goods.setViewCount(0);
        goods.setCreateTime(LocalDateTime.now());

        // 处理图片：优先上传文件到OSS
        List<String> imageUrls = new ArrayList<>();

        logger.debug("发布商品 - 图片处理: imageFiles={}, images={}",
                publishDTO.getImageFiles(), publishDTO.getImages());

        // 1. 处理上传的图片文件
        if (publishDTO.getImageFiles() != null && !publishDTO.getImageFiles().isEmpty()) {
            for (MultipartFile file : publishDTO.getImageFiles()) {
                if (file != null && !file.isEmpty()) {
                    try {
                        String url = aliyunOSSOperator.upload(file);
                        imageUrls.add(url);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("图片上传失败: " + e.getMessage());
                    }
                }
            }
        }

        // 2. 处理已提供的图片URL
        if (publishDTO.getImages() != null && !publishDTO.getImages().isEmpty()) {
            imageUrls.addAll(publishDTO.getImages());
        }

        // 转换图片列表为JSON
        if (!imageUrls.isEmpty()) {
            try {
                String imagesJson = objectMapper.writeValueAsString(imageUrls);
                logger.debug("商品图片JSON: {}", imagesJson);
                goods.setImages(imagesJson);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("图片格式错误");
            }
        } else {
            logger.warn("发布商品时未提供任何图片");
        }

        logger.debug("准备插入数据库 - images: {}", goods.getImages());
        secondhandGoodsMapper.insert(goods);

        return goods.getId();
    }

    @Override
    public PageResult<SecondhandGoodsVO> getList(Integer page, Integer size, String category, String keyword, String sort) {
        int safePage = PaginationUtils.safePage(page);
        int safeSize = PaginationUtils.safeSize(size);
        int offset = PaginationUtils.offset(safePage, safeSize);
        String safeSort = normalizeSort(sort);

        List<SecondhandGoodsVO> records = secondhandGoodsMapper.selectList(category, keyword, safeSort, offset, safeSize);
        Long total = secondhandGoodsMapper.count(category, keyword);

        // 转换图片字段并补充可解释推荐理由
        for (SecondhandGoodsVO vo : records) {
            vo.setImageList(parseImages(vo.getImages()));
        }
        enrichRecommendReasons(records, safeSort, keyword);

        return new PageResult<>(records, total, safePage, safeSize);
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "smart";
        }
        return MARKET_SORT_WHITELIST.contains(sort) ? sort : "smart";
    }

    private void enrichRecommendReasons(List<SecondhandGoodsVO> records, String sort, String keyword) {
        if (records == null || records.isEmpty()) {
            return;
        }

        final double avgPrice = records.stream()
                .map(SecondhandGoodsVO::getPrice)
                .filter(p -> p != null)
                .collect(Collectors.averagingDouble(p -> p.doubleValue()));

        for (SecondhandGoodsVO vo : records) {
            vo.setRecommendReason(buildRecommendReason(vo, sort, keyword, avgPrice));
        }
    }

    private String buildRecommendReason(SecondhandGoodsVO vo, String sort, String keyword, double avgPrice) {
        if ("price_asc".equals(sort)) {
            return "低价优先排序";
        }
        if ("price_desc".equals(sort)) {
            return "高价位商品优先展示";
        }
        if ("latest".equals(sort)) {
            return isRecentPublish(vo, 3) ? "近3天新发布" : "按发布时间排序";
        }

        // smart: 优先给出2条可解释标签，便于用户理解为什么被推荐
        List<String> tags = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            if (containsIgnoreCase(vo.getTitle(), kw)) {
                tags.add("标题匹配需求");
            } else if (containsIgnoreCase(vo.getDescription(), kw)) {
                tags.add("描述匹配需求");
            }
        }

        if (vo.getPrice() != null && avgPrice > 0 && vo.getPrice().doubleValue() <= avgPrice * 0.85) {
            tags.add("价格低于同页均价");
        }
        if (isGoodCondition(vo.getCondition())) {
            tags.add("成色较新");
        }
        if (vo.getViewCount() != null && vo.getViewCount() >= 80) {
            tags.add("关注度较高");
        }
        if (isRecentPublish(vo, 3)) {
            tags.add("近期发布");
        }

        if (tags.isEmpty()) {
            return "综合评分推荐";
        }
        if (tags.size() == 1) {
            return tags.get(0);
        }
        return tags.get(0) + " · " + tags.get(1);
    }

    private boolean isGoodCondition(String condition) {
        return "NEW".equals(condition) || "LIKE_NEW".equals(condition);
    }

    private boolean isRecentPublish(SecondhandGoodsVO vo, long days) {
        if (vo == null || vo.getCreateTime() == null) {
            return false;
        }
        return ChronoUnit.DAYS.between(vo.getCreateTime(), LocalDateTime.now()) <= days;
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        return text != null && keyword != null && text.toLowerCase().contains(keyword.toLowerCase());
    }

    @Override
    public SecondhandGoodsVO getDetail(Long id) {
        // 查询详情
        SecondhandGoodsVO vo = secondhandGoodsMapper.findDetailById(id);
        if (vo == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        // 增加浏览次数
        secondhandGoodsMapper.incrementViewCount(id);

        // 转换images字段
        vo.setImageList(parseImages(vo.getImages()));

        return vo;
    }

    @Override
    public PageResult<SecondhandGoodsVO> getMyList(Long sellerId, Integer page, Integer size, String status) {
        int safePage = PaginationUtils.safePage(page);
        int safeSize = PaginationUtils.safeSize(size);
        int offset = PaginationUtils.offset(safePage, safeSize);

        List<SecondhandGoodsVO> records = secondhandGoodsMapper.selectMyList(sellerId, status, offset, safeSize);
        Long total = secondhandGoodsMapper.countMy(sellerId, status);

        // 调试日志 - 检查viewCount
        for (SecondhandGoodsVO vo : records) {
            logger.debug("商品ID: {}, 标题: {}, viewCount: {}", vo.getId(), vo.getTitle(), vo.getViewCount());
            vo.setImageList(parseImages(vo.getImages()));
        }

        return new PageResult<>(records, total, safePage, safeSize);
    }

    @Override
    @Transactional
    public boolean delete(Long id, Long sellerId) {
        // 查询商品
        SecondhandGoods goods = secondhandGoodsMapper.findById(id);
        if (goods == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        // 验证是否是发布者
        if (!goods.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("无权限操作");
        }

        return secondhandGoodsMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean update(Long id, Long sellerId, SecondhandPublishDTO publishDTO) {
        // 查询商品
        SecondhandGoods goods = secondhandGoodsMapper.findById(id);
        if (goods == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        // 验证是否是发布者
        if (!goods.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("无权限操作");
        }

        // 处理图片URL列表
        String imagesJson = null;
        if (publishDTO.getImages() != null && !publishDTO.getImages().isEmpty()) {
            try {
                imagesJson = objectMapper.writeValueAsString(publishDTO.getImages());
            } catch (Exception e) {
                throw new RuntimeException("图片数据处理失败", e);
            }
        }

        // 更新商品信息
        goods.setTitle(publishDTO.getTitle());
        goods.setCategory(publishDTO.getCategory());
        goods.setPrice(publishDTO.getPrice());
        goods.setCondition(publishDTO.getCondition());
        goods.setDescription(publishDTO.getDescription());
        goods.setImages(imagesJson);

        return secondhandGoodsMapper.update(goods) > 0;
    }

    @Override
    @Transactional
    public boolean markAsSold(Long id, Long sellerId) {
        // 查询商品
        SecondhandGoods goods = secondhandGoodsMapper.findById(id);
        if (goods == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        // 验证是否是发布者
        if (!goods.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("无权限操作");
        }

        // 验证商品状态
        if (GoodsStatus.SOLD.equals(goods.getStatus())) {
            throw new IllegalArgumentException("商品已标记为已售");
        }

        // 更新状态为已售出
        secondhandGoodsMapper.updateStatus(id, GoodsStatus.SOLD.name());

        // 向notification表插入系统消息
        Notification notification = new Notification();
        notification.setUserId(goods.getSellerId());
        notification.setType("SECONDHAND");
        notification.setTitle("商品已售出");
        notification.setContent("您发布的商品【" + goods.getTitle() + "】已标记为已售出");
        notification.setRelatedId(id);
        notification.setRelatedType("SECONDHAND");
        notification.setCreateTime(LocalDateTime.now());
        notificationService.saveNotification(notification);

        return true;
    }

    @Override
    @Transactional
    public boolean relist(Long id, Long sellerId) {
        // 查询商品
        SecondhandGoods goods = secondhandGoodsMapper.findById(id);
        if (goods == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        // 验证是否是发布者
        if (!goods.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("无权限操作");
        }

        // 验证商品状态（只有已售出或已下架的商品可以重新上架）
        if (GoodsStatus.APPROVED.equals(goods.getStatus())) {
            throw new IllegalArgumentException("商品已在售中，无需重新上架");
        }

        // 已售出的商品重新上架，直接变为已上架（已审核过）
        // 已下架的商品重新上架，需要重新审核（变为待审核）
        if (GoodsStatus.SOLD.equals(goods.getStatus())) {
            secondhandGoodsMapper.updateStatus(id, GoodsStatus.APPROVED.name());
            secondhandInnovationService.notifySubscribersForGoods(goods);
        } else if (GoodsStatus.REMOVED.equals(goods.getStatus())) {
            secondhandGoodsMapper.updateStatus(id, GoodsStatus.PENDING.name());
            secondhandGoodsMapper.updateRemoveReason(id, null);
        } else if (GoodsStatus.REJECTED.equals(goods.getStatus())) {
            secondhandGoodsMapper.updateStatus(id, GoodsStatus.PENDING.name());
            secondhandGoodsMapper.updateRejectReason(id, null);
        }

        return true;
    }

    @Override
    @Transactional
    public boolean userRemove(Long id, Long sellerId, String reason) {
        SecondhandGoods goods = secondhandGoodsMapper.findById(id);
        if (goods == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        if (!goods.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("无权限操作");
        }

        String[] allowedStatuses = {GoodsStatus.APPROVED.name(), GoodsStatus.PENDING.name()};
        int updated = secondhandGoodsMapper.updateStatusWithCondition(id, GoodsStatus.REMOVED.name(), allowedStatuses);
        if (updated == 0) {
            throw new IllegalStateException("该商品状态不允许下架，请刷新后重试");
        }

        secondhandGoodsMapper.updateRemoveReason(id, reason);

        Notification notification = new Notification();
    notification.setUserId(goods.getSellerId());
        notification.setType("SECONDHAND");
        notification.setTitle("商品已下架");
        notification.setContent("您发布的商品【" + goods.getTitle() + "】已下架" + (reason != null && !reason.isEmpty() ? "，原因：" + reason : ""));
        notification.setRelatedId(id);
        notification.setRelatedType("SECONDHAND");
        notification.setCreateTime(LocalDateTime.now());
        notificationService.saveNotification(notification);

        return true;
    }

    /**
     * 解析JSON图片字符串为List
     */
    private List<String> parseImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(imagesJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public boolean favorite(Long userId, Long goodsId) {
        // 检查商品是否存在
        SecondhandGoods goods = secondhandGoodsMapper.findById(goodsId);
        if (goods == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        // 检查是否已收藏
        if (goodsFavoriteMapper.existsByUserIdAndGoodsId(userId, goodsId)) {
            throw new IllegalArgumentException("您已收藏过该商品");
        }

        // 创建收藏记录
        GoodsFavorite favorite = new GoodsFavorite();
        favorite.setUserId(userId);
        favorite.setGoodsId(goodsId);
        favorite.setCreateTime(LocalDateTime.now());

        return goodsFavoriteMapper.insert(favorite) > 0;
    }

    @Override
    @Transactional
    public boolean unfavorite(Long userId, Long goodsId) {
        // 检查是否已收藏
        if (!goodsFavoriteMapper.existsByUserIdAndGoodsId(userId, goodsId)) {
            throw new IllegalArgumentException("您还未收藏该商品");
        }

        return goodsFavoriteMapper.deleteByUserIdAndGoodsId(userId, goodsId) > 0;
    }

    @Override
    public PageResult<SecondhandGoodsVO> getMyFavorites(Long userId, Integer page, Integer size) {
        int safePage = PaginationUtils.safePage(page);
        int safeSize = PaginationUtils.safeSize(size);
        int offset = PaginationUtils.offset(safePage, safeSize);

        List<SecondhandGoodsVO> records = goodsFavoriteMapper.selectFavoritesByUserId(userId, offset, safeSize);
        Long total = goodsFavoriteMapper.countByUserId(userId);

        // 转换images字段
        for (SecondhandGoodsVO vo : records) {
            vo.setImageList(parseImages(vo.getImages()));
        }

        return new PageResult<>(records, total, safePage, safeSize);
    }

    @Override
    public boolean isFavorited(Long userId, Long goodsId) {
        return goodsFavoriteMapper.existsByUserIdAndGoodsId(userId, goodsId);
    }

    @Override
    public PageResult<SecondhandGoodsVO> getPendingList(Integer page, Integer size, String category, String keyword) {
        int safePage = PaginationUtils.safePage(page);
        int safeSize = PaginationUtils.safeSize(size);
        int offset = PaginationUtils.offset(safePage, safeSize);

        List<SecondhandGoodsVO> records = secondhandGoodsMapper.selectPendingList(category, keyword, offset, safeSize);
        Long total = secondhandGoodsMapper.countPending(category, keyword);
        for (SecondhandGoodsVO vo : records) {
            vo.setImageList(parseImages(vo.getImages()));
        }
        return new PageResult<>(records, total, safePage, safeSize);
    }

    @Override
    public PageResult<SecondhandGoodsVO> getAllList(Integer page, Integer size, String category, String keyword, String status) {
        int safePage = PaginationUtils.safePage(page);
        int safeSize = PaginationUtils.safeSize(size);
        int offset = PaginationUtils.offset(safePage, safeSize);

        List<SecondhandGoodsVO> records = secondhandGoodsMapper.selectAllList(category, keyword, status, offset, safeSize);
        Long total = secondhandGoodsMapper.countAll(category, keyword, status);
        for (SecondhandGoodsVO vo : records) {
            vo.setImageList(parseImages(vo.getImages()));
        }
        return new PageResult<>(records, total, safePage, safeSize);
    }

    @Override
    @Transactional
    public boolean approve(Long id, Long adminId) {
        SecondhandGoods goods = secondhandGoodsMapper.findById(id);
        if (goods == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (!GoodsStatus.PENDING.equals(goods.getStatus())) {
            throw new IllegalArgumentException("只有待审核商品可以审核");
        }
        secondhandGoodsMapper.updateStatus(id, GoodsStatus.APPROVED.name());
        secondhandGoodsMapper.updateHandler(id, adminId);
        secondhandGoodsMapper.updateHandleTime(id);
        secondhandInnovationService.notifySubscribersForGoods(goods);

        Notification notification = new Notification();
    notification.setUserId(goods.getSellerId());
        notification.setType("SECONDHAND");
        notification.setTitle("商品审核通过");
        notification.setContent("您发布的商品【" + goods.getTitle() + "】已审核通过并上架");
        notification.setRelatedId(id);
        notification.setRelatedType("SECONDHAND");
        notification.setCreateTime(LocalDateTime.now());
        notificationService.saveNotification(notification);
        return true;
    }

    @Override
    @Transactional
    public boolean reject(Long id, Long adminId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("请填写拒绝原因");
        }
        SecondhandGoods goods = secondhandGoodsMapper.findById(id);
        if (goods == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (!GoodsStatus.PENDING.equals(goods.getStatus())) {
            throw new IllegalArgumentException("只有待审核商品可以拒绝");
        }
        secondhandGoodsMapper.updateStatus(id, GoodsStatus.REJECTED.name());
        secondhandGoodsMapper.updateRejectReason(id, reason);
        secondhandGoodsMapper.updateHandler(id, adminId);
        secondhandGoodsMapper.updateHandleTime(id);

        Notification notification = new Notification();
    notification.setUserId(goods.getSellerId());
        notification.setType("SECONDHAND");
        notification.setTitle("商品审核被拒绝");
        notification.setContent("您发布的商品【" + goods.getTitle() + "】未通过审核，原因：" + reason);
        notification.setRelatedId(id);
        notification.setRelatedType("SECONDHAND");
        notification.setCreateTime(LocalDateTime.now());
        notificationService.saveNotification(notification);
        return true;
    }

    @Override
    @Transactional
    public boolean remove(Long id, Long adminId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("请填写下架原因");
        }
        SecondhandGoods goods = secondhandGoodsMapper.findById(id);
        if (goods == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        String[] allowedStatuses = {GoodsStatus.APPROVED.name(), GoodsStatus.PENDING.name()};
        int updated = secondhandGoodsMapper.updateStatusWithCondition(id, GoodsStatus.REMOVED.name(), allowedStatuses);
        if (updated == 0) {
            throw new IllegalStateException("该商品状态不允许下架，请刷新后重试");
        }

        secondhandGoodsMapper.updateRemoveReason(id, reason);
        secondhandGoodsMapper.updateHandler(id, adminId);
        secondhandGoodsMapper.updateHandleTime(id);

        Notification notification = new Notification();
    notification.setUserId(goods.getSellerId());
        notification.setType("SECONDHAND");
        notification.setTitle("商品已下架");
        notification.setContent("您发布的商品【" + goods.getTitle() + "】已被管理员下架，原因：" + reason);
        notification.setRelatedId(id);
        notification.setRelatedType("SECONDHAND");
        notification.setCreateTime(LocalDateTime.now());
        notificationService.saveNotification(notification);
        return true;
    }

    @Override
    @Transactional
    public boolean adminDelete(Long id, Long adminId, String reason) {
        SecondhandGoods goods = secondhandGoodsMapper.findById(id);
        if (goods == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        int deleted = secondhandGoodsMapper.softDelete(id, adminId, reason);
        if (deleted == 0) {
            throw new IllegalStateException("删除失败，商品可能已被删除，请刷新后重试");
        }

        Notification notification = new Notification();
    notification.setUserId(goods.getSellerId());
        notification.setType("SECONDHAND");
        notification.setTitle("商品已删除");
        notification.setContent("您发布的商品【" + goods.getTitle() + "】已被管理员删除" + (reason != null && !reason.isEmpty() ? "，原因：" + reason : ""));
        notification.setRelatedId(id);
        notification.setRelatedType("SECONDHAND");
        notification.setCreateTime(LocalDateTime.now());
        notificationService.saveNotification(notification);

        return true;
    }

    @Override
    @Transactional
    public boolean updateByUser(Long id, Long userId, SecondhandPublishDTO publishDTO) {
        SecondhandGoods goods = secondhandGoodsMapper.findById(id);
        if (goods == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (!goods.getSellerId().equals(userId)) {
            throw new IllegalArgumentException("无权限操作");
        }
        if (!GoodsStatus.REJECTED.equals(goods.getStatus())) {
            throw new IllegalArgumentException("只能修改被拒绝的商品");
        }

        String imagesJson = null;
        if (publishDTO.getImages() != null && !publishDTO.getImages().isEmpty()) {
            try {
                imagesJson = objectMapper.writeValueAsString(publishDTO.getImages());
            } catch (Exception e) {
                throw new RuntimeException("图片数据处理失败", e);
            }
        }

        goods.setTitle(publishDTO.getTitle());
        goods.setCategory(publishDTO.getCategory());
        goods.setPrice(publishDTO.getPrice());
        goods.setCondition(publishDTO.getCondition());
        goods.setDescription(publishDTO.getDescription());
        goods.setImages(imagesJson);
        goods.setStatus(GoodsStatus.PENDING);

        secondhandGoodsMapper.updateRejectReason(id, null);
        secondhandGoodsMapper.update(goods);
        return true;
    }
}
