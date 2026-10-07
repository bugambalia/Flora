# Test de alineación — Facturador Electrónico Agromanguera

> Cuestionario para el **dueño/equipo comercial** (secciones A, C, D, E, F) y el **contador** (secciones B, F). Responde marcando las opciones; cada respuesta orienta el alcance del PRD (`PRD-FACTURADOR.md`) y del roadmap. Si no sabes una respuesta fiscal, márcala "No sé" — no bloquea el desarrollo, solo el paso a producción.

---

## A. Alcance y urgencia

1. ¿Por qué se necesita el facturador ahora?
   - [ ] Obligación DIAN pendiente o próximo cumplimiento
   - [ ] Evitar doble digitación (hoy se factura aparte, manual o en Excel)
   - [ ] Profesionalizar la venta (documentos oficiales con CUFE/QR)
   - [ ] Ordenar inventario y saber qué hay disponible al vender
   - [ ] Otro: ____

2. ¿Qué documentos necesita emitir Agromanguera? (marca todos los que apliquen hoy)
   - [ ] Factura de venta electrónica
   - [ ] Nota crédito (devoluciones/ajustes a facturas)
   - [ ] Consumidor final (ventas **sin NIT** del cliente)
   - [ ] Retenciones de IVA/renta en factura
   - [ ] Recibo de caja / anticipos
   - [ ] Documento soporte (compras a no obligados a facturar)

3. ¿Con qué frecuencia facturan?
   - [ ] Menos de 10 al mes
   - [ ] 10–50 al mes
   - [ ] 50–200 al mes
   - [ ] Más de 200 al mes

4. ¿De qué forma venden?
   - [ ] Todo de contado
   - [ ] Mayormente a crédito → plazo típico: ____ días
   - [ ] Mixto → aproximadamente ____ % de las ventas a crédito

5. ¿Toda venta nace de una cotización del sistema?
   - [ ] Sí, siempre pasan por cotización
   - [ ] No, hay ventas directas que hoy se facturan sin cotización
   - [ ] Depende del vendedor

---

## B. Identidad fiscal (responder con el contador)

6. Régimen de Agromanguera:
   - [ ] Responsable de IVA — régimen común
   - [ ] Régimen simplificado
   - [ ] No sé

7. ¿Tienen **resolución de numeración** DIAN vigente (número de resolución, prefijo, rango y vigencia)?
   - [ ] Sí → número de resolución: ____ · prefijo: ____ · rango: ____ → ____ · vigencia: ____
   - [ ] No, hay que tramitarla
   - [ ] No sé

8. Datos para la configuración fiscal (llénalos con tu contador):
   - NIT sin formato (solo dígitos): ____________
   - Dígito de verificación (DV): ____
   - Código CIIU actividad principal: ________
   - Municipio del establecimiento (departamento + municipio): ____________
   - Dirección, teléfono y correo fiscal: ____________

9. ¿Está registrado el **software de facturación** ante la DIAN (Res. 000165/2023)?
   - [ ] Sí
   - [ ] No
   - [ ] No sé → Factus guía este trámite; lo incluimos en el plan (F1)

10. ¿Qué porcentaje de sus clientes **no tiene NIT** (personas naturales, consumidor final)?
    - [ ] 0 %
    - [ ] Menos del 10 %
    - [ ] Más del 10 %

11. ¿Vende a clientes extranjeros (exportaciones)?
    - [ ] No
    - [ ] Sí, ocasionalmente
    - [ ] Sí, con frecuencia

---

## C. Personalización (dirección visual)

12. Para la **cotización** (documento actual, estilo "lámina técnica" azul + dorado):
    - [ ] Me gusta como está — mantenerla
    - [ ] Prefiero el estilo "Talonario Carbon Copy" (talonarios colombianos, sellos, papel de copias)
    - [ ] Otro: ____

13. Para la **factura** (documento legal ante DIAN):
    - [ ] Híbrida: misma familia de la cotización con matices de talonario/sello y los campos DIAN integrados (recomendado)
    - [ ] Que se vea igual a la cotización actual (misma identidad visual)
    - [ ] Que la arme Factus tal cual (PDF oficial, sin diseño propio)
    - [ ] No me importa el diseño, con tal que esté toda la información DIAN

14. ¿Tiene Agromanguera **colores/logo de marca** confirmados?
    - [ ] Sí → adjuntar/describir: ____
    - [ ] No → seguir con la paleta actual (azul blueprint + dorado)
    - [ ] Quiero algo más agro (verdes, tierra): ____

15. El cliente final, tras la venta, ¿qué debe recibir?
    - [ ] Solo la factura PDF por email
    - [ ] Un enlace público a la factura con QR/CUFE
    - [ ] Ambos (recomendado)

---

## D. Proceso operativo

16. ¿Quién puede emitir facturas?
    - [ ] Solo el admin
    - [ ] Vendedores (con los límites de descuento actuales)
    - [ ] Admin + vendedores (recomendado)

17. ¿Quién configura datos fiscales y rangos de numeración?
    - [ ] Solo el admin
    - [ ] El contador (¿necesita acceso al sistema o solo le pasan PDF/XML/CSV?)

18. ¿El email de la factura lo envía nuestro sistema o Factus?
    - [ ] Nuestro sistema (mismo pipeline de la cotización — recomendado)
    - [ ] Factus (ellos envían el correo al cliente)
    - [ ] Indiferente

19. ¿El contador necesita acceso de solo lectura a facturas, PDF y XML?
    - [ ] Sí
    - [ ] No, le pasan archivos

20. ¿Qué pasa cuando la DIAN rechaza una factura (ej. dato mal escrito)?
    - [ ] El vendedor la corrige y re-emite
    - [ ] Solo el admin corrige y re-emite
    - [ ] No sé

---

## E. Inventario

21. ¿Controlan inventario hoy? ¿Cómo?
    - [ ] No se controla
    - [ ] Manual (hoja de cálculo/cuaderno)
    - [ ] En otro software: ____

22. Al vender sin stock suficiente, ¿qué prefiere?
    - [ ] Bloquear la emisión de la factura (no se puede facturar lo que no hay)
    - [ ] Avisar con advertencia pero permitir la venta
    - [ ] Depende del producto (algunos se manejan por pedido)

23. ¿Manejan devoluciones o mermas (productos dañados, vencidos, faltantes)?
    - [ ] No
    - [ ] Sí, ocasionalmente
    - [ ] Sí, con frecuencia → ¿quién debe poder registrarlas? ____

24. ¿El inventario es único o por bodega?
    - [ ] Único
    - [ ] Por bodega (¿cuántas? ____) → por ahora el sistema soporta inventario único

25. ¿Existe un conteo físico actual de existencias para cargar como stock inicial?
    - [ ] Sí, lo podemos entregar
    - [ ] No, hay que hacer el conteo
    - [ ] No sé

26. Umbral de alerta: ¿cuántos días de venta quieren de colchón antes de notificar reposición?
    - [ ] Definirlo por producto (recomendado: mínimo/máximo por ítem)
    - [ ] Un valor global
    - [ ] No sé — configurarlo por producto luego

---

## F. Contabilidad básica y estadísticas

27. ¿Qué necesita ver el contador del sistema? (marca todos)
    - [ ] Libro de ventas (facturas + IVA discriminado)
    - [ ] Cartera por cliente (saldo pendiente de facturas a crédito)
    - [ ] Registro de pagos recibidos contra facturas
    - [ ] Exportar a CSV/Excel
    - [ ] Descargar PDF y XML de cada factura

28. ¿Registran pagos parciales de las facturas a crédito?
    - [ ] No se vende a crédito
    - [ ] Sí, un pago por factura
    - [ ] Sí, pagos parciales múltiples por factura

29. ¿Usan un software contable externo (Excel, contador, otro sistema)?
    - [ ] Solo Excel
    - [ ] Contador externo que recibe archivos
    - [ ] Otro software: ____

30. Ordene las 3 métricas más importantes para el panel (1 = más importante):
    - ( ) Ventas del mes (total y número de facturas)
    - ( ) Ingresos por método de pago
    - ( ) Top de productos vendidos
    - ( ) Cartera pendiente total
    - ( ) Estado de facturas ante la DIAN (validadas/rechazadas/pendientes)
    - ( ) Alertas de inventario bajo
    - ( ) Otro: ____

31. ¿Necesitan ver el total de **IVA causado** por mes (para declaración)?
    - [ ] Sí, en el libro de ventas
    - [ ] No, lo maneja el contador aparte
    - [ ] No sé

---

## G. Presupuesto y tiempo

32. Presupuesto mensual para Factus (planes por volumen de documentos):
    - [ ] Menos de COP 100 000
    - [ ] COP 100 000–300 000
    - [ ] No lo sé — pedir cotización según volumen (respuesta de la pregunta 3)

33. ¿Fecha límite deseada para facturar en producción?
    - [ ] Ya (semanas)
    - [ ] 2–3 meses
    - [ ] Sin prisa

34. ¿Tienen credenciales o cuenta Factus?
    - [ ] No (el plan incluye la gestión — F1)
    - [ ] Sí, sandbox (pruebas)
    - [ ] Sí, producción

35. Comentarios o requisitos adicionales (ej. multi-vendedor con límites, impresión de facturas en punto de venta, integración con banco):
    ____________________________________________________________

---

## Cómo usar las respuestas

| Respuesta clave | Impacto en el plan |
|---|---|
| P2 + P10: consumidor final | Si "Sí" y >10 % → pasa del v1.1 al MVP |
| P2: nota crédito | Si "Sí" → MVP + nota crédito (F3 ampliada) |
| P6–P9: datos fiscales | Obligatorios para producción; el MVP sandbox no los exige |
| P13: dirección visual | Confirmada híbrida (recomendada); alternativas ajustan `templates/factura.html` |
| P22: venta sin stock | Define la política bloquear/avisar (default: bloquear) |
| P27–P29: contabilidad | Define el alcance real de "contabilidad básica" |
| P32–P34: presupuesto/credenciales | Define velocidad de F1 y el paso a producción |
