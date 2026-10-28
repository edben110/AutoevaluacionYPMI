<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { authorizedFetch, currentProfile, hasSession, loadProfile, responseError, signOut } from '@/auth/session'
import EvaluationPeriodNotice from '@/components/EvaluationPeriodNotice.vue'
import { type EvaluationPeriod } from '@/selfEvaluation/period'
import type { PmiOverview } from '@/pmi/types'
import PmiAreaCards from '@/components/PmiAreaCards.vue'
import menuIcon from '@/assets/icons/menu-burger.svg?raw'
import userIcon from '@/assets/icons/user.svg?raw'
import homeIcon from '@/assets/icons/home.svg?raw'
import documentIcon from '@/assets/icons/document.svg?raw'
import calendarIcon from '@/assets/icons/calendar.svg?raw'
import settingsIcon from '@/assets/icons/settings.svg?raw'
import exitIcon from '@/assets/icons/exit.svg?raw'

interface Area { id: string; name: string }
interface EvaluationSummary { year: number }

const router = useRouter()
const loading = ref(true)
const error = ref('')
const areas = ref<Area[]>([])
const areaError = ref('')
const year = ref<number | null>(null)
const periods = ref<EvaluationPeriod[]>([])
const period = computed(() => periods.value.find(item => item.year === year.value))
const available = computed(() => !periodLoading.value && !periodError.value && period.value?.writable === true)
const periodLoading = ref(false)
const periodError = ref('')
const pmiOverviews = ref<PmiOverview[]>([])
const pmiError = ref('')
const menuOpen = ref(false)
const profileOpen = ref(false)
const menuButtonRef = ref<HTMLButtonElement | null>(null)
const menuPanelRef = ref<HTMLElement | null>(null)
const profileButtonRef = ref<HTMLButtonElement | null>(null)
const profilePanelRef = ref<HTMLElement | null>(null)

async function loadPmiYears(): Promise<void> {
  pmiError.value = ''
  try {
    const response = await authorizedFetch('/api/self-evaluations')
    if (!response.ok) throw await responseError(response)
    const years = ((await response.json()) as EvaluationSummary[])
      .map(item => item.year).sort((first, second) => second - first)
    const overviews = await Promise.all(years.map(async year => {
      const overview = await authorizedFetch(`/api/pmi/${year}`)
      if (!overview.ok) throw await responseError(overview)
      return await overview.json() as PmiOverview
    }))
    pmiOverviews.value = overviews.filter(overview => overview.periodConfigured !== false)
  } catch (cause) {
    pmiError.value = cause instanceof Error ? cause.message : 'No se pudieron consultar las autoevaluaciones para el PMI.'
  }
}

async function loadPeriod(): Promise<void> {
  periodError.value = ''
  periodLoading.value = true
  try {
    const response = await authorizedFetch('/api/self-evaluation-periods')
    if (!response.ok) throw await responseError(response)
    periods.value = ((await response.json()) as EvaluationPeriod[])
      .filter(item => item.writable)
      .sort((first, second) => second.year - first.year)
    if (!periods.value.some(item => item.year === year.value)) year.value = periods.value[0]?.year ?? null
    if (periods.value.length === 0) areas.value = []
    else if (areas.value.length === 0) await loadAreas()
  } catch (cause) {
    periods.value = []
    year.value = null
    areas.value = []
    periodError.value = cause instanceof Error ? cause.message : 'No se pudo consultar el período.'
  } finally { periodLoading.value = false }
}

async function loadAreas(): Promise<void> {
  areaError.value = ''
  try {
    const response = await authorizedFetch('/api/areas?onlyActive=true')
    if (!response.ok) throw await responseError(response)
    areas.value = ((await response.json()) as Area[]).sort((first, second) => first.id.localeCompare(second.id))
  } catch (cause) {
    areaError.value = cause instanceof Error ? cause.message : 'No se pudieron cargar las áreas.'
  }
}

async function refreshProfile(): Promise<void> {
  loading.value = true
  error.value = ''
  areas.value = []
  try {
    const profile = await loadProfile()
    if (profile.role === 'ESTABLISHMENT') await Promise.all([loadPeriod(), loadPmiYears()])
    if (!hasSession.value) await router.replace({ name: 'login' })
  } catch (cause) {
    if (!hasSession.value) {
      await router.replace({ name: 'login' })
      return
    }
    error.value = cause instanceof Error ? cause.message : 'No se pudo cargar tu perfil.'
  } finally {
    loading.value = false
  }
}

function closeDropdowns(): void {
  menuOpen.value = false
  profileOpen.value = false
}

function toggleMenu(): void {
  menuOpen.value = !menuOpen.value
  if (menuOpen.value) profileOpen.value = false
}

function toggleProfileMenu(): void {
  profileOpen.value = !profileOpen.value
  if (profileOpen.value) menuOpen.value = false
}

function logout(): void {
  closeDropdowns()
  signOut()
  void router.replace({ name: 'login' })
}

function refreshAvailability(): void {
  if (currentProfile.value?.role === 'ESTABLISHMENT' && !loading.value && !periodLoading.value) {
    void loadPeriod()
    void loadPmiYears()
  }
}

function handleWindowClick(event: MouseEvent): void {
  const target = event.target as Node | null
  if (!target) return

  const clickedMenuButton = menuButtonRef.value?.contains(target)
  const clickedMenuPanel = menuPanelRef.value?.contains(target)
  const clickedProfileButton = profileButtonRef.value?.contains(target)
  const clickedProfilePanel = profilePanelRef.value?.contains(target)

  if (!clickedMenuButton && !clickedMenuPanel && !clickedProfileButton && !clickedProfilePanel) {
    closeDropdowns()
  }
}

onMounted(() => {
  void refreshProfile()
  window.addEventListener('focus', refreshAvailability)
  window.addEventListener('click', handleWindowClick)
})
onUnmounted(() => {
  window.removeEventListener('focus', refreshAvailability)
  window.removeEventListener('click', handleWindowClick)
})
</script>

<template>
  <main class="dashboard-page">
    <header class="dashboard-topbar">
      <div class="topbar-left">
        <button
          ref="menuButtonRef"
          class="topbar-button"
          type="button"
          aria-label="Abrir menú principal"
          :aria-expanded="menuOpen ? 'true' : 'false'"
          @click.stop="toggleMenu"
        >
          <span class="icon" aria-hidden="true" v-html="menuIcon"></span>
        </button>
        <div class="brand">
          <span class="brand-mark" aria-hidden="true">✦</span>
          <span>Ruta de Mejoramiento</span>
        </div>
      </div>

      <div class="topbar-right">
        <button
          ref="profileButtonRef"
          class="topbar-button profile-button"
          type="button"
          aria-label="Abrir menú de perfil"
          :aria-expanded="profileOpen ? 'true' : 'false'"
          @click.stop="toggleProfileMenu"
        >
          <span class="icon" aria-hidden="true" v-html="userIcon"></span>
        </button>
      </div>
    </header>

    <div class="dashboard-content">
      <aside v-if="menuOpen" ref="menuPanelRef" class="floating-panel left-panel" role="menu" aria-label="Menú principal">
        <div class="panel-header">
          <span class="panel-title">Navegación</span>
        </div>
        <nav class="panel-menu" aria-label="Menu de navegación">
          <button type="button" class="menu-item">
            <span class="menu-item-icon">
              <span class="icon" aria-hidden="true" v-html="homeIcon"></span>
            </span>
            <span>Inicio</span>
          </button>
          <button type="button" class="menu-item">
            <span class="menu-item-icon">
              <span class="icon" aria-hidden="true" v-html="documentIcon"></span>
            </span>
            <span>Evaluaciones</span>
          </button>
          <button type="button" class="menu-item">
            <span class="menu-item-icon">
              <span class="icon" aria-hidden="true" v-html="calendarIcon"></span>
            </span>
            <span>Periodos</span>
          </button>
        </nav>
      </aside>

      <aside v-if="profileOpen" ref="profilePanelRef" class="floating-panel right-panel" role="menu" aria-label="Menú de perfil">
        <div class="panel-header">
          <span class="panel-title">Mi cuenta</span>
        </div>
        <button type="button" class="profile-option">
          <span class="menu-item-icon">
            <span class="icon" aria-hidden="true" v-html="settingsIcon"></span>
          </span>
          <span>Configuración</span>
        </button>
        <button type="button" class="profile-option danger-option" @click="logout">
          <span class="menu-item-icon">
            <span class="icon" aria-hidden="true" v-html="exitIcon"></span>
          </span>
          <span>Cerrar sesión</span>
        </button>
      </aside>

      <div v-if="loading" class="status-card" role="status">Cargando tu espacio…</div>
      <div v-else-if="error" class="status-card" role="alert">
        <p>{{ error }}</p>
        <button class="secondary-button" type="button" @click="refreshProfile">Reintentar</button>
      </div>
      <template v-else-if="currentProfile">
        <div class="welcome-banner">
          <span class="eyebrow">TU ESPACIO DE TRABAJO</span>
          <h1>Hola, {{ currentProfile.name }}</h1>
          <p>
            {{
              currentProfile.role === 'SECRETARY'
                ? 'Accediste como Secretaría de Educación Municipal.'
                : 'Accediste como institución educativa.'
            }}
          </p>
        </div>

        <section v-if="currentProfile.role === 'SECRETARY'" class="status-card">
          <h2>Autoevaluación institucional</h2>
          <p>Configura las fechas para habilitar el diligenciamiento de las instituciones.</p>
          <RouterLink class="secondary-button dashboard-action" :to="{ name: 'secretary-periods' }">Administrar períodos</RouterLink>
        </section>
        <section v-if="currentProfile.role === 'ESTABLISHMENT'" class="area-evaluations" aria-labelledby="areas-title">
          <p v-if="periodError" class="form-error" role="alert">{{ periodError }}</p>
          <p v-else-if="periodLoading" class="status-card" role="status">Consultando disponibilidad de autoevaluación…</p>
          <section v-else-if="!available" class="status-card" role="status">
            <h2 id="areas-title">Autoevaluación pendiente de habilitación</h2>
            <p>No hay un período de autoevaluación abierto. Las cuatro áreas aparecerán cuando Secretaría habilite el diligenciamiento.</p>
          </section>
          <template v-if="available && period">
          <h2 id="areas-title">Autoevaluaciones por área</h2>
          <p>Elige el área que vas a diligenciar. En cada formulario encontrarás sus procesos y componentes.</p>
          <label v-if="periods.length > 1" class="year-form" for="dashboard-year">
            Período habilitado
            <select id="dashboard-year" v-model.number="year">
              <option v-for="item in periods" :key="item.year" :value="item.year">{{ item.year }}</option>
            </select>
          </label>
          <EvaluationPeriodNotice :period="period" :year="period.year" />
          <div v-if="areaError" class="form-error" role="alert">
            <p>{{ areaError }}</p>
            <button class="secondary-button" type="button" @click="loadAreas">Reintentar</button>
          </div>
          <p v-else-if="areas.length === 0" class="status-card">Todavía no hay áreas activas disponibles.</p>
          <div v-else class="area-evaluation-grid">
            <RouterLink
              v-for="(area, index) in areas"
              :key="area.id"
              class="area-evaluation-card"
              :to="{ name: 'self-evaluation', params: { areaId: area.id }, query: { year } }"
            >
              <span class="section-number">ÁREA {{ index + 1 }}</span>
              <h3>Autoevaluación · {{ area.name }}</h3>
              <span class="area-card-action">Abrir autoevaluación →</span>
            </RouterLink>
          </div>
          </template>
          <button class="text-button" type="button" :disabled="periodLoading" @click="refreshAvailability">Actualizar disponibilidad</button>
        </section>
        <section v-if="currentProfile.role === 'ESTABLISHMENT'" class="status-card pmi-entry" aria-labelledby="pmi-title">
          <span class="pmi-eyebrow">PLAN DE MEJORAMIENTO INSTITUCIONAL</span>
          <h2 id="pmi-title">Planes de mejoramiento por área</h2>
          <p>Cada área tiene su propio PMI. Se habilita al completar y enviar la autoevaluación de esa área.</p>
          <div v-if="pmiError" role="alert">
            <p class="form-error">{{ pmiError }}</p>
            <button class="pmi-link" type="button" @click="loadPmiYears">Reintentar</button>
          </div>
          <p v-else-if="pmiOverviews.length === 0">No hay años habilitados para iniciar un PMI. Consulta la publicación del período con Secretaría.</p>
          <section v-for="overview in pmiOverviews" v-else :key="overview.year" class="pmi-year-group">
            <h3>PMI · Autoevaluación {{ overview.year }}</h3>
            <PmiAreaCards :overview="overview" />
          </section>
        </section>
        <section class="profile-card" aria-labelledby="profile-title">
          <div class="section-title">
            <span class="section-number">01</span>
            <h2 id="profile-title">Datos de tu sesión</h2>
          </div>
          <dl class="profile-grid">
            <div>
              <dt>Nombre</dt>
              <dd>{{ currentProfile.name }}</dd>
            </div>
            <div>
              <dt>Correo</dt>
              <dd>{{ currentProfile.email }}</dd>
            </div>
            <div v-if="currentProfile.daneCode">
              <dt>Código DANE</dt>
              <dd>{{ currentProfile.daneCode }}</dd>
            </div>
            <div v-if="currentProfile.rector">
              <dt>Rectoría</dt>
              <dd>{{ currentProfile.rector }}</dd>
            </div>
          </dl>
        </section>
      </template>
    </div>
  </main>
</template>

<style scoped>
:global(html) {
  background: var(--sem-white);
}

.dashboard-page {
  position: relative;
  min-height: 100vh;
  background: var(--sem-white);
}

.dashboard-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  min-height: 4.5rem;
  padding: 0.9rem 1.5rem;
  background: var(--sem-blue);
  box-shadow: 0 8px 22px rgba(43, 44, 107, 0.12);
}

.topbar-left,
.topbar-right {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 0.7rem;
  color: var(--color-text-white);
  font-size: .96rem;
  font-weight: 700;
  letter-spacing: -.02em;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 2rem;
  height: 2rem;
  border-radius: .65rem;
  background: rgba(255, 255, 255, .16);
  color: var(--color-text-white);
  font-size: 1.1rem;
}

.topbar-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.7rem;
  height: 2.7rem;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: .75rem;
  background: rgba(255, 255, 255, 0.1);
  color: var(--color-text-white);
  transition: background-color 160ms ease, transform 160ms ease;
}

.topbar-button .icon {
  color: var(--color-text-white);
}

.topbar-button:hover {
  background: rgba(255, 255, 255, 0.18);
}

.topbar-button:active {
  transform: translateY(1px);
}

.icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.15rem;
  height: 1.15rem;
  line-height: 0;
  color: currentColor;
}

.icon svg {
  display: block;
  width: 100%;
  height: 100%;
  fill: currentColor !important;
}

.icon svg path,
.icon svg circle,
.icon svg g {
  fill: currentColor !important;
}

.profile-button .icon {
  width: 1.2rem;
  height: 1.2rem;
}

.dashboard-content {
  position: static;
  display: grid;
  gap: 1.5rem;
  width: min(1200px, calc(100% - 2rem));
  margin: 1.5rem auto 2.5rem;
}

.floating-panel {
  position: absolute;
  top: 4.8rem;
  z-index: 20;
  display: grid;
  gap: 0.75rem;
  min-width: min(18rem, 78vw);
  padding: 0.9rem;
  border: 1px solid rgba(32, 33, 36, 0.08);
  border-radius: 1rem;
  background: var(--color-background-top);
  box-shadow: 0 18px 42px rgba(32, 33, 36, 0.14);
}

.left-panel {
  left: 1rem;
}

.right-panel {
  right: 1rem;
}

.panel-header {
  padding: 0.3rem 0.2rem 0.2rem;
}

.panel-title {
  color: var(--sem-blue);
  font-size: .72rem;
  font-weight: 800;
  letter-spacing: .12em;
  text-transform: uppercase;
}

.panel-menu,
.profile-option {
  display: grid;
  gap: 0.4rem;
}

.menu-item,
.profile-option {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  width: 100%;
  min-height: 2.9rem;
  padding: 0.7rem 0.8rem;
  border: 0;
  border-radius: .7rem;
  background: transparent;
  color: var(--color-text);
  font: inherit;
  font-weight: 700;
  text-align: left;
}

.menu-item:hover,
.profile-option:hover {
  background: var(--sem-white-mute);
}

.menu-item-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.6rem;
  height: 1.6rem;
}

.profile-option {
  border: 1px solid transparent;
}

.danger-option {
  color: var(--sem-red);
}

.danger-option img {
  filter: brightness(0) saturate(100%) invert(17%) sepia(87%) saturate(1807%) hue-rotate(345deg) brightness(90%) contrast(100%);
}

.status-card,
.welcome-banner,
.area-evaluations,
.profile-card,
.pmi-entry {
  background: var(--color-background-top);
  border: 1px solid rgba(32, 33, 36, 0.07);
  border-radius: 1.15rem;
  padding: 1.25rem 1.3rem;
  box-shadow: 0 10px 30px rgba(43, 44, 107, 0.03);
}

.welcome-banner {
  padding-top: 1.5rem;
  padding-bottom: 1.5rem;
  background: linear-gradient(135deg, rgba(43, 44, 107, 0.04), rgba(43, 44, 107, 0.01));
}

.eyebrow {
  color: var(--sem-blue);
  font-size: .72rem;
  font-weight: 800;
  letter-spacing: .16em;
}

.welcome-banner h1 {
  margin-top: 0.8rem;
  color: var(--sem-blue);
  font-size: clamp(2rem, 3vw, 3rem);
  line-height: 1.1;
  letter-spacing: -.06em;
}

.welcome-banner p {
  margin-top: 0.5rem;
  color: var(--color-text-disabled);
}

.pmi-entry {
  border-top: 4px solid var(--sem-blue);
}

.pmi-entry h2 {
  margin: .5rem 0;
  color: var(--color-text-highlight);
}

.pmi-entry p {
  margin: .65rem 0;
}

.pmi-eyebrow {
  color: var(--color-text-highlight);
  font-size: .75rem;
  font-weight: 700;
  letter-spacing: .08em;
}

.pmi-year-links {
  display: flex;
  flex-wrap: wrap;
  gap: .8rem;
  margin-top: 1rem;
}

.pmi-link {
  display: inline-block;
  border: 1px solid var(--sem-blue);
  border-radius: .6rem;
  background: var(--color-background-top);
  padding: .7rem 1rem;
  color: var(--color-text-highlight);
  font: inherit;
  font-weight: 700;
  text-decoration: none;
}

.pmi-link:hover {
  background: var(--color-background-mute);
}

.pmi-link:focus-visible {
  outline: 3px solid var(--sem-yellow);
  outline-offset: 3px;
}

@media (max-width: 640px) {
  .dashboard-topbar {
    padding-left: 0.9rem;
    padding-right: 0.9rem;
  }

  .brand span:last-child {
    max-width: 10rem;
    line-height: 1.2;
  }

  .dashboard-content {
    width: min(100% - 1rem, 1200px);
    margin-top: 1rem;
  }

  .floating-panel {
    top: 0.6rem;
  }
}
</style>

