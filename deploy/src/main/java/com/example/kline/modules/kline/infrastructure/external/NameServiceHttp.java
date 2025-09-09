package com.example.kline.modules.kline.infrastructure.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Simulated external name service.
 *
 * Contract (api restriction):
 * Response JSON: {"code":"0","message":"success","data":{"stockName":"..."}}
 *
 * This stub constructs the contract JSON and returns data.stockName value.
 */
@Component
public class NameServiceHttp {
    private static final ObjectMapper M = new ObjectMapper();

    @Value("${app.namesvc.stub.enabled:true}")
    private boolean stubEnabled;

    @Value("${app.namesvc.stub.stockcode:300033}")
    private String stubSc;
    @Value("${app.namesvc.stub.marketId:33}")
    private String stubMk;
    @Value("${app.namesvc.stub.stockName:wu han}")
    private String stubName;

    public String fetchName(String stockcode, String marketId) {
        if (stubEnabled && stubSc.equals(stockcode) && stubMk.equals(marketId)) {
            try {
                String json = successJson(stubName);
                // parse back per contract
                Map<?,?> map = M.readValue(json, Map.class);
                Object data = map.get("data");
                if (data instanceof Map) {
                    Object name = ((Map<?,?>) data).get("stockName");
                    return name == null ? null : String.valueOf(name);
                }
            } catch (Exception ignore) { }
        }
        // fallback simple stub
        return "NAME-" + stockcode + "-" + marketId;
    }

    private String successJson(String stockName) throws Exception {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("code", "0");
        out.put("message", "success");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stockName", stockName);
        out.put("data", data);
        return M.writeValueAsString(out);
    }
}
