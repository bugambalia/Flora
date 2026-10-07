# Surface brief — FacturasPage (emisión y estado DIAN)

## Scope and mode

Superficie principal del facturador: lista de facturas (ledger), emisión directa y desde cotización aceptada, detalle con estado DIAN/CUFE/PDF/XML, sync manual, corrección de pendientes, pagos. **Modo: Operate.**

## Audience, job, action

- **Vendedor** (admin+usuario, límites de descuento): emite facturas directas (flujo principal, < 50 % de ventas nacen de cotización) o desde cotización aceptada; ve el estado DIAN y el stock disponible al vender.
- **Admin**: corrige/re-emite rechazadas, borra pendientes, sync manual.
- **Contador** (solo lectura): consulta, descarga PDF/XML, exporta.
- **Cliente final** (fuera de esta superficie): recibe email con PDF + enlace `/f/:token`.

Tarea: "vender y legalizar" — convertir una venta en factura DIAN válida en el menor tiempo posible, sin duplicados, y saber su estado exacto.

## Proof / content

- Ledger con número/prefijo (mono), cliente, fecha, total COP (tabular), método de pago, due, estado stamp, acciones.
- Estado DIAN visible siempre: `pendiente | validada | rechazada | anulada` (sellos con color funcional).
- Detalle: CUFE, resolución/rango, errores FAJ en español, PDF/XML, links públicos.
- Idempotencia por `reference_code`; una rechazada no descuenta stock.

## Constraints

- Requiere config fiscal completa para emitir (409 con mensaje claro si falta; first-run como coach).
- Roles: emisión/sync/delete/pagos = admin (y usuario con límites); lectura = contador.
- Rangos de numeración variables, prefijo opcional (hoy vacío), vigencia por `end_date`.
- Flujo híbrido de documento: PDF legal de Factus + vista propia `/f/:token` (talonario + DIAN).
- Hereda tokens/shell del cotizador (Blueprint Ink, Lock Foil, mono, facetas).

## Chosen direction

Mundo heredado "Deployable Sheet" re-tuneado a Operate ("The Validation Ledger"): lista como ledger denso (sin tarjetas), filtros como franja instrumental, dos paneles para emisión (ítems | totales+estado), sellos de estado, CUFE/mono siempre exactos. Referencia de densidad: Alegra POS / Siigo POS.

## Memorable moment

El "stamp settle" al sincronizar: cuando `sync` confirma la validación DIAN, el estado pasa a `validada` con el sello foil asentándose y el total band fija la cifra legal — el instante en que la venta queda legalizada.

## Unresolved decisions

- Columna/orden exacto de acciones por rol en el ledger (admin vs usuario vs contador).
- Nivel de detalle del detalle de errores FAJ (código + sugerencia de corrección).
- Paginación vs scroll infinito con 50–200 facturas/mes.
