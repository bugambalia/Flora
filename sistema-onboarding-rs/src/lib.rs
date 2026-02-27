mod api;
mod core;
mod model;

use worker::*;

#[event(fetch)]
pub async fn main(req: Request, env: Env, _ctx: Context) -> Result<Response> {
    let router = Router::new()
        .post_async("/login", |req, ctx| async move {
            api::auth::login(req, ctx).await
        })
        .post_async("/ingresos", |req, ctx| async move {
            api::onboarding::crear_ingreso(req, ctx).await
        })
        .get_async("/cargos", |req, ctx| async move {
            api::onboarding::listar_cargos(req, ctx).await
        })
        .get_async("/protected", |req, ctx| async move {
            api::auth::protected_route(req, ctx).await
        });

    router.run(req, env).await
}
