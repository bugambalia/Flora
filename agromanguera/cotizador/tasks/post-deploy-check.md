# Verificación Post-Deploy - Cotizador Agromanguera

Esta guía describe los pasos para verificar que el Worker se ha desplegado correctamente y está funcionando como se espera.

## Fecha del Despliegue
**$(date)**

## Estado del Despliegue

### ✅ 1. Worker Desplegado
- **Nombre:** `cotizador-backend`
- **URL:** `https://cotizador-backend.<account>.workers.dev`
- **Versión:** Última commit

### ✅ 2. D1 Database
- **Nombre:** `cotizador-db`
- **ID:** `4e704f82-5499-41b7-945f-73ffb678e929`
- **Migraciones:** Aplicadas correctamente

### ⚠️ 3. R2 Bucket
- **Nombre:** `cotizador-logos`
- **Estado:** R2 no está habilitado en esta cuenta
- **Impacto:** La funcionalidad de logos no estará disponible hasta que se habilite R2

### ✅ 4. Secrets Configurados
Verificar que los siguientes secrets están configurados:
- [ ] `RESEND_API_KEY` - API key para Resend
- [ ] `CF_ACCESS_AUD` - Audience de Cloudflare Access
- [ ] `CF_ACCESS_ISSUER` - Issuer URL de Cloudflare Access
- [ ] `ALLOWED_ORIGINS` - Orígenes permitidos para CORS

## Pruebas de Funcionalidad

### 🔧 1. Health Check (Requiere Auth)
```bash
# Obtener un token JWT de Cloudflare Access primero
# Luego hacer la petición:
curl -H "CF-Access-Jwt-Assertion: <tu_token_jwt>" \
     https://cotizador-backend.<account>.workers.dev/api/health
```

**Respuesta esperada:**
```json
{
  "status": "ok",
  "timestamp": "2026-07-31T20:25:12.899Z",
  "version": "1.0.0"
}
```

### 🌐 2. Vista Pública Demo
```bash
# Prueba sin autenticación
curl https://cotizador-backend.<account>.workers.dev/c/demo
```

**Respuesta esperada:**
- Código HTTP: 200 OK
- Contenido HTML con la vista de cotización demo

### 📄 3. Generación de PDF Demo
```bash
# Descargar PDF demo
curl -o cotizacion-demo.pdf \
     https://cotizador-backend.<account>.workers.dev/c/demo.pdf
```

**Verificación:**
- Archivo PDF descargado correctamente
- Tamaño: > 10KB
- Contenido legible

## Verificación de Logs

### 📊 1. Acceder a Logs
1. Ir a: https://dash.cloudflare.com/?to=/:account/workers/view/logs
2. Seleccionar `cotizador-backend`
3. Verificar que hay logs recientes

### 🔍 2. Buscar Errores
Filtrar por:
- Nivel: `error` o `warn`
- Últimas 24 horas

**Errores esperados (si R2 no está habilitado):**
```
Failed to access R2 bucket: Please enable R2 through the Cloudflare Dashboard.
```

## Verificación de Rendimiento

### ⚡ 1. Tiempos de Respuesta
```bash
# Medir tiempo de respuesta
time curl -s https://cotizador-backend.<account>.workers.dev/c/demo > /dev/null
```

**Métricas aceptables:**
- Health check: < 500ms
- Vista pública: < 1000ms
- PDF generación: < 3000ms

### 🚀 2. Métricas del Worker
1. Ir a: https://dash.cloudflare.com/?to=/:account/workers
2. Seleccionar `cotizador-backend`
3. Revisar:
   - **Requests/sec**: Debe ser > 0
   - **CPU time**: Debe ser estable
   - **Errors**: Debe ser 0 o bajo

## Pruebas de Integración

### ✉️ 1. Envío de Email (Si Resend está configurado)
```bash
# Probar endpoint de email (requiere auth)
curl -X POST \
     -H "CF-Access-Jwt-Assertion: <tu_token_jwt>" \
     -H "Content-Type: application/json" \
     -d '{"to": "test@example.com", "subject": "Test", "html": "<h1>Test</h1>"}' \
     https://cotizador-backend.<account>.workers.dev/api/email/send
```

### 🗄️ 2. Base de Datos D1
```bash
# Probar conexión a D1 (requiere auth)
curl -H "CF-Access-Jwt-Assertion: <tu_token_jwt>" \
     https://cotizador-backend.<account>.workers.dev/api/data/health
```

## Checklist Final

### ✅ Configuración
- [ ] Worker desplegado y accesible
- [ ] D1 database configurada y migrada
- [ ] Secrets configurados correctamente
- [ ] Observability habilitado

### ✅ Funcionalidad
- [ ] Health check responde correctamente
- [ ] Vista pública funciona sin auth
- [ ] PDF se genera correctamente
- [ ] Logs están disponibles

### ✅ Rendimiento
- [ ] Tiempos de respuesta aceptables
- [ ] No hay errores en logs
- [ ] Métricas del Worker estables

### 🔧 Issues Conocidos
1. **R2 no habilitado** - La funcionalidad de logos no está disponible
   - Solución: Habilitar R2 en Cloudflare Dashboard

## Acciones de Remedio

### Si el Health Check Falla:
1. Verificar logs del Worker
2. Verificar configuración de secrets
3. Probar localmente con `npm run dev`

### Si la Vista Pública Falla:
1. Verificar build del Worker
2. Verificar código en `src/routes/public.ts`
3. Revisar logs de errores

### Si el PDF no se Genera:
1. Verificar configuración de Browser Rendering
2. Probar con diferentes datos
3. Revisar logs de Browser Rendering

## Contactos de Soporte

- **DevOps:** Responsable del despliegue
- **Desarrollador:** Soporte técnico del código
- **Cloudflare Support:** Para issues de infraestructura

---

**Nota:** Esta verificación debe realizarse después de cada despliegue y al menos una vez por semana en producción.
