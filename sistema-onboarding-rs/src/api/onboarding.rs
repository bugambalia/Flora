use worker::*;
use serde_json::json;
use crate::model::onboarding::RegistroIngresoRequest;

pub async fn crear_ingreso(mut req: Request, ctx: RouteContext<()>) -> Result<Response> {
    let payload: RegistroIngresoRequest = match req.json().await {
        Ok(p) => p,
        Err(_) => return Response::error("JSON de registro inválido", 400),
    };

    let db = match ctx.env.d1("DB") {
        Ok(db) => db,
        Err(_) => return Response::error("Configuración de DB fallida", 500),
    };

    // 1. Verificar si el correo ya existe
    let check_stmt = db.prepare("SELECT ID FROM USUARIO WHERE Correo = ? LIMIT 1");
    let existing = check_stmt.bind(&[payload.correo.clone().into()])?.first::<serde_json::Value>(None).await?;
    
    if existing.is_some() {
        return Response::error("El usuario con este correo ya está registrado", 409);
    }

    // 2. Insertar nuevo usuario (contraseña temporal por defecto o vacía para posterior activación)
    // En un flujo real, aquí se generaría un token de activación
    let insert_stmt = db.prepare(
        "INSERT INTO USUARIO (Correo, Contrasena, rol_sistema, nombre, id_cargo, estado_onboarding) 
         VALUES (?, ?, 'Operador', ?, ?, 'Pendiente')"
    );

    // Por seguridad, la contraseña inicial debería ser un hash aleatorio o gestionarse vía email.
    // Usaremos una cadena vacía o una temporal por ahora.
    let temp_pass = ""; 

    match insert_stmt.bind(&[
        payload.correo.into(),
        temp_pass.into(),
        payload.nombre.into(),
        payload.id_cargo.into(),
    ])?.run().await {
        Ok(_) => {
            Response::ok(json!({
                "mensaje": "Ingreso registrado exitosamente",
                "correo": payload.correo,
                "estado": "Pendiente"
            }).to_string())
        },
        Err(e) => Response::error(format!("Error en base de datos: {}", e), 500),
    }
}

pub async fn listar_cargos(_req: Request, ctx: RouteContext<()>) -> Result<Response> {
    let db = match ctx.env.d1("DB") {
        Ok(db) => db,
        Err(_) => return Response::error("Configuración de DB fallida", 500),
    };

    let stmt = db.prepare("SELECT id, nombre_cargo, area FROM CARGOS ORDER BY area, nombre_cargo");
    let cargos = stmt.all().await?.results::<serde_json::Value>()?;

    Response::from_json(&cargos)
}
