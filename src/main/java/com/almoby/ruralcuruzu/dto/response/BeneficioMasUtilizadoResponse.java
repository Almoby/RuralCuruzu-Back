package com.almoby.ruralcuruzu.dto.response;

/**
 * Fila del ranking global "Beneficios más utilizados" (pantalla de Reportes):
 * a diferencia de {@link UsoBeneficioPorComercioResponse} (que agrupa por
 * comercio), acá cada fila es un beneficio individual, con la cantidad de
 * usos que tuvo en el mes en curso. Se calcula sobre HistorialBeneficio en
 * estado USADO con fechaUso dentro del mes actual (mismo criterio que
 * beneficiosUtilizadosEsteMes de IndicadoresPrincipalesResponse). Ordenado de
 * mayor a menor cantidad de usos, sin límite: el front recorta el top N que
 * necesite mostrar.
 */
public record BeneficioMasUtilizadoResponse(

        String beneficioId,
        String beneficioTitulo,
        String comercioNombre,
        long usosEsteMes

) {
}
