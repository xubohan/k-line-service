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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ApiController.class)
@Import(GlobalExceptionHandler.class)
public class ApiControllerMockMvcMoreTest {

    @Autowired private MockMvc mvc;
    @MockBean private KlineRepository klineRepository;
    @MockBean private NameResolver nameResolver;

    private static KlineResponse onePoint(String sc, String mk, long ts) {
        KlineResponse resp = new KlineResponse();
        resp.setStockcode(sc); resp.setMarketId(mk);
        PricePoint p = new PricePoint();
        p.setTs(ts);
        p.setOpen(BigDecimal.ONE); p.setHigh(BigDecimal.ONE); p.setLow(BigDecimal.ONE); p.setClose(BigDecimal.ONE); p.setVol(1L);
        resp.addPricePoint(p);
        return resp;
    }

    @Test
    public void testGetKline_SpecialCharsStockcode_ok() throws Exception {
        String sc = "!@#$%^&*()"; String mk = "SZ";
        when(klineRepository.findRange(eq(sc), eq(mk), any(), any(), any())).thenReturn(onePoint(sc, mk, 1L));
        when(nameResolver.resolve(sc, mk)).thenReturn("NAME-x");

        mvc.perform(get("/kline").param("stockcode", sc).param("marketId", mk).accept(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.code").value("0"))
           .andExpect(jsonPath("$.list").isArray());
    }

    @Test
    public void testGetKline_MaxLimit_ok() throws Exception {
        String sc = "SC"; String mk = "MK";
        when(klineRepository.findRange(eq(sc), eq(mk), any(), any(), eq(Integer.MAX_VALUE))).thenReturn(onePoint(sc, mk, 1L));
        when(nameResolver.resolve(sc, mk)).thenReturn("NM");

        mvc.perform(get("/kline").param("stockcode", sc).param("marketId", mk).param("limit", String.valueOf(Integer.MAX_VALUE)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    public void testGetKline_NegativeLimit_returns400() throws Exception {
        String sc = "SC"; String mk = "MK";
        when(nameResolver.resolve(sc, mk)).thenReturn("NM");

        mvc.perform(get("/kline").param("stockcode", sc).param("marketId", mk).param("limit", "-1"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    public void testGetKline_TooLongStockcode_returns400() throws Exception {
        String longCode = new String(new char[1000]).replace('\0', 'A');
        mvc.perform(get("/kline").param("stockcode", longCode).param("marketId", "MK"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    public void testGetKline_BlankStockcode_returns400() throws Exception {
        mvc.perform(get("/kline").param("stockcode", " ").param("marketId", "MK"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.code").value("400"))
           .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("stockcode must not be blank")));
    }

    @Test
    public void testGetKline_BlankMarketId_returns400() throws Exception {
        mvc.perform(get("/kline").param("stockcode", "SC").param("marketId", " "))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.code").value("400"))
           .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("marketId must not be blank")));
    }

    @Test
    public void testGetKline_TooLongMarketId_returns400() throws Exception {
        String longMk = new String(new char[100]).replace('\0', 'Z');
        mvc.perform(get("/kline").param("stockcode", "SC").param("marketId", longMk))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.code").value("400"))
           .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("marketId too long")));
    }

    @Test
    public void testGetKline_TimeRange_startGreaterThanEnd_returnsEmptyList() throws Exception {
        String sc = "SC"; String mk = "MK";
        when(klineRepository.findRange(eq(sc), eq(mk), eq(10L), eq(5L), isNull())).thenReturn(new KlineResponse());
        when(nameResolver.resolve(sc, mk)).thenReturn("NM");

        mvc.perform(get("/kline").param("stockcode", sc).param("marketId", mk).param("startTs", "10").param("endTs", "5"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.list").isArray())
           .andExpect(jsonPath("$.list").isEmpty());
    }

    @Test
    public void testGetKline_UnknownStock_returns200EmptyList() throws Exception {
        String sc = "UNKNOWN"; String mk = "ZZ";
        when(klineRepository.findRange(eq(sc), eq(mk), isNull(), isNull(), isNull())).thenReturn(new KlineResponse());
        when(nameResolver.resolve(sc, mk)).thenReturn("N/A");

        mvc.perform(get("/kline").param("stockcode", sc).param("marketId", mk))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.code").value("0"))
           .andExpect(jsonPath("$.list").isArray())
           .andExpect(jsonPath("$.list").isEmpty());
    }
}
