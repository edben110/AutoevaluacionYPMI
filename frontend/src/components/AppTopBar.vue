<script setup lang="ts">
import { ref } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import menuIcon from '@/assets/icons/menu-burger.svg?raw'
import userIcon from '@/assets/icons/user.svg?raw'
import closeIcon from '@/assets/icons/exit.svg?raw'

withDefaults(defineProps<{ title?: string; navigationOpen?: boolean; accountOpen?: boolean; compact?: boolean }>(), {
  title: 'Ruta de Mejoramiento', navigationOpen: false, accountOpen: false, compact: false,
})
const emit = defineEmits<{ (event: 'toggle-navigation'): void; (event: 'toggle-account'): void }>()
const menuButton = ref<HTMLButtonElement | null>(null)
const accountButton = ref<HTMLButtonElement | null>(null)
defineExpose({
  focusNavigation: () => menuButton.value?.focus(),
  focusAccount: () => accountButton.value?.focus(),
  containsAccountButton: (node: Node) => accountButton.value?.contains(node) === true,
})
</script>

<template>
  <header class="u-topbar sem-topbar">
    <button ref="menuButton" class="u-topbar__action shell-icon-button" type="button" :aria-expanded="navigationOpen" aria-controls="main-navigation" :aria-label="navigationOpen ? 'Cerrar menú de navegación' : 'Abrir menú de navegación'" @click="emit('toggle-navigation')"><SvgIcon :src="navigationOpen && compact ? closeIcon : menuIcon" class="topbar-symbol" /></button>
    <strong>{{ title }}</strong>
    <button ref="accountButton" class="u-topbar__action shell-icon-button" type="button" :aria-expanded="accountOpen" aria-controls="account-panel" aria-label="Abrir opciones de mi cuenta" @click="emit('toggle-account')"><SvgIcon :src="userIcon" class="topbar-symbol" /></button>
  </header>
</template>

<style scoped>
.u-topbar.sem-topbar { min-height: var(--topbar-height); box-shadow: none; }
.u-topbar__action .topbar-symbol { width: 30px; height: 30px; }
</style>
