package br.com.techalimenta.rotasolidaria.domain;

/**
 * Categorias de alimento, seguindo a Lei nº 14.016/2020.
 * Refeições prontas e itens vencidos não são aceitos pela plataforma.
 */
public enum Categoria {
    PANIFICADOS("Panificados", true, false, null),
    HORTIFRUTI("Hortifrúti", true, false, null),
    INDUSTRIALIZADOS("Industrializados", true, false, null),
    REFRIGERADOS("Refrigerados", true, true, null),
    REFEICOES_PRONTAS("Refeições prontas", false, false,
            "Não aceitas: exigem controle de temperatura e manipulação que a retirada rápida não garante."),
    VENCIDOS("Itens vencidos", false, false,
            "Não aceitos: a lei protege apenas alimentos dentro do prazo de validade.");

    private final String rotulo;
    private final boolean aceita;
    private final boolean refrigerado;
    private final String motivoRecusa;

    Categoria(String rotulo, boolean aceita, boolean refrigerado, String motivoRecusa) {
        this.rotulo = rotulo;
        this.aceita = aceita;
        this.refrigerado = refrigerado;
        this.motivoRecusa = motivoRecusa;
    }

    public String getRotulo() { return rotulo; }
    public boolean isAceita() { return aceita; }
    public boolean isRefrigerado() { return refrigerado; }
    public String getMotivoRecusa() { return motivoRecusa; }
}
