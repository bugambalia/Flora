# PRD: Turnos de Caja y Administración de Usuarios Vendedores

> **Estado: PLANIFICADO** (no implementado) — 2026-08-05. Migración propuesta `0007_shifts_caja.sql` (la `0008` queda para purchase_orders del PRD-ORDENES-COMPRA.md). Requiere turno abierto para emitir facturas (`invoices.ts`) y CRUD de usuarios por admin (`users.ts`).

> Extiende `PRD-FACTURADOR.md` (facturación electrónica DIAN vía Factus, inventario, contabilidad básica). Este PRD cubre la operación de punto de venta: **turnos** (apertura/cierre de caja por vendedor), **conteo y conciliación** de dinero por medio de pago (efectivo, transferencia, tarjeta), **gastos con razón** durante el turno, y la **gestión de usuarios vendedores por el admin**.

## 1. Executive Summary

### Problem Statement

Hoy las ventas se facturan sin control de caja: no hay trazabilidad de cuánto dinero en efectivo, transferencia o tarjeta ingresó por vendedor, no existe registro de gastos ni conciliación, y cualquier persona con acceso puede operar sin rendir cuentas al cierre del día. Además, los usuarios se crean solo por seed script (`seed-admin.mjs`), sin que el admin pueda gestionar vendedores desde la interfaz.

### Proposed Solution

Introducir **turnos de caja obligatorios**: cada venta (factura directa o desde cotización) debe pertenecer a un turno abierto del vendedor que la emite. El turno se abre con un **fondo inicial en efectivo** y se cierra con un **conteo físico por medio de pago**; el sistema concilia lo esperado (ventas + gastos) contra lo contado y registra **faltantes/sobrantes**. El admin gestiona **usuarios vendedores** (crear, editar, activar/desactivar, resetear contraseña) desde una nueva sección de administración.

### Success Criteria

- El 100 % de las facturas (directas y desde cotización) requieren un turno abierto; sin turno activo la emisión se rechaza con `409 Turno no abierto`.
- El cierre del turno calcula y persiste `esperado`, `contado` y `diferencia` por medio de pago; un turno no puede cerrarse dos veces ni reabrirse.
- El 100 % de los gastos registrados en turno exigen una **razón** (texto 3–200 caracteres) y restan del efectivo esperado.
- Un admin puede crear un vendedor, asignarle contraseña inicial y desactivarlo sin borrar su historial (soft delete / flag `active`).
- Reporte de turno (apertura, ventas, gastos, conteo, faltante/sobrante) disponible en < 1.5 s con 500 facturas en D1 local.

## 2. User Experience & Functionality

### User Personas

| Persona | Rol | Necesidad |
|---------|-----|-----------|
| **Vendedor** | usuario | Abrir/cerrar su turno, facturar solo dentro de su turno, registrar gastos con razón, hacer conteo físico al cierre |
| **Admin** | admin | Crear/editar/desactivar vendedores, ver todos los turnos, consultar cierres y faltantes/sobrantes |

### User Stories

- **Story 1 (Apertura de turno):** As a vendedor, I want to open my turn indicating the opening cash so that the caja starts controlled.
  - **AC**:
    - El vendedor solo puede tener **un turno abierto** a la vez.
    - El campo `apertura_efectivo` es obligatorio (≥ 0).
    - Abrir un turno con otro ya abierto devuelve `409`.
    - El turno registra `abierto_por` (user id) y `abierto_at`.

- **Story 2 (Ventas dentro del turno):** As a vendedor, I want to invoice only while my turn is open so that every sale is assigned to my caja.
  - **AC**:
    - `POST /api/invoices` (directa) y el flujo desde cotización exigen `shift_id` del turno abierto del usuario; validación server-side, no solo UI.
    - La factura persiste `shift_id` y se computa en el `esperado` del turno.
    - Sin turno abierto → `409 Turno no abierto`.

- **Story 3 (Gastos con razón):** As a vendedor, I want to register an expense with a mandatory reason so that the caja count reflects money that left.
  - **AC**:
    - `POST /api/shifts/:id/expenses` exige `amount > 0`, `reason` (3–200 chars) y `created_at` default now.
    - El gasto resta del efectivo esperado del turno.
    - Solo puede registrar gastos el vendedor dueño del turno abierto.

- **Story 4 (Cierre con conteo):** As a vendedor, I want to close my turn entering the physical count per payment method so that the system reconciles the caja.
  - **AC**:
    - `POST /api/shifts/:id/close` exige conteos: `contado_efectivo`, `contado_transferencia`, `contado_tarjeta` (≥ 0).
    - El sistema calcula `esperado` (ventas por medio + fondo − gastos) vs `contado`, y persiste `faltante` / `sobrante` por medio y total.
    - El turno pasa a estado `cerrado`; cerrar de nuevo devuelve `409`.
    - Conteo de medios se deriva de `payment_methods.payment_method_code` (DIAN 12: 10=Efectivo, transferencia/tarjeta mapeados en la config de medios).

- **Story 5 (Gestión de usuarios):** As an admin, I want to create, edit, deactivate and reset sellers' passwords so that I control who operates.
  - **AC**:
    - CRUD bajo `POST/GET/PATCH/DELETE /api/users` (solo `isAdmin`).
    - Crear: nombre, email (único, case-insensitive), rol (`admin`|`usuario`), contraseña inicial (≥ 8 chars, pbkdf2).
    - Desactivar: flag `active = 0` (login rechazado, historial intacto), no borrado físico.
    - Reset de contraseña: nuevo hash sin tocar historial.
    - Un admin no puede desactivarse a sí mismo.

### Non-Goals

- NO se genera arqueo PDF ni impresión de cierre en esta release.
- NO hay multi-caja simultánea por vendedor (un vendedor = un turno a la vez).
- NO hay aprobación intermedia del admin en el cierre; la diferencia solo se registra (el bloqueo por tolerancia queda fuera de alcance).
- NO hay invitación por email ni recuperación de contraseña.
- NO se modifican las reglas DIAN de facturación existentes.

## 3. AI System Requirements

No aplica: módulo transaccional sin componentes de IA.

## 4. Technical Specifications

### Architecture Overview

Flujo principal (backend D1 + Workers):

```
Vendedor
  ├─ POST /api/shifts  (abrir) ──→ shifts: abierto, apertura_efectivo
  ├─ POST /api/invoices ──→ exige shift_id del turno abierto del usuario
  │       └─ invoice_payments: amount, payment_method_id, reference
  ├─ POST /api/shifts/:id/expenses ──→ expenses: amount, reason
  └─ POST /api/shifts/:id/close ──→ concilia esperado vs contado → faltante/sobrante

Admin
  └─ CRUD /api/users (admin only)
```

La conciliación del cierre es **cálculo server-side**: nunca confía en totales enviados por el cliente.

### Migración propuesta: `migrations/0007_shifts_caja.sql`

```sql
CREATE TABLE IF NOT EXISTS shifts (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL REFERENCES users(id),
  status TEXT NOT NULL DEFAULT 'abierto'
    CHECK (status IN ('abierto', 'cerrado')),
  apertura_efectivo REAL NOT NULL DEFAULT 0 CHECK (apertura_efectivo >= 0),
  abierto_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
  cerrado_at TEXT,
  -- conteo físico al cierre (por medio)
  contado_efectivo REAL,
  contado_transferencia REAL,
  contado_tarjeta REAL,
  -- conciliación
  faltante_total REAL NOT NULL DEFAULT 0,
  sobrante_total REAL NOT NULL DEFAULT 0,
  cerrado_por INTEGER,
  created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
  FOREIGN KEY (cerrado_por) REFERENCES users(id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_shifts_open_user
  ON shifts(user_id) WHERE status = 'abierto';

CREATE TABLE IF NOT EXISTS shift_expenses (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  shift_id INTEGER NOT NULL REFERENCES shifts(id) ON DELETE CASCADE,
  amount REAL NOT NULL CHECK (amount > 0),
  reason TEXT NOT NULL CHECK (length(reason) BETWEEN 3 AND 200),
  created_by INTEGER REFERENCES users(id),
  created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);
CREATE INDEX IF NOT EXISTS idx_shift_expenses_shift ON shift_expenses(shift_id);

-- asociar ventas al turno
ALTER TABLE invoices ADD COLUMN shift_id INTEGER REFERENCES shifts(id);
CREATE INDEX IF NOT EXISTS idx_invoices_shift ON invoices(shift_id);
```

### Integration Points

- **Nuevo módulo** `src/routes/shifts.ts` con `handleShiftsRequest`: apertura, gastos, cierre, listado (admin ve todos; usuario solo los suyos).
- **`src/routes/invoices.ts`**: `createInvoiceDirect` y `createInvoiceFromQuote` validan turno abierto y persisten `shift_id`.
- **Nuevo módulo** `src/routes/users.ts` (solo admin) o extensión de `auth.ts`; reutiliza el hasher pbkdf2 existente.
- **`src/routes/api.ts`**: enrutar `/api/shifts*` y `/api/users*` con la misma estructura de auth (`identity`).
- **Frontend** (`facturador-frontend`): sección "Turnos" (abrir/cerrar/conteo/gastos) y "Usuarios" (admin) bajo el layout autenticado existente.

### Security & Privacy

- Toda ruta de turnos y usuarios exige sesión válida; las de admin exigen `isAdmin` (mismo patrón `deleteInvoice` en `invoices.ts:793`).
- La validación de "dueño del turno" impide que un vendedor cierre o cargue gastos en turno ajeno.
- `active = 0` bloquea login sin borrar filas (auditoría intacta).
- Las contraseñas se hashean con pbkdf2 existente; nunca en claro ni en logs.
- La conciliación usa valores numéricos redondeados a 2 decimales; inputs validados (números finitos ≥ 0) para evitar NaN/negativos.

## 5. Risks & Roadmap

### Phased Rollout

- **MVP (release 1):** migración `0007`, `shifts.ts` (abrir/gastos/cerrar), validación en `invoices.ts`, `users.ts` CRUD admin, endpoints de listado, tests backend.
- **v1.1:** UI frontend de turnos (abrir/cerrar/conteo) y de usuarios (admin).
- **v2.0:** reporte de cierres consolidado por día, export CSV, impresión/PDF de arqueo.

### Technical Risks

- **Facturas históricas sin `shift_id`:** son `NULL`; el reporte debe tratarlas como "sin turno", no romper consultas agregadas.
- **Redondeo/float en D1:** usar `Math.round(x * 100) / 100` consistente con `invoices.ts:768` para evitar diferencias centavo.
- **Conteo por medio:** requiere que `payment_method_code` de los medios de pago existentes esté configurado; si falta, el medio se excluye del desglose con aviso en el cierre.
- **Concurrencia:** el índice único parcial `idx_shifts_open_user` previene doble turno abierto; cubrir con test de `409`.

## 6. Tests

- **Backend (vitest + D1 local, patrón `test/d1/*.test.ts`):**
  - Abrir turno ok / doble turno → `409`.
  - Factura sin turno abierto → `409 Turno no abierto`.
  - Gastos: `amount > 0`, `reason` 3–200, solo dueño del turno.
  - Cierre: conciliación correcta (esperado = apertura + ventas por medio − gastos), faltante/sobrante bien calculados, doble cierre → `409`.
  - Usuarios: solo admin; email único; desactivar bloquea login; no auto-desactivación.
- **Frontend (Playwright):** flujo E2E abrir turno → facturar → registrar gasto → conteo → cerrar; y CRUD de vendedor por admin.
- **Typecheck/lint** en verde antes de merge (regla del repo).
