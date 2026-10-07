# Design brief — FacturasPage (emisión y estado DIAN)

## 1. Job and audience

**Quién llega:** vendedores (admin+usuario con límites de descuento) que emiten facturas diariamente en volumen (50–200/mes), admin que corrige rechazadas, contador en solo lectura. Trabajan en desktop (contador, admin) y mobile en campo (vendedor). Modo: **Operate**.

**Contexto:** la venta directa es el flujo principal (< 50 % de ventas nacen de cotización). El usuario quiere facturar rápido, saber si la DIAN validó, y no duplicar.

## 2. Outcome and proof

**Tarea primaria:** convertir una venta en factura DIAN válida en el menor tiempo posible, sin duplicados, con visibilidad total del estado.

**Éxito:** emisión ≤ 2 min; estado DIAN visible en cada fila sin abrir detalle; reintento no duplica (`reference_code` idempotente); una rechazada no descuenta stock.

**Prueba/evidencia:** ledger con número/prefijo (mono), cliente, fecha, total COP (tabular), método de pago, due, sello de estado; detalle con CUFE/resolución/errores FAJ en español, PDF/XML, pagos.

## 3. Selected direction

Mundo heredado **"The Validation Ledger"** (DESIGN.md): identidad Deployable Sheet del cotizador re-tuneada a Operate; lista como ledger denso (sin tarjetas); sellos de estado `pendiente | validada | rechazada | anulada`; CUFE/FAJ/mono siempre exactos. Referencia de densidad: Alegra POS / Siigo POS.

**Tesis estructural:** la factura es una fila en un sistema de archivo DIAN — no una tarjeta. Tres pantallas conectadas por rutas:

- **Lista (ledger):** filtros mono como franja instrumental → tabla densa → acciones inline.
- **Emisión (página dedicada):** dos paneles — ítems+fiscal | totales+estado.
- **Detalle (página propia):** expediente completo de la factura (CUFE, errores, PDF/XML, pagos, sync, delete).

**Secuencia:** abrir con mes actual → filtrar/emitir → detalle para sync o errores. **Momento focal:** el "stamp settle" — al hacer sync, el sello pasa a `validada` (foil) asentándose sobre el ledger mientras el total band fija la cifra legal.

**Consecuencia de implementación:** rutas `/facturas`, `/facturas/nueva`, `/facturas/:id`; componentes reutilizables del shell (tabla, stamp, mono label, botones foil).

## 4. Scope and boundaries

**Fidelidad:** pantallas producción-ready, flujo completo E2E (listar → emitir → validar → detalle).

**En esta superficie:**
- Ledger con filtros (mes, estado, rango numeración, cliente, método) + export CSV (admin/contador).
- Emisión directa y desde cotización `aceptada` (selector de cotización si aplica).
- Detalle con sync manual, PDF/XML (links Factus), vista pública `/f/:token`, pagos (admin), delete de pendientes (admin).
- Empty states como coach (primer-run: "configura la identidad fiscal" si falta).

**Anti-goals explícitos:** sin contabilidad/PUC (ContabilidadPage la tiene); sin retenciones (v1.1); sin webhooks; sin diseño de PDF propio (PDF legal de Factus + vista pública híbrida `/f/:token` en F7).

**Sin tocar:** shell existente, tokens, auth/roles, motor de cotizaciones, pipeline de email.

## 5. States and ranges

**Rangos de datos:** 50–200 facturas/mes; mes actual por defecto con paginación 25/fila; 12–20 ítems por factura típico; totales COP con 2 decimales.

**Estados materiales:**
- **Primer-run:** sin config fiscal → coach con una acción ("Configurar identidad fiscal"), sin dead-ends.
- **Vacío:** mes sin facturas → faceta plegada con `stamp` y CTA "Nueva factura".
- **Carga:** skeleton en filas (no spinners).
- **Error DIAN:** rechazada con códigos FAJ legibles en español en el detalle + mensaje accionable.
- **Pendiente:** sello valley, acción sync visible.
- **Permisos:** acciones por rol (emisión admin/usuario; sync/delete/pagos admin; lectura+descarga contador).
- **Sobreflujo:** columnas críticas del ledger se conservan en mobile (número, cliente, total, estado).

## 6. Interaction and layout

**Topología:** listado denso con filtros arriba (franja mono) y acciones a la derecha (export). Clic en fila → detalle; "Nueva factura" (foil) → página de emisión.

**Emisión:** dos paneles — izquierda: datos del cliente + ledger de ítems editable (código, descripción, cant., valor, dcto, IVA); derecha: resumen (subtotal, descuentos, IVA, total band) + estado/política de stock (bloquear/avisar según `allow_oversell` y permisos). Validación Zod inline; errores como mono annotations, no toasts ruidosos.

**Detalle:** carátula (folio + sello estado + resolución) → facetas: fiscal (NIT/DV/DIVIPOLA/CIIU), ítems, totales, DIAN (CUFE/QR/errores FAJ), pagos (admin), acciones (PDF/XML/sync/delete pendiente).

**Affordances:** botones foil solo para acción primaria; sellos de estado; mono en metadatos; hover de fila para acciones secundarias.

**Feedback/transiciones:** `fold-in` de entrada; stamp settle en sync; 150ms micro-transiciones; nunca spinners decorativos.

**Responsive:** drawer del shell en mobile; el ledger conserva columnas esenciales; emisión se apila (ítems arriba, totales abajo fijos).

## 7. Constraints and open decisions

**Constraints:**
- Hereda tokens/shell/fuentes del cotizador (Blueprint Ink, Lock Foil, Archivo/Inter/IBM Plex Mono, facetas).
- Roles: `admin` | `usuario` (límite descuento) | `contador` (solo lectura).
- Config fiscal obligatoria para emitir → 409 con mensaje claro.
- Rangos de numeración variables, prefijo opcional (hoy vacío), vigencia `end_date`.
- Montos REAL 2 decimales; totales tomados de la respuesta Factus (fuente de verdad).
- Migración `0005_facturador.sql` (+ `0006_f3_invoices.sql`); rutas en `facturador-backend/src/routes/invoices.ts` (backend ya implementado; esta UI está pendiente).

**Open decisions (no inventar):**
- Orden exacto de acciones inline por rol en cada fila (admin vs usuario vs contador).
- Nivel de detalle de sugerencias de corrección para errores FAJ (código + sugerencia).
- Si "Nueva factura desde cotización" usa selector modal o flujo inline.
- Si el contador ve el mismo ledger o una vista simplificada "libro de ventas" (posible enlace a ContabilidadPage).
