package com.almoby.ruralcuruzu.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.almoby.ruralcuruzu.dto.request.AnularCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.InformarPagoCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.RegistrarPagoCuotaRequest;
import com.almoby.ruralcuruzu.dto.request.RevisarPagoInformadoRequest;
import com.almoby.ruralcuruzu.dto.response.CuotaResponse;
import com.almoby.ruralcuruzu.dto.response.CuotaResumenResponse;
import com.almoby.ruralcuruzu.dto.response.EstadoCuentaSocioResponse;
import com.almoby.ruralcuruzu.dto.response.GeneracionCuotasResponse;
import com.almoby.ruralcuruzu.dto.response.InformarPagoResponse;
import com.almoby.ruralcuruzu.dto.response.LinkDePagoResponse;
import com.almoby.ruralcuruzu.dto.response.PagoResponse;
import com.almoby.ruralcuruzu.dto.response.RegistrarPagoResponse;
import com.almoby.ruralcuruzu.dto.response.ResumenCuotasResponse;
import com.almoby.ruralcuruzu.dto.response.RevisarPagoInformadoResponse;
import com.almoby.ruralcuruzu.enums.EstadoCuota;

/** Ver documento, sección 10 ("Gestión de cuotas"). */
public interface CuotaService {

    /**
     * Genera las cuotas del período indicado (o el mes actual si es null) para
     * todos los socios ACTIVO, tanto si la dispara el cron mensual como si la
     * dispara un admin manualmente (documento 10.2).
     */
    GeneracionCuotasResponse generarCuotas(String periodo, String adminId, String adminNombre);

    /** Historial de corridas de generación (documento 10.2, paso 8), más recientes primero. */
    List<GeneracionCuotasResponse> listarEjecuciones();

    List<CuotaResumenResponse> listarCuotas(EstadoCuota estado, String socioId, String periodo);

    CuotaResponse obtenerCuotaPorId(String id);

    /** Registro manual de un pago hecho por el admin (documento 10.4). */
    RegistrarPagoResponse registrarPago(RegistrarPagoCuotaRequest request, String adminId, String adminNombre);

    /**
     * El socio informa (autoservicio) que pagó una cuota propia por
     * transferencia; el comprobante es obligatorio (documento, sección 10.4).
     */
    InformarPagoResponse informarPago(String cuotaId, InformarPagoCuotaRequest request,
                                       MultipartFile comprobante, String socioId);

    /** El admin aprueba o rechaza un pago informado por un socio (estado EN_REVISION). */
    RevisarPagoInformadoResponse revisarPagoInformado(String cuotaId, RevisarPagoInformadoRequest request,
                                                       String adminId, String adminNombre);

    CuotaResponse anularCuota(String id, AnularCuotaRequest request, String adminId, String adminNombre);

    EstadoCuentaSocioResponse obtenerEstadoCuentaSocio(String socioId);

    List<CuotaResumenResponse> listarCuotasDeSocio(String socioId);

    /**
     * Historial completo de pagos de un socio (RN-17: el pago es su propia
     * entidad), más recientes primero. A diferencia de listarCuotasDeSocio,
     * acá aparecen TODOS los intentos, incluyendo los rechazados.
     */
    List<PagoResponse> listarPagosDeSocio(String socioId);

    /** Totales para las tarjetas y pestañas del panel de Gestión de Cuotas (Figma). */
    ResumenCuotasResponse obtenerResumen();

    /**
     * El socio genera un link de pago (Mercado Pago) para una cuota propia
     * (documento 10.4, canal "link de pago"). Crea un Pago EN_REVISION
     * asociado a una preferencia de Mercado Pago; la cuota queda pendiente de
     * que llegue la notificación del webhook con el resultado real.
     */
    LinkDePagoResponse generarLinkDePago(String cuotaId, String socioId);

    /**
     * Procesa la notificación del webhook de Mercado Pago para un pago dado.
     * Nunca confía en el contenido del webhook por sí solo: siempre reconsulta
     * el estado real contra la API de Mercado Pago antes de actualizar nada
     * (ver {@link MercadoPagoService#consultarPago}). Es idempotente: si el
     * Pago ya estaba resuelto (APROBADO o RECHAZADO), una notificación repetida
     * no hace nada.
     */
    void procesarNotificacionMercadoPago(String mercadoPagoPaymentId);
}
