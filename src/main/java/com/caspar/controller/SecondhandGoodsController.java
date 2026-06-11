package com.caspar.controller;

import com.caspar.common.PageResult;
import com.caspar.common.Result;
import com.caspar.entity.dto.SecondhandBargainTemplateVO;
import com.caspar.entity.dto.SecondhandGoodsVO;
import com.caspar.entity.dto.SecondhandMeetupRecommendationVO;
import com.caspar.entity.dto.SecondhandPublishDTO;
import com.caspar.entity.dto.SecondhandSubscriptionCreateDTO;
import com.caspar.entity.dto.SecondhandSubscriptionVO;
import com.caspar.entity.dto.SellerTrustScoreVO;
import com.caspar.service.SecondhandGoodsService;
import com.caspar.service.SecondhandInnovationService;
import com.caspar.util.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 二手商品控制器
 */
@RestController
@RequestMapping("/api/secondhand")
@Slf4j
public class SecondhandGoodsController {

    @Autowired
    private SecondhandGoodsService secondhandGoodsService;

    @Autowired
    private SecondhandInnovationService secondhandInnovationService;

    /**
     * 发布商品
     * 需要认证
     */
    @PostMapping("/publish")
    public Result<Long> publish(@Valid @RequestBody SecondhandPublishDTO publishDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            Long goodsId = secondhandGoodsService.publish(userId, publishDTO);
            return Result.success("发布成功", goodsId);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("发布商品失败, userId={}", userId, e);
            return Result.error("发布失败");
        }
    }

    /**
     * 商品列表
     * 支持分页、分类、关键词搜索、排序
     */
    @GetMapping("/list")
    public Result<PageResult<SecondhandGoodsVO>> getList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "smart") String sort) {

        try {
            PageResult<SecondhandGoodsVO> pageResult = secondhandGoodsService.getList(page, size, category, keyword, sort);
            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("获取二手列表失败, page={}, size={}, category={}, keyword={}, sort={}",
                    page, size, category, keyword, sort, e);
            return Result.error("获取列表失败");
        }
    }

    /**
     * 商品详情
     */
    @GetMapping("/{id}")
    public Result<SecondhandGoodsVO> getDetail(@PathVariable Long id) {
        try {
            SecondhandGoodsVO goodsVO = secondhandGoodsService.getDetail(id);
            return Result.success(goodsVO);
        } catch (IllegalArgumentException e) {
            return Result.notFound(e.getMessage());
        } catch (Exception e) {
            log.error("获取二手详情失败, id={}", id, e);
            return Result.error("获取详情失败");
        }
    }

    /**
     * 求购订阅：创建。
     */
    @PostMapping("/subscriptions")
    public Result<Long> createSubscription(@Valid @RequestBody SecondhandSubscriptionCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }
        try {
            Long id = secondhandInnovationService.createSubscription(userId, dto);
            return Result.success("订阅创建成功", id);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("创建订阅失败, userId={}", userId, e);
            return Result.error("创建订阅失败");
        }
    }

    /**
     * 求购订阅：列表。
     */
    @GetMapping("/subscriptions")
    public Result<List<SecondhandSubscriptionVO>> getSubscriptions() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }
        try {
            return Result.success(secondhandInnovationService.listSubscriptions(userId));
        } catch (Exception e) {
            log.error("获取订阅列表失败, userId={}", userId, e);
            return Result.error("获取订阅列表失败");
        }
    }

    /**
     * 求购订阅：删除。
     */
    @DeleteMapping("/subscriptions/{id}")
    public Result<Void> deleteSubscription(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }
        try {
            boolean deleted = secondhandInnovationService.deleteSubscription(userId, id);
            if (!deleted) {
                return Result.notFound("订阅不存在或无权限删除");
            }
            return Result.success("删除成功", null);
        } catch (Exception e) {
            log.error("删除订阅失败, userId={}, subscriptionId={}", userId, id, e);
            return Result.error("删除订阅失败");
        }
    }

    /**
     * 校区面交智能匹配建议。
     */
    @GetMapping("/{id}/meetup-recommendation")
    public Result<SecondhandMeetupRecommendationVO> getMeetupRecommendation(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }
        try {
            return Result.success(secondhandInnovationService.getMeetupRecommendation(userId, id));
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("获取面交建议失败, userId={}, goodsId={}", userId, id, e);
            return Result.error("获取面交建议失败");
        }
    }

    /**
     * 一键议价模板。
     */
    @GetMapping("/{id}/bargain-template")
    public Result<SecondhandBargainTemplateVO> getBargainTemplate(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal targetPrice) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }
        try {
            return Result.success(secondhandInnovationService.getBargainTemplate(userId, id, targetPrice));
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("获取议价模板失败, userId={}, goodsId={}", userId, id, e);
            return Result.error("获取议价模板失败");
        }
    }

    /**
     * 卖家可信度评分。
     */
    @GetMapping("/seller/{sellerId}/trust")
    public Result<SellerTrustScoreVO> getSellerTrustScore(@PathVariable Long sellerId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }
        try {
            return Result.success(secondhandInnovationService.getSellerTrustScore(sellerId));
        } catch (Exception e) {
            log.error("获取卖家可信度失败, sellerId={}", sellerId, e);
            return Result.error("获取卖家可信度失败");
        }
    }

    /**
     * 我的发布
     * 需要认证
     */
    @GetMapping("/my")
    public Result<PageResult<SecondhandGoodsVO>> getMyList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status) {

        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            PageResult<SecondhandGoodsVO> pageResult = secondhandGoodsService.getMyList(userId, page, size, status);
            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("获取我的发布失败, userId={}, page={}, size={}, status={}", userId, page, size, status, e);
            return Result.error("获取我的发布失败");
        }
    }

    /**
     * 下架商品
     * 需要认证，仅发布者可以操作
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            boolean success = secondhandGoodsService.delete(id, userId);
            if (success) {
                return Result.success("下架成功", null);
            } else {
                return Result.error("下架失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("下架商品失败, userId={}, goodsId={}", userId, id, e);
            return Result.error("下架失败");
        }
    }

    /**
     * 更新商品
     * 需要认证，仅发布者可以操作
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SecondhandPublishDTO publishDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            boolean success = secondhandGoodsService.update(id, userId, publishDTO);
            if (success) {
                return Result.success("更新成功", null);
            } else {
                return Result.error("更新失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("更新失败", e);
            return Result.error("更新失败");
        }
    }

    /**
     * 标记已售
     * 需要认证，仅发布者可以操作
     */
    @PutMapping("/{id}/sold")
    public Result<Void> markAsSold(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            boolean success = secondhandGoodsService.markAsSold(id, userId);
            if (success) {
                return Result.success("标记成功", null);
            } else {
                return Result.error("标记失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("标记失败", e);
            return Result.error("标记失败");
        }
    }

    /**
     * 重新上架
     * 需要认证，仅发布者可以操作
     */
    @PutMapping("/{id}/relist")
    public Result<Void> relist(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            boolean success = secondhandGoodsService.relist(id, userId);
            if (success) {
                return Result.success("重新上架成功", null);
            } else {
                return Result.error("重新上架失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("重新上架失败", e);
            return Result.error("重新上架失败");
        }
    }

    /**
     * 下架自己的商品
     * 需要认证，仅发布者可以操作
     */
    @PutMapping("/{id}/remove")
    public Result<Void> userRemove(@PathVariable Long id, @Valid @RequestBody RemoveRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            boolean success = secondhandGoodsService.userRemove(id, userId, request.getReason());
            if (success) {
                return Result.success("下架成功", null);
            } else {
                return Result.error("下架失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error("操作失败");
        } catch (Exception e) {
            log.error("下架失败", e);
            return Result.error("下架失败");
        }
    }

    /**
     * 收藏商品
     * 需要认证
     */
    @PostMapping("/{id}/favorite")
    public Result<Void> favorite(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            boolean success = secondhandGoodsService.favorite(userId, id);
            if (success) {
                return Result.success("收藏成功", null);
            } else {
                return Result.error("收藏失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("收藏失败", e);
            return Result.error("收藏失败");
        }
    }

    /**
     * 取消收藏
     * 需要认证
     */
    @DeleteMapping("/{id}/favorite")
    public Result<Void> unfavorite(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            boolean success = secondhandGoodsService.unfavorite(userId, id);
            if (success) {
                return Result.success("取消收藏成功", null);
            } else {
                return Result.error("取消收藏失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("取消收藏失败", e);
            return Result.error("取消收藏失败");
        }
    }

    /**
     * 获取我的收藏列表
     * 需要认证
     */
    @GetMapping("/favorites")
    public Result<PageResult<SecondhandGoodsVO>> getMyFavorites(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            PageResult<SecondhandGoodsVO> pageResult = secondhandGoodsService.getMyFavorites(userId, page, size);
            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("获取收藏列表失败", e);
            return Result.error("获取收藏列表失败");
        }
    }

    /**
     * 检查商品是否已收藏
     * 未登录返回false，不报错
     */
    @GetMapping("/{id}/favorite/status")
    public Result<Boolean> getFavoriteStatus(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            // 未登录用户返回false，不报错
            return Result.success(false);
        }

        try {
            boolean favorited = secondhandGoodsService.isFavorited(userId, id);
            return Result.success(favorited);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("controller={} error={}", getClass().getSimpleName(), e.getMessage(), e);
            // 出错时也返回false，不影响用户体验
            log.warn("检查收藏状态失败, userId={}, goodsId={}", userId, id, e);
            return Result.success(false);
        }
    }

    // Refactor: 用 @Data 替代手写 getter/setter，项目已引入 Lombok
    @Data
    public static class RemoveRequest {
        @NotBlank(message = "下架原因不能为空")
        @Size(max = 200, message = "下架原因长度不能超过200个字符")
        private String reason;
    }
}
