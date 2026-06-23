package com.caspar.service.impl;

import com.caspar.entity.LostFound;
import com.caspar.entity.dto.LostFoundClaimVO;
import com.caspar.entity.dto.LostFoundPublishDTO;
import com.caspar.mapper.LostFoundMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LostFoundServiceImplTest {

    private final LostFoundMapper mapper = mock(LostFoundMapper.class);
    private final LostFoundServiceImpl service = new LostFoundServiceImpl(mapper, new ObjectMapper());

    @Test
    void publish_shouldPersistWorkflowFields() {
        LostFoundPublishDTO dto = new LostFoundPublishDTO();
        dto.setType("lost");
        dto.setTitle(" 黑色雨伞 ");
        dto.setCategory("other");
        dto.setLocation(" 西区教学楼 ");
        dto.setLostTime(LocalDateTime.of(2026, 6, 15, 18, 30));
        dto.setDescription(" 一把黑色长柄伞 ");
        dto.setImages(List.of("/images/umbrella.jpg"));
        dto.setContact(" wx-test ");

        when(mapper.insert(any(LostFound.class))).thenAnswer(invocation -> {
            LostFound value = invocation.getArgument(0);
            value.setId(42L);
            return 1;
        });

        assertEquals(42L, service.publish(7L, dto));

        ArgumentCaptor<LostFound> captor = ArgumentCaptor.forClass(LostFound.class);
        verify(mapper).insert(captor.capture());
        LostFound saved = captor.getValue();
        assertEquals("LOST", saved.getType());
        assertEquals("OTHER", saved.getCategory());
        assertEquals("黑色雨伞", saved.getTitle());
        assertEquals("西区教学楼", saved.getLocation());
        assertEquals("wx-test", saved.getContact());
        assertEquals("OPEN", saved.getStatus());
    }

    @Test
    void getDetail_shouldRejectMissingPost() {
        when(mapper.findDetailById(99L)).thenReturn(null);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.getDetail(99L)
        );

        assertEquals("帖子不存在", error.getMessage());
    }

    @Test
    void claim_shouldRejectOwnPost() {
        LostFound post = post(5L, 7L, "OPEN");
        when(mapper.findById(5L)).thenReturn(post);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.claim(5L, 7L, "这是我的物品")
        );

        assertEquals("不能认领自己发布的信息", error.getMessage());
    }

    @Test
    void approveClaim_shouldResolvePostAndRejectOtherPendingClaims() {
        LostFound post = post(5L, 7L, "OPEN");
        LostFoundClaimVO claim = new LostFoundClaimVO();
        claim.setId(11L);
        claim.setLostfoundId(5L);
        claim.setStatus("PENDING");

        when(mapper.findById(5L)).thenReturn(post);
        when(mapper.findClaimById(11L)).thenReturn(claim);
        when(mapper.updatePendingClaimStatus(11L, 5L, "APPROVED")).thenReturn(1);
        when(mapper.updateStatusIfCurrent(5L, "RESOLVED", "OPEN")).thenReturn(1);

        service.reviewClaim(5L, 11L, 7L, true);

        verify(mapper).rejectOtherPendingClaims(5L, 11L);
        verify(mapper).updateStatusIfCurrent(5L, "RESOLVED", "OPEN");
    }

    @Test
    void delete_shouldRemoveClaimsBeforePost() {
        when(mapper.findById(5L)).thenReturn(post(5L, 7L, "OPEN"));
        when(mapper.deleteById(5L)).thenReturn(1);

        service.delete(5L, 7L);

        InOrder order = inOrder(mapper);
        order.verify(mapper).deleteClaimsByLostFoundId(5L);
        order.verify(mapper).deleteById(5L);
    }

    private LostFound post(Long id, Long publisherId, String status) {
        LostFound post = new LostFound();
        post.setId(id);
        post.setPublisherId(publisherId);
        post.setStatus(status);
        return post;
    }
}
