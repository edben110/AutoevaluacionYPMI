<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    src: string
    color?: string
  }>(),
  {
    color: 'currentColor',
  },
)

const normalizedSvg = computed(() => {
  const svg = props.src.trim()

  return svg
    .replace(/<svg\b([^>]*)>/i, (match, attributes) => {
      const sanitizedAttributes = attributes.replace(/\sclass=\"[^"]*\"/gi, '').trim()
      return `<svg ${sanitizedAttributes} class="svg-icon" aria-hidden="true" focusable="false" xmlns="http://www.w3.org/2000/svg">`
    })
    .replace(/fill=(['"])(.*?)\1/gi, 'fill="currentColor"')
    .replace(/stroke=(['"])(.*?)\1/gi, 'stroke="currentColor"')
    .replace(/style=(['"])(.*?)\1/gi, 'style="display:block;width:100%;height:100%"')
})
</script>

<template>
  <span
    class="svg-icon-wrapper"
    :style="{ color }"
    aria-hidden="true"
    v-html="normalizedSvg"
  />
</template>

<style scoped>
.svg-icon-wrapper {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1em;
  height: 1em;
  color: inherit;
  line-height: 0;
}

.svg-icon-wrapper :deep(svg) {
  display: block;
  width: 100%;
  height: 100%;
  color: currentColor;
  fill: currentColor;
  stroke: currentColor;
}
</style>
