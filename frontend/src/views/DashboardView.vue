<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { authorizedFetch, currentProfile, hasSession, loadProfile, responseError, signOut } from '@/auth/session'
import EvaluationPeriodNotice from '@/components/EvaluationPeriodNotice.vue'
import { type EvaluationPeriod } from '@/selfEvaluation/period'

interface Area { id: string; name: string }

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
    if (profile.role === 'ESTABLISHMENT') await loadPeriod()
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
  if (currentProfile.value?.role === 'ESTABLISHMENT' && !loading.value && !periodLoading.value) void loadPeriod()
}

onMounted(() => {
  void refreshProfile()
  window.addEventListener('focus', refreshAvailability)
})
onUnmounted(() => window.removeEventListener('focus', refreshAvailability))
</script>

<template>
  <main class="dashboard-page">
    <header class="dashboard-header">
      <div class="brand">
        <span class="brand-mark" aria-hidden="true">✦</span>
        <span>Ruta de Mejoramiento</span>
      </div>
      <button class="text-button" type="button" @click="logout">Cerrar sesión</button>
    </header>

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
          <button class="text-button" type="button" :disabled="periodLoading" @click="loadPeriod">Actualizar disponibilidad</button>
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
