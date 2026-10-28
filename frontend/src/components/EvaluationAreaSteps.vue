<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
const props = defineProps<{ areas: { id: string; name: string }[]; areaId: string; year: number }>()
const ordered = computed(() => [...props.areas].sort((a, b) => a.id.localeCompare(b.id)))
</script>

<template>
  <nav class="evaluation-steps" aria-label="Autoevaluaciones de las áreas de gestión">
    <RouterLink v-for="(area, index) in ordered" :key="area.id" :to="{ name: 'self-evaluation', params: { areaId: area.id }, query: { year } }" :aria-current="area.id === areaId ? 'step' : undefined" :class="{ current: area.id === areaId }"><span class="step-circle">{{ index + 1 }}</span><span class="step-name">{{ area.name }}</span></RouterLink>
  </nav>
</template>

<style scoped>
.evaluation-steps { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: .5rem; position: relative; margin: 2rem 0; }
.evaluation-steps::before { content: ''; position: absolute; top: 27px; height: 3px; left: 12%; right: 12%; background: var(--sem-black-mute); }
a { position: relative; display: flex; flex-direction: column; align-items: center; gap: .5rem; text-decoration: none; color: var(--sem-black-soft); text-align: center; line-height: 1.4; }
.step-circle { display: grid; place-items: center; width: 58px; height: 58px; border: 3px solid var(--sem-black-mute); border-radius: 50%; background: var(--sem-white-top); font-size: 1.3rem; font-weight: 700; }
.current .step-circle { background: var(--sem-yellow); color: var(--sem-black); border-color: var(--sem-blue); box-shadow: inset 0 0 0 4px var(--sem-white-top); }
.current .step-name { color: var(--sem-blue); font-weight: 700; }
.step-name { font-size: .8rem; max-width: 180px; }
a:focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 3px; border-radius: .3rem; }
@media (max-width: 600px) { .step-name { font-size: .65rem; } .step-circle { width: 42px; height: 42px; font-size: 1rem; } .evaluation-steps::before { top: 20px; } }
</style>
