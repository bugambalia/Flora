use hmac::{Hmac, Mac};
use sha2::Sha256;
use base64::{engine::general_purpose::URL_SAFE_NO_PAD, Engine};

use worker::{Error, Result};
use crate::model::claims::Claims;
use chrono::{Utc, Duration};

type HmacSha256 = Hmac<Sha256>;

/// Construye un JWT HS256 manualmente usando crates pure-Rust (WASM compatible).
pub fn create_token(sub: &str, company: &str, role: &str, secret: &str) -> Result<String> {
    let expiration = Utc::now()
        .checked_add_signed(Duration::hours(24))
        .ok_or_else(|| Error::RustError("Error calculando expiración".into()))?
        .timestamp();

    let claims = Claims {
        sub: sub.to_owned(),
        company: company.to_owned(),
        role: role.to_owned(),
        exp: expiration as usize,
    };

    let header = URL_SAFE_NO_PAD.encode(r#"{"alg":"HS256","typ":"JWT"}"#);
    let payload = URL_SAFE_NO_PAD.encode(
        serde_json::to_string(&claims)
            .map_err(|e| Error::RustError(format!("Error serializando claims: {}", e)))?,
    );

    let signing_input = format!("{}.{}", header, payload);
    let signature = hmac_sign(secret.as_bytes(), signing_input.as_bytes())?;

    Ok(format!("{}.{}", signing_input, signature))
}

/// Verifica y decodifica un JWT HS256.
pub fn verify_token(token: &str, secret: &str) -> Result<Claims> {
    let parts: Vec<&str> = token.splitn(3, '.').collect();
    if parts.len() != 3 {
        return Err(Error::RustError("Token mal formado".into()));
    }

    let signing_input = format!("{}.{}", parts[0], parts[1]);
    let expected_sig = hmac_sign(secret.as_bytes(), signing_input.as_bytes())?;

    // Comparación en tiempo constante para evitar timing attacks
    if !constant_time_eq(parts[2].as_bytes(), expected_sig.as_bytes()) {
        return Err(Error::RustError("Firma inválida".into()));
    }

    let payload_bytes = URL_SAFE_NO_PAD
        .decode(parts[1])
        .map_err(|e| Error::RustError(format!("Token base64 inválido: {}", e)))?;

    let claims: Claims = serde_json::from_slice(&payload_bytes)
        .map_err(|e| Error::RustError(format!("Claims inválidos: {}", e)))?;

    let now = Utc::now().timestamp() as usize;
    if claims.exp < now {
        return Err(Error::RustError("Token expirado".into()));
    }

    Ok(claims)
}

/// Calcula la firma HMAC-SHA256 y la retorna como base64url sin padding.
fn hmac_sign(key: &[u8], data: &[u8]) -> Result<String> {
    let mut mac = HmacSha256::new_from_slice(key)
        .map_err(|e| Error::RustError(format!("Error en HMAC: {}", e)))?;
    mac.update(data);
    let result = mac.finalize().into_bytes();
    Ok(URL_SAFE_NO_PAD.encode(result))
}

/// Comparación en tiempo constante para evitar timing side-channels.
fn constant_time_eq(a: &[u8], b: &[u8]) -> bool {
    if a.len() != b.len() {
        return false;
    }
    a.iter().zip(b.iter()).fold(0u8, |acc, (x, y)| acc | (x ^ y)) == 0
}

/// Extrae el token Bearer de la cabecera Authorization.
pub fn extract_bearer_token(auth_header: &str) -> Option<String> {
    auth_header.strip_prefix("Bearer ").map(|t| t.to_string())
}
