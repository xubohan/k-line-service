# 测试实施规划（重制）

本文基于《uml/comprehensive_testflow.md》制定并落地单元测试与集成测试方案；同时适配现有实现与既有用例，确保测试在本工程可稳定运行。

## 目标与范围
- 覆盖 Domain/Infrastructure/Interfaces 三层核心路径，优先级参考文档 P0/P1 列表。
- 使用已有基础数据 `kafka_data.json`（20 条）并按相同格式在内存随机生成更多样本；补充边界/异常数据已放在 `boundary_data.json`、`error_data.json`。
- 完成可执行的单元测试与集成测试，并校验构建通过与基础覆盖率（deploy 模块通过 tests 的 JaCoCo 报告聚合）。

## 落地步骤
1) 单元测试（Domain）
- KlineResponse：新增/范围查询/数据校验（已存在）；保持断言明确与边界覆盖。
- PricePoint：空字段、部分字段、负值、乱序关系（已存在）。
- NameResolverImpl：
  - 缓存命中覆盖外部调用（已存在）
  - 并发读取同一键（已完成：预热后并发仅走缓存，不新增外调）
  - 空输入健壮性（已完成：适配当前实现）

2) 单元测试（Infrastructure）
- RedisKlineCache：覆盖 put 覆盖写、乱序输入后 getRange 升序返回、limit 边界（部分已存在，补充覆盖写与排序）。
- KlineDao：insertBatch 与 selectRange 的 limit 行为、空结果、性能冒烟（1000 条 < 1s）。
- KlineRepositoryImpl：
  - 缓存命中直接返回（已存在）
  - 缓存 miss 回源 DB 并回填缓存（已存在）
  - limit 大于数据量（已完成）
  - upsert 空数据不抛异常（已完成）

3) 单元/接口测试（Interfaces）
- ApiController（MockMvc）：
  - 正常查询（已存在）
  - 缺失参数/类型错误 → 400（已存在）
  - startTs > endTs 返回空列表（已完成）
  - 特殊字符 stockcode 适配（已完成，响应 200，契约字段齐全）
  - limit 边界：`Integer.MAX_VALUE` 正常、负数 → 400（已完成，已在 Controller 校验）
- TimelineConsumer：委托仓储 upsert（已存在）。

4) 集成测试
- 轻量级 HTTP 集成（固定端口）：使用 `kafka_data.json` 预热 DAO，验证契约字段、顺序和长度（已存在）。
- 端到端 Kafka 模拟在 e2e 目录（存在但默认排除，不纳入本阶段）。

5) 测试数据与工具
- 资源文件：`kafka_data.json`（基础）、`boundary_data.json`、`error_data.json`（已存在）。
- 新增内存生成器：`RandomKlineDataGenerator` 用于批量生成 PricePoint/JSON 样本，支撑性能与大数据查询测试。

6) 执行与门禁
- 在本地执行：`mvn -q -pl tests -am test`（先安装 JDK，再执行）。
- 验收标准：
  - 测试全部通过（tests 模块 surefire 已排除需要真实端口/外部依赖的用例）。
  - deploy 侧 JaCoCo 报告可在本地 `mvn -pl deploy -am verify` 生成（可选）。

## 进度确认与适配说明
- 已完成：
  - 完成 Domain/Infrastructure/Interfaces 层单元测试与 MockMvc 场景补齐（含 startTs>endTs、limit 边界、特殊字符）。
  - 完成 HTTP 集成测试；完成随机数据生成器。
  - 在 ApiController 增加参数校验：stockcode/marketId 非空与长度限制，limit ≥ 0，从而实现“负数与超长参数 → 400”。
  - 全局异常 400 捕获 `IllegalArgumentException` 对齐测试。
- 差异已解决：
  - 负数 limit 现已返回 400（原计划差异项已消除）。
- 仍保留的现实约束：
  - NameResolver 未实现“并发去重锁”；测试通过“预热后并发仅走缓存”验证目标。

## 清单（新增/修改）
- 新增：
 - `tests/src/test/java/.../interfaces/rest/ApiControllerMockMvcMoreTest.java`
 - `tests/src/test/java/.../modules/kline/domain/service/impl/NameResolverImplMoreTest.java`
 - `tests/src/test/java/.../modules/kline/infrastructure/db/dao/KlineDaoMoreTest.java`
 - `tests/src/test/java/.../modules/kline/infrastructure/cache/RedisKlineCacheExtraTest.java`
 - `tests/src/test/java/.../modules/kline/infrastructure/db/repository/KlineRepositoryImplMoreTest.java`
 - `tests/src/test/java/.../util/RandomKlineDataGenerator.java`

## 新增任务（基于 comprehensive_testflow.md 补充）
- 黑盒安全用例：
  - [ ] SQL 注入 payload（stockcode="';DROP TABLE--"）：断言安全处理（不回显恶意内容；根据策略 200 或 400）。
  - [ ] XSS payload（stockcode="<script>alert(1)</script>"): 断言响应不包含注入内容。
- 功能边界用例：
  - [ ] 未知股票/市场：返回 200 且 `list` 为空（MockMvc 与 HTTP 集成各补 1 例）。
  - [ ] MockMvc 层补充 `startTs=endTs` 等值边界用例。
- 并发与缓存：
  - [ ] RedisKlineCache 读写并发一致性冒烟（多线程 put/get 同 key 观测顺序与数据完整）。
- 覆盖率目标：
  - [ ] 将 NameResolverImpl / KlineResponse / PricePoint 分支覆盖率提升至 ≥ 90%。
- 非适用项说明：
  - [ ] DAO 事务回滚（当前为内存 DAO，无事务，标注为 N/A）。

以上实现完成后，执行一次完整测试并确认通过。
