package com.caspar.mapper;

import com.caspar.entity.SecondhandSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface SecondhandSubscriptionMapper {

    int insert(SecondhandSubscription subscription);

    List<SecondhandSubscription> selectByUserId(@Param("userId") Long userId);

    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int updateLastNotifiedAt(@Param("id") Long id);

    int countSameActiveRule(@Param("userId") Long userId,
                            @Param("keyword") String keyword,
                            @Param("category") String category,
                            @Param("maxPrice") BigDecimal maxPrice,
                            @Param("campus") String campus);

    List<SecondhandSubscription> selectMatchedSubscriptions(@Param("sellerId") Long sellerId,
                                                            @Param("goodsCategory") String goodsCategory,
                                                            @Param("goodsPrice") BigDecimal goodsPrice,
                                                            @Param("sellerCampus") String sellerCampus,
                                                            @Param("goodsTitle") String goodsTitle,
                                                            @Param("goodsDescription") String goodsDescription);
}

