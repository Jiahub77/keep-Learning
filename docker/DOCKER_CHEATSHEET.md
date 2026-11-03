# Docker 速查手册

> 一句话理解：**镜像是模板（只读），容器是模板跑起来的实例（可写）。**
> 同一个镜像可以起 N 个容器，互不影响。

## 目录

- [0. 三个核心概念](#0-三个核心概念)
- [1. 三部曲：写 → 构建 → 启动](#1-三部曲写--构建--启动)
- [2. 镜像操作](#2-镜像操作)
- [3. 容器操作](#3-容器操作)
- [4. 网络](#4-网络)
- [5. Docker Compose](#5-docker-compose)
- [6. 排错常用](#6-排错常用)
- [7. 八个高频坑](#7-八个高频坑)
- [附录 A：Node + React 项目的实际 Dockerfile](#附录-anode--react-项目的实际-dockerfile)
- [附录 B：一份可用的 docker-compose.yml](#附录-b一份可用的-docker-composeyml)

---

## 0. 三个核心概念

| 概念 | 是什么 | 类比 |
| --- | --- | --- |
| **镜像 Image** | 只读模板，包含代码 + 运行时 + 依赖 + 启动命令 | 类（Class） |
| **容器 Container** | 镜像的一个运行实例，有独立的进程、文件系统、网络 | 对象（Instance） |
| **数据卷 Volume** | 独立于容器生命周期的持久化存储 | 外接硬盘 |

删容器不删镜像，删镜像不影响正在跑的容器（但删完就没法再起新的了）。

---

## 1. 三部曲：写 → 构建 → 启动

### 1.1 编写 Dockerfile

放在项目根目录，文件名就叫 `Dockerfile`（无扩展名）。

```dockerfile
FROM node:22-alpine          # 1. 基础镜像
WORKDIR /app                 # 2. 工作目录
COPY package*.json ./        # 3. 先只拷依赖清单（利用缓存）
RUN npm ci --omit=dev        # 4. 装依赖
COPY . .                     # 5. 再拷源码
EXPOSE 4000                  # 6. 声明容器内端口（仅文档作用）
CMD ["node", "server.js"]    # 7. 启动命令
```

> 第 3、4 步和第 5 步分开写是**故意的**：只要 `package.json` 没变，改代码不会触发重新装依赖。

### 1.2 构建镜像

```bash
docker build -t 镜像名称:版本号 .
```

| 参数 | 说明 |
| --- | --- |
| `-t name:tag` | 打标签。不写 tag 默认 `latest` |
| `.` | **构建上下文目录，不能漏**。漏了会报 `"docker build" requires exactly 1 argument` |
| `-f 路径/Dockerfile` | Dockerfile 不在当前目录时指定 |
| `--no-cache` | 强制不用缓存，重建 |

```bash
# 实际例子
docker build -t my-api:1.0.0 .
docker build -t my-api:1.0.0 -t my-api:latest .   # 一次打两个标签
```

### 1.3 启动容器（端口映射）

```bash
docker run -d \
  --name 容器名称 \
  -p 访问端口:开放端口 \
  镜像名称:版本号
```

```bash
# 实际例子：宿主机 8080 → 容器 4000
docker run -d --name my-api -p 8080:4000 my-api:1.0.0
```

**`-p 宿主:容器` 的顺序别写反**。写反了容器能起来，但浏览器访问不到。

常用附加参数：

| 参数 | 作用 |
| --- | --- |
| `-d` | 后台运行（不加会占住终端，Ctrl+C 就停） |
| `--name` | 给容器起名，后续 stop/rm 用名字比记 ID 方便 |
| `-e KEY=VALUE` | 传环境变量，可重复 |
| `--env-file .env` | 从文件批量读环境变量 |
| `-v 宿主路径:容器路径` | 挂载目录（改代码不用重建镜像） |
| `--restart unless-stopped` | 崩溃/重启机器后自动拉起 |
| `--rm` | 容器停止后自动删除（适合一次性任务） |
| `--network 网络名` | 加入自定义网络，容器间可用名字互访 |

### 1.4 请求模拟验证

```bash
curl http://localhost:8080/            # 普通 GET
curl -i http://localhost:8080/health   # 带响应头
curl -X POST http://localhost:8080/api/login \
     -H "Content-Type: application/json" \
     -d '{"user":"demo","pass":"123"}'
```

`curl` 不通时按这个顺序查：`docker ps`（容器是否 Up）→ `docker logs 容器名`（应用是否起来）→ 端口是否写反 → 应用是否监听 `0.0.0.0`（监听 `127.0.0.1` 的话容器外进不去）。

---

## 2. 镜像操作

```bash
docker images                        # 查看本地所有镜像
docker pull nginx                    # 拉取镜像（不写 tag 就是 latest）
docker pull nginx:1.27-alpine        # 指定版本，生产环境务必写死
docker rmi 镜像名称:版本号            # 删除镜像
docker rmi 镜像ID                    # 也可以用 ID
docker rmi -f 镜像名称:版本号         # 强制删除（有容器在用时会失败，-f 强删）
docker image prune                   # 清理悬空镜像（<none>:<none>）
docker image prune -a                # 清理所有没被容器使用的镜像
```

> **删不掉镜像**通常是有容器（哪怕是已停止的）还在引用它。先 `docker ps -a` 找到容器 `docker rm` 掉，再删镜像。

---

## 3. 容器操作

### 3.1 查看

```bash
docker ps                  # 只看正在运行的
docker ps -a               # 看所有（含已停止）—— 排查"容器怎么没了"必用
docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
docker logs 容器名称        # 看日志
docker logs -f 容器名称     # 实时滚动（相当于 tail -f）
docker logs --tail 100 容器名称
docker inspect 容器名称     # 看完整配置（IP、挂载、环境变量、健康检查）
docker stats               # 实时看 CPU/内存占用
```

### 3.2 生命周期

```bash
docker stop 容器名称       # 停止（发 SIGTERM，10 秒后 SIGKILL）
docker start 容器名称      # 启动已停止的容器
docker restart 容器名称    # 重启
docker kill 容器名称       # 强杀（不走优雅退出）
docker rm 容器名称         # 删除容器（必须先停止）
docker rm -f 容器名称      # 强制删除（运行中也能删）
```

### 3.3 进入容器

```bash
docker exec -it 容器名称 bash
docker exec -it 容器名称 sh        # alpine 等精简镜像没有 bash，用 sh
docker exec -it 容器名称 node -v   # 不进容器，直接跑一条命令
```

`-i` 保持输入流，`-t` 分配伪终端，两个一起用才有交互界面。

### 3.4 文件互传

```bash
docker cp 容器名称:/app/logs/error.log ./error.log   # 容器 → 宿主
docker cp ./config.json 容器名称:/app/config.json    # 宿主 → 容器
```

### 3.5 批量清理

```bash
docker container prune        # 删除所有已停止的容器
docker system prune           # 清理停止的容器 + 悬空镜像 + 无用网络
docker system prune -a -f     # 更彻底（⚠️ 会删掉所有未使用镜像）
docker system df              # 先看看空间被谁占了
```

---

## 4. 网络

```bash
docker network ls                          # 查看已有网络
docker network create 网络名称              # 创建自定义网络（默认 bridge 驱动）
docker network connect 网络名称 容器名称     # 给已运行的容器加入网络
docker network disconnect 网络名称 容器名称
docker network inspect 网络名称             # 看这个网络里有哪些容器、各自 IP
docker network rm 网络名称                  # 删除网络（先断开所有容器）
```

**为什么需要自定义网络**：默认 `bridge` 网络里容器只能用 IP 互访，IP 每次重启会变。加入**同一个自定义网络**后，可以直接用**容器名**当主机名：

```bash
docker network create app-net
docker run -d --name db  --network app-net postgres:16
docker run -d --name api --network app-net -p 4000:4000 my-api:1.0.0
# 此时 api 容器里连数据库，host 直接写 db 即可，不用写 IP
```

> 容器里的 `localhost` 指的是**容器自己**，不是宿主机。要访问宿主机服务用 `host.docker.internal`（Docker Desktop 默认支持）。

---

## 5. Docker Compose

多容器用 `docker-compose.yml`（新版本文件名也可叫 `compose.yaml`）统一编排，不用手敲一长串 `docker run`。

### 5.1 常用命令

```bash
# 创建并启动所有服务；-d 后台运行；--build 启动前先重新构建镜像（可省略）
docker compose up -d --build

docker compose ps                 # 查看本项目容器状态
docker compose logs -f            # 全部服务日志，实时滚动
docker compose logs -f backend    # 只看某个服务
docker compose logs --tail 50 backend
docker compose stop               # 停止全部服务（容器保留）
docker compose start              # 再次启动已停止的服务
docker compose restart            # 重启全部服务
docker compose down               # 停止并删除容器和网络（镜像保留）
docker compose down -v            # 连 Volume 一起删（⚠️ 数据会丢失）
docker compose config             # 校验 compose 文件语法，检查配置是否正确
docker compose exec backend sh    # 进入某个服务的容器
docker compose build              # 只构建不启动
```

### 5.2 `up` / `stop` / `down` 的区别

| 命令 | 容器 | 网络 | 数据卷 | 镜像 |
| --- | --- | --- | --- | --- |
| `stop` | 停止，保留 | 保留 | 保留 | 保留 |
| `down` | **删除** | **删除** | 保留 | 保留 |
| `down -v` | **删除** | **删除** | **删除** | 保留 |

日常改代码后重启用 `docker compose up -d --build`；彻底重来用 `down`；`down -v` 只在确认可以丢数据时用。

---

## 6. 排错常用

```bash
# 容器起不来，先看退出码和日志
docker ps -a
docker logs --tail 200 容器名称
docker inspect --format='{{.State.ExitCode}} {{.State.Error}}' 容器名称

# 镜像构建失败，看具体是哪一层
docker build -t test:dev . 2>&1 | tail -30
docker build --progress=plain --no-cache -t test:dev .   # 完整日志

# 端口被占用
lsof -nP -iTCP:8080 -sTCP:LISTEN

# 磁盘满了
docker system df
docker system prune -a
```

常见退出码：`0` 正常退出（前台进程跑完就结束了，用 `-d` 起的服务不该是 0）、`1` 应用报错、`137` 被 OOM Kill 或 `kill -9`、`143` 收到 SIGTERM。

---

## 7. 八个高频坑

1. **`docker build` 漏了最后的 `.`** —— 那个点表示"用当前目录作为构建上下文"，不是可有可无的装饰。
2. **`-p` 写反了** —— 是 `宿主端口:容器端口`。`-p 4000:8080` 表示访问宿主机 4000 转到容器 8080。
3. **容器起来就退出** —— 前台进程结束了容器就结束。`CMD` 不能写成 `npm run dev &` 这类后台命令，要留在前台。
4. **`docker ps` 看不到容器** —— 它只列运行中的，加 `-a`。
5. **`docker rm` 报错 `container is running`** —— 先 `stop`，或者直接 `rm -f`。
6. **`exec` 进不去，报 `executable file not found: bash`** —— alpine 系镜像用 `sh`。
7. **容器里连不上数据库** —— 别用 `localhost`，用容器名或 `host.docker.internal`；同时确认在同一个 network 里。
8. **`down -v` 把数据库数据删了** —— `-v` 连 volume 一起删。生产环境慎用，数据卷建议显式命名并在 compose 里声明。

---

## 附录 A：Node + React 项目的实际 Dockerfile

多阶段构建：前端在构建阶段编译成静态文件，最终镜像只留后端运行时，体积能小一个数量级。

```dockerfile
# ---------- 阶段 1：构建前端 ----------
FROM node:22-alpine AS web
WORKDIR /web
COPY kitchenware-admin/package*.json ./
RUN npm ci
COPY kitchenware-admin/ ./
RUN npm run build          # 产物在 /web/dist

# ---------- 阶段 2：后端运行时 ----------
FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production

COPY goods-node-api/package*.json ./
RUN npm ci --omit=dev      # 不装 devDependencies

COPY goods-node-api/ ./
COPY --from=web /web/dist ./public   # 把前端产物塞进来，由 Express 托管

EXPOSE 4000
CMD ["node", "server.js"]
```

配套的 `.dockerignore`（放项目根目录，减少构建上下文体积、避免把密钥打进去）：

```
node_modules
**/node_modules
dist
**/dist
.git
.env
*.log
uploads
generated_images
```

构建与启动：

```bash
docker build -t bizvision-api:1.0.0 .
docker run -d --name bizvision -p 8080:4000 --env-file goods-node-api/.env bizvision-api:1.0.0
curl -i http://localhost:8080/health
```

---

## 附录 B：一份可用的 docker-compose.yml

```yaml
services:
  db:
    image: mysql:8.0
    container_name: bizvision-db
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: goods_db_prod
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    volumes:
      - db-data:/var/lib/mysql          # 命名卷，down 不会丢
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      retries: 5

  api:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: bizvision-api
    restart: unless-stopped
    depends_on:
      db:
        condition: service_healthy       # 等数据库真正就绪再起
    environment:
      MYSQL_HOST: db                     # 用服务名，不用 IP
      MYSQL_PORT: 3306
      MYSQL_DATABASE: goods_db_prod
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    ports:
      - "8080:4000"
    volumes:
      - uploads:/app/uploads

volumes:
  db-data:
  uploads:
```

配套 `.env`（和 compose 文件同目录，**不要提交到 git**）：

```bash
MYSQL_ROOT_PASSWORD=change-me
MYSQL_USER=app
MYSQL_PASSWORD=change-me-too
```

```bash
docker compose config          # 先校验，能打印出展开后的完整配置就是对的
docker compose up -d --build
docker compose ps
docker compose logs -f api
```
