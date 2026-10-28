<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { authorizedFetch, currentProfile, hasSession, loadProfile, responseError, signOut } from '@/auth/session'
import EvaluationPeriodNotice from '@/components/EvaluationPeriodNotice.vue'
import { type EvaluationPeriod } from '@/selfEvaluation/period'
import type { PmiOverview } from '@/pmi/types'
import AppTopBar from '@/components/AppTopBar.vue'
import PmiAreaCards from '@/components/PmiAreaCards.vue'
import SvgIcon from '@/components/SvgIcon.vue'
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

function logout(): void {
  signOut()
  void router.replace({ name: 'login' })
}

function refreshAvailability(): void {
  if (currentProfile.value?.role === 'ESTABLISHMENT' && !loading.value && !periodLoading.value) {
    void loadPeriod()
    void loadPmiYears()
  }
}

onMounted(() => {
  void refreshProfile()
  window.addEventListener('focus', refreshAvailability)
})
onUnmounted(() => {
  window.removeEventListener('focus', refreshAvailability)
})
</script>

<template>
  <main class="dashboard-page">
    <AppTopBar title="Ruta de Mejoramiento" @logout="logout">
      <template #menu-items>
        <nav class="u-menu-list" aria-label="Menu de navegación">
          <button type="button" class="u-menu-item">
            <span class="u-menu-item__icon">
              <SvgIcon :src="homeIcon" color="currentColor" />
            </span>
            <span>Inicio</span>
          </button>
          <button type="button" class="u-menu-item">
            <span class="u-menu-item__icon">
              <SvgIcon :src="documentIcon" color="currentColor" />
            </span>
            <span>Evaluaciones</span>
          </button>
          <button type="button" class="u-menu-item">
            <span class="u-menu-item__icon">
              <SvgIcon :src="calendarIcon" color="currentColor" />
            </span>
            <span>Periodos</span>
          </button>
        </nav>
      </template>

      <template #profile-items>
        <button type="button" class="u-menu-item">
          <span class="u-menu-item__icon">
            <SvgIcon :src="settingsIcon" color="currentColor" />
          </span>
          <span>Configuración</span>
        </button>
        <button type="button" class="u-menu-item--danger" @click="logout">
          <span class="u-menu-item__icon">
            <SvgIcon :src="exitIcon" color="currentColor" />
          </span>
          <span>Cerrar sesión</span>
        </button>
      </template>
    </AppTopBar>

    <div class="dashboard-content">
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

.dashboard-content {
  position: static;
  display: grid;
  gap: 1.5rem;
  width: min(1200px, calc(100% - 2rem));
  margin: 1.5rem auto 2.5rem;
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

