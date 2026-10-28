<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { currentProfile, loadProfile } from '@/auth/session'
const error = ref('')
onMounted(() => void loadProfile().catch(cause => { error.value = cause instanceof Error ? cause.message : 'No se pudo consultar tu cuenta.' }))
</script>

<template>
  <main class="dashboard-page"><div class="dashboard-content account-content">
    <h1>Datos de la cuenta</h1><p v-if="error" class="form-error" role="alert">{{ error }}</p>
    <section v-if="currentProfile" class="profile-card">
      <dl class="account-data"><div><dt>Nombre</dt><dd>{{ currentProfile.name }}</dd></div>
        <div><dt>Tipo de acceso</dt><dd>{{ currentProfile.role === 'SECRETARY' ? 'Secretaría de Educación' : 'Institución educativa' }}</dd></div>
        <div><dt>Correo</dt><dd>{{ currentProfile.email }}</dd></div>
        <div v-if="currentProfile.daneCode"><dt>Código DANE</dt><dd>{{ currentProfile.daneCode }}</dd></div>
        <div v-if="currentProfile.rector"><dt>Nombre del rector</dt><dd>{{ currentProfile.rector }}</dd></div>
      </dl>
      <p>Si necesitas actualizar tus datos o recuperar el acceso, comunícate con el responsable de las cuentas de la Secretaría de Educación.</p>
    </section>
  </div></main>
</template>

<style scoped>
.account-content { max-width: 900px; }
h1 { text-align: center; color: var(--sem-black); text-transform: uppercase; font-size: 1.6rem; }
.account-data > div { display: grid; grid-template-columns: minmax(9rem, 1fr) 2fr; padding: 1rem 0; border-bottom: 1px solid var(--sem-black-mute); gap: 1rem; }
dt { font-weight: 700; } dd { margin: 0; overflow-wrap: anywhere; }
p { margin-top: 1.5rem; }
@media (max-width: 600px) { .account-data > div { grid-template-columns: 1fr; gap: .3rem; } }
</style>
