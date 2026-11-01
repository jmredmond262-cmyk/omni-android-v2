package com.example.data
import com.example.model.TouristAttraction
import kotlin.math.*
object ArgentinaAttractions {
 private data class P(val n:String,val city:String,val lat:Double,val lon:Double,val cat:String,val time:String,val desc:String)
 private val places=listOf(
 P("Monumento a la Bandera","Rosario",-32.9475,-60.6300,"Historia","1 h","Monumento emblemático junto al río Paraná."),
 P("Parque Nacional Iguazú","Puerto Iguazú",-25.6953,-54.4367,"Naturaleza","Medio día","Senderos y miradores de las Cataratas."),
 P("Ruinas de San Ignacio Miní","San Ignacio",-27.2558,-55.5331,"Historia","2 h","Antiguas misiones jesuíticas."),
 P("Cerro de los Siete Colores","Purmamarca",-23.7467,-65.4995,"Naturaleza","1 h","Paisaje de cerros multicolores."),
 P("Quebrada de Humahuaca","Humahuaca",-23.2050,-65.3500,"Paisaje","Medio día","Valle andino Patrimonio de la Humanidad."),
 P("Parque General San Martín","Mendoza",-32.8895,-68.8770,"Paseo","2 h","Gran parque urbano con lago y vistas a la montaña."),
 P("Centro Cívico","Bariloche",-41.1335,-71.3103,"Arquitectura","1 h","Punto clásico junto al lago Nahuel Huapi."),
 P("Circuito Chico","Bariloche",-41.0880,-71.5350,"Naturaleza","3 h","Recorrido panorámico por lagos y montañas."),
 P("Glaciar Perito Moreno","El Calafate",-50.4960,-73.1370,"Naturaleza","Medio día","Frente glaciar en el Parque Nacional Los Glaciares."),
 P("Canal Beagle","Ushuaia",-54.8070,-68.3030,"Naturaleza","3 h","Paisajes australes y navegación."),
 P("Parque Nacional Tierra del Fuego","Ushuaia",-54.6660,-68.5000,"Naturaleza","Medio día","Bosques, costas y senderos australes."),
 P("Teatro Colón","Buenos Aires",-34.6011,-58.3831,"Cultura","1 h 30 min","Teatro lírico de arquitectura y acústica reconocidas."),
 P("Obelisco","Buenos Aires",-34.6037,-58.3816,"Monumento","45 min","Monumento icónico sobre la avenida 9 de Julio."),
 P("Parque Nacional Talampaya","Villa Unión",-29.8000,-67.8500,"Naturaleza","Medio día","Cañones rojizos y formaciones geológicas."),
 P("Casa Histórica","Tucumán",-26.8320,-65.2030,"Historia","1 h","Sitio donde se declaró la independencia argentina."),
 P("Parque Sarmiento","Córdoba",-31.4260,-64.1810,"Paseo","2 h","Espacio verde tradicional con lagos y museos."),
 P("Esteros del Iberá","Colonia Carlos Pellegrini",-28.5360,-57.1780,"Naturaleza","Medio día","Humedales con gran diversidad de fauna."),
 P("Monumento a Güemes","Salta",-24.7820,-65.4140,"Historia","45 min","Monumento dedicado al héroe gaucho.")
 )
 fun nearby(lat:Double,lon:Double,limit:Int=6):List<TouristAttraction> = places.map { p ->
  val d1=Math.toRadians(p.lat-lat); val d2=Math.toRadians(p.lon-lon)
  val h=sin(d1/2).pow(2.0)+cos(Math.toRadians(lat))*cos(Math.toRadians(p.lat))*sin(d2/2).pow(2.0)
  p to (6371.0*2.0*atan2(sqrt(h),sqrt(1-h)))
 }.sortedBy{it.second}.take(limit).mapIndexed{i,pair ->
  val p=pair.first
  TouristAttraction("ar_att_"+i,p.n,p.cat,pair.second,p.time,p.desc+" · "+p.city,4.7f,0.5f,0.5f)
 }
}
