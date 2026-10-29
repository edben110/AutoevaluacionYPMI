<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave, useRouter } from 'vue-router'
import { authorizedFetch, currentProfile, loadProfile, responseError, signOut } from '@/auth/session'
import SemIcon from '@/components/SemIcon.vue'
const router = useRouter()
const error = ref('')
const editing = ref(false)
const saving = ref(false)
const saved = ref(false)
const form = reactive({ name: '', email: '', password: '', daneCode: '', rector: '' })
const institution = computed(() => currentProfile.value?.role === 'ESTABLISHMENT')
function patch(): Record<string, string> {
  const data: Record<string, string> = {}
  if (!currentProfile.value) return data
  for (const key of ['name', 'email', ...(institution.value ? ['daneCode', 'rector'] : [])] as const) {
    const field = key as 'name' | 'email' | 'daneCode' | 'rector'
    if (form[field].trim() !== (currentProfile.value[field] ?? '')) data[field] = form[field].trim()
  }
  if (form.password) data.password = form.password
  return data
}
const dirty = computed(() => editing.value && Object.keys(patch()).length > 0)
function reset(): void {
  Object.assign(form, { name: currentProfile.value?.name ?? '', email: currentProfile.value?.email ?? '',
    password: '', daneCode: currentProfile.value?.daneCode ?? '', rector: currentProfile.value?.rector ?? '' })
  error.value = ''; editing.value = false
}
async function save(): Promise<void> {
  if (saving.value || !dirty.value) return
  error.value = ''
  const data = patch()
  if (Object.values(data).some(value => !value.trim())) { error.value = 'Los campos modificados no pueden quedar vacíos.'; return }
  saving.value = true
  const role = currentProfile.value?.role
  try {
    const response = await authorizedFetch('/api/me', { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) })
    if (!response.ok) throw await responseError(response)
    saved.value = true; form.password = ''; signOut()
    await router.replace({ name: 'login', query: { updated: '1', role } })
  } catch (cause) { error.value = cause instanceof Error ? cause.message : 'No se pudieron guardar los datos.' }
  finally { saving.value = false }
}
onMounted(() => void loadProfile().then(reset).catch(cause => { error.value = cause instanceof Error ? cause.message : 'No se pudo consultar tu cuenta.' }))
onBeforeRouteLeave(() => saved.value || (!saving.value && (!dirty.value || window.confirm('Hay datos de cuenta sin guardar. ¿Salir y descartar los cambios?'))))
</script>

<template>
  <main class="dashboard-page"><div class="dashboard-content account-content">
    <RouterLink class="u-return-button" to="/dashboard">Volver a mi espacio</RouterLink>
    <h1>Datos de la cuenta</h1><p v-if="error" class="form-error" role="alert">{{ error }}</p>
    <section v-if="currentProfile" class="profile-card">
      <template v-if="!editing">
      <dl class="account-data"><div><dt>Nombre</dt><dd>{{ currentProfile.name }}</dd></div>
        <div><dt>Tipo de acceso</dt><dd>{{ currentProfile.role === 'SECRETARY' ? 'Secretaría de Educación' : 'Institución educativa' }}</dd></div>
        <div><dt>Correo</dt><dd>{{ currentProfile.email }}</dd></div>
        <div v-if="currentProfile.daneCode"><dt>Código DANE</dt><dd>{{ currentProfile.daneCode }}</dd></div>
        <div v-if="currentProfile.rector"><dt>Nombre del rector</dt><dd>{{ currentProfile.rector }}</dd></div>
      </dl>
      <button type="button" class="secondary-button" @click="editing = true"><SemIcon name="pencil" :size="18" /> Editar datos</button>
      </template>
      <form v-else class="account-form" @submit.prevent="save">
        <fieldset :disabled="saving">
          <label for="account-name">Nombre<input id="account-name" v-model="form.name" maxlength="150" autocomplete="organization" required /></label>
          <label for="account-email">Correo<input id="account-email" v-model="form.email" type="email" maxlength="150" autocomplete="email" required /></label>
          <label for="account-password">Nueva contraseña (opcional)<input id="account-password" v-model="form.password" type="password" maxlength="255" autocomplete="new-password" placeholder="Déjala vacía para conservar la actual" /></label>
          <template v-if="institution">
            <label for="account-dane">Código DANE<input id="account-dane" v-model="form.daneCode" maxlength="20" required /></label>
            <label for="account-rector">Nombre del rector<input id="account-rector" v-model="form.rector" maxlength="150" required /></label>
          </template>
          <p>Al guardar los cambios se cerrará tu sesión. Vuelve a ingresar con tus datos actualizados.</p>
          <div class="account-actions"><button class="primary-button" type="submit" :disabled="!dirty">{{ saving ? 'Guardando…' : 'Guardar cambios y cerrar sesión' }}</button><button class="secondary-button" type="button" @click="reset">Cancelar</button></div>
        </fieldset>
      </form>
    </section>
  </div></main>
</template>

<style scoped>
.account-content { max-width: 900px; }
h1 { text-align: center; color: var(--sem-black); text-transform: uppercase; font-size: 1.6rem; }
.account-data > div { display: grid; grid-template-columns: minmax(9rem, 1fr) 2fr; padding: 1rem 0; border-bottom: 1px solid var(--sem-black-mute); gap: 1rem; }
dt { font-weight: 700; } dd { margin: 0; overflow-wrap: anywhere; }
p { margin-top: 1.5rem; }
.account-form fieldset { border: 0; padding: 0; display: grid; gap: 1rem; }
.account-form label { display: grid; gap: .4rem; font-weight: 700; }
.account-form input { width: 100%; padding: .7rem; border: 1px solid var(--sem-black-mute); border-radius: 6px; font: inherit; color: var(--sem-black); background: var(--sem-white-top); }
.account-form input:focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 2px; }
.account-actions { display: flex; gap: 1rem; align-items: center; flex-wrap: wrap; }
.account-actions .primary-button { width: auto; padding: .8rem 1rem; }
.account-actions .secondary-button { margin-top: 0; }
.secondary-button svg, .secondary-button .svg-icon-wrapper { vertical-align: middle; }
@media (max-width: 600px) { .account-data > div { grid-template-columns: 1fr; gap: .3rem; } }
</style>
