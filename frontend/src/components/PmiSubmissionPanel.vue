<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { authorizedFetch, responseError } from '@/auth/session'

interface Submission {
  status: 'DRAFT' | 'SUBMITTED'
  submittedAt: string | null
  canSubmit: boolean
  pending: string[]
}
const props = defineProps<{ year: number; areaId: string }>()
const emit = defineEmits<{ submitted: [] }>()
const state = ref<Submission | null>(null)
const loading = ref(false)
const sending = ref(false)
const error = ref('')
let sequence = 0
let request: AbortController | null = null
const path = () => `/api/pmi/${props.year}/areas/${encodeURIComponent(props.areaId)}/submission`
async function load(): Promise<void> {
  const current = ++sequence
  request?.abort(); request = new AbortController()
  loading.value = true; error.value = ''; state.value = null
  try {
    const response = await authorizedFetch(path(), { signal: request.signal })
    if (!response.ok) throw await responseError(response)
    const data = await response.json() as Submission
    if (current === sequence) state.value = data
  } catch (cause) {
    if (current === sequence) error.value = cause instanceof Error ? cause.message : 'No se pudo comprobar el envío del PMI.'
  } finally { if (current === sequence) loading.value = false }
}
async function submit(): Promise<void> {
  if (loading.value || sending.value || !state.value?.canSubmit) return
  if (!window.confirm('¿Finalizar y enviar el PMI de esta área a Secretaría? Se enviará la información guardada de los anexos 2, 3 y 4, que quedarán para consulta. El Anexo 5 se diligenciará durante el período siguiente.')) return
  const current = sequence
  sending.value = true; error.value = ''
  try {
    const response = await authorizedFetch(path(), { method: 'POST' })
    if (!response.ok) throw await responseError(response)
    const data = await response.json() as Submission
    if (current === sequence) { state.value = data; emit('submitted') }
  } catch (cause) {
    if (current === sequence) error.value = cause instanceof Error ? cause.message : 'No se pudo enviar el PMI. Puedes volver a intentarlo.'
  } finally { if (current === sequence) sending.value = false }
}
function sentDate(value: string): string {
  return new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'America/Bogota' }).format(new Date(value))
}
onMounted(() => void load())
watch(() => [props.year, props.areaId], () => void load())
onUnmounted(() => { sequence++; request?.abort() })
</script>

<template>
  <section class="pmi-submission" aria-labelledby="pmi-submission-title" :aria-busy="loading || sending">
    <h2 id="pmi-submission-title">Entrega del PMI del área</h2>
    <p v-if="loading" role="status">Comprobando los anexos antes del envío…</p>
    <template v-else-if="state?.status === 'SUBMITTED'">
      <p class="submitted" role="status"><strong>PMI enviado a Secretaría.</strong> {{ state.submittedAt ? sentDate(state.submittedAt) : '' }}</p>
      <p>Los anexos 2, 3 y 4 quedan para consulta. El seguimiento del Anexo 5 se habilita en el período siguiente.</p>
    </template>
    <template v-else-if="state">
      <p><strong>Borrador: Secretaría todavía no puede consultar este PMI.</strong> Revisa el consolidado de la tabla y envía cuando los anexos estén completos.</p>
      <details v-if="state.pending.length" class="pending">
        <summary>Pendientes para enviar ({{ state.pending.length }})</summary>
        <ul><li v-for="item in state.pending" :key="item">{{ item }}</li></ul>
      </details>
      <p v-else>Los anexos 2, 3 y 4 están completos según los datos guardados. No se exige diligenciar el seguimiento del Anexo 5 para esta entrega.</p>
      <div class="actions">
        <button id="submit-pmi" type="button" :disabled="!state.canSubmit || sending || loading" @click="submit">{{ sending ? 'Enviando…' : 'Finalizar y enviar PMI del área' }}</button>
        <button type="button" class="outline" :disabled="sending || loading" @click="load">Comprobar pendientes</button>
      </div>
    </template>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <button v-if="error && !state" type="button" :disabled="loading || sending" @click="load">Reintentar consulta</button>
  </section>
</template>

<style scoped>
.pmi-submission { margin: 1.5rem 0; padding: 1.2rem; border: 1px solid var(--color-border-disabled); border-left: 4px solid var(--sem-blue); border-radius: .5rem; background: var(--color-background-top); overflow-wrap: anywhere; }
h2 { color: var(--sem-blue); font-size: 1.15rem; font-weight: 700; }
p { margin: .7rem 0; }
.submitted { color: var(--sem-blue); }
.pending { margin: 1rem 0; }
summary { color: var(--sem-blue); cursor: pointer; font-weight: 700; }
ul { margin: .7rem 0; padding-left: 1.5rem; } li { margin: .5rem 0; }
.actions { display: flex; flex-wrap: wrap; gap: .7rem; margin-top: 1rem; }
button { padding: .7rem 1rem; border: 1px solid var(--sem-blue); border-radius: .4rem; background: var(--sem-blue); color: var(--sem-white-top); font: inherit; font-weight: 700; cursor: pointer; }
button.outline { background: var(--color-background-top); color: var(--sem-blue); }
button:disabled { opacity: .6; cursor: default; }
.error { color: var(--color-text-red); border-left: 3px solid var(--sem-red); padding-left: .7rem; }
:is(button,summary):focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 3px; }
</style>
