# OMNI Pay DEMO — despliegue Firebase

Esta implementación mueve los saldos y el ledger de prueba al servidor. No integra bancos ni procesa dinero real.

## Requisitos del proyecto Firebase

1. El proyecto debe tener Authentication habilitado con al menos un proveedor que use OMNI (correo, teléfono o Google).
2. Crear/habilitar Cloud Firestore en modo producción.
3. Pasar el proyecto al plan Blaze de Firebase/Google Cloud: Cloud Functions requiere facturación habilitada, aunque el uso de pruebas pueda quedar dentro de cuotas sin cargo.
4. Tener Firebase CLI instalado y acceso de despliegue al proyecto.

## Desplegar

Desde la raíz del repositorio:

```bash
npm install -g firebase-tools
firebase login
firebase use --add
cd functions
npm install
npm run build
cd ..
firebase deploy --only functions,firestore:rules
```

Al ejecutar `firebase use --add`, seleccionar el mismo proyecto que corresponde al `google-services.json` utilizado para compilar la app. No compartir ni subir credenciales de servicio al repositorio.

## Funciones desplegadas

- `ensureDemoWallet`: crea una cuenta DEMO por UID autenticado con OM$ 50.000 iniciales, alias y CVU ficticios. Solo acredita el saldo inicial la primera vez.
- `transferDemoFunds`: transfiere por alias o CVU ficticio.
- `payDemoQr`: valida el QR de OMNI y transfiere el importe a la cuenta emisora.
- `topUpDemoWallet`: agrega saldo de prueba con límite de OM$ 50.000 por carga y OM$ 150.000 acumulados por cuenta.

Los movimientos de emisor y receptor y ambos saldos se escriben dentro de una única transacción Firestore. Las reglas impiden que la app cliente escriba directamente saldos o movimientos; solo las Cloud Functions con Admin SDK pueden hacerlo.

## Prueba entre dos teléfonos

1. Instalar la misma compilación en dos teléfonos.
2. Iniciar sesión con dos cuentas Firebase diferentes.
3. Abrir OMNI Pay en ambos y esperar a que aparezcan sus alias/CVU.
4. En el teléfono receptor, abrir **Cobrar QR** y mostrar el código.
5. En el teléfono emisor, tocar **Pagar QR**, escanear el código, indicar importe y confirmar.
6. Comprobar que el saldo del emisor baja, el del receptor sube y ambos historiales muestran la misma operación.
7. Repetir por alias o CVU desde **Transferir**.

No usar la misma cuenta en ambos teléfonos para esta prueba: cada dispositivo debe autenticarse con un UID distinto.

## Importante

- Todos los importes son ficticios y solo existen dentro del proyecto Firebase de pruebas.
- El CVU mostrado es un identificador interno de prueba, no un CVU del sistema financiero argentino.
- El QR solo es válido para pagos entre cuentas OMNI Pay DEMO.
- No desplegar esta arquitectura como billetera de dinero real. Para producción se necesita un proveedor financiero habilitado, controles regulatorios, prevención de fraude y auditoría especializada.
