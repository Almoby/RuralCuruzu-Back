package com.almoby.ruralcuruzu.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.almoby.ruralcuruzu.domain.Beneficio;
import com.almoby.ruralcuruzu.domain.Comercio;
import com.almoby.ruralcuruzu.domain.HistorialBeneficio;
import com.almoby.ruralcuruzu.dto.response.EstadisticasComercioResponse;
import com.almoby.ruralcuruzu.dto.response.InicioComercioResponse;
import com.almoby.ruralcuruzu.dto.response.UsoDiaSemanaResponse;
import com.almoby.ruralcuruzu.dto.response.UsoMensualResponse;
import com.almoby.ruralcuruzu.enums.EstadoBeneficio;
import com.almoby.ruralcuruzu.enums.EstadoComercio;
import com.almoby.ruralcuruzu.enums.EstadoUsoBeneficio;
import com.almoby.ruralcuruzu.exception.ComercioNoEncontradoException;
import com.almoby.ruralcuruzu.repository.BeneficioRepository;
import com.almoby.ruralcuruzu.repository.ComercioRepository;
import com.almoby.ruralcuruzu.repository.HistorialBeneficioRepository;

/**
 * El "Inicio" del portal de comercio se arma sobre HistorialBeneficio filtrado
 * por comercioId, con una sola consulta acotada compartida entre indicadores
 * y gráfico semanal. Estos tests cubren los puntos que más fácil se rompen:
 * que ANULADO no cuente como uso real, que la semana siempre traiga los 7
 * días aunque algunos no tengan usos, y que "socios alcanzados" sea histórico
 * (no acotado al mes) mientras el resto de los indicadores sí lo están.
 */
@ExtendWith(MockitoExtension.class)
class ComercioDashboardServiceImplTest {

    @Mock
    private ComercioRepository comercioRepository;
    @Mock
    private BeneficioRepository beneficioRepository;
    @Mock
    private HistorialBeneficioRepository historialBeneficioRepository;

    private ComercioDashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ComercioDashboardServiceImpl(comercioRepository, beneficioRepository, historialBeneficioRepository);
    }

    private Comercio comercio(EstadoComercio estado) {
        return Comercio.builder().id("comercio-1").estado(estado).build();
    }

    private Beneficio beneficio(EstadoBeneficio estado) {
        return Beneficio.builder().id("beneficio-1").comercioId("comercio-1").estado(estado).build();
    }

    private HistorialBeneficio uso(String socioId, EstadoUsoBeneficio estado, Instant fechaUso) {
        return HistorialBeneficio.builder()
                .comercioId("comercio-1")
                .socioId(socioId)
                .estado(estado)
                .fechaUso(fechaUso)
                .build();
    }

    private HistorialBeneficio usoDePromocion(String socioId, String socioNombre, String beneficioId,
                                               String beneficioTitulo, EstadoUsoBeneficio estado, Instant fechaUso) {
        return HistorialBeneficio.builder()
                .comercioId("comercio-1")
                .socioId(socioId)
                .socioNombre(socioNombre)
                .beneficioId(beneficioId)
                .beneficioTitulo(beneficioTitulo)
                .estado(estado)
                .fechaUso(fechaUso)
                .build();
    }

    @Test
    void obtenerInicio_calculaIndicadoresYSerieSemanalEnUnaSolaConsultaDeHistorial() {
        when(comercioRepository.findById("comercio-1")).thenReturn(Optional.of(comercio(EstadoComercio.ACTIVO)));
        when(beneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of(
                beneficio(EstadoBeneficio.ACTIVO), beneficio(EstadoBeneficio.INACTIVO)));

        // "ahora" siempre cae dentro del mes y de la semana en curso: sirve para las
        // tres ventanas de tiempo (mes, semana y hoy) sin depender de qué día se corra el test.
        Instant ahora = Instant.now();
        when(historialBeneficioRepository.findByComercioIdAndFechaUsoAfter(eq("comercio-1"), any(Instant.class)))
                .thenReturn(List.of(
                        uso("socio-1", EstadoUsoBeneficio.USADO, ahora),
                        uso("socio-2", EstadoUsoBeneficio.USADO, ahora),
                        uso("socio-3", EstadoUsoBeneficio.ANULADO, ahora)));

        // Histórico completo (findByComercioId, sin filtro de fecha): incluye un uso de hace 40 días
        // para probar que "socios alcanzados" no se acota al mes como el resto de los indicadores.
        when(historialBeneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of(
                uso("socio-1", EstadoUsoBeneficio.USADO, ahora),
                uso("socio-1", EstadoUsoBeneficio.USADO, ahora.minusSeconds(86_400 * 40)),
                uso("socio-2", EstadoUsoBeneficio.USADO, ahora),
                uso("socio-3", EstadoUsoBeneficio.ANULADO, ahora)));

        InicioComercioResponse respuesta = service.obtenerInicio("comercio-1");

        assertThat(respuesta.estado()).isEqualTo(EstadoComercio.ACTIVO);
        assertThat(respuesta.indicadores().promocionesActivas()).isEqualTo(1L);
        assertThat(respuesta.indicadores().usosEsteMes()).isEqualTo(2L);
        assertThat(respuesta.indicadores().validacionesHoy()).isEqualTo(2L);
        // socio-1 aparece dos veces (distintos meses) pero cuenta una sola vez; socio-3 está ANULADO y no cuenta.
        assertThat(respuesta.indicadores().sociosAlcanzados()).isEqualTo(2L);

        List<UsoDiaSemanaResponse> usosPorDia = respuesta.usosPorDia();
        assertThat(usosPorDia).hasSize(7);
        assertThat(usosPorDia.get(0).dia()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(usosPorDia.get(6).dia()).isEqualTo(DayOfWeek.SUNDAY);

        DayOfWeek hoy = ahora.atZone(ZoneId.systemDefault()).getDayOfWeek();
        long totalDeLaSemana = usosPorDia.stream().mapToLong(UsoDiaSemanaResponse::cantidad).sum();
        assertThat(totalDeLaSemana).isEqualTo(2L);
        assertThat(usosPorDia.stream().filter(r -> r.dia() == hoy).findFirst().orElseThrow().cantidad())
                .isEqualTo(2L);
        assertThat(usosPorDia.stream().filter(r -> r.dia() != hoy).allMatch(r -> r.cantidad() == 0L)).isTrue();
    }

    @Test
    void obtenerInicio_sinUsosRegistrados_devuelveIndicadoresEnCeroYLos7DiasEnCero() {
        when(comercioRepository.findById("comercio-1")).thenReturn(Optional.of(comercio(EstadoComercio.ACTIVO)));
        when(beneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of());
        when(historialBeneficioRepository.findByComercioIdAndFechaUsoAfter(eq("comercio-1"), any(Instant.class)))
                .thenReturn(List.of());
        when(historialBeneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of());

        InicioComercioResponse respuesta = service.obtenerInicio("comercio-1");

        assertThat(respuesta.indicadores().usosEsteMes()).isZero();
        assertThat(respuesta.indicadores().promocionesActivas()).isZero();
        assertThat(respuesta.indicadores().sociosAlcanzados()).isZero();
        assertThat(respuesta.indicadores().validacionesHoy()).isZero();
        assertThat(respuesta.usosPorDia()).hasSize(7);
        assertThat(respuesta.usosPorDia().stream().allMatch(r -> r.cantidad() == 0L)).isTrue();
    }

    @Test
    void obtenerInicio_comercioSuspendido_devuelveElEstadoParaQueElFrontMuestreElAviso() {
        when(comercioRepository.findById("comercio-1")).thenReturn(Optional.of(comercio(EstadoComercio.SUSPENDIDO)));
        when(beneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of());
        when(historialBeneficioRepository.findByComercioIdAndFechaUsoAfter(eq("comercio-1"), any(Instant.class)))
                .thenReturn(List.of());
        when(historialBeneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of());

        InicioComercioResponse respuesta = service.obtenerInicio("comercio-1");

        assertThat(respuesta.estado()).isEqualTo(EstadoComercio.SUSPENDIDO);
    }

    @Test
    void obtenerInicio_comercioInexistente_lanzaExcepcion() {
        when(comercioRepository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerInicio("no-existe"))
                .isInstanceOf(ComercioNoEncontradoException.class);
    }

    // ---------- obtenerEstadisticas ----------

    @Test
    void obtenerEstadisticas_calculaIndicadoresHistoricoYEsteMesIgnorandoAnulados() {
        when(beneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of(
                beneficio(EstadoBeneficio.ACTIVO), beneficio(EstadoBeneficio.INACTIVO)));

        Instant ahora = Instant.now();
        Instant haceDosMeses = ahora.minusSeconds(86_400L * 65);
        when(historialBeneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of(
                usoDePromocion("socio-1", "Juan", "beneficio-1", "15% en medicamentos",
                        EstadoUsoBeneficio.USADO, ahora),
                usoDePromocion("socio-2", "Laura", "beneficio-1", "15% en medicamentos",
                        EstadoUsoBeneficio.USADO, haceDosMeses),
                usoDePromocion("socio-3", "Ana", "beneficio-1", "15% en medicamentos",
                        EstadoUsoBeneficio.ANULADO, ahora)));

        EstadisticasComercioResponse respuesta = service.obtenerEstadisticas("comercio-1", Year.now().getValue());

        assertThat(respuesta.indicadores().usosHistoricoTotal()).isEqualTo(2L); // el ANULADO no cuenta
        assertThat(respuesta.indicadores().sociosUnicos()).isEqualTo(2L);
        assertThat(respuesta.indicadores().promocionesActivas()).isEqualTo(1L);
        assertThat(respuesta.indicadores().usosEsteMes()).isEqualTo(1L); // solo el de "ahora", no el de hace 2 meses
    }

    @Test
    void obtenerEstadisticas_serieMensualTraeLos12MesesConCeroDondeNoHuboUsos() {
        when(beneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of());

        int anioActual = Year.now().getValue();
        Instant enElAnio = YearMonth.of(anioActual, 3).atDay(15).atStartOfDay(ZoneId.systemDefault()).toInstant();
        when(historialBeneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of(
                usoDePromocion("socio-1", "Juan", "beneficio-1", "15% en medicamentos",
                        EstadoUsoBeneficio.USADO, enElAnio),
                usoDePromocion("socio-2", "Laura", "beneficio-1", "15% en medicamentos",
                        EstadoUsoBeneficio.USADO, enElAnio)));

        EstadisticasComercioResponse respuesta = service.obtenerEstadisticas("comercio-1", anioActual);

        List<UsoMensualResponse> usosMensuales = respuesta.usosMensuales();
        assertThat(usosMensuales).hasSize(12);
        assertThat(usosMensuales.get(2).mes()).isEqualTo("Mar");
        assertThat(usosMensuales.get(2).periodo()).isEqualTo(YearMonth.of(anioActual, 3).toString());
        assertThat(usosMensuales.get(2).cantidad()).isEqualTo(2L);
        assertThat(usosMensuales.stream().filter(u -> !u.mes().equals("Mar")).allMatch(u -> u.cantidad() == 0L))
                .isTrue();
    }

    @Test
    void obtenerEstadisticas_usosPorPromocionSoloIncluyeLasUsadasEsteMesOrdenadasDeMayorAMenor() {
        when(beneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of());

        Instant ahora = Instant.now();
        Instant elMesPasado = ahora.minusSeconds(86_400L * 40);
        when(historialBeneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of(
                usoDePromocion("socio-1", "Juan", "beneficio-1", "15% en medicamentos",
                        EstadoUsoBeneficio.USADO, ahora),
                usoDePromocion("socio-2", "Laura", "beneficio-1", "15% en medicamentos",
                        EstadoUsoBeneficio.USADO, ahora),
                usoDePromocion("socio-3", "Ana", "beneficio-2", "2x1 en menú",
                        EstadoUsoBeneficio.USADO, ahora),
                // Este uso es de un beneficio distinto y del mes pasado: no debería aparecer en el conteo de este mes.
                usoDePromocion("socio-4", "Carlos", "beneficio-3", "Sin usos este mes",
                        EstadoUsoBeneficio.USADO, elMesPasado)));

        EstadisticasComercioResponse respuesta = service.obtenerEstadisticas("comercio-1", Year.now().getValue());

        assertThat(respuesta.usosPorPromocion()).hasSize(2);
        assertThat(respuesta.usosPorPromocion().get(0).beneficioTitulo()).isEqualTo("15% en medicamentos");
        assertThat(respuesta.usosPorPromocion().get(0).cantidad()).isEqualTo(2L);
        assertThat(respuesta.usosPorPromocion().get(1).beneficioTitulo()).isEqualTo("2x1 en menú");
        assertThat(respuesta.usosPorPromocion().stream().noneMatch(u -> u.beneficioTitulo().equals("Sin usos este mes")))
                .isTrue();
    }

    @Test
    void obtenerEstadisticas_consumosRecientesOrdenaPorFechaDescendenteYLimitaA10() {
        when(beneficioRepository.findByComercioId("comercio-1")).thenReturn(List.of());

        Instant ahora = Instant.now();
        List<HistorialBeneficio> usos = new java.util.ArrayList<>();
        for (int i = 0; i < 12; i++) {
            usos.add(usoDePromocion("socio-" + i, "Socio " + i, "beneficio-1", "15% en medicamentos",
                    EstadoUsoBeneficio.USADO, ahora.minusSeconds(i * 3600L)));
        }
        when(historialBeneficioRepository.findByComercioId("comercio-1")).thenReturn(usos);

        EstadisticasComercioResponse respuesta = service.obtenerEstadisticas("comercio-1", Year.now().getValue());

        assertThat(respuesta.consumosRecientes()).hasSize(10);
        assertThat(respuesta.consumosRecientes().get(0).socioNombre()).isEqualTo("Socio 0"); // el más reciente primero
        assertThat(respuesta.consumosRecientes().get(9).socioNombre()).isEqualTo("Socio 9");
    }
}
