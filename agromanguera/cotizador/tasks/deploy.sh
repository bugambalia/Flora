#!/bin/bash

set -e

echo "🚀 Preparando despliegue de Cotizador Agromanguera a Cloudflare"
echo "================================================================"

# Variables de configuración
WORKER_NAME="cotizador-backend"
D1_DB_ID="4e704f82-5499-41b7-945f-73ffb678e929"
D1_DB_NAME="cotizador-db"
R2_BUCKET="cotizador-logos"
BACKEND_DIR="cotizador-backend"

# Verificar que wrangler está instalado y autenticado
echo "📋 Verificando configuración de wrangler..."
if ! command -v wrangler &> /dev/null; then
    echo "❌ Wrangler no está instalado. Instalar con: npm install -g wrangler"
    exit 1
fi

# Verificar autenticación
echo "🔐 Verificando autenticación en Cloudflare..."
if ! npx wrangler whoami &> /dev/null; then
    echo "❌ No autenticado en Cloudflare. Ejecutar: npx wrangler login"
    exit 1
fi

# Verificar que estamos en el directorio correcto
echo "📁 Verificando directorio del proyecto..."
cd /home/melaza/Documentos/Development/agromanguera/cotizador
cd "$BACKEND_DIR"

echo "✅ Wrangler configurado correctamente"

# Paso 1: Verificar que todos los secrets están configurados
echo ""
echo "📦 PASO 1: Verificar secrets configurados"
echo "------------------------------------------"
echo "Verificando que los siguientes secrets están configurados:"
echo ""
echo "1. RESEND_API_KEY - API key para enviar emails"
echo "2. CF_ACCESS_AUD  - Audience de Cloudflare Access"
echo "3. CF_ACCESS_ISSUER - Issuer URL de Cloudflare Access"
echo "4. ALLOWED_ORIGINS - Orígenes permitidos para CORS"
echo ""

# Listar secrets actuales
RESPONSE=$(npx wrangler secret list 2>/dev/null || echo "❌ No se pudieron obtener secrets")
if echo "$RESPONSE" | grep -q "No secrets found"; then
    echo "❌ No hay secrets configurados"
else
    echo "Secrets actualmente configurados:"
    echo "$RESPONSE"
fi

echo ""
echo "📋 Para configurar los secrets, ejecutar:")
echo "   npx wrangler secret put RESEND_API_KEY"
echo "   npx wrangler secret put CF_ACCESS_AUD"
echo "   npx wrangler secret put CF_ACCESS_ISSUER"
echo "   npx wrangler secret put ALLOWED_ORIGINS"
echo ""
echo "⚠️  Asegurarse de tener los valores listos antes de continuar"
read -p "¿Continuar con el despliegue? (s/n): " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Ss]$ ]]; then
    echo "❌ Despliegue cancelado"
    exit 0
fi

# Paso 2: Verificar configuración de R2
echo ""
echo "📦 PASO 2: Verificar R2"
echo "-----------------------"
echo "Verificando estado de R2 en la cuenta..."

# Verificar si R2 está habilitado
if npx wrangler r2 bucket list 2>&1 | grep -q "Please enable R2"; then
    echo "❌ R2 no está habilitado en esta cuenta"
    echo ""
    echo "📋 Pasos para habilitar R2:"
    echo "1. Ir a Cloudflare Dashboard: https://dash.cloudflare.com"
    echo "2. Seleccionar tu cuenta"
    echo "3. Navegar a: R2 → Overview"
    echo "4. Hacer clic en 'Enable R2' y seguir las instrucciones"
    echo "5. Una vez habilitado, regresar y ejecutar este script nuevamente"
    echo ""
    echo "⚠️  El Worker funcionará sin R2, pero la funcionalidad de logos no estará disponible"
    read -p "¿Continuar sin R2? (s/n): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Ss]$ ]]; then
        echo "❌ Despliegue cancelado"
        exit 0
    fi
else
    echo "✅ R2 está habilitado"
    
    # Verificar si el bucket existe
    if npx wrangler r2 bucket info "$R2_BUCKET" &> /dev/null; then
        echo "✅ Bucket R2 '$R2_BUCKET' ya existe"
    else
        echo "🔧 Creando bucket R2 '$R2_BUCKET'..."
        npx wrangler r2 bucket create "$R2_BUCKET"
        echo "✅ Bucket R2 creado"
    fi
fi

# Paso 3: Ejecutar migraciones D1
echo ""
echo "📦 PASO 3: Aplicar migraciones D1"
echo "---------------------------------"
echo "Aplicando migraciones a la base de datos remota..."

# Verificar que la base de datos existe
if ! npx wrangler d1 info "$D1_DB_NAME" &> /dev/null; then
    echo "❌ La base de datos D1 '$D1_DB_NAME' no existe"
    echo "   Crear con: npx wrangler d1 create $D1_DB_NAME"
    exit 1
fi

echo "📋 Ejecutando migraciones en remoto..."
npx wrangler d1 migrations apply "$D1_DB_NAME" --remote
echo "✅ Migraciones aplicadas"

# Paso 4: Build y verificación
echo ""
echo "📦 PASO 4: Build y verificación"
echo "-------------------------------"
echo "Ejecutando TypeScript type check..."
npx tsc --noEmit
echo "✅ TypeScript check pasado"

echo "Ejecutando lint..."
npx oxlint src
echo "✅ Lint pasado"

echo "Ejecutando tests..."
npm test
echo "✅ Tests pasados"

# Paso 5: Dry run del deploy
echo ""
echo "📦 PASO 5: Dry run del despliegue"
echo "---------------------------------"
echo "Verificando configuración del Worker..."
npx wrangler deploy --dry-run
echo "✅ Dry run completado"

# Paso 6: Despliegue a producción
echo ""
echo "📦 PASO 6: Despliegue a producción"
echo "----------------------------------"
echo "🚀 Desplegando Worker a Cloudflare..."
npx wrangler deploy
echo "✅ Worker desplegado"

# Obtener URL del Worker
WORKER_URL=$(npx wrangler deploy --dry-run --outdir /tmp 2>/dev/null | grep -o 'https://[^ ]*\.workers\.dev' | head -1)
if [ -z "$WORKER_URL" ]; then
    WORKER_URL="https://cotizador-backend.a1759fa613ee79449a37a60559991dab.workers.dev"
fi

echo ""
echo "🎉 DESPLIEGUE COMPLETADO"
echo "========================"
echo "✅ Worker: $WORKER_URL"
echo "✅ D1 Database: $D1_DB_NAME (ID: $D1_DB_ID)"
echo "✅ R2 Bucket: $R2_BUCKET (si está habilitado)"
echo ""
echo "📋 Endpoints disponibles:"
echo "   • Health check: $WORKER_URL/api/health (requiere auth)"
echo "   • Vista pública demo: $WORKER_URL/c/demo"
echo ""
echo "🔧 Próximos pasos:"
echo "   1. Configurar custom domain si es necesario"
echo "   2. Configurar Cloudflare Access para protección"
echo "   3. Verificar logs en Workers Dashboard"
echo ""
echo "📊 Para monitorear:"
echo "   • Dashboard: https://dash.cloudflare.com/?to=/:account/workers"
echo "   • Logs: https://dash.cloudflare.com/?to=/:account/workers/view/logs"
