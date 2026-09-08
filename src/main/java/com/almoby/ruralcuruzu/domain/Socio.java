package com.almoby.ruralcuruzu.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoSocio;
import com.almoby.ruralcuruzu.enums.PeriodicidadPago;
import com.almoby.ruralcuruzu.enums.TipoPersona;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



/**
 * Perfil de un Socio ya aprobado (documento, sección 8.4). Se crea al aprobar
 * una {@link SolicitudSocio}: los datos personales se COPIAN a este documento
 * (no queda como una referencia viva a la solicitud), para que la solicitud
 * original siga siendo un registro histórico inmutable de lo que se pidió en
 * su momento, mientras que el Socio puede editarse después sin tocarla.
 */
@Document(collection = "socios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Socio {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("numero_socio")
    private String numeroSocio;

    @Field("categoria")
    private CategoriaSocio categoria;

    /**
     * Con qué frecuencia paga este socio (pedido del dueño del proyecto: no
     * todos pagan mes a mes). MENSUAL por defecto: es el comportamiento de
     * siempre, así que los socios ya existentes (sin este dato en Mongo)
     * quedan como MENSUAL al deserializarse (ver @Builder.Default).
     */
    @Field("periodicidad_pago")
    @Builder.Default
    private PeriodicidadPago periodicidadPago = PeriodicidadPago.MENSUAL;

    @Field("tipo_persona")
    private TipoPersona tipoPersona;

    @Field("datos_persona_fisica")
    private DatosPersonaFisica datosPersonaFisica;

    @Field("datos_persona_juridica")
    private DatosPersonaJuridica datosPersonaJuridica;

    @Field("estado")
    private EstadoSocio estado;

    /** Id del Usuario con el que este socio inicia sesión. */
    @Indexed(unique = true, sparse = true)
    @Field("usuario_id")
    private String usuarioId;

    /** Número de la solicitud aprobada que originó este socio (solo trazabilidad). */
    @Field("numero_solicitud_origen")
    private String numeroSolicitudOrigen;

    @Field("admin_responsable_alta_id")
    private String adminResponsableAltaId;

    @Field("admin_responsable_alta_nombre")
    private String adminResponsableAltaNombre;

    @Field("fecha_alta")
    private Instant fechaAlta;

    @Field("fecha_actualizacion")
    private Instant fechaActualizacion;

    /** Nombre para mostrar: apellido y nombre (física) o razón social (jurídica). */
    public String nombreParaMostrar() {
        if (tipoPersona == TipoPersona.FISICA && datosPersonaFisica != null) {
            return datosPersonaFisica.getApellidoYNombre();
        }
        if (tipoPersona == TipoPersona.JURIDICA && datosPersonaJuridica != null) {
            return datosPersonaJuridica.getRazonSocial();
        }
        return "(sin datos)";
    }

    public boolean estaActivo() {
        return estado == EstadoSocio.ACTIVO;
    }

    /**
     * Periodicidad real a usar, tolerante a los socios que ya existían en
     * Mongo antes de este campo (quedan con periodicidadPago = null al
     * deserializarse, el @Builder.Default de arriba no los alcanza: solo
     * aplica a objetos nuevos construidos con el builder). Se trata a un null
     * como MENSUAL, el comportamiento de siempre.
     */
    public PeriodicidadPago periodicidadEfectiva() {
        return periodicidadPago != null ? periodicidadPago : PeriodicidadPago.MENSUAL;
    }

    /** Correo de contacto según el tipo de persona. */
    public String obtenerEmail() {
        if (tipoPersona == TipoPersona.FISICA && datosPersonaFisica != null) {
            return datosPersonaFisica.getCorreoElectronico();
        }
        if (tipoPersona == TipoPersona.JURIDICA && datosPersonaJuridica != null) {
            return datosPersonaJuridica.getCorreoElectronico();
        }
        return null;
    }
}
