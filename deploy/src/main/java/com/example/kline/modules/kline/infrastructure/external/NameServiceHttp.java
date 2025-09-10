package com.example.kline.modules.kline.infrastructure.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 股票名称服务HTTP客户端
 * 
 * 调用外部接口获取股票名称信息
 * 请求格式: GET {baseUrl}?stockcode=xxx&marketId=xxx
 * 响应格式: {"code":"0","message":"success","data":{"stockName":"xxx"}}
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-10 13:30:00
 */
@Component
public class NameServiceHttp {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.namesvc.baseUrl:}")
    private String baseUrl;
    
    @Value("${app.namesvc.timeout:5000}")
    private int timeoutMs;
    
    @Value("${app.namesvc.stub.enabled:true}")
    private boolean stubEnabled;

    @Value("${app.namesvc.stub.stockcode:300033}")
    private String stubSc;
    @Value("${app.namesvc.stub.marketId:33}")
    private String stubMk;
    @Value("${app.namesvc.stub.stockName:wu han}")
    private String stubName;

    /**
     * 获取股票名称
     * 
     * @param stockcode 股票代码
     * @param marketId 市场ID
     * @return 股票名称，获取失败时返回null
     */
    public String fetchName(String stockcode, String marketId) {
        // 参数验证
        if (!StringUtils.hasText(stockcode) || !StringUtils.hasText(marketId)) {
            return null;
        }
        
        // 如果启用桩模式，直接返回配置的测试数据
        if (stubEnabled) {
            if (stubSc.equals(stockcode) && stubMk.equals(marketId)) {
                return stubName;
            }
            // 其他股票返回格式化名称
            return "STOCK-" + stockcode + "-" + marketId;
        }
        
        // 调用真实的外部服务
        return callRealNameService(stockcode, marketId);
    }
    
    /**
     * 调用真实的名称服务接口
     */
    private String callRealNameService(String stockcode, String marketId) {
        if (!StringUtils.hasText(baseUrl)) {
            // 没有配置baseUrl，使用桩数据
            return "STUB-" + stockcode + "-" + marketId;
        }
        
        try {
            String url = baseUrl + "?stockcode=" + stockcode + "&marketId=" + marketId;
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseNameFromResponse(response.getBody());
            }
            
            return null;
            
        } catch (ResourceAccessException e) {
            // 网络超时或连接失败
            return null;
        } catch (Exception e) {
            // 其他异常
            return null;
        }
    }
    
    /**
     * 解析响应JSON，提取股票名称
     */
    private String parseNameFromResponse(String responseBody) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = MAPPER.readValue(responseBody, Map.class);
            
            // 检查响应code
            Object code = response.get("code");
            if (!"0".equals(String.valueOf(code))) {
                return null; // 业务失败
            }
            
            // 提取data.stockName
            Object data = response.get("data");
            if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) data;
                Object stockName = dataMap.get("stockName");
                return stockName != null ? String.valueOf(stockName) : null;
            }
            
            return null;
            
        } catch (Exception e) {
            return null;
        }
    }


}
