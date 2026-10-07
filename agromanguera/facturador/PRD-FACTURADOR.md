# PRD — Facturador Electrónico Agromanguera (Factus API + inventario + contabilidad básica)

> **Estado 2026-08-05**: F1–F5 implementadas en `facturador-backend` (config fiscal, cliente Factus OAuth2, emisión de facturas DIAN, inventario, contabilidad básica). F6–F8 (estadísticas, template/vista pública, hardening) y la **UI** de facturas/stock/contabilidad pendientes. Módulos adicionales planificados en PRDs propios: turnos/caja/gastos y usuarios ([PRD-TURNOS-Y-CAJA.md](PRD-TURNOS-Y-CAJA.md)), órdenes de compra ([PRD-ORDENES-COMPRA.md](PRD-ORDENES-COMPRA.md)).
>
> Extiende `REQUIREMENTS.md` (Cotizador Agromanguera). La cotización existente se mantiene como está documentada; este PRD cubre la emisión de facturas electrónicas de venta ante la DIAN vía **Factus API V2**, la facturación directa (sin cotización), la gestión de inventario, la contabilidad básica y las estadísticas operativas. Fuentes técnicas de referencia: `facturador-backend/docs/api/rest-api.md`, `roadmap.md` (sección 7), documentación oficial Factus (developers.factus.com.co, V2) y el test de alineación `test-empresa-facturador.md`.

## 1. Executive Summary

### Problem Statement

Las cotizaciones aceptadas se convierten en ventas que hoy se facturan manualmente (o no se facturan electrónicamente), sin trazabilidad DIAN: sin CUFE, sin resolución de numeración, sin corrección formal ni registro contable. Además, el equipo comercial no tiene visibilidad de inventario al vender (riesgo de vender sin stock), no registra pagos ni cartera, y carece de estadísticas de venta consolidadas.

### Proposed Solution

Convertir el Cotizador Agromanguera en un **facturador electrónico**: emitir facturas de venta ante la DIAN a través de Factus API V2 (sandbox → producción), tanto desde una cotización `aceptada` como directas, persistiendo el estado DIAN (CUFE, número, validación, errores FAJ) y el PDF/XML legales de Factus. En la misma release se incorpora **inventario** (stock, movimientos, alertas), **contabilidad básica** (pagos, cartera, libro de ventas, export CSV) y **estadísticas** del panel.

### Success Criteria

- El flujo E2E en sandbox `cotización aceptada → emitir factura → Factus valida → CUFE + PDF + XML → email al cliente` pasa en Playwright sin errores críticos; igual para el flujo de facturación **directa** sin cotización.
- ≤ 5 % de facturas rechazadas por **errores de integración** (mapeo de campos, redondeo, idempotencia) sobre un set de fixtures sandbox con IVA 19 %, IVA 5 %, exento 0 %, descuento por ítem, descuento global y crédito con `due_date`; los rechazos restantes solo por reglas de negocio DIAN.
- El 100 % de las facturas emitidas persisten en D1 `cufe`, `number`, `validated_at`, `errors` y `links` devueltos por Factus antes de responder al usuario (auditoría completa).
- Emitir una factura (directa o desde cotización) toma ≤ 2 minutos para un usuario comercial entrenado; reintentar una emisión fallida no produce duplicados (idempotencia por `reference_code`).
- La baja de stock es atómica con la validación de la factura (una rechazada no descuenta); la notificación de inventario bajo llega al admin en < 60 s tras la baja y el cron diario revisa todo el catálogo.
- Cartera por cliente, libro de ventas y estadísticas del panel responden en < 1.5 s con 500 facturas y 2 000 movimientos de inventario en D1 local.

## 2. User Experience & Functionality

### User Personas

- **Vendedor comercial:** emite facturas desde cotizaciones aceptadas o directas, ve el estado DIAN de cada factura, consulta stock disponible y recibe avisos de inventario bajo.
- **Admin comercial:** configura los datos fiscales de la empresa (régimen, municipio DIVIPOLA, dígito de verificación, CIIU, prefijo), rangos de numeración/resolución, métodos de pago con códigos Factus, umbrales de stock y reglas de emisión.
- **Contador:** consulta el libro de ventas, descarga PDF/XML de cualquier factura, registra pagos y revisa la cartera por cliente (exporta a CSV).
- **Cliente final:** recibe la factura electrónica por email (PDF legal de Factus y enlace público con QR/CUFE).
- **Agente técnico/operador:** configura secrets de Factus, cambia ambiente sandbox/producción, monitorea rechazos y colas.

### User Stories

- **Story 1 (Config fiscal):** As an admin, I want to configure NIT con dígito de verificación, municipio DIVIPOLA, CIIU, régimen, prefijo de factura y rangos de numeración autorizados so that every emitted invoice complies with the DIAN Anexo Técnico.
- **Story 2 (Factura desde cotización):** As a seller, I want to emit an invoice from an accepted quote so that I don't re-type data.
- **Story 3 (Factura directa):** As a seller, I want to emit an invoice directly (with or without client and catalog items) so that sales that never went through a quote are still billed.
- **Story 4 (Estado DIAN):** As a seller or admin, I want to see the DIAN status (pendiente/validada/rechazada con códigos FAJ), CUFE, número y resolución of every invoice so that I know the sale was legalized.
- **Story 5 (PDF/XML/email):** As a seller, I want to send the invoice to the client by email with the legal PDF so that the customer receives the DIAN-validated document.
- **Story 6 (Inventario):** As a seller, I want stock to decrease atomically when the invoice is validated and to see available stock while quoting/billing so that I don't sell what isn't available.
- **Story 7 (Alertas de stock):** As an admin, I want low-stock notifications (immediate on decrement + daily cron + dashboard view) so that replenishment happens in time.
- **Story 8 (Contabilidad básica):** As an admin or accountant, I want to register payments against invoices, view the sales ledger and per-client balance, and export CSV so that basic accounting is not manual.
- **Story 9 (Estadísticas):** As an admin, I want a panel with monthly sales, top products, per-status invoice counts, revenue by payment method, pending receivables and stock alerts so that decisions are data-driven.

### Acceptance Criteria

**Story 1: Config fiscal**
- Onboarding/ajustes obligatorios para emitir: NIT sin formato + dígito de verificación (o calculado), municipio DIVIPOLA del establecimiento, CIIU, régimen (`comun`/`simplificado`) y prefijo de factura. Emitir sin esta configuración responde `409` con mensaje claro.
- Al menos un rango de numeración activo registrado (manual por ahora: `numbering_ranges`); la validación de emisión verifica que el rango no esté vencido (`end_date`).
- Solo rol `admin` puede modificar configuración fiscal y rangos.

**Story 2: Factura desde cotización**
- `POST /api/quotes/:id/invoice` solo desde estado `aceptada` (cualquier otro → `409`); el detalle de la cotización se convierte en `invoice_items` snapshot.
- `reference_code` = `FEV-####` consecutivo atómico (`company_settings.factura_sequence`, patrón de `quote_sequence`); idempotente: reintentar la misma emisión devuelve la factura existente.
- La respuesta de Factus (CUFE, número, rango, resolución, `is_validated`, `errors`, `links`) se persiste íntegra antes de responder 201.

**Story 3: Factura directa**
- `POST /api/invoices` sin cotización: cliente (o `consumidor final` si se habilita), items de catálogo o manuales, método de pago y observación; mismas validaciones de cálculo que el motor existente.
- Si se emite con `product_id` y stock activo, se valida disponibilidad antes de enviar a Factus.

**Story 4: Estado DIAN**
- `invoices.status` ∈ `pendiente | validada | rechazada | anulada`; `GET /api/invoices` filtra por status, rango y fecha.
- `GET /api/invoices/:id` expone `cufe`, `qr_url`, `pdf_url` (Factus), `number`, `numbering_range` (prefijo/resolución/vigencia) y `errors` con códigos FAJ legibles (término en español).
- `POST /api/invoices/:id/sync` re-consulta `GET /v2/bills/:id` y actualiza el estado; `errors_json` nunca se pisa con vacío si el documento ya está validado.
- Una factura `pendiente` (Factus 409 "pendiente por enviar") permite `DELETE /api/invoices/:id` (borrado en Factus + D1) y re-emisión limpia.

**Story 5: PDF/XML/email**
- `send_email: false` a Factus; el envío usa el pipeline existente (Email Service + cola `email-retry`) con `from` = `company_settings.email`, adjuntando/enlazando el PDF legal de Factus (`GET /v2/bills/:id/pdf`).
- El email incluye el enlace público `/f/:token` con QR/CUFE.
- Cliente sin email válido → `409` con mensaje configurado (mismo criterio que cotizaciones).

**Story 6: Inventario**
- `products.stock_qty` (inicializable en alta/edición de producto); `stock_movements` registra toda entrada/salida/ajuste con `ref_type/ref_id`, `note` y `created_by`.
- La baja de stock ocurre en la misma transacción que marca la factura `validada` (decremento por `invoice_items`); una factura `rechazada` o `pendiente` no descuenta.
- Política de insuficiencia **per-producto**: `products.allow_oversell` (1 = se emite y queda alerta, 0 = 400 antes de emitir) + permisos por rol; no es global.
- Movimientos consultables por producto (`GET /api/products/:id/movements`).

**Story 7: Alertas de stock**
- Tras cada baja, si `stock_qty <= stock_min` y `stock_notify`, se encola email al admin (cola existente).
- Cron diario (Cloudflare Cron Triggers) recalcula alertas y reenvía resumen; `GET /api/stats/stock-alerts` lista productos bajo umbral.

**Story 8: Contabilidad básica**
- `POST /api/invoices/:id/payments` registra pagos (parciales/totales) con `amount`, `paid_at`, `method` y `created_by`; el saldo = `total` − Σ pagos.
- `GET /api/accounting/sales-ledger` (facturas + IVA por tarifa, filtros fecha/estado) y `GET /api/accounting/receivables` (saldo por cliente) con `export=csv`.

**Story 9: Estadísticas**
- `GET /api/stats/summary` devuelve: ventas del mes (total y nº), ingresos por método de pago, conteo por estado DIAN, cartera pendiente total y alertas de stock.
- `GET /api/stats/top-products` (top 10 por ingresos/últimos 90 días).
- Panel (`PanelPage`) con esas métricas y estados vacíos/carga/error consistentes con el shell existente.

### Non-Goals (MVP)

- No notas crédito/débito automáticas (v1.1) — solo el flujo de factura de venta.
- No consumidor final (factura sin NIT) en MVP (v1.1), salvo decisión del test de empresa.
- No retenciones en la UI del MVP (los campos quedan mapeados en el cliente Factus para uso futuro).
- No recibos de caja ni anticipos (`prepayment_details`).
- No documento soporte, nómina electrónica, contingencia ni doble moneda.
- No webhooks de Factus (sync manual + endpoint de re-consulta); webhook en v1.1 si Factus V2 lo permite.
- No contabilidad formal: sin PUC, sin comprobantes de egreso/gastos, sin conciliación bancaria, sin impuestos de nómina.
- No múltiples establecimientos ni inventario por bodega (stock único).
- No diseñador visual del PDF legal de Factus más allá del logo de empresa configurado en Factus.
- No migrar la numeración de cotizaciones (`COT-####`): el rango DIAN es independiente.

## 3. AI System Requirements (If Applicable)

No hay feature de IA orientada al cliente final en esta release (igual que Release 1 del cotizador). La IA/agentes solo apoyan el desarrollo y operación:

- Colecciones oficiales de Factus (Postman/Bruno) para explorar y probar los endpoints sin escribir código.
- Sandbox DIAN real vía Factus como entorno de validación de fixtures.
- Cloudflare MCP (bindings/observability) para secrets, logs y colas del Worker.

### Evaluation Strategy

- **Fixture set sandbox** (mínimo 12 casos): IVA 19 %, IVA 5 %, exento 0 % (`is_excluded`), ítem con descuento `discount_rate`, descuento global → `allowance_charges`, crédito con `due_date`, contado, cliente jurídico con DV, cliente natural, ítem manual sin producto, 409 "pendiente por enviar" con DELETE y re-emisión, y error FAJ (rechazo con `errors` persistido).
- **Criterio**: ≥ 95 % de los fixtures validan con `is_validated: true` en el primer intento; el 100 % de los rechazos dejan `errors_json` persistido y un mensaje accionable en la UI.
- **Paridad de totales**: para cada fixture, `data.totals.total` de Factus debe coincidir con nuestro `quoteMath` previsualizado; diferencias > COP 1 por redondeo quedan documentadas como caso de ajuste.

## 4. Technical Specifications

### Architecture Overview

Se extiende el stack del cotizador (Worker TypeScript + D1 + React SPA), pero como proyecto independiente en `facturador-backend` / `facturador-frontend` (NO se toca `../cotizador/*`).

```
CotizacionesPage / FacturasPage / PanelPage (React SPA, Vercel)
   │  sesión propia (Bearer, ADR 002) — roles admin/usuario
   ▼
Worker /api/*  (Cloudflare)
   ├─ routes/quotes.ts  → POST /api/quotes/:id/invoice      (desde cotización aceptada)
   ├─ routes/invoices.ts → CRUD facturas directas, sync, DELETE pendiente, payments, pdf, xml
   ├─ routes/stock.ts   → movements, adjust, stock-alerts (inventario)
   ├─ routes/accounting.ts → sales-ledger, receivables, export CSV
   ├─ routes/stats.ts → summary, top-products, stock-alerts (F6, planificada)
   ├─ routes/catalog.ts → + stock/movements/ajuste
   ├─ lib/factus.ts → cliente HTTP Factus V2 (token OAuth2 1h + refresh, cache en memoria)
   │     POST /v2/bills/validate · GET /v2/bills/:id · GET /v2/bills/:id/pdf
   │     GET /v2/bills/:id/xml  · DELETE /v2/bills/destroy/reference/:code
   │     GET /v2/numbering-ranges · GET /v2/companies (config)
   ├─ D1 → invoices, invoice_items, numbering_ranges, stock_movements, invoice_payments
   ├─ cola email-retry (Email Service) → factura con PDF legal + enlace público
   └─ Cron Triggers → revisión diaria de stock bajo
Vista pública /f/:token → template factura.html (CUFE/QR/resolución) servido por el Worker
```

Flujo de emisión (síncrono): validar sesión/rol → validar configuración fiscal y estado → construir payload (`buildFactusInvoice`, mapper quote→Factus) → `POST /v2/bills/validate` → persistir respuesta completa en D1 (misma transacción que la baja de stock si `is_validated`) → encolar email → responder 201 con la factura.

### Integration Points

- **Factus API V2** (`https://api-sandbox.factus.com.co` / `https://api.factus.com.co`):
  - Auth: `POST /oauth/token` (OAuth2, form-urlencoded `grant_type=password`: `client_id`, `client_secret`, `username`, `password`) → `access_token` (1 h) + `refresh_token`; refresco automático con cache en memoria. *Nota de discrepancia: este PRD/PLAN indicaba `POST /v2/auth`; implementado según doc oficial con `/oauth/token`.*
  - Emisión: `POST /v2/bills/validate` (idempotente por `reference_code`; `send_email: false`).
  - Consulta: `GET /v2/bills/:id`; descargas: `GET /v2/bills/:id/pdf`, `GET /v2/bills/:id/xml`.
  - Eliminación de no validada: `DELETE /v2/bills/destroy/reference/:code` (patrón oficial ante 409 "pendiente por enviar").
  - Config: `GET /v2/companies`, `GET /v2/numbering-ranges` (sincronizar rangos con D1).
- **Secrets (wrangler)**: `FACTUS_CLIENT_ID`, `FACTUS_CLIENT_SECRET`, `FACTUS_USERNAME`, `FACTUS_PASSWORD`, `FACTUS_ENV` (`sandbox|production`). El token de acceso nunca se persiste en D1.
- **Email**: pipeline existente (Cloudflare Email Service + cola `email-retry`), sin cambios de arquitectura.
- **Cron**: Cloudflare Cron Triggers → `stock-alerts-daily`.
- **Frontend**: nuevas páginas `FacturasPage`, `ContabilidadPage` (o secciones en el shell existente), `StockPage`; TanStack Query para `invoices`, `stats`, `accounting`.

### Data Model — migración `0005_facturador.sql`

> Implementado. La migración real es `0005_facturador.sql` (0004 ya existía como `quote_payment_methods`); F3 añadió `0006_f3_invoices.sql` (factus_bill_id + índices) y el módulo de turnos/caja propone `0007_shifts_caja.sql` (ver PRD-TURNOS-Y-CAJA.md). Las tablas abajo son la referencia de diseño; el esquema definitivo vive en `facturador-backend/migrations/`.

Nuevas tablas:

```sql
invoices (id PK, quote_id FK NULL UNIQUE, status,        -- pendiente|validada|rechazada|anulada
  reference_code TEXT NOT NULL UNIQUE,                   -- FEV-#### idempotente
  factus_bill_id TEXT, number TEXT, numbering_range_id FK NULL,
  cufe TEXT, qr_url TEXT, pdf_url TEXT, xml_url TEXT,
  validated_at TEXT, errors_json TEXT,                   -- códigos FAJ completos
  subtotal REAL, discount_items REAL, discount_global REAL,
  tax_total REAL, total REAL,                            -- tomados de la respuesta Factus
  created_by TEXT NOT NULL, created_at, updated_at)

invoice_items (id PK, invoice_id FK CASCADE, product_id FK NULL,
  code_reference TEXT, description TEXT, quantity REAL, unit_price REAL,
  discount_percent REAL, tax_rate REAL, line_total REAL)

numbering_ranges (id PK, kind TEXT CHECK (kind IN ('factura','nota_credito')),
  prefix TEXT, resolution_number TEXT, from_num INT, to_num INT,
  start_date TEXT, end_date TEXT, months INT, factus_range_id INT, active INT)

stock_movements (id PK, product_id FK, kind TEXT,             -- entrada|salida|ajuste
  qty REAL NOT NULL, ref_type TEXT, ref_id INT, note TEXT,
  created_by TEXT, created_at)

invoice_payments (id PK, invoice_id FK CASCADE, amount REAL,
  paid_at TEXT, method TEXT, note TEXT, created_by TEXT, created_at)
```

Cambios a tablas existentes:

```sql
ALTER TABLE company_settings ADD COLUMN dv TEXT;                  -- dígito verificación NIT
ALTER TABLE company_settings ADD COLUMN municipality_code TEXT;   -- DIVIPOLA establecimiento
ALTER TABLE company_settings ADD COLUMN ciuu_code TEXT;
ALTER TABLE company_settings ADD COLUMN regime TEXT DEFAULT 'comun';
ALTER TABLE company_settings ADD COLUMN factura_prefix TEXT DEFAULT 'FEV';
ALTER TABLE company_settings ADD COLUMN factura_sequence INT DEFAULT 1;

ALTER TABLE clients ADD COLUMN identification_document_code TEXT DEFAULT '31';  -- 13 CC | 31 NIT | 06 NIT otra | 03 Pasaporte
ALTER TABLE clients ADD COLUMN dv TEXT;
ALTER TABLE clients ADD COLUMN legal_organization_code TEXT DEFAULT '2';        -- 1 jurídica | 2 natural
ALTER TABLE clients ADD COLUMN municipality_code TEXT;
ALTER TABLE clients ADD COLUMN country_code TEXT DEFAULT 'CO';

ALTER TABLE products ADD COLUMN stock_qty REAL NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN stock_min REAL NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN stock_notify INT NOT NULL DEFAULT 1;
ALTER TABLE products ADD COLUMN unit_measure_code TEXT DEFAULT '94';            -- UBL; 94 = unidad
ALTER TABLE products ADD COLUMN standard_code TEXT DEFAULT '999';               -- estándar del contribuyente

ALTER TABLE payment_methods ADD COLUMN payment_form TEXT DEFAULT '1';           -- 1 contado | 2 crédito
ALTER TABLE payment_methods ADD COLUMN payment_method_code TEXT DEFAULT '42';   -- código Factus (42 transferencia)
ALTER TABLE payment_methods ADD COLUMN due_days INT DEFAULT 0;                  -- plazo si crédito
```

### Mapeo de campos (motor de cotización → payload Factus)

| Motor actual | Payload Factus (`POST /v2/bills/validate`) | Notas |
|---|---|---|
| `quote_items.description` | `items[].name` | máx. 2 decimales en todos los montos |
| `quote_items.quantity` | `items[].quantity` | `string` con 2 decimales |
| `quote_items.unit_price` | `items[].price` | neto sin impuestos ni descuentos |
| `quote_items.discount_percent` | `items[].discount_rate` | en % (2 decimales) |
| descuento global (`%`) | `allowance_charges[0]` `{ is_surcharge: false, reason, base_amount, amount }` | `concept_type` según tabla Factus (validar en sandbox) |
| `quote_items.tax_rate` | `items[].taxes[]` `{ code: "01", rate }` | IVA; tarifa 0 → `is_excluded: true` (validar criterio con Factus) |
| `products.code` | `items[].code_reference` | código del producto o manual |
| `products.unit_measure_code` | `items[].unit_measure_code` | UBL |
| `products.standard_code` | `items[].standard_code` | default `999` |
| `clients.nit` (`901.234.567-8`) | `customer.identification` (solo dígitos) + `customer.dv` | nunca con puntos/guiones |
| `clients.identification_document_code` | `customer.identification_document_code` | NIT=31, CC=13… |
| `clients.legal_organization_code` | `customer.legal_organization_code` + `company`/`names` | 1→`company` (razón social), 2→`names` |
| `clients.municipality_code` | `customer.municipality_code` | DIVIPOLA; vacío si extranjero |
| `clients.address/email/phone` | `customer.address/email/phone` | — |
| `payment_methods.payment_form/method_code` | `payment_details[]` `{ payment_form, payment_method_code, amount }` | crédito requiere `due_date` |
| `company_settings.nit/municipality` | (config del emisor en Factus: `PUT /v1/companies`) | se sincroniza, no se envía por factura |
| `terms` / `notes` | `observation` | máx. 250 caracteres en Factus |
| folio cotización `COT-0001` | `order_reference.reference_code` (opcional) | trazabilidad opcional |
| **totales calculados** | `data.totals` (respuesta) | **fuente de verdad DIAN**: persistir respuesta, no recálculo local |

### Security & Privacy

- Toda ruta `/api/*` exige sesión válida (sistema actual, ADR 002). **Emisión** (directa o desde cotización) y **registro de pagos**: roles `admin` **y** `usuario` (los vendedores emiten y cobran). **Admin-only**: sync (`POST /api/invoices/:id/sync`), borrado de pendientes (`DELETE`), ajustes de stock (`POST /api/products/:id/adjust`), contabilidad (`sales-ledger`, `receivables`), configuración fiscal/rangos y catálogo en escritura. Lectura de facturas y libro de ventas: `admin` y `usuario`.
- `reference_code` idempotente: reintentos y doble clic no generan facturas duplicadas ante Factus.
- `errors_json` y `cufe` se tratan como datos sensibles de auditoría: no se exponen en listados agregados (solo detalle), igual patrón que `public_token`.
- Vista pública `/f/:token`: token UUID v4 único e inmutable para la factura, sin contenido si el token no existe; `noindex`, `no-store`, `referrer-policy: no-referrer` y rate limit existente de `/c/*` reutilizado.
- Secrets de Factus solo en `wrangler secret`, nunca en `vars` ni en el frontend; el token OAuth2 vive en memoria del Worker (se pierde en reinicio → refresh desde secrets).
- Logs: `created_by` en `invoices`, `stock_movements` e `invoice_payments` para auditoría; errores de Factus con `reference_code` para diagnóstico.
- No se persisten PDF ni XML en R2 en MVP (descarga on-demand de Factus); el email adjunta o enlaza el PDF en el momento del envío.

### Testing Requirements

- **Unit**: `factus.ts` (auth/refresh, timeout, retries con backoff), mapper `buildFactusInvoice` con fixtures (19/5/0, descuentos, crédito, DV, jurídico/natural), idempotencia de `reference_code`, cálculo de saldo de pagos, baja de stock atómica.
- **Integration (mock de Factus)**: emisión desde `aceptada` (201 + persistencia), desde otros estados (409), factura directa, `is_validated: true/false`, rechazo FAJ con `errors_json`, 409 "pendiente por enviar" → DELETE → re-emisión, sync (`GET /v2/bills/:id`), 403 para rol `usuario` en emisión.
- **E2E (Playwright, sandbox real)**: config fiscal → alta de producto con stock → cotización → aceptación → emisión → validación DIAN → email → decremento de stock → alerta de stock bajo → registro de pago → cartera/estadísticas.
- **Manual QA**: vista pública `/f/:token` en móvil/desktop, PDF de Factus, export CSV, pantallas de error FAJ en español.

## 5. Risks & Roadmap

### Phased Rollout

#### MVP / Fases F1–F8 (ver `roadmap.md` §7)

> **Estado 2026-08-05**: F3–F5 del roadmap implementadas en backend (los entregables F4/F5/F6 originales se fusionaron: inventario y contabilidad ya viven en `stock.ts`/`accounting.ts`). Lo pendiente es la **UI** (FacturasPage/StockPage/ContabilidadPage), F6 estadísticas, F7 template/vista pública y F8 hardening.

- **F1 Onboarding Factus/DIAN (gestión)**: cuenta Factus (plan por volumen), habilitación sandbox, registro del software ante la DIAN (Res. 000165/2023, con guía de Factus) y resolución de numeración con prefijo `FEV`. **Bloqueante para producción, no para sandbox.** *(Pendiente de gestión externa.)*
- **F2 Config fiscal + migración**: `0005_facturador.sql`, campos fiscales en ajustes/clientes/productos/pagos, sync de rangos con `GET /v2/numbering-ranges`. *(Implementado en `0005` + `0006`.)*
- **F3 Emisión**: `lib/factus.ts`, mapper, `POST /api/quotes/:id/invoice` y `POST /api/invoices`, persistencia de respuesta, idempotencia, DELETE de pendientes, sync manual. *(Implementado.)*
- **F4 Inventario**: stock, movimientos, baja atómica al validar, política bloquear/avisar, alertas (email + cron + vista). *(Backend implementado en `stock.ts`; alertas por cron pendientes.)*
- **F5 Contabilidad básica**: pagos, cartera, libro de ventas, CSV. *(Implementado.)*
- **F6 Estadísticas**: `summary`, `top-products`, stock-alerts y UI en `PanelPage`. *(Pendiente.)*
- **F7 Template factura híbrido**: `templates/factura.html` + `factura.css` (matices Talonario Carbon Copy), vista pública `/f/:token`, envío de email con PDF legal de Factus. *(Pendiente.)*
- **F8 Hardening + lanzamiento**: E2E sandbox completo, review de seguridad, switch `FACTUS_ENV=production` solo cuando exista resolución; checklist de lanzamiento. *(Pendiente.)*

#### v1.1

- Notas crédito/débito vinculadas (`related_documents`, tabla `credit_notes`) y su corrección formal.
- Consumidor final (factura sin NIT) si el test de empresa lo requiere.
- Retenciones en UI (`withholding_taxes`) y autorretenciones.
- Webhooks de Factus (si disponibles en V2) para sync automático de estados.
- Ajustes de inventario desde la UI (mermas/devoluciones con `stock_movements` de ajuste).
- Gastos/egresos simples si la contabilidad básica lo exige.

#### v2

- Documento soporte y notas de ajuste, recibos de caja/anticipos (`prepayment_details`), contingencia DIAN, doble moneda.
- Inventario por bodega/establecimiento, reportes avanzados (export XLSX, gráficos).

### Technical Risks

- **Dependencia externa del cronograma**: la resolución de numeración y el registro del software son del cliente (Agromanguera). Mitigación: MVP completo en sandbox sin bloquearse; producción con checklist y switch explícito.
- **Rechazos DIAN por datos incompletos**: municipio, DV, régimen, código de producto. Mitigación: validación Zod previa al payload, fixtures sandbox, mensajes FAJ accionables.
- **Idempotencia ante 409 "pendiente por enviar"**: duplicados si no se maneja. Mitigación: `reference_code` único + DELETE oficial + tests.
- **Diferencia de redondeo/centavos**: `REAL` 2 decimales es compatible con el Anexo Técnico, pero los totales se toman de Factus; si la contabilidad lo exige, migrar precios a centavos enteros (INTEGER) en F2 antes de lanzar.
- **Personalización limitada del PDF legal de Factus**: solo logo/colores básicos de la representación gráfica. Mitigación: vista pública propia `/f/:token` con nuestro template (marca híbrida) + PDF oficial de Factus; validar opciones de representación gráfica con soporte Factus.
- **Inventario inexacto al arranque**: requiere conteo inicial y alta con stock real. Mitigación: carga inicial + ajustes auditados; decisión bloquear/avisar tomada con el test de empresa.
- **Costo**: suscripción Factus por volumen + Browser Rendering paid (para cotizaciones). Mitigación: PDF de facturas desde Factus (no consume Browser Rendering), monitorizar volumen.
- **Cambios normativos DIAN**: resolución vigente o nuevos requisitos del Anexo Técnico. Mitigación: Factus absorbe el cumplimiento (PT); alerta de rango próximo a vencer con `end_date`; sync de rangos.

## 6. Decisiones de negocio (RESUELTAS en test-empresa-facturador.md)

1. Política de venta sin stock: **per-producto** (`allow_oversell`) + permisos por rol (no global bloquear/avisar).
2. Consumidor final en MVP: **sí**, con documento 222.222.222 (definido en el test de empresa).
3. Dirección visual de la factura: **híbrida** confirmada (matices talonario + DIAN) — template pendiente (F7).
4. Volumen/plan Factus: plan **COP 220 000/mes ≈ 1 600 facturas**; producción "en semanas" tras onboarding.
5. Alcance "contabilidad básica": **libro de ventas + cartera + pagos + CSV** cubren al contador (implementado en F5).
6. Email de la factura: **lo envía nuestro sistema** (`send_email: false` a Factus) — pipeline pendiente (F7).
7. La numeración es **independiente** de la de cotizaciones: rango DIAN propio con prefijo `FEV`.

> Decisión de alcance 2026-08-05: los **turnos de caja con gastos** y la **gestión de usuarios (vendedores) por admin** son requisitos confirmados del negocio y se tratan como PRDs propios: [PRD-TURNOS-Y-CAJA.md](PRD-TURNOS-Y-CAJA.md) y, para compras, [PRD-ORDENES-COMPRA.md](PRD-ORDENES-COMPRA.md).
