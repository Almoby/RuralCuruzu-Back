package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.almoby.ruralcuruzu.domain.Cuota;
import com.almoby.ruralcuruzu.domain.HistorialBeneficio;
import com.almoby.ruralcuruzu.domain.Socio;
import com.almoby.ruralcuruzu.dto.response.CobranzaMensualResponse;
import com.almoby.ruralcuruzu.dto.response.EstadoSociosResponse;
import com.almoby.ruralcuruzu.dto.response.IndicadoresPrincipalesResponse;
import com.almoby.ruralcuruzu.dto.response.UsoBeneficioPorComercioResponse;
import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoBeneficio;
import com.almoby.ruralcuruzu.enums.EstadoComercio;
import com.almoby.ruralcuruzu.enums.EstadoCuota;
import com.almoby.ruralcuruzu.enums.EstadoSocio;
import com.almoby.ruralcuruzu.enums.EstadoUsoBeneficio;
import com.almoby.ruralcuruzu.enums.TipoBeneficio;
import com.almoby.ruralcuruzu.enums.TipoPersona;
import com.almoby.ruralcuruzu.repository.BeneficioRepository;
import com.almoby.ruralcuruzu.repository.ComercioRepository;
import com.almoby.ruralcuruzu.repository.CuotaRepository;
import com.almoby.ruralcuruzu.repository.HistorialBeneficioRepository;
import com.almoby.ruralcuruzu.repository.SocioRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private SocioRepository socioRepository;
    @Mock
    private CuotaRepository cuotaRepository;
    @Mock
    private ComercioRepository comercioRepository;
    @Mock
    private BeneficioRepository beneficioRepository;
    @Mock
    private HistorialBeneficioRepository historialBeneficioRepository;

    private DashboardServiceImpl service() {
        return new DashboardServiceImpl(socioRepository, cuotaRepository, comercioRepository, beneficioRepository,
                historialBeneficioRepository);
    }

    private static final String PERIODO_ACTUAL = YearMonth.now().toString();
    private static final String PERIODO_ANTERIOR = YearMonth.now().minusMonths(1).toString();

    private Socio socio(String id, EstadoSocio estado, CategoriaSocio categoria, TipoPersona tipoPersona, Instant fechaAlta) {
        return Socio.builder()
                .id(id)
                .numeroSocio("SOC-" + id)
                .estado(estado)
                .categoria(categoria)
                .tipoPersona(tipoPersona)
                .fechaAlta(fechaAlta)
                .build();
    }

    private Cuota cuota(String socioId, String periodo, BigDecimal importe, EstadoCuota estado) {
        return Cuota.builder()
                .socioId(socioId)
                .periodo(periodo)
                .importe(importe)
                .estado(estado)
                .build();
    }

    private HistorialBeneficio historial(String comercioId, String comercioNombre, String socioId,
                                          String beneficioTitulo, EstadoUsoBeneficio estado, Instant fechaUso) {
        return HistorialBeneficio.builder()
                .comercioId(comercioId)
                .comercioNombre(comercioNombre)
                .socioId(socioId)
                .beneficioTitulo(beneficioTitulo)
                .tipo(TipoBeneficio.DESCUENTO_PORCENTAJE)
                .estado(estado)
                .fechaUso(fechaUso)
                .build();
    }

    // ---------- obtenerIndicadoresPrincipales ----------

    @Test
    void indicadores_clasificaSociosActivosPorEstadoDeCuota_eIgnoraInactivos() {
        Instant hace2Meses = Instant.now().minus(Duration.ofDays(60));
        Socio alDia = socio("s1", EstadoSocio.ACTIVO, CategoriaSocio.ACTIVO, TipoPersona.FISICA, hace2Meses);
        Socio pendiente = socio("s2", EstadoSocio.ACTIVO, CategoriaSocio.ACTIVO, TipoPersona.FISICA, hace2Meses);
        Socio vencido = socio("s3", EstadoSocio.ACTIVO, CategoriaSocio.ACTIVO, TipoPersona.FISICA, hace2Meses);
        Socio inactivo = socio("s4", EstadoSocio.INACTIVO, CategoriaSocio.ACTIVO, TipoPersona.FISICA, hace2Meses);
        Socio sinCuotas = socio("s5", EstadoSocio.ACTIVO, CategoriaSocio.ADHERENTE, TipoPersona.JURIDICA, hace2Meses);

        when(socioRepository.findAll()).thenReturn(List.of(alDia, pendiente, vencido, inactivo, sinCuotas));
        when(cuotaRepository.findAll()).thenReturn(List.of(
                cuota("s1", PERIODO_ACTUAL, BigDecimal.TEN, EstadoCuota.PAGADA),
                cuota("s2", PERIODO_ACTUAL, BigDecimal.TEN, EstadoCuota.PENDIENTE),
                cuota("s3", PERIODO_ACTUAL, BigDecimal.TEN, EstadoCuota.VENCIDA)));
        when(comercioRepository.findByEstado(EstadoComercio.ACTIVO)).thenReturn(List.of());
        when(beneficioRepository.findByEstado(EstadoBeneficio.ACTIVO)).thenReturn(List.of());
        when(historialBeneficioRepository.findAll()).thenReturn(List.of());

        IndicadoresPrincipalesResponse resultado = service().obtenerIndicadoresPrincipales();

        assertThat(resultado.totalSocios()).isEqualTo(5);
        assertThat(resultado.sociosConCuotaAlDia()).isEqualTo(2); // s1 y s5 (sin cuotas => al día)
        assertThat(resultado.sociosConCuotaPendiente()).isEqualTo(1);
        assertThat(resultado.sociosConCuotaVencida()).isEqualTo(1);
        // s4 (inactivo) no entra en ninguna de las 3 categorías
    }

    @Test
    void indicadores_unaVencidaGanaAUnaPendienteEnElMismoSocio() {
        Socio socio = socio("s1", EstadoSocio.ACTIVO, CategoriaSocio.ACTIVO, TipoPersona.FISICA, Instant.now());
        when(socioRepository.findAll()).thenReturn(List.of(socio));
        when(cuotaRepository.findAll()).thenReturn(List.of(
                cuota("s1", "2026-01", BigDecimal.TEN, EstadoCuota.PENDIENTE),
                cuota("s1", "2026-02", BigDecimal.TEN, EstadoCuota.VENCIDA)));
        when(comercioRepository.findByEstado(EstadoComercio.ACTIVO)).thenReturn(List.of());
        when(beneficioRepository.findByEstado(EstadoBeneficio.ACTIVO)).thenReturn(List.of());
        when(historialBeneficioRepository.findAll()).thenReturn(List.of());

        IndicadoresPrincipalesResponse resultado = service().obtenerIndicadoresPrincipales();

        assertThat(resultado.sociosConCuotaVencida()).isEqualTo(1);
        assertThat(resultado.sociosConCuotaPendiente()).isEqualTo(0);
    }

    @Test
    void indicadores_facturacionMensualExcluyeAnuladasYSumaElRestoDelPeriodoActual() {
        when(socioRepository.findAll()).thenReturn(List.of());
        when(cuotaRepository.findAll()).thenReturn(List.of(
                cuota("s1", PERIODO_ACTUAL, new BigDecimal("100"), EstadoCuota.PENDIENTE),
                cuota("s2", PERIODO_ACTUAL, new BigDecimal("200"), EstadoCuota.PAGADA),
                cuota("s3", PERIODO_ACTUAL, new BigDecimal("300"), EstadoCuota.ANULADA),
                cuota("s4", PERIODO_ANTERIOR, new BigDecimal("999"), EstadoCuota.PAGADA)));
        when(comercioRepository.findByEstado(EstadoComercio.ACTIVO)).thenReturn(List.of());
        when(beneficioRepository.findByEstado(EstadoBeneficio.ACTIVO)).thenReturn(List.of());
        when(historialBeneficioRepository.findAll()).thenReturn(List.of());

        IndicadoresPrincipalesResponse resultado = service().obtenerIndicadoresPrincipales();

        assertThat(resultado.facturacionMensual()).isEqualByComparingTo("300"); // 100 + 200, sin la ANULADA
    }

    @Test
    void indicadores_variacionPorcentualEsNulaSiNoHuboFacturacionElMesAnterior() {
        when(socioRepository.findAll()).thenReturn(List.of());
        when(cuotaRepository.findAll()).thenReturn(List.of(
                cuota("s1", PERIODO_ACTUAL, new BigDecimal("100"), EstadoCuota.PENDIENTE)));
        when(comercioRepository.findByEstado(EstadoComercio.ACTIVO)).thenReturn(List.of());
        when(beneficioRepository.findByEstado(EstadoBeneficio.ACTIVO)).thenReturn(List.of());
        when(historialBeneficioRepository.findAll()).thenReturn(List.of());

        IndicadoresPrincipalesResponse resultado = service().obtenerIndicadoresPrincipales();

        assertThat(resultado.variacionPorcentualFacturacionVsMesAnterior()).isNull();
    }

    @Test
    void indicadores_deudaAcumuladaSoloSumaVencidasYCuentaSociosEnMoraUnaVez() {
        when(socioRepository.findAll()).thenReturn(List.of());
        when(cuotaRepository.findAll()).thenReturn(List.of(
                cuota("s1", "2026-01", new BigDecimal("50"), EstadoCuota.VENCIDA),
                cuota("s1", "2026-02", new BigDecimal("50"), EstadoCuota.VENCIDA),
                cuota("s2", "2026-01", new BigDecimal("30"), EstadoCuota.PENDIENTE)));
        when(comercioRepository.findByEstado(EstadoComercio.ACTIVO)).thenReturn(List.of());
        when(beneficioRepository.findByEstado(EstadoBeneficio.ACTIVO)).thenReturn(List.of());
        when(historialBeneficioRepository.findAll()).thenReturn(List.of());

        IndicadoresPrincipalesResponse resultado = service().obtenerIndicadoresPrincipales();

        assertThat(resultado.deudaAcumulada()).isEqualByComparingTo("100");
        assertThat(resultado.sociosEnMora()).isEqualTo(1);
    }

    @Test
    void indicadores_beneficiosUtilizadosEsDelMesActualYExcluyeAnulados() {
        when(socioRepository.findAll()).thenReturn(List.of());
        when(cuotaRepository.findAll()).thenReturn(List.of());
        when(comercioRepository.findByEstado(EstadoComercio.ACTIVO)).thenReturn(List.of());
        when(beneficioRepository.findByEstado(EstadoBeneficio.ACTIVO)).thenReturn(List.of());
        when(historialBeneficioRepository.findAll()).thenReturn(List.of(
                historial("c1", "Farmacia", "s1", "15%", EstadoUsoBeneficio.USADO, Instant.now()),
                historial("c1", "Farmacia", "s2", "15%", EstadoUsoBeneficio.USADO,
                        Instant.now().minus(Duration.ofDays(400))),
                historial("c1", "Farmacia", "s3", "15%", EstadoUsoBeneficio.ANULADO, Instant.now())));

        IndicadoresPrincipalesResponse resultado = service().obtenerIndicadoresPrincipales();

        assertThat(resultado.beneficiosUtilizados()).isEqualTo(1); // solo el de este mes (no el de hace 400 días)
        assertThat(resultado.beneficiosUtilizadosHistoricoTotal()).isEqualTo(2); // los 2 USADO, sin importar la fecha
    }

    // ---------- obtenerCobranzaMensual ----------

    @Test
    void cobranzaMensual_devuelveLos12MesesConCeroDondeNoHayDatos() {
        int anio = YearMonth.now().getYear();
        when(cuotaRepository.findAll()).thenReturn(List.of());

        List<CobranzaMensualResponse> resultado = service().obtenerCobranzaMensual(anio);

        assertThat(resultado).hasSize(12);
        assertThat(resultado.get(0).periodo()).isEqualTo(YearMonth.of(anio, 1).toString());
        assertThat(resultado.get(0).cobrado()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.get(0).pendiente()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void cobranzaMensual_separaCobradoDePendienteEnElMesCorrecto() {
        int anio = 2026;
        when(cuotaRepository.findAll()).thenReturn(List.of(
                cuota("s1", "2026-03", new BigDecimal("500"), EstadoCuota.PAGADA),
                cuota("s2", "2026-03", new BigDecimal("200"), EstadoCuota.PENDIENTE),
                cuota("s3", "2026-03", new BigDecimal("100"), EstadoCuota.VENCIDA),
                cuota("s4", "2026-03", new BigDecimal("999"), EstadoCuota.ANULADA)));

        List<CobranzaMensualResponse> resultado = service().obtenerCobranzaMensual(anio);
        CobranzaMensualResponse marzo = resultado.get(2);

        assertThat(marzo.mes()).isEqualTo("Mar");
        assertThat(marzo.cobrado()).isEqualByComparingTo("500");
        assertThat(marzo.pendiente()).isEqualByComparingTo("300"); // 200 + 100, sin la anulada
    }

    // ---------- obtenerEstadoSocios ----------

    @Test
    void estadoSocios_filtraPorCategoriaYTipoPersona() {
        Socio activoFisica = socio("s1", EstadoSocio.ACTIVO, CategoriaSocio.ACTIVO, TipoPersona.FISICA, Instant.now());
        Socio adherenteFisica = socio("s2", EstadoSocio.ACTIVO, CategoriaSocio.ADHERENTE, TipoPersona.FISICA, Instant.now());
        Socio activoJuridica = socio("s3", EstadoSocio.ACTIVO, CategoriaSocio.ACTIVO, TipoPersona.JURIDICA, Instant.now());
        when(socioRepository.findAll()).thenReturn(List.of(activoFisica, adherenteFisica, activoJuridica));
        when(cuotaRepository.findAll()).thenReturn(List.of());

        EstadoSociosResponse resultado = service().obtenerEstadoSocios(CategoriaSocio.ACTIVO, TipoPersona.FISICA);

        assertThat(resultado.alDia()).isEqualTo(1); // solo activoFisica pasa ambos filtros
        assertThat(resultado.pendientes()).isZero();
        assertThat(resultado.vencidos()).isZero();
        assertThat(resultado.inactivos()).isZero();
    }

    @Test
    void estadoSocios_sinFiltros_cuentaInactivosPorSeparado() {
        Socio activo = socio("s1", EstadoSocio.ACTIVO, CategoriaSocio.ACTIVO, TipoPersona.FISICA, Instant.now());
        Socio inactivo = socio("s2", EstadoSocio.INACTIVO, CategoriaSocio.ACTIVO, TipoPersona.FISICA, Instant.now());
        Socio dadoDeBaja = socio("s3", EstadoSocio.DADO_DE_BAJA, CategoriaSocio.ACTIVO, TipoPersona.FISICA, Instant.now());
        when(socioRepository.findAll()).thenReturn(List.of(activo, inactivo, dadoDeBaja));
        when(cuotaRepository.findAll()).thenReturn(List.of());

        EstadoSociosResponse resultado = service().obtenerEstadoSocios(null, null);

        assertThat(resultado.alDia()).isEqualTo(1);
        assertThat(resultado.inactivos()).isEqualTo(2);
    }

    // ---------- obtenerUsoBeneficiosPorComercio ----------

    @Test
    void usoBeneficios_agrupaPorComercioYCalculaSociosUnicosYPromocionMasUsada() {
        when(historialBeneficioRepository.findAll()).thenReturn(List.of(
                historial("c1", "Farmacia", "s1", "15% medicamentos", EstadoUsoBeneficio.USADO, Instant.now()),
                historial("c1", "Farmacia", "s1", "15% medicamentos", EstadoUsoBeneficio.USADO, Instant.now()),
                historial("c1", "Farmacia", "s2", "2x1 vitaminas", EstadoUsoBeneficio.USADO, Instant.now()),
                historial("c2", "Gimnasio", "s1", "Clase gratis", EstadoUsoBeneficio.USADO, Instant.now()),
                historial("c1", "Farmacia", "s3", "15% medicamentos", EstadoUsoBeneficio.ANULADO, Instant.now())));

        List<UsoBeneficioPorComercioResponse> resultado = service().obtenerUsoBeneficiosPorComercio();

        assertThat(resultado).hasSize(2);
        UsoBeneficioPorComercioResponse farmacia = resultado.get(0);
        assertThat(farmacia.comercioNombre()).isEqualTo("Farmacia");
        assertThat(farmacia.cantidadBeneficiosUtilizados()).isEqualTo(3); // no cuenta la ANULADA
        assertThat(farmacia.cantidadSociosUnicos()).isEqualTo(2); // s1 y s2
        assertThat(farmacia.promocionMasUtilizada()).isEqualTo("15% medicamentos");

        UsoBeneficioPorComercioResponse gimnasio = resultado.get(1);
        assertThat(gimnasio.comercioNombre()).isEqualTo("Gimnasio");
        assertThat(gimnasio.cantidadBeneficiosUtilizados()).isEqualTo(1);
    }

    @Test
    void usoBeneficios_agrupaUsoPorPeriodo() {
        Instant esteMes = Instant.now();
        Instant mesPasado = esteMes.minus(Duration.ofDays(35));
        when(historialBeneficioRepository.findAll()).thenReturn(List.of(
                historial("c1", "Farmacia", "s1", "15%", EstadoUsoBeneficio.USADO, esteMes),
                historial("c1", "Farmacia", "s2", "15%", EstadoUsoBeneficio.USADO, mesPasado)));

        List<UsoBeneficioPorComercioResponse> resultado = service().obtenerUsoBeneficiosPorComercio();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).usoPorPeriodo()).hasSize(2);
    }
}
