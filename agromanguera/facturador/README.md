# Facturador Electrónico Agromanguera

Sistema de facturación electrónica DIAN que evoluciona el [Cotizador Agromanguera](../cotizador/): emisión de facturas de venta ante la DIAN (vía Factus API V2), rangos de numeración/resoluciones, inventario, contabilidad básica y estadísticas. La cotización sigue existiendo como flujo previo a la venta.

> **Regla dura del proyecto**: el facturador es una evolución del cotizador (`../cotizador/*`), pero **NO se toca ninguna parte del código, tests ni configuración del cotizador**. Todo cambio se hace en el facturador. Los problemas heredados del cotizador se resuelven aquí (ver [tickets.md](tickets.md)).

## Estructura del repositorio

| Ruta | Descripción |
|------|-------------|
| `facturador-backend/` | Worker de Cloudflare (TypeScript): API REST, D1, KV, Durable Objects, Email Service, cola `email-retry`, Browser Rendering (PDF), cron de backup. Módulos: auth, catálogo, clientes, cotizaciones, **facturas DIAN (Factus)**, **stock/inventario**, **contabilidad** |
| `facturador-frontend/` | SPA Vite + React + shadcn/ui (TypeScript): auth, catálogo, clientes, cotizaciones, facturas, stock, contabilidad, estadísticas, órdenes de compra, turnos/caja y gestión de usuarios |
| `PRD-FACTURADOR.md` | PRD principal: facturación DIAN (Factus V2), inventario, contabilidad, estadísticas |
| `PRD-ORDENES-COMPRA.md` | PRD: órdenes de compra, proveedores e ingresos de inventario (implementado en F10) |
| `PRD-TURNOS-Y-CAJA.md` | PRD: turnos de caja, conciliación de medios de pago, gastos y gestión de usuarios por admin (implementado en F9) |
| `DESIGN.md` | Design system: "The Validation Ledger", hereda identidad del cotizador |
| `PLAN-IMPLEMENTACION.md` | Plan por fases (F1 config fiscal → F8) + módulos planificados |
| `test-definicion-mvp.md` | Test de definición del alcance del MVP (completado) |
| `tickets.md` | Problemas preexistentes del cotizador que el facturador debe resolver o evitar |
| `test-empresa-facturador.md` | Test de alineación con la configuración real de la empresa |
| `brief-facturaspage.md`, `surface-facturas.md`, `PRODUCT.md` | Briefs y superficie de la página de facturas |

## Documentación de referencia

| Documento | Contenido |
|-----------|-----------|
| [PRD-FACTURADOR.md](PRD-FACTURADOR.md) | Requisitos del producto: Factus API V2, inventario, contabilidad, estadísticas |
| [PRD-ORDENES-COMPRA.md](PRD-ORDENES-COMPRA.md) | Requisitos del módulo de compras (implementado F10) |
| [PRD-TURNOS-Y-CAJA.md](PRD-TURNOS-Y-CAJA.md) | Requisitos de turnos/caja, gastos y usuarios admin (implementado F9) |
| [DESIGN.md](DESIGN.md) | Design system (tokens, estados fiscales, densidades) |
| [PLAN-IMPLEMENTACION.md](PLAN-IMPLEMENTACION.md) | Plan de implementación por fases (F1..F8) + módulos planificados |
| [test-definicion-mvp.md](test-definicion-mvp.md) | Alcance del MVP (decisiones 2026-08-03) |
| [tickets.md](tickets.md) | Hallazgos P1–Pn del cotizador: lo que NO se toca allá y cómo se resuelve acá |
| [facturador-backend/README.md](facturador-backend/README.md) | API del backend, setup local, deploy y pendientes de producción |
| [facturador-frontend/README.md](facturador-frontend/README.md) | Setup del frontend, scripts y pruebas E2E |

## Quickstart (5 pasos)

Requisitos: Node.js >= 20 y sesión de wrangler (`npx wrangler login`) para migraciones remotas.

```bash
# 1. Dependencias del backend
cd facturador-backend
npm install

# 2. Dependencias del frontend
cd ../facturador-frontend
npm install

# 3. Migraciones locales (ver "Flujo obligatorio" más abajo)
cd ../facturador-backend
rm -rf .wrangler/state/v3/d1 && npm run migrate:local

# 4. Seed del admin local (imprime la contraseña UNA sola vez; o usa ADMIN_PASSWORD=...)
npm run seed:admin

# 5. Desarrollo (dos terminales)
cd facturador-backend && npm run dev     # API en http://localhost:8787
cd ../facturador-frontend && npm run dev # SPA en http://localhost:5173
```

El frontend apunta por defecto a `http://localhost:8787` (ver `src/lib/api.ts` en el frontend); el backend solo acepta el origen `http://localhost:5173` (variable `ALLOWED_ORIGINS`).

## Scripts

### Backend (`facturador-backend/`)

| Script | Comando | Descripción |
|--------|---------|-------------|
| `dev` | `wrangler dev` | Worker local en `:8787` con D1/KV/DO locales |
| `deploy` | `wrangler deploy` | Despliega el Worker a producción |
| `migrate:local` | `wrangler d1 migrations apply facturador-db --local` | Aplica migraciones a la D1 local |
| `migrate:remote` | `wrangler d1 migrations apply facturador-db --remote` | Aplica migraciones a la D1 de producción |
| `seed:admin` | `node scripts/seed-admin.mjs` | Crea/resetea el admin en D1 local (contraseña aleatoria impresa una vez; `ADMIN_PASSWORD=...` para fijarla) |
| `seed:admin:remote` | `node scripts/seed-admin.mjs --remote` | Igual, contra D1 de producción |
| `lint` | `oxlint src` | Linter |
| `typecheck` | `tsc --noEmit` | Tipos (incluye tests) |
| `test` | `vitest run` | Suite de tests (pool de Workers) |

### Frontend (`facturador-frontend/`)

| Script | Comando | Descripción |
|--------|---------|-------------|
| `dev` | `vite` | Dev server en `:5173` |
| `build` | `tsc -b && vite build` | Typecheck + build de producción |
| `lint` | `oxlint` | Linter |
| `preview` | `vite preview` | Sirve el build local |
| `e2e:setup` | borra el estado D1 local, migra y siembra admin `admin@agromanguera.co` / `e2e-test-pass` | Preparación de los E2E (borra datos locales) |
| `test:e2e` | `npm run e2e:setup && playwright test` | E2E completos (levanta backend y frontend) |

## Verificación

Estado real (los números pueden variar con el avance del desarrollo):

### Backend

| Chequeo | Estado |
|---------|--------|
| `npm test` | 593 tests verdes (28 archivos) — verificado 2026-10-05 |
| `npm run typecheck` | Limpio (`tsc --noEmit`, incluye `test/`) |
| `npm run lint` | Limpio (`oxlint src`) |

Pendientes funcionales heredados del cotizador (ver [tickets.md](tickets.md)): P3 clientes (NIT/DV + campos DIAN no persistidos por `clients.ts`), P5/P6 (campos DIAN de `payment_methods` y `products` no expuestos en CRUD), P10 (reject sin `reason`, migración `next_reference` → rangos).

### Frontend

| Chequeo | Estado |
|---------|--------|
| `npm run build` (`tsc -b`) | OK |
| `npm run lint` | Limpio (`oxlint`) — verificado 2026-10-05 |

## Flujo obligatorio: migraciones locales

Al añadir una migración nueva (`migrations/000N_*.sql`), el estado local debe resetearse **antes** de correr migraciones y tests:

```bash
cd facturador-backend
rm -rf .wrangler/state/v3/d1
npm run migrate:local
```

Sin el `rm -rf`, el pool de vitest puede quedar con el schema viejo y los tests fallan de forma engañosa. El script `e2e:setup` del frontend ya aplica este mismo reseteo.

## Estado operativo (resumen)

Ya configurado en la cuenta de Cloudflare:

- D1 remota `facturador-db` (`3036e340-8da7-4630-94ee-927b607f592d`); migraciones locales aplicadas 0001–0010 (verificar `migrate:remote` antes de desplegar).
- KV `RATE_KV`, Durable Object `RateLimitDO` (rate limiting multi-instancia), cola propia `email-retry-facturador` con DLQ.
- `ALLOWED_ORIGINS=http://localhost:5173` y `PUBLIC_BASE_URL=https://facturador-backend.jhonier0301.workers.dev`.
- Secrets de Factus (`FACTUS_CLIENT_ID/SECRET/USERNAME/PASSWORD`, `FACTUS_ENV`) listos para el entorno; la emisión a producción requiere `FACTUS_ENV=production` y el software/resolución DIAN (ver checklist de producción en el README del backend).

Pendientes de producción (detalle en [facturador-backend/README.md](facturador-backend/README.md#checklist-de-pendientes-de-producción)):

- R2: crear buckets `facturador-logos` y `facturador-backups` y descomentar el bloque en `wrangler.jsonc`.
- Onboardear dominio de email (`wrangler email sending enable <dominio>`).
- Dominio DNS propio para el frontend y `PUBLIC_BASE_URL`.
- Añadir el origen del frontend desplegado a `ALLOWED_ORIGINS`.

## Fases

| Fase | Alcance | Estado |
|------|---------|--------|
| F1 | Config fiscal DIAN (company + rangos de numeración), migración 0005 | Implementada |
| F2 | Cliente Factus API V2 + Auth OAuth2 (`lib/factus.ts`, 52 tests unitarios) | Implementada |
| F3 | Emisión de facturas DIAN: `routes/invoices.ts` + `lib/factus-mapper.ts` (directa y desde cotización) | Implementada (backend + frontend) |
| F4 | Inventario: `routes/stock.ts` (movimientos, ajustes, alertas) | Implementada (backend + frontend) |
| F5 | Contabilidad: `routes/accounting.ts` (libro de ventas, cartera, export CSV) | Implementada (backend + frontend) |
| F6 | Estadísticas del panel (`routes/stats.ts` + PanelPage) | Implementada |
| F7 | Template factura híbrido + vista pública `/f/:token` | Implementada |
| F8 | Hardening + lanzamiento (E2E verde 6/6 en sandbox) | En curso: falta review OWASP y `FACTUS_ENV=production` |
| F9 | Turnos/caja + gastos + usuarios admin ([PRD-TURNOS-Y-CAJA.md](PRD-TURNOS-Y-CAJA.md)) | Implementado (backend + frontend, migración 0009) |
| F10 | Órdenes de compra + proveedores ([PRD-ORDENES-COMPRA.md](PRD-ORDENES-COMPRA.md)) | Implementado (backend + frontend, migración 0008) |

Toda la UI de facturas/stock/contabilidad/órdenes/turnos existe en `facturador-frontend/src/pages/`; la suite E2E (`flow`, `ajustes-f1`, `facturas`, `stock`, `turnos`, `usuarios`) está en verde según PLAN-IMPLEMENTACION (2026-08-11).
