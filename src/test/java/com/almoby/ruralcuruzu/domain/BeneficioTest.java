package com.almoby.ruralcuruzu.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tabla de verdad de límiteUsosPorSocio (mejora pedida por el comercio:
 * poder configurar cuántas veces un mismo socio puede usar un beneficio, en
 * vez del "una sola vez" fijo original). Se prueba en aislamiento porque es
 * lógica de dominio pura, sin dependencias — BeneficioServiceImplTest ya la
 * ejercita indirectamente, pero acá queda cubierta la tabla completa.
 */
class BeneficioTest {

    private Beneficio beneficioConLimite(Integer limite) {
        return Beneficio.builder().id("beneficio-1").titulo("15% en medicamentos").limiteUsosPorSocio(limite).build();
    }

    @Test
    void limiteUsosPorSocioEfectivo_null_esUnoElDefaultLegado() {
        assertThat(beneficioConLimite(null).limiteUsosPorSocioEfectivo()).isEqualTo(1);
    }

    @Test
    void limiteUsosPorSocioEfectivo_cero_sigueSiendoCero() {
        assertThat(beneficioConLimite(0).limiteUsosPorSocioEfectivo()).isEqualTo(0);
    }

    @Test
    void limiteUsosPorSocioEfectivo_positivo_sigueSiendoElMismoValor() {
        assertThat(beneficioConLimite(3).limiteUsosPorSocioEfectivo()).isEqualTo(3);
    }

    @Test
    void puedeCanjear_limiteNull_secomportaComoLimiteUno() {
        Beneficio beneficio = beneficioConLimite(null);

        assertThat(beneficio.puedeCanjear(0)).isTrue();
        assertThat(beneficio.puedeCanjear(1)).isFalse();
        assertThat(beneficio.puedeCanjear(2)).isFalse();
    }

    @Test
    void puedeCanjear_limiteCero_esIlimitadoSinImportarCuantosUsosPrevios() {
        Beneficio beneficio = beneficioConLimite(0);

        assertThat(beneficio.puedeCanjear(0)).isTrue();
        assertThat(beneficio.puedeCanjear(1_000_000)).isTrue();
    }

    @Test
    void puedeCanjear_limitePositivo_permiteHastaJustoAntesDelLimiteYBloqueaEnAdelante() {
        Beneficio beneficio = beneficioConLimite(3);

        assertThat(beneficio.puedeCanjear(0)).isTrue();
        assertThat(beneficio.puedeCanjear(1)).isTrue();
        assertThat(beneficio.puedeCanjear(2)).isTrue(); // sería el 3er uso, justo el límite
        assertThat(beneficio.puedeCanjear(3)).isFalse(); // ya usó las 3 veces permitidas
        assertThat(beneficio.puedeCanjear(4)).isFalse();
    }
}
