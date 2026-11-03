# Docker 学习笔记

Docker 用镜像描述运行环境，用容器运行程序。开发时可以通过 Docker 启动 PHP、MySQL、Redis 等服务，减少不同机器之间的环境差异。

## 阅读顺序

1. [Docker 基础](./基础.md)：镜像、容器、端口、目录挂载、网络和 Compose。
2. [常用命令与排查](./常用命令.md)：启停、更新、日志、网络、数据与故障排查。
3. [新增 PHP 8.2 容器并切换 Laravel 站点](./新增PHP82容器并切换Laravel站点.md)：保留原配置、启动官方镜像、配置 Nginx 上游和回退。

当前电脑的 PHP 环境位于 `/Users/mac/disk2/php-server`，完整搭建步骤见 [Docker 从零搭建指南](../../../php-server/DOCKER从零搭建指南.md)。

## 最常用的工作流程

在包含 `compose.yaml` 或 `docker-compose.yaml` 的目录执行：

```bash
docker compose config --quiet   # 检查配置
docker compose pull             # 下载配置中指定的镜像
docker compose up -d            # 创建并后台启动容器
docker compose ps -a            # 查看状态
docker compose logs -f --tail=100 # 持续查看日志，Ctrl+C 退出查看
docker compose stop             # 停止服务，保留容器
```

`up -d` 在本地缺少镜像时会自动拉取；更新已有标签的镜像通常先执行 `pull`。容器运行不代表应用已经就绪，需要再检查日志和实际请求。
