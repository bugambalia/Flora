use serde::Deserialize;

#[derive(Deserialize)]
pub struct LoginRequest {
    pub correo: String,
    pub contrasena: String,
}
