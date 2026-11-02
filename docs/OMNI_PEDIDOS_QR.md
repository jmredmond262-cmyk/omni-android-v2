# OMNI Pedidos QR y Delivery — modo de prueba

## Flujo
1. El usuario encargado abre el Portal de Comerciante → Pedidos QR y Delivery. En esta etapa DEMO, la primera cuenta autenticada que active la ficha del comercio queda asociada como titular técnico del panel.
2. El cliente abre el detalle del comercio, agrega productos disponibles y elige retiro o delivery con dirección.
3. OMNI registra el pedido en Firestore con identificador, detalle, total estimado, cliente, comercio, nonce aleatorio y estado inicial `RECIBIDO`.
4. El cliente muestra el QR del pedido. El QR solo identifica el pedido; no acredita un pago.
5. El comercio puede escanear el QR para validar que coincide con un pedido guardado, pertenece a ese comercio y no superó 24 horas.
6. El comercio actualiza los estados: recibido → aceptado → en preparación → listo → en camino → entregado. Puede rechazar un pedido recién recibido.
7. El cliente ve el avance desde Pedidos OMNI.

## Seguridad y límites
- Las reglas Firestore restringen quién crea, lee y cambia estados de cada pedido.
- El cliente no puede marcar el pedido como pagado ni cambiar total, artículos o nonce después de crearlo.
- El estado de pago permanece `PENDIENTE`; el método de prueba es pagar al retirar o recibir.
- El comercio debe verificar productos, cantidades y total antes de aceptar. Los precios del pedido se originan en el catálogo cargado por la app cliente y no son una liquidación financiera firmada por servidor.
- La asociación inicial de titularidad del comercio es solo una comodidad de laboratorio: no verifica identidad legal ni evita que un usuario no autorizado intente reclamar primero una ficha sin propietario. No habilitar esto en producción sin verificación real del comercio.
- No hay ubicación GPS en vivo del repartidor, pasarela de pago real ni liquidación de fondos. El seguimiento disponible es por estados del pedido.
- La protección actual es una base DEMO de control de acceso, nonce y transiciones de estado; no equivale a un sistema antifraude financiero o de logística de producción.

## Reglas
Publicar el archivo completo `firestore.rules` actualizado desde Firebase Console → Firestore Database → Rules. Incluye también las reglas de Rutina/Agenda/Empleos en sus colecciones correspondientes. No usar reglas abiertas.
