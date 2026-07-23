package com.almoby.ruralcuruzu.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.almoby.ruralcuruzu.enums.CategoriaSocio;
import com.almoby.ruralcuruzu.enums.EstadoSolicitud;
import com.almoby.ruralcuruzu.enums.TipoPersona;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Solicitud para ser socio (documento, secciones 4 y 5). Se crea cuando alguien
 * completa el formulario "Quiero ser socio"; en ese momento NO existe todavía
 * ningún {@link Usuario} habilitado para esa persona (5.4) — la solicitud es
 * un trámite aparte que un admin revisa y aprueba o rechaza.
 *
 * {@code email} y {@code documentos} están denormalizados a nivel raíz (aunque
 * el dato "real" vive en datosPersonaFisica o datosPersonaJuridica) únicamente
 * para poder indexarlos y validar duplicados sin importar el tipo de persona.
 */
@Document(collection = "solicitudes_socio")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudSocio {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("numero_solicitud")
    private String numeroSolicitud;

    @Field("categoria_solicitada")
    private CategoriaSocio categoriaSolicitada;

    @Field("tipo_persona")
    private TipoPersona tipoPersona;

    @Field("datos_persona_fisica")
    private DatosPersonaFisica datosPersonaFisica;

    @Field("datos_persona_juridica")
    private DatosPersonaJuridica datosPersonaJuridica;

    /**
     * Denormalizado desde el sub-documento correspondiente según tipoPersona.
     * Ver validaciones de duplicados en SolicitudSocioServiceImpl.
     */
    @Indexed
    @Field("email")
    private String email;

    /**
     * Todos los documentos identificatorios de esta solicitud, sin formatear:
     * persona física → [DNI, CUIL]; persona jurídica → [CUIT]. Se guardan los
     * dos en persona física porque, aunque en teoría el CUIL se deriva del DNI,
     * nada impide que alguien mande un DNI distinto con el mismo CUIL para
     * esquivar la validación de duplicados si solo se chequeara el DNI.
     */
    @Indexed
    @Field("documentos")
    private List<String> documentos;

    @Field("acepta_terminos_y_condiciones")
    private boolean aceptaTerminosYCondiciones;

    @Field("estado")
    private EstadoSolicitud estado;

    @Field("historial")
    @Builder.Default
    private List<CambioEstadoSolicitud> historial = new ArrayList<>();

    @Field("fecha_creacion")
    private Instant fechaCreacion;

    @Field("fecha_actualizacion")
    private Instant fechaActualizacion;

    /** Nombre para mostrar: nombre+apellido (física) o razón social (jurídica). */
    public String nombreParaMostrar() {
        if (tipoPersona == TipoPersona.FISICA && datosPersonaFisica != null) {
            return datosPersonaFisica.getApellido() + ", " + datosPersonaFisica.getNombre();
        }
        if (tipoPersona == TipoPersona.JURIDICA && datosPersonaJuridica != null) {
            return datosPersonaJuridica.getRazonSocial();
        }
        return "(sin datos)";
    }
}
