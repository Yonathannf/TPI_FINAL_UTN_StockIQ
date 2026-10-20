package com.stockiq.catalogo.dto;

/** Versión resumida de una categoría o un proveedor dentro de la respuesta de un producto. */
public record ReferenciaResponse(Long id, String nombre, boolean activo) {
}
