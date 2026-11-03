# 插件、Profile 与常用命令

## 1. 依赖和插件不是一回事

`dependencies` 是项目代码需要的库；`build/plugins` 是构建时使用的工具。给项目加 JUnit 依赖，不等于配置了合适版本的测试插件；配置 Compiler Plugin，也不等于给程序引入编译器库。

常见插件：

| 插件 | 作用 |
|---|---|
| maven-clean-plugin | 清理构建输出 |
| maven-resources-plugin | 处理资源 |
| maven-compiler-plugin | 编译 Java |
| maven-surefire-plugin | 单元测试 |
| maven-failsafe-plugin | 集成测试及结果检查 |
| maven-jar-plugin / maven-war-plugin | 创建 JAR / WAR |
| maven-install-plugin / maven-deploy-plugin | 本地安装 / 远程发布 |
| maven-dependency-plugin | 分析、复制依赖 |
| maven-help-plugin | 查看有效模型、属性、帮助 |
| maven-enforcer-plugin | 执行 JDK、Maven、依赖等规则 |
| maven-shade-plugin | 创建含依赖的 JAR、处理包重定位 |
| spring-boot-maven-plugin | Spring Boot 可执行归档 |

## 2. 固定插件版本并配置编译

放入 `build/plugins` 的示例：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <release>21</release>
        <encoding>UTF-8</encoding>
    </configuration>
</plugin>
```

也可以使用属性 `maven.compiler.release`，避免在多处重复配置。只写 source/target，不能像 release 那样同时限制目标版本的标准 API。

需要严格复现的项目应固定参与构建的插件版本，不依赖 Maven 发行版或远程元数据提供的默认版本。本目录示例固定了 clean、resources、compiler、surefire、jar、install、deploy 插件。

## 3. execution、phase、goal

下面示例放在 `build/plugins`，在 validate 阶段要求 Maven 3.9.x 范围和 JDK 21：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <version>3.5.0</version>
    <executions>
        <execution>
            <id>check-build-environment</id>
            <phase>validate</phase>
            <goals>
                <goal>enforce</goal>
            </goals>
            <configuration>
                <rules>
                    <requireMavenVersion>
                        <version>[3.9,4.0)</version>
                    </requireMavenVersion>
                    <requireJavaVersion>
                        <version>[21,22)</version>
                    </requireJavaVersion>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

一个插件可以有多个 execution，每个执行项可配置不同目标和参数。`id` 参与父子配置合并，应使用稳定、有含义的名字。

没有 execution 不一定代表插件不会运行：对 jar 项目，compile 阶段已有 Compiler Plugin 的默认绑定；对没有默认生命周期绑定的插件，通常需要显式 execution 或直接调用目标。

## 4. plugins 与 pluginManagement

pluginManagement 用于管理插件版本和默认配置；它本身通常不会给任意插件创建新的执行任务。

子模块在 plugins 中引用相应插件后，可以使用父项目管理的版本和配置。已经由 packaging 默认绑定的插件，也能从 pluginManagement 获得版本和配置，因此不能绝对说“必须手写 plugins 才能生效”。

父项目直接在 plugins 配置的可继承 execution，可能自动在子项目执行。需要统一版本但不想让所有模块自动增加任务时，优先考虑 pluginManagement，详见 [继承与冲突处理](10-继承与冲突处理详解.md)。

## 5. Profile

Profile 用于在特定条件下切换一组构建配置，例如启用集成测试、设置可选仓库或输出额外产物。不要为每台电脑复制一套 POM。

放在 project 下的示例：

```xml
<profiles>
    <profile>
        <id>integration</id>
        <properties>
            <run.integration.tests>true</run.integration.tests>
        </properties>
    </profile>
</profiles>
```

```bash
mvn help:active-profiles
mvn verify -Pintegration
mvn verify -Pintegration,coverage
```

最后一条仅在项目确实还定义了 coverage Profile 时使用。示例中 `run.integration.tests` 是自定义属性，**不会自动运行任何测试**，必须有插件配置读取它。

Profile 还可以通过 JDK、操作系统、属性或文件等条件激活。`activeByDefault` 在同一 POM 的其他 Profile 激活时会被停用，不能认为它永远叠加生效。多个 Profile 叠加后应查看 effective-pom。

生产环境数据库密码、令牌等应在运行时或 CI 秘密系统中注入，不应通过 Profile 打进 JAR。构建环境与应用运行环境是两个维度。

## 6. 资源过滤

在 `build` 中配置：

```xml
<resources>
    <resource>
        <directory>src/main/resources</directory>
        <filtering>true</filtering>
        <includes>
            <include>build-info.properties</include>
        </includes>
    </resource>
    <resource>
        <directory>src/main/resources</directory>
        <filtering>false</filtering>
        <excludes>
            <exclude>build-info.properties</exclude>
        </excludes>
    </resource>
</resources>
```

`build-info.properties` 中可写 `app.version=${project.version}`，在资源处理时替换。把过滤限定在确实需要的文本文件，避免二进制损坏和与应用自身 `${...}` 占位符冲突。不要过滤写入秘密信息。

## 7. 常用命令速查

以下均在项目根目录执行；带占位路径或模块名的命令需要替换后使用。

| 命令 | 用途 |
|---|---|
| `mvn -v` | 查看 Maven 使用的版本和 JDK |
| `mvn clean` | 清理输出 |
| `mvn compile` | 编译主源码 |
| `mvn test` | 编译并运行单元测试 |
| `mvn clean verify` | 从清理开始完成构建验证 |
| `mvn install` | 构建并安装到本地仓库 |
| `mvn dependency:tree` | 查看依赖树 |
| `mvn dependency:analyze` | 辅助分析使用/声明情况 |
| `mvn help:effective-pom` | 查看最终 POM |
| `mvn help:effective-settings` | 查看合并后的设置，注意敏感信息 |
| `mvn help:active-profiles` | 查看激活的 Profile |
| `mvn help:describe -Dplugin=compiler -Ddetail` | 查看插件目标与参数 |
| `mvn -f path/to/pom.xml verify` | 指定项目 POM |
| `mvn -s path/to/settings.xml verify` | 指定用户设置 |
| `mvn -Dtest=AppTest test` | 使用 Surefire 选择测试 |
| `mvn '-Dtest=AppTest#addsMoney' test` | 选择测试方法，引用含特殊字符参数 |

`dependency:analyze` 主要分析字节码使用情况；反射、SPI、运行时驱动等可能被误判，不能把所有“未使用依赖”直接删除。

常见选项：

| 参数 | 作用 |
|---|---|
| `-Dkey=value` | 传入用户属性；是否影响行为取决于消费者 |
| `-Pname` | 激活 Profile |
| `-U` | 检查更新的快照和缺失 release |
| `-o` | 离线模式 |
| `-B` | 批处理模式，适合 CI |
| `-ntp` | 隐藏下载传输进度 |
| `-e` | 展示异常详情 |
| `-X` | 调试日志，分享前检查秘密信息 |
| `-q` | 降低输出量，排错时慎用 |
| `-T 1C` | 按 CPU 核心数并行构建模块，需评估插件线程安全 |

用于脚本和 CI 的独立目标也可以固定插件版本：

```bash
mvn org.apache.maven.plugins:maven-dependency-plugin:3.8.1:tree
```

`-D` 不是操作系统环境变量；`${env.NAME}` 才是 POM 中读取环境变量的一种方式。
