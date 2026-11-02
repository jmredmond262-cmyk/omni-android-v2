# OMNI Admin — nueva arquitectura

OMNI Admin se reconstruye como aplicación Android independiente. No utiliza Google Sign-In, no vincula credenciales del usuario OMNI y no accede a Firestore desde el dispositivo.

## Arquitectura

- Android Application ID: `com.omni.adminpanel`.
- Firebase Authentication: proyecto independiente, exclusivo para cuentas administrativas, con proveedor Email/Password.
- Datos: conserva el Firestore existente del proyecto `omni-57f69`.
- API: Cloud Function HTTPS `omniAdminApi`, desplegada en el proyecto de datos `omni-57f69`.
- Autorización: cada llamada valida el ID token emitido por el proyecto de autenticación y consulta `systemAdmins/{UID}` en el Firestore principal. El documento debe tener `enabled: true` y rol `owner` o `admin`.
- El cliente Android no incorpora credenciales de servicio ni permisos directos de Firestore.

## Configuración de Firebase necesaria

La configuración externa no puede generarse desde el repositorio: requiere acceso al proyecto Firebase del propietario.

1. Crear un proyecto Firebase exclusivo para OMNI Admin Authentication.
2. Habilitar Authentication → Sign-in method → Email/Password.
3. Registrar una aplicación Android con package name `com.omni.adminpanel`.
4. Descargar el `google-services.json` de esa aplicación.
5. En GitHub → Settings → Secrets and variables → Actions, crear:
   - `OMNI_ADMIN_GOOGLE_SERVICES_JSON_BASE64`: contenido del archivo codificado en Base64.
   - `OMNI_ADMIN_AUTH_PROJECT_ID`: ID del proyecto Firebase de autenticación.
6. Crear en ese proyecto de autenticación la cuenta administrativa con correo y contraseña. Confirmar el correo si la configuración lo exige.
7. Copiar el UID de esa cuenta y, en Firestore del proyecto original `omni-57f69`, crear `systemAdmins/{UID}`:
   ```json
   { "role": "owner", "enabled": true }
   ```

No se debe crear ese documento con un UID del proyecto equivocado: debe ser exactamente el UID emitido por el proyecto de autenticación nuevo.

## Despliegue del backend

Para desplegar la función al proyecto existente, crear el secreto de GitHub:

- `OMNI_PRIMARY_FIREBASE_SERVICE_ACCOUNT_BASE64`: JSON de una cuenta de servicio del proyecto `omni-57f69`, codificado en Base64, con permisos suficientes para desplegar Cloud Functions y actuar como service account.

Luego ejecutar manualmente el workflow **Deploy OMNI Admin API**. Este despliega únicamente `omniAdminApi`; no modifica las reglas ni los datos de las demás colecciones.

La función necesita el parámetro `OMNI_ADMIN_AUTH_PROJECT_ID`, que el workflow toma del secreto y entrega al despliegue.

## Construcción de la APK

El workflow **OMNI Admin Rebuild** valida el paquete Firebase, compila `:admin:assembleDebug`, verifica TypeScript y publica el artefacto `omni-admin-rebuild-debug-apk`.

## Funciones incluidas en esta versión

- Inicio de sesión Email/Password independiente y recuperación de contraseña.
- Validación de autorización en servidor para cada solicitud.
- Panel de indicadores y muestra de saldo DEMO.
- Lectura de usuarios, billeteras, operaciones, comercios, pedidos y comentarios.
- Lectura de módulos y activación/desactivación desde el panel.
- Configuración global: mantenimiento, mensaje, aviso y versiones.
- Registro de auditoría y creación de invitaciones beta.
- Separación de roles: solo `owner` puede guardar configuración y módulos; `owner` y `admin` pueden consultar y registrar invitaciones.

## Límites explícitos

- Los listados actuales cargan hasta 100 documentos por colección y el panel muestra ese límite.
- El saldo DEMO del resumen suma hasta 1.000 billeteras y se identifica como muestra.
- La configuración se guarda en Firestore, pero solo afectará al comportamiento de la app OMNI cuando sus módulos lean esos parámetros. No se cambia el cliente OMNI en este proyecto.
- No se habilitan operaciones de dinero real ni edición destructiva de cuentas de usuarios.
