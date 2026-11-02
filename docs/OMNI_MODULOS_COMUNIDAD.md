# OMNI — módulos de comunidad y productividad

## Modo Rutina
- Las rutinas se guardan localmente en el dispositivo.
- Admiten título, mensaje, hora, días de repetición, activar/pausar, editar y eliminar.
- Los avisos se programan con AlarmManager y se publican como notificaciones.
- La primera vez Android solicita permiso para notificaciones en versiones compatibles.
- Los modos silencioso y sonoro usan canales de notificación separados.
- El modo voz deja el texto listo para lectura al abrir el aviso; no reproduce audio por sorpresa mientras la app está cerrada.
- La programación es local a cada dispositivo y se restaura al reiniciar el teléfono. Android puede retrasar avisos por ahorro de batería; para mayor puntualidad conviene permitir notificaciones y no restringir OMNI en el ahorro de batería.

## Bolsa de Trabajo y Servicios
- Las publicaciones se almacenan en Firestore en `jobPosts`.
- Las postulaciones se almacenan en `jobApplications` y se identifican por usuario + publicación para evitar duplicados.
- Los usuarios autenticados pueden buscar publicaciones, filtrar por tipo, postularse y contactar por WhatsApp o correo.
- Las publicaciones solo pueden ser editadas o eliminadas por su autor.
- Los enlaces a CV, títulos y certificados se comparten como enlaces externos. En esta etapa OMNI no aloja archivos ni requiere Firebase Storage.

## Eventos y Agenda
- Los eventos se almacenan en Firestore en `omniEvents`.
- Incluye búsqueda, filtros por categoría, favoritos locales, apertura de ubicación en mapas y envío de evento al calendario del teléfono.
- La opción Recordarme utiliza la aplicación de calendario disponible en el dispositivo; no crea una alarma propia de OMNI.

## Difusión y enlaces compartibles
- Las novedades manuales del portal de comerciante exponen una acción de compartir en cada tarjeta.
- Los eventos y las oportunidades laborales/servicios incluyen compartir el resumen por el menú nativo de Android.
- OMNI Buenos Días mantiene su acción de compartir el resumen.
- Las acciones usan el selector de aplicaciones instalado en el teléfono; no requieren API paga ni permisos adicionales.

## Pedidos QR y Delivery
El flujo de pedidos del comercio y sus límites DEMO están documentados en `docs/OMNI_PEDIDOS_QR.md`. El QR identifica una orden y nunca confirma pago. Los estados de preparación y entrega se actualizan desde el panel del comercio.

## Reglas de Firestore
La versión actual de `firestore.rules` incorpora permisos para `jobPosts`, `jobApplications` y `omniEvents`. Después de actualizar la app, publicar el archivo completo desde Firebase Console → Firestore Database → Rules. No reemplazar las reglas por reglas abiertas ni activar facturación.

## Seguridad y alcance
- Todas las publicaciones y eventos son visibles para usuarios autenticados.
- Los datos de contacto publicados serán visibles para esos usuarios; no cargar información sensible.
- Los módulos son funciones comunitarias de prueba. No constituyen verificación laboral, contratación, reserva garantizada ni servicio de emergencia.
- Las funciones de pedidos QR, delivery y antifraude real requieren un flujo de órdenes y validaciones de servidor. No deben confundirse con los pagos QR DEMO de OMNI Pay.
