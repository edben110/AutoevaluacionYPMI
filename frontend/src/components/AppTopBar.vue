<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import menuIcon from '@/assets/icons/menu-burger.svg?raw'
import userIcon from '@/assets/icons/user.svg?raw'

withDefaults(
  defineProps<{
    title?: string
  }>(),
  {
    title: 'Ruta de Mejoramiento',
  },
)

const emit = defineEmits<{
  (event: 'logout'): void
}>()

const menuOpen = ref(false)
const profileOpen = ref(false)
const menuButtonRef = ref<HTMLButtonElement | null>(null)
const menuPanelRef = ref<HTMLElement | null>(null)
const profileButtonRef = ref<HTMLButtonElement | null>(null)
const profilePanelRef = ref<HTMLElement | null>(null)

function closeAll(): void {
  menuOpen.value = false
  profileOpen.value = false
}

function toggleMenu(): void {
  menuOpen.value = !menuOpen.value
  if (menuOpen.value) profileOpen.value = false
}

function toggleProfileMenu(): void {
  profileOpen.value = !profileOpen.value
  if (profileOpen.value) menuOpen.value = false
}

function handleWindowClick(event: MouseEvent): void {
  const target = event.target as Node | null
  if (!target) return

  const clickedMenuButton = menuButtonRef.value?.contains(target)
  const clickedMenuPanel = menuPanelRef.value?.contains(target)
  const clickedProfileButton = profileButtonRef.value?.contains(target)
  const clickedProfilePanel = profilePanelRef.value?.contains(target)

  if (!clickedMenuButton && !clickedMenuPanel && !clickedProfileButton && !clickedProfilePanel) {
    closeAll()
  }
}

function onLogout(): void {
  closeAll()
  emit('logout')
}

function onMenuKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') closeAll()
}

function onProfileKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') closeAll()
}

onMounted(() => {
  window.addEventListener('click', handleWindowClick)
  window.addEventListener('keydown', onMenuKeydown)
  window.addEventListener('keydown', onProfileKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('click', handleWindowClick)
  window.removeEventListener('keydown', onMenuKeydown)
  window.removeEventListener('keydown', onProfileKeydown)
})
</script>

<template>
  <header class="u-topbar">
    <div class="u-topbar__group">
      <button
        ref="menuButtonRef"
        type="button"
        class="u-topbar__action"
        :aria-expanded="menuOpen ? 'true' : 'false'"
        aria-label="Abrir menú principal"
        @click.stop="toggleMenu"
      >
        <SvgIcon :src="menuIcon" color="var(--color-text-white)" />
      </button>
      <div class="u-topbar__brand">
        <span class="u-topbar__brand-mark" aria-hidden="true">✦</span>
        <span>{{ title }}</span>
      </div>
    </div>

    <div class="u-topbar__group">
      <button
        ref="profileButtonRef"
        type="button"
        class="u-topbar__action u-topbar__action--profile"
        :aria-expanded="profileOpen ? 'true' : 'false'"
        aria-label="Abrir menú de perfil"
        @click.stop="toggleProfileMenu"
      >
        <SvgIcon :src="userIcon" color="var(--color-text-white)" />
      </button>
    </div>

    <aside
      v-if="menuOpen"
      ref="menuPanelRef"
      class="u-flyout u-flyout--left"
      role="menu"
      aria-label="Menú principal"
    >
      <div class="u-flyout__header">
        <span class="u-flyout__title">Navegación</span>
      </div>
      <slot name="menu-items" />
    </aside>

    <aside
      v-if="profileOpen"
      ref="profilePanelRef"
      class="u-flyout u-flyout--right"
      role="menu"
      aria-label="Menú de perfil"
    >
      <div class="u-flyout__header">
        <span class="u-flyout__title">Mi cuenta</span>
      </div>
      <slot name="profile-items" />
    </aside>
  </header>
</template>
