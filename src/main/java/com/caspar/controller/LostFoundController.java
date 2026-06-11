package com.caspar.controller;

import com.caspar.common.PageResult;
import com.caspar.common.Result;
import com.caspar.entity.LostFoundClaim;
import com.caspar.entity.dto.LostFoundMatchVO;
import com.caspar.entity.dto.LostFoundClaimDTO;
import com.caspar.entity.dto.LostFoundPublishDTO;
import com.caspar.entity.dto.LostFoundVO;
import com.caspar.service.LostFoundService;
import com.caspar.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * 失物招领控制器
 */
@RestController
@RequestMapping("/api/lostfound")
@Slf4j
public class LostFoundController {

    @Autowired
    private LostFoundService lostFoundService;

    /**
     * 发布失物/招领信息
     * 需要认证
     */
    @PostMapping("/publish")
    public Result<Long> publish(@Valid @RequestBody LostFoundPublishDTO publishDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        // Refactor: 基础入参校验已在 Service 层执行，全局异常处理器主动返回 badRequest
        try {
            Long id = lostFoundService.publish(userId, publishDTO);
            return Result.success("发布成功", id);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("发布失物招领失败, userId={}", userId, e);
            return Result.error("发布失败");
        }
    }

    /**
     * 列表
     * 支持分页、类型、分类、关键词搜索
     */
    @GetMapping("/list")
    public Result<PageResult<LostFoundVO>> getList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {

        try {
            PageResult<LostFoundVO> pageResult = lostFoundService.getList(page, size, type, category, keyword);
            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("获取失物招领列表失败, page={}, size={}, type={}, category={}, keyword={}",
                    page, size, type, category, keyword, e);
            return Result.error("获取列表失败");
        }
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public Result<LostFoundVO> getDetail(@PathVariable Long id) {
        try {
            LostFoundVO vo = lostFoundService.getDetail(id);
            return Result.success(vo);
        } catch (IllegalArgumentException e) {
            return Result.notFound(e.getMessage());
        } catch (Exception e) {
            log.error("获取失物招领详情失败, id={}", id, e);
            return Result.error("获取详情失败");
        }
    }

    /**
     * 认领
     * 需要认证
     */
    @PostMapping("/{id}/claim")
    public Result<Void> claim(@PathVariable Long id, @Valid @RequestBody LostFoundClaimDTO claimDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            boolean success = lostFoundService.claim(id, userId, claimDTO.getMessage());
            if (success) {
                return Result.success("认领申请已提交", null);
            } else {
                return Result.error("认领失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("认领失败, id={}, userId={}", id, userId, e);
            return Result.error("认领失败");
        }
    }

    /**
     * 获取认领记录
     * 需要认证，仅发布者可以查看
     */
    @GetMapping("/{id}/claims")
    public Result<List<LostFoundClaim>> getClaims(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            // 验证是否是发布者
            LostFoundVO vo = lostFoundService.getDetail(id);
            if (!vo.getPublisherId().equals(userId)) {
                return Result.forbidden();
            }

            List<LostFoundClaim> claims = lostFoundService.getClaims(id);
            return Result.success(claims);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("获取认领记录失败, id={}, userId={}", id, userId, e);
            return Result.error("获取认领记录失败");
        }
    }

    /**
     * 我的发布
     * 需要认证
     */
    @GetMapping("/my")
    public Result<PageResult<LostFoundVO>> getMyList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String type) {

        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            PageResult<LostFoundVO> pageResult = lostFoundService.getMyList(userId, page, size, type);
            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("获取我的失物招领失败, userId={}, page={}, size={}, type={}", userId, page, size, type, e);
            return Result.error("获取我的发布失败");
        }
    }

    /**
     * 删除
     * 需要认证，仅发布者可以操作
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            boolean success = lostFoundService.delete(id, userId);
            if (success) {
                return Result.success("删除成功", null);
            } else {
                return Result.error("删除失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("删除失物招领失败, id={}, userId={}", id, userId, e);
            return Result.error("删除失败");
        }
    }

    /**
     * 标记已解决
     * 需要认证，仅发布者可以操作
     */
    @RequestMapping(value = "/{id}/resolve", method = {RequestMethod.POST, RequestMethod.PUT})
    public Result<Void> resolve(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            boolean success = lostFoundService.resolve(id, userId);
            if (success) {
                return Result.success("标记成功", null);
            } else {
                return Result.error("标记失败");
            }
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("标记失物招领已解决失败, id={}, userId={}", id, userId, e);
            return Result.error("标记失败");
        }
    }

    /**
     * 智能匹配
     * LOST 自动匹配 FOUND，FOUND 自动匹配 LOST
     */
    @GetMapping("/{id}/matches")
    public Result<List<LostFoundMatchVO>> getSmartMatches(
            @PathVariable Long id,
            @RequestParam(defaultValue = "5") Integer limit) {
        try {
            List<LostFoundMatchVO> matches = lostFoundService.getSmartMatches(id, limit);
            return Result.success(matches);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("获取失物招领智能匹配失败, id={}, limit={}", id, limit, e);
            return Result.error("获取智能匹配失败");
        }
    }

}
