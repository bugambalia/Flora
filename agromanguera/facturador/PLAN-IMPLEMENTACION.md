# Plan de Implementación: Facturador Electrónico Agromanguera

## Resumen Ejecutivo

Extender el cotizador existente (Worker TypeScript + D1 + React SPA) para emitir facturas electrónicas ante la DIAN vía Factus API V2, incorporando inventario, contabilidad básica y estadísticas. Se reutiliza al 100% la infraestructura existente (auth, email pipeline, cola de reintentos, rate limiting, CORS, templates).

> **Estado 2026-08-11**: F1–F10 implementadas (**backend + frontend**), incluyendo el frontend de F9 (turnos/caja + usuarios). La suite E2E completa (`flow`, `ajustes-f1`, `facturas`, `stock`, `turnos`, `usuarios`) pasa en verde (6/6). Frente abierto de **F8 (hardening)**: review OWASP y switch a `FACTUS_ENV=production` (requiere resolución registrada). Ver [IMPLEMENTATION-TRACKING.md](IMPLEMENTATION-TRACKING.md).

> **Nota de paths**: todas las rutas de código de este plan son `facturador-backend/...` y `facturador-frontend/...` (el facturador es un proyecto independiente del cotizador; NO se toca `../cotizador/*`).

---

## Fase F1: Config Fiscal + Migración BD — ✅ IMPLEMENTADA

### Archivos a crear/Modificar

| Archivo | Acción | Descripción |
|---------|--------|-------------|
| `facturador-backend/migrations/0005_facturador.sql` | **Creado** | Tablas fiscales DIAN + invoices/stock/credit_notes/receipts |
| `facturador-backend/src/routes/catalog.ts` | Modificado | Campos fiscales en `company_settings` |
| `facturador-frontend/src/pages/AjustesPage.tsx` | Modificado | Formulario de config fiscal + rangos de numeración |

### Migración `0005_facturador.sql`

```sql
-- Nuevas tablas
CREATE TABLE invoices ( ... );          -- 13 columnas nuevas del PRD
CREATE TABLE invoice_items ( ... );     -- snapshot de línea
CREATE TABLE numbering_ranges ( ... );  -- rangos DIAN
CREATE TABLE stock_movements ( ... );   -- trazabilidad de stock
CREATE TABLE invoice_payments ( ... );  -- pagos parciales/totales
CREATE TABLE credit_notes ( ... );      -- notas crédito DIAN (schema; rutas en F3+)
CREATE TABLE receipts ( ... );          -- recibos de caja/anticipos (schema; sin rutas)

-- ALTER TABLEs existentes (company_settings, clients, products, payment_methods)
-- Total: ~18 columnas nuevas
```

**Nota:** `0004_quote_payment_methods.sql` ya existía, así que la migración fiscal es `0005_facturador.sql`; la de F3 es `0006_f3_invoices.sql` (factus_bill_id + índices).

### Backend: Extender `company_settings`

- Agregar campos: `dv`, `municipality_code`, `ciuu_code`, `regime`, `factura_prefix`, `factura_sequence`
- Modificar handler `PUT /api/company` para validar y persistir nuevos campos
- Validación: NIT solo dígitos, DIVIPOLA obligatorio para emitir

### Frontend: Extender AjustesPage

- Sección "Datos Fiscales" con campos: NIT + DV, municipio DIVIPOLA, CIIU, régimen, prefijo
- Sección "Rangos de Numeración" con tabla de rangos activos
- Solo accesible para rol `admin`

---

## Fase F2: Cliente Factus + Auth OAuth2 — ✅ IMPLEMENTADA

### Archivos a crear

| Archivo | Descripción |
|---------|-------------|
| `facturador-backend/src/lib/factus.ts` | Cliente HTTP Factus V2 |

### `lib/factus.ts` — Componentes

1. **Auth OAuth2:**
   - `POST /oauth/token` con `client_id`, `client_secret`, `username`, `password` (doc oficial; el PRD decía `/v2/auth`, ver nota de discrepancia en el README del backend)
   - Token cache en memoria del Worker (1h TTL)
   - Refresh automático cuando expira (`grant_type=refresh_token`, single-flight)

2. **Emisión:**
   - `POST /v2/bills/validate` (payload completo de factura)
   - `send_email: false` (usamos nuestro pipeline)

3. **Consulta:**
   - `GET /v2/bills/:id` (estado, CUFE, errores)
   - `GET /v2/bills/:id/pdf` (descarga PDF legal)
   - `GET /v2/bills/:id/xml` (descarga XML)

4. **Eliminación:**
   - `DELETE /v2/bills/validate?reference_code=...` (para 409 "pendiente")

5. **Config:**
   - `GET /v1/companies` (config emisor en Factus)
   - `GET /v1/numbering-ranges` (sincronizar rangos)

### Secrets (wrangler)

```
FACTUS_CLIENT_ID, FACTUS_CLIENT_SECRET, FACTUS_USERNAME, FACTUS_PASSWORD, FACTUS_ENV
```

---

## Fase F3: Emisión de Facturas — ✅ IMPLEMENTADA (backend + frontend)

### Archivos a crear

| Archivo | Descripción |
|---------|-------------|
| `facturador-backend/src/routes/invoices.ts` | CRUD facturas + emisión |
| `facturador-backend/src/lib/factus-mapper.ts` | Mapeo cotización → payload Factus |
| `facturador-backend/test/d1/invoices.test.ts` | Tests de integración |

### Rutas (`routes/invoices.ts`)

| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `GET` | `/api/invoices` | admin+usuario | Lista con filtros (status, rango, fecha) |
| `POST` | `/api/invoices` | admin+usuario | Factura directa (sin cotización) — **flujo principal** |
| `GET` | `/api/invoices/:id` | admin+usuario | Detalle + CUFE + PDF URL + errores |
| `DELETE` | `/api/invoices/:id` | admin | Solo si `pendiente` (borra en Factus + D1) |
| `POST` | `/api/invoices/:id/sync` | admin | Re-consulta Factus y actualiza estado |
| `POST` | `/api/invoices/:id/payments` | admin+usuario | Registra abono parcial/total |

### Ruta desde cotización

| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `POST` | `/api/quotes/:id/invoice` | admin+usuario | Desde estado `aceptada` |

### `lib/factus-mapper.ts`

- `buildFactusInvoice(quote, company, client, items)` → payload Factus
- Mapeo detallado en la tabla del PRD (§4)
- Validación Zod previa al payload
- Redondeo a 2 decimales en todos los montos

### Flujo de emisión (síncrono)

```
1. Validar sesión/rol
2. Validar config fiscal completa (409 si falta)
3. Validar estado cotización = aceptada (si aplica)
4. Construir payload (mapper)
5. POST /v2/bills/validate (Factus)
6. Persistir respuesta completa en D1 (misma transacción)
7. Si is_validated: decrementar stock atómico
8. Encolar email (sendOrQueue)
9. Responder 201 con factura
```

### Tests

- **Unit:** mapper con fixtures (IVA 19/5/0, descuentos, crédito, DV, jurídico/natural)
- **Integration:** emisión desde aceptada (201), desde otros estados (409), rechazo FAJ, 409 pendiente → DELETE → re-emisión, idempotencia
- **Fixture set sandbox:** 12 casos mínimo (PRD §3)

---

## Fase F4: Inventario — ✅ IMPLEMENTADA (backend + frontend)

### Archivos a crear/Modificar

| Archivo | Acción | Descripción |
|---------|--------|-------------|
| `facturador-backend/src/routes/stock.ts` | **Creado** | Movimientos, ajustes y alertas |
| `facturador-backend/src/routes/catalog.ts` | Modificado | Agregar campos stock a productos |
| `facturador-frontend/src/pages/StockPage.tsx` | **Implementado** | Vista de inventario (tabla, ajustes, alertas) |

### Rutas (`routes/stock.ts`)

| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `GET` | `/api/products/:id/movements` | admin+usuario | Historial de movimientos |
| `POST` | `/api/products/:id/adjust` | admin | Ajuste manual de stock |

### Lógica de baja atómica

- La baja de stock ocurre en la **misma transacción** que marca la factura `validada`
- Si la factura es `rechazada` o `pendiente`: **no descuenta**
- Política configurable: `bloquear` (409 antes de emitir) o `avisar` (emite con alerta)

### Alertas de stock

- Tras cada baja: si `stock_qty <= stock_min` y `stock_notify`, encola email al admin
- Cron diario (Cloudflare Cron Triggers): recalcula alertas y reenvía resumen
- `GET /api/stats/stock-alerts` para el panel

### Frontend

- `StockPage`: tabla de productos con stock, botón ajuste, indicador de alerta
- Integrar stock visible en `QuoteFormPage` y `FacturaFormPage`

---

## Fase F5: Contabilidad Básica — ✅ IMPLEMENTADA (backend + frontend)

### Archivos a crear

| Archivo | Descripción |
|---------|-------------|
| `facturador-backend/src/routes/accounting.ts` | Libro de ventas, cartera, CSV |

### Rutas (`routes/accounting.ts`)

| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `POST` | `/api/invoices/:id/payments` | admin+usuario | Registrar pago (parcial/total) — vive en `invoices.ts` |
| `GET` | `/api/accounting/sales-ledger` | admin | Facturas + IVA por tarifa, filtros fecha/estado |
| `GET` | `/api/accounting/receivables` | admin | Saldo por cliente |
| `GET` | `/api/accounting/sales-ledger?export=csv` | admin | Export CSV |

### Modelo de pagos

- `invoice_payments`: amount, paid_at, method, note, created_by
- Saldo = `total` − Σ pagos
- Pagos parciales múltiples permitidos

### Frontend

- `ContabilidadPage`: pestañas (Libro de Ventas, Cartera, Pagos)
- Export CSV con filtros de fecha

---

## Fase F6: Estadísticas — ✅ IMPLEMENTADA (backend + frontend)

### Archivos a crear

| Archivo | Descripción |
|---------|-------------|
| `facturador-backend/src/routes/stats.ts` | Métricas del panel (summary + top-products) |

### Rutas (`routes/stats.ts`)

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/api/stats/summary` | Ventas del mes, ingresos por método, conteo por estado DIAN, cartera pendiente, alertas stock |
| `GET` | `/api/stats/top-products` | Top 10 por ingresos (últimos 90 días) |
| `GET` | `/api/stats/stock-alerts` | Productos bajo umbral |

### Frontend

- Extender `PanelPage` con métricas de facturación
- Estados vacíos/carga/error consistentes con el shell existente

---

## Fase F7: Template Factura + Vista Pública — ✅ IMPLEMENTADA

### Archivos a crear

| Archivo | Descripción |
|---------|-------------|
| `facturador-backend/templates/factura.html` | Template híbrido (talonario + DIAN) |
| `facturador-backend/templates/factura.css` | Estilos específicos factura |
| `facturador-backend/src/routes/public.ts` | Modificado: agrega `/f/:token` (vista HTML factura) |

### Rutas públicas

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| `GET` | `/f/:token` | Rate limit | Vista HTML factura (CUFE/QR/resolución) |

### Template híbrido

- Misma familia visual de la cotización (azul blueprint + dorado)
- Matices de talonario colombiano (sellos, papel de copias)
- Campos DIAN integrados: CUFE, QR, resolución, rango, número
- PDF legal desde Factus (no consumir Browser Rendering)

### Email

- Reutilizar `sendOrQueue()` existente
- Adjuntar/enlazar PDF legal de Factus
- Incluir enlace público `/f/:token` con QR/CUFE

---

## Fase F8: Hardening + Lanzamiento — 🔄 EN CURSO

### Checklist

- [x] Migración `0007_public_invoices.sql` + `public_token` al emitir (F7)
- [x] Validación de rangos de numeración: rango activo obligatorio al facturar (409 sin rango)
- [x] Email factura reutilizando `sendOrQueue()` (cola `email-retry`) + enlace público `/f/:token`
- [x] Monitoreo base: cron diario de alertas de stock; cola de reintentos de email configurada
- [x] Documentación de API (`docs/api/rest-api.md`) actualizada
- [x] **E2E sandbox completo (Playwright)** — `flow`, `ajustes-f1`, `facturas`, `stock`, `turnos`, `usuarios`: suite completa en verde (2026-08-11). Requiere que `vite dev` corra con `VITE_PUBLIC_BASE_URL=http://localhost:8787` (si no, los enlaces públicos apuntan al backend desplegado)
- [ ] Review de seguridad (OWASP)
- [ ] Switch `FACTUS_ENV=production` solo cuando exista resolución real en Factus

---

## Módulos planificados (PRDs independientes)

### Turnos de caja + gestión de usuarios (PRD-TURNOS-Y-CAJA.md) — ✅ IMPLEMENTADO (F9, backend 2026-08-08 · frontend 2026-08-11)

Migración `0009_shifts_caja.sql`: `shifts` (abierto/cerrado, apertura_efectivo, conteo por medio, faltante/sobrante), `shift_expenses` (amount + reason), `invoices.shift_id`. Módulos:

| Archivo | Descripción | Estado |
|---------|-------------|--------|
| `facturador-backend/src/routes/shifts.ts` | Apertura, gastos, cierre con conciliación | ✅ Backend |
| `facturador-backend/src/routes/users.ts` | CRUD de usuarios (admin): crear vendedores, desactivar, reset contraseña | ✅ Backend |
| `facturador-backend/migrations/0009_shifts_caja.sql` | Tablas de turnos y gastos + `invoices.shift_id` | ✅ |
| `facturador-backend/src/routes/invoices.ts` | Modificado: exige turno abierto al facturar (`409` sin turno) | ✅ |
| `facturador-backend/test/d1/shifts.test.ts` + `users.test.ts` | Cobertura de turnos y usuarios | ✅ |
| `facturador-frontend/src/pages/UsuariosPage.tsx` | CRUD de usuarios en `ajustes/usuarios` (crear, editar rol, activar/desactivar sin auto-desactivación, reset de contraseña) | ✅ 2026-08-11 |
| `facturador-frontend/src/app/router.tsx` + `AjustesPage.tsx` | Ruta `ajustes/usuarios` (RequireAdmin) + acceso desde Ajustes | ✅ 2026-08-11 |
| `facturador-frontend/src/pages/TurnosPage.tsx` | Turnos en `/turnos`: abrir con fondo, desglose esperado en vivo, gasto con razón, cierre con conteo y conciliación, historial + detalle | ✅ 2026-08-11 |
| `facturador-frontend/src/components/layout/AppShell.tsx` | Nav "Turnos" (todos los roles) | ✅ 2026-08-11 |
| `facturador-frontend/src/pages/FacturaFormPage.tsx` | Aviso dinámico: "Turno abierto — puedes emitir" / "No tienes turno abierto → Abrir turno" | ✅ 2026-08-11 |
| `facturador-frontend/e2e/turnos.spec.ts` + `usuarios.spec.ts` | E2E abrir→gasto→cierre y CRUD de usuarios | ✅ 2026-08-11 |

**Pendiente frontend (v1.1 del PRD):**
- [ ] Reporte de cierres consolidado por día / export CSV / impresión de arqueo (v2.0 del PRD).

### Órdenes de compra (PRD-ORDENES-COMPRA.md) — ✅ IMPLEMENTADO (F10)

Migración `0008_purchase_orders.sql`: `suppliers`, `purchase_orders`, `purchase_order_items` + ingreso de stock al aprobar. Módulos `routes/suppliers.ts`, `routes/purchase-orders.ts`, vistas `/oc/:token` + `OrdenesComprasPage`/`ProveedoresPage` en el frontend.

---

## Orden de Implementación Recomendado

```
F1 (Config + Migración) → F2 (Cliente Factus) → F3 (Emisión) →
F4 (Inventario) → F5 (Contabilidad) → F6 (Estadísticas) →
F7 (Template) → F8 (Hardening)
     └── luego (independientes): F9 Turnos/Caja + Usuarios ✅ · F10 Órdenes de Compra ✅
```

**Estado de ejecución (2026-08-11):** F1–F10 completas (backend + frontend), E2E completo en verde. Solo queda cerrar F8: review OWASP y switch a `FACTUS_ENV=production` (ver checklist arriba).

**Dependencias:**
- F1 → F2 → F3 (secuencial obligatorio)
- F4 puede paralelizar con F5/F6 después de F3
- F7 puede hacerse después de F3
- F8 al final — último frente abierto
- UI de F3/F4/F5/F6/F9/F10 completa (`FacturasPage`, `StockPage`, `ContabilidadPage`, `PanelPage`, turnos/caja, órdenes de compra)

---

## Preguntas para el Usuario — RESUELTAS (ver test-definicion-mvp.md)

Respuestas confirmadas en `test-definicion-mvp.md` (2026-08-03):

1. **Migración**: `0005_facturador.sql` (0004 ya existía) — ✅
2. **Política de venta sin stock**: per-producto (`allow_oversell`) + permisos por rol — ✅
3. **Rol contador**: sí, solo lectura (crear rol `contador` en F1) — ✅ (implementado: `contador` en `auth.ts`, solo lectura en `accounting.ts` y stats)
4. **Email de la factura**: nuestro sistema (`send_email: false` a Factus) — ✅
5. **Dirección visual de la factura**: híbrida (talonario + DIAN) — ✅

Adicionales definidos en el test de empresa: consumidor final 222.222.222 en el MVP, nota crédito y recibos de caja en el MVP, plan Factus COP 220 000/mes ≈ 1 600 facturas, producción "en semanas".
