import { createRouter, createWebHistory } from 'vue-router'
import { hasSession } from '@/auth/session'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/dashboard' },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/views/DashboardView.vue'),
    },
    {
      path: '/autoevaluacion',
      name: 'self-evaluation',
      component: () => import('@/views/SelfEvaluationView.vue'),
    },
  ],
})

router.beforeEach((to) => {
  if (to.name !== 'login' && !hasSession.value) return { name: 'login' }
  if (to.name === 'login' && hasSession.value) return { name: 'dashboard' }
})

export default router
