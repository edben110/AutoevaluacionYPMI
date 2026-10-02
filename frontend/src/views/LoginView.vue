<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { signIn, type UserRole } from '@/auth/session'

const router = useRouter()
const role = ref<UserRole>('ESTABLISHMENT')
const identifier = ref('')
const password = ref('')
const submitting = ref(false)
const error = ref('')

const isSecretary = computed(() => role.value === 'SECRETARY')

function chooseRole(nextRole: UserRole): void {
  role.value = nextRole
  identifier.value = ''
  password.value = ''
  error.value = ''
}

async function submit(): Promise<void> {
  if (submitting.value) return
  error.value = ''
  submitting.value = true

  try {
    await signIn(role.value, identifier.value.trim(), password.value)
    await router.replace({ name: 'dashboard' })
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'No fue posible iniciar sesión.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="intro-panel" aria-labelledby="intro-title">
      <div class="intro-content">
        <div class="brand brand-light">
          <span class="brand-mark" aria-hidden="true">✦</span>
          <span>Ruta de Mejoramiento</span>
        </div>

        <div class="intro-copy">
          <span class="eyebrow">GESTIÓN EDUCATIVA</span>
          <h1 id="intro-title">Un camino compartido hacia una mejor educación.</h1>
          <p>
            Un espacio para la autoevaluación institucional, el plan de mejoramiento y el
            seguimiento de sus avances.
          </p>
        </div>

        <div class="route-card" aria-label="Etapas de la ruta de mejoramiento">
          <span><b>01</b> Autoevaluación</span>
          <span class="route-line" aria-hidden="true"></span>
          <span><b>02</b> Plan de mejoramiento</span>
          <span class="route-line" aria-hidden="true"></span>
          <span><b>03</b> Seguimiento</span>
        </div>
      </div>
      <div class="intro-decoration intro-decoration-one" aria-hidden="true"></div>
      <div class="intro-decoration intro-decoration-two" aria-hidden="true"></div>
    </section>

    <section class="form-panel" aria-labelledby="login-title">
      <div class="form-wrap">
        <span class="eyebrow eyebrow-dark">BIENVENIDO</span>
        <h2 id="login-title">Ingresa a tu espacio</h2>
        <p class="form-description">Selecciona tu tipo de acceso para continuar.</p>

        <div class="role-selector" role="group" aria-label="Tipo de acceso">
          <button
            type="button"
            class="role-option"
            :class="{ selected: !isSecretary }"
            :aria-pressed="!isSecretary"
            @click="chooseRole('ESTABLISHMENT')"
          >
            Institución educativa
          </button>
          <button
            type="button"
            class="role-option"
            :class="{ selected: isSecretary }"
            :aria-pressed="isSecretary"
            @click="chooseRole('SECRETARY')"
          >
            Secretaría
          </button>
        </div>

        <form class="login-form" @submit.prevent="submit">
          <div class="field">
            <label for="identifier">{{ isSecretary ? 'Correo institucional' : 'Código DANE' }}</label>
            <input
              id="identifier"
              v-model="identifier"
              :type="isSecretary ? 'email' : 'text'"
              :autocomplete="isSecretary ? 'username' : 'off'"
              :placeholder="isSecretary ? 'nombre@secretaria.gov.co' : 'Escribe el código DANE'"
              required
            />
          </div>

          <div class="field">
            <label for="password">Contraseña</label>
            <input
              id="password"
              v-model="password"
              type="password"
              autocomplete="current-password"
              placeholder="Escribe tu contraseña"
              required
            />
          </div>

          <p v-if="error" class="form-error" role="alert">{{ error }}</p>

          <button class="primary-button" type="submit" :disabled="submitting">
            {{ submitting ? 'Ingresando…' : 'Ingresar' }}
            <span aria-hidden="true">→</span>
          </button>
        </form>

        <p class="access-note">
          El acceso es asignado por la Secretaría de Educación Municipal.
        </p>
      </div>
    </section>
  </main>
</template>
