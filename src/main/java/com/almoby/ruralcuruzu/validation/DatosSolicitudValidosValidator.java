package com.almoby.ruralcuruzu.validation;

import com.almoby.ruralcuruzu.enums.TipoPersona;
import com.almoby.ruralcuruzu.dto.request.SolicitudSocioRequest;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DatosSolicitudValidosValidator
        implements ConstraintValidator<DatosSolicitudValidos, SolicitudSocioRequest> {

    @Override
    public boolean isValid(SolicitudSocioRequest request, ConstraintValidatorContext context) {
        if (request == null || request.tipoPersona() == null) {
            // @NotNull en tipoPersona ya reporta este caso; no duplicamos el error.
            return true;
        }

        boolean valido;
        String mensaje;

        if (request.tipoPersona() == TipoPersona.FISICA) {
            valido = request.datosPersonaFisica() != null && request.datosPersonaJuridica() == null;
            mensaje = request.datosPersonaFisica() == null
                    ? "Faltan los datos de persona física (datosPersonaFisica)"
                    : "No corresponde enviar datosPersonaJuridica cuando tipoPersona es FISICA";
        } else {
            valido = request.datosPersonaJuridica() != null && request.datosPersonaFisica() == null;
            mensaje = request.datosPersonaJuridica() == null
                    ? "Faltan los datos de persona jurídica (datosPersonaJuridica)"
                    : "No corresponde enviar datosPersonaFisica cuando tipoPersona es JURIDICA";
        }

        if (!valido) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(mensaje)
                    .addPropertyNode(request.tipoPersona() == TipoPersona.FISICA ? "datosPersonaFisica" : "datosPersonaJuridica")
                    .addConstraintViolation();
        }

        return valido;
    }
}
