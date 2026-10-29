<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { authorizedFetch, hasSession, responseError } from '@/auth/session'
import type { ComponentGuidance } from '@/selfEvaluation/guidance'

const props = defineProps<{ componentId: string; componentName: string }>()
const emit = defineEmits<{ close: [] }>()
const router = useRouter()
const dialog = ref<HTMLDialogElement | null>(null)
const guidance = ref<ComponentGuidance | null>(null)
const loading = ref(true)
const error = ref('')
let sequence = 0
let controller: AbortController | null = null
let opener: HTMLElement | null = null

const sourceUrl = computed(() => {
  if (!guidance.value?.sourceUrl) return null
  try {
    const url = new URL(guidance.value.sourceUrl)
    return ['http:', 'https:'].includes(url.protocol) ? url.href : null
  } catch { return null }
})

async function load(): Promise<void> {
  const request = ++sequence
  controller?.abort()
  const activeController = new AbortController()
  controller = activeController
  loading.value = true
  error.value = ''
  guidance.value = null
  try {
    const response = await authorizedFetch(`/api/components/${encodeURIComponent(props.componentId)}/guidance`, { signal: activeController.signal })
    if (!response.ok) throw await responseError(response)
    const body = await response.json() as ComponentGuidance
    if (request !== sequence || activeController.signal.aborted) return
    if (body.componentId !== props.componentId) throw new Error('La ayuda recibida no corresponde al componente seleccionado. Reintenta la consulta.')
    guidance.value = body
  } catch (cause) {
    if (request !== sequence || activeController.signal.aborted) return
    error.value = cause instanceof Error ? cause.message : 'No se pudo consultar la ayuda del componente.'
    if (!hasSession.value) { dismiss(); await router.replace({ name: 'login' }) }
  } finally { if (request === sequence) loading.value = false }
}

function dismiss(): void {
  sequence++
  controller?.abort()
  dialog.value?.close()
  if (opener?.isConnected) opener.focus()
  emit('close')
}

function trapFocus(event: KeyboardEvent): void {
  if (event.key !== 'Tab' || !dialog.value) return
  const controls = Array.from(dialog.value.querySelectorAll<HTMLElement>(
    'button:not([disabled]), a[href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
  )).filter(element => !element.hidden && element.getClientRects().length > 0)
  const first = controls[0]
  const last = controls.at(-1)
  if (!first || !last) { event.preventDefault(); dialog.value.focus(); return }
  const active = document.activeElement
  const index = controls.findIndex(element => element === active)
  if ((event.shiftKey && (active === first || index < 0)) || (!event.shiftKey && (active === last || index < 0))) {
    event.preventDefault()
    ;(event.shiftKey ? last : first).focus()
  }
}
watch(() => props.componentId, () => { void load() }, { immediate: true })
onMounted(() => {
  opener = document.activeElement instanceof HTMLElement ? document.activeElement : null
  dialog.value?.showModal()
})
onUnmounted(() => {
  sequence++
  controller?.abort()
  dialog.value?.close()
  if (opener?.isConnected) opener.focus()
})
</script>

<template>
  <dialog ref="dialog" class="component-guidance-dialog" aria-labelledby="component-guidance-title" aria-describedby="component-guidance-intro" @cancel.prevent="dismiss" @keydown="trapFocus">
    <div class="guidance-header">
      <div><span class="guidance-eyebrow">AYUDA DEL COMPONENTE</span><h2 id="component-guidance-title">{{ componentName }}</h2></div>
      <button class="guidance-close" type="button" autofocus aria-label="Cerrar ayuda del componente" @click="dismiss">Cerrar ✕</button>
    </div>
    <p id="component-guidance-intro" class="guidance-intro">Consulta qué evalúa este componente y ejemplos de evidencias, sin modificar tus respuestas.</p>
    <p v-if="loading" class="guidance-message" role="status">Consultando la ayuda…</p>
    <section v-else-if="error" class="guidance-message guidance-error" role="alert"><p>{{ error }}</p><button class="guidance-button" type="button" @click="load">Reintentar consulta</button></section>
    <template v-else-if="guidance">
      <p class="guidance-context">{{ guidance.areaName }} · {{ guidance.processName }}</p>
      <section class="guidance-section" aria-labelledby="guidance-item-title">
        <h3 id="guidance-item-title">¿Qué evalúa este componente?</h3>
        <p class="guidance-item">{{ guidance.evaluationItem || 'La descripción del ítem todavía no está disponible para este componente.' }}</p>
      </section>
      <section class="guidance-section" aria-labelledby="guidance-evidence-title">
        <h3 id="guidance-evidence-title">Ejemplos de evidencias</h3>
        <ul v-if="guidance.evidenceExamples.length"><li v-for="(example, index) in guidance.evidenceExamples" :key="index">{{ example }}</li></ul>
        <p v-else>No hay ejemplos registrados para este componente.</p>
        <p class="guidance-notice">{{ guidance.evidenceNotice || 'Los ejemplos son orientadores, no obligatorios ni exhaustivos.' }}</p>
        <p class="guidance-paper">Si la evidencia está en papel, puedes indicar el nombre del documento, su fecha y dónde se encuentra. No necesitas escanearlo para describirlo aquí.</p>
      </section>
      <section class="guidance-source" aria-labelledby="guidance-source-title">
        <h3 id="guidance-source-title">Fuente y páginas</h3>
        <p v-if="guidance.sourceTitle">{{ guidance.sourceTitle }}</p>
        <p v-if="guidance.itemPages.length">Descripción del ítem: {{ guidance.itemPages.length === 1 ? 'página' : 'páginas' }} {{ guidance.itemPages.join(', ') }}.</p>
        <p v-if="guidance.evidencePages.length">Ejemplos de evidencias: {{ guidance.evidencePages.length === 1 ? 'página' : 'páginas' }} {{ guidance.evidencePages.join(', ') }}.</p>
        <p v-if="!guidance.sourceTitle && !guidance.itemPages.length && !guidance.evidencePages.length">Referencia documental pendiente.</p>
        <a v-if="sourceUrl" :href="sourceUrl" target="_blank" rel="noopener noreferrer">Abrir fuente de consulta en otra pestaña ↗</a>
      </section>
    </template>
  </dialog>
</template>

<style scoped>
.component-guidance-dialog { position: fixed; inset: 0; width: min(760px, calc(100vw - 2rem)); max-height: calc(100dvh - 2rem); margin: auto; padding: clamp(1.1rem, 4vw, 2rem); overflow: auto; border: 1px solid var(--color-background-mute); border-top: 5px solid var(--sem-blue); border-radius: .8rem; color: var(--color-text); background: var(--color-background-top); }
.component-guidance-dialog::backdrop { background: color-mix(in srgb, var(--sem-black) 55%, transparent); }
.guidance-header { display: flex; justify-content: space-between; align-items: start; gap: 1rem; }
.guidance-eyebrow { color: var(--color-text-highlight); font-size: .7rem; letter-spacing: .08em; font-weight: 700; }
h2 { margin: .4rem 0; color: var(--color-text-highlight); font-size: clamp(1.2rem, 4vw, 1.6rem); line-height: 1.35; }
h3 { margin-bottom: .6rem; color: var(--color-text-highlight); font-size: 1.05rem; }
.guidance-close, .guidance-button { flex: 0 0 auto; padding: .55rem .7rem; border: 1px solid var(--sem-blue); border-radius: .4rem; background: var(--color-background-top); color: var(--color-text-highlight); font: inherit; font-size: .82rem; font-weight: 700; }
button:focus-visible, a:focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 3px; }
.guidance-intro { margin: .6rem 0 1.2rem; }
.guidance-context { color: var(--color-text-disabled); font-size: .82rem; }
.guidance-section { margin-top: 1.3rem; padding-top: 1.1rem; border-top: 1px solid var(--color-background-mute); }
.guidance-item, li { white-space: pre-wrap; overflow-wrap: anywhere; }
ul { margin: .6rem 0; padding-left: 1.4rem; }
li { margin: .45rem 0; }
.guidance-notice { margin-top: 1rem; padding: .75rem; border-left: 4px solid var(--sem-yellow); background: var(--color-background-mute); }
.guidance-paper { margin-top: .8rem; }
.guidance-source { margin-top: 1.3rem; padding-top: 1rem; border-top: 1px solid var(--color-background-mute); font-size: .85rem; }
.guidance-source p { margin: .35rem 0; }
.guidance-source a { display: inline-block; margin-top: .5rem; color: var(--color-text-highlight); font-weight: 700; }
.guidance-message { margin: 1rem 0; padding: 1rem; background: var(--color-background-mute); }
.guidance-error { color: var(--sem-red); }
.guidance-button { margin-top: .8rem; }
@media (max-width: 480px) { .guidance-header { flex-wrap: wrap; } }
</style>
