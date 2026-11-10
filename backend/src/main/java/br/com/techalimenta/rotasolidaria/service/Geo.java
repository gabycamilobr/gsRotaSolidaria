package br.com.techalimenta.rotasolidaria.service;

/** Cálculos geográficos simples (sem API paga): distância em linha reta pela fórmula de haversine. */
public final class Geo {

    private static final double RAIO_TERRA_M = 6_371_008.8;

    private Geo() {}

    public static double distanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RAIO_TERRA_M * Math.asin(Math.sqrt(a));
    }
}
