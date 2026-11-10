import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, ROTA_INICIAL, useAuth } from './lib/auth'
import { NotificacoesProvider } from './lib/notificacoes'
import Layout from './components/Layout'
import Login from './pages/Login'
import { Carregando } from './components/Comum'

// Telas de cada perfil carregadas sob demanda: o bundle inicial fica só com o essencial.
const ComoFunciona = lazy(() => import('./pages/ComoFunciona'))
const NaoEncontrado = lazy(() => import('./pages/NaoEncontrado'))
const Inicio = lazy(() => import('./pages/comerciante/Inicio'))
const Publicar = lazy(() => import('./pages/comerciante/Publicar'))
const ConfirmarRetirada = lazy(() => import('./pages/comerciante/ConfirmarRetirada'))
const Recibos = lazy(() => import('./pages/comerciante/Recibos'))
const Recibo = lazy(() => import('./pages/comerciante/Recibo'))
const Cartaz = lazy(() => import('./pages/comerciante/Cartaz'))
const Impacto = lazy(() => import('./pages/comerciante/Impacto'))
const Proximos = lazy(() => import('./pages/receptor/Proximos'))
const DetalheLote = lazy(() => import('./pages/receptor/DetalheLote'))
const MinhasReservas = lazy(() => import('./pages/receptor/MinhasReservas'))
const Rota = lazy(() => import('./pages/voluntario/Rota'))

function Protegida({ perfil }) {
  const { usuario } = useAuth()
  if (!usuario) return <Navigate to="/entrar" replace />
  if (perfil && usuario.perfil !== perfil) return <Navigate to={ROTA_INICIAL[usuario.perfil]} replace />
  return <Layout />
}

function Raiz() {
  const { usuario } = useAuth()
  return <Navigate to={usuario ? ROTA_INICIAL[usuario.perfil] : '/entrar'} replace />
}

/** "Como funciona" é pública; para quem está logado aparece dentro do layout com a navegação. */
function ComoFuncionaRota() {
  const { usuario } = useAuth()
  return usuario ? <Layout /> : <ComoFunciona />
}

export default function App() {
  return (
    <AuthProvider>
      <NotificacoesProvider>
        <BrowserRouter>
          <Suspense fallback={<Carregando />}>
            <Routes>
              <Route path="/" element={<Raiz />} />
              <Route path="/entrar" element={<Login />} />
              <Route path="/como-funciona" element={<ComoFuncionaRota />}>
                <Route index element={<ComoFunciona />} />
              </Route>

              <Route path="/comerciante" element={<Protegida perfil="COMERCIANTE" />}>
                <Route index element={<Inicio />} />
                <Route path="publicar" element={<Publicar />} />
                <Route path="retirada" element={<ConfirmarRetirada />} />
                <Route path="recibos" element={<Recibos />} />
                <Route path="recibos/:id" element={<Recibo />} />
                <Route path="cartaz" element={<Cartaz />} />
                <Route path="impacto" element={<Impacto />} />
              </Route>

              <Route path="/receptor" element={<Protegida perfil="RECEPTOR" />}>
                <Route index element={<Proximos />} />
                <Route path="lotes/:id" element={<DetalheLote />} />
                <Route path="reservas" element={<MinhasReservas />} />
              </Route>

              <Route path="/voluntario" element={<Protegida perfil="VOLUNTARIO" />}>
                <Route index element={<Rota />} />
              </Route>

              <Route path="*" element={<NaoEncontrado />} />
            </Routes>
          </Suspense>
        </BrowserRouter>
      </NotificacoesProvider>
    </AuthProvider>
  )
}
