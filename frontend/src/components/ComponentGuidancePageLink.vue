<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { authorizedFetch, responseError } from '@/auth/session'
import type { ComponentGuidance } from '@/selfEvaluation/guidance'

const props = defineProps<{ componentId: string }>()
const guidance = ref<ComponentGuidance | null>(null)
const error = ref('')
let sequence = 0
let controller: AbortController | null = null

const sourceUrl = computed(() => {
  if (!guidance.value?.sourceUrl) return null
  try {
    const url = new URL(guidance.value.sourceUrl)
    return ['http:', 'https:'].includes(url.protocol) ? url.href : null
  } catch { return null }
})

function sourcePageUrl(page: number): string | null {
  if (!sourceUrl.value || !Number.isInteger(page) || page < 1) return null
  const url = new URL(sourceUrl.value)
  url.hash = `page=${page}`
  return url.href
}

async function load(): Promise<void> {
  const request = ++sequence
  controller?.abort()
  const activeController = new AbortController()
  controller = activeController
  guidance.value = null
  error.value = ''
  try {
    const response = await authorizedFetch(`/api/components/${encodeURIComponent(props.componentId)}/guidance`, { signal: activeController.signal })
    if (!response.ok) throw await responseError(response)
    const body = await response.json() as ComponentGuidance
    if (request !== sequence || activeController.signal.aborted) return
    if (body.componentId !== props.componentId) throw new Error('La ayuda recibida no corresponde al componente seleccionado.')
    guidance.value = body
  } catch (cause) {
    if (request !== sequence || activeController.signal.aborted) return
    error.value = cause instanceof Error ? cause.message : 'No se pudo consultar la referencia del componente.'
  }
}

watch(() => props.componentId, () => { void load() }, { immediate: true })
onUnmounted(() => {
  sequence++
  controller?.abort()
})
</script>

<template>
  <p v-if="error" class="guidance-page-link-error" role="status">{{ error }}</p>
  <p v-else-if="sourceUrl && guidance?.itemPages.length" class="guidance-page-links">
    Descriptor del ítem:
    <template v-for="(page, index) in guidance.itemPages" :key="page">
      <span v-if="index">, </span><a :href="sourcePageUrl(page) ?? undefined" target="_blank" rel="noopener noreferrer">página {{ page }} ↗</a>
    </template>

  </p>
</template>

<style scoped>
.guidance-page-links, .guidance-page-link-error { margin: .6rem 0 0; font-size: .78rem; }
.guidance-page-links a { color: var(--color-text-highlight); font-weight: 700; }
.guidance-page-link-error { color: var(--sem-red); }
</style>
