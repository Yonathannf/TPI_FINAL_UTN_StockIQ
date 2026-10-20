# StockIQ — Despliegue

| Componente | Plataforma | Carpeta del repo |
|---|---|---|
| Base de datos | Railway (MySQL) | — |
| Backend | Render (Web Service con Docker) | `backend/` |
| Frontend | Vercel | `frontend/` |

El orden importa: primero la base, después el backend (necesita los datos de la base) y por último el frontend (necesita la URL del backend). Al final se vuelve a Render para habilitar el dominio de Vercel en CORS.

Railway, Render y Vercel cambian sus planes gratuitos con cierta frecuencia: antes de empezar, conviene revisar las condiciones vigentes de cada uno.

## 1. Base de datos en Railway

1. Crear un proyecto nuevo y agregar una base **MySQL**.
2. En la pestaña **Variables** de la base, anotar el host y el puerto **públicos** (los del TCP Proxy, porque Render está fuera de Railway), el usuario, la contraseña y el nombre de la base (suele ser `railway`).
3. No hace falta crear tablas: Flyway las crea al primer arranque del backend.

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
| `DB_URL` | `jdbc:mysql://HOST:PUERTO/BASE?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&useUnicode=true&characterEncoding=utf8`, con los datos públicos de Railway. |
| `DB_USERNAME` | Usuario de Railway. |
| `DB_PASSWORD` | Contraseña de Railway. |
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

## 4. Habilitar el frontend en CORS

En Render, cambiar `CORS_ALLOWED_ORIGINS` por la URL de Vercel (por ejemplo `https://stockiq.vercel.app`, sin barra final). Para seguir probando contra producción desde una máquina local, se pueden poner las dos separadas por coma. Render vuelve a desplegar solo.

## 5. Verificación

- Abrir la URL de Vercel e ingresar con `ADMIN_USERNAME` y `ADMIN_PASSWORD`.
- La primera vez, o después de un rato sin uso, el login puede tardar hasta un minuto: el plan gratuito de Render apaga el servicio cuando no recibe pedidos. La pantalla de login avisa cuando pasa.
- A partir de acá, cada push a `main` despliega automáticamente en Render y en Vercel.
- Completar las URLs en la sección "Entornos desplegados" del README.

## Problemas frecuentes

| Síntoma | Causa probable y solución |
|---|---|
| El login dice "No se pudo conectar con el servidor" y la consola del navegador muestra un error de CORS. | `CORS_ALLOWED_ORIGINS` no coincide exactamente con la URL de Vercel (revisar `https` y que no tenga barra final). |
| El backend no arranca: "No hay usuarios cargados y falta la contraseña del ENCARGADO inicial". | Falta `ADMIN_PASSWORD` en Render. |
| El backend no arranca: "JWT_SECRET debe tener al menos 32 caracteres". | Falta `JWT_SECRET` o es demasiado corto. |
| El deploy de Render falla con "Communications link failure". | `DB_URL` usa el host interno de Railway. Tiene que ser el host y el puerto públicos del TCP Proxy. |
| El backend falla con "Public Key Retrieval is not allowed". | Agregar `&allowPublicKeyRetrieval=true` al final de `DB_URL`. |
| Flyway falla con "Validate failed: Migration checksum mismatch". | Se modificó un script de migración que ya se había ejecutado. Las migraciones aplicadas no se editan: cualquier cambio va en una migración nueva (`V3__...`). |
| Recargar una página en Vercel da 404. | Falta `frontend/vercel.json`. |
