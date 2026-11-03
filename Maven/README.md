# Maven 学习笔记

更新日期：2026-10-29。

本目录从零梳理 Maven 的概念、安装、配置、构建和日常使用。示例以 **Maven 3.9.x、JDK 21、JUnit 5** 为基线；版本号是教学示例，不代表当前最新版。Maven 4 存在兼容性和行为变化，升级时应阅读对应版本的迁移指南。

## 文档导航

| 顺序 | 文档 | 主要内容 |
|---|---|---|
| 1 | [概述与核心功能](01-概述与核心功能.md) | Maven 解决什么问题、约定优于配置、构建过程、与其他工具的关系 |
| 2 | [安装与环境配置](02-安装与环境配置.md) | macOS、Linux、Windows、JAVA_HOME、settings.xml、Wrapper |
| 3 | [项目结构、POM 与 GAV](03-项目结构-POM与GAV.md) | 标准目录、坐标、POM、继承、Super POM、有效 POM |
| 4 | [依赖管理与仓库](04-依赖管理与仓库.md) | 依赖传递、scope、冲突、BOM、仓库、镜像、SNAPSHOT |
| 5 | [生命周期与项目构建过程](05-生命周期与项目构建过程.md) | 三套生命周期、phase、goal、测试、打包、安装、部署 |
| 6 | [插件、Profile 与常用命令](06-插件-Profile与常用命令.md) | 插件执行、版本固定、属性、Profile、资源过滤、命令速查 |
| 7 | [多模块与团队工程实践](07-多模块与团队工程实践.md) | 父工程、聚合、Reactor、模块选择、私服、CI、安全与可重复构建 |
| 8 | [常见问题与排查](08-常见问题与排查.md) | 下载失败、版本冲突、JDK、测试、打包、IDE 差异 |
| 9 | [完整实战与复习清单](09-完整实战与复习清单.md) | 可运行工程、逐步练习、预期结果、自测题 |
| 10 | [继承与冲突处理详解](10-继承与冲突处理详解.md) | 父子配置合并、覆盖、BOM、版本冲突、重复类、运行时冲突 |

## 建议学习路线

1. 阅读第 1～3 篇，理解 Maven、JDK、POM、坐标各自的职责。
2. 按第 2 篇安装环境，以 `mvn -v` 的实际输出为准。
3. 进入 [示例工程](examples/hello-maven)，按第 9 篇完成测试和打包。
4. 学习第 4～6 篇，能解释依赖从哪里来、命令执行了哪些阶段。
5. 学习多模块和排错，用 `dependency:tree`、`help:effective-pom` 解释真实项目。
6. 结合第 10 篇，重点练习父子 POM 合并和依赖冲突的定位、修复与验证。

## 先记住这些区别

| 概念 | 含义 |
|---|---|
| JDK | 提供 Java 编译、运行等工具；Maven 不能代替 JDK |
| Maven | 读取项目模型，解析依赖，按生命周期调度插件 |
| POM | `pom.xml` 中的项目模型：坐标、依赖、插件、模块等 |
| GAV | `groupId:artifactId:version`，定位一个版本的构件 |
| 生命周期阶段 | 构建流程中的位置，例如 `test`、`package` |
| 插件目标 | 真正执行工作的操作，例如 `compiler:compile` |
| package | 在 `target/` 生成项目产物 |
| install | 在完成前面阶段后，将项目产物安装到本地仓库 |
| deploy | 在完成前面阶段后，将项目产物发布到远程仓库 |

本目录中的“运行结果”是环境满足条件时的预期结果，不表示已经在当前机器执行成功。当前编写环境缺少可用 JDK 和 Maven，实战工程仍需按步骤实际运行验证。

## 官方参考

- [Maven 官方指南](https://maven.apache.org/guides/)
- [安装说明](https://maven.apache.org/install.html)
- [POM 参考](https://maven.apache.org/pom.html)
- [依赖机制](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html)
- [生命周期](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
- [Settings 参考](https://maven.apache.org/settings.html)
- [插件目录](https://maven.apache.org/plugins/)
- [Maven Wrapper](https://maven.apache.org/wrapper/)
