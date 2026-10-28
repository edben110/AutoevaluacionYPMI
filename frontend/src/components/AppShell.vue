<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { currentProfile, hasSession, loadProfile } from '@/auth/session'
import SemIcon from '@/components/SemIcon.vue'
import SemWave from '@/components/SemWave.vue'
import InstitutionalBrand from '@/components/InstitutionalBrand.vue'

const route = useRoute()
const router = useRouter()
const compact = ref(window.matchMedia('(max-width: 1000px)').matches)
const navigationOpen = ref(!compact.value)
const accountOpen = ref(false)
const navButton = ref<HTMLButtonElement | null>(null)
const accountButton = ref<HTMLButtonElement | null>(null)
const accountPanel = ref<HTMLElement | null>(null)
const secretary = computed(() => currentProfile.value?.role === 'SECRETARY')
const title = computed(() => {
  if (route.name === 'dashboard') return route.query.section === 'autoevaluacion' ? 'AUTOEVALUACIÓN' : route.query.section === 'pmi' ? 'PLAN DE MEJORAMIENTO INSTITUCIONAL' : 'INICIO'
  if (route.name === 'secretary-periods') return 'DEFINICIÓN DE FECHAS'
  if (route.name === 'account') return 'DATOS DE LA CUENTA'
  if (route.name === 'deliveries') return secretary.value ? 'PAQUETES INSTITUCIONALES' : 'AUTOEVALUACIONES ANTERIORES'
  if (route.name === 'delivery-review') return 'REVISIÓN DE AUTOEVALUACIÓN Y PMI'
  if (String(route.name).startsWith('pmi')) return 'PLAN DE MEJORAMIENTO INSTITUCIONAL'
  return 'AUTOEVALUACIÓN'
})
const links = computed(() => [
  { label: 'Inicio', icon: 'home', to: { name: 'dashboard' }, active: route.name === 'dashboard' && !route.query.section },
  ...(secretary.value ? [
    { label: 'Definir fechas', icon: 'document-add', to: { name: 'secretary-periods' }, active: route.name === 'secretary-periods' },
    { label: 'Autoevaluación y PMI', icon: 'calendar', to: { name: 'deliveries' }, active: ['deliveries', 'delivery-review'].includes(String(route.name)) },
  ] : [
    { label: 'Autoevaluación', icon: 'calendar', to: { name: 'dashboard', query: { section: 'autoevaluacion' } }, active: route.name === 'self-evaluation' || route.query.section === 'autoevaluacion' },
    { label: 'PMI', icon: 'document', to: { name: 'dashboard', query: { section: 'pmi' } }, active: String(route.name).startsWith('pmi') || route.query.section === 'pmi' },
    { label: 'Autoevaluaciones anteriores', icon: 'history', to: { name: 'deliveries' }, active: ['deliveries', 'delivery-review'].includes(String(route.name)) },
  ]),
])
function closeOverlays(): void {
  if (accountOpen.value) { accountOpen.value = false; accountButton.value?.focus() }
  else if (compact.value && navigationOpen.value) { navigationOpen.value = false; navButton.value?.focus() }
}
function keyboard(event: KeyboardEvent): void { if (event.key === 'Escape') closeOverlays() }
function outside(event: PointerEvent): void {
  const target = event.target as Node
  if (accountOpen.value && !accountPanel.value?.contains(target) && !accountButton.value?.contains(target)) accountOpen.value = false
}
function resize(): void {
  const next = window.matchMedia('(max-width: 1000px)').matches
  if (next !== compact.value) { compact.value = next; navigationOpen.value = !next }
}
async function logout(): Promise<void> {
  // La navegación respeta los avisos de formularios con cambios pendientes.
  await router.push({ name: 'logout' })
}
watch(() => route.fullPath, () => { accountOpen.value = false; if (compact.value) navigationOpen.value = false })
onMounted(() => {
  void loadProfile().catch(() => { if (!hasSession.value) void router.replace({ name: 'login' }) })
  window.addEventListener('keydown', keyboard); window.addEventListener('pointerdown', outside); window.addEventListener('resize', resize)
})
onUnmounted(() => { window.removeEventListener('keydown', keyboard); window.removeEventListener('pointerdown', outside); window.removeEventListener('resize', resize) })
</script>

<template>
  <div class="app-shell" :class="{ 'with-navigation': navigationOpen && !compact }">
    <a class="skip-link" href="#workspace">Saltar al contenido</a>
    <header class="sem-topbar">
      <button ref="navButton" class="shell-icon-button" type="button" :aria-expanded="navigationOpen" aria-controls="main-navigation" :aria-label="navigationOpen ? 'Cerrar menú de navegación' : 'Abrir menú de navegación'" @click="navigationOpen = !navigationOpen; accountOpen = false"><SemIcon :name="navigationOpen && compact ? 'close' : 'menu'" :size="30" /></button>
      <strong>{{ title }}</strong>
      <button ref="accountButton" class="shell-icon-button" type="button" :aria-expanded="accountOpen" aria-controls="account-panel" aria-label="Abrir opciones de mi cuenta" @click="accountOpen = !accountOpen; if (compact) navigationOpen = false"><SemIcon name="user" :size="30" /></button>
    </header>
    <button v-if="navigationOpen && compact" type="button" class="navigation-scrim" aria-label="Cerrar menú de navegación" @click="closeOverlays" />
    <aside v-show="navigationOpen" id="main-navigation" class="sem-sidebar" :inert="!navigationOpen">
      <nav aria-label="Navegación principal"><RouterLink v-for="link in links" :key="link.label" :to="link.to" :class="{ active: link.active }" :aria-current="link.active ? 'page' : undefined"><SemIcon :name="link.icon" :size="33" /><span>{{ link.label }}</span></RouterLink></nav>
      <div class="sidebar-brand"><InstitutionalBrand /></div>
    </aside>
    <section v-if="accountOpen" id="account-panel" ref="accountPanel" class="sem-account-panel" aria-label="Opciones de mi cuenta">
      <p class="account-name">{{ currentProfile?.name || 'Mi cuenta' }}</p>
      <RouterLink :to="{ name: 'account' }"><SemIcon name="settings" :size="30" />Datos de la cuenta</RouterLink>
      <button type="button" @click="logout"><SemIcon name="logout" :size="30" />Cerrar sesión</button>
    </section>
    <div id="workspace" class="app-shell-content" tabindex="-1" :inert="compact && navigationOpen"><slot /></div>
    <SemWave v-if="route.name === 'dashboard'" class="home-wave" />
  </div>
</template>
