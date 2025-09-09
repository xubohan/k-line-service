# K线数据服务 (K-Line Service)

一个基于领域驱动设计（DDD）的最小化股票K线数据服务，用于消费Kafka消息中的股票价格更新，使用Redis缓存数据并提供HTTP API查询指定股票的分钟级K线数据。

## 项目概述

### 核心功能
- 消费Kafka消息中的股票价格更新
- 使用Redis存储K线数据（ZSET结构，支持时间范围查询）
- 提供HTTP API查询分钟级K线数据
- 从外部名称服务获取并缓存股票名称
- 支持时间范围查询和数据限制

### 技术特性
- 领域驱动设计（DDD）架构
- 多层架构（Interfaces、Domain、Infrastructure）
- Redis ZSET存储，支持高效时间范围查询
- 外部服务集成与缓存策略
- 完整的错误处理和异常映射

## 快速开始

### 环境要求
- JDK 1.8+
- Maven 3.6.3+
- Redis 服务（端口6379）

### 构建与运行

```bash
# 1. 克隆项目
git clone <repository-url>
cd k-line-service-1

# 2. 构建项目
mvn clean package -pl deploy -am -DskipTests

# 3. 启动Redis服务
redis-server

# 4. 运行应用
cd deploy
java -jar target/deploy-0.0.1-SNAPSHOT.jar --server.port=8080

# 5. 摄取测试数据（可选）
java -jar target/deploy-0.0.1-SNAPSHOT.jar --app.ingest.file=../tests/src/test/resources/kafka_data.json --server.port=8080
```

### 验证服务

```bash
# 查询K线数据
curl "http://localhost:8080/kline?stockcode=300033&marketId=33&limit=5"

# 检查Redis中的数据
redis-cli ZCARD "kline:1m:33:300033"
```

## API接口参考

### GET /kline - 查询K线数据

#### 请求参数

| 参数名    | 类型    | 必填 | 说明                     | 示例值             |
|-----------|---------|------|--------------------------|--------------------|
| stockcode | String  | 是   | 股票代码（≤64字符）      | `300033`           |
| marketId  | String  | 是   | 市场ID（≤16字符）        | `33`               |
| startTs   | Long    | 否   | 起始时间戳（秒）         | `1577872800`       |
| endTs     | Long    | 否   | 结束时间戳（秒）         | `1577876400`       |
| limit     | Integer | 否   | 返回最大条数（≤1000）    | `100`              |

#### 响应格式

**成功响应（200）：**
```json
{
  "code": "0",
  "message": "success",
  "data": {
    "stockName": "wu han"
  },
  "list": [
    {
      "date": "20200101",
      "time": "1024",
      "open": 88.45,
      "high": 88.45,
      "low": 88.45,
      "close": 88.45,
      "vol": 0
    }
  ]
}
```

**错误响应（400）：**
```json
{
  "code": "400",
  "message": "stockcode must not be blank",
  "data": null,
  "list": []
}
```

#### 字段说明

- **code**: 响应码，"0"表示成功，非"0"表示失败
- **message**: 响应消息
- **data.stockName**: 股票名称（从外部名称服务获取）
- **list**: K线数据数组，按时间升序排列
  - **date**: 日期（yyyyMMdd格式，UTC时间）
  - **time**: 时间（HHmm格式，UTC时间）
  - **open/high/low/close**: 开盘价/最高价/最低价/收盘价
  - **vol**: 成交量

#### 调用示例

```bash
# 查询全部数据
curl "http://localhost:8080/kline?stockcode=300033&marketId=33"

# 查询指定时间范围的前10条数据
curl "http://localhost:8080/kline?stockcode=300033&marketId=33&startTs=1577872800&endTs=1577876400&limit=10"

# 查询最近5条数据
curl "http://localhost:8080/kline?stockcode=300033&marketId=33&limit=5"
```

## 项目架构

### 整体架构

本项目采用领域驱动设计（DDD）架构，分为三个主要层次：

```
┌─────────────────┐
│   接口层        │ ← REST API、Kafka Consumer、文件摄取器
│   (Interfaces)  │
├─────────────────┤
│   领域层        │ ← 业务逻辑、领域实体、仓储接口
│   (Domain)      │
├─────────────────┤
│   基础设施层    │ ← Redis缓存、外部服务、数据持久化
│   (Infrastructure)│
└─────────────────┘
```

### 核心模块组件

#### 1. 接口层 (Interfaces)

- **`ApiController`**: REST API控制器，处理/kline查询请求
- **`TimelineConsumer`**: Kafka消息消费者（支持开关控制）
- **`TimelineFileIngestor`**: 文件数据摄取器（用于测试数据导入）
- **`GlobalExceptionHandler`**: 全局异常处理器

#### 2. 领域层 (Domain)

**实体 (Entities):**
- **`KlineResponse`**: K线响应聚合根，包含股票代码、市场ID和价格点列表
- **`PricePoint`**: 价格点值对象，包含时间戳和OHLC数据

**仓储接口 (Repository):**
- **`KlineRepository`**: K线数据仓储接口，定义数据查询和存储契约

**领域服务 (Services):**
- **`NameResolver`**: 股票名称解析服务接口

#### 3. 基础设施层 (Infrastructure)

**缓存层:**
- **`RedisKlineCache`**: Redis K线数据缓存，使用ZSET存储支持时间范围查询
- **`RedisNameCache`**: Redis股票名称缓存
- **`TimelineRedisWriter`**: 实时数据写入Redis的组件

**数据访问层:**
- **`KlineRepositoryImpl`**: K线仓储实现，整合缓存和数据库访问
- **`KlineDao`**: 数据访问对象（当前为内存实现）

**外部服务层:**
- **`NameServiceHttp`**: 外部股票名称服务HTTP客户端
- **`NameResolverImpl`**: 名称解析服务实现

### 数据流架构

```
Kafka消息 → TimelineConsumer → TimelineRedisWriter → Redis ZSET
                ↓
文件数据 → TimelineFileIngestor → TimelineRedisWriter → Redis ZSET
                                       ↓
客户端请求 → ApiController → KlineRepository → RedisKlineCache → Redis ZSET
             ↓
           NameResolver → RedisNameCache/NameServiceHttp
```

### Redis数据存储结构

#### K线数据存储（ZSET）
- **Key格式**: `kline:1m:{marketId}:{stockCode}`
- **Score**: 时间戳（分钟级别，`tsSec/60`）
- **Member**: 价格字符串（`price.toPlainString()`）
- **优势**: 支持高效的时间范围查询（`ZRANGEBYSCORE`）

#### 名称缓存存储（String）
- **Key格式**: `{stockCode}:{marketId}`
- **Value**: JSON格式的股票信息

### 配置项说明

#### application.properties 核心配置

```properties
# 服务端口
server.port=8080

# Redis连接配置
app.redis.external=true
spring.redis.host=127.0.0.1
spring.redis.port=6379

# Kafka配置（默认关闭）
app.kafka.enabled=false
# spring.kafka.bootstrap-servers=localhost:9092

# 名称服务桩配置
app.namesvc.stub.enabled=true
app.namesvc.stub.stockcode=300033
app.namesvc.stub.marketId=33
app.namesvc.stub.stockName=wu han

# 数据摄取配置（可选）
# app.ingest.file=/path/to/kafka_data.json
```

## 技术栈

- **核心框架**: Spring Boot 2.3.12.RELEASE
- **编程语言**: Java SE 8
- **构建工具**: Maven 3.6.3
- **缓存数据库**: Redis 6.x
- **消息队列**: Apache Kafka（可选）
- **测试框架**: JUnit 5
- **JSON处理**: Jackson
- **Redis客户端**: Jedis 3.7.1

## 开发与测试

### 模块结构
```
k-line-service-1/
├── api/                    # 共享接口模块
├── deploy/                 # 主应用模块
│   ├── src/main/java/     # 业务逻辑代码
│   └── src/main/resources/ # 配置文件
├── tests/                  # 测试模块
│   ├── src/test/java/     # 单元测试和集成测试
│   └── src/test/resources/ # 测试数据
├── uml/                    # 架构设计文档
└── pom.xml                # 根POM文件
```

### 运行测试

```bash
# 运行所有测试
mvn test -pl tests -am

# 生成测试覆盖率报告
mvn verify -pl tests -am

# 查看覆盖率报告
open tests/target/site/jacoco/index.html
```

### 数据摄取工具

项目提供了便捷的数据摄取工具，可以将JSON格式的测试数据批量导入Redis：

```bash
# 启动应用并摄取测试数据
java -jar deploy/target/deploy-0.0.1-SNAPSHOT.jar \
  --app.ingest.file=tests/src/test/resources/kafka_data.json \
  --server.port=8080
```

## 生产部署建议

### 环境准备
1. 确保Redis服务可用
2. 配置适当的JVM参数
3. 设置合适的日志级别
4. 配置监控和告警

### 配置调优
```properties
# 生产环境建议配置
logging.level.com.example.kline=INFO
management.endpoints.web.exposure.include=health,info,metrics
app.rate.kline.qps=1000
```

### 扩展说明

当前版本为MVP实现，未来可扩展：
- 集成真实的Kafka集群
- 添加持久化数据库支持
- 实现更复杂的缓存策略
- 增加认证和授权机制
- 添加更多的监控指标

## 许可证

本项目采用 [MIT License](LICENSE) 开源许可证。

