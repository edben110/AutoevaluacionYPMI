<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { authorizedFetch, currentProfile, hasSession, loadProfile, responseError } from '@/auth/session'
import SemIcon from '@/components/SemIcon.vue'
import EvaluationPeriodNotice from '@/components/EvaluationPeriodNotice.vue'
import { type EvaluationPeriod } from '@/selfEvaluation/period'
import type { PmiOverview } from '@/pmi/types'
import PmiAreaCards from '@/components/PmiAreaCards.vue'
import FollowUpRequirement from '@/components/FollowUpRequirement.vue'
import type { FollowUpPrerequisite } from '@/selfEvaluation/prerequisites'

interface Area { id: string; name: string }
interface EvaluationSummary { year: number }

const router = useRouter()
const route = useRoute()
const section = computed(() => currentProfile.value?.role === 'SECRETARY' ? '' : ['autoevaluacion', 'pmi'].includes(String(route.query.section)) ? String(route.query.section) : '')
const loading = ref(true)
const error = ref('')
const areas = ref<Area[]>([])
const areaError = ref('')
const year = ref<number | null>(null)
const periods = ref<EvaluationPeriod[]>([])
const period = computed(() => periods.value.find(item => item.year === year.value))
const periodOpen = computed(() => !periodLoading.value && !periodError.value && period.value?.writable === true)
const available = computed(() => periodOpen.value && !prerequisiteLoading.value && prerequisite.value?.ready === true)
const prerequisite = ref<FollowUpPrerequisite | null>(null)
const prerequisiteLoading = ref(false)
const prerequisiteError = ref('')
let prerequisiteSequence = 0
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
    if (periods.value.length === 0) { areas.value = []; prerequisite.value = null }
    else await loadPrerequisites()
  } catch (cause) {
    periods.value = []
    year.value = null
    areas.value = []
    periodError.value = cause instanceof Error ? cause.message : 'No se pudo consultar el período.'
  } finally { periodLoading.value = false }
}

async function loadPrerequisites(): Promise<void> {
  const current = ++prerequisiteSequence
  const selectedYear = year.value
  prerequisite.value = null; prerequisiteError.value = ''; prerequisiteLoading.value = true
  try {
    if (selectedYear === null) return
    const response = await authorizedFetch(`/api/self-evaluations/${selectedYear}/prerequisites`)
    if (!response.ok) throw await responseError(response)
    const data = await response.json() as FollowUpPrerequisite
    if (current !== prerequisiteSequence || selectedYear !== year.value) return
    prerequisite.value = data
    if (data.ready && areas.value.length === 0) await loadAreas()
  } catch (cause) {
    if (current === prerequisiteSequence) prerequisiteError.value = cause instanceof Error ? cause.message : 'No se pudo verificar el seguimiento anterior.'
  } finally { if (current === prerequisiteSequence) prerequisiteLoading.value = false }
}

watch(year, () => { if (!periodLoading.value && currentProfile.value?.role === 'ESTABLISHMENT') void loadPrerequisites() })

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
onUnmounted(() => window.removeEventListener('focus', refreshAvailability))
</script>

<template>
  <main class="dashboard-page">
    <div class="dashboard-content" :class="{ 'home-content': !section }">
      <div v-if="loading" class="status-card" role="status">Cargando tu espacio…</div>
      <div v-else-if="error" class="status-card" role="alert"><p>{{ error }}</p><button class="secondary-button" type="button" @click="refreshProfile">Reintentar</button></div>
      <template v-else-if="currentProfile">
        <template v-if="!section">
          <div class="home-heading"><p>{{ currentProfile.role === 'SECRETARY' ? 'Secretaría de Educación Municipal' : currentProfile.name }}</p><h1>Bienvenido a tu espacio</h1></div>
          <nav class="home-actions" aria-label="Opciones principales">
            <template v-if="currentProfile.role === 'SECRETARY'">
              <RouterLink class="home-action home-action-wide" :to="{ name: 'secretary-periods' }"><SemIcon name="document-add" :size="90" /><span>Definir fechas de<br />autoevaluación</span></RouterLink>
              <RouterLink class="home-action" :to="{ name: 'deliveries' }"><SemIcon name="calendar" :size="104" /><span>Autoevaluación</span><small>Revisar entregas de las instituciones</small></RouterLink>
              <RouterLink class="home-action" :to="{ name: 'deliveries', query: { section: 'pmi' } }"><SemIcon name="document" :size="104" /><span>PMI</span><small>Consultar planes y seguimiento</small></RouterLink>
            </template>
            <template v-else>
              <RouterLink class="home-action" :to="{ name: 'dashboard', query: { section: 'autoevaluacion' } }"><SemIcon name="calendar" :size="104" /><span>Autoevaluación</span></RouterLink>
              <RouterLink class="home-action" :to="{ name: 'dashboard', query: { section: 'pmi' } }"><SemIcon name="document" :size="104" /><span>PMI</span></RouterLink>
            </template>
          </nav>
          <div class="home-intro"><p>Bienvenido al sistema de gestión para Autoevaluación y Plan de Mejoramiento Institucional.</p><p>Selecciona una de las opciones para continuar.</p>
            <RouterLink v-if="currentProfile.role === 'ESTABLISHMENT'" class="history-link" :to="{ name: 'deliveries' }">Ver autoevaluaciones, comentarios y PMI anteriores →</RouterLink>
          </div>
        </template>
        <section v-if="currentProfile.role === 'ESTABLISHMENT' && section === 'autoevaluacion'" class="area-evaluations" aria-labelledby="areas-title">
          <h1 id="areas-title">Autoevaluación institucional</h1>
          <p>Elige el área de gestión que vas a diligenciar o continuar.</p>
          <p v-if="periodError" class="form-error" role="alert">{{ periodError }}</p>
          <p v-else-if="periodLoading || prerequisiteLoading" class="status-card" role="status">Consultando disponibilidad de autoevaluación…</p>
          <div v-else-if="prerequisiteError" class="status-card" role="alert"><p>{{ prerequisiteError }}</p><button type="button" class="secondary-button" @click="loadPrerequisites">Reintentar</button></div>
          <FollowUpRequirement v-else-if="periodOpen && prerequisite && !prerequisite.ready" :requirement="prerequisite" />
          <section v-else-if="!periodOpen" class="status-card unavailable-state" role="status"><SemIcon name="document-add" :size="110" /><h2>Autoevaluación pendiente de habilitación</h2><p>Ahora mismo no se encuentra activo el formulario de autoevaluación. Las cuatro áreas aparecerán cuando Secretaría habilite el diligenciamiento.</p></section>
          <label v-if="periods.length > 1" class="year-form" for="dashboard-year">Período habilitado<select id="dashboard-year" v-model.number="year"><option v-for="item in periods" :key="item.year" :value="item.year">{{ item.year }}</option></select></label>
          <template v-if="available && period">
            <EvaluationPeriodNotice :period="period" :year="period.year" />
            <div v-if="areaError" class="form-error" role="alert"><p>{{ areaError }}</p><button class="secondary-button" type="button" @click="loadAreas">Reintentar</button></div>
            <p v-else-if="areas.length === 0" class="status-card">Todavía no hay áreas activas disponibles.</p>
            <div v-else class="area-evaluation-grid">
              <RouterLink v-for="(area, index) in areas" :key="area.id" class="area-evaluation-card" :to="{ name: 'self-evaluation', params: { areaId: area.id }, query: { year } }">
                <SemIcon name="document-add" :size="52" /><span class="section-number">ÁREA {{ index + 1 }} · {{ year }}</span><h3>{{ area.name }}</h3><span class="area-card-action">Abrir autoevaluación →</span>
              </RouterLink>
            </div>
          </template>
          <div class="dashboard-footer-actions"><button class="text-button" type="button" :disabled="periodLoading" @click="refreshAvailability">Actualizar disponibilidad</button><RouterLink :to="{ name: 'deliveries' }">Consultar autoevaluaciones anteriores y comentarios</RouterLink></div>
        </section>
        <section v-if="currentProfile.role === 'ESTABLISHMENT' && section === 'pmi'" class="pmi-entry" aria-labelledby="pmi-title">
          <h1 id="pmi-title">Planes de mejoramiento por área</h1><p>Cada área tiene su propio PMI. Se habilita al completar y enviar la autoevaluación de esa área.</p>
          <div v-if="pmiError" class="status-card" role="alert"><p class="form-error">{{ pmiError }}</p><button class="secondary-button" type="button" @click="loadPmiYears">Reintentar</button></div>
          <p v-else-if="pmiOverviews.length === 0" class="status-card">No hay años habilitados para iniciar un PMI. Consulta la publicación del período con Secretaría.</p>
          <section v-for="overview in pmiOverviews" v-else :key="overview.year" class="pmi-year-group status-card"><h2>PMI · Autoevaluación {{ overview.year }}</h2><PmiAreaCards :overview="overview" /></section>
        </section>
      </template>
    </div>
  </main>
</template>

<style scoped>
.home-content { padding-bottom: 150px; }
.home-heading { text-align: center; margin: 2rem 0 2.5rem; }
.home-heading p { color: var(--sem-black-soft); }
.home-heading h1 { margin-top: .4rem; }
.home-actions { display: grid; grid-template-columns: repeat(2, minmax(0, 240px)); justify-content: center; gap: 1.8rem; max-width: 700px; margin: 2rem auto; }
.home-action { display: flex; flex-direction: column; gap: 1rem; align-items: center; justify-content: center; min-height: 210px; padding: 1.3rem; border: 2px solid var(--sem-black); border-radius: 18px; background: var(--sem-white-top); color: var(--sem-black); text-align: center; text-decoration: none; }
.home-action:hover { border-color: var(--sem-blue); color: var(--sem-blue); background: var(--sem-white); }
.home-action:focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 4px; }
.home-action span { font-size: 1.35rem; font-weight: 700; line-height: 1.4; }
.home-action small { color: var(--sem-black-soft); font-size: .85rem; }
.home-action-wide { grid-column: 1 / -1; flex-direction: row; gap: 1.8rem; min-height: 165px; }
.home-intro { max-width: 790px; margin: 3.5rem auto 0; color: var(--sem-black-soft); font-weight: 700; text-align: center; }
.history-link { display: inline-block; margin-top: 1rem; color: var(--sem-blue); font-weight: 700; }
.area-evaluations { margin-top: 0; }
.area-evaluation-card { align-items: center; text-align: center; border: 1.5px solid var(--sem-black-mute); border-radius: 12px; padding: 1.8rem; }
.area-evaluation-card h3 { color: var(--sem-blue); }
.unavailable-state { min-height: 330px; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; color: var(--sem-black-soft); gap: 1rem; }
.unavailable-state p { max-width: 600px; }
.dashboard-footer-actions { display: flex; align-items: center; flex-wrap: wrap; gap: 1rem; margin-top: 1rem; }
.dashboard-footer-actions a { color: var(--sem-blue); }
.pmi-entry > p { margin: .8rem 0; }
.pmi-year-group h2 { color: var(--sem-blue); font-size: 1.3rem; }
@media (max-width: 600px) { .home-actions { gap: 1rem; grid-template-columns: repeat(2, minmax(0, 1fr)); } .home-action { min-height: 175px; padding: 1rem .7rem; } .home-action svg { width: 72px; height: 72px; } .home-action span { font-size: 1.05rem; } .home-action-wide { flex-direction: column; gap: .8rem; } }
</style>
