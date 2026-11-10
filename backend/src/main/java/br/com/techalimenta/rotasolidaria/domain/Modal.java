package br.com.techalimenta.rotasolidaria.domain;

/** Meio de transporte do voluntário, com a velocidade média usada na estimativa de tempo. */
public enum Modal {
    A_PE("A pé", 5, "walking"),
    BICICLETA("Bicicleta", 15, "bicycling"),
    CARRO("Carro", 25, "driving");

    private final String rotulo;
    private final int velocidadeKmH;
    private final String travelModeGoogle;

    Modal(String rotulo, int velocidadeKmH, String travelModeGoogle) {
        this.rotulo = rotulo;
        this.velocidadeKmH = velocidadeKmH;
        this.travelModeGoogle = travelModeGoogle;
    }

    public String getRotulo() { return rotulo; }
    public int getVelocidadeKmH() { return velocidadeKmH; }
    public String getTravelModeGoogle() { return travelModeGoogle; }
}
