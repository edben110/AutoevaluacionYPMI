<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { signIn, type UserRole } from '@/auth/session'
import SemIcon from '@/components/SemIcon.vue'
import SemWave from '@/components/SemWave.vue'
import InstitutionalBrand from '@/components/InstitutionalBrand.vue'

const router = useRouter()
const route = useRoute()
const role = ref<UserRole>(route.query.role === 'SECRETARY' ? 'SECRETARY' : 'ESTABLISHMENT')
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
  <main class="login-page sem-login">
    <section class="login-identity" aria-labelledby="intro-title">
      <div class="login-intro">
        <span class="login-kicker">SECRETARÍA DE EDUCACIÓN MUNICIPAL</span>
        <h1 id="intro-title">Autoevaluación y Plan de Mejoramiento Institucional</h1>
        <p>Un espacio para reconocer nuestros avances y construir una mejor educación.</p>
      </div>
      <div class="login-brand"><InstitutionalBrand /></div>
    </section>
    <section class="form-panel" aria-labelledby="login-title">
      <div class="form-wrap">
        <h2 id="login-title">Inicio de sesión</h2>
        <p v-if="route.query.updated === '1'" class="account-updated" role="status">Tus datos se actualizaron. Inicia sesión con la información actualizada.</p>
        <p class="role-label">Soy:</p>
        <div class="role-selector" role="group" aria-label="Tipo de acceso">
          <button type="button" class="u-button role-option" :class="{ selected: isSecretary }" :aria-pressed="isSecretary" :disabled="submitting" @click="chooseRole('SECRETARY')"><SemIcon name="user" :size="28" />Secretaría</button>
          <button type="button" class="u-button role-option" :class="{ selected: !isSecretary }" :aria-pressed="!isSecretary" :disabled="submitting" @click="chooseRole('ESTABLISHMENT')"><SemIcon name="school" :size="32" />Institución</button>
        </div>
        <form class="u-stack login-form" @submit.prevent="submit">
          <div class="u-form-field field">
            <label class="u-form-label" for="identifier">{{ isSecretary ? 'Correo institucional:' : 'Código DANE:' }}</label>
            <input id="identifier" v-model="identifier" class="u-form-input" :type="isSecretary ? 'email' : 'text'" autocomplete="username" :placeholder="isSecretary ? 'ejemplo@sempasto.gov.co' : 'Código DANE de la institución'" :disabled="submitting" required />
          </div>
          <div class="u-form-field field"><label class="u-form-label" for="password">Contraseña:</label><input id="password" v-model="password" class="u-form-input" type="password" autocomplete="current-password" placeholder="Escribe tu contraseña" :disabled="submitting" required /></div>
          <p v-if="error" class="form-error" role="alert">{{ error }}</p>
          <button class="u-button u-button-primary primary-button" type="submit" :disabled="submitting">{{ submitting ? 'Ingresando…' : 'Iniciar sesión' }}</button>
        </form>
        <p class="access-note">Si necesitas recuperar tu acceso, contacta al responsable de las cuentas de la Secretaría de Educación.</p>
      </div>
    </section>
    <SemWave class="login-wave" />
  </main>
</template>

<style scoped>
.sem-login { position: relative; display: grid; grid-template-columns: 60% 40%; min-height: 100dvh; overflow: clip; background: var(--sem-white-mute); }
.login-identity { position: relative; display: flex; align-items: center; min-height: 100dvh; padding: clamp(2rem, 6vw, 7rem); padding-bottom: 11rem; }
.login-kicker { color: var(--sem-black-soft); font-size: .8rem; letter-spacing: .12em; font-weight: 700; }
.login-intro h1 { max-width: 760px; font-size: clamp(2rem, 3.7vw, 4.1rem); color: var(--sem-blue); line-height: 1.2; margin: 1rem 0; }
.login-intro p { max-width: 530px; color: var(--sem-black-soft); font-size: 1.1rem; }
.login-brand { position: absolute; left: 0; bottom: 3rem; width: min(350px, 65%); padding: .8rem 1rem; background: var(--sem-white-top); border-radius: 0 26px 26px 0; }
.sem-login .form-panel { position: relative; z-index: 1; border-radius: 22px 0 0 0; background: var(--sem-white); padding: 3rem clamp(1.5rem, 4vw, 5rem) 8rem; }
.sem-login .form-wrap { width: 100%; max-width: 450px; }
.sem-login .form-wrap h2 { color: var(--sem-black); text-align: center; font-size: clamp(1.7rem, 2.5vw, 2.5rem); letter-spacing: normal; margin: 0 0 2rem; }
.role-label { text-align: center; color: var(--sem-black); font-size: 1.2rem; font-weight: 700; margin-bottom: .8rem; }
.account-updated { margin-bottom: 1rem; padding: .8rem; border-left: 3px solid var(--sem-yellow); background: var(--sem-white-top); color: var(--sem-blue); font-size: .85rem; }
.sem-login .role-selector { border: 2px solid var(--sem-black-mute); background: var(--sem-white-top); border-radius: 14px; padding: 0; gap: 0; }
.sem-login .role-option { display: flex; align-items: center; justify-content: center; gap: .7rem; border: 3px solid transparent; border-radius: 11px; color: var(--sem-black-soft); font-size: 1rem; min-height: 64px; padding: .7rem .4rem; }
.sem-login .role-option.selected { color: var(--sem-blue); border-color: var(--sem-blue); background: var(--sem-white-top); }
.sem-login .login-form { margin-top: 2rem; gap: 1.4rem; }
.sem-login .field { gap: .4rem; }
.sem-login .field label { color: var(--sem-black); font-size: 1.05rem; }
.sem-login .field input { border: 2px solid var(--sem-black-mute); border-radius: 12px; height: 3.5rem; background: var(--sem-white-top); }
.sem-login .field input:focus { border-color: var(--sem-blue); outline: 2px solid var(--sem-yellow); outline-offset: 2px; }
.sem-login .primary-button { justify-self: center; padding: .8rem 2rem; min-width: 210px; height: auto; min-height: 54px; font-size: 1.1rem; margin-top: .8rem; border-radius: 12px; }
.sem-login .access-note { text-align: left; font-size: .82rem; line-height: 1.6; }
.login-wave { position: absolute; right: 0; bottom: 0; width: 54%; height: 170px; z-index: 2; }
@media (max-width: 900px) {
  .sem-login { grid-template-columns: 1fr; }
  .login-identity { min-height: 0; padding: 2rem 1.5rem; }
  .login-intro h1 { font-size: 1.65rem; max-width: 600px; }
  .login-intro p, .login-brand { display: none; }
  .sem-login .form-panel { min-height: 0; padding: 2.5rem 1.5rem 8rem; border-radius: 20px 20px 0 0; }
  .login-wave { height: 80px; width: 85%; }
}
</style>
