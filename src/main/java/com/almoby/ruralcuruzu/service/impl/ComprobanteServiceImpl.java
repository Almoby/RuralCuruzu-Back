package com.almoby.ruralcuruzu.service.impl;

import java.time.Instant;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.almoby.ruralcuruzu.domain.Comprobante;
import com.almoby.ruralcuruzu.domain.Pago;
import com.almoby.ruralcuruzu.dto.response.PagoResponse;
import com.almoby.ruralcuruzu.enums.EstadoPago;
import com.almoby.ruralcuruzu.enums.OrigenComprobante;
import com.almoby.ruralcuruzu.repository.ComprobanteRepository;
import com.almoby.ruralcuruzu.service.AlmacenamientoComprobantesService;
import com.almoby.ruralcuruzu.service.ComprobantePagoPdfService;
import com.almoby.ruralcuruzu.service.ComprobanteService;

import lombok.extern.slf4j.Slf4j;

/** Ver ComprobanteService. */
@Slf4j
@Service
public class ComprobanteServiceImpl implements ComprobanteService {

    private final ComprobanteRepository comprobanteRepository;
    private final AlmacenamientoComprobantesService almacenamientoComprobantesService;
    private final ComprobantePagoPdfService comprobantePagoPdfService;

    public ComprobanteServiceImpl(ComprobanteRepository comprobanteRepository,
                                   AlmacenamientoComprobantesService almacenamientoComprobantesService,
                                   ComprobantePagoPdfService comprobantePagoPdfService) {
        this.comprobanteRepository = comprobanteRepository;
        this.almacenamientoComprobantesService = almacenamientoComprobantesService;
        this.comprobantePagoPdfService = comprobantePagoPdfService;
    }

    @Override
    public Comprobante registrarSubidoPorSocio(Pago pago, String ruta, MultipartFile archivoOriginal) {
        Comprobante comprobante = Comprobante.builder()
                .pagoId(pago.getId())
                .cuotaId(pago.getCuotaId())
                .socioId(pago.getSocioId())
                .origen(OrigenComprobante.SUBIDO_POR_SOCIO)
                .nombreArchivo(archivoOriginal.getOriginalFilename())
                .ruta(ruta)
                .contentType(archivoOriginal.getContentType())
                .tamanioBytes(archivoOriginal.getSize())
                .fechaCreacion(Instant.now())
                .build();
        comprobanteRepository.save(comprobante);
        log.info("Comprobante registrado (subido por socio) pagoId={}", pago.getId());
        return comprobante;
    }

    @Override
    public Optional<Comprobante> obtenerOGenerarParaPago(PagoResponse pago, String socioId) {
        Optional<Comprobante> existente = comprobanteRepository.findByPagoId(pago.id());
        if (existente.isPresent()) {
            return existente;
        }

        // Dato legacy: pagos que ya estaban en Mongo antes de que Comprobante
        // existiera como entidad propia, con el archivo referenciado directo en
        // Pago.comprobanteRuta. Se migra de forma perezosa la primera vez que se pide.
        // OJO: Pago.comprobanteRuta se usa con dos significados distintos según el
        // canal (documento 10.4) - solo es una ruta de archivo real cuando el pago
        // lo informó el propio socio (informarPago, transferencia); si lo cargó un
        // admin (registrarPago), ese mismo campo guarda una nota de texto libre
        // (RegistrarPagoCuotaRequest.comprobante(), ej. "Recibo #123"), no un
        // archivo. Por eso acá no alcanza con mirar si comprobanteRuta() no es
        // null: hay que confirmar además que sea genuinamente informadoPorSocio,
        // si no, un pago de ventanilla con esa nota terminaría con un Comprobante
        // "roto" apuntando a un archivo que no existe en disco.
        if (pago.informadoPorSocio() && pago.comprobanteRuta() != null) {
            Comprobante comprobante = Comprobante.builder()
                    .pagoId(pago.id())
                    .cuotaId(pago.cuotaId())
                    .socioId(socioId)
                    .origen(OrigenComprobante.SUBIDO_POR_SOCIO)
                    .nombreArchivo(nombreSinPrefijoUuid(pago.comprobanteRuta()))
                    .ruta(pago.comprobanteRuta())
                    .contentType(null)
                    .tamanioBytes(0)
                    .fechaCreacion(pago.fechaCreacion() != null ? pago.fechaCreacion() : Instant.now())
                    .build();
            return Optional.of(guardarEvitandoDuplicado(comprobante, pago.id(),
                    "migrado desde el dato legacy Pago.comprobanteRuta"));
        }

        if (pago.estado() != EstadoPago.APROBADO) {
            return Optional.empty();
        }

        // Sin archivo real y sin dato legacy: se genera la constancia en PDF una
        // sola vez y queda persistida en disco para las próximas descargas.
        byte[] pdf = comprobantePagoPdfService.generarConstancia(pago);
        String nombreArchivo = "constancia-pago-" + pago.id() + ".pdf";
        String ruta = almacenamientoComprobantesService.guardarBytes(pago.id(), nombreArchivo, pdf);

        Comprobante comprobante = Comprobante.builder()
                .pagoId(pago.id())
                .cuotaId(pago.cuotaId())
                .socioId(socioId)
                .origen(OrigenComprobante.GENERADO_POR_SISTEMA)
                .nombreArchivo(nombreArchivo)
                .ruta(ruta)
                .contentType("application/pdf")
                .tamanioBytes(pdf.length)
                .fechaCreacion(Instant.now())
                .build();
        return Optional.of(guardarEvitandoDuplicado(comprobante, pago.id(), "generado (constancia PDF)"));
    }

    /**
     * Guarda un Comprobante recién armado, salvo que otra request paralela para el
     * mismo pago ya haya ganado la carrera (el índice único de pagoId lo rechazaría
     * con DuplicateKeyException): en ese caso, en vez de fallar, se devuelve el que
     * esa otra request ya persistió, para que la descarga funcione igual.
     */
    private Comprobante guardarEvitandoDuplicado(Comprobante comprobante, String pagoId, String descripcionLog) {
        try {
            comprobanteRepository.save(comprobante);
            log.info("Comprobante {} pagoId={}", descripcionLog, pagoId);
            return comprobante;
        } catch (DuplicateKeyException ex) {
            log.warn("Comprobante para pagoId={} ya había sido creado por otra request en paralelo", pagoId);
            return comprobanteRepository.findByPagoId(pagoId).orElseThrow(() -> ex);
        }
    }

    /** Le saca el prefijo UUID_ (y cualquier subcarpeta) con el que se guarda el archivo en disco. */
    private String nombreSinPrefijoUuid(String rutaOArchivo) {
        if (rutaOArchivo == null) {
            return null;
        }
        String soloNombre = rutaOArchivo.contains("/")
                ? rutaOArchivo.substring(rutaOArchivo.lastIndexOf('/') + 1)
                : rutaOArchivo;
        return soloNombre.replaceFirst(
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}_", "");
    }
}
