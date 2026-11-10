// Push web pelo Firebase Cloud Messaging. Opcional: só é carregado (import dinâmico, fora do bundle
// inicial) quando as variáveis VITE_FIREBASE_* existem. Sem elas, os avisos chegam dentro do app.
import { api } from './api'

const cfg = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
}
const vapidKey = import.meta.env.VITE_FIREBASE_VAPID_KEY

export const pushConfigurado = () =>
  Boolean(cfg.apiKey && cfg.projectId && cfg.messagingSenderId && cfg.appId && vapidKey) &&
  'serviceWorker' in navigator && 'Notification' in window && 'PushManager' in window

export async function ativarPush() {
  if (!pushConfigurado()) return { ok: false, motivo: 'Push não configurado neste ambiente.' }
  const permissao = await Notification.requestPermission()
  if (permissao !== 'granted') return { ok: false, motivo: 'Permissão de notificação negada.' }
  try {
    const { initializeApp, getMessaging, getToken, isSupported } = await import('./firebase')
    if (!(await isSupported())) return { ok: false, motivo: 'Este navegador não suporta push.' }
    const app = initializeApp(cfg)
    const registro = await navigator.serviceWorker.ready
    const token = await getToken(getMessaging(app), { vapidKey, serviceWorkerRegistration: registro })
    await api('/api/push/token', { metodo: 'POST', corpo: { token } })
    return { ok: true }
  } catch (e) {
    console.warn('Push indisponível; seguindo com avisos dentro do app.', e)
    return { ok: false, motivo: 'Não foi possível ativar o push. Os avisos continuam aparecendo no app.' }
  }
}
