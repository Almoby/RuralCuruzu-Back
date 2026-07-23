package com.almoby.ruralcuruzu.dto.request;

import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.TipoPersona;
import com.almoby.ruralcuruzu.validation.DatosSolicitudValidos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo del formulario de "Solicitud para ser socio" (documento, sección 5).
 * Exactamente uno de {@code datosPersonaFisica} / {@code datosPersonaJuridica}
 * debe venir completo, según {@code tipoPersona} (ver {@link DatosSolicitudValidos}).
 */
@DatosSolicitudValidos
public record SolicitudSocioRequest(

        @Schema(description = "Categoría de asociación solicitada")
        @NotNull(message = "La categoría solicitada es obligatoria")
        CategoriaSocio categoriaSolicitada,

        @Schema(description = "Si el solicitante es persona física o jurídica")
        @NotNull(message = "El tipo de persona es obligatorio")
        TipoPersona tipoPersona,

        @Valid
        DatosPersonaFisicaRequest datosPersonaFisica,

        @Valid
        DatosPersonaJuridicaRequest datosPersonaJuridica,

        @Schema(description = "El solicitante debe aceptar los términos y condiciones para poder enviar la solicitud")
        @AssertTrue(message = "Debés aceptar los términos y condiciones para continuar")
        boolean aceptaTerminosYCondiciones

) {
}
