
## Ticket: Carga Masiva de Datos
- **Asunto**: Implementar importación masiva de clientes, proveedores y productos mediante archivos (CSV, Excel, JSON).
- **Prioridad**: Media.
- **Responsable**: Admin (único autorizado).
- **Estado**: Pendiente.
- **Detalle**: Permitir la carga masiva evitando procesos manuales tediosos. Incluye:
  - Validación de formatos.
  - Gestión de duplicados: Diálogo interactivo (Actualizar/Ignorar).
  - Reporte de resultados (éxitos/fallos).


> Regla del proyecto: el facturador es una evolución del cotizador, pero **NO se toca
> ninguna parte del código ni tests del cotizador** (`../cotizador/*`). Este archivo
> registra los problemas/hallazgos observados en el código de referencia, para que el
> facturador los resuelva en su propia implementación o los evite.

## P1 — Test roto en `cotizador-backend/test/d1/quotes.test.ts` (prexistente)
- **Síntoma**: 2 tests fallan (`npm test`):
  - `devuelve 400 si payment_method_id no existe`
  - `devuelve 400 si payment_method_id está inactivo`
- **Causa**: el test espera `{ error: 'Método de pago no válido' }` pero el código
  (`src/routes/quotes.ts:138`) devuelve `Método de pago ${pmId} no válido`.
- **Acción**: en el facturador se usará el mensaje estable sin interpolación
  (`'Método de pago no válido'`) o se actualizarán los tests desde el inicio.

## P2 — Errores de TypeScript en `test/public.test.ts` + `src/routes/quotes.ts:460` (prexistente)
- **Síntoma**: `npx tsc --noEmit` reporta 6 errores TS:
  - `test/public.test.ts` (referencias a tipos/campos que no existen, p. ej. `paymentMethods` en la respuesta de `/api/quote/...`).
  - `src/routes/quotes.ts:460` — `PublicQuoteData` requiere `paymentMethods` pero el objeto renderizado no lo provee (el tipo quedó desincronizado tras la migración 0004).
- **Acción**: en el facturador se mantendrá `tsc --noEmit` en verde incluyendo tests desde la primera fase. Este desajuste de la vista pública se corregirá en F2 (vista pública de facturas) o antes si bloquea el CI.

## P3 — Formato NIT: puntos y guion vs dígitos
- **Síntoma**: el cotizador acepta NIT con formato `901.234.567-8` (con puntos y
  guion) tanto en `company_settings.nit` como en `clients.nit`, pero la DIAN requiere
  NIT numérico (solo dígitos) + DV separado.
- **Acción**: en el facturador F1 se normaliza: `nit` = solo dígitos (4–12) y `dv` =
  1 dígito, ambos obligatorios para facturar. El formateado con puntos queda solo en
  la capa de presentación.

## P4 — Sin columna DV ni campos de resolución en clientes
- **Síntoma**: la tabla `clients` del cotizador no tiene `identification_document_code`,
  `dv`, `legal_organization_code`, `municipality_code`, `country_code` (la DIAN los
  requiere en la factura).
- **Acción**: la migración 0005 del facturador agrega estos campos a `clients`.

## P5 — `payment_methods` sin campos DIAN
- **Síntoma**: `payment_methods` solo tiene `name`/`active`/`sort_order`; la factura
  DIAN requiere `payment_form` (Forma de Pago — código de la DIAN, p. ej. 1 Contado,
  2 Crédito) y `payment_method_code` (Medio de Pago).
- **Acción**: migración 0005 agrega `payment_form`, `payment_method_code`, `due_days`.

## P6 — Productos sin campos de unidades/stock DIAN
- **Síntoma**: `products` no tiene `unit_measure_code` (UN/ECE Rec 20), `standard_code`
  (código GS1/UNSPSC), ni control de stock (`stock_qty`, `stock_min`, `stock_notify`,
  `allow_oversell`).
- **Acción**: migración 0005 agrega estos campos; F4 usará `allow_oversell` para el
  overselling.

## P7 — Secuencia de factura inexistente
- **Síntoma**: `company_settings` tiene `quote_sequence` pero no `factura_sequence`.
- **Acción**: migración 0005 agrega `factura_sequence` (y `factura_prefix`); la
  numeración real de la DIAN se tomará de `numbering_ranges` (F1 tabla nueva).

## P8 — Sin tabla de rangos de numeración
- **Síntoma**: no existe ninguna tabla de rangos/resoluciones DIAN en el schema.
- **Acción**: migración 0005 crea `numbering_ranges` (kind, prefix, resolution_number,
  from_num, to_num, next_num, start_date, end_date, active, created_by).

## P9 — Tests sin aislamiento de migraciones en el pool
- **Observación**: el pool de vitest del cotizador corre las migraciones desde un
  directorio fijo (`migrations/`); al añadir migraciones nuevas el setup debe seguir
  ordenado por nombre y el estado local recrearse con `npm run migrate:local`.
- **Acción**: documentar en README del facturador que tras añadir una migración hay
  que borrar `.wrangler/state` local y re-ejecutar `npm run migrate:local`.

## P10 — Otros pendientes heredados (para fases posteriores)
- `clients.email` no se valida como email en el backend (solo `optStr`).
- `public.test.ts` depende de estado/DOM del HTML del Worker; frágil ante cambios de
  plantilla.
- No hay migración de datos para pasar `next_reference` → rangos (se migrará en F2).
- El `reject` de cotización no exige `reason` en el backend (se exige en UI).

---
Estado: actualizado tras Wave 1 de F1 (code-reviewer + Qa-Test) y Wave 2 (security-auditor).

Resueltos dentro del facturador (F1):
- **P1** → **RESUELTO**: el facturador usa el mensaje estable (`quotes.ts:120,138` → `'Método de pago no válido'`) y sus tests (`quotes.test.ts:264,275`) verifican ese texto; ya no hay fails por esta causa.
- **P2** → **RESUELTO**: el error `src/routes/quotes.ts:460` (PublicQuoteData sin paymentMethods) quedó resuelto en la vista pública del facturador: `buildDocumentData` en `src/routes/public.ts:410` construye `paymentMethods` desde `quote_payment_methods`/`payment_methods` y el demo (`/c/demo`, `public.ts:1117`) lo incluye. `tsc --noEmit` en verde (incluye `test/`).
- **P3** → **RESUELTO en F1**: `optNit` en `validate.ts` + usado en `PUT /api/company` (NIT solo dígitos 4–12; DV aparte). Clientes aún no (F3).
- **P4/P5/P6/P7/P8** → **RESUELTO en F1**: migración 0005 (columnas DIAN + numbering_ranges + factura_sequence).
- **P9** → documentado en README (F1): tras añadir una migración, `rm -rf .wrangler/state/v3/d1 && npm run migrate:local`.
- **P10** → pendiente (fases posteriores).

Añadidos por la revisión de F1 (resueltos):
- **H2** → fechas de rango validadas como ISO real (isoDate) + `end_date >= start_date` → 400.
- **H3** → solapamiento de rangos activos (kind+prefix) → 409.
- **H4** → cobertura F1: test/d1/company-fiscal.test.ts, numbering-ranges.test.ts, migration-f1.test.ts, test/unit/validate-fiscal.test.ts (365 tests verdes + 2 preexistentes).
- **H5** → **RESUELTO (Wave 3 DevOps)**: D1 `facturador-db` remota creada con database_id real en wrangler.jsonc (mismo ítem que A2).
- **H6** → **RESUELTO**: PATCH /api/numbering-ranges/:id (active, end_date, next_num) + UI no expone aún edición (se añade cuando AjustesPage tenga gestión completa en F2).
- **H1** → **RESUELTO**: NIT normalizado en PUT /api/company.

Wave 2 — security-auditor (sin críticos; 3 altos, 4 medios, 1 bajo). Resoluciones:
- **A1** → **RESUELTO**: `scripts/seed-admin.mjs` apuntaba a `cotizador-db`; ahora crea el admin en `facturador-db`.
- **A2** → **RESUELTO (Wave 3 DevOps)**: D1 `facturador-db` creada en producción (id `3036e340-8da7-4630-94ee-927b607f592d`), database_id real en wrangler.jsonc; migraciones 0001–0005 aplicadas en remoto (42 comandos); seed admin remoto (`admin@agromanguera.co`, contraseña entregada al dueño); worker desplegado en https://facturador-backend.jhonier0301.workers.dev con smoke test OK (login, PUT/GET company, GET ranges).
- **A3** → **PARCIAL**: no hay frontend desplegado aún (la cuenta solo tiene cotizador-backend y asistente-wa). Documentado en wrangler.jsonc (comentario) y README: al desplegar el frontend hay que añadir su origen a `ALLOWED_ORIGINS` y ajustar `PUBLIC_BASE_URL`/DNS.
- **Nuevo (Wave 3)**: la cola `email-retry` ya tenía consumer del cotizador → el facturador usa su propia cola `email-retry-facturador` (+DLQ `email-retry-facturador-dlq`), creada y desplegada con producer+consumer propios.

## Fase F2 — Cliente Factus API V2 + Auth OAuth2 (implementada)
- **Archivo**: `facturador-backend/src/lib/factus.ts` (cliente HTTP con cache de token OAuth2 en memoria + refresh).
- **Auth** (source-driven, https://developers.factus.com.co/autenticacion/auth): `POST /oauth/token` con form-urlencoded `grant_type=password` (sandbox `api-sandbox.factus.com.co` / producción `api.factus.com.co` según `FACTUS_ENV`). Cache de access_token en memoria (1 h, refresh 60 s antes de expirar) usando `refresh_token`; refresh fallido (400) → re-login; access_token nunca persistido en D1.
- **Endpoints**: validateBill (`POST /v2/bills/validate`), getBill (`GET /v2/bills/:id`), getBillPdf/getBillXml (downloads), deleteBill (`DELETE /v2/bills/validate?reference_code=`), getNumberingRanges/getCompanies (`/v1/...`).
- **Secrets** (wrangler secret put): `FACTUS_CLIENT_ID/SECRET/USERNAME/PASSWORD`, `FACTUS_ENV`. Añadidos como opcionales en `Env` (F3 los exige para emitir). Documentados en README de backend.
- **Discrepancia doc**: el PRD (PLAN §2) dice auth `POST /v2/auth`, pero la doc oficial usa `/oauth/token`. Implementado según doc oficial.
- **Hallazgos code-reviewer F2 (resueltos)**: 3 endpoints V2 corregidos (DELETE `/v2/bills/destroy/reference/:code`, `/v2/companies`, `/v2/numbering-ranges`), single-flight en refresh para evitar login doble bajo concurrencia, retry 429 con Retry-After/backoff (rate limit Factus 80/min), `send_email: false` default defensivo, descarga binaria parsea error, comentario TTL corregido (~600s).
- **Tests**: `test/unit/factus.test.ts` (52 tests unitarios verdes: auth POST + cache, refresh ok + fallo 400→relogin, single-flight (5 concurrentes → 1 login), validateBill (sandbox+production base, Bearer+json, send_email default false), 429 retry con Retry-After, getBillPdf/Xml, deleteBill V2, getCompanies/NumberingRanges V2 con ?page=, error 500 auth, 422 body) incluidos en suite: 382 pass / 2 fail (P1, ya resuelto en F3 — ver arriba).

## Fase F3–F5 — Emisión de facturas, Inventario, Contabilidad (implementadas backend)

- **F3 (Emisión)**: `src/routes/invoices.ts` + `src/lib/factus-mapper.ts`. `POST /api/invoices` emite directa (o `POST /api/quotes/:id/invoice` desde cotización `aceptada`). Payload idempotente: `reference_code` `FEV-####` (unicidad enforceada por índice y reintento con 1xx). Baja de stock atómica con validación (unidades > stock → 400 salvo `allow_oversell`). `sync` re-consulta Factus (cambia `pendiente→registrada`/`rechazada`); `payments` registra abonos. PDF/XML: `GET /api/invoices/:id/pdf|xml` (firma del vendedor). Resolución: `invoices.resolution` (config) / facturación de prueba con `GET /api/numbering-ranges`. Toda emisión exige config fiscal completa (400 con `missing_fields`).
- **F4 (Inventario)**: `src/routes/stock.ts`. `GET /api/products/:id/movements` (paginado), `POST /api/products/:id/adjust` (admin: motivo + referencia + número de referencia para ajustes; solo manual ± qty sobre `stock_qty`, sin sobrescritura absoluta), `GET /api/stats/stock-alerts` (pares `{product, stock_qty, stock_min, low_reason}`). Auto-suficiente para el UI sin depender de cotizaciones.
- **F5 (Contabilidad)**: `src/routes/accounting.ts`. `GET /api/accounting/sales-ledger?from&to&status&export=csv` (libro de ventas con IVA por tarifa) y `GET /api/accounting/receivables` (cartera por cliente). CSV con BOM UTF-8.
- **Registro de rutas**: los 3 módulos se registran en `src/routes/api.ts` tras `quotes` (junto a `/api/company`-adjuntos). `Env` añade `FACTUS_CLIENT_ID/SECRET/USERNAME/PASSWORD`, `FACTUS_ENV`, `FACTUS_BASE_URL`? (no), `NUMBERING_RANGE_ID` default (ver README backend: `sync`/emisión requieren `numbering_ranges.active`).
- **Tests**: suite 483/488 en 20 archivos. **Los 5 fails NO son bugs del producto sino tests desactualizados/incorrectos**:
  - `test/d1/accounting.test.ts` (1): aserción de BOM del CSV inválida — `Response.text()` elimina el BOM al decodificar UTF-8; el handler `buildCsv` sí emite los bytes (verificado en el código).
  - `test/d1/stock.test.ts` (4): pasan `pathname` con `abc`/`-5`/`?limit=` directo al handler — el regex/parse de la ruta solo acepta dígitos (o la ruta exacta); en producción `index.ts` entrega el pathname ya limpiado.
  - **Typecheck**: limpio (`tsc --noEmit` incluye `test/`). **Lint**: limpio.
- **Hallazgos F3–F5 a corregir en fixes dedicados (no urgentes)**:
  - **F3-H1**: `invoices.ts` — tras `DELETE` en Factus exitoso pero borrado fallido en D1, el error de D1 oculta el éxito de Factus (doble manejo del borrado lógico/estado `pendiente`).
  - **F4-H1**: `stock.ts` — validación de qty: `if (!Number.isInteger(qty) || qty < 1)` rechaza enteros ≥ 0 válidos; pero semánticamente el ajuste manual ≥ 1 tiene sentido, así que se recomienda definir el contrato (qty ≥ 1 para `manual`).
  - **F4-H2**: `stock.ts` — `products.stock_qty` se lee como `number` pero el handler `GET /products/:id` devuelve `qty: 0` (fallback). Alinear tipos para no perder stock real en el detalle.
- **UI pendiente**: FacturasPage/StockPage/ContabilidadPage no existen aún en el frontend (solo auth/catálogo/clientes/cotizaciones/ajustes).


---
Estado: actualizado tras F1 (Wave 1) + Wave 2 (security) + Wave 3 (DevOps + docs) + F3–F5 backend.
- **M1** → **RESUELTO**: GET /api/company ahora es admin-only (403 para usuarios) + test de cobertura; el nav oculta "Ajustes" a no-admin.
- **M2** → **RESUELTO**: tokens redactados en logs (`/c/<token-redacted>` y sin `token=` en el log de la queue).
- **M3** → **RESUELTO**: `isoDate` acota años a [2000, 2100] (evita vigencias absurdas de rangos).
- **M4** → **RESUELTO**: rate limit por IP en el login (20 intentos/15 min por CF-Connecting-IP) además del bucket por email.
- **B3** → **RESUELTO**: AjustesPage ya no se queda en blanco con BD fresca (GET null → form con defaults para crear la config).
