# PRD: Órdenes de Compra e Ingresos de Inventario

> **Estado: PLANIFICADO** (no implementado) — 2026-08-05. La migración de este PRD es `0008_purchase_orders.sql` (la `0007` la ocupa `shifts_caja` del PRD-TURNOS-Y-CAJA.md). Depende del CHECK de `numbering_ranges` (ver §3.4: SQLite no permite ALTER CHECK → validar `kind` en código, no en BD).

## 1. Executive Summary

**Problem**: El sistema actual solo gestiona la cadena de ventas (cotización → factura → stock salida). No existe trazabilidad de compras: no se registran proveedores, no se asocian facturas de compra a entradas de inventario, y no hay documento formal de orden de compra para enviar a proveedores.

**Solution**: Implementar un módulo completo de órdenes de compra con CRUD de proveedores, generación de documento PDF (mismo pipeline que cotizaciones/facturas), numeración propia, y actualización automática de inventario al aprobar.

**Success Criteria**:
- 100% de órdenes de compra generadas con documento PDF descargable
- Stock actualizado automáticamente al transicionar a estado "aprobada" (sin paso manual)
- Trazabilidad completa: orden → factura de compra → movimiento de stock (referencia cruzada)
- Tiempo de creación de una orden de compra < 30 segundos (reutilizando productos existentes del catálogo)

---

## 2. User Experience & Functionality

### User Personas

| Persona | Rol | Necesidad |
|---------|-----|-----------|
| **Compras** | admin | Crear órdenes, seleccionar proveedor, aprobar, descargar PDF para enviar |
| **Bodega** | admin/usuario | Ver órdenes aprobadas, verificar entradas de stock automáticas |
| **Contabilidad** | admin | Consultar libro de compras, conciliar facturas de proveedores |

### User Stories

#### Proveedores

- **PO-1**: Como admin, quiero crear/editar/eliminar proveedores (CRUD) con NIT, nombre, contacto, dirección, email, teléfono.
- **PO-2**: Como admin, quiero listar proveedores activos con búsqueda por nombre/NIT para seleccionarlos al crear una orden.

#### Órdenes de Compra

- **PO-3**: Como admin, quiero crear una orden de compra en estado "borrador" seleccionando proveedor y añadiendo líneas de productos (del catálogo existente o con datos manuales).
- **PO-4**: Como admin, quiero aprobar una orden de compra para que: (a) se genere el movimiento de stock entrada atómico, (b) se asigne folio OC-XXXX definitivo, (c) se genere el documento PDF.
- **PO-5**: Como admin, quiero marcar una orden como "recibida" y asociar la factura de compra (referencia: número, fecha, monto).
- **PO-6**: Como admin/usuario, quiero ver el listado de órdenes con filtros (estado, proveedor, rango de fechas).
- **PO-7**: Como admin/usuario, quiero ver el detalle de una orden incluyendo: líneas, movimientos de stock generados, factura de compra asociada.
- **PO-8**: Como admin, quiero descargar el PDF de una orden de compra (mismo pipeline Browser Rendering que cotizaciones/facturas).
- **PO-9**: Como admin/usuario, quiero acceder a la vista pública de una orden vía `/oc/:token` (token generado al aprobar).

#### Ingresos de Inventario

- **PO-10**: Como sistema, debo generar un `stock_movements` de tipo `entrada` por cada línea de producto al aprobar la orden, con `reference_type='purchase_order'` y `reference_id` apuntando a la orden.
- **PO-11**: Como sistema, debo actualizar `products.stock_qty` sumando las cantidades recibidas de forma atómica (batch D1).

### Acceptance Criteria

| ID | Criterio |
|----|----------|
| PO-1 | Proveedor requiere: name (1-150 chars), nit (4-12 dígitos). email/phone/address opcionales. Soft-delete. 409 si NIT duplicado. |
| PO-2 | Búsqueda con `?q=` (nombre/NIT), filtro `?active=0/1`. Paginación 200 máx. |
| PO-3 | Orden en borrador sin folio definitivo (usa temporal). Líneas: product_id (opcional), description, quantity (>0), unit_price (≥0), tax_rate (0/5/19), discount_percent (0-100). |
| PO-4 | Transición borrador→aprobada: valida proveedor activo, al menos 1 línea, rango `orden_compra` activo/vigente. Genera batch: folio + stock movements + update stock_qty. |
| PO-5 | Transición aprobada→recibida: captura purchase_invoice_number (obligatorio), purchase_invoice_date, purchase_invoice_amount. No muta stock. |
| PO-6 | Listado: filtros `?status=` (borrador/aprobada/recibida/anulada), `?supplier_id=`, `?from=`, `?to=`. Ordenado por created_at DESC. |
| PO-7 | Detalle: orden completa + líneas + stock_movements asociados + datos factura compra + balance (total en letras). |
| PO-8 | PDF: mismo `env.BROWSER.quickAction('pdf', ...)`. Headers: `content-type: application/pdf`, `content-disposition: inline; filename="OC-0001.pdf"`. Fallback 501 sin BROWSER. |
| PO-9 | Vista pública `/oc/:token`: HTML renderizado, rate limit heredado de `/c/*`, headers `noindex, no-store, referrer-policy: no-referrer`. |
| PO-10 | Movement: kind='entrada', quantity=línea quantity, reason=`Orden de compra OC-XXXX`, reference_type='purchase_order', reference_id=orden.id, stock_after=qty_anterior + quantity. |
| PO-11 | Batch atómico: UPDATE products SET stock_qty = stock_qty + ? WHERE id = ?. Rollback si falla cualquiera. |

### Non-Goals (No en este PRD)

- Notas débito/crédito de compras (posterior a F5/F6)
- Multi-moneda en órdenes de compra (solo COP)
- Recepción parcial (mínimo v1.1; v1 es recepción total)
- Aprobación multi-nivel (flujo simple: una transición)
- Integración con API de proveedores o catálogo electrónico
- Retenciones en compras (se manejan en contabilidad F5)

---

## 3. Technical Specifications

### Architecture Overview

```
Admin crea OC (borrador)
    │
    ▼
POST /api/purchase-orders          → insert borrador (sin folio)
    │
    ▼
PATCH /api/purchase-orders/:id/approve  → transición a "aprobada"
    │                                       ├─ asigna folio OC-XXXX (rango propio)
    │                                       ├─ batch: stock_movements entrada + update stock_qty
    │                                       ├─ genera public_token (UUID v4)
    │                                       └─ responde orden completa
    │
    ▼
POST /api/purchase-orders/:id/receive    → transición a "recibida"
    │                                       └─ asocia factura de compra (número, fecha, monto)
    │
    ▼
GET /oc/:token                    → vista pública HTML
GET /oc/:token.pdf                → descarga PDF (Browser Rendering)
```

### Database Schema

#### Nueva tabla: `suppliers`

```sql
CREATE TABLE IF NOT EXISTS suppliers (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,                       -- razón social del proveedor
  nit TEXT NOT NULL UNIQUE,                 -- NIT (solo dígitos, 4-12)
  dv TEXT,                                  -- dígito de verificación
  address TEXT,
  phone TEXT,
  email TEXT,
  contact_name TEXT,                        -- persona de contacto
  active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0,1)),
  notes TEXT,
  created_by TEXT,
  created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
  updated_at TEXT,
  deleted_at TEXT                           -- soft-delete
);
CREATE INDEX IF NOT EXISTS idx_suppliers_nit ON suppliers (nit);
CREATE INDEX IF NOT EXISTS idx_suppliers_active ON suppliers (active);
```

#### Nueva tabla: `purchase_orders`

```sql
CREATE TABLE IF NOT EXISTS purchase_orders (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  folio TEXT UNIQUE,                        -- OC-0001 (NULL en borrador, asignado al aprobar)
  numbering_range_id INTEGER,               -- FK → numbering_ranges (kind='orden_compra')
  supplier_id INTEGER NOT NULL,             -- FK → suppliers
  status TEXT NOT NULL DEFAULT 'borrador'
    CHECK (status IN ('borrador','aprobada','recibida','anulada')),
  public_token TEXT UNIQUE,                 -- UUID v4 (generado al aprobar)
  expires_at TEXT,                          -- validez del documento público
  subtotal REAL NOT NULL DEFAULT 0,
  discount_items REAL NOT NULL DEFAULT 0,
  discount_global REAL NOT NULL DEFAULT 0,
  tax_total REAL NOT NULL DEFAULT 0,
  total REAL NOT NULL DEFAULT 0,
  total_letters TEXT,                       -- total en letras
  currency_code TEXT NOT NULL DEFAULT 'COP',
  notes TEXT,                               -- notas internas
  terms TEXT,                               -- condiciones de la orden
  -- Factura de compra asociada (referencia simple, sin validación DIAN)
  purchase_invoice_number TEXT,             -- número factura del proveedor
  purchase_invoice_date TEXT,               -- fecha factura (YYYY-MM-DD)
  purchase_invoice_amount REAL,             -- monto total factura
  created_by TEXT,
  approved_at TIMESTAMP,                    -- cuándo se aprobó
  received_at TIMESTAMP,                    -- cuándo se recibió
  created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
  updated_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
  FOREIGN KEY (numbering_range_id) REFERENCES numbering_ranges (id),
  FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_po_folio ON purchase_orders (folio);
CREATE INDEX IF NOT EXISTS idx_po_supplier ON purchase_orders (supplier_id);
CREATE INDEX IF NOT EXISTS idx_po_status ON purchase_orders (status);
CREATE INDEX IF NOT EXISTS idx_po_token ON purchase_orders (public_token);
CREATE INDEX IF NOT EXISTS idx_po_created ON purchase_orders (created_at);
```

#### Nueva tabla: `purchase_order_items`

```sql
CREATE TABLE IF NOT EXISTS purchase_order_items (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  purchase_order_id INTEGER NOT NULL,
  product_id INTEGER,                       -- FK → products (NULL si es línea manual)
  description TEXT NOT NULL,
  quantity REAL NOT NULL CHECK (quantity > 0),
  unit_price REAL NOT NULL DEFAULT 0,
  discount_percent REAL NOT NULL DEFAULT 0,
  tax_rate REAL NOT NULL DEFAULT 0,
  gross REAL NOT NULL DEFAULT 0,
  discount REAL NOT NULL DEFAULT 0,
  net REAL NOT NULL DEFAULT 0,
  tax REAL NOT NULL DEFAULT 0,
  line_total REAL NOT NULL DEFAULT 0,
  FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id) ON DELETE CASCADE,
  FOREIGN KEY (product_id) REFERENCES products (id)
);
CREATE INDEX IF NOT EXISTS idx_poi_order ON purchase_order_items (purchase_order_id);
```

#### Extensión `numbering_ranges`

Agregar `orden_compra` al CHECK constraint:

```sql
-- En migración nueva (0008):
ALTER TABLE purchase_orders ADD COLUMN ...  -- no se puede ALTER CHECK en SQLite
-- Alternativa: el CHECK se define en código, no en BD, o se usa kind TEXT sin CHECK
```

**Nota técnica**: SQLite no soporta ALTER TABLE ... DROP CONSTRAINT. El CHECK `kind IN ('factura','nota_credito')` existente **no se puede extender directamente**. Opciones:
- **Opción A (recomendada)**: Remover el CHECK de BD y validar en código (capa routes). Mantener `kind` como TEXT libre.
- **Opción B**: Crear nueva tabla `document_sequences` independiente para órdenes de compra.

**Decisión**: Opción A — el CHECK se relaja en una migración futura (documentar), y se valida en código.

### API Endpoints

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| GET | `/api/suppliers` | admin+usuario | Listado con búsqueda `?q=`, `?active=` |
| POST | `/api/suppliers` | admin | Crear proveedor |
| GET | `/api/suppliers/:id` | admin+usuario | Detalle |
| PUT/PATCH | `/api/suppliers/:id` | admin | Actualizar |
| DELETE | `/api/suppliers/:id` | admin | Soft-delete |
| GET | `/api/purchase-orders` | admin+usuario | Listado con filtros |
| POST | `/api/purchase-orders` | admin | Crear borrador |
| GET | `/api/purchase-orders/:id` | admin+uario | Detalle completo |
| PATCH | `/api/purchase-orders/:id` | admin | Editar borrador (solo estado borrador) |
| POST | `/api/purchase-orders/:id/approve` | admin | Aprobar (stock entrada + folio + token) |
| POST | `/api/purchase-orders/:id/receive` | admin | Marcar recibida + asociar factura |
| POST | `/api/purchase-orders/:id/cancel` | admin | Anular (solo borrador o aprobada sin factura) |
| GET | `/oc/:token` | público | Vista HTML pública |
| GET | `/oc/:token.pdf` | público | Descarga PDF |

### State Machine

```
borrador ──[approve]──► aprobada ──[receive]──► recibida
   │                       │
   └──[cancel]──► anulada  └──[cancel]──► anulada (solo si sin factura compra)
```

Transiciones inválidas:
- `aprobada → borrador` (no se puede revertir; usar cancel)
- `recibida → *` (estado terminal)
- `anulada → *` (estado terminal)

### Stock Movement on Approve

```typescript
// Al aprobar (batch atómico D1):
for (const item of items) {
  if (item.product_id) {
    batch.push(
      env.DB.prepare(
        `UPDATE products SET stock_qty = stock_qty + ?, updated_at = NOW
         WHERE id = ? AND deleted_at IS NULL`
      ).bind(item.quantity, item.product_id)
    )
    batch.push(
      env.DB.prepare(
        `INSERT INTO stock_movements
         (product_id, kind, quantity, reason, reference_type, reference_id, stock_after, created_by)
         SELECT ?, 'entrada', ?, ?, 'purchase_order', ?, stock_qty, ? FROM products WHERE id = ?`
      ).bind(item.product_id, item.quantity, `Orden de compra ${folio}`, orderId, userId, item.product_id)
    )
  }
}
```

### PDF Generation

Reutilizar `env.BROWSER.quickAction('pdf', { html, pdfOptions: { format: 'a4', printBackground: true } })`.

Template HTML: nuevo archivo `templates/orden-compra.html` con mismos placeholders adaptados:
- `{{folio}}` → OC-0001
- `{{supplier.name}}`, `{{supplier.nit}}`, `{{supplier.address}}`, `{{supplier.phone}}`
- `{{issued}}`, `{{expires}}`
- `{{lines}}` (mismo formato que cotización)
- `{{subtotal}}`, `{{tax_total}}`, `{{total}}`
- `{{terms}}`, `{{notes}}`

### Public Route

Nuevo prefijo `/oc/` para diferenciar de cotizaciones `/c/`:
- Token generado con `crypto.randomUUID()` al aprobar
- Rate limit heredado del worker (mismo de `/c/*`: 30 req/min por IP en producción)
- Headers de seguridad: `noindex, no-store, referrer-policy: no-referrer`

### Files to Create/Modify

| Archivo | Acción | Descripción |
|---------|--------|-------------|
| `facturador-backend/migrations/0008_purchase_orders.sql` | **Crear** | Tablas suppliers, purchase_orders, purchase_order_items + extender numbering_ranges (relajar CHECK de kind en código) |
| `facturador-backend/src/routes/suppliers.ts` | **Crear** | CRUD proveedores |
| `facturador-backend/src/routes/purchase-orders.ts` | **Crear** | CRUD órdenes + approve/receive/cancel |
| `facturador-backend/src/routes/public.ts` | Modificar | Agregar rutas `/oc/:token` y `/oc/:token.pdf` |
| `facturador-backend/src/routes/api.ts` | Modificar | Wiring de suppliers + purchase-orders |
| `facturador-backend/templates/orden-compra.html` | **Crear** | Template HTML documento |
| `facturador-backend/templates/orden-compra.css` | **Crear** | Estilos (reutilizar documento.css como base) |
| `facturador-backend/test/d1/suppliers.test.ts` | **Crear** | Tests CRUD proveedores |
| `facturador-backend/test/d1/purchase-orders.test.ts` | **Crear** | Tests órdenes (flujo completo) |

---

## 4. Security & Privacy

- **Roles**: CRUD de proveedores y órdenes: solo admin. Lectura: admin + usuario.
- **Soft-delete**: Proveedores con órdenes asociadas no se pueden hard-delete (FK protege).
- **Token público**: UUID v4 de 36 caracteres, sin datos sensibles expuestos (solo lo que el proveedor necesita ver).
- **Rate limit**: Mismo pipeline que `/c/*` (RateLimitDO + KV fallback).
- **Validación**: Todos los inputs pasan por `validate.ts` (str, optStr, optPrice, optInt, isoDate).

---

## 5. Risks & Roadmap

### Technical Risks

| Riesgo | Mitigación |
|--------|------------|
| SQLite no soporta ALTER CHECK | Validar kind en código, no en BD. Migración futura para normalizar. |
| Condición de carrera en aprobar (2 requests simultáneas) | Usar transacción D1 batch atómico. Validar estado actual antes de transicionar. |
| BROWSER rendering sin add-on | Fallback 501 con mensaje claro (mismo patrón que `/c/:token.pdf`). |
| Token colisión (UUID v4) | Prácticamente imposible; UNIQUE constraint protege. |

### Phased Rollout

#### v1.0 (este PRD)
- CRUD proveedores
- Órdenes de compra: borrador → aprobada → recibida
- Stock entrada automático al aprobar
- Documento PDF
- Vista pública `/oc/:token`
- Asociación factura de compra (referencia simple)

#### v1.1 (futuro)
- Recepción parcial (múltiples ingresos por orden)
- Notas crédito de compras
- Email de orden al proveedor (mismo pipeline sendOrQueue)
- Retenciones en compras

#### v2.0 (futuro)
- Aprobación multi-nivel (flujo de autorización)
- Multi-moneda
- Integración catálogo electrónico de proveedores
