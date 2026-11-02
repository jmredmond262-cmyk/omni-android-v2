# OMNI Admin Android

OMNI Admin es una aplicación Android independiente del cliente OMNI.

## Compilación
- Módulo: `:admin`
- Application ID: `com.omni.admin`
- APK debug: `admin/build/outputs/apk/debug/admin-debug.apk`
- Firebase: utiliza el mismo proyecto que OMNI mediante `admin/google-services.json`.

## Acceso administrativo
1. Crear o habilitar el usuario desde Firebase Authentication (correo/contraseña).
2. En Firestore crear manualmente `systemAdmins/{UID}` con:
   ```json
   { "role": "owner", "enabled": true }
   ```
3. No se permite crear ni elevar privilegios desde la aplicación.
4. El documento de autorización no debe exponerse en reglas de escritura a clientes.

## Primera entrega
- Inicio de sesión Firebase Auth.
- Verificación de autorización contra `systemAdmins/{uid}`.
- Resumen de perfiles, billeteras, saldo DEMO, operaciones, comercios, pedidos y eventos.
- Listado inicial de usuarios y operaciones.
- Catálogo inicial de módulos.
- Lectura y almacenamiento de parámetros administrativos con auditoría básica.

## Límites conocidos de esta iteración
- Las consultas se limitan a 500 documentos por colección; los indicadores se presentan como datos consultados, no como conteos ilimitados.
- La configuración guardada en `adminConfig/global` todavía no controla los límites ni el modo mantenimiento de OMNI Android. Requiere integración del lado del cliente y reglas financieras coordinadas antes de aplicar cambios operativos.
- La telemetría `usageEvents` queda habilitada en reglas, pero los eventos deben incorporarse en cada módulo Android.
- No hay movimientos reales de dinero: OMNI Pay permanece en modo DEMO.
- No se utiliza Cloud Functions ni servicios pagos.

## Colecciones
- `systemAdmins`: autorización administrativa, aprovisionada manualmente.
- `adminConfig/global`: parámetros administrados.
- `adminAuditLogs`: auditoría de cambios administrativos.
- `usageEvents`: telemetría mínima de módulos.
- `appFeedback`: comentarios de participantes.
