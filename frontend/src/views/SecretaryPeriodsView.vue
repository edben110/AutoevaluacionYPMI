<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { authorizedFetch, hasSession, loadProfile, responseError } from '@/auth/session'
import { formatPeriodDate, periodLabels, type EvaluationPeriod } from '@/selfEvaluation/period'

const router = useRouter()
const loading = ref(true)
const saving = ref(false)
const deleting = ref(false)
const pendingDeletion = ref<EvaluationPeriod | null>(null)
const error = ref('')
const success = ref('')
const periods = ref<EvaluationPeriod[]>([])
const year = ref(new Date().getFullYear())
const startDate = ref('')
const finishDate = ref('')
const editing = ref(false)
const authorized = ref(false)
const configuredYear = computed(() => periods.value.some(period => period.year === year.value))

async function loadPeriods(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    const response = await authorizedFetch('/api/self-evaluation-periods')
    if (!response.ok) throw await responseError(response)
    periods.value = await response.json() as EvaluationPeriod[]
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'No se pudieron cargar los períodos.'
    if (!hasSession.value) await router.replace({ name: 'login' })
  } finally { loading.value = false }
}

function edit(period: EvaluationPeriod): void {
  if (saving.value || deleting.value) return
  year.value = period.year
  startDate.value = period.startDate
  finishDate.value = period.finishDate
  editing.value = true
  error.value = ''
  success.value = ''
  document.getElementById('period-start')?.focus()
}

function newPeriod(): void {
  editing.value = false
  year.value = new Date().getFullYear()
  startDate.value = ''
  finishDate.value = ''
  success.value = ''
  error.value = ''
}

function confirmDeletion(period: EvaluationPeriod): void {
  if (saving.value || deleting.value) return
  pendingDeletion.value = period
  error.value = ''
  success.value = ''
}

async function deletePeriod(): Promise<void> {
  const target = pendingDeletion.value
  if (!target || saving.value || deleting.value) return
  deleting.value = true
  error.value = ''
  success.value = ''
  try {
    const response = await authorizedFetch(`/api/self-evaluation-periods/${target.year}`, { method: 'DELETE' })
    if (!response.ok) throw await responseError(response)
    periods.value = periods.value.filter(period => period.year !== target.year)
    if (editing.value && year.value === target.year) newPeriod()
    pendingDeletion.value = null
    success.value = `Período ${target.year} eliminado junto con los PMI de todas las instituciones de ese año y sus anexos. Las autoevaluaciones y sus respuestas se conservan para consulta.`
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'No se pudo eliminar el período.'
    if (!hasSession.value) await router.replace({ name: 'login' })
  } finally { deleting.value = false }
}

async function save(): Promise<void> {
  if (saving.value || deleting.value || pendingDeletion.value) return
  error.value = ''
  success.value = ''
  if (!Number.isInteger(year.value) || year.value < 1 || year.value > 9999) {
    error.value = 'Escribe un año entre 1 y 9999.'
    return
  }
  if (!startDate.value || !finishDate.value || finishDate.value < startDate.value) {
    error.value = 'La fecha de fin debe ser igual o posterior a la fecha de inicio.'
    return
  }
  saving.value = true
  try {
    const response = await authorizedFetch(`/api/self-evaluation-periods/${year.value}`, {
      method: 'PUT', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ startDate: startDate.value, finishDate: finishDate.value }),
    })
    if (!response.ok) throw await responseError(response)
    const saved = await response.json() as EvaluationPeriod
    periods.value = [...periods.value.filter(period => period.year !== saved.year), saved]
      .sort((first, second) => second.year - first.year)
    editing.value = true
    success.value = `Período ${saved.year} guardado. Las instituciones registradas tienen su borrador disponible. Estado: ${periodLabels[saved.status].toLowerCase()}.`
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'No se pudo guardar el período.'
    if (!hasSession.value) await router.replace({ name: 'login' })
  } finally { saving.value = false }
}

onMounted(async () => {
  try {
    const profile = await loadProfile()
    if (profile.role !== 'SECRETARY') {
      await router.replace({ name: 'dashboard' })
      return
    }
    authorized.value = true
    await loadPeriods()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'No se pudo abrir esta pantalla.'
    loading.value = false
    if (!hasSession.value) await router.replace({ name: 'login' })
  }
})
</script>

<template>
  <main class="evaluation-page">
    <header class="evaluation-header"><RouterLink class="u-return-button" to="/dashboard">Volver a mi espacio</RouterLink></header>
    <div class="evaluation-content">
      <span class="eyebrow eyebrow-dark">SECRETARÍA DE EDUCACIÓN</span>
      <h1>Períodos de autoevaluación</h1>
      <p>Define cuándo las instituciones pueden diligenciar la autoevaluación de las cuatro áreas.</p>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <p v-if="success" class="status-card" role="status">{{ success }}</p>
      <template v-if="authorized">
        <section class="evaluation-empty">
          <h2>{{ configuredYear ? 'Editar fechas del período' : 'Habilitar período anual' }}</h2>
          <p v-if="configuredYear">Este período ya está publicado. Ajustar sus fechas conserva las respuestas y los PMI de las instituciones.</p>
          <p v-else>Al habilitar un período nuevo, las cuatro áreas de cada institución empiezan vacías y sin envíos. Si vuelves a publicar un año eliminado, se reinician sus respuestas anteriores y sus PMI.</p>
          <form @submit.prevent="save">
            <fieldset class="period-form" :disabled="saving || loading || deleting || !!pendingDeletion">
              <label>Año de la autoevaluación
                <input v-model.number="year" type="number" min="1" max="9999" required :disabled="editing" />
              </label>
              <label>Fecha de inicio<input id="period-start" v-model="startDate" type="date" required /></label>
              <label>Fecha de fin<input v-model="finishDate" type="date" :min="startDate || undefined" required /></label>
              <p>Se incluyen completos el día de inicio y el de fin, con la hora de Bogotá. Fuera de estas fechas, las respuestas quedan disponibles para consulta.</p>
              <div class="period-actions">
                <button class="primary-button" type="submit">{{ saving ? 'Guardando…' : configuredYear ? 'Guardar fechas' : 'Habilitar período' }}</button>
                <button v-if="editing" class="secondary-button" type="button" @click="newPeriod">Configurar otro año</button>
              </div>
            </fieldset>
          </form>
        </section>
        <section class="status-card">
          <h2>Períodos configurados</h2>
          <section v-if="pendingDeletion" class="deletion-confirmation" aria-labelledby="delete-period-title">
            <h3 id="delete-period-title">¿Eliminar el período {{ pendingDeletion.year }}?</h3>
            <p>Se retirarán las fechas de habilitación y se eliminarán los PMI de las cuatro áreas de todas las instituciones de ese año, incluidos la priorización, los planes, las fichas de indicadores y los seguimientos de los anexos 2 a 5. Esta eliminación no se puede deshacer.</p>
            <p>Las autoevaluaciones y sus respuestas se conservarán para consulta hasta volver a publicar ese año. Al publicarlo nuevamente, las instituciones empezarán con autoevaluaciones vacías y deberán completarlas y enviarlas para iniciar sus PMI.</p>
            <div class="period-actions">
              <button class="secondary-button" type="button" :disabled="deleting" @click="pendingDeletion = null">Cancelar</button>
              <button class="secondary-button delete-button" type="button" :disabled="deleting" @click="deletePeriod">{{ deleting ? 'Eliminando…' : 'Confirmar eliminación' }}</button>
            </div>
          </section>
          <p v-if="loading" role="status">Cargando períodos…</p>
          <p v-else-if="periods.length === 0">Aún no se ha configurado ningún período.</p>
          <div v-else class="period-table-wrap">
            <table class="period-table">
              <caption>Fechas y estado del diligenciamiento anual</caption>
              <thead><tr><th scope="col">Año</th><th scope="col">Inicio</th><th scope="col">Fin</th><th scope="col">Estado</th><th scope="col">Acción</th></tr></thead>
              <tbody><tr v-for="period in periods" :key="period.year">
                <th scope="row">{{ period.year }}</th>
                <td>{{ formatPeriodDate(period.startDate) }}</td><td>{{ formatPeriodDate(period.finishDate) }}</td>
                <td>{{ periodLabels[period.status] }}</td>
                <td>
                  <button class="text-button" type="button" :disabled="saving || deleting || !!pendingDeletion" @click="edit(period)">Editar fechas</button>
                  <button class="text-button delete-button" type="button" :disabled="saving || deleting || !!pendingDeletion" @click="confirmDeletion(period)">Eliminar período</button>
                </td>
              </tr></tbody>
            </table>
          </div>
          <button class="text-button" type="button" :disabled="loading || saving || deleting || !!pendingDeletion" @click="loadPeriods">Actualizar lista</button>
        </section>
      </template>
    </div>
  </main>
</template>

<style scoped>
.period-form { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; margin: 1.5rem 0 0; padding: 0; border: 0; min-width: 0; }
.period-form > label:first-child { grid-column: 1 / -1; max-width: 20rem; width: 100%; justify-self: center; }
.period-form > p, .period-form > .period-actions { grid-column: 1 / -1; }
.period-form > p { text-align: center; margin: 1rem 0; }
.period-form label { display: grid; gap: .5rem; font-weight: 600; }
.period-form input { padding: .7rem; border: 1px solid var(--color-border-disabled); border-radius: .5rem; width: min(100%, 24rem); color: var(--color-text); background: var(--color-background-top); }
.period-actions { display: flex; flex-wrap: wrap; gap: 1rem; align-items: center; }
.period-form .period-actions { justify-content: center; }
.period-actions button { padding: .8rem 1.2rem; }
.period-table-wrap { overflow-x: auto; }
.period-table { width: 100%; border-collapse: collapse; }
.period-table caption { text-align: left; padding: .5rem 0 1rem; color: var(--color-text-disabled); }
.period-table th, .period-table td { border: 1px solid var(--color-background-mute); padding: .7rem; text-align: left; }
.period-table thead { color: var(--color-text-white); background: var(--sem-blue); }
.delete-button { color: var(--sem-red); }
.deletion-confirmation { margin: 1rem 0; padding: 1rem; border: 1px solid var(--sem-red); border-radius: .5rem; background: var(--color-background-mute); }
.deletion-confirmation h3 { margin: 0; }
@media (max-width: 650px) { .period-form { grid-template-columns: 1fr; } }
</style>
