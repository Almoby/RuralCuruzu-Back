package com.almoby.ruralcuruzu.enums;

/**
 * De dónde sale el archivo que hay detrás de un {@link com.almoby.ruralcuruzu.domain.Comprobante}
 * (documento, sección 10.4).
 */
public enum OrigenComprobante {

    /** El propio socio lo adjuntó al informar un pago por transferencia (CuotaService.informarPago). */
    SUBIDO_POR_SOCIO,

    /**
     * El sistema lo generó (constancia en PDF con los datos del pago) porque no había
     * ningún archivo real: pago registrado por un admin en ventanilla, o pagado por
     * Mercado Pago. Se genera una sola vez, la primera vez que se pide la descarga, y
     * queda persistido para las siguientes.
     */
    GENERADO_POR_SISTEMA
}
