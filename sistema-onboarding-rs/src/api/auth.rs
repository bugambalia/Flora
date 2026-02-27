use worker::*;
use serde_json::json;
use argon2::{
    password_hash::{PasswordHash, PasswordVerifier},
    Argon2,
};
use crate::core::jwt::{verify_token, extract_bearer_token, create_token};
use crate::model::user::LoginRequest;

pub async fn login(mut req: Request, ctx: RouteContext<()>) -> Result<Response> {
    let payload: LoginRequest = match req.json().await {
        Ok(p) => p,
        Err(_) => return Response::error("JSON inválido", 400),
    };

    let db = match ctx.env.d1("DB") {
        Ok(db) => db,
        Err(_) => return Response::error("Configuración de DB fallida", 500),
    };

    // Consulta al nuevo esquema (Contrasena y rol_sistema)
    let stmt = db.prepare("SELECT Correo, Contrasena, rol_sistema, nombre FROM USUARIO WHERE Correo = ? LIMIT 1");
    let row = match stmt.bind(&[payload.correo.clone().into()])?.first::<serde_json::Value>(None).await? {
        Some(row) => row,
        None => return Response::error("Usuario no encontrado", 401),
    };

    let db_pass_hash = row["Contrasena"].as_str().unwrap_or("");
    
    // Verificación de contraseña usando Argon2id
    let parsed_hash = match PasswordHash::new(db_pass_hash) {
        Ok(h) => h,
        Err(_) => return Response::error("Error interno: Hash de contraseña inválido en DB", 500),
    };

    if Argon2::default().verify_password(payload.contrasena.as_bytes(), &parsed_hash).is_err() {
        return Response::error("Contraseña incorrecta", 401);
    }

    let nombre = row["nombre"].as_str().unwrap_or("Usuario");
    let rol = row["rol_sistema"].as_str().unwrap_or("Operador");
    let company = "Sistema Onboarding"; 

    let secret = match ctx.env.secret("JWT_SECRET") {
        Ok(s) => s.to_string(),
        Err(_) => return Response::error("JWT Secret no configurado", 500),
    };

    match create_token(&payload.correo, company, rol, &secret) {
        Ok(token) => {
            let body = json!({ 
                "token": token,
                "user": {
                    "nombre": nombre,
                    "correo": payload.correo,
                    "rol": rol
                }
            }).to_string();
            let headers = Headers::new();
            headers.set("Content-Type", "application/json")?;
            Ok(Response::from_body(ResponseBody::Body(body.into_bytes()))?.with_headers(headers))
        }
        Err(e) => Response::error(format!("Error generando token: {}", e), 500),
    }
}

pub async fn protected_route(req: Request, ctx: RouteContext<()>) -> Result<Response> {
    let auth_header = match req.headers().get("Authorization")? {
        Some(h) => h,
        None => return Response::error("Falta la cabecera Authorization", 401),
    };

    let token = match extract_bearer_token(&auth_header) {
        Some(t) => t,
        None => return Response::error("Formato de token Bearer inválido", 401),
    };

    let secret = match ctx.env.secret("JWT_SECRET") {
        Ok(s) => s.to_string(),
        Err(_) => return Response::error("Error interno: Secreto no configurado", 500),
    };

    match verify_token(&token, &secret) {
        Ok(claims) => Response::ok(json!({
            "mensaje": format!("Acceso concedido. Bienvenido {}", claims.sub),
            "rol": claims.role,
            "correo": claims.sub
        }).to_string()),
        Err(e) => Response::error(e.to_string(), 401),
    }
}