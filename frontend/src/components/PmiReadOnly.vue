<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { authorizedFetch, responseError } from '@/auth/session'
import type { Delivery } from '@/review/types'
import type { PriorityMatrix } from '@/pmi/types'
import type { FormulationResponse } from '@/pmi/formulation'
import type { IndicatorDefinition, PmiIndicators } from '@/pmi/indicators'
import { executionLabels, type PmiFollowUp } from '@/pmi/followUp'

const props = defineProps<{ delivery: Delivery; secretary: boolean }>()
const annex = ref(2)
const loading = ref(false)
const error = ref('')
const matrix = ref<PriorityMatrix | null>(null)
const formulation = ref<FormulationResponse | null>(null)
const indicators = ref<PmiIndicators | null>(null)
const followUp = ref<PmiFollowUp | null>(null)
const controlDate = ref('')
const fields: { key: keyof IndicatorDefinition; label: string }[] = [
  { key: 'type', label: 'Tipo' }, { key: 'description', label: 'Descripción' },
  { key: 'measureUnit', label: 'Unidad de medida' }, { key: 'variablesDefinition', label: 'Definición de variables' },
  { key: 'dataFormula', label: 'Fórmula' }, { key: 'dataSource', label: 'Fuente de datos' },
  { key: 'methodologicalAspects', label: 'Aspectos metodológicos' }, { key: 'calculationFrequency', label: 'Frecuencia de cálculo' },
  { key: 'measurementResponsible', label: 'Responsable de medición' },
]
function definitionValue(value: string | null): string {
  return value === 'PROCESS' ? 'Proceso' : value === 'RESULT' ? 'Resultado' : value || 'Sin diligenciar'
}
async function load(number = annex.value, date?: string): Promise<void> {
  annex.value = number; loading.value = true; error.value = ''
  try {
    if (!props.delivery.pmiId) return
    const suffix = ['', '', '', '/formulation', '/indicators', '/follow-ups'][number] ?? ''
    const params = new URLSearchParams()
    if (props.secretary) params.set('establishmentId', props.delivery.establishmentId)
    if (date && number === 5) params.set('controlDate', date)
    const response = await authorizedFetch(`/api/pmi/${props.delivery.year}/areas/${encodeURIComponent(props.delivery.areaId)}${suffix}?${params}`)
    if (!response.ok) throw await responseError(response)
    const data: unknown = await response.json()
    if (number === 2) matrix.value = data as PriorityMatrix
    if (number === 3) formulation.value = data as FormulationResponse
    if (number === 4) indicators.value = data as PmiIndicators
    if (number === 5) { followUp.value = data as PmiFollowUp; controlDate.value = followUp.value.controlDate }
  } catch (cause) { error.value = cause instanceof Error ? cause.message : 'No se pudo consultar el PMI.' }
  finally { loading.value = false }
}
onMounted(() => void load())
</script>

<template>
  <section class="pmi-read" aria-label="PMI de solo lectura">
    <h2>PMI {{ delivery.year }} · {{ delivery.areaName }}</h2>
    <p>Consulta del contenido guardado por la institución, incluidos los apartados que aún estén incompletos.</p>
    <p v-if="!delivery.pmiId" class="status-card">Esta área aún no tiene un PMI iniciado.</p>
    <template v-else>
      <nav class="annexes" aria-label="Anexos del PMI">
        <button v-for="item in [{ n: 2, label: 'Priorización' }, { n: 3, label: 'Plan de mejora' }, { n: 4, label: 'Indicadores' }, { n: 5, label: 'Consolidado y seguimiento' }]" :key="item.n" type="button" :aria-pressed="annex === item.n" :disabled="loading" @click="load(item.n)">Anexo {{ item.n }} · {{ item.label }}</button>
      </nav>
      <p v-if="loading" role="status">Consultando anexo…</p>
      <div v-else-if="error" class="form-error" role="alert"><p>{{ error }}</p><button type="button" class="secondary-button" @click="load(annex)">Reintentar</button></div>
      <template v-else>
        <section v-if="annex === 2 && matrix">
          <h3>Componentes priorizados</h3>
          <p>Menor estado del área: {{ matrix.candidateStateLabel || 'Todos en Mejoramiento continuo' }}. {{ matrix.prioritizationComplete ? 'Priorización completa.' : 'Priorización pendiente.' }}</p>
          <p v-if="matrix.selectionNeedsReview">La selección requiere revisión de la institución.</p>
          <div class="table-scroll" tabindex="0" role="region" aria-label="Matriz de priorización"><table>
            <thead><tr><th>Proceso y componente</th><th>Estado</th><th>Factores críticos</th><th>U</th><th>T</th><th>I</th><th>Total</th><th>Selección</th></tr></thead>
            <tbody><tr v-for="component in matrix.components" :key="component.componentId"><td><strong>{{ component.processName }}</strong><br />{{ component.componentName }}</td><td>{{ component.stateLabel }}</td><td>{{ component.criticalFactors || 'Sin diligenciar' }}</td><td>{{ component.urgency ?? '—' }}</td><td>{{ component.tendency ?? '—' }}</td><td>{{ component.impact ?? '—' }}</td><td>{{ component.total ?? '—' }}</td><td>{{ component.selected ? 'Seleccionado' : component.eligible ? 'Sin seleccionar' : 'Histórico' }}</td></tr></tbody>
          </table></div>
          <p v-if="matrix.components.length === 0">No hay componentes para priorizar en esta área.</p>
        </section>
        <section v-else-if="annex === 3 && formulation">
          <h3>Objetivos, metas y actividades</h3>
          <p v-if="formulation.objectives.length === 0">La institución aún no ha formulado objetivos.</p>
          <article v-for="objective in formulation.objectives" :key="objective.id" class="block">
            <h4>{{ objective.processName }} · {{ objective.componentName }}</h4>
            <p><strong>Objetivo:</strong> {{ objective.description || 'Sin diligenciar' }}</p>
            <p v-if="!objective.editable">Registro histórico conservado del plan.</p>
            <section v-for="(goal, index) in objective.goals" :key="goal.id ?? index" class="goal">
              <p><strong>Meta {{ index + 1 }}:</strong> {{ goal.description || 'Sin diligenciar' }}</p>
              <article v-for="(row, rowIndex) in goal.rows" :key="row.id ?? rowIndex" class="row">
                <h5>{{ row.indicatorName || 'Indicador sin nombre' }}</h5>
                <dl><div><dt>Actividades</dt><dd>{{ row.activities || 'Sin diligenciar' }}</dd></div>
                  <div><dt>Responsable</dt><dd>{{ row.responsible || 'Sin diligenciar' }}</dd></div>
                  <div><dt>Inicio / fin</dt><dd>{{ row.startDate || 'Sin fecha' }} / {{ row.finishDate || 'Sin fecha' }}</dd></div>
                  <div><dt>Costo total / FSE / otros</dt><dd>{{ row.totalCost ?? '—' }} / {{ row.fseAmount ?? '—' }} / {{ row.otherAmount ?? '—' }}</dd></div>
                  <div><dt>Instrumentos de recolección</dt><dd>{{ row.collectionInstruments || 'Sin diligenciar' }}</dd></div>
                  <div><dt>Frecuencia de recolección</dt><dd>{{ row.collectionFrequency || 'Sin diligenciar' }}</dd></div>
                </dl>
              </article>
            </section>
          </article>
          <p>Total de recursos: {{ formulation.totals.totalCost }} · FSE: {{ formulation.totals.fseAmount }} · Otros: {{ formulation.totals.otherAmount }}</p>
        </section>
        <section v-else-if="annex === 4 && indicators">
          <h3>Fichas de indicadores</h3>
          <p v-if="indicators.unnamedRows">Hay {{ indicators.unnamedRows }} filas sin nombre de indicador en el Anexo 3.</p>
          <p v-if="indicators.indicators.length === 0">No hay indicadores con nombre registrados.</p>
          <article v-for="indicator in indicators.indicators" :key="indicator.id" class="block">
            <h4>{{ indicator.name }}</h4><p>{{ indicator.processName }} · {{ indicator.componentName }}</p>
            <p><strong>Objetivo:</strong> {{ indicator.objectiveDescription || 'Sin diligenciar' }}</p>
            <p><strong>Meta:</strong> {{ indicator.goalDescription || 'Sin diligenciar' }}</p>
            <dl><div v-for="field in fields" :key="field.key"><dt>{{ field.label }}</dt><dd>{{ definitionValue(indicator.definition[field.key]) }}</dd></div></dl>
          </article>
        </section>
        <section v-else-if="annex === 5 && followUp">
          <h3>Consolidado y seguimiento</h3>
          <p>{{ followUp.phase === 'COMPLETED' ? 'Seguimiento finalizado.' : followUp.phase === 'PLANNING' ? 'Plan consolidado. El seguimiento se habilita en el siguiente período.' : 'Seguimiento del plan anterior.' }}</p>
          <label v-if="followUp.availableDates.length">Consultar control guardado
            <select :value="controlDate" @change="load(5, ($event.target as HTMLSelectElement).value)">
              <option v-if="!followUp.availableDates.includes(controlDate)" :value="controlDate">{{ controlDate }} · Vista actual</option>
              <option v-for="date in followUp.availableDates" :key="date" :value="date">{{ date }}</option>
            </select>
          </label>
          <p v-if="followUp.rows.length === 0">Aún no hay indicadores o actividades formulados para este seguimiento.</p>
          <article v-for="row in followUp.rows" :key="row.rowId" class="block">
            <h4>{{ row.source.processName }} · {{ row.source.componentName }}</h4>
            <p><strong>Objetivo:</strong> {{ row.source.objectiveDescription || 'Sin diligenciar' }}</p>
            <p><strong>Meta:</strong> {{ row.source.goalDescription || 'Sin diligenciar' }}</p>
            <p><strong>Indicador:</strong> {{ row.source.indicatorName || 'Sin nombre' }}</p>
            <p><strong>Actividades:</strong> {{ row.source.activities || 'Sin diligenciar' }}</p>
            <p><strong>Inicio / fin:</strong> {{ row.source.startDate || 'Sin fecha' }} / {{ row.source.finishDate || 'Sin fecha' }}</p>
            <p v-if="row.sourceRemoved || row.sourceChanged">Este control conserva la información del plan registrado en esa fecha.</p>
            <dl><div><dt>Estado</dt><dd>{{ row.executionStatus ? executionLabels[row.executionStatus] : 'Sin registrar' }}</dd></div>
              <div><dt>Porcentaje</dt><dd>{{ row.executionPercentage === null ? 'Sin registrar' : row.executionPercentage + '%' }}</dd></div>
              <div><dt>Observaciones de seguimiento de la institución</dt><dd>{{ row.observations || 'Sin registrar' }}</dd></div></dl>
          </article>
          <RouterLink v-if="!secretary" class="secondary-button dashboard-action" :to="{ name: 'pmi-follow-up', params: { year: delivery.year, areaId: delivery.areaId } }">Abrir seguimiento de esta área</RouterLink>
        </section>
      </template>
    </template>
  </section>
</template>

<style scoped>
.pmi-read { padding: 1.2rem; background: var(--color-background-top); border: 1px solid var(--color-background-mute); border-radius: .6rem; overflow-wrap: anywhere; }
h2, h3, h4, h5 { color: var(--sem-blue); font-weight: 700; }
h2 { font-size: 1.3rem; } h3 { font-size: 1.15rem; } h5 { font-size: 1rem; }
p { margin: .6rem 0; white-space: pre-wrap; }
.annexes { display: flex; flex-wrap: wrap; gap: .6rem; margin: 1rem 0; }
button, select { padding: .6rem; color: var(--sem-blue); background: var(--color-background-top); border: 1px solid var(--sem-blue); border-radius: .4rem; font: inherit; max-width: 100%; }
button[aria-pressed='true'] { background: var(--sem-blue); color: var(--sem-white-top); }
button:disabled { opacity: .6; cursor: wait; }
.table-scroll { max-width: 100%; overflow-x: auto; margin-top: 1rem; }
table { width: 100%; min-width: 740px; border-collapse: collapse; }
td, th { padding: .7rem; border: 1px solid var(--color-background-mute); text-align: left; vertical-align: top; white-space: pre-wrap; }
th { background: var(--sem-blue); color: var(--sem-white-top); }
.block { margin: 1rem 0; padding: 1rem; border: 1px solid var(--color-background-mute); border-top: 3px solid var(--sem-blue); border-radius: .4rem; }
.goal { margin-top: 1rem; border-left: 3px solid var(--sem-yellow); padding-left: .8rem; }
.row { margin: .8rem 0; padding: .8rem; background: var(--color-background); }
dl > div { margin-top: .7rem; }
dt { font-weight: 700; } dd { margin: .3rem 0; white-space: pre-wrap; }
label { display: grid; gap: .4rem; max-width: 24rem; margin: 1rem 0; }
button:focus-visible, a:focus-visible, select:focus-visible, .table-scroll:focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 3px; }
</style>
