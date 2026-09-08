package com.almoby.ruralcuruzu.validation;

import java.time.LocalDate;

import com.almoby.ruralcuruzu.enums.TipoPersona;

/**
 * Contrato mínimo que necesita {@link DatosSolicitudValidosValidator} para
 * validar que los datos de persona física/jurídica sean coherentes con
 * {@code tipoPersona}, sin atarse a un DTO concreto. Lo implementan tanto
 * SolicitudSocioRequest (documento, sección 5: formulario público) como
 * AltaManualSocioRequest (documento, sección 9.5: alta manual del admin),
 * que comparten exactamente la misma regla de negocio.
 */
public interface DatosPersonaRequestValidable {

    TipoPersona tipoPersona();

    String documento();

    LocalDate fechaNacimiento();

    String nombreResponsable();

    String dniResponsable();

    /**
     * A diferencia de documento/fechaNacimiento/nombreResponsable/dniResponsable
     * (exclusivos de un tipo de persona), cuit puede venir en los dos casos: es
     * obligatorio para JURIDICA (no tiene otro documento identificatorio) pero
     * opcional para FISICA (pedido del dueño del proyecto: hay socios/solicitantes
     * que solo tienen DNI, sin CUIT/CUIL registrado).
     */
    String cuit();
}
