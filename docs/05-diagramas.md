# StockIQ — Diagramas

Todos los diagramas están en Mermaid, que GitHub renderiza directamente en los archivos `.md`.

## 1. Diagrama Entidad-Relación

```mermaid
erDiagram
    CATEGORIA ||--o{ PRODUCTO : "clasifica"
    PROVEEDOR ||--o{ PRODUCTO : "abastece"
    PRODUCTO  ||--o{ MOVIMIENTO : "es movido en"
    USUARIO   ||--o{ MOVIMIENTO : "registra"
    USUARIO   |o--o{ MOVIMIENTO : "resuelve transferencia"
    DEPOSITO  |o--o{ MOVIMIENTO : "origen"
    DEPOSITO  |o--o{ MOVIMIENTO : "destino"

    CATEGORIA {
        bigint id PK
        varchar nombre UK
        varchar descripcion
        boolean activo
    }
    PROVEEDOR {
        bigint id PK
        varchar nombre UK
        varchar contacto
        varchar telefono
        varchar email
        int lead_time_dias
        boolean activo
    }
    PRODUCTO {
        bigint id PK
        varchar codigo UK
        varchar nombre
        bigint categoria_id FK
        bigint proveedor_id FK
        varchar unidad_medida
        decimal precio_referencia
        decimal stock_seguridad
        boolean fraccionable
        boolean activo
    }
    DEPOSITO {
        bigint id PK
        varchar nombre UK
        varchar ubicacion
        boolean activo
    }
    USUARIO {
        bigint id PK
        varchar username UK
        varchar password_hash
        varchar nombre
        varchar apellido
        enum rol
        boolean activo
    }
    MOVIMIENTO {
        bigint id PK
        enum tipo
        bigint producto_id FK
        decimal cantidad
        datetime fecha
        varchar motivo
        bigint usuario_id FK
        bigint deposito_origen_id FK
        bigint deposito_destino_id FK
        enum estado_transferencia
        datetime fecha_resolucion
        bigint usuario_resolucion_id FK
    }
    PARAMETRO {
        varchar clave PK
        varchar valor
        varchar descripcion
    }
```

## 2. Casos de uso

```mermaid
flowchart LR
    ENC([Encargado])
    OPE([Operario])

    subgraph SIS["Sistema StockIQ"]
        UC1(Iniciar sesión)
        UC2(Registrar entrada)
        UC3(Registrar salida)
        UC4(Registrar transferencia)
        UC5("Confirmar o cancelar transferencia")
        UC6(Consultar stock)
        UC7(Consultar historial de movimientos)
        UC8("Gestionar productos, categorías y proveedores")
        UC9(Gestionar depósitos)
        UC10(Gestionar usuarios)
        UC11(Configurar parámetros)
        UC12(Consultar alertas de reposición)
        UC13(Consultar ABC y rotación)
    end

    OPE --- UC1
    OPE --- UC2
    OPE --- UC3
    OPE --- UC4
    OPE --- UC5
    OPE --- UC6
    OPE --- UC7

    ENC --- UC1
    ENC --- UC2
    ENC --- UC3
    ENC --- UC4
    ENC --- UC5
    ENC --- UC6
    ENC --- UC7
    ENC --- UC8
    ENC --- UC9
    ENC --- UC10
    ENC --- UC11
    ENC --- UC12
    ENC --- UC13
```

## 3. Secuencia: transferencia entre depósitos

```mermaid
sequenceDiagram
    actor U as Usuario
    participant FE as Frontend
    participant C as MovimientoController
    participant S as MovimientoService
    participant ST as StockService
    participant R as MovimientoRepository

    U->>FE: Completa formulario de transferencia
    FE->>C: POST /movimientos/transferencias
    C->>S: crearTransferencia(dto)
    S->>S: Valida origen ≠ destino, cantidad > 0, producto y depósitos activos
    S->>R: Bloquea el producto (SELECT ... FOR UPDATE)
    S->>ST: stockDisponible(producto, origen)
    ST-->>S: stock
    alt stock insuficiente
        S-->>C: StockInsuficienteException
        C-->>FE: 409 Conflict
    else stock suficiente
        S->>R: save(Movimiento PENDIENTE)
        R-->>S: movimiento
        S-->>C: MovimientoResponse
        C-->>FE: 201 Created
    end

    Note over U,R: Más tarde, en el depósito destino

    U->>FE: Confirma la recepción
    FE->>C: PATCH /movimientos/{id}/confirmar
    C->>S: confirmar(id, usuario)
    S->>R: findById(id) con bloqueo (FOR UPDATE)
    R-->>S: movimiento
    alt no está PENDIENTE
        S-->>C: EstadoInvalidoException
        C-->>FE: 409 Conflict
    else está PENDIENTE
        S->>R: save(estado CONFIRMADA, fecha y usuario de resolución)
        S-->>C: MovimientoResponse
        C-->>FE: 200 OK
    end

    Note over U,R: La cancelación (PATCH /movimientos/{id}/cancelar) sigue el mismo flujo y deja el estado CANCELADA
```

## 4. Estados de una transferencia

```mermaid
stateDiagram-v2
    [*] --> PENDIENTE: Se registra (descuenta del origen, en tránsito)
    PENDIENTE --> CONFIRMADA: Se confirma la recepción (suma al destino)
    PENDIENTE --> CANCELADA: Se cancela (vuelve al origen)
    CONFIRMADA --> [*]
    CANCELADA --> [*]
```

## 5. Arquitectura

```mermaid
flowchart LR
    subgraph Cliente
        FE[Frontend React + Vite<br/>Vercel]
    end

    subgraph Backend [Backend Spring Boot - Render]
        SEC[Spring Security + JWT]
        CTRL[Controllers]
        SRV[Services]
        REP[Repositories JPA]
        SEC --> CTRL --> SRV --> REP
    end

    DB[(MySQL 8<br/>Railway)]

    FE -- "HTTPS / REST + JWT" --> SEC
    REP -- JDBC --> DB
```

## 6. Diagrama de clases (dominio)

```mermaid
classDiagram
    class Categoria {
        Long id
        String nombre
        String descripcion
        boolean activo
    }
    class Proveedor {
        Long id
        String nombre
        String contacto
        String telefono
        String email
        int leadTimeDias
        boolean activo
    }
    class Producto {
        Long id
        String codigo
        String nombre
        String unidadMedida
        BigDecimal precioReferencia
        BigDecimal stockSeguridad
        boolean fraccionable
        boolean activo
    }
    class Deposito {
        Long id
        String nombre
        String ubicacion
        boolean activo
    }
    class Usuario {
        Long id
        String username
        String passwordHash
        String nombre
        String apellido
        Rol rol
        boolean activo
    }
    class Movimiento {
        Long id
        TipoMovimiento tipo
        BigDecimal cantidad
        LocalDateTime fecha
        String motivo
        EstadoTransferencia estadoTransferencia
        LocalDateTime fechaResolucion
    }
    class Parametro {
        String clave
        String valor
        String descripcion
    }

    class Rol {
        <<enumeration>>
        ENCARGADO
        OPERARIO
    }
    class TipoMovimiento {
        <<enumeration>>
        ENTRADA
        SALIDA
        TRANSFERENCIA
    }
    class EstadoTransferencia {
        <<enumeration>>
        PENDIENTE
        CONFIRMADA
        CANCELADA
    }

    Categoria "1" --> "*" Producto
    Proveedor "1" --> "*" Producto
    Producto "1" --> "*" Movimiento
    Usuario "1" --> "*" Movimiento : registra
    Usuario "0..1" --> "*" Movimiento : resuelve
    Deposito "0..1" --> "*" Movimiento : origen
    Deposito "0..1" --> "*" Movimiento : destino
    Usuario --> Rol
    Movimiento --> TipoMovimiento
    Movimiento --> EstadoTransferencia
```
