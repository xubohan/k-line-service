# Kafka 数据发送功能使用指南

## 🎯 功能概述

现在 [TestSender](file:///Users/bohan/Documents/k-line-service-1/deploy/src/main/java/com/example/kline/common/external/message/TestSender.java) 已经实现了读取 [kafka_data.json](file:///Users/bohan/Documents/k-line-service-1/deploy/src/main/resources/kafka_data.json) 文件并将所有数据推送到 Kafka topic "timeline" 的功能。

### 数据流程
```
kafka_data.json → TestSender → Kafka Topic "timeline" → TimelineConsumer → Redis
```

## 🔧 问题修复

### ✅ 修复 InterruptedException 问题

**问题描述**: 之前在发送大量消息时出现 `InterruptedException`，导致所有消息发送失败。

**原因分析**: 
- 在 Web 请求线程中使用了 `Thread.sleep(1)` 
- Spring Boot 的请求超时机制会中断长时间运行的 Web 请求
- 导致所有 3600 条消息都发送失败

**解决方案**:
1. ✅ **移除 Thread.sleep()**: 删除了不必要的延迟，Kafka 本身有限流机制
2. ✅ **添加异步支持**: 新增 `@Async` 方法，避免阻塞 Web 请求线程
3. ✅ **改进错误处理**: 单个消息失败不会中断整个发送流程
4. ✅ **启用异步配置**: 在主应用类中添加 `@EnableAsync` 注解

## 📊 数据统计

- **总消息数量**: 3600 条
- **股票代码**: 300033 
- **市场ID**: 33
- **数据格式**: 每条消息包含 `stockCode`, `marketId`, `price`, `date`, `time` 字段

## 🚀 使用方法

### 1. 启动应用
```bash
cd /Users/bohan/Documents/k-line-service-1
mvn clean package -pl deploy -am -DskipTests
java -jar deploy/target/deploy-0.0.1-SNAPSHOT.jar
```

### 2. 发送所有数据到 Kafka（异步方式）
```bash
curl -X POST "http://localhost:61851/kafka/send/all-data"
```

**预期响应**:
```json
{
  "code": "0",
  "message": "All kafka data sending started asynchronously (3600 messages)",
  "data": {
    "topic": "timeline",
    "totalMessages": 3600,
    "source": "kafka_data.json",
    "mode": "async",
    "note": "Messages are being sent in background, check logs for progress"
  }
}
```

**特点**:
- ✅ **异步执行**: API 立即返回，后台继续发送消息
- ✅ **无阻塞**: 不会因为长时间操作导致 Web 请求超时
- ✅ **进度监控**: 可以通过日志查看发送进度

### 3. 发送单条测试数据
```bash
curl -X POST "http://localhost:61851/kafka/send/timeline"
```

## 📋 Kafka 消息格式

### 发送到 Kafka 的消息格式
```json
{
  "topic": "timeline",
  "stock_minute_data": {
    "stockCode": "300033",
    "marketId": "33",
    "date": "20200101",
    "price": 88.94,
    "time": "0940"
  }
}
```

这个格式符合 [TimelineConsumer](file:///Users/bohan/Documents/k-line-service-1/deploy/src/main/java/com/example/kline/interfaces/consumer/TimelineConsumer.java) 的预期消息结构。

## 🔄 消费和存储流程

### TimelineConsumer 处理逻辑
1. **消息验证**: 检查 `stockCode`, `marketId`, `price`, `date`, `time` 字段
2. **数据转换**: 将 timeline 数据转换为 `PricePoint` 对象
3. **存储到仓储**: 调用 `KlineRepository.upsertBatch()` 
4. **Redis 缓存**: 数据最终存储到 Redis ZSet 中

### Redis 存储格式
- **Key**: `kline:1m:33:300033` (market:stock格式)
- **Score**: 分钟时间戳 (时间/60)
- **Member**: 价格字符串 

## 🕰️ 监控和日志

### 发送进度监控
TestSender 会记录发送进度（无中断风险）:
```
INFO - Starting to send 3600 timeline messages to Kafka
INFO - Sent 100/3600 messages
INFO - Sent 200/3600 messages
...
INFO - Finished sending Kafka data: 3600 success, 0 errors, 3600 total
```

### 消费监控
TimelineConsumer 会记录消费情况:
```
INFO - Processing timeline message for stock: 300033, market: 33
DEBUG - Converted timeline to PricePoint: ts=1577768400, price=88.94
```

### 错误处理改进
- ✅ **单个消息失败**: 不会中断整个发送流程
- ✅ **详细日志**: 记录失败消息的索引和原因
- ✅ **统计信息**: 最终显示成功、失败和总数量

## 🛠️ 技术实现细节

### TestSender 关键方法
- `sendAllKafkaDataAsync()`: 异步读取并发送所有数据（推荐）
- `sendAllKafkaData()`: 同步读取并发送数据（内部使用）
- `sendKafkaDataFromStream()`: 处理JSON数组并逐条发送
- `sendAsync()`: 异步发送单条消息

### 发送策略优化
- ✅ **异步执行**: 使用 `@Async` 注解，避免阻塞 Web 请求
- ✅ **批处理**: 逐条发送，避免内存压力
- ✅ **进度报告**: 每100条消息打印进度
- ✅ **错误容忍**: 单条消息失败不影响整体流程
- ✅ **无限流**: 移除了 `Thread.sleep()`，依赖 Kafka 内置限流

## ⚠️ 注意事项

1. **Kafka 连接**: 确保 Kafka 服务器 `10.10.80.109:9092` 可访问
2. **Redis 连接**: 确保 Redis 配置正确，数据将存储到 Redis DB 0
3. **消息顺序**: 使用单线程消费保证消息处理顺序
4. **错误恢复**: 如果 Kafka 服务不可用，消息会失败但不会影响应用运行

## 🧪 测试验证

### 验证数据是否成功存储到 Redis
```bash
# 查询 K线数据 API
curl "http://localhost:61851/kline?stockcode=300033&marketId=33"
```

**预期结果**: 返回包含3600条数据的响应，数据按时间顺序排列。

---

**更新时间**: 2025年9月10日  
**功能版本**: v2.0 (修复 InterruptedException 问题)  
**支持的数据格式**: Timeline JSON 格式 ✅  
**稳定性**: 异步执行，无中断风险 ✅