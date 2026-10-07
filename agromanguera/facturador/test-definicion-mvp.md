# Test de definición del MVP — Facturador Electrónico Agromanguera

> **Estado: COMPLETADO** — respondido el 2026-08-03. Las respuestas marcadas definen el alcance del MVP y actualizan `PRD-FACTURADOR.md` y `PLAN-IMPLEMENTACION.md`. Los ítems en **v1.1+** no se implementan en esta release.

## Reglas de uso

1. Responde **una sola opción** por pregunta (marca con `[x]`).
2. "No sé" **no bloquea** el desarrollo: aplica el default indicado y queda anotado en §7.
3. Al terminar, completa §8 (Definición del MVP) con el resultado — ese documento es el contrato de alcance para el equipo.
4. Si una respuesta cambia de alcance (amplía/corta), se actualiza `PRD-FACTURADOR.md` y `PLAN-IMPLEMENTACION.md` antes de empezar la fase afectada.

---

## 1. Alcance documental del MVP

**1.1 Nota crédito (devoluciones/ajustes a facturas) — ¿en el MVP?**
- [x] **Sí, en el MVP** — **amplía F3**: tabla `credit_notes`, `related_documents`, endpoint de emisión y baja de stock inversa (devolución de entrada)
- [ ] No, en v1.1 (default PRD)

**1.2 Consumidor final (venta sin NIT) — ¿en el MVP?**
- [x] **Sí, en el MVP** — modalidad DIAN de **consumidor final con identificación 222.222.222**: no hay ventas sin NIT reales; el cliente sin documentación se factura al cliente especial "Consumidor final" con NIT 222.222.222
- [ ] No, en v1.1 (default)

**1.3 Retenciones (IVA/renta) en factura — ¿en el MVP?**
- [x] **No, en v1.1** (default) — campos mapeados pero ocultos en UI
- [ ] Sí, en el MVP

**1.4 Recibos de caja / anticipos — ¿en el MVP?**
- [x] **Sí** — **amplía F5**: `prepayment_details` en payload Factus, tabla `receipts`
- [ ] No (default)

**1.5 Facturación directa sin cotización — ¿en el MVP?**
- [x] **Sí** (default) — ventas directas entran al sistema
- [ ] No — solo desde cotización `aceptada`

**1.6 ¿Cuál es el % real de ventas que nacen de cotización?**
- [ ] ~100 %
- [ ] 50–90 %
- [x] **< 50 %** → **la factura directa es el flujo principal de la UI** (mayor prioridad en F3 que la factura desde cotización)

---

## 2. Config fiscal y DIAN

**2.1 Régimen de Agromanguera**
- [x] **Responsable de IVA — régimen común**
- [ ] Régimen simplificado

**2.2 Resolución de numeración vigente**
- [x] **Sí, existe** — **requisito clave confirmado**: el sistema debe permitir **crear y modificar rangos de numeración fácilmente** (rangos variables). Formato típico `FAC-0000` o `AGRO-0000`, pero **actualmente se factura desde 0 sin prefijo** → el prefijo debe ser **opcional/editable** por rango
- [ ] No, hay que tramitarla
- [ ] No sé

**2.3 Registro del software ante la DIAN (Res. 000165/2023)**
- [ ] Ya registrado
- [x] **No → se tramita en F1** (guía Factus). No bloquea desarrollo; **es el camino crítico para la fecha objetivo de producción**
- [ ] No sé

**2.4 Datos fiscales base**
- [x] **Los entrega el contador esta semana** → se incorporan directamente en F1
- [ ] Aún no los tenemos → placeholders en desarrollo

**2.5 Clientes: ¿qué % no tiene NIT (personas naturales)?**
- [ ] 0 %
- [ ] < 10 %
- [x] **> 10 %** → refuerza 1.2: el flujo consumidor final (222.222.222) es prioridad en el MVP

**2.6 ¿Venden a clientes extranjeros?**
- [x] **No**
- [ ] Sí, ocasional
- [ ] Sí, con frecuencia

**2.7 Prefijo y formato de numeración de facturas**
- [ ] `FEV-####` (default PRD — **DESCARTADO**)
- [x] **Prefijo variable por rango, actualmente vacío** → cambiar default del PRD: `factura_prefix` nullable, editable por rango de numeración (ver 2.2)

---

## 3. Proceso operativo y roles

**3.1 ¿Quién emite facturas?**
- [ ] Solo admin
- [x] **Admin + vendedores** — con **límites de descuento por vendedor administrados por el admin** (mismo mecanismo que cotizaciones) → amplía F1: campo `discount_limit` por vendedor y validación en emisión

**3.2 ¿Quién configura datos fiscales y rangos?**
- [x] **Solo admin** (default)
- [ ] El contador también

**3.3 El contador, ¿necesita acceso al sistema?**
- [x] **Sí, solo lectura** (facturas, PDF/XML, libro de ventas, cartera) → **se crea rol `contador` en F1**
- [ ] No, recibe archivos

**3.4 ¿Quién corrige y re-emite una factura rechazada por la DIAN?**
- [ ] El vendedor que la emitió
- [x] **Solo admin** (default)

**3.5 El email de la factura, ¿quién lo envía?**
- [x] **Nuestro sistema** (default) — pipeline existente, con PDF legal de Factus adjunto/enlace
- [ ] Factus (`send_email: true`)

**3.6 El cliente final recibe:**
- [x] **Email con PDF + enlace público `/f/:token`** (default PRD)
- [ ] Solo PDF por email
- [ ] No sé

---

## 4. Inventario

**4.1 ¿Controlan inventario hoy?**
- [ ] No se controla
- [x] **Manual (Excel/cuaderno)** → el archivo se usa como **base de carga inicial** en F4
- [ ] Otro software

**4.2 Política de venta sin stock suficiente**
- [x] **Depende de producto y permisos** → modelo **per-producto + permisos por rol**: cada producto tiene `allow_oversell` (bloquear/avisar), y el admin decide qué vendedores pueden emitir sobre stock bajo → amplía F4 y F1 (permisos)
- [ ] Bloquear emisión (default)
- [ ] Avisar pero permitir

**4.3 Devoluciones y mermas — ¿quién y cómo?**
- [ ] No aplica en MVP
- [ ] Ocasional → ajuste manual
- [x] **Frecuentes → flujo completo**: ajustes manuales con motivo + **devoluciones vinculadas a factura vía nota crédito (1.1)** con devolución de stock → amplía F3/F4

**4.4 Inventario único o por bodega**
- [x] **Único en el MVP** — el default del PRD se mantiene; inventario por bodega se documenta para v2
- [ ] Por bodega

**4.5 Conteo físico inicial (stock de arranque)**
- [x] **Existe y se entrega** → carga inicial en F4 con ajustes auditados
- [ ] Hay que hacer el conteo

**4.6 Umbral de alerta de stock**
- [x] **Por producto** (`stock_min` individual, default)
- [ ] Valor global

**4.7 Métodos de pago usados hoy**
- [x] **Efectivo**
- [x] **Transferencia/nequi/daviplata**
- [x] **Tarjeta (datafono)**
- [x] **Crédito a plazo — plazo variable por factura** (`due_date` por factura, no fijo por método)

---

## 5. Contabilidad básica

**5.1 Qué necesita ver el contador (marcar todos — los marcados entran al MVP)**
- [x] **Libro de ventas** (facturas + IVA discriminado por tarifa)
- [x] **Cartera por cliente** (saldo pendiente)
- [x] **Registro de pagos contra facturas**
- [x] **Export CSV**
- [x] **Descarga PDF y XML de cada factura**
- [x] **Total de IVA causado por mes** (para declaración) → se agrega al libro de ventas/reporte mensual
- [ ] Ninguno de los anteriores

**5.2 Pagos: ¿parciales múltiples por factura?**
- [x] **Sí** (default — `invoice_payments` ya lo soporta)
- [ ] Un solo pago por factura
- [ ] No se vende a crédito

**5.3 Alcance mínimo confirmado del contador**
- [ ] Libro de ventas + cartera + pagos + CSV cubren lo necesario
- [x] **Se necesita más** → el alcance 5.1 entra al MVP, pero el contador requiere funcionalidad adicional (documentar requisitos concretos en v1.1/v2 — **pendiente de recoger detalles con el contador**; candidatos típicos: PUC, comprobantes, conciliación)

---

## 6. Factus: volumen, presupuesto y credenciales

**6.1 Volumen mensual de facturas**
- [ ] < 10
- [ ] 10–50
- [x] **50–200**
- [ ] > 200

**6.2 ¿Hay cuenta Factus o credenciales?**
- [x] **No** (F1 incluye la gestión)
- [ ] Sí, sandbox
- [ ] Sí, producción

**6.3 Presupuesto mensual para Factus**
- [ ] < COP 100 000
- [ ] COP 100 000–300 000
- [x] **Plan definido: COP 220 000/mes ≈ 1 600 facturas** (Factus vende por paquetes de facturas hasta agotar el límite). Suficiente para el volumen 6.1; monitorear consumo mensual
- [ ] No sé

**6.4 Fecha objetivo para facturar en producción**
- [x] **Ya (semanas)** → urgencia alta: se prioriza F8; el registro del software DIAN (2.3) y los datos fiscales (2.4) son el camino crítico
- [ ] 2–3 meses
- [ ] Sin prisa

---

## 7. Decisiones técnicas (equipo — selladas)

**7.1 Migración**: `0005_facturador.sql` (PRD §4 dice `0004` — **errata corregida**, `0004` ya existe) — [x] Confirmado

**7.2 Montos en D1**:
- [x] REAL 2 decimales (los totales se toman de Factus)
- [ ] INTEGER centavos

**7.3 Email de facturas**: reutilizar `sendOrQueue()` existente — [x] Confirmado

**7.4 ID de factura pública**: token UUID v4 inmutable, ruta `/f/:token` — [x] Confirmado

**7.5 Baja de stock**: en la misma transacción D1 que marca `validada`; `rechazada`/`pendiente` no descuentan — [x] Confirmado

**7.6 Reconciliación de emisiones huérfanas** (Factus validó pero el Worker murió antes de persistir):
- [x] **Se agrega al MVP** (job en el cron diario: reconsultar `GET /v2/bills` por `reference_code` pendientes y reconciliar)
- [ ] Se difiere a v1.1

---

## 8. Definición del MVP resultante (completada)

| Área | Decisión MVP | Default si "No sé" |
|---|---|---|
| Alcance documental | Nota crédito: **SÍ** · Consumidor final: **SÍ (222.222.222)** · Retenciones: **v1.1** · Recibos/anticipos: **SÍ** · Directa sin cotización: **SÍ (flujo principal)** | Ninguno de los extra; directa: sí |
| Config fiscal | Régimen: **común** · Resolución: **existe (rangos variables, prefijo opcional/vacío)** · Software DIAN: **tramitar en F1** · Prefijo: **editable, actualmente vacío** | `comun` · tramitar · tramitar · `FEV` (**descartado**) |
| Roles | Emisión: **admin + vendedores (límites de descuento por vendedor)** · Config fiscal: **solo admin** · Rol contador: **SÍ, solo lectura (F1)** · Corrección rechazos: **solo admin** | solo admin · solo admin · no · solo admin |
| Email/visual | Envío: **nuestro sistema** · Vista pública: **sí, `/f/:token`** · Estilo: **híbrida (PRD)** | nuestro sistema · sí · híbrida |
| Inventario | Política sin stock: **per-producto (`allow_oversell`) + permisos por rol** · Mermas: **flujo completo (nota crédito + ajustes)** · Conteo: **existe, se entrega** · Umbral: **por producto** · Bodegas: **únicas en v2** | bloquear · ajuste manual · hacer conteo · por producto |
| Contabilidad | Alcance: **libro de ventas + IVA causado + cartera + pagos + CSV + PDF/XML (rol contador)** · Pagos parciales: **sí, múltiples** · Extra: **documentar en v1.1** | 5.1 marcados · sí |
| Factus | Volumen: **50–200/mes** · Credenciales: **ninguna aún** · Plan: **COP 220 000 ≈ 1 600 facturas** · Fecha: **ya (semanas)** | 10–50 · no · cotizar · 2–3 meses |
| Técnicas | Migración **0005** · Montos **REAL** · Reconciliación **en MVP** | — |

**Resultado del MVP final** (resumen que sustituye al PRD §2 para el equipo):
```
MVP v1.0 incluye:
- Facturas electrónicas de venta ante DIAN vía Factus (desde cotización aceptada y directas;
  la directa es el flujo principal)
- Nota crédito y recibos de caja/anticipos (amplían F3/F5 del plan original)
- Consumidor final vía cliente especial con NIT 222.222.222
- Estados DIAN, CUFE/PDF/XML, sync manual, corrección de pendientes (solo admin)
- Config fiscal (F1) con rangos de numeración variables y prefijo opcional
- Roles: admin, vendedores (límites de descuento), contador (solo lectura)
- Inventario (F4) con política por producto + permisos, devoluciones vía nota crédito,
  alertas por producto; stock único (bodegas en v2)
- Contabilidad básica (F5): libro de ventas con IVA causado, cartera, pagos parciales,
  CSV, PDF/XML; recibos/anticipos
- Estadísticas (F6); email con PDF + vista pública /f/:token (F7)
- Reconciliación de emisiones huérfanas en el cron diario (nuevo, recomendado)
NO incluye (v1.1+): retenciones en UI, inventario por bodega, PUC/contabilidad formal,
requisitos adicionales del contador (5.3, por definir), webhooks Factus
Riesgos aceptados:
- Software DIAN y resolución en trámite → bloquean producción, no sandbox (fecha: semanas)
- "Se necesita más" del contador (5.3) no está detallado → recoger con el contador
```

---

## 9. Cambios derivados que se deben aplicar a PRD y PLAN

> Revisión 2026-08-05: los ítems implementados en el backend (F1–F5) quedan marcados y ya constan en PRD/PLAN/PRODUCT actualizados. Los pendientes corresponden a rol `contador`, cron de reconciliación, E2E sandbox y requisitos del contador.

- [x] PRD §4: corregir migración `0004_facturador.sql` → `0005_facturador.sql` (real: `0005_facturador.sql` + `0006_f3_invoices.sql`)
- [x] PRD §4: `company_settings.factura_prefix` → **nullable/editable por rango** (sin default `FEV`; `0005` lo define `TEXT` nullable, vacío = sin prefijo)
- [x] PRD §4: `numbering_ranges` → gestión CRUD completa de rangos variables (crear/modificar, prefijo opcional, vigencia) — requisito del cliente (`PATCH /api/numbering-ranges/:id` implementado)
- [x] PRD §3/§5 (Non-Goals): mover nota crédito y recibos de caja/anticipos de v1.1 a **MVP**; mover consumidor final de v1.1 a **MVP** (modalidad 222.222.222)
- [x] PRD §4: nueva tabla `credit_notes` + `receipts` en la migración 0005 (solo esquema; rutas pendientes)
- [x] PRD §4: `users`/vendedores → campo `discount_limit` (límite de descuento administrado por admin) — ya existía en `0001_init.sql`
- [x] PRD §4: nuevo rol `contador` (solo lectura) en F1 — **implementado** (backend: `auth.ts` + bloqueos en `invoices`/`quotes`/`clients`/`shifts`/`suppliers`/`purchase-orders` + lectura en `accounting`/`stats`; frontend: guards `RequireOperative`/`RequireStrictAdmin`, nav y CTAs; tests en `authz.test.ts`)
- [x] PRD §4: `products.allow_oversell` (política por producto) + permisos de emisión sobre stock bajo por rol — implementado
- [ ] PRD §3: fixture de consumidor final 222.222.222 en el set de 12 casos — pendiente (F8 E2E sandbox)
- [ ] PRD §5 (F5): añadir "IVA causado por mes" al libro de ventas — pendiente de confirmar agrupación mensual en `sales-ledger`
- [ ] PRD §5 (F3): añadir job de **reconciliación de emisiones huérfanas** al cron diario — pendiente
- [x] PLAN: F3 con flujo directo como prioridad UI; F3/F5 ampliadas por nota crédito y recibos; F1 con rol contador y límites por vendedor; F4 con `allow_oversell` + devoluciones vía nota crédito; F8 con urgencia "semanas" (camino crítico: registro software DIAN) — reflejado en PLAN actualizado (estado 2026-08-05)
- [ ] Recoger con el contador: requisitos adicionales de 5.3 para v1.1 (pendiente)
