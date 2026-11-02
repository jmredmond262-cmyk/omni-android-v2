# OMNI — módulos de comunidad y productividad

## Modo Rutina
- Las rutinas se guardan localmente en el dispositivo.
- Admiten título, mensaje, hora, días de repetición, activar/pausar, editar y eliminar.
- Los avisos se programan con AlarmManager y se publican como notificaciones.
- La primera vez Android solicita permiso para notificaciones en versiones compatibles.
- Los modos silencioso y sonoro usan canales de notificación separados.
- El modo voz deja el texto listo para lectura al abrir el aviso; no reproduce audio por sorpresa mientras la app está cerrada.
- La programación actual no se sincroniza entre dispositivos y Android puede retrasar avisos por ahorro de batería. La reprogramación tras reinicio queda pendiente de una siguiente mejora.

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

## Reglas de Firestore
La versión actual de `firestore.rules` incorpora permisos para `jobPosts`, `jobApplications` y `omniEvents`. Después de actualizar la app, publicar el archivo completo desde Firebase Console → Firestore Database → Rules. No reemplazar las reglas por reglas abiertas ni activar facturación.

## Seguridad y alcance
- Todas las publicaciones y eventos son visibles para usuarios autenticados.
- Los datos de contacto publicados serán visibles para esos usuarios; no cargar información sensible.
- Los módulos son funciones comunitarias de prueba. No constituyen verificación laboral, contratación, reserva garantizada ni servicio de emergencia.
- Las funciones de pedidos QR, delivery y antifraude real requieren un flujo de órdenes y validaciones de servidor. No deben confundirse con los pagos QR DEMO de OMNI Pay.
