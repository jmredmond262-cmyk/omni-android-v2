# OMNI Pay DEMO — configuración gratuita con Firebase Spark

OMNI Pay usa Firebase Authentication + Cloud Firestore. La lógica de transferencias, pagos QR, depósitos virtuales y movimientos se ejecuta desde la app mediante transacciones atómicas de Firestore. No se despliegan Cloud Functions y no hace falta activar Blaze para esta arquitectura.

## Qué permite probar

- Una billetera DEMO por cuenta autenticada.
- Crédito inicial ficticio de OM$ 50.000 por cuenta.
- Depósitos/cargas virtuales de hasta OM$ 50.000 por operación y OM$ 150.000 acumulados por cuenta.
- Transferencias entre cuentas OMNI Pay por alias o CVU ficticio.
- Cobros mediante QR: un usuario muestra su QR y otro lo escanea y paga.
- Actualización en tiempo real de saldos e historial en ambos usuarios.
- Validación de saldo suficiente, límites y prevención de transferencias a la propia cuenta.
- Historial de hasta 50 movimientos recientes por billetera.

## Requisitos

1. Firebase Authentication habilitado con el proveedor que usa la app.
2. Cloud Firestore creado en el mismo proyecto Firebase que corresponde al `google-services.json`.
3. Proyecto en el plan Spark (gratuito) o superior.
4. Publicar las reglas de `firestore.rules` desde Firebase Console o Firebase CLI.

## Publicar reglas

Desde la raíz del repositorio:

```bash
npm install -g firebase-tools
firebase login
firebase use --add
firebase deploy --only firestore:rules
```

También se pueden copiar las reglas del archivo `firestore.rules` en Firebase Console → Firestore Database → Rules y presionar **Publicar**. No activar facturación ni desplegar Functions para probar esta versión.

## Reglas compartidas con otros módulos OMNI

La versión más reciente de `firestore.rules` también protege las colecciones de comunidad:
- `jobPosts` y `jobApplications` para Bolsa de Trabajo y Servicios.
- `omniEvents` para Agenda y Eventos.

Si ya habías publicado las reglas de OMNI Pay antes de esta actualización, debés volver a publicar el archivo completo actualizado. No alcanza con conservar la versión anterior: los módulos nuevos recibirían `PERMISSION_DENIED`.

## Prueba entre dos teléfonos

1. Instalar la misma compilación en dos teléfonos.
2. Iniciar sesión con dos cuentas Firebase distintas.
3. Abrir OMNI Pay en ambos. La primera vez se crea una billetera con crédito ficticio de OM$ 50.000.
4. En el teléfono receptor, elegir **Mostrar mi QR**.
5. En el teléfono emisor, elegir **Pagar QR**, escanear el código, ingresar un importe y confirmar.
6. Verificar que el saldo del emisor disminuye, el del receptor aumenta y ambos historiales reflejan la operación.
7. Repetir desde **Transferir** usando el alias o CVU de prueba.
8. Probar **Agregar saldo** para simular un depósito virtual y comprobar los topes.

## Límites y seguridad

- No hay conexión bancaria, CBU/CVU real, tarjetas, retiros ni dinero real.
- Los depósitos son créditos ficticios de laboratorio; no representan dinero ingresado.
- Las reglas Firestore restringen los cambios de saldo a operaciones vinculadas y limitadas, pero esta arquitectura cliente es exclusivamente para un sandbox de pruebas. No debe usarse con fondos reales ni presentarse como billetera financiera de producción.
- Para mover dinero real o lanzar una billetera pública se necesitará un backend confiable, proveedor financiero habilitado, auditoría y cumplimiento regulatorio. Ese será un paso de crecimiento posterior, no un requisito de esta etapa gratuita.
