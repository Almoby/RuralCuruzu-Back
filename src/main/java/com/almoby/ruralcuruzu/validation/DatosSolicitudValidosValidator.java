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

        context.disableDefaultConstraintViolation();
        boolean valido = true;

        if (request.tipoPersona() == TipoPersona.FISICA) {
            valido &= requerido(context, request.documento(), "documento", "El documento es obligatorio para persona física");
            valido &= requerido(context, request.fechaNacimiento() != null, "fechaNacimiento",
                    "La fecha de nacimiento es obligatoria para persona física");
            valido &= noCorresponde(context, request.nombreResponsable(), "nombreResponsable",
                    "No corresponde enviar nombreResponsable cuando tipoPersona es FISICA");
            valido &= noCorresponde(context, request.dniResponsable(), "dniResponsable",
                    "No corresponde enviar dniResponsable cuando tipoPersona es FISICA");
        } else {
            valido &= requerido(context, request.nombreResponsable(), "nombreResponsable",
                    "El nombre del responsable es obligatorio para persona jurídica");
            valido &= requerido(context, request.dniResponsable(), "dniResponsable",
                    "El DNI del responsable es obligatorio para persona jurídica");
            valido &= noCorresponde(context, request.documento(), "documento",
                    "No corresponde enviar documento cuando tipoPersona es JURIDICA");
            valido &= noCorresponde(context, request.fechaNacimiento() != null, "fechaNacimiento",
                    "No corresponde enviar fechaNacimiento cuando tipoPersona es JURIDICA");
        }

        return valido;
    }

    private boolean requerido(ConstraintValidatorContext context, String valor, String campo, String mensaje) {
        return requerido(context, valor != null && !valor.isBlank(), campo, mensaje);
    }

    private boolean requerido(ConstraintValidatorContext context, boolean presente, String campo, String mensaje) {
        if (!presente) {
            agregarError(context, campo, mensaje);
        }
        return presente;
    }

    private boolean noCorresponde(ConstraintValidatorContext context, String valor, String campo, String mensaje) {
        boolean ausente = valor == null || valor.isBlank();
        return noCorresponde(context, !ausente, campo, mensaje);
    }

    private boolean noCorresponde(ConstraintValidatorContext context, boolean presente, String campo, String mensaje) {
        if (presente) {
            agregarError(context, campo, mensaje);
            return false;
        }
        return true;
    }

    private void agregarError(ConstraintValidatorContext context, String campo, String mensaje) {
        context.buildConstraintViolationWithTemplate(mensaje)
                .addPropertyNode(campo)
                .addConstraintViolation();
    }
}
