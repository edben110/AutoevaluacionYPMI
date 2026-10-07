import { computed, ref } from 'vue'

export type UserRole = 'SECRETARY' | 'ESTABLISHMENT'

export interface UserProfile {
  id: string
  name: string
  email: string
  role: UserRole
  daneCode?: string
  rector?: string
}

interface LoginResponse {
  token: string
  id: string
  name: string
  role: UserRole
}

interface ApiErrorResponse {
  message?: string
}

const TOKEN_KEY = 'pmi.auth.token'
const token = ref<string | null>(sessionStorage.getItem(TOKEN_KEY))
const profile = ref<UserProfile | null>(null)

export const hasSession = computed(() => Boolean(token.value))
export const currentProfile = computed(() => profile.value)

export function signOut(): void {
  token.value = null
  profile.value = null
  sessionStorage.removeItem(TOKEN_KEY)
}

export async function authorizedFetch(path: string, options: RequestInit = {}): Promise<Response> {
  if (!token.value) throw new Error('Inicia sesión para continuar.')
  const headers = new Headers(options.headers)
  headers.set('Authorization', `Bearer ${token.value}`)

  let response: Response
  try {
    response = await fetch(path, { ...options, headers })
  } catch {
    throw new Error('No hay conexión con el servidor. Inténtalo de nuevo.')
  }

  if (response.status === 401) {
    signOut()
    throw new Error('Tu sesión terminó. Inicia sesión de nuevo.')
  }
  if (response.status === 403) throw await responseError(response)
  return response
}

export async function responseError(response: Response): Promise<Error> {
  return new Error(await errorMessage(response))
}

async function errorMessage(response: Response): Promise<string> {
  if (response.status === 401) {
    return 'Los datos de acceso no son válidos.'
  }

  try {
    const body = (await response.json()) as ApiErrorResponse
    if (typeof body.message === 'string' && body.message.trim()) return body.message
  } catch {
    // Algunos errores del servidor no tienen cuerpo JSON.
  }

  if (response.status === 403) {
    return 'El servidor rechazó el acceso. Comprueba el tipo de usuario seleccionado.'
  }

  return 'No fue posible completar la solicitud. Inténtalo de nuevo.'
}

export async function loadProfile(): Promise<UserProfile> {
  if (!token.value) throw new Error('Inicia sesión para continuar.')
  if (profile.value) return profile.value

  let response: Response
  try {
    response = await fetch('/api/me', {
      headers: { Authorization: `Bearer ${token.value}` },
    })
  } catch {
    throw new Error('No hay conexión con el servidor. Inténtalo de nuevo.')
  }

  if (response.status === 401 || response.status === 403) {
    signOut()
    throw new Error('Tu sesión terminó. Inicia sesión de nuevo.')
  }
  if (!response.ok) throw new Error(await errorMessage(response))

  const data = (await response.json()) as UserProfile
  if (data.role !== 'SECRETARY' && data.role !== 'ESTABLISHMENT') {
    signOut()
    throw new Error('El servidor devolvió un perfil no válido.')
  }

  profile.value = data
  return data
}

export async function signIn(
  role: UserRole,
  identifier: string,
  password: string,
): Promise<UserProfile> {
  const path =
    role === 'SECRETARY' ? '/api/auth/secretary/login' : '/api/auth/establishment/login'
  const credentials =
    role === 'SECRETARY'
      ? { email: identifier, password }
      : { daneCode: identifier, password }

  let response: Response
  try {
    response = await fetch(path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(credentials),
    })
  } catch {
    throw new Error('No hay conexión con el servidor. Inténtalo de nuevo.')
  }

  if (!response.ok) throw new Error(await errorMessage(response))

  const data = (await response.json()) as LoginResponse
  if (!data.token || data.role !== role) {
    throw new Error('El servidor devolvió una sesión no válida.')
  }

  token.value = data.token
  sessionStorage.setItem(TOKEN_KEY, data.token)
  profile.value = null

  try {
    return await loadProfile()
  } catch (error) {
    signOut()
    throw error
  }
}
