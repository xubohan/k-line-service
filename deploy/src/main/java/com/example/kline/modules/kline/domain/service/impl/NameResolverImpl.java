package com.example.kline.modules.kline.domain.service.impl;

import com.example.kline.modules.kline.domain.service.NameResolver;
import com.example.kline.modules.kline.infrastructure.cache.RedisNameCache;
import com.example.kline.modules.kline.infrastructure.external.NameServiceHttp;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * Name resolver implementation with cache.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-08 20:24:08
 */
@Service
public class NameResolverImpl implements NameResolver {
    private final RedisNameCache nameCache;
    private final NameServiceHttp nameServiceHttp;

    @Autowired
    public NameResolverImpl(RedisNameCache nameCache, NameServiceHttp nameServiceHttp) {
        this.nameCache = nameCache;
        this.nameServiceHttp = nameServiceHttp;
    }

    @Override
    public String resolve(String stockcode, String marketId) {
        String cached = nameCache.getName(stockcode, marketId);
        if (StringUtils.isNotBlank(cached)) {
            return cached;
        }
        String name = nameServiceHttp.fetchName(stockcode, marketId);
        if (StringUtils.isNotBlank(name)) {
            nameCache.setName(stockcode, marketId, name, 3600);
        }
        return name;
    }
}
