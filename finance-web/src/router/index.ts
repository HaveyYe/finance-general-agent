/**
 * 路由配置
 * 定义应用的四个主要页面路由
 */
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/chat',
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('@/views/ChatView.vue'),
      meta: { title: '对话' },
    },
    {
      path: '/cockpit',
      redirect: '/chat',
    },
    {
      path: '/invoice',
      redirect: '/chat',
    },
    {
      path: '/voucher',
      redirect: '/chat',
    },
    {
      path: '/expense',
      name: 'expense',
      component: () => import('@/views/ExpenseAssistantView.vue'),
      meta: { title: '报销' },
    },
    {
      path: '/knowledge',
      name: 'knowledge',
      component: () => import('@/views/KnowledgeView.vue'),
      meta: { title: '知识库' },
    },
    {
      path: '/report',
      redirect: '/chat',
    },
  ],
})

export default router
