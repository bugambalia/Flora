---

## 1. Desglose para el Frontend (React)

### **A. Módulo de Registro e Ingreso**

* **Diseño de Formulario:** Crear componentes de entrada para nombre, documento y fecha de ingreso, usando los colores corporativos: Azul Oxford (#1B365D) para bordes y Blanco Humo (#F8F9FA) para fondos.

* **Selector Organizacional:** Implementar un menú desplegable dinámico que cargue las 6 gerencias y sus respectivas áreas (ej. Gerencia TI -> Infraestructura y Redes) .

* **Validación de Fechas:** Programar lógica en el cliente que bloquee la selección de fechas anteriores a la actual y permita postergar ingresos pero no adelantarlos.



### **B. Mapa de Puestos de Trabajo**

* **Renderizado de Plano:** Crear un componente que dibuje un grid o SVG interactivo que represente las oficinas.


* **Gestión de Estados Visuales:** Programar el cambio de color de las "sillas" (Verde para disponible, Gris para ocupado).


* **Asignación por Click:** Capturar las coordenadas del puesto seleccionado para enviarlas al backend.

### **C. Panel de Operadores y Check-in**

* **Listado de Tareas:** Crear tablas filtrables por área (TI, Dotación, etc.) donde se vean los empleados pendientes.

* **Formulario de Tallas/Equipos:** Vista para que el Jefe Inmediato cargue tallas de uniformes y periféricos.

* **Interfaz de Firma:** Pantalla de validación donde el empleado ingresa el código recibido en su correo personal.

---

## 2. Desglose para el Backend (Cloudflare Workers + Rust/WASM)

### **A. Núcleo de la API y Seguridad**

* **Middleware de Auth:** Implementar la verificación de roles (Root, Admin, Jefe, Operador) usando JWT o el sistema de identidades externo .

* **Estructura D1/SQLite:** Ejecutar el script de creación de tablas ajustado, asegurando las llaves foráneas para la integridad entre usuarios y solicitudes.

* **CRUD de Ingresos:** Desarrollar los endpoints para crear registros de ingreso y generar el código autogestionado del proceso.


### **B. Motor de Recomendación e IA (Rust/WASM)**

* **Lógica Predictiva de Kits:** Crear una función en Rust que analice el `cargo_id` y retorne el JSON de insumos (ej. si el cargo es "Analista de QA", sugerir licencias de pruebas y PC portátil).

* **Mapeador de Capacitación:** Programar el sistema para que asocie automáticamente el cargo con los cursos del portafolio (ej. "Técnicas de Negociación" para el área comercial).

* **Disparador de Solicitudes:** Crear la función que, tras el registro, inserte automáticamente filas en la tabla `SOLICITUDES` para cada área con su fecha de cumplimiento.

### **C. Automatización y Reportes**

* **Generador de Actas (WASM):** Integrar una crate de Rust (como `printpdf`) para generar el acta de entrega serializada en PDF el día del ingreso.

* **Servicio de Notificaciones:** Implementar el consumo de la API externa (SendGrid) para enviar correos automáticos al crear procesos.

* **Cálculo de Eficiencia:** Crear un endpoint que procese el historial y calcule el porcentaje de tareas finalizadas antes de la fecha máxima por cada área.

---