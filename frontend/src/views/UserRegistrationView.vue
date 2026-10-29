<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave, useRouter } from 'vue-router'
import { authorizedFetch, loadProfile, responseError, type UserRole } from '@/auth/session'

interface CreatedUser { id: string; name: string; email: string; daneCode?: string }
const router = useRouter()
const authorized = ref(false)
const role = ref<UserRole>('ESTABLISHMENT')
const form = reactive({ name: '', email: '', password: '', daneCode: '', rector: '' })
const saving = ref(false)
const error = ref('')
const created = ref<CreatedUser | null>(null)
const institution = computed(() => role.value === 'ESTABLISHMENT')
const dirty = computed(() => !created.value && Object.values(form).some(value => value.trim()))
function reset(): void { Object.assign(form, { name: '', email: '', password: '', daneCode: '', rector: '' }); created.value = null; error.value = '' }
async function save(): Promise<void> {
  if (saving.value || !authorized.value) return
  error.value = ''
  const payload = { name: form.name.trim(), email: form.email.trim(), password: form.password,
    ...(institution.value ? { daneCode: form.daneCode.trim(), rector: form.rector.trim() } : {}) }
  if (Object.values(payload).some(value => !value.trim())) { error.value = 'Completa todos los datos requeridos.'; return }
  saving.value = true
  try {
    const response = await authorizedFetch(`/api/users/${institution.value ? 'establishments' : 'secretaries'}`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload),
    })
    if (!response.ok) throw await responseError(response)
    created.value = await response.json() as CreatedUser
    form.password = ''
  } catch (cause) { error.value = cause instanceof Error ? cause.message : 'No se pudo registrar el usuario.' }
  finally { saving.value = false }
}
onMounted(async () => {
  try {
    if ((await loadProfile()).role !== 'SECRETARY') { await router.replace({ name: 'dashboard' }); return }
    authorized.value = true
  } catch (cause) { error.value = cause instanceof Error ? cause.message : 'No se pudo verificar tu acceso.' }
})
onBeforeRouteLeave(() => !saving.value && (!dirty.value || window.confirm('Hay datos de registro sin guardar. ¿Salir y descartarlos?')))
</script>

<template>
  <main class="dashboard-page"><div class="dashboard-content registration-content">
    <RouterLink class="u-return-button" to="/dashboard">Volver a mi espacio</RouterLink>
    <h1>Registrar usuarios</h1><p>Crea las cuentas de las instituciones educativas o del personal de Secretaría.</p>
    <p v-if="error" class="form-error" role="alert">{{ error }}</p>
    <section v-if="created" class="status-card" role="status">
      <h2>Usuario registrado</h2><p>{{ created.name }} · {{ created.email }}</p>
      <p v-if="created.daneCode">La institución puede ingresar con el código DANE {{ created.daneCode }} y la contraseña que definiste. Sus borradores se preparan si hay períodos abiertos.</p>
      <p v-else>La cuenta puede ingresar como Secretaría con su correo y la contraseña definida.</p>
      <button type="button" class="secondary-button" @click="reset">Registrar otro usuario</button>
    </section>
    <form v-else-if="authorized" class="status-card registration-form" @submit.prevent="save">
      <fieldset :disabled="saving">
        <label for="registration-role">Tipo de usuario<select id="registration-role" v-model="role"><option value="ESTABLISHMENT">Institución educativa</option><option value="SECRETARY">Secretaría</option></select></label>
        <label for="registration-name">Nombre<input id="registration-name" v-model="form.name" maxlength="150" required /></label>
        <label for="registration-email">Correo<input id="registration-email" v-model="form.email" type="email" maxlength="150" autocomplete="email" required /></label>
        <label for="registration-password">Contraseña inicial<input id="registration-password" v-model="form.password" type="password" maxlength="255" autocomplete="new-password" required /></label>
        <template v-if="institution"><label for="registration-dane">Código DANE<input id="registration-dane" v-model="form.daneCode" maxlength="20" required /></label><label for="registration-rector">Nombre del rector<input id="registration-rector" v-model="form.rector" maxlength="150" required /></label></template>
        <button type="submit" class="primary-button">{{ saving ? 'Registrando…' : 'Registrar usuario' }}</button>
      </fieldset>
    </form>
  </div></main>
</template>

<style scoped>
.registration-content { max-width: 900px; }
h1, h2 { color: var(--sem-blue); }
.registration-form fieldset { display: grid; grid-template-columns: 1fr 1fr; gap: 1.2rem; padding: 0; border: 0; }
label { display: grid; gap: .4rem; font-weight: 700; }
input, select { width: 100%; min-width: 0; font: inherit; padding: .7rem; border: 1px solid var(--sem-black-mute); border-radius: 6px; color: var(--sem-black); background: var(--sem-white-top); }
.registration-form button { grid-column: 1 / -1; width: auto; justify-self: center; padding: .8rem 2rem; }
p { margin: .8rem 0; }
input:focus-visible, select:focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 2px; }
@media (max-width: 650px) { .registration-form fieldset { grid-template-columns: 1fr; } }
</style>
