# 🗄️ Diseño de Base de Datos: POS-ERP Edge-Native

Este documento define la estructura relacional, las reglas de integridad y el código SQL necesario para soportar el sistema POS con ERP.

## 1. Reglas de Negocio en la Base de Datos

* **Atomicidad de Venta:** Cada venta debe realizarse dentro de una transacción que actualice el stock, registre el movimiento de inventario y guarde el detalle de la venta. Si un paso falla, nada se guarda.
* **Trazabilidad (Kardex):** No se permite modificar la columna `stock` de un producto sin insertar un registro en `movimientos_inventario`.
* **Persistencia de Precios:** Los precios de venta y costos de compra se duplican en las tablas de detalle (`detalle_ventas`, `detalle_compras`) para que los reportes históricos no cambien si el precio del producto se actualiza hoy.
* **Integridad de Caja:** Para registrar una venta o un gasto, debe existir una sesión de caja (`turnos_caja`) con estado `ABIERTO`.
* **Seguridad:** El `rol_id` en la tabla `usuarios` define el acceso a los módulos (Ventas, Contabilidad, Inventario).

---

## 2. Esquema SQL (Cloudflare D1 / SQLite)

```sql
-- ==========================================
-- 1. SEGURIDAD Y ACCESOS
-- ==========================================

CREATE TABLE roles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL UNIQUE -- 'admin', 'ventas', 'contabilidad', 'inventario'
);

CREATE TABLE usuarios (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    rol_id INTEGER REFERENCES roles(id),
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 2. ENTIDADES MAESTRAS
-- ==========================================

CREATE TABLE clientes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nit TEXT UNIQUE NOT NULL, -- Por defecto '22.222.222'
    nombre TEXT NOT NULL,     -- Por defecto 'Consumidor Final'
    email TEXT,
    telefono TEXT,
    direccion TEXT,
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE proveedores (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nit TEXT UNIQUE NOT NULL,
    nombre TEXT NOT NULL,
    contacto TEXT,
    telefono TEXT
);

CREATE TABLE productos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo_barras TEXT UNIQUE NOT NULL,
    nombre TEXT NOT NULL,
    costo_compra REAL NOT NULL,      -- Valor actual de reposición
    precio_venta_std REAL NOT NULL,  -- Precio base para el POS
    stock_actual INTEGER DEFAULT 0,
    stock_minimo INTEGER DEFAULT 5,
    categoria TEXT,
    fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 3. CONTROL DE CAJA (FINANZAS POS)
-- ==========================================

CREATE TABLE turnos_caja (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id INTEGER REFERENCES usuarios(id),
    fecha_apertura DATETIME DEFAULT CURRENT_TIMESTAMP,
    fecha_cierre DATETIME,
    base_inicial REAL NOT NULL DEFAULT 0,
    monto_final_esperado REAL, -- Calculado: Base + Ventas - Gastos
    monto_final_real REAL,     -- Ingresado por el usuario al cerrar
    estado TEXT DEFAULT 'ABIERTO' -- 'ABIERTO', 'CERRADO'
);

CREATE TABLE gastos_caja (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    turno_id INTEGER REFERENCES turnos_caja(id),
    monto REAL NOT NULL,
    descripcion TEXT NOT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 4. MOVIMIENTOS E HISTORIAL (TRAZABILIDAD)
-- ==========================================

-- Historial de compras (Entradas de mercancía)
CREATE TABLE compras (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    proveedor_id INTEGER REFERENCES proveedores(id),
    usuario_id INTEGER REFERENCES usuarios(id),
    total_compra REAL NOT NULL,
    num_factura_proveedor TEXT,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE detalle_compras (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    compra_id INTEGER REFERENCES compras(id),
    producto_id INTEGER REFERENCES productos(id),
    cantidad INTEGER NOT NULL,
    costo_unitario REAL NOT NULL
);

-- Historial de ventas (Salidas de mercancía)
CREATE TABLE ventas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cliente_id INTEGER REFERENCES clientes(id),
    usuario_id INTEGER REFERENCES usuarios(id),
    turno_id INTEGER REFERENCES turnos_caja(id),
    total_bruto REAL NOT NULL,   -- Suma de subtotales
    descuento_global REAL DEFAULT 0,
    recargo_global REAL DEFAULT 0,
    total_neto REAL NOT NULL,    -- Valor final cobrado
    metodo_pago TEXT,            -- 'Efectivo', 'Tarjeta', 'Transferencia'
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE detalle_ventas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    venta_id INTEGER REFERENCES ventas(id),
    producto_id INTEGER REFERENCES productos(id),
    cantidad INTEGER NOT NULL,
    precio_aplicado REAL NOT NULL, -- Puede ser diferente al std por override
    costo_momento REAL NOT NULL,   -- Costo de compra para calcular utilidad real
    utilidad REAL NOT NULL         -- (precio_aplicado - costo_momento) * cantidad
);

-- KARDEX: Registro de auditoría de stock
CREATE TABLE movimientos_inventario (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    producto_id INTEGER REFERENCES productos(id),
    tipo_movimiento TEXT NOT NULL, -- 'ENTRADA_COMPRA', 'SALIDA_VENTA', 'AJUSTE', 'DEVOLUCION'
    cantidad INTEGER NOT NULL,
    referencia_id INTEGER,         -- ID de la venta o compra relacionada
    usuario_id INTEGER REFERENCES usuarios(id),
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 5. AUDITORÍA DE SISTEMA
-- ==========================================

CREATE TABLE logs_acciones (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id INTEGER REFERENCES usuarios(id),
    accion TEXT NOT NULL, -- 'LOGIN', 'CAMBIO_PRECIO', 'ELIMINACION'
    detalle TEXT,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP
);

```

---

## 3. Notas de Implementación para el Agente

1. **Cálculo de Ganancias:** La utilidad se calcula en el momento de la venta y se guarda en `detalle_ventas.utilidad`. Esto permite que el módulo de contabilidad genere informes instantáneos sin recargar el servidor haciendo cálculos complejos de promedios.
2. **Manejo de Ids:** Para el cliente estándar, la base de datos debe inicializarse con un `id=1` para "Consumidor Final" con NIT `22.222.222`.
3. **Seguridad en el Edge:** Al usar Rust, las consultas a estas tablas deben usar el binding de `env.d1`. El backend debe validar que el `turno_id` esté en estado 'ABIERTO' antes de permitir un INSERT en la tabla `ventas`.
4. **Trazabilidad de Precios:** Si un administrador cambia el precio estándar en la tabla `productos`, la tabla `detalle_ventas` mantiene el precio al que se vendió originalmente, garantizando que la contabilidad del pasado nunca se altere.