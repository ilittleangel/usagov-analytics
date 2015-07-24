# usagov-analytics
==================
Real-time analytics about shortened urls of US government websites

Este repositorio contiene una aplicación para el análisis real-time y análisis batch del acortamiento mediante [bitly](https://bitly.com/) de url's de paginas web de USA, es decir paginas acabadas en .gov y en .mil. Estos datos los ofrece el gobierno de los EEUU en un stream continuo y en tiempo real mediante eventos que contienen información del acortamiento que se produce en cada instante. Información sobre el usuario que produce el evento o click, es decir, información sobre la localización, navegador, timestamp... Estos eventos están serializados en formato JSON, lo que hace sencilla la interacción con las tecnologías de procesado y persistencia de los datos.

La aplicación hace uso de tecnologías big data, tanto para el procesamiento como para la renderización de los resultados.

* Tecnologías [Spark](https://spark.apache.org/):
	* [Spark Streaming](http://spark.apache.org/streaming/) para la captura y procesado de los eventos que se están generando en tiempo real, mediante ventanas temporales de segundos, minutos y horas.
	* [Spark SQL](https://spark.apache.org/sql/) combinado con Spark Streaming, para hacer agregaciones de los eventos contenidos en una ventana temporal. Y para el análisis en batch, Spark SQL es usado para hacer analíticas en busca de patrones.
* [Cassandra](http://cassandra.apache.org/) como motor NoSQL para la persistencia de los datos analizados en real-time.
* [Elasticsearch](https://www.elastic.co/products/elasticsearch) para la renderización de los resultados y búsqueda de grupos de eventos.
* [Kibana](https://www.elastic.co/products/kibana) para la visualización mediante dashboards.


## Trabajo hecho

Esta sección la voy a dividir en las diferentes fases en las que se ha desarrollado la realización del proyecto, ordenado temporalmente:

1. El primer paso ha sido capturar mediante Spark Streaming cada uno de los clicks del pub/sub feed [http://developer.usa.gov/1usagov](http://developer.usa.gov/1usagov) e imprimirlo por la salida estándar.

2. El segundo paso ha sido procesar esta información con Spark Streaming y Spark SQL para obtener diferentes analíticas:
	* número de clicks por segundo, minuto y hora
	* número de clicks por segundo, minuto y hora agrupado por país
	* contador total diario de clicks

3. Escribir en tiempo real estos datos en un motor NoSQL para la persistencia y posterior renderización de los datos.

4. Una vez acabado las 3 fases anteriores, llega la parte de presentación de resultados, en la que opté en una primera instancia por montar una página web que hiciera peticiones a un servidor de aplicaciones, mediante un webapp que recuperara los datos de Cassandra y construyera un JSON con los resultados y los enviara de vuelta a la página web donde poder pintar los datos con librerías de visualización como Ampcharts.js y D3.js. Para ello monté esta arquitectura cliente-servidor de la que explicaré mas detalladamente en el punto de "Codigo fuente".

5. Después de acabar con el modelo anterior y no salir convencido con esta parte tan importante, que es la presentación de datos, decidí darle una vuelta a la visualización y opté por una herramienta que facilita muchísimo la creación de cuadros de mando. Esta herramienta es Kibana. Para el uso de Kibana es necesario indexar toda tu información, tus datos en Elasticsearch. Por lo tanto en esta última etapa del proyecto he montado un servidor Elasticsearch y mediante Spark Streaming he indexado todos los clicks capturados en Elasticsearch y mediante Kibana poder presentar gráficas con las estadísticas en tiempo real.


## Arquitectura



## Porque lo he montado así



## Pruebas



## Resultados (capturas de pantalla de los dashboards)



## Analisis de escalabilidad



## Posibles mejoras



## Cosas aprendidas



## Ubicacion codigo fuente



## Explicacion codigo fuente

division en bloques funcionales


