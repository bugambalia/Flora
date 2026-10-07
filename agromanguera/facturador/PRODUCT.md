# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

El usuario principal es el equipo comercial y operativo de Agromanguera: **vendedores** que emiten facturas electrónicas (directas o desde cotización), abren/cierran su turno de caja, registran gastos con razón y consultan stock; y **admins** que configuran la identidad fiscal (NIT, régimen, rangos de numeración), administran límites de descuento por vendedor, crean/desactivan usuarios (contraseña inicial por admin), corrigen facturas rechazadas por la DIAN y gestionan stock.

El **contador** es usuario confirmado con rol propio de solo lectura: libro de ventas, cartera, pagos, IVA causado mensual, PDF/XML y export CSV.

El **cliente final** es audiencia secundaria: recibe por email el PDF legal de Factus y un enlace público `/f/:token` con QR/CUFE, pero no administra el sistema.

## Product Purpose

El Facturador Agromanguera evoluciona el Cotizador (misma identidad, repositorios propios) para emitir **facturas electrónicas de venta ante la DIAN vía Factus API V2**, con inventario, contabilidad básica y estadísticas en la misma release.

El producto existe para legalizar la venta: la cotización aceptada (o la venta directa, que es el flujo principal, < 50 % de ventas nacen de cotización) se convierte en factura con CUFE, número y resolución DIAN, sin doble digitación, sin vender sin stock y con cartera y libro de ventas al día.

El éxito significa que el equipo puede emitir, validar, cobrar y reportar facturas ante la DIAN con trazabilidad completa, que el stock baja atómicamente solo cuando la DIAN valida, y que el contador obtiene libro de ventas, IVA causado y cartera sin trabajo manual.

## Positioning

La diferencia central es que el documento legal (PDF de Factus + vista pública propia `/f/:token`) y el control operativo (estado DIAN, stock, cartera) nacen de la **misma transacción persistida**: la respuesta de Factus es la fuente de verdad, se guarda íntegra antes de responder al usuario, y la baja de stock es atómica con la validación — una factura rechazada no descuenta.

## Operating Context

El flujo esperado incluye login (sistema actual del cotizador), onboarding fiscal obligatorio para emitir (409 si falta config), configuración de rangos de numeración **variables y editables** (hoy facturan desde 0 sin prefijo; formatos típicos `FAC-0000` / `AGRO-0000`), emisión directa o desde cotización `aceptada`, estado DIAN (`pendiente | validada | rechazada | anulada`), sync manual, corrección de pendientes (solo admin), notas crédito por devoluciones (flujo frecuente), recibos de caja/anticipos, pagos parciales, alertas de stock bajo y cron diario con reconciliación de emisiones huérfanas.

> **Turnos de caja (confirmado 2026-08-05)**: toda venta se hace dentro de un turno de caja abierto por el vendedor (apertura con fondo de efectivo, conteo físico por medio al cierre, faltante/sobrante registrado sin bloquear, gastos con razón obligatoria que restan del efectivo). El admin crea los usuarios vendedores (contraseña inicial definida por admin). Detalle en `PRD-TURNOS-Y-CAJA.md`.

El contexto documental es Colombia: COP, español es-CO, `Intl.NumberFormat('es-CO')`, NIT con DV, régimen común, IVA 19 %/5 %/exento, DIVIPOLA, CIIU, CUFE/QR y códigos FAJ en español. El consumidor final se factura al cliente especial con NIT **222.222.222** (no hay ventas sin NIT).

La emisión exige rol `admin` o `usuario` (vendedores con límites de descuento administrados por admin y turno de caja abierto al emitir); contador con rol `contador` de solo lectura; configuración fiscal y rangos solo `admin`.

## Capabilities and Constraints

MVP (definido por `test-definicion-mvp.md`, completado 2026-08-03; **estado backend al 2026-08-05: F1–F5 implementadas**):

- Facturas electrónicas de venta ante DIAN vía Factus V2: desde cotización `aceptada` y **directas (flujo principal)**, con `reference_code` idempotente, persistencia íntegra de la respuesta Factus, estado DIAN, sync manual, DELETE de pendientes. **Implementado** (`routes/invoices.ts`).
- **Nota crédito en el MVP** (devoluciones frecuentes): `credit_notes`, `related_documents`, devolución de stock. *(Solo esquema `0005`; rutas pendientes.)*
- **Consumidor final en el MVP** vía cliente especial con NIT 222.222.222.
- **Recibos de caja / anticipos en el MVP** (`prepayment_details`, tabla `receipts`). *(Solo esquema `0005`; rutas pendientes.)*
- Retenciones de IVA/renta: **NO en el MVP** (v1.1), campos mapeados en el cliente Factus.
- Inventario: stock único (bodegas en v2), movimientos auditados (`created_by`), baja atómica con la validación, política por producto (`allow_oversell`) + permisos por rol, alertas por producto (`stock_min`). **Implementado** (`routes/stock.ts`: `movements`, `adjust` admin, `stock-alerts`); alertas por email/cron y reconciliación de huérfanas pendientes.
- Contabilidad: libro de ventas con **IVA causado por mes**, cartera por cliente, pagos parciales múltiples, export CSV, PDF/XML por factura (rol contador). **Implementado** (`routes/accounting.ts`); requisitos adicionales del contador (5.3) pendientes de detallar → v1.1.
- Config fiscal: NIT+DV, DIVIPOLA, CIIU, régimen común, prefijo por rango **opcional/vacío**, rangos de numeración CRUD (crear/modificar, vigencia por `end_date`), sync con `GET /v2/numbering-ranges`. **Implementado** (migraciones `0001`–`0006`; `PATCH /api/numbering-ranges/:id`).
- Roles: `admin`, `usuario` (vendedor con límite de descuento), `contador` (solo lectura). *(Implementado: backend bloquea escritura de contador en facturas/cotizaciones/clientes/turnos/compras; frontend oculta CTAs y guarda rutas.)*
- Estadísticas: ventas del mes, ingresos por método de pago, top productos, estado DIAN, cartera, alertas. **Pendiente (F6)**.
- Email propio del sistema (pipeline existente `sendOrQueue`) con PDF legal de Factus + enlace `/f/:token`; vista pública con CUFE/QR/resolución. **Pendiente (F7)**.
- Montos almacenados como `REAL` 2 decimales (los totales se toman de la respuesta Factus, no de recálculo local). Migraciones `0001`–`0006`.
- Volumen 50–200 facturas/mes; plan Factus COP 220 000/mes ≈ 1 600 facturas; producción objetivo **en semanas**; software DIAN aún no registrado (se tramita en F1, bloquea producción no sandbox).
- Sin webhooks de Factus (sync manual + re-consulta), sin contabilidad formal (PUC), sin múltiples establecimientos, sin inventario por bodega, sin doble moneda.

**Fuera de este MVP (PRDs propios, planificados)**:
- **Turnos de caja + gastos + gestión de usuarios** → `PRD-TURNOS-Y-CAJA.md` (migración `0007_shifts_caja.sql`).
- **Órdenes de compra** → `PRD-ORDENES-COMPRA.md` (migración `0008_purchase_orders.sql`).

## Brand Commitments

El nombre confirmado del producto es **Facturador Agromanguera** (evolución del Cotizador Agromanguera; repositorios propios).

La voz es la misma del cotizador: profesional, clara, orientada a operaciones comerciales, en español colombiano.

**Compromiso visual confirmado:** el facturador **hereda la identidad visual del cotizador** (mundo "Deployable Sheet": Blueprint Ink `#10243E`, Lock Foil `#C8A44D`, Valley Blue `#A9D6E5`, Matte Sheet `#F8F7F0`, tipografía Archivo/Inter/IBM Plex Mono, facetas plegadas) y adopta **la densidad y patrones Operate de los sistemas de facturación colombianos de referencia (Alegra POS / Siigo POS)** para el shell de trabajo: tablas densas, filtros, badges de estado DIAN, sellos. El documento legal se diferencia del PDF genérico de Factus con la vista pública propia.

## Evidence on Hand

- `PRD-FACTURADOR.md`: alcance, arquitectura, mapeo de campos Factus, riesgos, roadmap F1–F8.
- `PLAN-IMPLEMENTACION.md`: plan por fases con archivos y rutas (estado 2026-08-05: F1–F5 implementadas en backend).
- `PRD-TURNOS-Y-CAJA.md`: turnos de caja, gastos, faltante/sobrante y gestión de usuarios (planificado).
- `PRD-ORDENES-COMPRA.md`: órdenes de compra a proveedores (planificado).
- `tickets.md`: problemas heredados del cotizador y hallazgos F1–F5 resueltos/pendientes.
- `test-empresa-facturador.md`: cuestionario de alineación con el negocio (35 preguntas).
- `test-definicion-mvp.md`: **completado** — decisiones selladas del MVP (referencia autoritativa de alcance).
- Incumbente visual y stack en `../cotizador` (`DESIGN.md`, `cotizador-frontend`, `cotizador-backend`, `templates/documento.html`).
- Referencias operativas: sistemas colombianos de facturación POS (Alegra POS, Siigo POS) para densidad de operación.
- No se deben inventar testimonios, clientes, métricas comerciales, números de resolución DIAN ni claims de adopción. El número de resolución y datos fiscales reales los entrega el contador (semana en curso).

## Product Principles

- **La respuesta de Factus es la fuente de verdad**: se persiste íntegra (CUFE, número, errores, links) antes de responder al usuario; los totales nunca se recalculan por el lado local.
- **Una factura rechazada no toca el inventario**: la baja de stock es atómica con la validación DIAN; reintentar no duplica (`reference_code` idempotente).
- **La venta directa manda**: es el flujo principal de la UI porque < 50 % de las ventas nacen de cotización.
- **Rangos de numeración variables y auditables**: prefijo opcional (hoy vacío), CRUD completo, vigencia controlada — la realidad fiscal de Agromanguera es flexible, no un solo rango fijo.
- **Las reglas colombianas confirmadas** (COP, es-CO, NIT+DV, DIVIPOLA, CIIU, régimen común, IVA, CUFE/FAJ) son parte del producto, no formato superficial.

## Accessibility & Inclusion

Sin requisitos de accesibilidad específicos del producto más allá de los del cotizador. El contador (rol solo lectura) es el único perfil con necesidades distintas confirmadas: tablas densas, export CSV y descarga de documentos deben ser utilizables desde navegador de escritorio sin plugins.
