# StockIQ — Despliegue

| Componente | Plataforma | Carpeta del repo |
|---|---|---|
| Base de datos | Aiven (MySQL 8) | — |
| Backend | Render (Web Service con Docker) | `backend/` |
| Frontend | Vercel | `frontend/` |

El orden importa: primero la base, después el backend (necesita los datos de la base) y por último el frontend (necesita la URL del backend). Al final se vuelve a Render para habilitar el dominio de Vercel en CORS.

Aiven, Render y Vercel cambian sus planes gratuitos con cierta frecuencia: antes de empezar, conviene revisar las condiciones vigentes de cada uno.

> **Por qué Aiven y no Railway.** El diseño original usaba Railway para la base de datos, pero en octubre de 2026 Railway pausó la creación de cuentas nuevas después de un incidente de seguridad, sin fecha de reapertura. Se eligió Aiven porque ofrece MySQL 8 gratis, sin tarjeta de crédito y sin vencimiento. El cambio no afectó al código: el backend se conecta igual a cualquier MySQL y solo cambian los datos de conexión.

## 1. Base de datos en Aiven

1. Crear una cuenta en **aiven.io** y un proyecto (el nombre solo admite minúsculas, números y guiones; por ejemplo `stockiq`).
2. **Create service** → **MySQL** → plan **Free**. Si el asistente de bienvenida solo ofrece planes de prueba, usar **Skip service creation** y crear el servicio desde la consola, donde aparece el plan gratuito. Los planes de prueba consumen créditos y se apagan cuando vencen.
3. Esperar a que el servicio quede en **Running**.
4. En **Databases**, crear la base `stockiq_db`. Se evita el guion en el nombre de la base, porque en MySQL obliga a encerrarlo entre comillas invertidas y puede traer problemas con Flyway o con el driver de Java. La base `defaultdb` que viene por defecto se puede dejar como está.
5. En **Overview** → **Connection information**, anotar el host, el puerto (no suele ser 3306), el usuario (`avnadmin`) y la contraseña.
6. No hace falta crear tablas: Flyway las crea al primer arranque del backend.

Aiven puede apagar los servicios gratuitos que quedan mucho tiempo sin uso (avisa antes y se pueden volver a encender). Antes de una presentación conviene entrar al panel y confirmar que el servicio esté en **Running**.

## 2. Backend en Render

1. **New → Web Service** y conectar el repositorio de GitHub.
2. Configuración:
   - **Root Directory:** `backend`
   - **Runtime:** Docker (Render detecta el `Dockerfile`)
   - **Health Check Path:** `/actuator/health`
3. Variables de entorno (las lee `application-prod.yml`; las que no tienen valor por defecto son obligatorias y, si faltan, el backend no arranca):

| Variable | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod`. Sin esta variable, el backend arranca con el perfil `dev`. |
| `DB_URL` | `jdbc:mysql://HOST:PUERTO/stockiq_db?sslMode=REQUIRED&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&useUnicode=true&characterEncoding=utf8`, con el host y el puerto de Aiven. `sslMode=REQUIRED` es obligatorio: Aiven solo acepta conexiones cifradas. |
| `DB_USERNAME` | Usuario de Aiven (`avnadmin`). |
| `DB_PASSWORD` | Contraseña de Aiven. |
| `JWT_SECRET` | Texto aleatorio de al menos 32 caracteres (ver abajo). Nunca usar el de desarrollo. |
| `JWT_EXPIRATION_MINUTES` | Opcional. Por defecto `480` (8 horas). |
| `ADMIN_USERNAME` | Opcional. Por defecto `admin`. |
| `ADMIN_PASSWORD` | Contraseña del `ENCARGADO` inicial. Obligatoria en el primer arranque, cuando la tabla de usuarios está vacía. |
| `CORS_ALLOWED_ORIGINS` | Por ahora `http://localhost:5173`; se completa en el paso 4. |

Render define `PORT` automáticamente; el backend ya lo lee.

Para generar `JWT_SECRET` en PowerShell:

```powershell
$b = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
[Convert]::ToBase64String($b)
```

4. Desplegar y verificar que `https://<servicio>.onrender.com/actuator/health` devuelva `{"status":"UP"}`.

## 3. Frontend en Vercel

1. **Add New → Project** e importar el repositorio.
2. Configuración:
   - **Root Directory:** `frontend`
   - **Framework:** Vite (lo detecta solo)
3. Variable de entorno `VITE_API_URL` con la URL de Render, sin barra final.
4. Desplegar. El archivo `vercel.json` hace que las rutas de la app (por ejemplo `/catalogo`) funcionen al recargar la página.
5. En **Settings** → **Deployment Protection**, desactivar **Vercel Authentication**. Si queda activada, solo los miembros del equipo de Vercel pueden abrir la aplicación, y quien la evalúe no podría entrar.
6. Usar siempre la dirección de **producción** que figura en **Domains** (por ejemplo `https://tpi-final-utn-stock-iq.vercel.app`). Las direcciones largas con letras al azar corresponden a despliegues de prueba individuales: cambian con cada despliegue y están protegidas.

## 4. Habilitar el frontend en CORS

En Render, cambiar `CORS_ALLOWED_ORIGINS` por la URL de Vercel (por ejemplo `https://stockiq.vercel.app`, sin barra final). Para seguir probando contra producción desde una máquina local, se pueden poner las dos separadas por coma. Render vuelve a desplegar solo.

## 5. Verificación

- Abrir la URL de Vercel e ingresar con `ADMIN_USERNAME` y `ADMIN_PASSWORD`.
- La primera vez, o después de un rato sin uso, el login puede tardar hasta un minuto: el plan gratuito de Render apaga el servicio cuando no recibe pedidos. La pantalla de login avisa cuando pasa.
- A partir de acá, cada push a `main` despliega automáticamente en Render y en Vercel.
- Completar las URLs en la sección "Entornos desplegados" del README.

## 6. Datos de demostración (opcional)

Para mostrar la rotación, el ABC y las alertas con datos realistas, en Render cambiar `SPRING_PROFILES_ACTIVE` a `prod,demo`. Al arrancar, el backend carga un catálogo de ejemplo con 120 días de movimientos y dos operarios de prueba (`lgomez` y `mperez`, contraseña `demo1234`). La carga **solo corre si la base no tiene ningún movimiento**, así que no duplica datos si el backend se reinicia. Ver la sección "Datos de demostración" del README.

## Problemas frecuentes

| Síntoma | Causa probable y solución |
|---|---|
| El login dice "No se pudo conectar con el servidor" y la consola del navegador muestra un error de CORS. | `CORS_ALLOWED_ORIGINS` no coincide exactamente con la URL de Vercel (revisar `https` y que no tenga barra final). |
| El backend no arranca: "No hay usuarios cargados y falta la contraseña del ENCARGADO inicial". | Falta `ADMIN_PASSWORD` en Render. |
| El backend no arranca: "JWT_SECRET debe tener al menos 32 caracteres". | Falta `JWT_SECRET` o es demasiado corto. |
| El deploy de Render falla con "Communications link failure" o con errores de SSL. | Revisar que `DB_URL` tenga el host y el puerto exactos de Aiven, que incluya `sslMode=REQUIRED` y que el servicio de Aiven esté en **Running**. |
| El deploy falla con "Unknown database 'stockiq_db'". | La base no está creada en Aiven (**Databases**) o el nombre de `DB_URL` no coincide. |
| El login dice "El servidor tardó demasiado en responder". | El backend estaba apagado por falta de uso y tardó más de un minuto en volver. Abrir `/actuator/health` del backend, esperar a que muestre `UP` y volver a intentar. |
| Al abrir la dirección de Vercel pide iniciar sesión o solicitar acceso. | Está activada la protección de despliegues: desactivar **Vercel Authentication** en **Settings** → **Deployment Protection**, y usar la dirección de producción de **Domains**. |
| Vercel muestra "No Production Deployment". | Ningún despliegue quedó como producción. En **Deployments**, abrir el último de `main` → **⋯** → **Promote to Production**, o volver a importar el proyecto con **Root Directory** en `frontend`. |
| El backend falla con "Public Key Retrieval is not allowed". | Agregar `&allowPublicKeyRetrieval=true` al final de `DB_URL`. |
| Flyway falla con "Validate failed: Migration checksum mismatch". | Se modificó un script de migración que ya se había ejecutado. Las migraciones aplicadas no se editan: cualquier cambio va en una migración nueva (`V3__...`). |
| Recargar una página en Vercel da 404. | Falta `frontend/vercel.json`. |
