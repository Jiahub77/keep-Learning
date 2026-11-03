# 项目结构、POM 与 GAV

## 1. 标准目录与产物

| 路径 | 内容 | 构建中的去向 |
|---|---|---|
| `pom.xml` | 项目模型 | Maven 读取并合并为有效模型 |
| `src/main/java` | 业务 Java 源码 | 编译到 `target/classes` |
| `src/main/resources` | 配置等生产资源 | 通常复制到 `target/classes` |
| `src/test/java` | 测试源码 | 编译到 `target/test-classes` |
| `src/test/resources` | 测试资源 | 通常复制到 `target/test-classes` |
| `src/main/webapp` | 传统 WAR 的 Web 资源 | 参与 WAR 打包 |
| `target/surefire-reports` | 单元测试结果 | 供开发者和 CI 检查 |
| `target/` | 构建产物与中间文件 | 通常整体忽略版本控制 |

`resources` 中的文件不会因为扩展名是 `.java` 就自动参与 Java 编译。测试源码和测试资源默认不进入普通生产 JAR。

## 2. 最小 POM

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example.learning</groupId>
    <artifactId>hello-maven</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>
</project>
```

这是项目模型的最小示意，不是本目录推荐的完整构建配置；Java 21 工程还应固定编译和测试插件。完整文件见 [示例 POM](examples/hello-maven/pom.xml)。

`modelVersion` 是 POM 模型版本，不是 Maven 安装版本，也不是项目版本。本目录 Maven 3 示例使用 `4.0.0`。

## 3. GAV：构件的基本坐标

| 字段 | 含义 | 例子 |
|---|---|---|
| `groupId` | 组织或项目组标识 | `com.example.learning` |
| `artifactId` | 组内构件名称 | `hello-maven` |
| `version` | 构件版本 | `1.0.0-SNAPSHOT` |

合起来是：

```text
com.example.learning:hello-maven:1.0.0-SNAPSHOT
```

`groupId` 常使用反向域名形式，但它不是必须能访问的网址，也不强制等于 Java 包名。`artifactId` 通常与项目目录相近，但两者也不是硬性绑定。

在完整构件定位中还要考虑类型/扩展名和分类器：

```text
groupId:artifactId:version                  # 常用 GAV 表达
groupId:artifactId:type:classifier:version  # 依赖坐标的一种完整表达
```

普通依赖的 `type` 默认是 `jar`，classifier 默认无值。例如 `sources` 和 `javadoc` 常用于区分同一 GAV 的附属构件。

仓库文件示意：

```text
com/example/learning/hello-maven/1.0.0/
├── hello-maven-1.0.0.pom
├── hello-maven-1.0.0.jar
├── hello-maven-1.0.0-sources.jar
└── hello-maven-1.0.0-javadoc.jar
```

`type` 不是永远等于文件扩展名，例如 `test-jar` 类型通常对应 `jar` 扩展名和 `tests` 分类器。

## 4. packaging、type、classifier 的区别

- `packaging`：描述当前项目如何构建，影响默认插件绑定。省略时通常为 `jar`。
- 依赖的 `type`：描述要获取哪类构件，默认 `jar`。
- `classifier`：区分同一版本的不同附属产物，例如 `sources`。

常见 packaging：

| packaging | 用途 |
|---|---|
| `jar` | 普通 Java 类库或应用 |
| `war` | 部署到 Servlet 容器的传统 Web 应用 |
| `pom` | 父工程、聚合工程、BOM |
| `maven-plugin` | Maven 插件项目 |

`pom` 项目仍然有 POM 构件，可以执行生命周期；只是默认不会像普通 JAR 项目那样编译 Java 并生成主 JAR。

## 5. POM 中常见区域

| 区域 | 职责 |
|---|---|
| `<parent>` | 声明父项目，复用配置 |
| `<properties>` | 定义可引用属性 |
| `<dependencies>` | 声明项目需要的依赖 |
| `<dependencyManagement>` | 管理依赖默认版本等信息，不自动引入依赖 |
| `<build><plugins>` | 使用或配置构建插件 |
| `<build><pluginManagement>` | 管理插件默认版本和配置 |
| `<repositories>` | 声明依赖仓库 |
| `<pluginRepositories>` | 声明插件仓库 |
| `<distributionManagement>` | 声明发布目标仓库 |
| `<modules>` | 声明参与聚合构建的模块目录 |
| `<profiles>` | 按条件启用构建配置 |

## 6. 属性与版本集中管理

下面内容放入 `project` 内：

```xml
<properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <junit.version>5.11.4</junit.version>
</properties>
```

通过 `${junit.version}` 引用属性。`${project.version}`、`${project.groupId}`、`${project.basedir}` 则来自项目模型。

`maven.compiler.release` 需要适当版本的 Compiler Plugin 支持。它约束目标 Java 版本的语法、字节码和公开标准 API；不能让旧 JDK 编译未来版本，也不能保证第三方依赖兼容该 Java 版本。

## 7. 父 POM、继承与 Super POM

子项目可以声明：

```xml
<parent>
    <groupId>com.example.learning</groupId>
    <artifactId>learning-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>
<artifactId>learning-service</artifactId>
```

子项目可继承父项目的 `groupId`、`version`，因此无需重复填写。通常还会继承属性、依赖管理及可继承的构建配置。`artifactId` 需要子项目自己指定。

`relativePath` 默认是 `../pom.xml`，用于查找本地父 POM；`<relativePath/>` 关闭该相对路径查找，转由仓库解析父 POM。父 POM 坐标仍然必须正确。

即使没有显式父 POM，Maven 也有 Super POM 提供基础默认值。最终生效的项目模型还会综合父链、激活的 Profile 等信息，不能只看当前文件判断构建行为。

```bash
mvn help:effective-pom
mvn help:effective-pom -Doutput=target/effective-pom.xml
mvn help:evaluate -Dexpression=project.version -q -DforceStdout
```

输出文件放在构建目录更便于清理，生成文件时应确保父目录存在。`help:effective-settings` 可以查看合并后的设置；相关输出可能包含内部地址或其他敏感信息，不要未经检查公开上传。

## 8. 项目版本与输出文件名

普通 JAR 的默认名字通常是 `${artifactId}-${version}.jar`。可通过 `<build><finalName>...</finalName></build>` 修改本地输出名，但这不改变 GAV，也不改变安装、发布时使用的仓库坐标。

`1.0.0-SNAPSHOT` 表示开发快照；`1.0.0` 表示固定发布版本的命名约定。Maven 不会根据业务变化自动判断你应增加主版本还是次版本，版本策略由团队制定。
