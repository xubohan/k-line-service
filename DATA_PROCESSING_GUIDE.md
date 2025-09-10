# 数据处理和Redis写入验证指南

## 🎯 目标
您希望：
1. 逐条发送 PricePoint 数据
2. 等待每条数据处理确认
3. 确保数据被写入 Redis
4. 能够查看 Redis 中写入的数据

## 🚀 解决方案

由于 Kafka Topic "timeline" 不存在导致发送失败，我提供了两个替代方案：

### 方案一：直接数据摄取（推荐）

**1. 使用直接摄取 API**
```bash
# 摄取10条数据到Redis（绕过Kafka）
curl -X POST "http://localhost:61851/ingest/direct?limit=10"
```

这个API会：
- ✅ 读取 kafka_data.json 中的前10条数据
- ✅ 逐条处理每个 PricePoint
- ✅ 直接调用 TimelineConsumer 处理
- ✅ 将数据写入 Redis
- ✅ 提供详细的处理日志

**2. 检查Redis中的数据**
```bash
# 检查Redis中是否有数据
curl "http://localhost:61851/redis/check?stockcode=300033&marketId=33"
```

**3. 使用标准API查询数据**
```bash
# 查询K线数据
curl "http://localhost:61851/kline?stockcode=300033&marketId=33"
```

### 方案二：修复Kafka环境

如果您想使用真正的Kafka，需要：

**1. 创建Kafka Topic**
```bash
# 在Kafka服务器上创建topic
kafka-topics.sh --create --topic timeline --bootstrap-server 10.10.80.109:9092 --partitions 1 --replication-factor 1
```

**2. 然后使用同步发送API**
```bash
# 同步发送前10条数据
curl -X POST "http://localhost:61851/kafka/send/sync-data"
```

## 📊 验证数据流程

**完整的数据流验证步骤**：

1. **摄取数据** → `POST /ingest/direct?limit=5`
2. **检查Redis** → `GET /redis/check`  
3. **查询API** → `GET /kline?stockcode=300033&marketId=33`
4. **验证数据一致性** → 对比摄取、Redis、API 的数据

## 🎯 预期结果

使用方案一后，您应该看到：

**摄取响应**：
```json
{
  "code": "0",
  "message": "Direct data ingestion completed",
  "data": {
    "processed": 10,
    "success": 10,
    "errors": 0,
    "source": "kafka_data.json",
    "mode": "direct_to_redis"
  }
}
```

**Redis检查**：
```json
{
  "code": "0",
  "message": "Redis data check completed",
  "data": {
    "stockcode": "300033",
    "marketId": "33", 
    "dataCount": 10,
    "hasData": true,
    "firstRecord": {...},
    "lastRecord": {...}
  }
}
```

**API查询**：
```json
{
  "code": "0",
  "message": "success",
  "data": {"stockName": "wu han"},
  "list": [10条数据...]
}
```

这样您就可以验证整个数据处理流程，确保数据被正确写入Redis并可以通过API查询到。

---

**建议**: 先使用方案一验证数据流程，确认一切正常后再考虑修复Kafka环境。