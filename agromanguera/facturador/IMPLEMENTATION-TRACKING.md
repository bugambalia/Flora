# Seguimiento de Implementación — F6 a F8 + módulos planificados

> Estado general: COMPLETADO F6–F7, F9, F10 + UI + E2E. F8 en curso (falta review OWASP y `FACTUS_ENV=production`).
> Inicio: 2026-08-05
> Última actualización: 2026-10-05 (auditoría: 593 tests verdes, lint/typecheck limpios backend y frontend)
> Objetivo: Completar F6 (estadísticas), F7 (template + vista pública + email), UI F3/F4/F5, UI F6 + rol contador, F8 (hardening), F9 (turnos) y F10 (compras).

---

## Fase A — F6: Estadísticas (backend)
- [x] `src/routes/stats.ts` (summary, top-products, stock-alerts)
- [x] Montado en `routes/api.ts`
- [x] Tests `test/d1/stats.test.ts`
- [x] Verificación: lint + typecheck + tests en verde

## Fase B — F7: Template + Vista pública + Email factura
- [x] Migración `0007_public_invoices.sql` (public_token en invoices)
- [x] `templates/factura.html` / `factura.css` (+ temas compartidos en `templates/`)
- [x] `buildFacturaDocument()` + handler `GET /f/:token` en `routes/public.ts`
- [x] `isPublicPath()` incluye `/f/` en `auth.ts`
- [x] `EmailJob` con `docType` + consumer ramifica en `index.ts`
- [x] `public_token` + email al emitir en `routes/invoices.ts`
- [x] Tests (`test/d1/public-invoices.test.ts`, `public.test.ts`)

## Fase C — UI F3/F4/F5 (Facturas, Stock, Contabilidad)
- [x] `pages/FacturasPage.tsx`, `FacturaFormPage.tsx`, `FacturaDetailPage.tsx`
- [x] `pages/StockPage.tsx`
- [x] `pages/ContabilidadPage.tsx`
- [x] Router, AppShell nav, api.ts + domain.ts
- [x] `QuoteDetailPage` botón "Facturar", `QuoteFormPage` stock visible
- [x] E2E `facturas.spec.ts` + `stock.spec.ts` en verde

## Fase D — UI F6 + Rol contador
- [x] `PanelPage` con métricas de facturación
- [x] Rol `contador` en `auth.ts` + `accounting.ts` (solo lectura)
- [x] Tests de autorización (`test/d1/authz.test.ts`)

## Fase E — F8: Hardening + Lanzamiento
- [x] Paquete crítico OWASP (2026-10-07): emisión atómica en batch único (sin factura huérfana; folio duplicado → 409 reintentable), `cancelOrder` reversa stock con movimiento compensatorio, abono condicional anti-doble-pago, `closeShift` con guarda `status='abierto'`, validación `isFinite` + cotas en líneas, `FACTUS_MOCK` bloqueado en producción. Suite 627/627 + lint/typecheck limpios
- [x] Segundo paquete OWASP (2026-10-07): rate-limit de login solo cuenta fallos (éxito limpia buckets email+IP), `/catalogo` y `/clientes` con `RequireOperative`, bulk con cota 500 (413 backend + 2 MB/500 filas frontend), anti-bloqueo último admin (`false` y auto-degradación → 403). Suite 633/633 + lint/typecheck limpios (backend y frontend)
- [x] Refactor estructural (2026-10-07, 4 subagentes en paralelo): `lib/http.ts` central (json/guards/NOW/escapeLike, 16 archivos migrados), `TurnosPage` partida en `components/shifts/*` + invalidación de `shifts-report`, `CatalogoPage` partida en `pages/catalog/*` + queryKey única `['clients']`, `useLineItems`/`LineItemsEditor` compartido en las 3 form-pages, `lib/api.ts` y `lib/domain.ts` partidos con barrels estables. Suite 633/633 + lint/typecheck limpios (backend y frontend)
- [x] E2E completos (`flow`, `ajustes-f1`, `facturas`, `stock`, `turnos`, `usuarios`) — verde 2026-08-11
- [x] `docs/api/rest-api.md` actualizado
- [x] Email alerta stock + cron diario
- [ ] Review OWASP sobre el diff completo (pendiente)
- [ ] Switch `FACTUS_ENV=production` solo con resolución real en Factus (pendiente)
- [x] R2 activo (2026-10-07): buckets `facturador-logos` y `facturador-backups` creados, bloque `r2_buckets` en `wrangler.jsonc`, deploy OK (v8bfa5b93) con ambos bindings + cron mensual; `/api/health` responde `{"ok":true}`
- [ ] Onboarding dominio de email + dominio DNS propio + `ALLOWED_ORIGINS` del frontend desplegado

## Fase F9 — Turnos de caja + usuarios (PRD-TURNOS-Y-CAJA.md)
- [x] Migración `0009_shifts_caja.sql`
- [x] `routes/shifts.ts` (apertura, gastos, cierre con conciliación)
- [x] `routes/users.ts` (CRUD admin)
- [x] `invoices.ts` exige turno abierto (409 sin turno)
- [x] UI: `TurnosPage.tsx`, `UsuariosPage.tsx`, aviso en `FacturaFormPage.tsx`
- [x] E2E `turnos.spec.ts` + `usuarios.spec.ts`
- [x] v2.0 PRD: reporte consolidado de cierres / export CSV / arqueo imprimible

## Fase F10 — Órdenes de compra (PRD-ORDENES-COMPRA.md)
- [x] Migración `0008_purchase_orders.sql`
- [x] `routes/suppliers.ts`, `routes/purchase-orders.ts`
- [x] Vista pública `/oc/:token` + `OrdenesComprasPage`, `OrdenCompraFormPage`, `OrdenCompraDetailPage`, `ProveedoresPage`
- [x] Ingreso de stock al aprobar OC
- [x] Tests `purchase-orders.test.ts`, `suppliers.test.ts`

---

## Hallazgos de auditoría 2026-10-05

- **Documentación obsoleta**: README y este tracking decían "F6/F7/F8 planificadas, UI F3–F5 planificada, compras/turnos no implementados" mientras el código ya lo tenía todo. Corregido en ambos (2026-10-05).
- **Tests**: los 5 fails históricos de F4/F5 (BOM en CSV, pathname en stock) ya no existen — suite 593/593 verde.
- **Lint frontend**: los 3 warnings heredados (button.tsx, badge.tsx, auth-context.tsx) ya no aparecen; lint limpio.
- **Tracking inconsistente**: F9/F10 no figuraban aquí; agregadas. Los checkboxes sin marcar se completaron según estado real del código.
- **Pendiente real de producción**: ~~R2 comentado en `wrangler.jsonc`~~ ✅ R2 activo y desplegado (2026-10-07), ~~migraciones remotas reportadas hasta 0006~~ ✅ remota al día 0001–0010 (verificado 2026-10-07: tablas `shifts`/`suppliers`/`purchase_orders` + columnas `public_token`/`shift_id`). Queda: `FACTUS_ENV=sandbox` (requiere resolución DIAN), sin dominio de email/DNS propio.
- **Repo**: `agromanguera/facturador/` figura sin trackear en git (`??`); commitear para no perder el trabajo.
