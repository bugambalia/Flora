use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize)]
pub struct Cargo {
    pub id: Option<i32>,
    pub nombre_cargo: String,
    pub area: String,
    pub id_jefe_inmediato: Option<i32>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct Usuario {
    pub id: Option<i32>,
    pub correo: String,
    pub contrasena: String,
    pub rol_sistema: String,
    pub nombre: String,
    pub id_cargo: Option<i32>,
    pub estado_onboarding: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct Solicitud {
    pub id: Option<i32>,
    pub id_empleado: i32,
    pub id_area_encargada: String,
    pub fecha_creacion: Option<String>,
    pub fecha_maxima_cumplimiento: String,
    pub fecha_fin: Option<String>,
    pub estado: String,
    pub especificaciones: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct Historial {
    pub id: Option<i32>,
    pub id_solicitud: i32,
    pub fecha_cambio: Option<String>,
    pub estado_antiguo: String,
    pub nuevo_estado: String,
    pub comentario: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct RegistroIngresoRequest {
    pub correo: String,
    pub nombre: String,
    pub id_cargo: i32,
    pub fecha_ingreso: String, // Será usada para calcular fecha_maxima_cumplimiento
}
