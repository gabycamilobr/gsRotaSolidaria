// Ponto de entrada do Firebase Cloud Messaging: importado sob demanda só quando o push está configurado.
export { initializeApp } from 'firebase/app'
export { getMessaging, getToken, isSupported } from 'firebase/messaging'
