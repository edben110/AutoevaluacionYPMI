<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, RouterLink, useRoute, useRouter } from 'vue-router'
import { authorizedFetch, hasSession, loadProfile, responseError } from '@/auth/session'
import type { IndicatorDefinition, IndicatorSheet, PmiIndicators } from '@/pmi/indicators'

type TextField = Exclude<keyof IndicatorDefinition, 'type'>
interface SheetForm {
  source: IndicatorSheet
  values: Record<TextField, string> & { type: '' | 'PROCESS' | 'RESULT' }
  snapshot: string
  saving: boolean
  error: string
  success: string
}
const fields: { key: TextField; label: string; hint: string; max: number }[] = [
  { key: 'description', label: 'Objetivo del indicador', hint: '¿Qué quieren medir y para qué les servirá conocerlo?', max: 10000 },
  { key: 'measureUnit', label: 'Unidad de medida', hint: 'Por ejemplo: porcentaje (%), número de jornadas o nivel de cumplimiento.', max: 500 },
  { key: 'variablesDefinition', label: 'Definición de las variables de la fórmula', hint: 'Explica qué significa cada dato que usarán en el cálculo.', max: 10000 },
  { key: 'dataFormula', label: 'Fórmula de cálculo', hint: 'Por ejemplo: (sesiones realizadas / sesiones programadas) × 100.', max: 10000 },
  { key: 'dataSource', label: 'Fuentes de los datos para el cálculo del indicador', hint: '¿Dónde encontrarán los datos? Por ejemplo: actas, registros o listas de asistencia.', max: 10000 },
  { key: 'methodologicalAspects', label: 'Aspectos metodológicos', hint: 'Explica cómo recogerán y verificarán los datos para medir siempre de la misma manera.', max: 10000 },
  { key: 'calculationFrequency', label: 'Periodicidad de cálculo', hint: '¿Cada cuánto calcularán el indicador? Por ejemplo: mensual, trimestral o anual.', max: 500 },
  { key: 'measurementResponsible', label: 'Responsable de la medición', hint: 'Persona o equipo que recogerá los datos y calculará el indicador.', max: 500 },
]
const route = useRoute()
const router = useRouter()
const year = computed(() => Number(route.params.year))
const areaId = computed(() => String(route.params.areaId ?? ''))
const apiBase = computed(() => `/api/pmi/${year.value}/areas/${encodeURIComponent(areaId.value)}/indicators`)
const source = ref<PmiIndicators | null>(null)
const sheets = reactive<SheetForm[]>([])
const loading = ref(true)
const authorized = ref(false)
const verified = ref(false)
const error = ref('')
let sequence = 0
let request: AbortController | null = null
const hasChanges = computed(() => sheets.some(changed))
const saving = computed(() => sheets.some(form => form.saving))
const completed = computed(() => sheets.filter(form => form.source.complete).length)

function formFor(sheet: IndicatorSheet): SheetForm {
  const values = { type: sheet.definition.type ?? '' } as SheetForm['values']
  for (const field of fields) values[field.key] = sheet.definition[field.key] ?? ''
  return { source: sheet, values, snapshot: JSON.stringify(values), saving: false, error: '', success: '' }
}
function changed(form: SheetForm): boolean { return JSON.stringify(form.values) !== form.snapshot }
function confirmDiscard(): boolean {
  if (!hasSession.value) return true
  if (saving.value) { window.alert('Espera a que termine el guardado de las fichas antes de salir.'); return false }
  return !hasChanges.value || window.confirm('Hay fichas de indicadores sin guardar. ¿Salir y descartar esos cambios?')
}
function beforeUnload(event: BeforeUnloadEvent): void {
  if (!hasSession.value || (!hasChanges.value && !saving.value)) return
  event.preventDefault(); event.returnValue = ''
}
async function load(clear = false): Promise<void> {
  const current = ++sequence
  request?.abort()
  request = new AbortController()
  const signal = request.signal
  loading.value = true; verified.value = false; error.value = ''
  if (clear) { source.value = null; sheets.splice(0) }
  try {
    if (!Number.isInteger(year.value) || year.value < 1 || year.value > 9999 || !areaId.value) throw new Error('Selecciona un año y un área válidos del PMI.')
    const response = await authorizedFetch(apiBase.value, { signal })
    if (!response.ok) throw await responseError(response)
    const data = await response.json() as PmiIndicators
    if (current !== sequence) return
    source.value = data
    sheets.splice(0, sheets.length, ...data.indicators.map(formFor))
    verified.value = true
  } catch (cause) {
    if (current !== sequence || signal.aborted) return
    error.value = cause instanceof Error ? cause.message : 'No se pudieron cargar los indicadores.'
    if (!hasSession.value) await router.replace({ name: 'login' })
  } finally { if (current === sequence) loading.value = false }
}
async function initialize(): Promise<void> {
  loading.value = true; error.value = ''
  try {
    const profile = await loadProfile()
    if (profile.role !== 'ESTABLISHMENT') { await router.replace({ name: 'dashboard' }); return }
    authorized.value = true
    await load(true)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'No se pudo abrir el Anexo 4.'
    loading.value = false
    if (!hasSession.value) await router.replace({ name: 'login' })
  }
}
async function refresh(): Promise<void> {
  if (loading.value || !confirmDiscard()) return
  if (!authorized.value) await initialize()
  else await load()
}
async function save(form: SheetForm): Promise<void> {
  if (form.saving || !verified.value || loading.value || !form.source.editable) return
  const current = sequence
  const path = `${apiBase.value}/${encodeURIComponent(form.source.id)}`
  const body: IndicatorDefinition = {
    type: form.values.type || null,
    description: null, measureUnit: null, variablesDefinition: null, dataFormula: null,
    dataSource: null, methodologicalAspects: null, calculationFrequency: null, measurementResponsible: null,
  }
  for (const field of fields) body[field.key] = form.values[field.key].trim() || null
  form.saving = true; form.error = ''; form.success = ''
  try {
    const response = await authorizedFetch(path, {
      method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body),
    })
    if (!response.ok) throw await responseError(response)
    const saved = await response.json() as IndicatorSheet
    if (current !== sequence) return
    const index = sheets.findIndex(item => item.source.id === saved.id)
    if (index >= 0) sheets.splice(index, 1, { ...formFor(saved), success: 'Ficha guardada. Puedes continuar con el siguiente indicador.' })
  } catch (cause) {
    if (current === sequence) form.error = cause instanceof Error ? cause.message : 'No se pudo guardar la ficha. Tus respuestas siguen aquí; vuelve a intentar.'
    if (!hasSession.value) await router.replace({ name: 'login' })
  } finally { if (current === sequence) form.saving = false }
}
function discard(form: SheetForm): void {
  if (form.saving || !window.confirm('¿Descartar los cambios sin guardar de esta ficha?')) return
  sheets.splice(sheets.indexOf(form), 1, formFor(form.source))
}
onMounted(() => { window.addEventListener('beforeunload', beforeUnload); void initialize() })
watch(() => [route.params.year, route.params.areaId], () => { if (authorized.value) void load(true) })
onBeforeRouteLeave(confirmDiscard)
onBeforeRouteUpdate((to, from) => (to.params.year === from.params.year && to.params.areaId === from.params.areaId) || confirmDiscard())
onUnmounted(() => { sequence++; request?.abort(); window.removeEventListener('beforeunload', beforeUnload) })
</script>

<template>
  <main class="indicators-page">
    <header class="page-header">
      <RouterLink :to="{ name: 'pmi-formulation', params: { year: route.params.year, areaId } }">← Volver al plan · Anexo 3</RouterLink>
      <RouterLink :to="{ name: 'dashboard' }">Mi espacio</RouterLink>
    </header>
    <div class="page-content">
      <span class="eyebrow">PLAN DE MEJORAMIENTO INSTITUCIONAL · ANEXO 4</span>
      <h1>Definamos nuestros indicadores</h1>
      <h2 v-if="source">{{ source.areaName }}</h2>
      <p class="intro">Cada indicador que guardaste en el Anexo 3 tiene su propia tabla. Completa cómo lo van a medir y guarda cada ficha; puedes avanzar con campos pendientes.</p>
      <p v-if="source" class="metadata"><strong>{{ source.establishmentName }}</strong> · Autoevaluación {{ source.year }}</p>
      <p v-if="loading" class="notice" role="status">Cargando indicadores…</p>
      <div v-if="error" class="notice error" role="alert"><p>{{ error }}</p><button type="button" :disabled="loading" @click="refresh">Reintentar</button></div>
      <template v-if="authorized && source">
        <section class="overview" aria-label="Resumen de fichas">
          <p><strong>{{ sheets.length }} {{ sheets.length === 1 ? 'tabla de indicador' : 'tablas de indicadores' }}</strong> · Fichas completas: {{ completed }} según los datos guardados.</p>
          <button class="outline" type="button" :disabled="loading || saving" @click="refresh">Actualizar indicadores</button>
          <p class="hint">Si agregas o cambias un nombre, guarda el plan en el Anexo 3 y actualiza esta lista.</p>
        </section>
        <p v-if="source.unnamedRows" class="notice warning">Hay {{ source.unnamedRows }} {{ source.unnamedRows === 1 ? 'fila sin nombre de indicador' : 'filas sin nombre de indicador' }} en el Anexo 3. Completa y guarda esos nombres para diligenciar sus fichas aquí.</p>
        <p v-if="!source.canEdit" class="notice warning">{{ source.pmiSubmittedAt ? 'PMI enviado a Secretaría. Sus fichas de indicadores están disponibles para consulta.' : 'Solo consulta: revisa el envío de la autoevaluación y la selección de componentes del Anexo 2 para continuar.' }}</p>
        <section v-if="sheets.length === 0 && !loading" class="empty-state">
          <h2>Aún no hay indicadores guardados</h2>
          <p>En el Anexo 3 escribe los nombres de los indicadores y pulsa Guardar este plan. Aquí aparecerá una tabla por cada uno.</p>
          <RouterLink :to="{ name: 'pmi-formulation', params: { year, areaId } }">Ir al Anexo 3 →</RouterLink>
        </section>
        <nav v-if="sheets.length > 1" class="indicator-index" aria-label="Ir a un indicador">
          <a v-for="(form, index) in sheets" :key="form.source.id" :href="`#indicator-${form.source.id}`">{{ index + 1 }}. {{ form.source.name || 'Indicador sin nombre' }}</a>
        </nav>
        <section v-for="(form, index) in sheets" :id="`indicator-${form.source.id}`" :key="form.source.id" class="sheet" :aria-labelledby="`sheet-title-${form.source.id}`">
          <header class="sheet-header">
            <span>INDICADOR {{ index + 1 }}</span>
            <h2 :id="`sheet-title-${form.source.id}`">{{ form.source.name || 'Indicador sin nombre' }}</h2>
            <p>{{ form.source.processName }} · {{ form.source.componentName }}</p>
          </header>
          <div class="sheet-body">
            <dl class="context"><div><dt>Objetivo del plan</dt><dd>{{ form.source.objectiveDescription || 'Pendiente en el Anexo 3' }}</dd></div><div><dt>Meta</dt><dd>{{ form.source.goalDescription || 'Pendiente en el Anexo 3' }}</dd></div></dl>
            <p v-if="!form.source.editable" class="notice warning">{{ source.pmiSubmittedAt ? 'Ficha enviada, disponible para consulta.' : 'Solo consulta: el componente ya no está seleccionado o su indicador necesita un nombre en el Anexo 3. La ficha se conserva.' }}</p>
            <p class="sheet-status" role="status">{{ changed(form) ? 'Cambios sin guardar' : form.source.complete ? 'Ficha completa guardada' : 'Ficha con campos pendientes' }}</p>
            <form @submit.prevent="save(form)">
              <fieldset :disabled="loading || !verified || !form.source.editable || form.saving">
                <legend class="sr-only">Definición de {{ form.source.name || 'indicador' }}</legend>
                <table class="indicator-table">
                  <caption class="sr-only">Conceptos y descripción del indicador {{ index + 1 }}</caption>
                  <thead><tr><th scope="col">Concepto</th><th scope="col">Descripción</th></tr></thead>
                  <tbody>
                    <tr><th scope="row">Nombre del indicador</th><td><p class="indicator-name">{{ form.source.name || 'Pendiente en el Anexo 3' }}</p><small>Se toma del Anexo 3. Cambia el nombre allí para mantener ambos anexos relacionados.</small></td></tr>
                    <tr><th scope="row"><label :for="`type-${form.source.id}`">Tipo de indicador</label></th><td><select :id="`type-${form.source.id}`" v-model="form.values.type" :aria-describedby="`type-hint-${form.source.id}`" @change="form.success = ''"><option value="">Selecciona el tipo</option><option value="PROCESS">Proceso</option><option value="RESULT">Resultado</option></select><small :id="`type-hint-${form.source.id}`">Proceso: mide el avance de las acciones. Resultado: mide los efectos logrados.</small></td></tr>
                    <tr v-for="field in fields" :key="field.key"><th scope="row"><label :for="`${field.key}-${form.source.id}`">{{ field.label }}</label></th><td>
                      <textarea :id="`${field.key}-${form.source.id}`" v-model="form.values[field.key]" :rows="field.max === 500 ? 2 : 3" :maxlength="field.max" :aria-describedby="`${field.key}-hint-${form.source.id}`" @input="form.success = ''" />
                      <small :id="`${field.key}-hint-${form.source.id}`">{{ field.hint }}</small>
                    </td></tr>
                  </tbody>
                </table>
                <div class="sheet-actions"><button type="submit">{{ form.saving ? 'Guardando…' : 'Guardar ficha del indicador' }}</button><button v-if="changed(form)" class="outline" type="button" @click="discard(form)">Descartar cambios</button></div>
              </fieldset>
            </form>
            <p v-if="form.error" class="notice error" role="alert">{{ form.error }} Puedes volver a pulsar Guardar ficha del indicador.</p>
            <p v-if="form.success" class="notice" role="status">{{ form.success }}</p>
          </div>
        </section>
        <section class="overview" aria-labelledby="follow-up-next-title">
          <h2 id="follow-up-next-title">Seguimiento del plan · Anexo 5</h2>
          <p class="hint">Registra el estado de ejecución, el porcentaje de avance y las observaciones de las actividades por fecha de control.</p>
          <RouterLink :to="{ name: 'pmi-follow-up', params: { year, areaId } }">Ver consolidado y seguimiento →</RouterLink>
        </section>
        <p class="source-note">La fórmula define cómo medir el indicador. El Anexo 5 registra el avance de ejecución de las actividades.</p>
      </template>
    </div>
  </main>
</template>

<style scoped>
.indicators-page { min-height: 100vh; background: var(--color-background); }
.page-header { display: flex; justify-content: space-between; flex-wrap: wrap; gap: 1rem; padding: 1.2rem clamp(1rem, 5vw, 5rem); background: var(--color-background-top); border-bottom: 1px solid var(--color-background-mute); }
a { color: var(--color-text-highlight); font-weight: 700; }
.page-content { width: min(100% - 3rem, 1050px); margin: 3rem auto 5rem; }
.eyebrow { color: var(--color-text-highlight); font-size: .74rem; font-weight: 700; letter-spacing: .08em; }
h1 { color: var(--color-text-highlight); margin: .6rem 0; font-size: clamp(1.9rem, 4vw, 2.7rem); line-height: 1.2; }
h2 { color: var(--color-text-highlight); font-size: 1.2rem; }
.intro, .metadata, .hint, .source-note { margin-top: .8rem; color: var(--color-text-disabled); }
.overview, .empty-state { margin-top: 1.5rem; padding: 1.2rem; background: var(--color-background-top); border: 1px solid var(--color-background-mute); border-radius: .65rem; }
.overview { display: flex; flex-wrap: wrap; gap: .8rem 1rem; justify-content: space-between; align-items: center; }
.overview strong { color: var(--color-text-highlight); font-weight: 700; }
.overview .hint { flex-basis: 100%; margin: 0; font-size: .85rem; }
.empty-state p { margin: .7rem 0 1rem; }
.indicator-index { display: grid; gap: .6rem; margin: 1.5rem 0; }
.indicator-index a { overflow-wrap: anywhere; }
.sheet { margin-top: 2rem; border: 1px solid var(--color-background-mute); border-radius: .75rem; overflow: hidden; scroll-margin-top: 1rem; background: var(--color-background-top); }
.sheet-header { padding: 1.2rem clamp(1rem, 3vw, 2rem); background: var(--sem-blue); color: var(--sem-white-top); overflow-wrap: anywhere; }
.sheet-header span { font-size: .75rem; letter-spacing: .08em; }
.sheet-header h2 { margin: .35rem 0; color: var(--sem-white-top); font-weight: 700; }
.sheet-header p { font-size: .85rem; }
.sheet-body { padding: clamp(.8rem, 3vw, 2rem); }
.context { display: grid; gap: .6rem; margin-bottom: 1rem; }
.context dt { color: var(--color-text-highlight); font-weight: 700; font-size: .85rem; }
.context dd { white-space: pre-wrap; overflow-wrap: anywhere; font-size: .9rem; }
.sheet-status { margin: .8rem 0; color: var(--color-text-highlight); font-weight: 700; font-size: .85rem; }
fieldset { border: 0; padding: 0; margin: 0; min-width: 0; }
.indicator-table { border-collapse: collapse; table-layout: fixed; width: 100%; }
.indicator-table th, .indicator-table td { padding: .85rem; border: 1px solid var(--color-border-disabled); text-align: left; vertical-align: top; overflow-wrap: anywhere; }
.indicator-table thead { background: var(--sem-blue); color: var(--sem-white-top); }
.indicator-table thead th:first-child { width: 30%; }
.indicator-table tbody th { background: var(--color-background-mute); color: var(--color-text-highlight); font-weight: 700; font-size: .9rem; }
.indicator-table label { font-weight: 700; }
.indicator-name { white-space: pre-wrap; color: var(--color-text-highlight); font-weight: 700; }
textarea, select { width: 100%; min-width: 0; border: 1px solid var(--color-border-disabled); border-radius: .35rem; background: var(--color-background-top); color: var(--color-text); font: inherit; padding: .65rem; }
textarea { resize: vertical; }
small { display: block; font-size: .79rem; color: var(--color-text-disabled); margin-top: .4rem; }
.sheet-actions { display: flex; gap: .8rem; flex-wrap: wrap; margin-top: 1.1rem; }
button { padding: .7rem 1rem; border: 1px solid var(--sem-blue); border-radius: .4rem; color: var(--sem-white-top); background: var(--sem-blue); font: inherit; font-weight: 700; cursor: pointer; }
button.outline { color: var(--sem-blue); background: var(--color-background-top); }
button:disabled, fieldset:disabled button { opacity: .6; cursor: default; }
fieldset:disabled textarea, fieldset:disabled select { background: var(--color-background-mute); }
button:hover:enabled { filter: brightness(.92); }
:is(a, button, textarea, select):focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 3px; }
.notice { margin: 1rem 0; padding: .9rem; border-left: 4px solid var(--sem-blue); background: var(--color-background-mute); overflow-wrap: anywhere; }
.notice.error { border-color: var(--sem-red); color: var(--color-text-red); }
.notice.warning { border-color: var(--sem-yellow); }
.notice button { margin-top: .7rem; }
.source-note { font-size: .85rem; }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip-path: inset(50%); white-space: nowrap; border: 0; }
@media (max-width: 600px) {
  .page-content { width: calc(100% - 1.5rem); margin-top: 1.8rem; }
  .indicator-table thead th:first-child { width: 35%; }
  .indicator-table th, .indicator-table td { padding: .5rem; }
  .indicator-table tbody th { font-size: .8rem; }
  textarea, select { padding: .45rem; font-size: .9rem; }
  .sheet-actions button { width: 100%; }
}
</style>
