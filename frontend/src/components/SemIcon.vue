<script setup lang="ts">
withDefaults(defineProps<{ name: string; size?: number }>(), { size: 24 })
</script>

<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">
    <template v-if="name === 'menu'"><path d="M3 5h18M3 12h18M3 19h18" /></template>
    <template v-else-if="name === 'close'"><path d="m5 5 14 14M19 5 5 19" /></template>
    <template v-else-if="name === 'home'"><path d="m3 10 9-8 9 8v11h-6v-8H9v8H3Z" /></template>
    <template v-else-if="name === 'calendar'"><rect x="3" y="5" width="18" height="16" rx="3" /><path d="M7 2v6M17 2v6M3 10h18" /><circle cx="8" cy="15" r=".8" fill="currentColor" /><circle cx="12" cy="15" r=".8" fill="currentColor" /><circle cx="16" cy="15" r=".8" fill="currentColor" /></template>
    <template v-else-if="name === 'user'"><circle cx="12" cy="6" r="4" fill="currentColor" stroke="none" /><path d="M4 22v-3a8 8 0 0 1 16 0v3Z" fill="currentColor" stroke="none" /></template>
    <template v-else-if="name === 'school'"><path d="m2 8 10-5 10 5-10 5Z" fill="currentColor" /><path d="M5 11v6c4 3 10 3 14 0v-6M22 9v9" /></template>
    <template v-else-if="name === 'logout'"><path d="M9 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h4M8 12h13m-5-5 5 5-5 5" /></template>
    <template v-else-if="name === 'settings'"><circle cx="12" cy="12" r="3" /><path d="m9 3 1-2h4l1 2 2 1 3-.1 2 3-1.5 2.5v3L22 15l-2 3-3-.1-2 1-1 2h-4l-1-2-2-1-3 .1-2-3 1.5-2.5v-3L2 7l2-3 3 .1Z" transform="translate(1 1) scale(.9)" /></template>
    <template v-else-if="name === 'history'"><path d="M3 11a9 9 0 1 1 3 8M3 4v7h7M12 7v6l4 2" /></template>
    <template v-else><path d="M14 2H6a3 3 0 0 0-3 3v14a3 3 0 0 0 3 3h12a3 3 0 0 0 3-3V9Z" /><path d="M14 2v7h7" /><path v-if="name === 'document-add'" d="M12 12v6M9 15h6" /><path v-else d="M8 13h8M8 17h5" /></template>
  </svg>
</template>
