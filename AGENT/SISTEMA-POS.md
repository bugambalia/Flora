## 1. Estructura de Módulos (Frontend & Backend)

### A. Módulo de Ventas (Alta Velocidad)

Este módulo prioriza la **latencia cero** y el manejo de estados múltiples.

* **Venta Multitarea:** Implementación de "Tabs" (Pestañas) en el frontend para manejar hasta 5 ventas simultáneas sin pérdida de datos (usando `Zustand` o `Redux` con persistencia en `IndexedDB`).
* **Cliente Express:** Botón de acceso rápido para "Consumidor Final" (NIT: 22.222.222).
* **Modificadores de Precio:** Función `apply_override()` que permite cambiar el `precio_unitario` en el `detalle_venta` solo para la sesión actual, sin afectar la tabla maestra de `productos`.
* **Control de Caja:** * `base_caja`: Registro de apertura.
* `gastos_caja`: Salidas menores (ej. limpieza, papelería).
* `arqueo_final`: Comparación automática entre (Base + Ventas - Gastos) vs. Efectivo real.


* **Impresión de Tirilla:** Generación de formatos POS (80mm/58mm) mediante Web Bluetooth o comandos `window.print()` optimizados para impresoras térmicas.

---

### B. Módulo de Inventario (Inteligencia de Stock)

Diseñado para la **trazabilidad y prevención de errores**.

* **Semáforo de Stock:** * 🔴 **Rojo:** Agotado / Sobre-vendido (Stock < 0).
* 🟡 **Naranja:** Bajo stock (Stock <= stock_minimo).
* 🟢 **Verde:** Stock saludable.


* **Monitor de Conflictos:** Cruce de datos entre `movimientos_inventario` y `detalle_ventas` para detectar discrepancias donde el stock físico no coincide con el digital.
* **Gestión de Pendientes:** Tabla de órdenes de compra con estado `RECIBIDO_PARCIAL` o `PENDIENTE` para productos que ya se pagaron pero no han llegado al almacén.

---

### C. Módulo de Contabilidad (Analítica y Exportación)

El cerebro financiero del ERP.

* **Dashboard de Rendimiento:** Cálculo en tiempo real de la "Semana más Productiva" comparando `SUM(total_venta)` agrupado por semana.
* **Cálculo de Ganancia Real:** * `Ganancia = SUM(detalle_ventas.utilidad_generada) - SUM(gastos_caja)`.
* **Motor de Reportes:** Uso de librerías como `jsPDF` (Frontend) o servicios en el Edge para generar PDFs que consoliden:
* Listado de Ventas vs. Compras.
* Top 10 productos con mayor margen de contribución.


* **Exportación Directa:** Botón para envío automático de reportes PDF vía API (ej. SendGrid o MailChannels desde Cloudflare Workers).

---

## 2. Plan de Desarrollo para el "Agente" (Backend Rust)

Para evitar los `if` largos, el backend usará un **Dispatcher de Comandos**. Cada petición al worker se procesará así:

```rust
// Ejemplo de la lógica limpia que pediste
pub async fn procesar_accion(accion: AccionReq, db: D1Database) -> Result<Response> {
    match accion {
        AccionReq::VentaRapida { items, cliente_id, descuento_global } => {
            // Lógica de venta con rollback automático si falla el stock
            ventas::ejecutar_transaccion(items, cliente_id, db).await
        },
        AccionReq::RegistrarGasto { monto, motivo } => {
            caja::registrar_salida(monto, motivo, db).await
        },
        AccionReq::GenerarInforme { mes } => {
            contabilidad::exportar_pdf(mes, db).await
        },
        _ => Err(Error::AccionNoPermitida)
    }
}

```

---

## 3. Checklist de Verificación de Estado (Paso a Paso)

| Paso | Tarea de Verificación | Estado |
| --- | --- | --- |
| **1** | **Auth Check:** ¿El usuario 'Ventas' tiene bloqueado el acceso a `/contabilidad`? | [ ] |
| **2** | **Multi-venta:** ¿Puedo abrir la Venta A, pasar a la Venta B y volver a la A sin borrar los productos? | [ ] |
| **3** | **Caja Check:** ¿El gasto de caja resta correctamente del saldo final del turno? | [ ] |
| **4** | **Consistency:** Si vendo el último producto, ¿el módulo de inventario se pone en rojo al instante? | [ ] |
| **5** | **Override:** ¿El cambio de precio en la venta afectó el precio global del producto? (Debe ser NO). | [ ] |
| **6** | **PDF:** ¿El informe mensual muestra la ganancia neta restando los costos de compra? | [ ] |
| **7** | **Performance:** ¿La respuesta del backend en Cloudflare es < 200ms? | [ ] |

---