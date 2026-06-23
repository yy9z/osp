package com.caspar.config;

import com.caspar.entity.dto.LostFoundVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CacheConfigTest {

    @Test
    @SuppressWarnings("unchecked")
    void cacheSerializer_shouldPreserveValueType() {
        RedisCacheConfiguration configuration =
                new CacheConfig().redisCacheConfiguration(new ObjectMapper().findAndRegisterModules());
        RedisSerializationContext.SerializationPair<Object> pair =
                (RedisSerializationContext.SerializationPair<Object>) configuration.getValueSerializationPair();

        LostFoundVO value = new LostFoundVO();
        value.setId(9L);
        value.setTitle("测试失物");

        byte[] bytes = pair.write(value).array();
        Object restored = pair.read(java.nio.ByteBuffer.wrap(bytes));

        LostFoundVO restoredValue = assertInstanceOf(LostFoundVO.class, restored);
        assertEquals(9L, restoredValue.getId());
        assertEquals("测试失物", restoredValue.getTitle());
    }
}
