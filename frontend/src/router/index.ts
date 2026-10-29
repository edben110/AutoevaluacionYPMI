import { createRouter, createWebHistory } from 'vue-router'
import { hasSession, signOut } from '@/auth/session'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/secretaria/usuarios/nuevo', name: 'user-registration', component: () => import('@/views/UserRegistrationView.vue') },
    { path: '/mi-cuenta', name: 'account', component: () => import('@/views/AccountView.vue') },
    { path: '/salir', name: 'logout', component: { render: () => null }, beforeEnter: () => { signOut(); return { name: 'login' } } },
    {
      path: '/entregas',
      name: 'deliveries',
      component: () => import('@/views/DeliveriesView.vue'),
    },
    {
      path: '/entregas/:year/areas/:areaId',
      name: 'delivery-review',
      component: () => import('@/views/DeliveryReviewView.vue'),
    },
    {
      path: '/pmi/:year/areas/:areaId/seguimiento',
      name: 'pmi-follow-up',
      component: () => import('@/views/PmiFollowUpView.vue'),
    },
    {
      path: '/pmi/:year/areas/:areaId/indicadores',
      name: 'pmi-indicators',
      component: () => import('@/views/PmiIndicatorsView.vue'),
    },
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
