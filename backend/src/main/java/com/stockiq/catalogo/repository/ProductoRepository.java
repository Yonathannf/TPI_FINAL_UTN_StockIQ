package com.stockiq.catalogo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stockiq.catalogo.model.Producto;

import jakarta.persistence.LockModeType;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /**
     * Listado con filtros opcionales (null = sin filtro). Trae categoría y proveedor
     * en la misma consulta para no hacer una consulta extra por producto.
     * {@code texto} llega en minúsculas y con los comodines ya agregados (%texto%).
     */
    @Query("""
            select p from Producto p
              join fetch p.categoria
              join fetch p.proveedor
             where (:categoriaId is null or p.categoria.id = :categoriaId)
               and (:proveedorId is null or p.proveedor.id = :proveedorId)
               and (:activo is null or p.activo = :activo)
               and (:texto is null or lower(p.codigo) like :texto or lower(p.nombre) like :texto)
             order by p.nombre
            """)
    List<Producto> buscar(@Param("categoriaId") Long categoriaId,
                          @Param("proveedorId") Long proveedorId,
                          @Param("activo") Boolean activo,
                          @Param("texto") String texto);

    @Query("select p from Producto p join fetch p.categoria join fetch p.proveedor where p.id = :id")
    Optional<Producto> buscarConRelaciones(@Param("id") Long id);

    /**
     * Bloquea la fila del producto (SELECT ... FOR UPDATE). Se usa antes de validar
     * reglas que dependen del stock (RN-39, RN-47), igual que lo hará el registro de
     * movimientos (D-13), para que ambas operaciones no se crucen.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p join fetch p.categoria join fetch p.proveedor where p.id = :id")
    Optional<Producto> bloquearPorId(@Param("id") Long id);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsByProveedorId(Long proveedorId);
}
