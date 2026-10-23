import { createRouter, createWebHistory } from 'vue-router'
import { hasSession } from '@/auth/session'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/pmi/:year/areas/:areaId/formulacion',
      name: 'pmi-formulation',
      component: () => import('@/views/PmiFormulationView.vue'),
    },
    {
      path: '/pmi/:year/areas/:areaId',
      name: 'pmi',
      component: () => import('@/views/PmiView.vue'),
    },
    {
      path: '/pmi/:year',
      name: 'pmi-overview',
      component: () => import('@/views/PmiOverviewView.vue'),
    },
    {
      path: '/secretaria/periodos-autoevaluacion',
      name: 'secretary-periods',
      component: () => import('@/views/SecretaryPeriodsView.vue'),
    },
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
      path: '/autoevaluacion/:areaId?',
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
