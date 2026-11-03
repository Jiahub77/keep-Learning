# Docker PHP 开发环境：从 0 到 1 搭建指南

> 整理日期：2026-10-28。依据当前目录的 `README.md`、`docker-compose.yaml`、配置文件和本机 Docker 实测结果。本文面向 macOS 本地开发；Linux 需要调整宿主机路径及数据库访问方式。
>
> 本次只编写文档并做只读检查，没有启动、停止、重建现有容器，也没有修改业务配置。以下新建站点、MySQL 和队列步骤是搭建操作说明，尚未实际执行。

## 1. 最终要搭建什么

```text
浏览器 http://localhost:10108
           │
           ▼
       OpenResty（Nginx）
           │ 按站点配置选择 PHP
           ├── php-fpm:9000    → PHP 7.4
           └── php82-fpm:9000  → PHP 8.2
                        │
                        ├── redis:6379
                        └── 数据库（另行配置）

php-worker（PHP 7.4） → Supervisor 队列 / Cron 定时任务
所有 PHP 和 OpenResty 容器共享 /disk2/www 业务代码目录
```

本机配置目录为 `/Users/mac/disk2/php-server`，业务代码目录为 `/Users/mac/disk2/www`。它们是两个同级目录，仓库内的 `www/` 不是当前实际挂载的业务目录。

### 当前已确认的环境

| 项目 | 本机实际情况 |
| --- | --- |
| 宿主机 / Docker 引擎架构 | macOS arm64 / linux arm64 |
| Docker Desktop / Engine / Compose | 4.75.0 / 29.5.2 / v5.1.3 |
| Docker context | `desktop-linux` |
| `openresty` | 运行中；镜像 `huaji01/openresty:1.0` |
| `php-fpm` | 运行中；镜像 `huaji01/php-fpm:3.0.4-74`；PHP 7.4.18 |
| `php82-fpm` | 运行中；镜像 `huaji01/php-fpm:3.0.7-8.2.35`；PHP 8.2.35 |
| `php-worker` | 运行中；镜像 `huaji01/php-worker:3.0.4-74`；PHP 7.4.18 |
| `redis` | 运行中；镜像 `huaji01/redis:5.0.3`；需要密码 |
| `mysql_container` | 已停止；`mysql:8.0`；属于独立的 `mysql-docker` 项目 |

上述 huaji01 镜像均来自 `registry.cn-shenzhen.aliyuncs.com`。当前 Compose 中五个启用服务都指定了 `platform: linux/amd64`，Apple Silicon 运行这些镜像需要架构模拟。

### 旧 README 与当前配置的差异

- 实际 Web 服务名为 `openresty`；`nginx` 和 `mysql` 服务已注释，不能直接照抄旧文档启动它们。
- `.env.example` 缺少 `GITLAB_CI_PATH`，需要补齐。
- PHP 8.2 是独立服务；修改 `PHP_VERSION` 不会修改 `php82-fpm` 的镜像。
- Redis 端口在 Compose 中写死为 `6379:6379`，修改 `.env` 的 `REDIS_PORT` 当前不会生效。
- Compose 内没有启用 MySQL，修改 `.env` 中的 MySQL 参数不会自动创建数据库。
- 当前工作区包含未提交/未跟踪的 PHP 8.2、FastCGI 等配置。仅克隆远端仓库不保证得到本文描述的完整环境，迁移前应确认这些文件已保存并同步。

## 2. 安装 Docker 并准备目录

1. 安装适合芯片架构的 [Docker Desktop for Mac](https://docs.docker.com/desktop/setup/install/mac-install/)，启动并等待引擎就绪。
2. 在终端验证：

```bash
docker version
docker compose version
docker context show
```

`docker version` 应同时显示 Client 和 Server。如果提示无法连接 daemon，先启动 Docker Desktop。

以下使用当前 Mac 的目录布局，新机器将 `/Users/mac` 替换为实际用户目录：

```bash
mkdir -p /Users/mac/disk2/www /Users/mac/disk2/gitlab-ci
cd /Users/mac/disk2
# 仅在 php-server 目录不存在时克隆；仓库地址来自原 README，需要相应访问权限。
git clone https://git.ping-qu.cn/docker/dev-server.git php-server
cd /Users/mac/disk2/php-server
```

如果已存在本仓库，直接进入它，不要重复克隆或覆盖现有配置。迁移当前环境时，还需要单独迁移业务源码及需要保留的数据。

确认包含以下配置；缺失的文件应从当前工作区或团队维护版本取得，不要创建空文件替代：

```text
php-server/
├── docker-compose.yaml
├── .env.example
├── .env                         # 本机配置，不提交密码
├── nginx/
│   ├── fastcgi_params
│   ├── config/nginx.conf
│   ├── config/laravel.conf       # PHP 7.4 路由
│   ├── config/laravel82.conf     # PHP 8.2 路由
│   ├── vhost/                   # 每个站点一个 .conf
│   ├── cert/
│   ├── log/
│   └── lua/
├── php/php.ini
├── php-fpm/www.conf
├── php82/php.ini
├── php82-fpm/www.conf
├── php-worker/crontab/www-data
├── php-worker/supervisor/conf.d/
└── redis/
    ├── config/redis.conf
    └── db_data/
```

创建挂载目录：

```bash
mkdir -p nginx/cert nginx/log nginx/lua redis/db_data php-worker/supervisor/conf.d
```

Docker Desktop 若提示目录无法共享，在文件共享设置中允许 `/Users/mac/disk2`。文件挂载源必须是真实文件，否则可能被创建为目录，导致容器挂载失败。

## 3. 配置环境变量

在 `php-server` 目录执行，保留已有 `.env`：

```bash
test -f .env || cp .env.example .env
```

编辑 `.env`，至少包含：

```dotenv
WWW_PATH=/Users/mac/disk2/www/
GITLAB_CI_PATH=/Users/mac/disk2/gitlab-ci/
REGISTRY_DOMAIN=registry.cn-shenzhen.aliyuncs.com
START_CRON=0
NGINX_HOST_HTTP_PORT=80
NGINX_HOST_HTTPS_PORT=443
PHP_VERSION=74

# 以下变量目前未被启用的 MySQL 服务使用
MYSQL_PORT=3306
MYSQL_ROOT_PASSWORD=replace-with-your-own-password
MYSQL_DB_PATH=./mysql/db_data

# 当前 Compose 的 Redis 端口仍是写死的 6379:6379
REDIS_PORT=6379
```

普通 Mac 使用公网镜像仓库；`registry-vpc.cn-shenzhen.aliyuncs.com` 是原模板中的 VPC 地址，不适用于一般本地网络。

### 必须理解的路径映射

| 宿主机 | 容器内 |
| --- | --- |
| `/Users/mac/disk2/www/` | `/disk2/www/` |
| `/Users/mac/disk2/gitlab-ci/` | `/disk2/gitlab-ci/`（仅 OpenResty） |
| `nginx/vhost/` | `/etc/nginx/conf.d/` |
| `nginx/config/` | `/etc/nginx/config/` |
| `nginx/log/` | `/var/log/nginx/` |
| `redis/db_data/` | `/data/` |

Nginx 的 `root`、Supervisor 的命令和 Cron 路径都必须写容器路径，例如 `/disk2/www/demo/public`，不能写 `/Users/mac/...`。

另外，`php-server/.env` 用于 Docker Compose；Laravel 项目自己的 `.env` 用于业务配置，两者不能混用。

## 4. 校验、拉取并启动基础服务

```bash
cd /Users/mac/disk2/php-server
docker compose config --quiet
docker compose config --services
docker compose pull
docker compose up -d redis php-fpm php82-fpm php-worker
docker compose up -d openresty
docker compose ps
```

先启动两个 FPM，再启动 OpenResty，避免加载已有站点时无法解析 PHP 上游主机。当前配置没有 readiness/healthcheck，容器 `Up` 后仍需执行下面的验证。

预期服务列表：`redis`、`php-fpm`、`php82-fpm`、`php-worker`、`openresty`。新版 Compose 提示顶层 `version` 已过时只是警告，不影响运行；后续维护可删除该字段。

镜像需要权限时，使用有权限的账号执行：

```bash
docker login registry.cn-shenzhen.aliyuncs.com
docker compose pull
```

仓库未提供这些自定义镜像的完整构建上下文，所以镜像无法获取时，单靠当前目录不能保证自行重建。

### 端口规则

- OpenResty 发布 `80`、`443`、`10100–10200`。
- 当前站点使用 `10100–10107`；发布端口不等于已经配置对应 HTTP/HTTPS 站点。
- PHP 的 `9000` 只供容器网络内 FastCGI 使用，不是浏览器 HTTP 端口。
- Redis 当前发布到宿主机 `6379`，且绑定所有网卡。仅本地开发可将映射改为 `127.0.0.1:6379:6379`，再重建 Redis 服务；容器间仍通过 `redis:6379` 访问。
- 如果出现同名容器冲突，先用 `docker ps -a` 确认归属；本配置固定 `container_name`，仅更换 Compose 项目名无法实现多套并行。

## 5. 新建一个可验证的 PHP 8.2 站点

先用不依赖数据库的最小 PHP 页面验证链路。以下使用当前未配置的 `10108` 端口，执行前确认本机没有新增占用；文件名已存在时请换名。

```bash
mkdir -p /Users/mac/disk2/www/docker-demo/public
cat > /Users/mac/disk2/www/docker-demo/public/index.php <<'PHP'
<?php
header('Content-Type: text/plain; charset=utf-8');
echo "Docker PHP OK\n";
echo 'PHP version: ' . PHP_VERSION . "\n";
PHP

cat > nginx/vhost/10108-docker-demo.conf <<'NGINX'
server {
    listen 10108;
    server_name localhost;
    root /disk2/www/docker-demo/public;

    access_log /var/log/nginx/docker-demo_access.log;
    error_log /var/log/nginx/docker-demo_error.log error;

    include /etc/nginx/config/laravel82.conf;
}
NGINX

docker compose exec openresty nginx -t
# 仅上一步成功后执行 reload
docker compose exec openresty nginx -s reload
curl --fail http://localhost:10108/
```

预期返回 `Docker PHP OK` 和 `PHP version: 8.2.35`。浏览器也可打开 `http://localhost:10108/`。

需要 PHP 7.4 时，将 include 改为 `/etc/nginx/config/laravel.conf`，校验后重载。对应关系：

| include | FastCGI 上游 |
| --- | --- |
| `laravel.conf` | `php-fpm:9000` |
| `laravel82.conf` | `php82-fpm:9000` |

两个 FPM 容器都监听内部 9000 并不冲突，因为它们拥有独立网络空间。

### 当前业务站点参考

以下 root 均在容器 `/disk2/www/` 下：

| 端口 | 项目 public 目录 | PHP |
| --- | --- | --- |
| 10100 | `LuckdateWholeProduct/America/api/public` | 7.4 |
| 10101 | `luckdate-mx/api/public` | 7.4 |
| 10102 | `LuckdateWholeProduct/Brazil/api/public` | 8.2 |
| 10103 | `LuckdateWholeProduct/Mexico/api/public` | 7.4 |
| 10104 | `LuckdateWholeProduct/Vietnam/api/public` | 7.4 |
| 10105 | `chatviva-mx/api/public` | 7.4 |
| 10106 | `xiaoyouhui/api/public` | 7.4 |
| 10107 | `ai-healthhub/admin-api/public` | 7.4 |

## 6. 接入真正的 Laravel 项目

将业务项目克隆或复制到 `/Users/mac/disk2/www/项目目录`。下面假设一个兼容 PHP 8.2 的项目位于 `/Users/mac/disk2/www/my-app`，并且 `artisan` 在该目录根部：

```bash
cd /Users/mac/disk2/www/my-app
test -f .env || cp .env.example .env
cd /Users/mac/disk2/php-server

docker compose exec -w /disk2/www/my-app php82-fpm composer install
docker compose exec -w /disk2/www/my-app php82-fpm composer check-platform-reqs
相当于
docker compose exec php82-fpm bash
cd /disk2/www/my-app
composer install
composer check-platform-reqs
```

编辑业务 `.env`，设置应用地址、数据库和 Redis。仅新项目首次初始化时生成 APP_KEY，已有项目应保留原密钥：

```bash
docker compose exec -w /disk2/www/my-app php82-fpm php artisan key:generate
docker compose exec -w /disk2/www/my-app php82-fpm php artisan config:clear
```

为项目创建站点配置，`root` 指向 `/disk2/www/my-app/public`，参考上一节选择未占用端口及对应 PHP include，再校验重载。项目若还有 `/api` 等子目录，工作目录和 root 都要跟着调整。

Laravel 的 `storage/` 和 `bootstrap/cache/` 必须允许容器内 FPM 用户 `www-data` 写入。遇到 Permission denied 时先检查挂载目录权限，按实际 UID/GID 修正，不要直接给整份源码 `777`。

数据库导入、`php artisan migrate`、前端依赖及构建请按业务仓库说明执行。迁移会改变数据库，确认连接的是目标开发库后再执行。

## 7. 配置 Redis

本机 Redis 开启了密码认证，未认证 `PING` 返回 `NOAUTH Authentication required`；这表示服务响应了请求，不代表 Redis 未启动。密码应从维护者或本地配置中取得，本文不复制现有密码。

在 `redis/config/redis.conf` 中为新环境设置自己的 `requirepass`，修改后运行 `docker compose restart redis`。业务 `.env` 示例：

```dotenv
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD="填写与 Redis 配置一致的密码"
```

Redis 客户端类型依据项目依赖选择 `phpredis` 或 `predis`，不要仅凭模板修改。密码含 `#` 等字符时注意使用引号。

可以在交互式客户端里验证（`<密码>` 替换为实际值）：

```bash
docker compose exec redis redis-cli
```

```text
AUTH <密码>
PING
QUIT
```

预期依次返回 `OK`、`PONG`。宿主机工具连接 `127.0.0.1:6379`；PHP 容器连接 `redis:6379`，不能用 `127.0.0.1` 指代另一个容器。

当前 Redis 持久化为 RDB 快照，`appendonly no`，数据目录为 `redis/db_data`；不要把它当成已启用 AOF 的配置。

## 8. MySQL：当前独立于 PHP 环境

### 方案 A：使用本机已有 MySQL

本机 `mysql_container` 属于 `mysql-docker` 项目，映射为宿主机 `33061` → 容器 `3306`，当前已停止，且不在 `php-server_backend` 网络。

确认需要启用这份已有数据库后：

```bash
docker start mysql_container
docker logs --tail=100 mysql_container
```

数据库就绪后，macOS Docker Desktop 内的 Laravel 配置为：

```dotenv
DB_CONNECTION=mysql
DB_HOST=host.docker.internal
DB_PORT=33061
DB_DATABASE=你的开发库
DB_USERNAME=你的数据库用户
DB_PASSWORD="你的数据库密码"
```

宿主机数据库客户端连接 `127.0.0.1:33061`。当前网络下不能直接假设 PHP 容器能解析 `mysql_container` 或 `mysql`。已有数据使用 Docker volume `mysql-docker_mysql_data`，不要用 MySQL 5.7 打开这份 MySQL 8.0 数据目录。

### 方案 B：新机器创建独立 MySQL 8.0

如果没有现成数据库，可在 `php-server` 下新建 `compose.mysql.yaml`，并与主配置合并使用。该示例使用全新卷，不复用上述已有数据；与方案 A 二选一，避免同时占用 33061。

```yaml
services:
  mysql:
    image: mysql:8.0
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:?请先在 .env 设置数据库 root 密码}
      MYSQL_DATABASE: app
      MYSQL_USER: app
      MYSQL_PASSWORD: ${MYSQL_APP_PASSWORD:?请先在 .env 设置应用数据库密码}
    ports:
      - "127.0.0.1:33061:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - backend

volumes:
  mysql_data:
```

在 `php-server/.env` 中设置新的 `MYSQL_ROOT_PASSWORD`、`MYSQL_APP_PASSWORD`，然后：

```bash
docker compose -f docker-compose.yaml -f compose.mysql.yaml config --quiet
docker compose -f docker-compose.yaml -f compose.mysql.yaml up -d mysql
docker compose -f docker-compose.yaml -f compose.mysql.yaml logs --tail=100 mysql
```

看到数据库 ready 后，在业务 `.env` 使用 `DB_HOST=mysql`、`DB_PORT=3306`、`DB_DATABASE=app`、`DB_USERNAME=app`，密码与 `MYSQL_APP_PASSWORD` 一致。

初始化变量只在空数据目录首次初始化时生效，更改 `.env` 不会重设已有数据库密码。此方案后续管理 MySQL 仍需同时传入两个 `-f` 参数。

## 9. 队列和定时任务（按项目需要启用）

当前 `php-worker` 是 PHP 7.4，不能直接承担要求 PHP 8.2 的项目任务。PHP 8.2 项目需要另配匹配版本的 worker 镜像或服务；仓库目前没有提供该服务。以下示例仅适用于兼容 PHP 7.4 的项目。

### Supervisor 队列

将以下内容保存为 `php-worker/supervisor/conf.d/my-app.conf`，路径改成实际项目路径：

```ini
[program:my-app-queue]
command=/usr/local/bin/php /disk2/www/my-app/artisan queue:work --sleep=3 --tries=3
user=www-data
autostart=true
autorestart=true
redirect_stderr=true
stdout_logfile=/disk2/www/my-app/storage/logs/queue.log
stopasgroup=true
killasgroup=true
stopwaitsecs=3600
```

先确保日志目录存在且用户可写，再加载：

```bash
docker compose exec php-worker supervisorctl reread
docker compose exec php-worker supervisorctl update
docker compose exec php-worker supervisorctl status
```

使用 Laravel Horizon 的项目可将命令改为 `php .../artisan horizon`，前提是已安装并配置 Horizon。不要照抄旧示例中的占位项目路径。

### Cron 定时任务

当前 `.env` 中 `START_CRON=0`，实测 worker 内 `www-data` 没有安装 crontab。挂载文件中还保留旧业务的定时任务，启用前必须检查并替换为目标项目：

```cron
* * * * * /usr/local/bin/php /disk2/www/my-app/artisan schedule:run >> /disk2/www/my-app/storage/logs/scheduler.log 2>&1
```

这是用户 crontab 格式，不需要添加 `www-data` 字段。保存到 `php-worker/crontab/www-data` 并确保文件末尾有换行；设置 `.env` 的 `START_CRON=1`，然后：

```bash
docker compose up -d --force-recreate php-worker
docker compose exec php-worker crontab -u www-data -l
```

镜像入口脚本只在启动且 `START_CRON=1` 时复制宿主机文件、修正权限并启动 cron。后续修改定时任务文件后，需要重启 worker 才会重新复制。仅修改 `.env` 后 `restart` 不会更新容器环境变量，因此首次启用应使用上面的重建命令。

## 10. 配置生效规则及本机发现的问题

### OpenResty 主配置实际未使用挂载的 nginx.conf

实测 OpenResty 以 `/usr/bin/openresty -g daemon off;` 启动，默认读取：

```text
/usr/local/openresty/nginx/conf/nginx.conf
```

但 Compose 将 `nginx/config/nginx.conf` 挂载到了 `/etc/nginx/nginx.conf`。两者不是同一个文件，因此直接修改仓库里的主配置再 reload，不保证生效。仓库根下的 `nginx/nginx.conf` 也没有被挂载。

站点配置仍然有效，因为镜像默认主配置包含 `/etc/nginx/conf.d/*.conf`。用下面的命令查看实际加载内容：

```bash
docker compose exec openresty nginx -T
```

需要启用自定义主配置时，可将 Compose 的该文件挂载目标改为 `/usr/local/openresty/nginx/conf/nginx.conf`，重建 OpenResty 并执行 `nginx -t`。变更前可先测试候选配置：

```bash
docker compose exec openresty nginx -t -c /etc/nginx/nginx.conf
```

确认配置中的 include、日志及 PID 路径均可用后再切换，不要只 reload 未被加载的文件。

### PHP-FPM 的最终监听配置

仓库 `php-fpm/www.conf` 写着 `listen = 127.0.0.1:9000`，但镜像里的 `zz-docker.conf` 覆盖为 `listen = 9000`；实测两个 FPM 最终均监听 9000，因此当前跨容器连接可用。换镜像时应重新核对，不能仅看一个配置文件：

```bash
docker compose exec php-fpm php-fpm -tt
docker compose exec php82-fpm php-fpm -tt
```

### PHP 8.2 重复扩展提示

本机 PHP 8.2 执行时提示 `swoole`、`xlswriter` 已重复加载。命令仍可执行，但应通过 `php --ini` 检查 `php82/php.ini` 与镜像 `conf.d`，同一扩展只保留一个加载入口：

```bash
docker compose exec php82-fpm php --ini
docker compose exec php82-fpm php -m
```

不要把 PHP 7.4 的配置直接整份覆盖给 8.2；修改 PHP ini 后重启对应 FPM。

## 11. 验收与日常维护

### 验收清单

```bash
cd /Users/mac/disk2/php-server
docker compose config --quiet
docker compose ps
docker compose exec openresty nginx -t
docker compose exec php-fpm php -v
docker compose exec php82-fpm php -v
docker compose exec php-worker php -v
# 完成第 5 节后执行：
curl --fail http://localhost:10108/
```

验收标准：五个服务运行、Nginx 语法通过、两个 PHP 版本正确、示例页面可访问、Redis 认证后返回 PONG。如果接入 Laravel，还需验证数据库连接和一条实际业务请求；队列/定时任务需单独确认处理成功。

本次已验证：Compose 配置可解析、五个服务运行、Nginx 配置校验通过、PHP 版本、PHP 8.2 中 Composer 2.8.12 可运行、Redis 返回认证要求。尚未验证：新机器镜像拉取、示例站点、业务接口、数据库登录及队列任务端到端执行。

### 常用命令

```bash
# 查看日志；部分站点日志写入宿主机 nginx/log，而非容器标准输出
docker compose logs --tail=100 openresty php82-fpm redis

# 进入 PHP 容器
docker compose exec php82-fpm sh

# PHP 配置变化后重启
docker compose restart php82-fpm

# .env、端口、镜像或挂载变化后应用配置
docker compose up -d

# 停止但保留容器
docker compose stop

# 再次启动已存在的容器
docker compose start

# 移除本 Compose 的容器和网络；下次用 up -d 重建
docker compose down
```

`down` 默认不删除命名卷，绑定到宿主机的源码和数据也会保留；`down -v` 会删除命名卷，不应用作日常停止命令。独立 `mysql-docker` 项目不受本项目 `down` 管理。采用 MySQL 方案 B 时，统一管理整套服务需加上两个 `-f` 参数。

## 12. 拉取并应用新镜像

以下以 `php82-fpm` 为例。命令中的 `php82-fpm` 是 Compose 服务名，不是镜像名称；其他服务可以替换为对应服务名。

### 情况一：更新同一个镜像标签

当仓库发布了相同标签的新镜像，无需修改 Compose：

```bash
cd /Users/mac/disk2/php-server

# 下载当前 Compose 配置指定的镜像
docker compose pull php82-fpm

# 镜像发生变化时重建该服务，不联动其他服务
docker compose up -d --no-deps php82-fpm
```

若标签对应的镜像没有变化，`up -d` 可能直接保留现有容器，这是正常行为。

### 情况二：切换镜像版本或仓库

编辑 `docker-compose.yaml`，找到 `php82-fpm` 的 `image`，将它替换为目标镜像的完整地址及标签。下面只是字段示意，保留原服务的 volumes、networks 等其他配置：

```yaml
php82-fpm:
    platform: linux/amd64
    container_name: php82-fpm
    image: 你的仓库地址/镜像名称:新标签
```

目标镜像必须真实存在，并兼容当前 `linux/amd64` 平台、PHP 配置挂载路径和 FPM 9000 端口。新 PHP 版本还需满足业务依赖及扩展要求；不能直接用仅提供 CLI 的镜像替换 FPM 镜像。

保存配置后执行：

```bash
cd /Users/mac/disk2/php-server
docker compose config --quiet
docker compose pull php82-fpm
docker compose up -d --no-deps --force-recreate php82-fpm
```

按顺序执行，校验或拉取失败时先解决错误，再继续重建。若镜像仓库需要登录，先执行 `docker login 你的仓库域名`。

当前 `php-fpm` 和 `php-worker` 的镜像标签由 `.env` 中的 `PHP_VERSION` 共同控制；修改该变量会同时影响两个服务的配置。`php82-fpm` 的镜像则直接写在 Compose 中，需要单独修改。

### 更新后验证

```bash
docker compose ps php82-fpm
docker compose logs --tail=50 php82-fpm
docker compose exec php82-fpm php -v
```

PHP 容器重建后地址可能变化，确认 FPM 正常运行后，重载 OpenResty，让它重新解析 PHP 服务地址：

```bash
docker compose exec openresty nginx -t &&
docker compose exec openresty nginx -s reload
```

最后访问使用该 PHP 服务的站点，验证实际请求。若已经按第 5 节创建示例站点：

```bash
curl --fail http://localhost:10108/
```

`docker pull` 或 `docker compose pull` 只下载镜像；`docker compose restart` 只重启旧容器，不能应用新镜像。需要用 `docker compose up -d` 重建容器，`--force-recreate` 表示即使配置或镜像未变也强制重建。

重建会短暂中断该服务。宿主机挂载的代码、配置和数据会保留，容器内未挂载的临时修改会丢失。更新前记录旧镜像标签或 digest；需要回退时，将 `image` 改回已保留的旧版本，再重建并验证。同标签可能被覆盖，精确回退应保留旧镜像或记录可拉取的 digest。

## 13. 常见故障速查

| 现象 | 优先检查 |
| --- | --- |
| Cannot connect to Docker daemon | Docker Desktop 是否启动、context 是否正确 |
| 镜像拉取超时 / denied | 是否使用公网 registry、网络和镜像权限；镜像 tag 是否存在 |
| Apple Silicon 架构错误 | 保留已验证的 `linux/amd64` 配置，并检查 Docker Desktop 的模拟支持 |
| `GITLAB_CI_PATH` 未设置 | 在 Compose `.env` 中补齐绝对路径并创建目录 |
| port is already allocated | 用 `docker ps` 检查占用；HTTP 可改 `.env`，Redis 及 10100–10200 要改 Compose |
| `host not found in upstream` | 两个 FPM 服务是否已启动，并与 OpenResty 同在 backend 网络 |
| 502 Bad Gateway | FPM 是否启动、`fastcgi_pass` 名称及实际 listen 配置是否正确 |
| 404 / File not found | root 是否指向容器里的 public，OpenResty 与 FPM 是否挂载同一代码目录 |
| Laravel 500 | `storage/logs`、APP_KEY、依赖、写权限、数据库/Redis 连接、配置缓存 |
| Redis NOAUTH / WRONGPASS | 应用密码是否与 Redis 配置一致，是否清除了 Laravel 配置缓存 |
| PHP 连不到数据库 | 方案 A 使用 `host.docker.internal:33061`；方案 B 使用 `mysql:3306` |
| 修改 nginx.conf 不生效 | `nginx -T` 确认主配置路径；当前默认并不读取挂载的 `/etc/nginx/nginx.conf` |
| 队列无任务执行 | worker 的 PHP 版本、Supervisor 状态、业务队列连接和日志 |
| Cron 没执行 | START_CRON、容器内 crontab、项目路径、脚本权限；修改挂载文件后是否重启 |

## 14. 常用命令与日志排查速查表

以下 Compose 命令默认在配置目录执行：

```bash
cd /Users/mac/disk2/php-server
```

`docker compose ...` 使用服务名（如 `php82-fpm`）；`docker logs/exec/inspect ...` 使用容器名或 ID。本项目部分服务名与容器名相同，但独立 MySQL 的容器名是 `mysql_container`，不能在当前 Compose 下执行 `docker compose logs mysql`。

示例中的 `my-app` 是项目目录占位符，使用前替换。采用第 8 节 MySQL 方案 B 时，管理该 MySQL 服务的 Compose 命令需要加 `-f docker-compose.yaml -f compose.mysql.yaml`。

### 14.1 查看环境与运行状态

```bash
docker version                         # 客户端和引擎版本
docker compose version                 # Compose 版本
docker context show                    # 当前连接的 Docker 环境
docker compose config --quiet          # 校验配置，不打印展开后的密码
docker compose config --services       # 查看服务名
docker compose ps -a                   # 当前项目，包括已停止容器
docker ps -a                           # 本机全部容器
docker compose images                  # 当前项目容器使用的镜像
docker image ls                        # 本机镜像列表
docker stats --no-stream               # CPU、内存、网络及磁盘 I/O 快照
docker system df                       # 镜像、容器和卷的磁盘用量
```

### 14.2 启动、停止和更新

| 目的 | 命令 |
| --- | --- |
| 创建并后台启动所有服务 | `docker compose up -d` |
| 只启动一个服务（可能联动其依赖） | `docker compose up -d php82-fpm` |
| 停止所有服务，保留容器 | `docker compose stop` |
| 停止一个服务 | `docker compose stop php-worker` |
| 启动已有的停止容器 | `docker compose start` |
| 重启一个服务，保留旧容器配置 | `docker compose restart php82-fpm` |
| 拉取一个服务的镜像 | `docker compose pull php82-fpm` |
| 应用新镜像或 Compose 配置 | `docker compose up -d --no-deps php82-fpm` |
| 强制重建一个服务 | `docker compose up -d --no-deps --force-recreate php82-fpm` |
| 停止并移除本项目容器和网络 | `docker compose down` |

修改 `.env`、端口、挂载或镜像后使用 `up -d`；仅 `restart` 不会应用这些变更。FPM 重建后按第 12 节重载 OpenResty。`down -v` 会删除命名卷，不能当作普通停止命令。

### 14.3 查看容器日志

```bash
# 当前项目所有服务最近 100 行
docker compose logs --tail=100

# 指定服务，显示时间戳
docker compose logs --timestamps --tail=100 php82-fpm

# 持续跟踪多个服务，Ctrl+C 退出查看，不会停止容器
docker compose logs -f --tail=100 openresty php82-fpm redis

# 最近 30 分钟的日志
docker compose logs --since=30m --timestamps php82-fpm

# 已退出的容器也可以查看日志
docker logs --timestamps --tail=100 mysql_container

# 将最近日志保存到文件，便于检索或分享
docker compose logs --no-color --timestamps --since=30m > docker-debug.log 2>&1
```

Compose 日志主要采集容器标准输出和标准错误。Nginx、Laravel、Supervisor 写入文件的日志，需要单独查看；日志为空不能说明业务没有报错。分享日志前检查其中是否包含密码、令牌或用户数据。

### 14.4 查看 Nginx 和 Laravel 文件日志

```bash
# 查看现有 Nginx 日志文件
ls -lh nginx/log/

# 持续查看一个真实站点的访问和错误日志
tail -n 100 -f nginx/log/ai-healthhub_access.log nginx/log/ai-healthhub_error.log

# 查看实际加载的 Nginx 配置和配置路径
docker compose exec openresty nginx -T

# 检查配置；成功后才重载
docker compose exec openresty nginx -t &&
docker compose exec openresty nginx -s reload

# 查看 Laravel 日志目录，先确认实际日志文件名
docker compose exec php82-fpm ls -lh /disk2/www/my-app/storage/logs

# 单文件日志模式示例
docker compose exec php82-fpm tail -n 100 -f /disk2/www/my-app/storage/logs/laravel.log
```

Laravel 若使用 daily 日志，实际文件一般为 `laravel-YYYY-MM-DD.log`，将命令中的文件名替换即可。其他日志通道以业务项目配置为准。

### 14.5 进入容器及检查 PHP

```bash
docker compose exec php82-fpm sh
docker compose exec -w /disk2/www/my-app php82-fpm sh
docker compose exec php82-fpm php -v
docker compose exec php82-fpm php -m           # 已加载扩展
docker compose exec php82-fpm php --ini        # 配置文件及扫描目录
docker compose exec php82-fpm php-fpm -tt      # FPM 最终配置

# 检查项目挂载是否存在
docker compose exec openresty ls -l /disk2/www/my-app/public/index.php
docker compose exec php82-fpm ls -l /disk2/www/my-app/public/index.php

# 安装锁定版本的依赖，并验证 PHP 和扩展要求
docker compose exec -w /disk2/www/my-app php82-fpm composer install
docker compose exec -w /disk2/www/my-app php82-fpm composer check-platform-reqs

# 修改业务 .env 后清除配置缓存
docker compose exec -w /disk2/www/my-app php82-fpm php artisan config:clear
```

旧项目将服务名替换为 `php-fpm`。不要为了修复环境问题随意执行 `composer update`，它会重新解析依赖并可能修改锁文件。

### 14.6 MySQL、Redis 和队列排查

```bash
# 独立 MySQL 容器：状态、启动日志、交互式登录
docker ps -a --filter name=mysql_container
docker logs --tail=100 mysql_container
docker exec -it mysql_container mysql -u root -p

# Redis：先进入客户端，再输入 AUTH <密码> 和 PING
docker compose exec redis redis-cli

# Supervisor 当前进程状态
docker compose exec php-worker supervisorctl status

# 修改 Supervisor 配置后重新加载
docker compose exec php-worker supervisorctl reread
docker compose exec php-worker supervisorctl update

# 查看已安装的用户定时任务
docker compose exec php-worker crontab -u www-data -l

# 查看 Laravel 已记录的失败任务（要求项目已配置失败任务存储）
docker compose exec -w /disk2/www/my-app php82-fpm php artisan queue:failed
```

数据库交互登录后可执行 `SELECT 1;` 验证响应，使用 `exit` 退出。Redis 认证成功后 `PING` 应返回 `PONG`，用 `QUIT` 退出。

Supervisor 的任务日志位置由每个配置的 `stdout_logfile` 决定，按实际路径执行 `tail -n 100 -f`。当前 `php-worker` 是 PHP 7.4；PHP 8.2 项目失败任务查询可以在 `php82-fpm` 执行，但常驻消费者仍需匹配版本的 worker。

重试失败任务会重新执行业务，先检查失败原因和任务是否可重复执行，再按任务 ID 重试，不要直接批量重试全部任务。

### 14.7 网络、端口和异常退出

```bash
# 宿主机发请求，显示响应头和连接过程
curl -v --max-time 10 http://localhost:10102/

# macOS：查看宿主机端口占用
lsof -nP -iTCP:10102 -sTCP:LISTEN

# 查看 Compose 的端口映射
docker compose port openresty 10102

# 查看容器退出码、是否内存不足、重启次数
docker inspect php82-fpm --format 'status={{.State.Status}} exit={{.State.ExitCode}} oom={{.State.OOMKilled}} restarts={{.RestartCount}} error={{.State.Error}}'

# 仅查看挂载信息或网络信息
docker inspect php82-fpm --format '{{json .Mounts}}'
docker inspect php82-fpm --format '{{json .NetworkSettings.Networks}}'
docker network ls
```

可用 PHP 自带的 Socket 功能验证 FPM 容器到 Redis 的 DNS 和 TCP 连接，不依赖镜像中是否安装 ping/nc：

```bash
docker compose exec php82-fpm php -r '$s = @fsockopen("redis", 6379, $code, $message, 3); if (!$s) { fwrite(STDERR, "$code $message\n"); exit(1); } echo "TCP OK\n"; fclose($s);'
```

此检查只证明网络连通，不代表密码认证或业务调用成功。检查数据库时，按实际方案替换主机和端口：独立 MySQL 使用 `host.docker.internal` / `33061`，同 Compose 的 MySQL 使用 `mysql` / `3306`。

### 14.8 推荐的排查顺序
1. `docker compose config --quiet`：确认配置和环境变量能解析。
2. `docker compose ps -a`：确认目标容器是否运行或反复重启。
3. `docker compose logs --tail=100 服务名`：检查启动错误；异常退出再查退出码和 OOM。
4. `curl -v`：区分连接失败、404、502 和应用 500。
5. 连接失败先查端口映射；404 查站点 root 和代码挂载；502 查 FPM 状态、网络及监听地址。
6. 500 查 Laravel 文件日志，再查数据库、Redis、项目依赖和写权限。
7. 修改后校验配置、重载或重建对应服务，再验证同一个请求。

这套配置用于复现当前本地开发环境；PHP 7.4、Redis 5 等历史版本应按业务兼容性规划升级。若要部署公网生产环境，还需另行配置 TLS、对外端口、凭据、备份和运维策略。
