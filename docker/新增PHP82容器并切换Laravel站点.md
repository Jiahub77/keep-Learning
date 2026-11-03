# 新增 PHP 8.2 容器并切换 Laravel 站点

本文说明如何直接使用 Docker Hub 官方 `php:8.2-fpm` 镜像，在现有环境中新增 PHP 容器，并让指定 Laravel 项目使用它。不需要从 Git 获取一套新的 Docker 环境。

以下是操作说明，编写文档不会实际拉取镜像、修改服务或切换业务站点。

## 1. 最终结构

```text
浏览器 → OpenResty → 指定项目的 vhost
                         │
                         └── laravel82-official.conf
                                  │
                                  └── php82-official:9000

其他站点继续使用原来的 php-fpm 或 php82-fpm
```

原来的 `php82-fpm` 不需要删除或替换。新增服务使用名称 `php82-official`，与 OpenResty 加入相同的 `backend` 网络，并挂载相同的代码目录。

## 2. 进入现有配置目录

```bash
cd /Users/mac/disk2/php-server
```

本文修改的是这里的 `docker-compose.yaml`。其中 `${WWW_PATH}` 来自该目录的 `.env`，本机配置为 `/Users/mac/disk2/www/`，在容器内映射到 `/disk2/www/`。

## 3. 保留原 PHP 配置

`php.ini` 控制 PHP 的内存、上传大小、时区、扩展加载等；`www.conf` 控制 FPM 的进程池、用户、监听地址和进程数量。

官方镜像可以使用默认配置启动，挂载这两个文件并不是必需的。如果要保留原有设置，有两种选择。

### 推荐：复制后独立维护

确认目标文件尚不存在，再执行一次，避免覆盖后续调整：

```bash
mkdir -p php82-official
cp php82/php.ini php82-official/php.ini
cp php82-fpm/www.conf php82-official/www.conf
```

这样保留原配置内容，新旧容器的配置后续可以分别修改。

### 可选：共用原文件

也可以直接挂载 `./php82/php.ini` 和 `./php82-fpm/www.conf`。不过以后在宿主机修改这些文件，两个容器下次加载时都会受影响。`:ro` 只限制容器内写入，不会阻止宿主机修改文件。

## 4. 在 Compose 中新增服务

编辑 `docker-compose.yaml`，在 `services:` 下面新增，与现有 `php82-fpm` 同级。以下采用上面复制出的独立配置：

```yaml
    php82-official:
        image: php:8.2-fpm
        volumes:
            - ${WWW_PATH}:/disk2/www/
            - ./php82-official/php.ini:/usr/local/etc/php/php.ini:ro
            - ./php82-official/www.conf:/usr/local/etc/php-fpm.d/www.conf:ro
        restart: unless-stopped
        networks:
            - backend
```

如果选择共用配置，将两个源路径分别改为 `./php82/php.ini` 和 `./php82-fpm/www.conf`。

这里没有设置 `ports`：FPM 使用 FastCGI 协议，由 OpenResty 在容器网络内访问，不需要发布到宿主机。新旧 PHP 容器都监听内部 9000 不会冲突。

也无需指定 `container_name`，Compose 会生成容器名；其他服务仍通过服务名 `php82-official` 访问它。以下 Compose 命令也使用该服务名。

官方镜像支持多种架构，默认按本机架构拉取。当前 Mac 是 Apple Silicon，不必照搬旧自定义镜像的 `platform: linux/amd64`，除非依赖确实要求该架构。

## 5. 拉取、启动并检查

```bash
docker compose config --quiet
docker compose up -d --no-deps php82-official
docker compose ps -a php82-official
docker compose logs --tail=100 php82-official
```

不必提前执行 `docker pull php:8.2-fpm`。本地没有镜像时，`up` 会自动拉取；如果本地已有同标签镜像而希望获取更新，先执行 `docker compose pull php82-official`。

按顺序执行，校验或启动失败时先解决问题，再继续。容器正常运行后检查：

```bash
docker compose exec php82-official php -v
docker compose exec php82-official php --ini
docker compose exec php82-official php -m
docker compose exec php82-official php-fpm -tt
```

重点检查：PHP 版本正确，ini 路径符合预期，项目所需扩展已加载，FPM 最终监听容器网络可访问的 `9000`，而不是仅监听 `127.0.0.1:9000`。

镜像内还存在其他 FPM 配置，例如 `zz-docker.conf`，可能覆盖 `www.conf` 的部分参数，最终以 `php-fpm -tt` 输出为准。

## 6. 保留配置不等于保留扩展

当前自定义 PHP 镜像中安装的 Redis、Swoole、xlswriter 等扩展，不会因为复制 `php.ini` 自动出现在官方镜像中。Composer 也通常需要另行安装。

如果出现 `Unable to load dynamic library`，先查看 `php --ini` 和 `php -m`，确认配置是否引用了未安装的扩展。不要仅通过忽略错误来切换业务；项目需要的扩展应在新镜像中安装。

例如业务只需要补充 MySQL PDO 扩展时，可以创建 `php82-official/Dockerfile`：

```dockerfile
FROM php:8.2-fpm
RUN docker-php-ext-install pdo_mysql
```

将新增服务中的 `image` 替换为本地镜像名，并添加 `build`，其他挂载和网络配置保持不变：

```yaml
        image: local/php82-official:dev
        build:
            context: ./php82-official
```

然后构建并启动：

```bash
docker compose up -d --no-deps --build php82-official
```

这个 Dockerfile 只安装 `pdo_mysql`，其他扩展按项目依赖另行配置。不要把旧镜像的扩展二进制直接复制到不同架构或不同构建的镜像。

## 7. 新增 Nginx 的 Laravel 配置片段

先确认新文件不存在，再复制：

```bash
cp nginx/config/laravel82.conf nginx/config/laravel82-official.conf
```

编辑 `nginx/config/laravel82-official.conf`，找到：

```nginx
fastcgi_pass php82-fpm:9000;
```

改为：

```nginx
fastcgi_pass php82-official:9000;
```

### 这个文件是什么意思

它是一份 Nginx 配置片段，不是 Laravel 程序，也不是镜像。它通常负责：

- 设置入口文件和静态资源规则。
- 将不存在的文件路径转给 Laravel 的 `index.php` 处理。
- 使用 `fastcgi_pass` 将 PHP 请求交给指定 FPM 服务。
- 设置 FastCGI 参数、超时和其他站点通用规则。

文件名是自定义的，`official` 只是方便辨认；真正决定请求交给哪个容器的是 `fastcgi_pass`。

宿主机 `nginx/config/` 已挂载到 OpenResty 的 `/etc/nginx/config/`，所以 Nginx 中引用的是容器路径。

## 8. 让指定项目加载新配置

找到目标项目的 `nginx/vhost/项目.conf`，只修改该项目的 include。

原来可能是：

```nginx
include /etc/nginx/config/laravel.conf;
```

或者：

```nginx
include /etc/nginx/config/laravel82.conf;
```

改成：

```nginx
include /etc/nginx/config/laravel82-official.conf;
```

站点完整结构示意，端口和项目路径以原站点为准：

```nginx
server {
    listen 10108;
    root /disk2/www/my-app/public;

    include /etc/nginx/config/laravel82-official.conf;
}
```

`vhost` 决定站点端口及项目 public 路径；被 include 的片段决定 Laravel 请求规则和 PHP 上游。只修改一个站点的 include，就只切换该站点。

不要把示意端口直接覆盖到现有站点；如果另建端口，还要确认 OpenResty 已发布该端口。当前环境发布了 `10100–10200`。

## 9. 校验、重载并验收

确认新 FPM 服务已经启动且扩展满足业务要求，再执行：

```bash
docker compose exec openresty nginx -t &&
docker compose exec openresty nginx -s reload
```

只有校验成功才重载。随后用目标项目的实际地址测试，例如：

```bash
curl -v --max-time 10 http://localhost:10108/
docker compose logs --tail=100 php82-official
```

还应检查站点自己的 Nginx 错误日志、Laravel `storage/logs`，并验证数据库、Redis 和实际业务接口。访问根路径是否返回 200 取决于项目路由，不能单凭根路径 404 判断切换失败。

需要在新容器执行 Laravel 命令时使用新服务名，下面的项目路径需替换：

```bash
docker compose exec -w /disk2/www/my-app php82-official php artisan config:clear
```

清除配置缓存会影响共享该源码的项目运行配置，应在需要重新加载业务 `.env` 时执行。现有队列 `php-worker` 仍为 PHP 7.4，不会因为 HTTP 请求切换到新 FPM 自动升级。

## 10. 回退和常见问题

需要回退时，将该站点的 include 恢复为切换前的原文件（`laravel.conf` 或 `laravel82.conf`），确认对应原 FPM 服务仍在运行，再校验并重载 OpenResty。回退完成且没有站点使用新服务后，可以停止它：

```bash
docker compose stop php82-official
```

| 现象 | 检查方向 |
| --- | --- |
| 镜像拉取失败 | Docker Hub 网络连通性、限流或登录要求 |
| 文件挂载失败 | 源文件是否存在，是否误建成目录 |
| 扩展加载错误 | 复制的 ini 是否引用了官方镜像没有的扩展 |
| host not found in upstream | 服务名拼写、新服务是否已启动、是否同在 backend 网络 |
| 502 | 新 FPM 是否运行、最终 listen 是否允许跨容器访问 |
| File not found / Primary script unknown | 两个容器的代码挂载、站点 root 和 SCRIPT_FILENAME 是否匹配 |
| Laravel 500 | 项目日志、缺少扩展、数据库/Redis 配置及文件权限 |
| 仍然请求旧 PHP | 实际加载的 vhost/include 是否正确，可用 `nginx -T` 查看 |

本流程只调整新增容器及指定站点的请求去向，不会自动迁移数据库、安装项目依赖或升级队列进程。
