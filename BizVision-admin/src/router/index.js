import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/dashboard' },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('../views/DashboardView.vue'),
    meta: { title: '概览', subtitle: '商品与文章的整体情况' },
  },
  {
    path: '/products',
    name: 'products',
    component: () => import('../views/ProductsView.vue'),
    meta: { title: '商品管理', subtitle: '新增、编辑、删除商品，扣减库存' },
  },
  {
    path: '/articles',
    name: 'articles',
    component: () => import('../views/ArticlesView.vue'),
    meta: { title: '文章管理', subtitle: '新增、编辑、删除文章（软删除）' },
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

// 用 hash 模式，打包后直接丢到 Spring Boot 的 static 目录也不用配转发规则
export default createRouter({
  history: createWebHashHistory(),
  routes,
})
