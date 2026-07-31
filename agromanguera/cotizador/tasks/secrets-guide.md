# Guía de Secrets - Cotizador Agromanguera

## Introducción
Este documento describe los secrets necesarios para el funcionamiento del Worker `cotizador-backend` en producción. Los secrets son variables de entorno sensibles que se configuran mediante `wrangler secret put`.

## Configuración Previa Requerida

### R2 (Almacenamiento de Logos)
**Estado actual:** R2 no está habilitado en esta cuenta. Los logos no estarán disponibles hasta que se habilite R2.

**Pasos para habilitar R2:**
1. Ir a **Cloudflare Dashboard**: https://dash.cloudflare.com
2. Seleccionar tu cuenta (`Jhonier0301@gmail.com`)
3. Navegar a: **R2 → Overview**
4. Hacer clic en **"Enable R2"** y seguir las instrucciones
5. Una vez habilitado, regresar y ejecutar este script nuevamente

**Impacto:** El Worker funcionará sin R2, pero la funcionalidad de logos no estará disponible.

## Secrets Requeridos

### 1. `RESEND_API_KEY`
**Propósito:** API key para enviar emails transaccionales mediante Resend.
**Tipo:** String

**Cómo obtenerlo:**
1. Crear una cuenta en [Resend](https://resend.com)
2. Ir a **API Keys** en el dashboard
3. Generar una nueva API key
4. Copiar el valor

**Valor de ejemplo:**
```
re_1234567890abcdefghijklmnopqrstuvwxyz
```

**Comando para configurar:**
```bash
npx wrangler secret put RESEND_API_KEY
```

### 2. `CF_ACCESS_AUD`
**Propósito:** Audience (aud) de la aplicación en Cloudflare Access. Es el identificador único de tu aplicación Access.
**Tipo:** String

**Cómo obtenerlo:**
1. Ir a **Cloudflare Dashboard** → **Access** → **Applications**
2. Seleccionar tu aplicación Access
3. Ir a **Settings** → **Configurations**
4. En la sección **JWT**, encontrar el campo **Audience Tag**
5. Copiar el valor (formato UUID)

**Valor de ejemplo:**
```
4b2a1c3d-5e6f-7a8b-9c0d-1e2f3a4b5c6d
```

**Comando para configurar:**
```bash
npx wrangler secret put CF_ACCESS_AUD
```

### 3. `CF_ACCESS_ISSUER`
**Propósito:** Issuer URL del tenant de Cloudflare Access. Es la URL que emite los tokens JWT.
**Tipo:** URL string

**Cómo obtenerlo:**
1. Ir a **Cloudflare Dashboard** → **Access** → **Applications**
2. Seleccionar tu aplicación Access
3. Ir a **Settings** → **Configurations**
4. En la sección **JWT**, encontrar el campo **Issuer URL**
5. Copiar el valor completo

**Valor de ejemplo:**
```
https://<your-team>.cloudflareaccess.com
```

o
```
https://<your-tenant>.cloudflareaccess.com
```

**Comando para configurar:**
```bash
npx wrangler secret put CF_ACCESS_ISSUER
```

### 4. `ALLOWED_ORIGINS`
**Propósito:** Orígenes permitidos para CORS (Cross-Origin Resource Sharing). Controla qué dominios pueden acceder a la API.
**Tipo:** String (lista separada por comas)

**Valores recomendados:**
- Para desarrollo: `http://localhost:5173,http://localhost:3000`
- Para producción: `https://cotizador.agromanguera.co,https://app.agromanguera.co`

**Valor de ejemplo (producción):**
```
https://cotizador.agromanguera.co,https://app.agromanguera.co
```

**Comando para configurar:**
```bash
npx wrangler secret put ALLOWED_ORIGINS
```

## Configuración en `wrangler.jsonc`

Las siguientes variables de entorno están configuradas en `wrangler.jsonc` (no son secrets):

```json
{
  "vars": {
    "ENVIRONMENT": "production",
    "RESEND_API_URL": "https://api.resend.com/emails"
  }
}
```

## Comandos para Configurar Todos los Secrets

```bash
# Navegar al directorio del backend
cd cotizador-backend

# Configurar todos los secrets
npx wrangler secret put RESEND_API_KEY
npx wrangler secret put CF_ACCESS_AUD
npx wrangler secret put CF_ACCESS_ISSUER
npx wrangler secret put ALLOWED_ORIGINS
```

## Verificar Secrets Configurados

Para verificar que los secrets están configurados correctamente:

```bash
# Listar todos los secrets
npx wrangler secret list

# Verificar un secret específico (dry run del Worker)
npx wrangler deploy --dry-run
```

## Troubleshooting

### Error: "Secret not found"
Si el Worker falla porque no encuentra un secret, verificar:

1. Que el secret está configurado: `npx wrangler secret list`
2. Que el nombre del secret coincide exactamente con el definido en el código
3. Que estás en el directorio correcto del proyecto

### Error: "Invalid JWT"
Si hay problemas con la autenticación de Cloudflare Access:

1. Verificar que `CF_ACCESS_AUD` y `CF_ACCESS_ISSUER` son correctos
2. Asegurarse que la aplicación Access está configurada correctamente
3. Verificar que los tokens JWT incluyen las claims correctas

### Error: "Failed to send email"
Si hay problemas con el envío de emails:

1. Verificar que `RESEND_API_KEY` es válido
2. Verificar que el dominio está verificado en Resend
3. Verificar logs del Worker para más detalles

## Seguridad

- **No** compartir los secrets en repositorios git
- **No** guardar los secrets en archivos de configuración públicos
- **Sí** rotar los secrets periódicamente
- **Sí** usar diferentes secrets por entorno (dev/staging/prod)

## Rotación de Secrets

Para rotar un secret:

```bash
# Actualizar el secret
npx wrangler secret put RESEND_API_KEY

# El Worker se actualizará automáticamente
```

---

*Última actualización: $(date)*
