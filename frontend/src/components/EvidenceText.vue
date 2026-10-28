<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ text: string }>()
const pieces = computed(() => props.text.split(/(https?:\/\/[^\s<>]+)/g).map(text => ({ text, link: /^https?:\/\//.test(text) })))
</script>

<template>
  <span class="evidence-text"><template v-for="(piece, index) in pieces" :key="index"><a v-if="piece.link" :href="piece.text" target="_blank" rel="noopener noreferrer">{{ piece.text }} <span class="sr-only">(abre en otra pestaña)</span></a><template v-else>{{ piece.text }}</template></template></span>
</template>

<style scoped>
.evidence-text { white-space: pre-wrap; overflow-wrap: anywhere; }
a { color: var(--sem-blue); text-decoration: underline; }
a:focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 3px; }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); white-space: nowrap; }
</style>
