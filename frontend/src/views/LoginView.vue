<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { signIn, type UserRole } from '@/auth/session'
import semLogo from '@/assets/images/Logo-SEM.svg'
import curveVector from '@/assets/images/Curva-vector.svg'
import graduationCapIcon from '@/assets/icons/graduation-cap.svg'
import userIcon from '@/assets/icons/user.svg'

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
    <section class="intro-panel" aria-label="Presentación institucional">
      <div class="photo-placeholder" aria-label="Espacio reservado para fotografía institucional">
        <span>Fotografía institucional</span>
      </div>
      <div class="sem-logo-container">
        <img class="sem-logo" :src="semLogo" alt="Secretaría de Educación Municipal" />
      </div>
    </section>

    <section class="form-panel" aria-labelledby="login-title">
      <div class="form-wrap">
        <span class="eyebrow eyebrow-dark">BIENVENIDO</span>
        <h2 id="login-title">Ingresa a tu espacio</h2>
        <p class="form-description text-semilight">Selecciona tu tipo de acceso para continuar.</p>

        <div class="role-selector" role="group" aria-label="Tipo de acceso">
          <button
            type="button"
            class="role-option"
            :class="{ selected: isSecretary }"
            :aria-pressed="isSecretary"
            @click="chooseRole('SECRETARY')"
          >
            <img :src="userIcon" alt="" aria-hidden="true" />
            <span>Secretaría</span>
          </button>
          <button
            type="button"
            class="role-option"
            :class="{ selected: !isSecretary }"
            :aria-pressed="!isSecretary"
            @click="chooseRole('ESTABLISHMENT')"
          >
            <img :src="graduationCapIcon" alt="" aria-hidden="true" />
            <span>Institución educativa</span>
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
            {{ submitting ? 'Ingresando…' : 'Iniciar Sesión' }}
          </button>
        </form>

        <p class="access-note">
          El acceso es asignado por la Secretaría de Educación Municipal.
        </p>
      </div>
    </section>
    <img class="curve-vector" :src="curveVector" alt="" aria-hidden="true" />
  </main>
</template>
