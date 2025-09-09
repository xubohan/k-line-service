# 股票K线数据服务测试计划 v2.0

## 测试架构概览

```
┌─────────────────────────────────────────────┐
│           E2E测试 (端到端测试)                │
├─────────────────────────────────────────────┤
│           集成测试 (Integration)              │
├─────────────────────────────────────────────┤
│           单元测试 (Unit)                     │
└─────────────────────────────────────────────┘
```

## 1. 单元测试层 (Unit Testing)

### 1.1 Domain层测试

#### 1.1.1 KlineResponse聚合根测试
**现有测试**: `KlineResponseTest.java`, `KlineResponseMoreTest.java`

| 测试项 | 测试目的 | 测试用例 | 预期结果 |
|--------|---------|---------|----------|
| `testAddPricePoint_Normal` | 验证正常添加数据点 | 添加有效的PricePoint | data列表包含该点 |
| `testAddPricePoint_Null` | 验证空值处理 | 添加null | data列表不变 |
| `testGetDataInRange_BoundaryCase` | 验证边界查询 | startTs=endTs=某个时间戳 | 返回该时间点数据 |
| `testGetDataInRange_EmptyResult` | 验证空结果处理 | 查询不存在的时间范围 | 返回空列表 |
| `testValidateData_MixedValid` | 验证混合数据校验 | 包含有效和无效数据 | 返回false |

#### 1.1.2 PricePoint值对象测试
**现有测试**: `PricePointTest.java`

| 测试项 | 测试目的 | 测试用例 | 预期结果 |
|--------|---------|---------|----------|
| `testIsValid_AllFieldsNull` | 验证全空校验 | 所有字段为null | 返回false |
| `testIsValid_PartialNull` | 验证部分字段空 | 仅设置ts和open | 返回false |
| `testPriceRelations` | 验证价格关系合理性 | high < low的异常数据 | isValid返回true（当前未校验） |
| `testNegativeValues` | 验证负值处理 | price为负数, vol为负数 | isValid返回true（当前未校验） |

#### 1.1.3 NameResolver域服务测试
**现有测试**: `NameResolverImplTest.java`

| 测试项 | 测试目的 | 测试用例 | 预期结果 |
|--------|---------|---------|----------|
| `testResolve_EmptyInput` | 验证空输入处理 | stockcode=""或null | 返回默认值或null |
| `testResolve_ServiceTimeout` | 验证超时处理 | 模拟服务超时 | 返回缓存或默认值 |
| `testResolve_ConcurrentAccess` | 验证并发访问 | 多线程同时请求同一股票 | 只调用一次外部服务 |

### 1.2 Infrastructure层测试

#### 1.2.1 KlineRepositoryImpl测试
**现有测试**: `KlineRepositoryImplTest.java`, `KlineRepositoryCacheHitTest.java`

| 测试项 | 测试目的 | 测试用例 | 预期结果 |
|--------|---------|---------|----------|
| `testFindRange_CachePartialHit` | 验证部分缓存命中 | 缓存只有部分数据 | 从DB补充缺失数据 |
| `testFindRange_LimitExceedsData` | 验证limit超过数据量 | limit=100, 实际5条 | 返回全部5条 |
| `testUpsertBatch_EmptyData` | 验证空数据处理 | response.data为空 | 不抛异常，正常返回 |
| `testUpsertBatch_DuplicateTs` | 验证重复时间戳 | 多个相同ts的数据点 | 覆盖或保留最新 |

#### 1.2.2 缓存层测试
**现有测试**: `RedisKlineCacheTest.java`, `RedisKlineCacheMoreTest.java`

| 测试项 | 测试目的 | 测试用例 | 预期结果 |
|--------|---------|---------|----------|
| `testPutBatch_Overwrite` | 验证覆盖写入 | 重复写入同一key | 新数据覆盖旧数据 |
| `testGetRange_SortOrder` | 验证排序 | 乱序数据 | 按ts升序返回 |
| `testConcurrentReadWrite` | 验证并发安全 | 多线程读写同一key | 数据一致性保证 |

### 1.3 Interfaces层测试

#### 1.3.1 REST Controller测试
**现有测试**: `ApiControllerMockMvcTest.java`

| 测试项 | 测试目的 | 测试用例 | 预期结果 |
|--------|---------|---------|----------|
| `testGetKline_SpecialChars` | 验证特殊字符处理 | stockcode包含特殊字符 | 正确处理或400错误 |
| `testGetKline_MaxLimit` | 验证最大limit | limit=Integer.MAX_VALUE | 正常处理或限制 |
| `testGetKline_NegativeLimit` | 验证负数limit | limit=-1 | 返回400错误 |
| `testGetKline_TimeRange` | 验证时间范围 | startTs > endTs | 返回空数据或错误 |

## 2. 集成测试层 (Integration Testing)

### 2.1 数据库集成测试

| 测试项 | 测试目的 | 测试环境 | 验证点 |
|--------|---------|---------|--------|
| `testDao_BatchInsertPerformance` | 验证批量插入性能 | 内存DB | 1000条数据<1秒 |
| `testDao_ConcurrentInsert` | 验证并发插入 | 内存DB | 无死锁，数据完整 |
| `testDao_LargeDataQuery` | 验证大数据查询 | 内存DB | 分页正确，无OOM |
| `testDao_TransactionRollback` | 验证事务回滚 | 内存DB | 异常时数据不入库 |

### 2.2 缓存集成测试

| 测试项 | 测试目的 | 测试环境 | 验证点 |
|--------|---------|---------|--------|
| `testCache_TTLExpiry` | 验证TTL过期 | Redis Mock | 过期后自动清除 |
| `testCache_MemoryLimit` | 验证内存限制 | Redis Mock | 达到限制时LRU淘汰 |
| `testCache_NetworkFailure` | 验证网络故障 | Redis Mock | 降级到DB查询 |

### 2.3 外部服务集成测试

| 测试项 | 测试目的 | 测试环境 | 验证点 |
|--------|---------|---------|--------|
| `testNameService_Retry` | 验证重试机制 | WireMock | 失败后重试3次 |
| `testNameService_CircuitBreaker` | 验证熔断器 | WireMock | 连续失败触发熔断 |
| `testNameService_Fallback` | 验证降级处理 | WireMock | 服务不可用时返回默认值 |

## 3. 端到端测试 (E2E Testing)

### 3.1 完整业务流程测试
**现有测试**: `KafkaE2eTest.java`

| 测试场景 | 测试目的 | 测试步骤 | 验证点 |
|---------|---------|---------|--------|
| `test_CompleteDataFlow` | 验证完整数据流 | 1.Kafka写入→2.查询API | 数据一致性 |
| `test_CacheWarming` | 验证缓存预热 | 1.冷启动→2.首次查询→3.二次查询 | 二次查询更快 |
| `test_DataConsistency` | 验证数据一致性 | 1.并发写入→2.并发查询 | 无数据丢失 |
| `test_ErrorRecovery` | 验证错误恢复 | 1.注入故障→2.恢复→3.验证 | 自动恢复正常 |

### 3.2 Kafka消息处理测试

| 测试场景 | 测试目的 | 输入数据 | 验证点 |
|---------|---------|---------|--------|
| `test_ValidMessage` | 验证正常消息 | 符合契约的JSON | 成功入库和缓存 |
| `test_MalformedJSON` | 验证格式错误 | 非JSON字符串 | 记录错误，不崩溃 |
| `test_MissingRequiredField` | 验证缺失字段 | 缺少stockCode | 拒绝处理，记录日志 |
| `test_InvalidDateFormat` | 验证日期格式 | date="2020/01/01" | 拒绝处理 |
| `test_NegativePrice` | 验证负价格 | price=-1 | 根据业务规则处理 |
| `test_MessageOrder` | 验证消息顺序 | 乱序消息 | 按时间戳排序存储 |

## 4. 黑盒测试 (Black Box Testing)

### 4.1 API功能测试

| 测试场景 | 输入 | 预期输出 | 测试类型 |
|---------|------|---------|---------|
| 正常查询 | stockcode=300033&marketId=33 | 200, 含数据 | 功能测试 |
| 股票不存在 | stockcode=999999&marketId=XX | 200, 空数据 | 边界测试 |
| 参数缺失 | 仅stockcode | 400错误 | 异常测试 |
| SQL注入 | stockcode=';DROP TABLE-- | 安全处理 | 安全测试 |
| XSS攻击 | stockcode=<script>alert(1)</script> | 转义处理 | 安全测试 |
| 超长参数 | stockcode=重复1000字符 | 400错误 | 边界测试 |

### 4.2 业务规则测试

| 测试场景 | 测试目的 | 验证点 |
|---------|---------|--------|
| 交易时间查询 | 验证交易时间数据 | 9:30-15:00有数据 |
| 非交易时间查询 | 验证非交易时间 | 返回最近数据 |
| 停牌股票查询 | 验证停牌处理 | 返回历史数据 |
| 新股查询 | 验证新股处理 | 无数据时合理提示 |

## 5. 白盒测试 (White Box Testing)

### 5.1 代码覆盖测试

| 覆盖类型 | 目标覆盖率 | 重点关注 |
|---------|-----------|---------|
| 行覆盖 | ≥90% | 核心业务逻辑 |
| 分支覆盖 | ≥80% | if/else, switch |
| 路径覆盖 | ≥70% | 复杂方法 |

### 5.2 逻辑路径测试

| 测试路径 | 场景描述 | 验证点 |
|---------|---------|--------|
| 缓存命中路径 | L1→返回 | 最快响应 |
| 缓存未中路径 | L1未中→L2→返回 | 正确降级 |
| 全未命中路径 | L1未中→L2未中→DB→回填 | 完整流程 |
| 异常处理路径 | 各层异常→降级→返回 | 优雅降级 |

## 6. 边界测试 (Boundary Testing)

### 6.1 数值边界

| 测试项 | 边界值 | 验证点 |
|-------|--------|--------|
| 时间戳边界 | 0, Long.MAX_VALUE | 正确处理 |
| 价格边界 | 0, Double.MAX_VALUE | 精度不丢失 |
| 数量边界 | 0, 1, Integer.MAX_VALUE | 无溢出 |
| Limit边界 | 0, 1, null, 负数 | 合理默认值 |

### 6.2 时间边界

| 测试项 | 边界条件 | 验证点 |
|-------|---------|--------|
| 查询范围 | startTs=endTs | 返回单点数据 |
| 查询范围 | startTs>endTs | 返回空或错误 |
| 未来时间 | ts>当前时间 | 合理处理 |
| 历史数据 | 5年前数据 | 正确查询 |

## 7. 测试执行策略

### 7.1 测试分级执行

```yaml
Level 1 (PR级别): # 每次提交必须通过
  - 单元测试 (全部)
  - 集成测试 (核心路径)
  时间: <5分钟

Level 2 (Daily Build): # 每日构建
  - Level 1全部
  - 集成测试 (全部)
  - E2E测试 (核心场景)
  时间: <30分钟

Level 3 (Release): # 发布前
  - Level 2全部
  - E2E测试 (全部)
  - 黑盒测试
  - 性能基准测试
  时间: <2小时
```

### 7.2 测试数据管理

| 数据类型 | 存储位置 | 用途 |
|---------|---------|------|
| 基础测试数据 | `/tests/resources/kafka_data.json` | 标准测试集 |
| 边界测试数据 | `/tests/resources/boundary_data.json` | 边界条件 |
| 异常测试数据 | `/tests/resources/error_data.json` | 异常场景 |
| 性能测试数据 | 动态生成 | 大数据量测试 |

## 8. 测试优先级和风险评估

### 高优先级（P0）- 必须测试
- Kafka消息正确解析和存储
- API基本查询功能
- 数据一致性保证
- 缓存和数据库同步

### 中优先级（P1）- 应该测试
- 并发访问处理
- 异常数据处理
- 缓存失效和回填
- 外部服务降级

### 低优先级（P2）- 可选测试
- 极端边界条件
- 罕见异常场景
- 性能优化验证

## 9. 测试报告要求

每个测试执行后应产生报告包含：
- 测试覆盖率统计（行/分支/路径）
- 失败用例详情和根因分析
- 性能指标（如适用）
- 发现的缺陷和修复建议
- 风险评估和遗留问题

## 10. 持续改进

- 定期回顾测试有效性
- 根据生产问题补充测试用例
- 优化测试执行时间
- 提升测试自动化程度
- 建立测试知识库
