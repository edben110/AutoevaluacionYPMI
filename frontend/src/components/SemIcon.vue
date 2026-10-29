<script setup lang="ts">
import { computed } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import home from '@/assets/icons/home.svg?raw'
import calendar from '@/assets/icons/calendar.svg?raw'
import document from '@/assets/icons/document.svg?raw'
import documentAdd from '@/assets/icons/add-document.svg?raw'
import user from '@/assets/icons/user.svg?raw'
import school from '@/assets/icons/graduation-cap.svg?raw'
import logout from '@/assets/icons/exit.svg?raw'
import settings from '@/assets/icons/settings.svg?raw'
import menu from '@/assets/icons/menu-burger.svg?raw'
import pencil from '@/assets/icons/pencil.svg?raw'
const props = withDefaults(defineProps<{ name: string; size?: number }>(), { size: 24 })
const icons: Record<string, string> = { home, calendar, document, 'document-add': documentAdd, user, school, logout, settings, menu, pencil }
const source = computed(() => icons[props.name])
</script>

<template>
  <SvgIcon v-if="source" :src="source" :style="{ width: size + 'px', height: size + 'px', flexShrink: 0 }" />
  <svg v-else :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">
    <path v-if="name === 'close'" d="m5 5 14 14M19 5 5 19" />
    <path v-else d="M3 11a9 9 0 1 1 3 8M3 4v7h7M12 7v6l4 2" />
  </svg>
</template>
