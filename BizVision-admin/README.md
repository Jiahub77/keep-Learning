# BizVision 后台管理（admin）

Vue 3 + Vite + Tailwind CSS，对接 `BizVision` 的 `/products` 和 `/article` 接口。

## 启动

```bash
# 1. 先起后端（需要数据库密码环境变量）
export BIZVISION_DB_PASSWORD=你的密码
mysql -u jia -p bizvision_learning < ../sql/schema.sql   # 首次建表
# IDEA 里运行 BizVisionApplication，监听 8080

# 2. 起前端
cd admin
npm install
npm run dev        # http://localhost:5173
```

开发环境由 Vite 把 `/products`、`/article` 代理到 `http://localhost:8080`，所以后端不需要配 CORS。
后端端口不是 8080 时：

```bash
VITE_PROXY_TARGET=http://localhost:9090 npm run dev
```

## 没有数据库时先看界面

仓库自带一个内存版 mock 服务，接口契约和真实后端一致：

```bash
npm run mock                                   # 终端 A，监听 8081
VITE_PROXY_TARGET=http://localhost:8081 npm run dev   # 终端 B
```

## 打包

```bash
npm run build      # 产物在 admin/dist
```

把 `dist/` 整个复制到 `src/main/resources/static/admin/`，即可通过 `http://localhost:8080/admin/` 访问。
路由用的是 hash 模式（`/#/products`），不需要额外配转发规则。

## 目录

```
src/
├── api/            # 接口封装（字段归一化、错误信息统一）
│   ├── http.js     # fetch 封装，把后端的纯文本/JSON 错误统一成 ApiError
│   ├── products.js
│   └── articles.js
├── components/     # BaseModal / ConfirmDialog / PaginationBar / StateBlock / ToastHost
├── composables/    # useToast
├── utils/format.js # 分↔元换算、时间格式化
├── views/          # Dashboard / Products / Articles
└── router/
```

## 和后端约定的小细节

- 商品价格：数据库存「分」，界面显示「元」，提交时 `Math.round(元 * 100)`。
- 扣库存请求体是**裸数字**（`2`），不是 `{"quantity": 2}`；库存不足返回 409。
- 文章删除是软删除（`is_on = 0`），列表和详情都不再返回它。
