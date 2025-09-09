package com.example.kline.interfaces.rest;

import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.entity.PricePoint;
import com.example.kline.modules.kline.domain.repository.KlineRepository;
import com.example.kline.modules.kline.domain.service.NameResolver;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc tests for ApiController.
 * Tests API contract and validation behavior with mocked dependencies.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-09 22:30:00
 */
@WebMvcTest(controllers = ApiController.class)
@Import(GlobalExceptionHandler.class)
public class ApiControllerMockMvcTest {

    @Autowired private MockMvc mvc;

    @MockBean private KlineRepository klineRepository;
    @MockBean private NameResolver nameResolver;

    @Test
    public void getKline_returnsContractJson() throws Exception {
        String stock = "300033";
        String market = "33";
        KlineResponse resp = new KlineResponse();
        resp.setStockcode(stock);
        resp.setMarketId(market);
        PricePoint p = new PricePoint();
        p.setTs(1577862000L); // 2020-01-01 09:00:00 UTC
        p.setOpen(new BigDecimal("1"));
        p.setHigh(new BigDecimal("1"));
        p.setLow(new BigDecimal("1"));
        p.setClose(new BigDecimal("1"));
        p.setVol(1L);
        resp.addPricePoint(p);

        when(klineRepository.findRange(eq(stock), eq(market), isNull(), isNull(), isNull()))
            .thenReturn(resp);
        when(nameResolver.resolve(stock, market)).thenReturn("wuhan city");

        mvc.perform(get("/kline")
                .param("stockcode", stock)
                .param("marketId", market)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.stockName").value("wuhan city"))
            .andExpect(jsonPath("$.list").isArray())
            .andExpect(jsonPath("$.list[0].date").exists())
            .andExpect(jsonPath("$.list[0].time").exists());
    }

    @Test
    public void getKline_missingParam_returns400() throws Exception {
        mvc.perform(get("/kline").param("stockcode", "300033"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").exists());
    }

    @Test
    public void getKline_invalidLimit_returns400() throws Exception {
        mvc.perform(get("/kline")
                .param("stockcode", "300033")
                .param("marketId", "33")
                .param("limit", "abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").exists());
    }

    @Test
    public void getKline_nullTsMappedToEpochZero_andSorted() throws Exception {
        String stock = "TSTNULL";
        String market = "SZ";
        KlineResponse resp = new KlineResponse();
        resp.setStockcode(stock);
        resp.setMarketId(market);
        PricePoint pNull = new PricePoint();
        pNull.setTs(null);
        pNull.setOpen(BigDecimal.ONE);
        pNull.setHigh(BigDecimal.ONE);
        pNull.setLow(BigDecimal.ONE);
        pNull.setClose(BigDecimal.ONE);
        pNull.setVol(1L);
        PricePoint pLater = new PricePoint();
        pLater.setTs(2L);
        pLater.setOpen(BigDecimal.ONE);
        pLater.setHigh(BigDecimal.ONE);
        pLater.setLow(BigDecimal.ONE);
        pLater.setClose(BigDecimal.ONE);
        pLater.setVol(1L);
        // intentionally add later first to test sorting
        resp.addPricePoint(pLater);
        resp.addPricePoint(pNull);

        when(klineRepository.findRange(eq(stock), eq(market), isNull(), isNull(), isNull()))
            .thenReturn(resp);
        when(nameResolver.resolve(stock, market)).thenReturn("X");

        mvc.perform(get("/kline")
                .param("stockcode", stock)
                .param("marketId", market))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list[0].date").value("19700101"))
            .andExpect(jsonPath("$.list[0].time").value("0000"));
    }
}
