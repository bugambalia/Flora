# Cloudflare Workers en Rust (`workers-rs`)

Bindings de Rust para el entorno de Cloudflare Workers (trabajo en progreso). ¡Escribe todo tu Worker íntegramente en Rust!

> [!IMPORTANT]
> Lee las Notas y las Preguntas Frecuentes (FAQ) antes de comenzar.

## Ejemplo de Uso

```rust
use worker::*;

#[event(fetch)]
pub async fn main(mut req: Request, env: Env, _ctx: worker::Context) -> Result<Response> {
    console_log!(
        "{} {}, ubicado en: {:?}, dentro de: {}",
        req.method().to_string(),
        req.path(),
        req.cf().unwrap().coordinates().unwrap_or_default(),
        req.cf().unwrap().region().unwrap_or("unknown region".into())
    );

    if !matches!(req.method(), Method::Post) {
        return Response::error("Method Not Allowed", 405);
    }

    if let Some(file) = req.form_data().await?.get("file") {
        return match file {
            FormEntry::File(buf) => {
                Response::ok(&format!("size = {}", buf.bytes().await?.len()))
            }
            _ => Response::error("La parte `file` del formulario POST debe ser un archivo", 400),
        };
    }

    Response::error("Bad Request", 400)
}

```

---

## Primeros Pasos

El proyecto utiliza **Wrangler** para ejecutar y publicar tu Worker.

1. **Inicia desde una plantilla** usando `cargo generate`:
```bash
cargo generate cloudflare/workers-rs

```


*Hay varias plantillas para elegir. Verás un nuevo diseño de proyecto con un archivo `src/lib.rs`. ¡Empieza por ahí!*
2. **Ejecuta tu proyecto localmente**:
```bash
npx wrangler dev

```


3. **Publica tu Worker**:
```bash
# Configura tus rutas y zonas en el archivo `wrangler.toml`
npx wrangler deploy

```



---

## Característica `http`

La versión `worker 0.0.21` introdujo el flag `http`, que reemplaza tipos personalizados por tipos estándar del crate `http`. Esto facilita el uso de frameworks como **Axum** o **Hyper**.

Al activar esta característica:

* `req` en la macro `[event(fetch)]` se convierte en `http::Request<worker::Body>`.
* El tipo de retorno esperado es `http::Response<B>`.

### Ejemplo con Axum:

```rust
pub async fn root() -> &'static str {
    "Hello Axum!"
}

fn router() -> Router {
    Router::new().route("/", get(root))
}

#[event(fetch)]
async fn fetch(
    req: HttpRequest,
    _env: Env,
    _ctx: Context,
) -> Result<http::Response<axum::body::Body>> {
    Ok(router().call(req).await?)
}

```

---

## Uso del Router

Puedes parametrizar rutas y acceder a sus valores fácilmente.

```rust
use serde::{Deserialize, Serialize};
use worker::*;

#[event(fetch)]
pub async fn main(req: Request, env: Env, _ctx: worker::Context) -> Result<Response> {
    let router = Router::new();

    #[derive(Deserialize, Serialize)]
    struct Account { id: u64 }

    router
        .get_async("/account/:id", |_req, ctx| async move {
            if let Some(id) = ctx.param("id") {
                let accounts = ctx.kv("ACCOUNTS")?;
                return match accounts.get(id).json::<Account>().await? {
                    Some(account) => Response::from_json(&account),
                    None => Response::error("Not found", 404),
                };
            }
            Response::error("Bad Request", 400)
        })
        .run(req, env).await
}

```

---

## Durable Objects

Para definir un **Durable Object**, implementa el trait `DurableObject` y usa la macro `#[durable_object]`.

```rust
use worker::{durable_object, DurableObject, State, Env, Result, Request, Response};

#[durable_object]
pub struct Chatroom {
    state: State,
    env: Env,
}

impl DurableObject for Chatroom {
    fn new(state: State, env: Env) -> Self {
        Self { state, env }
    }

    async fn fetch(&self, _req: Request) -> Result<Response> {
        Response::ok("Hola desde Durable Object")
    }
}

```

### Configuración en `wrangler.toml`:

```toml
[durable_objects]
bindings = [
  { name = "CHATROOM", class_name = "Chatroom" }
]

[[migrations]]
tag = "v1"
new_classes = ["Chatroom"]

```

---

## Almacenamiento SQLite en Durable Objects

Los Durable Objects pueden usar SQLite para persistencia relacional.

```rust
// Acceso vía state.storage().sql()
let sql = state.storage().sql();
sql.exec("CREATE TABLE IF NOT EXISTS counter(value INTEGER);", None)?;

```

---

## Queues (Colas)

Para usar colas, activa el feature en tu `Cargo.toml`:
`worker = { version = "...", features = ["queue"] }`

```rust
#[event(queue)]
pub async fn main(message_batch: MessageBatch<MyType>, env: Env, _ctx: Context) -> Result<()> {
    for message in message_batch.messages()? {
        console_log!("Mensaje recibido: {:?}", message.body());
        message.ack();
    }
    Ok(())
}

```

---

## Pruebas con Miniflare

Para testear localmente tu Worker en Rust, usa **Miniflare** (basado en Node.js).

1. Instala: `npm install --save-dev wrangler miniflare`.
2. Construye: `wrangler deploy --dry-run`.
3. Configura el `scriptPath` hacia `./build/worker/shim.mjs`.

---

## Notas y FAQ

* **¿Puedo usar Tokio o async_std?** No. Todo debe compilar a `wasm32-unknown-unknown`. Sin embargo, `async/await` funciona nativamente.
* **Tamaño del binario:** Si excedes el límite, intenta optimizar el tamaño del `.wasm` eliminando dependencias innecesarias o usando configuraciones de optimización en Cargo.
* **APIs faltantes:** Si falta alguna funcionalidad de Workers, abre un *issue* en el repositorio oficial.

---

## Estructura del Proyecto

| Crate | Descripción |
| --- | --- |
| **worker** | Crate principal con abstracciones amigables de Rust. |
| **worker-sys** | Definiciones `extern "C"` para FFI con el Runtime de JS. |
| **worker-macros** | Macros para `[event(fetch)]` y `[durable_object]`. |
| **worker-build** | Comando de compilación multiplataforma. |

