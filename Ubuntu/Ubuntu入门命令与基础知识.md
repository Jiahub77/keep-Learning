# Ubuntu 入门命令与基础知识

适用对象：第一次接触 Linux、准备使用 Ubuntu 桌面或服务器的学习者。\
适用环境：以 Ubuntu 22.04 / 24.04 LTS 为例，多数命令也适用于其他较新的 Ubuntu 版本。\
学习目标：能够独立操作文件、安装软件、管理权限、查看服务和日志，并完成基础网络排查。

> 建议先在虚拟机或测试环境练习。文中的文件操作默认在自己的练习目录中完成，不要直接拿系统目录做实验。示例中的用户名、地址、进程号需要替换成实际值。

## 一、先认识 Ubuntu

### 1. Ubuntu、Linux、终端和 Shell

| 名称     | 含义                                   |
| ------ | ------------------------------------ |
| Linux  | 通常指 Linux 内核，也常被用来统称基于它构建的操作系统       |
| Ubuntu | 基于 Linux 内核的操作系统发行版，包含软件管理、桌面或服务器工具等 |
| 终端     | 输入命令、查看文本输出的界面                       |
| Shell  | 解释和执行命令的程序，Ubuntu 常见默认 Shell 是 Bash  |
| root   | 系统超级管理员，权限非常高                        |
| sudo   | 以其他用户身份执行命令的工具，常用于获得管理员权限            |
| LTS    | 长期支持版本，适合希望环境稳定的学习和服务器使用场景           |

Ubuntu Desktop 带图形桌面；Ubuntu Server 通常以命令行管理为主。桌面版一般可以用 `Ctrl + Alt + T` 打开终端，也可以在应用菜单中搜索“终端”。

### 2. 查看自己的系统

```bash
cat /etc/os-release
uname -r
whoami
hostname
```

依次查看发行版信息、内核版本、当前用户名和主机名。不同版本的软件包、默认配置可能不同，遇到教程不一致时，先核对系统版本。

## 二、命令行的基本规则

### 1. 命令的结构

```text
命令 [选项] [参数]
```

例如：

```bash
ls -lh /var/log
```

其中 `ls` 是命令，`-l` 表示详细列表，`-h` 表示使用更易读的容量单位，`/var/log` 是目标路径。文档中的方括号通常表示“可选”，不需要把方括号一起输入。

### 2. 必须记住的规则

- Linux 文件名区分大小写：`notes.txt` 和 `Notes.txt` 是两个不同文件。
- 命令、选项和参数之间通常用空格分隔。
- 路径带空格时加引号，例如 `cd "My Documents"`。
- 复制教程时不要输入行首用于表示提示符的 `$` 或 `#`。
- 文件扩展名通常只是命名习惯，文件是否可执行主要看权限和内容。
- 以 `.` 开头的文件是隐藏文件，例如 `.bashrc`。

### 3. 常用快捷操作

| 操作         | 用途                           |
| ---------- | ---------------------------- |
| `Tab`      | 补全命令或路径，部分场景连续按两次可查看候选项      |
| 方向键上 / 下   | 查找之前执行的命令                    |
| `Ctrl + C` | 请求中断前台程序                     |
| `Ctrl + D` | 在空提示符处通常退出 Shell；输入数据时表示输入结束 |
| `Ctrl + L` | 清理当前屏幕显示，不删除历史记录             |
| `Ctrl + R` | 搜索命令历史                       |
| `q`        | 退出 `less`、`man` 等分页阅读界面      |

终端里的复制、粘贴快捷键常为 `Ctrl + Shift + C` 和 `Ctrl + Shift + V`，具体取决于终端程序。

### 4. 不会用时先查帮助

```bash
ls --help
man ls
command -v bash
history
```

`--help` 适合快速查参数，`man` 适合查看完整手册。手册中可以输入 `/关键词` 搜索，按 `n` 跳到下一个结果，按 `q` 退出。`command -v` 可查询命令的解析位置；`history` 可查看历史命令，不要把密码或令牌直接写进命令行。

## 三、目录结构和路径

### 1. 常见目录

| 路径              | 主要用途                      |
| --------------- | ------------------------- |
| `/`             | 整个文件系统的根目录                |
| `/home`         | 普通用户的家目录，例如 `/home/alice` |
| `/root`         | root 用户的家目录，与 `/` 不是一回事   |
| `/etc`          | 系统和软件的配置文件                |
| `/var/log`      | 常见日志目录                    |
| `/var/lib`      | 服务运行时需要持久保存的数据            |
| `/tmp`          | 临时文件目录，内容可能被系统清理          |
| `/usr`          | 大量系统程序、库和共享资源             |
| `/opt`          | 部分第三方软件的安装目录              |
| `/dev`          | 设备文件                      |
| `/proc`         | 内核和进程信息的虚拟文件系统            |
| `/mnt`、`/media` | 常见挂载位置                    |

Ubuntu 不按 Windows 的 `C:`、`D:` 组织路径，磁盘或分区通常通过“挂载”接入某个目录。家目录的具体位置以 `echo "$HOME"` 的结果为准。

### 2. 路径的含义

| 写法                 | 含义           |
| ------------------ | ------------ |
| `/home/alice/demo` | 从根目录开始的绝对路径  |
| `demo/notes.txt`   | 相对于当前目录的相对路径 |
| `.`                | 当前目录         |
| `..`               | 上一级目录        |
| `~`                | 当前用户的家目录     |

### 3. 移动和查看目录

```bash
pwd
ls
ls -lah
cd ~
mkdir -p ~/ubuntu-practice/docs
cd ~/ubuntu-practice
cd docs
cd ..
cd -
```

`pwd` 显示当前目录；`ls -lah` 包括隐藏文件和详细信息；`mkdir -p` 可以创建多层目录；`cd -` 返回上一次所在目录。

## 四、文件与目录操作

下面先进入练习目录：

```bash
mkdir -p ~/ubuntu-practice
cd ~/ubuntu-practice
```

### 1. 新建、复制、移动

```bash
touch notes.txt
mkdir -p docs
cp notes.txt notes-backup.txt
cp -r docs docs-backup
mv notes-backup.txt notes-old.txt
mv notes-old.txt docs/
```

- `touch`：文件不存在时创建空文件；存在时更新时间戳，不会清空内容。
- `cp`：复制文件；复制目录通常需要 `-r`。
- `mv`：移动或重命名，目标文件已存在时可能被覆盖。

练习时可以用 `cp -i`、`mv -i`，在覆盖前请求确认。

### 2. 删除

```bash
rm -i docs/notes-old.txt
rmdir docs-backup
```

`rm` 删除文件，通常不会送入桌面的回收站；`rmdir` 只能删除空目录。删除非空目录可以使用 `rm -r 目录名`，执行前必须确认当前目录和目标路径。

> 不要把 `sudo rm -rf` 当作普通清理命令。`-r` 表示递归删除，`-f` 表示强制处理、减少提示，路径写错可能造成严重数据丢失。

### 3. 查看内容

```bash
cat notes.txt
less /etc/os-release
head -n 10 /etc/passwd
tail -n 10 /etc/passwd
```

`cat` 适合短文件，`less` 适合长文件，`head`、`tail` 分别查看开头和末尾。`/etc/passwd` 记录账户基本信息，不保存现代 Ubuntu 用户的密码哈希。

持续观察日志时可使用：

```bash
tail -f app.log
```

这里的 `app.log` 必须是实际存在且有权读取的日志文件；按 `Ctrl + C` 停止观察。

### 4. 编辑文本

```bash
nano notes.txt
```

在 Nano 中输入内容后，按 `Ctrl + O` 写入，再按回车确认文件名，按 `Ctrl + X` 退出。如果系统没有 Nano，可以执行 `sudo apt install nano` 安装。

修改系统配置前先备份，并阅读对应服务的说明；只有确实需要时才使用管理员权限编辑。

## 五、搜索、管道和重定向

### 1. 查找文件

```bash
find ~/ubuntu-practice -type f -name '*.txt'
find ~/ubuntu-practice -type d -name 'docs'
```

`-type f` 表示普通文件，`-type d` 表示目录，`-name` 按名称匹配。通配符用引号包住，避免先被 Shell 展开。`-iname` 可以忽略大小写。

### 2. 搜索文件内容

```bash
grep -n 'Ubuntu' notes.txt
grep -in 'ubuntu' notes.txt
grep -rIn 'Ubuntu' ~/ubuntu-practice
```

`-n` 显示行号，`-i` 忽略大小写，`-r` 递归搜索目录。只想匹配普通文本而不是正则表达式时，可以增加 `-F`。

### 3. 写入和追加

```bash
printf 'Ubuntu learning\n' > notes.txt
printf 'Practice every day\n' >> notes.txt
cat notes.txt
```

`>` 会覆盖目标文件，`>>` 会追加内容。重定向由 Shell 在执行命令前处理，因此 `cat notes.txt > notes.txt` 会先清空文件，不要这样操作。

### 4. 管道和错误输出

```bash
ls -la | less
grep -F 'Ubuntu' notes.txt | wc -l
find ~/ubuntu-practice -type f > files.txt 2> errors.txt
```

`|` 把前一个命令的标准输出交给后一个命令；`wc -l` 统计行数；`2>` 把错误输出保存到文件。上述文件清单示例会在当前目录创建输出文件，因此清单中可能包含这些文件自身。

### 5. 命令之间的关系

| 写法           | 含义                   | <br /> | <br />       |
| ------------ | -------------------- | :----- | :----------- |
| `命令1 && 命令2` | 命令1成功后才执行命令2         | <br /> | <br />       |
| \`命令1        | <br />               | 命令2\`  | 命令1失败后才执行命令2 |
| `命令1 ; 命令2`  | 不论命令1是否成功，都接着执行命令2   | <br /> | <br />       |
| `命令 &`       | 让命令在后台运行             | <br /> | <br />       |
| `echo $?`    | 查看上一条命令的退出状态，通常0表示成功 | <br /> | <br />       |

## 六、用户、sudo 和文件权限

### 1. 查看身份

```bash
whoami
id
sudo -l
```

`id` 显示用户和所属组；`sudo -l` 查看当前用户被允许通过 sudo 执行的命令，可能要求输入密码。输入 sudo 密码时通常不会显示字符或星号，这是正常现象。

不要长期使用 root 账户处理日常工作。遇到“Permission denied”时，先检查路径和权限，不要直接给所有命令加 sudo。

### 2. 理解权限字符串

执行 `ls -l`，可能看到：

```text
-rw-r--r-- 1 alice alice 120 Oct 27 10:00 notes.txt
```

第一个字符表示类型：`-` 是普通文件，`d` 是目录，`l` 是符号链接。接下来的九个字符，每三个一组，分别表示所有者、所属组和其他用户的权限。

| 权限  | 对普通文件的意义            | 对目录的意义             |
| --- | ------------------- | ------------------ |
| `r` | 读取内容                | 列出目录中的名称           |
| `w` | 修改内容                | 配合执行权限创建、删除或重命名目录项 |
| `x` | 允许作为程序执行，仍需有效的可执行内容 | 进入目录、访问目录项         |

删除文件主要受父目录权限影响，还可能受到粘滞位、只读挂载等限制，并不只取决于文件本身是否可写。

### 3. 修改权限和所有者

```bash
chmod 644 notes.txt
chmod 600 notes.txt
chmod u+x hello.sh
```

数字权限中 `r=4`、`w=2`、`x=1`，相加得到一组权限：`644` 是所有者可读写、其他用户只读；`600` 是只有所有者可读写；`755` 常用于可执行文件或可供其他用户访问的目录。

`hello.sh` 需要先创建，后面的脚本章节会演示。修改所有者的命令形式是 `sudo chown 用户名:组名 文件路径`，用户名和组名必须存在。

> 不要用 `chmod -R 777` 解决权限问题。它会扩大写入权限，并可能破坏目录中原有的权限设计。

## 七、安装、更新和卸载软件

Ubuntu 常用 APT 管理软件包；安装系统软件一般需要 sudo 权限和网络连接。

```bash
sudo apt update
apt list --upgradable
sudo apt upgrade
```

- `apt update`：更新本地的软件包索引，不会直接升级已安装软件。
- `apt list --upgradable`：查看可升级的软件包。
- `apt upgrade`：根据软件源和依赖关系升级软件，执行前阅读确认列表。

### 常用软件管理命令

```bash
apt search tree
apt show tree
sudo apt install tree
tree ~/ubuntu-practice
sudo apt remove tree
```

`remove` 通常保留软件包管理的部分配置文件；`sudo apt purge 包名` 还会移除该软件包管理的配置，但不会保证删除所有用户数据。`sudo apt autoremove` 会清理自动安装且已不再需要的依赖，执行前检查删除列表。

补充知识：

- `dpkg -l` 可以查看软件包安装信息。
- Ubuntu 也可能使用 Snap 分发软件，可以用 `snap list` 查看；极简环境未必安装 Snap。
- 软件包名称和命令名称不一定相同。
- 不要同时运行多个 APT 安装或更新任务。遇到锁占用先等待或调查对应进程，不要直接删除锁文件。
- 不要随意添加未知软件源，也不要直接执行来源不明的下载脚本。
- 普通软件更新不等于跨版本升级 Ubuntu；发行版升级应单独规划和备份。

## 八、进程、服务和日志

### 1. 查看进程

```bash
ps aux
top
pgrep -a bash
```

进程是正在运行的程序实例，PID 是进程编号。`top` 可动态查看 CPU 和内存占用，按 `q` 退出；`pgrep -a` 根据进程名查找并显示命令行。

停止自己的进程时，先核实 PID，再执行 `kill PID`。默认发送 `SIGTERM`，让程序有机会清理资源。`kill -9 PID` 强制结束进程，可能导致未保存数据丢失，只在正常停止无效且确认影响后考虑。

### 2. 管理系统服务

多数 Ubuntu 桌面和服务器使用 systemd。以下以常见的 `cron` 服务为例，精简镜像可能未安装它：

```bash
systemctl status cron
systemctl is-enabled cron
systemctl list-units --type=service --state=running
```

对确认需要管理的服务，可使用以下形式：

| 命令形式                              | 作用               |
| --------------------------------- | ---------------- |
| `sudo systemctl start 服务名`        | 立即启动             |
| `sudo systemctl stop 服务名`         | 立即停止             |
| `sudo systemctl restart 服务名`      | 重新启动，可能中断正在处理的请求 |
| `sudo systemctl reload 服务名`       | 请求重新加载配置，仅部分服务支持 |
| `sudo systemctl enable 服务名`       | 设置开机启动，通常不会立即启动  |
| `sudo systemctl disable 服务名`      | 取消开机启动，通常不会立即停止  |
| `sudo systemctl enable --now 服务名` | 设置开机启动并立即启动      |

容器或部分 WSL 环境可能没有使用 systemd 作为 PID 1；遇到相关报错，应先确认运行环境。

### 3. 查看系统日志

```bash
journalctl -u cron -n 50 --no-pager
journalctl -u cron --since '1 hour ago'
journalctl -u cron -f
```

日志可见范围取决于当前用户权限，必要时使用 sudo。`-u` 按服务过滤，`-n` 限制条数，`-f` 持续观察。日志可能包含敏感数据，分享前先脱敏。

## 九、磁盘、内存和系统资源

```bash
df -h
du -sh ~/ubuntu-practice
free -h
uptime
lsblk
```

| 命令          | 用途                            |
| ----------- | ----------------------------- |
| `df -h`     | 查看已挂载文件系统的容量和剩余空间             |
| `du -sh 路径` | 汇总目录或文件占用的空间                  |
| `free -h`   | 查看内存和交换空间，判断余量时重点参考 available |
| `uptime`    | 查看运行时间和系统负载，负载不是 CPU 使用率百分比   |
| `lsblk`     | 查看块设备、分区和挂载点                  |

磁盘空间看似足够但无法新建文件时，还可以执行 `df -i` 检查 inode 使用情况。入门阶段不要随意执行格式化、分区、`dd` 写盘等操作。

## 十、网络基础和排查

### 1. 先认识几个概念

| 概念          | 简单解释                                |
| ----------- | ----------------------------------- |
| IP 地址       | 网络接口上的地址，用于网络通信                     |
| 网关          | 通往其他网络的下一跳设备                        |
| DNS         | 把域名解析为 IP 地址等记录的系统                  |
| 端口          | 主机上区分网络服务的编号，如 SSH 常用22、HTTPS 常用443 |
| `127.0.0.1` | IPv4 回环地址，表示当前主机自身                  |
| `0.0.0.0`   | 作为监听地址时通常表示所有本地 IPv4 接口             |
| 公网 / 私网     | 私网地址通常不能从公网直接访问，需要路由、映射或其他接入方式      |

### 2. 常用网络命令

```bash
ip addr
ip route
getent ahosts ubuntu.com
ping -c 4 ubuntu.com
ss -lnt
curl -I https://ubuntu.com
```

分别查看地址、路由、名称解析、ICMP 连通性、TCP 监听端口和 HTTP 响应头。`curl` 未安装时可用 `sudo apt install curl` 安装。`sudo ss -lntp` 还能显示有权限查看的监听进程。

注意：`ping` 失败可能是对方屏蔽 ICMP，不一定代表网站不可访问；`curl -I` 发送 HEAD 请求，某些服务不支持 HEAD，需要进一步用实际 GET 请求判断。

推荐排查顺序：本机是否有地址 → 默认路由是否存在 → DNS 是否正常 → 目标端口是否可达 → 服务响应与日志是否正常。监听端口存在不等于公网一定能访问，还可能受主机防火墙、云安全组、路由或 NAT 影响。

### 3. 防火墙入门

Ubuntu 常见的防火墙管理工具是 UFW，可先查看：

```bash
sudo ufw status verbose
```

如果 UFW 未安装，命令会提示不存在。远程服务器启用防火墙前，必须先允许实际 SSH 端口，并检查云平台安全组与备用控制台访问方式。不要在不清楚规则时直接启用或重置防火墙。

## 十一、SSH 远程连接和文件传输

连接一台已经配置好 SSH 服务的主机：

```bash
ssh alice@192.168.1.100
ssh -p 2222 alice@192.168.1.100
```

第一条使用默认端口22，第二条指定端口2222。首次连接会提示核对主机密钥指纹，应通过可信渠道与服务器端信息核对后再接受。主机密钥突然变化时，先确认是否重装系统或更换了服务器。

传输一个文件到远程用户的家目录：

```bash
scp notes.txt alice@192.168.1.100:~/
scp -P 2222 notes.txt alice@192.168.1.100:~/
```

注意 `ssh` 指定端口是小写 `-p`，`scp` 是大写 `-P`。连接成功后用 `exit` 退出远程 Shell。

如果 Ubuntu 需要作为 SSH 服务器接收连接，可在目标 Ubuntu 上安装 `openssh-server`，并用 `systemctl status ssh` 检查服务。仅作为客户端连接其他机器时，不需要安装 SSH 服务端。

长期使用建议配置 SSH 密钥登录：妥善保护私钥，设置口令，不要把私钥上传到代码仓库或发送给他人。禁用密码登录前，先验证密钥登录和备用访问方式确实可用。

## 十二、压缩、解压和链接

### 1. tar.gz 归档

在练习目录中执行：

```bash
cd ~/ubuntu-practice
tar -czf docs.tar.gz docs
tar -tzf docs.tar.gz
mkdir -p extracted
tar -xzf docs.tar.gz -C extracted
```

`c` 创建归档，`t` 列出内容，`x` 解包，`z` 使用 gzip，`f` 指定归档文件。解包前先查看内容，并使用单独目录，避免覆盖已有文件。不要用 sudo 解压来源不明的归档。

### 2. ZIP 文件

```bash
sudo apt install zip unzip
zip -r docs.zip docs
unzip -l docs.zip
unzip docs.zip -d zip-extracted
```

### 3. 符号链接

```bash
ln -s notes.txt notes-link.txt
ls -l notes-link.txt
```

符号链接可以理解为指向另一个路径的引用。这里的相对目标 `notes.txt` 是相对于链接所在目录解析的；目标被移动或删除后，链接可能失效。

## 十三、环境变量和第一个脚本

### 1. 环境变量

```bash
echo "$HOME"
echo "$PATH"
export LEARNING_TOPIC='Ubuntu'
printf '%s\n' "$LEARNING_TOPIC"
```

`HOME` 是家目录；`PATH` 是 Shell 查找外部命令的目录列表。`export` 设置的变量通常只对当前 Shell 及随后启动的子进程有效，不会自动永久保存。

交互式 Bash 常从 `~/.bashrc` 加载配置，但登录 Shell 和其他 Shell 的启动规则不同。不要不加判断地把配置追加到所有启动文件中。

### 2. 创建并运行脚本

```bash
cd ~/ubuntu-practice
nano hello.sh
```

输入以下内容并保存：

```bash
#!/usr/bin/env bash

# 显示当前用户、日期和工作目录
printf '用户：%s\n' "$(whoami)"
printf '时间：%s\n' "$(date '+%F %T')"
printf '目录：%s\n' "$PWD"
```

执行：

```bash
bash hello.sh
chmod u+x hello.sh
./hello.sh
```

`bash hello.sh` 由 Bash 读取脚本，不要求脚本有执行权限；`./hello.sh` 表示执行当前目录中的脚本，需要执行权限。当前目录通常不在 `PATH` 中，所以直接输入 `hello.sh` 可能提示找不到命令。

脚本中的变量引用通常应该加双引号，例如 `"$PWD"`，防止空格和通配符导致意外拆分或展开。

## 十四、综合练习：建立学习记录并备份

目标：在自己的家目录中创建笔记，搜索内容，制作归档，再解压验证。只使用练习目录，不需要 sudo。

```bash
mkdir -p ~/ubuntu-practice/day1
cd ~/ubuntu-practice/day1
printf '今天学习 Ubuntu\n掌握 pwd、ls、cd\n' > learning.txt
cat learning.txt
cp -i learning.txt learning-backup.txt
grep -n 'Ubuntu' learning.txt
wc -l learning.txt
chmod 600 learning.txt
ls -l learning.txt
tar -czf day1-notes.tar.gz learning.txt learning-backup.txt
tar -tzf day1-notes.tar.gz
mkdir -p restored
tar -xzf day1-notes.tar.gz -C restored
diff learning.txt restored/learning.txt
```

预期结果：`grep` 找到包含 Ubuntu 的一行，`wc -l` 显示2行，`learning.txt` 的权限为 `-rw-------`，归档中包含两份笔记。`diff` 没有输出且退出状态为0，表示两个文件内容相同。

重复练习时，写入命令会覆盖同名练习文件，`cp -i` 可能提示是否覆盖。学完后可以保留整个目录作为笔记，不必马上删除。

## 十五、七天学习安排

| 天数  | 学习重点              | 完成标准                     |
| --- | ----------------- | ------------------------ |
| 第1天 | 终端、帮助、路径、目录结构     | 能说明当前目录、家目录和根目录的区别       |
| 第2天 | 创建、复制、移动、删除、文本编辑  | 能独立建立自己的笔记目录             |
| 第3天 | find、grep、管道、重定向  | 能从多个文本文件中找到指定内容          |
| 第4天 | 用户、sudo、权限        | 能读懂 `ls -l`，知道600和644的区别 |
| 第5天 | APT、进程、systemd、日志 | 能安装软件、查看服务状态和日志          |
| 第6天 | IP、DNS、端口、SSH     | 能描述远程连接失败时的排查顺序          |
| 第7天 | 归档、环境变量、脚本、综合练习   | 能完成备份恢复并验证文件内容           |

建议每天投入30～60分钟，采用“读一节 → 手动输入 → 观察结果 → 写下自己的解释”的方式学习。先记住命令的用途，再逐步熟悉参数。

## 十六、常见问题速查

| 问题                          | 优先检查                                             |
| --------------------------- | ------------------------------------------------ |
| `command not found`         | 拼写是否正确、软件是否安装、程序目录是否在 `PATH` 中                   |
| `No such file or directory` | 当前目录、路径拼写、大小写、符号链接目标；执行脚本时也检查解释器路径               |
| `Permission denied`         | 文件和父目录权限、所有者、执行权限；确有必要时再考虑 sudo                  |
| `sudo` 提示用户无权限              | 当前用户是否被管理员授予 sudo 权限，不要随意改 sudo 配置               |
| 输入密码没反应                     | 终端通常不显示密码字符，输入完成后按回车                             |
| 服务启动失败                      | 查看 `systemctl status` 和对应 `journalctl`，检查配置与端口占用 |
| 域名访问失败                      | 查看名称解析、路由、代理和服务响应，不能只依赖 ping                     |
| SSH 连接被拒绝                   | 地址和端口是否正确、服务是否监听、防火墙是否主动拒绝                       |
| SSH 连接超时                    | 检查网络路径、防火墙、云安全组及目标主机状态                           |
| 磁盘空间不足                      | 用 `df -h`、`df -i`、`du -sh` 定位，确认用途后再清理           |
| APT 提示锁被占用                  | 是否正在自动更新或有其他包管理任务，先等待或检查进程                       |
| `systemctl` 提示没有 systemd    | 是否处于容器、chroot 或未启用 systemd 的 WSL 环境              |

## 十七、核心命令速查表

| 类别 | 命令                                  | 记忆要点              |
| -- | ----------------------------------- | ----------------- |
| 帮助 | `man`、`--help`                      | 不会先查帮助            |
| 位置 | `pwd`、`cd`、`ls`                     | 看位置、换位置、列内容       |
| 文件 | `touch`、`mkdir`、`cp`、`mv`、`rm`      | 创建、复制、移动、删除       |
| 阅读 | `cat`、`less`、`head`、`tail`          | 按内容长度和场景选择        |
| 编辑 | `nano`                              | 入门文本编辑器           |
| 搜索 | `find`、`grep`                       | 找文件、找内容           |
| 身份 | `whoami`、`id`、`sudo`                | 当前身份与管理员权限        |
| 权限 | `chmod`、`chown`                     | 修改权限、修改所有者        |
| 软件 | `apt update/install/upgrade/remove` | 更新索引、安装、升级、卸载     |
| 进程 | `ps`、`top`、`pgrep`、`kill`           | 查看、定位、停止进程        |
| 服务 | `systemctl`、`journalctl`            | 服务状态和日志           |
| 资源 | `df`、`du`、`free`、`lsblk`            | 空间、目录占用、内存、磁盘     |
| 网络 | `ip`、`getent`、`ping`、`ss`、`curl`    | 地址、解析、连通性、端口、HTTP |
| 远程 | `ssh`、`scp`                         | 远程登录和复制           |
| 归档 | `tar`、`zip`、`unzip`                 | 打包和解包             |

## 十八、学完后的自测

1. `apt update` 和 `apt upgrade` 有什么区别？
2. `/`、`/root` 和 `~` 分别表示什么？
3. 为什么 `>` 比 `>>` 更容易误覆盖文件？
4. `644` 和 `600` 分别允许谁读取文件？
5. 为什么不应该用 `chmod -R 777` 解决权限问题？
6. 服务已运行和服务已设置开机启动是一回事吗？
7. 网站能打开，但 ping 不通，是否一定异常？
8. 为什么执行当前目录的脚本经常需要写 `./hello.sh`？
9. 删除文件时，它本身不可写是否就一定删不掉？
10. 怎样确认备份解压后的文件与原文件内容一致？

参考答案：

1. 前者刷新软件包索引，后者升级已安装的软件包。
2. 分别是文件系统根目录、root 的家目录、当前用户的家目录。
3. `>` 覆盖内容，`>>` 在末尾追加内容。
4. `644` 允许所有者读写、组和其他用户读取；`600` 只有所有者可读写，特权访问另论。
5. 它给所有人开放读写执行权限，扩大风险并破坏原有权限设计。
6. 不是，运行状态与开机启动配置是两个不同维度。
7. 不一定，对方或网络可能限制 ICMP，但允许 HTTP/HTTPS。
8. 当前目录通常不在 `PATH` 中，`./` 明确指定脚本路径。
9. 不是，通常主要取决于父目录权限，同时受其他系统约束影响。
10. 可用 `diff 原文件 恢复文件` 比较；重要备份还应定期验证完整恢复流程。

## 十九、后续学习与官方资料

入门后可继续学习 Bash 脚本、SSH 密钥、定时任务、日志轮转、备份恢复和防火墙；准备维护服务器时，再系统学习磁盘挂载、用户隔离和服务部署。

- Ubuntu 文档入口：<https://docs.ubuntu.com/>
- Ubuntu Server 文档：<https://documentation.ubuntu.com/server/>
- Ubuntu 官方教程：<https://ubuntu.com/tutorials>
- Ubuntu 手册查询：<https://manpages.ubuntu.com/>

遇到版本差异时，以本机 `man`、命令帮助和对应 Ubuntu 版本的官方文档为准。
