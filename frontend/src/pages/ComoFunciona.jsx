import { Link } from 'react-router-dom'
import Logo from '../components/Logo'
import { Cabecalho } from '../components/Comum'
import { ROTA_INICIAL, useAuth } from '../lib/auth'

const PASSOS = [
  { titulo: 'O comércio publica', texto: 'Padaria, quitanda ou mercadinho fotografa a sobra e publica em até 30 segundos: categoria, quantidade, peso e até que horas retirar.' },
  { titulo: 'A organização mais próxima é avisada', texto: 'ONGs, igrejas e associações num raio de 2 km que aceitam aquela categoria recebem o aviso, do mais perto ao mais longe. A primeira que reservar leva; a reserva vale 45 minutos.' },
  { titulo: 'As famílias ficam sabendo pelo WhatsApp', texto: 'A organização repassa a mensagem pronta no grupo de WhatsApp que já usa. As famílias respondem à organização, nunca à plataforma.' },
  { titulo: 'O voluntário busca', texto: 'O voluntário recebe a rota de coleta e mostra o QR code no comércio. A retirada é confirmada e o recibo digital é gerado na hora.' },
]

export default function ComoFunciona() {
  const { usuario } = useAuth()
  return (
    <div className={usuario ? 'pilha-grande' : 'pilha-grande'} style={usuario ? undefined : { maxWidth: 640, margin: '0 auto', padding: '1.25rem 16px 3rem' }}>
      {!usuario && (
        <div className="linha-entre">
          <Logo variante="claro" tamanho="pequeno" />
          <Link to="/entrar" className="botao botao-escuro">Entrar</Link>
        </div>
      )}
      <Cabecalho eyebrow="Como funciona" titulo="Da sobra ao prato, no mesmo bairro, em menos de uma hora" />

      <ol className="pilha" style={{ listStyle: 'none', padding: 0, margin: 0 }}>
        {PASSOS.map((p, i) => (
          <li key={p.titulo} className={i === 0 ? 'cartao-destaque' : 'cartao'}>
            <p className="eyebrow">Passo {i + 1}</p>
            <h2 style={{ fontSize: '1.15rem' }}>{p.titulo}</h2>
            <p style={{ margin: 0 }}>{p.texto}</p>
          </li>
        ))}
      </ol>

      <section className="cartao pilha" aria-labelledby="lei">
        <p className="eyebrow">Lei nº 14.016/2020</p>
        <h2 id="lei">Doar alimento é permitido e protegido</h2>
        <p>A Lei nº 14.016, de 23 de junho de 2020, autoriza estabelecimentos que produzem ou vendem alimentos a doar o excedente que não foi vendido e que ainda está próprio para consumo, desde que:</p>
        <ul>
          <li>esteja dentro do prazo de validade e nas condições de conservação indicadas pelo fabricante;</li>
          <li>não tenha a integridade e a segurança sanitária comprometidas, mesmo que a embalagem esteja danificada;</li>
          <li>mantenha as propriedades nutricionais, ainda que tenha aspecto comercialmente indesejável.</li>
        </ul>
        <p>A doação pode ser feita diretamente ou por meio de entidades beneficentes, religiosas ou bancos de alimentos, e é gratuita: não configura relação de consumo. Quem doa de boa-fé só responde por danos se agir com dolo.</p>
        <p style={{ margin: 0 }}>Por isso o Rota Solidária aceita panificados, hortifrúti, industrializados e refrigerados (com transporte refrigerado e retirada em até 2 horas), e não aceita refeições prontas nem itens vencidos. Ao publicar, o comerciante confirma a validade e a conservação; essa declaração fica registrada no lote e no recibo.</p>
      </section>

      <section className="cartao pilha" aria-labelledby="lgpd">
        <p className="eyebrow">Seus dados (LGPD)</p>
        <h2 id="lgpd">Só o mínimo necessário</h2>
        <ul style={{ margin: 0 }}>
          <li><strong>Comércios e organizações:</strong> nome, endereço, localização do estabelecimento e e-mail de acesso. A localização é usada só para calcular quem está a até 2 km, com consentimento no cadastro.</li>
          <li><strong>Voluntários:</strong> nome, organização, meio de transporte e ponto de partida. A localização atual só é usada se você pedir, para calcular a rota, e não é guardada.</li>
          <li><strong>Famílias:</strong> nenhum dado. Elas não se cadastram; quem fala com elas é a organização, pelo WhatsApp dela.</li>
          <li><strong>Fotos:</strong> só do alimento, comprimidas no celular antes do envio.</li>
          <li><strong>Senhas:</strong> guardadas com criptografia (BCrypt), nunca em texto.</li>
        </ul>
      </section>

      {usuario && <Link to={ROTA_INICIAL[usuario.perfil]} className="botao botao-escuro botao-bloco">Voltar ao início</Link>}
    </div>
  )
}
