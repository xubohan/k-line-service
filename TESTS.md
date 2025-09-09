# 测试用例总览（按项目文件分组）

说明：本清单以项目源码文件为单位，列出对应被测函数与覆盖的测试用例（文件/方法）。集成/E2E 测试单独标注。

## deploy/src/main/java/com/example/kline/modules/kline/domain/entity/PricePoint.java
- isValid: 字段非空即有效（不校验数值范围/高低关系）
  - tests/src/test/java/com/example/kline/modules/kline/domain/entity/PricePointIsValidTest.java: 全字段非空为真；逐一字段置空为假
  - tests/src/test/java/com/example/kline/modules/kline/domain/entity/PricePointTest.java: 全空为假；部分字段为空为假；负值/高低反转仍为真（现实现）
- getters/setters: ts/open/high/low/close/vol 的读写
  - tests/src/test/java/com/example/kline/modules/kline/domain/entity/PricePointTest.java: 值保持性断言

## deploy/src/main/java/com/example/kline/modules/kline/domain/entity/KlineResponse.java
- addPricePoint: 忽略 null，正常追加
  - tests/src/test/java/com/example/kline/modules/kline/domain/entity/KlineResponseTest.java: 正常追加；追加 null 不变
- getDataInRange: 时间范围筛选（闭区间）
  - tests/src/test/java/com/example/kline/modules/kline/domain/entity/KlineResponseTest.java: 边界等值；空结果
- validateData: 所有点有效才为真
  - tests/src/test/java/com/example/kline/modules/kline/domain/entity/KlineResponseTest.java: 混合有效/无效为假
  - tests/src/test/java/com/example/kline/modules/kline/domain/entity/KlineResponseMoreTest.java: 全部有效为真；追加无效点变为假
- stockName getters/setters
  - tests/src/test/java/com/example/kline/modules/kline/domain/entity/KlineResponseMoreTest.java: 读写断言

## deploy/src/main/java/com/example/kline/modules/kline/domain/service/impl/NameResolverImpl.java
- resolve: 先读缓存，缓存 miss 调外部服务并写回
  - tests/src/test/java/com/example/kline/modules/kline/domain/service/impl/NameResolverImplTest.java: 首次调用写缓存；二次调用命中缓存不再外调；预置缓存优先
  - tests/src/test/java/com/example/kline/modules/kline/domain/service/impl/NameResolverImplMoreTest.java: 空输入返回非空字符串（适配现实现）；预热后并发重复解析仅走缓存（外调次数保持 1）

## deploy/src/main/java/com/example/kline/modules/kline/infrastructure/cache/RedisKlineCache.java
- putBatch: 覆盖写入；按 key 替换全量
  - tests/src/test/java/com/example/kline/modules/kline/infrastructure/cache/RedisKlineCacheExtraTest.java: 覆盖写入后仅保留新批次
- getRange: 范围过滤、升序、limit 截断、key 不存在返回空
  - tests/src/test/java/com/example/kline/modules/kline/infrastructure/cache/RedisKlineCacheTest.java: 范围/limit/无结果
  - tests/src/test/java/com/example/kline/modules/kline/infrastructure/cache/RedisKlineCacheMoreTest.java: key 不存在返回空；limit=1 边界
  - tests/src/test/java/com/example/kline/modules/kline/infrastructure/cache/RedisKlineCacheExtraTest.java: 乱序输入后升序返回

## deploy/src/main/java/com/example/kline/modules/kline/infrastructure/cache/RedisNameCache.java
- getName/setName: 简单 KV
  - 覆盖于 NameResolverImpl* 测试中（间接）

## deploy/src/main/java/com/example/kline/modules/kline/infrastructure/db/dao/KlineDao.java
- insertBatch: 批量插入
  - tests/src/test/java/com/example/kline/modules/kline/infrastructure/db/dao/KlineDaoTest.java: 插入后可查询
  - tests/src/test/java/com/example/kline/modules/kline/infrastructure/db/dao/KlineDaoMoreTest.java: 1000 条 < 1s 性能冒烟；并发多键插入无死锁
- selectRange: 范围筛选、排序、limit 截断
  - tests/src/test/java/com/example/kline/modules/kline/infrastructure/db/dao/KlineDaoTest.java: limit 截断
  - tests/src/test/java/com/example/kline/modules/kline/infrastructure/db/dao/KlineDaoMoreTest.java: 大数据范围中段；limit 大于数据量时返回全部

## deploy/src/main/java/com/example/kline/modules/kline/infrastructure/db/repository/KlineRepositoryImpl.java
- findRange: 先查缓存命中则返回；未命中回源 DB 并回填缓存
  - tests/src/test/java/com/example/kline/modules/kline/infrastructure/db/repository/KlineRepositoryImplTest.java: 回源并回填；跨实例缓存命中
  - tests/src/test/java/com/example/kline/modules/kline/infrastructure/db/repository/KlineRepositoryCacheHitTest.java: 缓存命中返回并可应用 limit
  - tests/src/test/java/com/example/kline/modules/kline/infrastructure/db/repository/KlineRepositoryImplMoreTest.java: limit 超量返回全部
- upsertBatch: DAO 写入 + 缓存回填
  - tests/src/test/java/com/example/kline/modules/kline/infrastructure/db/repository/KlineRepositoryImplTest.java: 写入后 DAO 与缓存数量一致
  - tests/src/test/java/com/example/kline/modules/kline/infrastructure/db/repository/KlineRepositoryImplMoreTest.java: 空数据不抛异常

## deploy/src/main/java/com/example/kline/interfaces/rest/ApiController.java
- getKline: 参数校验（由全局异常处理）、仓储查询、名称解析、排序输出契约
  - 参数校验（长度/空白/范围）
    - tests/src/test/java/com/example/kline/interfaces/rest/ApiControllerMockMvcTest.java: getKline_missingParam_returns400（缺失必填参数 → 400）
    - tests/src/test/java/com/example/kline/interfaces/rest/ApiControllerMockMvcTest.java: getKline_invalidLimit_returns400（limit 非数字 → 400）
    - tests/src/test/java/com/example/kline/interfaces/rest/ApiControllerMockMvcMoreTest.java: testGetKline_NegativeLimit_returns400（limit 负数 → 400）
    - tests/src/test/java/com/example/kline/interfaces/rest/ApiControllerMockMvcMoreTest.java: testGetKline_TooLongStockcode_returns400（stockcode 过长 → 400）
    - tests/src/test/java/com/example/kline/interfaces/rest/ApiControllerMockMvcMoreTest.java: testGetKline_BlankStockcode_returns400（stockcode 空白 → 400）
    - tests/src/test/java/com/example/kline/interfaces/rest/ApiControllerMockMvcMoreTest.java: testGetKline_BlankMarketId_returns400（marketId 空白 → 400）
    - tests/src/test/java/com/example/kline/interfaces/rest/ApiControllerMockMvcMoreTest.java: testGetKline_TooLongMarketId_returns400（marketId 过长 → 400）
  - 正常/边界与排序
    - tests/src/test/java/com/example/kline/interfaces/rest/ApiControllerMockMvcTest.java: getKline_returnsContractJson（正常 200+契约字段）
    - tests/src/test/java/com/example/kline/interfaces/rest/ApiControllerMockMvcMoreTest.java: testGetKline_SpecialCharsStockcode_ok（特殊字符 stockcode 仍可 200）
    - tests/src/test/java/com/example/kline/interfaces/rest/ApiControllerMockMvcMoreTest.java: testGetKline_MaxLimit_ok（limit=Integer.MAX_VALUE 正常）
    - tests/src/test/java/com/example/kline/interfaces/rest/ApiControllerMockMvcTest.java: getKline_nullTsMappedToEpochZero_andSorted（null ts → 19700101 0000 且排序）
    - tests/src/test/java/com/example/kline/interfaces/rest/ApiControllerMockMvcMoreTest.java: testGetKline_TimeRange_startGreaterThanEnd_returnsEmptyList（startTs>endTs → 空列表）
- toItem: ts 转 yyyyMMdd/HHmm，包含 OHLC/vol（间接覆盖）
  - 覆盖于上述 MockMvc 用例

## deploy/src/main/java/com/example/kline/interfaces/rest/GlobalExceptionHandler.java
- handleBadRequest / handleServerError: 400/500 响应体
  - 间接覆盖：ApiControllerMockMvcTest/MoreTest 的 400/500 场景

## deploy/src/main/java/com/example/kline/interfaces/consumer/TimelineConsumer.java
- run: 委托 klineRepository.upsertBatch
  - tests/src/test/java/com/example/kline/interfaces/consumer/TimelineConsumerTest.java: 调用一次且无多余交互

## 集成/E2E
- 集成（固定端口 HTTP）
  - tests/src/test/java/com/example/kline/interfaces/rest/ApiControllerHttpTest.java: 预热 DAO（kafka_data.json 20 条），验证 200、契约、排序、缺参/非法参数 400、首次调用后名称写入缓存
- 端到端（E2E，默认排除）
  - tests/src/test/java/com/example/kline/e2e/KafkaE2eTest.java: 模拟 Kafka 摄入 → DAO+缓存 校验 → HTTP 契约校验（已在 surefire excludes 中默认跳过）

## 测试数据与工具
- 测试数据
  - tests/src/test/resources/kafka_data.json: 基础 20 样本
  - tests/src/test/resources/boundary_data.json: 数值/时间边界
  - tests/src/test/resources/error_data.json: 异常数据
  - tests/src/test/java/com/example/kline/util/TestDataJsonTest.java: JSON 可解析性校验
- 生成器
  - tests/src/test/java/com/example/kline/util/RandomKlineDataGenerator.java: 顺序/随机时间点生成（用于性能与范围测试）

## 执行方式
- 单元+集成：`mvn -q -pl tests -am test`
- 覆盖率报告：`mvn -q -pl deploy -am verify`（HTML 报告见 `deploy/target/site/jacoco/index.html`）

## API Restrictions 对齐情况（黑盒）
- 接口与方法：`GET /kline`（仅此接口）
  - 覆盖：MockMvc 与固定端口 HTTP 测试均针对 `GET /kline`。
- 必填参数：`stockcode`,`marketId`
  - 覆盖：缺参返回 400（MockMvc: `testGetKline_missingParam_returns400`，HTTP: `testBadRequestWhenMissingRequiredParams`）。
- 参数类型：`limit` 必须为整数
  - 覆盖：`limit=abc` 返回 400（MockMvc/HTTP 用例）。
- 时间范围：`startTs <= endTs`
  - 覆盖：`startTs > endTs` 返回空列表（MockMvcMore: `testGetKline_TimeRange_startGreaterThanEnd_returnsEmptyList`）。
- 未知股票/市场：返回 200 且空数据
  - 待补：可通过 Mock 仓储返回空 `KlineResponse` 断言 `list` 为空、`code=0`。
- 安全输入：
  - SQL 注入 payload（如 `"';DROP TABLE--"`）
    - 待补：应断言“安全处理”（返回 200 且不回显危险内容，或 400 拒绝）。当前实现未专门校验长度/黑名单。
  - XSS payload（如 `<script>alert(1)</script>`）
    - 待补：应断言响应未包含注入内容（当前响应不回显 `stockcode` 字段，风险较低，但仍建议补用例）。
  - 超长参数（`stockcode` 重复 1000 字符）
    - 待补：文档期望 400；现实现未限制长度，建议新增校验或在测试中按“安全处理”路径断言。
- 内容与协商：`Accept: application/json`
  - 覆盖：MockMvc 测试使用 `accept(JSON)`，响应契约字段断言齐全。
- 状态码语义：200/400/500
  - 覆盖：200 正常查询；400 缺参/类型错误/负数或超长参数；500 其他未捕获服务器错误。
- 响应契约：`code/message/data.stockName/list[].{date,time,OHLC,vol}` 排序
  - 覆盖：MockMvc 与 HTTP 测试断言字段与顺序；`null ts` 映射为 epoch 0（19700101 0000）。

待办建议（不影响现有通过）：
- 增加三类黑盒用例：SQL 注入、XSS、超长参数。
