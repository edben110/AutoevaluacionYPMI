<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { currentProfile, hasSession, loadProfile, signOut } from '@/auth/session'

const router = useRouter()
const loading = ref(true)
const error = ref('')

async function refreshProfile(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    await loadProfile()
  } catch (cause) {
    if (!hasSession.value) {
      await router.replace({ name: 'login' })
      return
    }
    error.value = cause instanceof Error ? cause.message : 'No se pudo cargar tu perfil.'
  } finally {
    loading.value = false
  }
}

function logout(): void {
  signOut()
  void router.replace({ name: 'login' })
}

onMounted(() => {
  void refreshProfile()
})
</script>

<template>
  <main class="dashboard-page">
    <header class="dashboard-header">
      <div class="brand">
        <span class="brand-mark" aria-hidden="true">✦</span>
        <span>Ruta de Mejoramiento</span>
      </div>
      <button class="text-button" type="button" @click="logout">Cerrar sesión</button>
    </header>

    <div class="dashboard-content">
      <div v-if="loading" class="status-card" role="status">Cargando tu espacio…</div>
      <div v-else-if="error" class="status-card" role="alert">
        <p>{{ error }}</p>
        <button class="secondary-button" type="button" @click="refreshProfile">Reintentar</button>
      </div>
      <template v-else-if="currentProfile">
        <div class="welcome-banner">
          <span class="eyebrow">TU ESPACIO DE TRABAJO</span>
          <h1>Hola, {{ currentProfile.name }}</h1>
          <p>
            {{
              currentProfile.role === 'SECRETARY'
                ? 'Accediste como Secretaría de Educación Municipal.'
                : 'Accediste como institución educativa.'
            }}
          </p>
        </div>

        <section class="profile-card" aria-labelledby="profile-title">
          <div class="section-title">
            <span class="section-number">01</span>
            <h2 id="profile-title">Datos de tu sesión</h2>
          </div>
          <dl class="profile-grid">
            <div>
              <dt>Nombre</dt>
              <dd>{{ currentProfile.name }}</dd>
            </div>
            <div>
              <dt>Correo</dt>
              <dd>{{ currentProfile.email }}</dd>
            </div>
            <div v-if="currentProfile.daneCode">
              <dt>Código DANE</dt>
              <dd>{{ currentProfile.daneCode }}</dd>
            </div>
            <div v-if="currentProfile.rector">
              <dt>Rectoría</dt>
              <dd>{{ currentProfile.rector }}</dd>
            </div>
          </dl>
        </section>
        <RouterLink
          v-if="currentProfile.role === 'ESTABLISHMENT'"
          class="secondary-button dashboard-action"
          to="/autoevaluacion"
        >
          Abrir autoevaluación anual
        </RouterLink>
      </template>
    </div>
  </main>
</template>
