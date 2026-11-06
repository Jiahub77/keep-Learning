# Java 学习笔记

更新日期：2026-11-05。

## 学习背景与目标

教学协作约定（2026-11-05）：用户明确要求“教我写，不是你写”。后续业务代码由学习者亲自修改；导师每次提供一个小步骤、解释和练习，读取代码及验证结果，继续同步笔记。除非用户明确要求代改，不直接实现业务代码。此前误代写的 CRUD、额外依赖和测试配置已撤回，保留用户原有代码及数据库配置；没有运行 CRUD 测试或修改数据库，不能记为 CRUD 实践完成。

已有前端、数据库经验和少量 Java 基础。目标是工作中能够独立开发、维护 Java 后端模块，每周投入 10～15 小时。

## 文档导航

1. [12 周学习计划](01-学习计划.md)
2. [Java 基础与商品对象](02-Java基础与商品对象.md)
3. [List 集合与方法返回值](03-List集合与方法返回值.md)
4. [Map 查询与条件判断](04-Map查询与条件判断.md)
5. [Stream 筛选、统计与排序](05-Stream筛选统计与排序.md)
6. [异常处理与接口入门](06-异常处理与接口入门.md)
7. [易错点与复习清单](07-易错点与复习清单.md)
8. [接口进阶、泛型、枚举、record 与 Optional](09-接口进阶泛型枚举与Optional.md)
9. [Java 基础知识](08-Java基础知识.md)：环境与运行、类型、运算符、流程控制、数组、字符串、方法、面向对象、集合、异常、金额与日期。
10. [Spring Boot 启动类、Controller 与 GET 接口](10-Spring-Boot启动类Controller与GET接口.md)
11. [MyBatis：Mapper 与 SQL 的对应关系](11-MyBatis-Mapper与SQL对应关系.md)

基础语法不熟悉时，可先阅读第 8 篇的第 1～6 节，再回到商品案例。新增资料用于补充学习，不代表已完成实践。

## 当前进度

测试阶段完成记录（2026-11-06）：用户授权导师补测，Maven测试31项通过（含原有启动测试），当前MySQL HTTP验证29项断言通过；10并发争抢5件库存得到5次200、5次409和库存0，独立商品id=4已清理。H2真实SQL执行后抛异常的事务回滚验证通过，MySQL回滚未验证。未发现需修改业务代码的阻塞问题；仅新增测试文件及测试所需POM配置，src/main未代改。测试由导师实现，不代表学习者独立编写测试已掌握。完整证据见[商品模块测试记录](12-商品模块测试记录.md)。本段优先于下方历史待测记录。

全局异常409分支验证（2026-11-06）：用户提供 POST /products/decrease-stock/1 请求体98，返回 HTTP409、text/plain;charset=UTF-8 和“库存不足”。结合前次400结果，全局异常处理两个分支均按用户反馈运行通过。概念学习有、代码与编译有、运行有、项目实践有（全局400/409基本链路）。本次没有再次提供库存查询，不新增认定库存检查已完成。回滚实验按用户要求跳过；并发、自动化测试和其他边界仍未完成。下一步建议先学习自动化测试中 Arrange/Act/Assert，用纯单元测试验证非法分页参数不会调用Mapper，不依赖数据库。尚未开始编写或执行测试。

全局异常400分支验证（2026-11-06）：用户提供 GET /products?page=0&size=2 返回 HTTP400、text/plain;charset=UTF-8 与“页码必须大于等于1”。结合此前局部处理器已删除的源码检查，全局参数异常处理按用户反馈运行通过。概念学习有，编译有，400运行和项目实践有；迁移后409仍待验证，其他分页边界未全部验证。

全局异常迁移复核（2026-11-06）：用户已将400/409两个处理方法移至 exception.GlobalExceptionHandler，@RestControllerAdvice 与导入正确，Controller 已无局部 @ExceptionHandler。Maven 3.8.8 -DskipTests compile 退出码0，编译通过；未确认重启后的全局处理HTTP响应，不能沿用迁移前409结果作为迁移验证。概念学习有、自动注册判断题答对；迁移代码与编译有，迁移后运行待验证。事务回滚实验仍按用户要求跳过。

分页校验完成复核（2026-11-06）：page 下限、size 1至100、long offsetValue 计算、Integer.MAX_VALUE 范围检查与安全转 int 均已由学习者补齐，静态检查正确。Maven 3.8.8 -DskipTests compile 退出码0，编译通过；新校验 HTTP 400 分支未验证，自动化测试未执行。先 int 乘法再赋 long 是否避免溢出的题尚未回答，不记为答题通过。下一步整理 Controller 局部异常处理为全局异常处理，尚未迁移。

分页校验静态复核（2026-11-06）：学习者已添加 page<1、size<1 时抛 IllegalArgumentException，写法正确；size 上限和 offset 整数溢出尚未处理。指导学习上限设为100（项目约定），使用 long 中间计算并检查 Integer.MAX_VALUE 后转为 int，保持 Mapper 参数不变。本轮未编译或请求验证。用户明确跳过真实事务回滚实验，记为概念/答题有、回滚运行验证没有；源码当前未见临时抛异常实验分支。

事务回滚预测题（2026-11-06）：用户正确回答有效 @Transactional 事务内库存 97 扣减 3 后抛出未捕获 RuntimeException，事务结束后仍为 97。下一步由用户在 decreaseStock 的 rows==0 检查之后、成功 return 之前临时添加 quantity==3 时抛 RuntimeException 的实验分支；用练习商品先查库存、请求扣减 3、再查库存验证。默认预期 HTTP 500，数据库应保持请求前库存；必须移除临时分支并重启。仅提供教学步骤，未代改代码或执行写请求。回滚概念与答题有，实验编译运行、回滚项目验证尚无。

库存不足分支运行通过（2026-11-06）：用户提供 POST /products/decrease-stock/1 请求体 98，响应 HTTP 409 和库存不足提示；随后 GET 商品 1 的 stock 仍为 97。源码复查确认扣减方法已使用 findByIdForUpdate，并保留 @Transactional。概念学习有、代码有、运行有、项目实践有（成功扣减与库存不足两分支）；本轮未独立执行 Maven 编译或写请求。库存不变由条件 UPDATE 未匹配解释，不能据此证明已成功更新后回滚。非法数量、商品不存在、扣减后异常回滚及并发仍待验证。下一步阅读判断条件未匹配与事务回滚的区别。

库存异常修改复查（2026-11-06）：InsufficientStockException 类、Service rows==0 抛异常、Controller 返回 409 的处理器均已写入。扣减方法仍使用 findID，未改为 findByIdForUpdate；指导仅修改这一行，保留 @Transactional，避免存在性检查后并发删除导致错误报库存不足。此次仅静态核对，未编译或发送库存请求；409 运行验证仍待完成。

库存不足 409 教学（2026-11-06）：当前源码还没有自定义业务异常。指导学习者创建 InsufficientStockException extends RuntimeException；扣减 Service 保留 @Transactional，改用已有 findByIdForUpdate 锁定存在记录，缺失仍返回 Optional.empty，条件 UPDATE 返回 0 时抛库存不足异常；Controller 添加专用 @ExceptionHandler 返回 409/异常消息。锁定读取用于防止存在性检查后商品被并发删除，单条条件 UPDATE 本身负责库存不足时不扣减。仅提供指导，未代改 Java；讲解有，答题待反馈，新增异常及 409 编译运行没有，项目实践未完成。成功扣减 POST 原始状态码仍未提供。

库存扣减首次反馈（2026-11-06）：用户提供商品 1 查询 JSON，stock=97，与此前库存 100 扣减 3 的预期一致。扣减效果按用户反馈记录为已验证；POST 原始状态码未提供。概念学习有，编译有，运行有（用户反馈），项目实践有（成功扣减基本链路）；库存不足 409、非法数量、事务回滚及并发仍待验证。终端 JSON 后的 % 是 zsh 对无末尾换行输出的提示，不属于 JSON。

库存扣减修正复核（2026-11-06）：SQL 已改 AND，Controller 已改 POST /products/decrease-stock/{id} 并绑定 id，Service 已检查影响行数。Maven compile 已通过；未发送库存写请求，扣减运行、回滚和并发均未验证。仍有业务语义问题：库存不足返回 Optional.empty 会被映射为 404；这是上一轮导师简化建议造成，应后续区分商品不存在（404）与库存不足（409），普通存在性查询还需考虑并发删除。浏览器地址栏为 GET，不能验证 POST 扣库存。

库存扣减代码检查（2026-11-06）：发现三处需修正后再运行：Mapper 条件 SQL 误写 ADD，应为 AND（否则属于 SQL 运行时错误）；Controller 路径没有 `{id}` 却使用 @PathVariable，应改为包含 id 的路径；Service 忽略 decreaseStock 返回行数，库存不足时也会错误返回成功。当前 Maven 编译可通过，但库存接口尚未启动验证。

分页响应运行验证（2026-11-06）：用户访问 GET /products?page=1&size=2，返回 list 2 条、total=3、page=1、size=2，分页响应项目实践验证通过。用户当前响应还包含 id 和 price_in_cents，说明其 Response DTO 已扩展；本次不重新推断字段实现。下一步开始条件库存扣减 Mapper，库存扣减尚未接入项目。

分页响应 DTO 完成（2026-11-06）：用户已创建 ProductPageResponse，并修改 Service/Controller 返回分页对象；DTO 字段名为 list（含当前页商品），不是示例中的 items，但含义正确。Maven 3.8.8 compile 退出码 0。尚未重启并验证 JSON 响应。下一步请求 GET /products?page=1&size=2，检查 list、total、page、size。

分页第二页验证（2026-11-06）：用户确认 GET /products?page=2&size=2 结果正确。分页第一页、第二页已运行；当前响应仍只有商品数组，没有 total/page/size 元数据。下一步添加 Mapper 的 COUNT(*) 查询。

分页参数修正与验证（2026-11-06）：此前 [] 原因是 LIMIT/OFFSET 参数错位；用户已改为 Mapper 接收 size、offset，Service 计算 offset=(page-1)*size 并传入，Controller 默认 page=1、size=2。Maven 3.8.8 compile 通过；导师只读请求 GET /products?page=1&size=10 返回 3 条数据库商品。分页第一页已运行，第二页与参数校验待验证。

文件职责复习（2026-11-06）：已按当前 BizVision 源码说明 ProductController、ProductService、ProductMapper、ProductEntity、CreateProductRequest、ProductResponse、主启动类、application.properties、pom.xml 的职责和请求链路。当前列表 Controller 已存在，列表运行验证仍待完成；分页与库存扣减尚未开始。

Service 列表转换复核（2026-11-06）：用户已完成 List<ProductEntity> 到 List<ProductResponse> 的 for 循环转换，方法命名为 findAll，逻辑正确。导师执行 Maven 3.8.8 -DskipTests compile，退出码 0，编译通过；尚未启动或请求列表接口。下一步在 Controller 添加 GET /products。

列表 Mapper 复核（2026-11-06）：用户已添加 java.util.List 与 findAll()，并正确回答 3 条记录 size=3、无记录返回空列表。Mapper 代码尚未编译运行。下一步手写 Service 的 List<ProductResponse> 转换方法。

列表查询当前步骤：重复删除时不会调用删除 Mapper 的题答对。开始讲解真实数据库列表 Mapper：返回 List<ProductEntity>、按 id 排序、无结果返回空列表。概念学习有；本轮列表答题待反馈；列表代码尚未编写、编译运行没有、数据库列表项目实践没有。此前模拟列表运行不等于数据库列表已完成。

删除接口验证（2026-11-05）：用户先反馈“删除了”，随后提供再次 DELETE /products/2 的 HTTP/1.1 404、Content-Length: 0。首次删除成功按用户反馈记录，首次原始状态码未提供；重复删除不存在分支已由用户请求结果验证。概念学习有、删除行数答题有、编译有（此前 Maven compile 通过）、运行有、项目实践有（基本删除链路）。商品按 ID 查询、新增、更新、删除已有基本实践，不代表完整模块、自动化测试、分页或事务回滚/并发验证完成。下一步复习 404 的 Service → Controller 返回链路，再恢复真实数据库商品列表。

删除修正复核（2026-11-05）：Controller 已改为 @DeleteMapping，Service 已删除多余 request 参数，当前使用 Long id，可接收 Controller 的 long 自动装箱。导师执行 Maven 3.8.8 的 -DskipTests compile，退出码 0，编译通过；没有启动新服务、执行删除或运行测试。保留的 MyBatis Delete 导入与 resilt 拼写是清理建议，不是编译错误。概念学习有、答题有；编译有，删除运行没有；删除代码已编写，项目接口验证未完成。下一步学习者重启后删除练习商品并验证重复删除。

删除最新检查：行数题 1、0 答对；用户已编写删除三层代码。当前 Controller 误用 MyBatis @Delete，且 Service 声明两个参数而调用只传一个，需用户修正。现有 Optional<Long> + 成功 200/id、缺失 404 设计可保留。概念学习有、代码编写有；尚未通过本轮编译或删除运行验证。

最新验证（2026-11-05）：用户反馈商品 2 的 price_in_cents 为 10900，不存在商品的 PUT 返回 HTTP/1.1 404；结合此前 GET 的名称与库存，更新效果及不存在分支已验证（价格和 PUT 状态来自用户反馈）。成功 PUT 原始响应、事务回滚与并发仍未验证。下一步由学习者手写删除 Mapper；删除尚未编译运行或项目实践。以下逐次记录为历史状态，以本段及当前进度表为准。

更新流程最新检查：不存在与相同内容两题答对；锁定查询 Mapper 已写入，但 Service 漏加 @Transactional。用户自行写的更新 Controller 用了 POST、RequestParam 和 201，且直接返回 Optional。已指导修正为事务更新及 PUT + PathVariable + 200/404；等待学习者修改与实际请求验证，尚未完成更新实践。

更新编号修正已静态检查：updateProduct 已传入 id 并设置到 Entity，toEntity 已为 private。下一步手写事务中的 FOR UPDATE 存在性检查，返回 Optional<Long>，后续由 PUT Controller 映射 200/404。讲解已提供，更新运行及锁定/回滚行为尚未验证。

Service 重构最新检查：createProduct 已复用 toEntity，“toEntity 不入库”答对；尚未独立运行重构后的新增。学习者自行添加的 updateProduct 缺少 id 参数和 entity.setId，导致 UPDATE 无目标编号；已指导先补编号传递，再处理存在性与事务，暂不接 PUT、不执行更新。完整更新实践尚未完成。

更新当前步骤：Mapper.update 已由用户编写，WHERE 作用两题答对；真实 UPDATE 未执行。下一步先把 Service 现有校验与 Entity 赋值提取为 private toEntity，新增方法复用它，为随后更新复用做准备。用户亲自修改，导师只提供指导；不记录更新接口完成。

新增最新反馈：用户报告“成功”；导师检查 POST/异常处理源码已写好，并只读请求 GET /products/2，确认 HTTP 200 与 {"name":"新增练习杯","stock":0}。新增成功按用户反馈记录，201 和 POST 返回的原始 id 未另提供；存在记录的查询已独立验证。用户已反馈缺失 stock 返回 400 和“库存必须填写且不能为负数”，该错误分支按用户反馈验证通过；未独立查询记录数。下一步手写 Mapper 更新方法，UPDATE 尚未执行。

新增 Service 最新状态：createProduct 源码已核对，缺失库存和零库存两题均回答正确。下一步学习者接 POST /products 与 Controller 参数异常处理，分别验证成功返回 201/新 id、缺失库存返回 400。Service 已编写，新增链路尚未实际执行，不标记新增实践完成。

新增 Mapper 最新检查：insert 声明与 @Options 主键回写配置正确，学习者正确回答受影响行数为 1、回写 id 为 12。方法已编写，真实插入仍未执行。下一步手写 Service.createProduct：校验请求、创建 Entity、调用 Mapper、返回生成 id；POST 和参数异常的 HTTP 映射尚未接入。

新增功能当前进度：CreateProductRequest 已由学习者创建，三个字段及 getter/setter 静态检查正确；Integer 缺失值为 null 的题答对。尚未通过 POST 或插入验证。下一步手写 Mapper.insert 与自增主键回写，教材已提供，运行与答题待完成。

最新实践（2026-11-05）：用户修复 Mapper 接口、SQL 空格与实例方法调用后，GET /products/1 返回 {"name":"iPhone 17 pro","stock":100}。导师核对 Service → Mapper SQL 链路并实际请求确认 HTTP 200 和该 JSON。ProductEntity 已实际参与映射，真实商品详情查询运行通过；价格字段尚未通过接口核实。当前列表方法已由用户移除，增改删尚未实现。

查无对象的返回值初答 0，已纠正为 null、尚未复答；AS 不改表列名答对。下一步由学习者创建 CreateProductRequest，逐步实现新增，导师不代改业务代码。

| 知识点 | 概念学习 | 讲解与答题 | 编译运行 | 项目实践 |
|---|---|---|---|---|
| Java 类型、类与对象、封装、引用、方法 | 有 | 有 | 没有 | 没有 |
| List、Map、空值判断、布尔值与短路求值 | 有 | 有 | 没有 | 没有 |
| Stream 的 `filter`、`map`、统计、排序 | 有 | 有 | 没有 | 没有 |
| 异常基础、`throw`、`try-catch` | 有 | 部分；练习曾跳过 | 没有 | 没有 |
| 接口、多态、构造器注入、多个实现、`default` | 有 | 有 | 没有 | 没有 |
| 泛型、泛型方法、`? extends` | 有 | 有 | 没有 | 没有 |
| enum、record、Optional | 有 | 有 | 没有 | 没有 |
| Maven | 有；本阶段不重复讲 | 有 | 导师 compile 成功；用户也已确认编译通过 | 没有完整后端项目实践 |
| Spring Boot 启动类、Controller、GET/POST、Service、构造器注入 | 有 | 有 | 有：启动、GET、POST 新增及 PUT 更新链路；成功写请求原始状态码未全部记录 | 有：模拟商品查询分层；数据库商品业务未完成 |
| JSON、路径参数、查询参数、`@RequestBody`、HTTP 状态码 | 有 | 有 | 有：查询参数、JSON、路径参数、请求体及部分 400/404 分支 | 有：真实商品查询、新增和更新的请求绑定 |
| DTO：Request DTO、Response DTO、Entity 的区别 | 有；已通过示例学习 | 有 | 有：Request、Entity、Response 参与请求与映射 | 有：用户编写 DTO 及转换 |
| MyBatis Mapper、SQL 参数绑定、结果映射、CRUD | 有 | 有；null 返回值待复答 | 有：真实详情、新增、更新及更新不存在返回 404 | 有：查、增、改、删基本链路；完整测试待完成 |
| MySQL 数据源、JDBC 驱动、表结构、分页 | 有部分概念 | 部分 | Java 连接检查及商品表结构检查成功（用户反馈）；分页未验证 | 有：练习库、建表与商品查增改；分页未完成 |
| 参数校验、全局异常、事务、库存扣减 | 有部分概念 | 部分 | 有：缺失库存返回 400；回滚、并发及库存扣减未验证 | 有：手动校验与 Controller 局部异常处理；全局异常及事务专项测试未完成 |

结论：主要概念已有阅读与答题。2026-11-04 导师使用 JDK 8 和 Maven 3.8.8 成功编译 BizVision 的 4 个主源码文件（BUILD SUCCESS），用户随后确认编译通过，并反馈访问 /hello?name=Java 得到 Hello Java。已完成模板启动与首次 GET 访问；不表示所有笔记示例已运行或商品业务实践完成。2026-11-05 用户已确认 Java 数据库连接检查成功；商品表结构随后获用户确认；真实 MyBatis 商品详情、新增和更新已验证；删除及完整测试仍待完成。

## 示例约定

### 转入 BizVision 实践：环境核对（2026-11-04）

- 本次重新检查确认 Java 工程已位于 `/Users/mac/disk2/www/keep-learning/BizVision`（单层 BizVision 目录），以此路径为准。此前检查的外部同名 Node.js + React 项目与本次 Java 实践无关。
- 本工程已具备 pom.xml、启动类、示例 Controller 和启动测试源码。POM 指定 Java 1.8、Spring Boot 2.7.18、MyBatis starter 2.3.0；IDE 项目 SDK 为 zulu-1.8。测试源码存在不代表已运行通过。
- 静态检查发现：已引入 MyBatis，但没有配置数据源 URL 和 JDBC 驱动，也未提供内存数据库依赖，默认数据源自动配置预计会导致启动失败；尚未实际启动验证。Spring Boot Maven 插件还设置了 skip=true，后续制作可执行 jar 时需处理，不影响当前先从 IDE 启动的教学目标。
- 当前终端 `java -version` 与 `javac -version` 均成功返回 1.8.0_442（Zulu 8）；此前“尚未安装 Java 环境”应作为历史状态。`/usr/libexec/java_home -V` 未识别 Java Runtime，环境发现配置仍待核对。
- 已通过完整路径验证 `/Users/mac/.sdkman/candidates/maven/3.8.8/bin/mvn`：Maven 3.8.8、Java 1.8.0_442。直接执行 mvn 仍提示找不到命令，属于当前终端 PATH 未加载；IDE 自带 Maven 版本另为 3.9.16，学习工程应选已安装的 3.8.8。
- 导师已执行 Maven compile，成功编译 4 个主源码文件；用户随后确认编译通过，并反馈 GET /hello?name=Java 返回 Hello Java。概念学习有；编译运行有（仅限当前模板）；项目实践有入门操作，商品业务实践未完成。测试源码仍未验证执行。
- 已检查到 application.properties 添加了临时排除 DataSourceAutoConfiguration 的配置；接 MySQL 时删除排除项并配置驱动和数据源。本次运行成功是在临时排除数据源的前提下，不代表数据库可用。
- 用户已反馈 name=BizVision 与不传 name 两次访问结果，查询参数和默认值实践通过。第一条反馈写成小写 hello，现有代码实际应返回 Hello BizVision，已提醒大小写；第二条为 Hello unknown user。
- 学习者已创建 ProductController 并运行：反馈 /products/1 返回“保温杯，库存 20”，/products/2 报错。导师检查源码并通过 HTTP 请求验证 /products/1 为 200 和固定文本，/products/2 为 404 Not Found。本接口的代码创建、编译运行和最小接口实践已有证据；不是数据库查询或完整商品模块实践。
- 本次 404 含义题初答错误，已解释路径未匹配不等于数据库商品不存在。随后正确回答“返回库存 20 不能说明数据库库存为 20，因为当前没有数据库”，本次固定响应与数据库数据的区别复答通过。
- 商品 JSON 实践完成：学习者已创建普通 ProductResponse 类并修改 Controller，反馈响应为 {"name":"保温杯","stock":20}，正确回答修改第二个构造参数会改变 stock。导师核对源码并请求验证 HTTP 200、Content-Type 为 application/json、响应内容一致；尚未实际改为 8 验证。
- Service 分层实践通过：学习者已创建 ProductService 并完成 Controller 构造器注入，反馈 JSON 不变且由 Spring 注入；导师核对源码并实际请求得到 HTTP 200 与一致 JSON。Controller 方法取名 findProduct 不影响路径映射。概念、答题、该功能编译运行和最小分层实践有；数据仍为模拟值。
- 路径参数实践通过：用户反馈 /products/7 返回 {"name":"模拟商品7","stock":20}，正确判断 abc 无法转换为 long，不进入 Service。导师核对源码和请求：/products/7 为 200，/products/abc 为 400。实际名称无空格，符合用户代码。
- Optional 与 ResponseEntity 实践已运行：学习者正确修改代码，但怀疑服务未启动。导师验证 Maven compile 为 BUILD SUCCESS（7 个主源码文件）、Java 进程监听 8080，/products/7 为 200 和商品 JSON，/products/8 为 404 且 Content-Length 为 0。代码无需修复；本次是预期的业务响应，不是编译或启动失败。该功能编译运行和最小实践有，404 空响应与连接失败的区别已解释、待确认理解。数据库、CRUD、校验、分页、事务及测试仍待实践。

默认示例优先使用 Java 8/17 都能理解的写法。商品价格统一以整数“分”保存，字段名为 `priceInCents`，读取方法为 `getPriceInCents()`。

最新答题反馈：用户已报告 /products/7 的商品 JSON 与 /products/8 的 404，导师再次请求验证前者 200、后者 404 且空响应体。执行链路题初答错误，纠正后复答 /products/9 会进入 Service，并正确指出 ResponseEntity.notFound().build() 是构造 404 的语句。尚未直接回答所在类名，已补充：由 ProductController 调用 ResponseEntity 构造响应。该功能已有编译运行与最小实践，数据库实践没有。用户明确要求跳过调试，标记为未完成、后补，不再要求本阶段反馈断点结果。2026-11-05 商品列表实践通过：用户反馈 JSON 数组，导师验证 HTTP 200 与内容一致；本次复核 Controller 直接返回 List<ProductResponse>，与实际响应一致。minStock 筛选代码已完成，导师实际请求验证 minStock=10 返回 200 和仅保温杯，minStock=30 返回 200 和 []，空列表响应已验证。用户最新反馈已更正为仅保温杯和空数组，与两次实测一致，筛选结果确认通过。minStock 校验已运行，用户正确反馈 -1 为 400、30 为 200。曾将条件误写为 <= 0；学习者现已改为 < 0 并反馈 0 和默认请求都返回两件商品。导师复验 -1 为 400、0 和默认请求为 200 且返回两件商品、30 为 200 和 []，边界修正通过。下一步：确认可用的练习 MySQL 环境，再学习数据源连接；数据库实践仍未开始。

## JDK 版本与实际项目

### 本机 MySQL 准备（2026-11-05）

最新建表反馈：用户确认 products 表具备四列、id 的 Extra 为 auto_increment，并正确回答 id 自增所以新增通常无需手填。建表实践按用户反馈记为完成；尚未独立查询数据库核实。下一步在 Navicat 插入两条练习商品并 SELECT 查询实际 id；插入、查询结果待反馈，MyBatis 查询与真实数据库商品接口仍未完成。下方旧的“建表待完成”属于历史阶段记录。

最新里程碑：用户在添加 DatabaseConnectionCheck 并按要求重启后反馈“成功了”。导师核对类确实执行 SELECT DATABASE() 后打印库名；连接检查按用户反馈记为完成（未独立读取运行日志）。JDBC 已有实际 SQL 执行实践，不能扩展为 MyBatis 商品查询完成。下方认证和连接待验证均为历史记录，以本条为准。下一步暂停临时检查的 Component 注册，并在练习库创建 products 表；建表及其后 CRUD、分页、事务尚待完成。

当前状态（优先于下方历史排查记录）：指导定向更新 root@127.0.0.1 密码并重试 TCP 后，用户反馈“可以连上了”，记录连接恢复（用户确认），未另提供客户端或改密日志，不据此声称已独立核实根因。此前认证待排查项目不再阻塞学习；Java JDBC 连接仍未配置、未验证。

用户确认 SELECT DATABASE() 返回 bizvision_learning，练习库创建与选库按用户反馈记为完成。数据源配置后用户回复“可以了”；导师检查到 JDBC 驱动依赖和数据源配置键已加入，临时数据源排除项已删除，未读取密码值。用户已确认环境变量配置操作完成，但尚无 Java 执行 SQL 的结果；不据此认定数据库连接已验证。下一步添加临时 DatabaseConnectionCheck，启动时执行 SELECT DATABASE()，待用户反馈输出；用实际 SQL 结果验证所连数据库。

无默认配置对照结果：新 socket 密码登录成功，同一密码的 TCP 登录仍返回 1045。下一步指导用户仅更新 root@127.0.0.1 的密码后重试 TCP；这是待执行的定向修复，不表示已确定根因。密码仅在本机输入，不回传；改密、TCP 修复与 Java JDBC 连接均尚未确认。

认证排查最新结果：root@localhost 与 root@127.0.0.1 均使用 caching_sha2_password，均未锁定或密码过期；不属于 auth_socket 认证。两个账号独立，但密码是否相同及失败 TCP 的实际账号匹配尚不能确定。下一步用忽略默认配置和 login-path 的新 socket/TCP 连接对照，保留当前成功会话，不修改账号。Java JDBC 连接仍未完成。

已收到成功会话的诊断结果：UNIX socket /tmp/mysql.sock，USER() 与 CURRENT_USER() 均为 root@localhost，服务端 9.6.0 Homebrew、@@port 为 3306。下一步只读查看 root/匿名账号的 Host、plugin、锁定与密码过期状态；TCP 认证失败原因尚未确定。不读取密码散列，不修改账号；Java 数据源连接仍未完成。

补充：用户显式 TCP 连接 127.0.0.1:3306 仍返回 1045，默认终端会话此前成功；连接差异尚未查明。下一步在成功会话读取 status、USER()、CURRENT_USER()、@@port 和 @@socket，确认实际连接方式及账号，不先重置密码或修改授权。Java 连接仍未完成。

最新连接状态：Navicat root 登录报 1045，但用户已反馈在终端通过 mysql -u root -p 成功登录，服务端版本确认是 9.6.0 Homebrew。说明用户拥有该终端连接可用的密码；无需据此前空密码测试失败重置账号。终端默认连接可能使用 Unix socket，实际传输方式尚待确认。下一步以相同密码显式测试 TCP 127.0.0.1:3306，与 Navicat 配置对齐；不收集密码。没有重置密码或修改数据库。Java 数据库连接仍未完成。

用户确认可以连接本机 MySQL。导师只读检查发现 mysqld 正在监听 3306，mysql 客户端位于 /opt/homebrew/bin/mysql。此证据仅证明服务在监听，不代表 Spring Boot 已认证并连接成功。当前工程仍缺少 MySQL JDBC 驱动和数据源配置，且仍临时排除了 DataSourceAutoConfiguration。

下一步：用户在自己的数据库客户端查询 SELECT VERSION() AS mysql_version, DATABASE() AS current_database;，反馈服务端版本以及拟用于练习的数据库名。DATABASE() 为 NULL 仅表示当前未选库。确认后再补驱动、数据源和连接验证，密码只在本机配置，不要求发送到对话中。当前 Java 数据库连接、SQL 执行及数据库项目实践均未完成。

2026-11-04 用户确认 Java 8 工作项目版本：JDK 8、Spring Boot 2.7.18、Maven 3.8.8。后续 Java 8 教学以此为基线；参数校验使用 `javax.validation.*`，Servlet 使用 `javax.servlet.*`。Spring/MyBatis 自己的注解仍使用各自的包名，不是所有导入都改为 `javax.*`。此信息仅确认项目技术栈，不代表本机已安装环境或运行项目。Java 17 项目的具体框架版本尚未确认。

本次复习：多个 Mapper 参数的 `@Param` 绑定已有文档与讲解，已答题；参数找不到属于 MyBatis 运行时错误的判断正确。查询结果题曾漏看 `AND stock >= 1`，解释后正确回答 `search("杯", 0)` 返回保温杯和咖啡杯，复答通过。参数命名题回答正确：`@Param("stock")` 对应 `#{stock}`。条件库存扣减题两题正确：初始库存各为 5，扣减 3 返回 1、剩余 2；扣减 8 返回 0、库存仍为 5。返回 0 的含义判断正确：商品不存在也会返回 0，不能仅据此认定库存不足。Service 异常流程题两题正确：rows 为 0 抛异常、不打印成功；rows 为 1 不抛该异常、打印成功。事务回滚对照题结果均正确：事务回滚后库存为 5；无事务且扣减已自动提交时，后续异常不会撤销扣减，库存为 3。已讲解 throw 中断执行与事务回滚的区别；尚未独立口述原因。事务边界题回答正确：扣库存事务已提交后，不能再通过该事务回滚。全局异常响应题回答正确：状态码 409、响应体为异常消息。自定义业务异常两道判断题回答正确，详见第 11 篇末尾；抛出业务异常的单行语句经讲解与纠正后已写对；数量校验条件 quantity <= 0 填写正确；数量校验与库存扣减综合阅读题两题结果正确；下一步：try-catch 对事务回滚的影响（尚未讲解或答题）。概念学习：有；编译运行：没有；项目实践：没有。

学习示例原先按 JDK 21 编写，但实际项目使用 Java 8 和 Java 17，后续按兼容性分层：

| 版本 | 教学策略 |
|---|---|
| Java 8 | 作为业务代码的兼容基线；使用普通类代替 `record`，使用 `Collectors.toList()` 代替 `Stream.toList()`，不使用文本块、`var` 和新版本 `switch` 语法 |
| Java 17 | 可以使用 `record`、文本块、`Stream.toList()` 等当前示例中的语法；Spring Boot 3 通常要求至少 Java 17 |
| Java 21 | 用于学习和后续实践环境；额外的新特性单独标注，不默认带入 Java 8 项目 |

之后每段代码会标注适用版本。涉及商品业务、Spring Boot、MyBatis 的核心思想会优先用 Java 8 也能理解的写法解释，再补充 Java 17/21 的简写。实际项目应以项目的 Maven 编译版本和 Spring Boot 版本为准：Java 8 项目通常使用支持 Java 8 的 Spring Boot 版本，Java 17 项目可以使用 Spring Boot 3。

示例是教学代码，默认集合参数不为 null、商品名称不为 null，且不考虑整数溢出和多线程竞争；不是生产级库存实现。

除特别说明外，各段练习独立执行。商品库存会随着操作变化，不要把不同练习的中间状态混用。现有 `demo.java` 未修改。


## 当前阶段路线：分页与库存扣减（2026-11-06）

### 文件职责复习

已按 BizVision 当前源码复习 Controller、Service、Mapper、Entity、Request DTO、Response DTO、启动类、配置文件和 pom.xml 的职责。概念学习有，已讲解；本次不代表新增运行实践。

### 商品模块状态

| 功能 | 概念学习 | 代码编写 | 编译运行 | 项目实践 |
|---|---|---|---|---|
| 查询单个商品 | 有 | 有 | 有 | 有 |
| 新增商品 | 有 | 有 | 有 | 有 |
| 更新商品 | 有 | 有 | 有 | 有 |
| 删除商品 | 有 | 有 | 有 | 有 |
| 查询商品列表（已改分页） | 有 | 有：Mapper、Service、Controller | 有：真实接口与自动化验证 | 有 |
| 分页查询 | 有 | 有 | 有：分页响应、边界自动化验证 | 有 |
| 条件库存扣减 | 有 | 有 | 有：成功/不足/并发，H2回滚 | 有：MySQL扣减与并发；MySQL回滚未验证 |

### 后续学习顺序

1. 先验证当前 `GET /products` 返回数据库商品列表。
2. 分页第一步只理解 `page`、`size`、`LIMIT`、`OFFSET`，再由学习者添加 Mapper 分页参数。
3. 接着添加分页响应中的 `items`、`total`、`page`、`size`。
4. 再学习库存扣减：先写带 `stock >= quantity` 条件的 UPDATE，再检查受影响行数。
5. 最后把库存扣减放入 Service 事务，补充商品不存在、库存不足和成功三种结果。

当前阶段仍不把“有分页示例”或“有库存扣减示例”记为项目实践完成。
