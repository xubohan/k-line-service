package com.example.kline.modules.kline.domain.repository;

import com.example.kline.modules.kline.domain.entity.KlineResponse;

/**
 * Repository for k-line data.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-08 20:24:08
 */
public interface KlineRepository {
    /**
     * Find k-line range.
     *
     * @param stockcode stock code
     * @param marketId  market id
     * @param startTs   start timestamp
     * @param endTs     end timestamp
     * @param limit     limit of points
     * @return response
     */
    KlineResponse findRange(String stockcode, String marketId, Long startTs, Long endTs, Integer limit);

    /**
     * Persist k-line batch.
     *
     * @param response response data
     */
    void upsertBatch(KlineResponse response);
}
