# usagov-analytics


Este repositorio contiene una aplicación para el análisis real-time y análisis batch del acortamiento mediante [bitly](https://bitly.com/) de url's de paginas web de USA, es decir paginas acabadas en .gov y en .mil. Estos datos los ofrece el gobierno de los EEUU, en un stream continuo y en tiempo real, mediante eventos que contienen información del acortamiento. La información contenida en cada evento o click, es información sobre la geo-localización desde donde un usuario ha acortado una url, sobre el navegador utilizado, sobre el instante de tiempo en el que se produce el evento, la ciudad, el país y una serie de campos que permiten hacer analíticas en tiempo real. Estos eventos están serializados en formato JSON, lo que hace sencilla la interacción con las tecnologías de procesado y persistencia de los datos.

La aplicación hace uso de tecnologías big data para el almacenamiento, procesamiento y renderización de resultados:

* [Hadoop Distributed File System](http://hadoop.apache.org/docs/r1.2.1/hdfs_design.html) HDFS como sistema de ficheros distribuido para almacenar los ficheros históricos que luego son procesados en la capa batch.
* [Spark](https://spark.apache.org/) como framework de procesamiento distribuido.
* [Spark Streaming](http://spark.apache.org/streaming/) dentro del framework de Spark, para la captura y procesado de los eventos que se están generando en tiempo real, mediante ventanas temporales.
* [Spark SQL](https://spark.apache.org/sql/) combinado con Spark Streaming, para hacer agregaciones de los eventos contenidos en una ventana temporal. Y para el análisis en batch, Spark SQL es usado para hacer analíticas en busca de patrones.
* [Cassandra](http://cassandra.apache.org/) como motor NoSQL para la persistencia de los datos analizados en real-time.
* [Elasticsearch](https://www.elastic.co/products/elasticsearch) para la renderización de los resultados y búsqueda de grupos de eventos.
* [Kibana](https://www.elastic.co/products/kibana) para la visualización mediante dashboards.


## Evolución de a implementación del proyecto

Esta sección la voy a dividir en las diferentes fases en las que se ha desarrollado la realización del proyecto, ordenado temporalmente:

1. El primer paso ha sido capturar mediante Spark Streaming cada uno de los clicks del pub/sub feed [http://developer.usa.gov/1usagov](http://developer.usa.gov/1usagov) e imprimirlo por la salida estándar.

2. El segundo paso ha sido procesar esta información con Spark Streaming y Spark SQL para obtener diferentes analíticas:
	* número de clicks por segundo, minuto y hora
	* número de clicks por segundo, minuto y hora agrupado por país
	* contador total diario de clicks

3. Escribir en tiempo real estos datos en un motor NoSQL para la persistencia y posterior renderización de los datos.

4. Una vez acabado las 3 fases anteriores, llega la parte de presentación de resultados, en la que opté en una primera instancia por montar una página web que hiciera peticiones a un servidor de aplicaciones, mediante un webapp que recuperara los datos de Cassandra y construyera un JSON con los resultados y los enviara de vuelta a la página web donde poder pintar los datos con librerías de visualización como [amcharts.js](http://www.amcharts.com/) y [D3.js](http://d3js.org/). Para ello monté esta arquitectura cliente-servidor de la que explicaré mas detalladamente en el punto de "Codigo fuente".

5. Después de acabar con el modelo anterior y no salir convencido con esta parte tan importante, que es la presentación de datos, decidí darle una vuelta a la visualización y opté por una herramienta que facilita muchísimo la creación de cuadros de mando. Esta herramienta es Kibana. Para el uso de Kibana es necesario indexar toda tu información, tus datos en Elasticsearch. Por lo tanto en esta última etapa del proyecto he montado un servidor Elasticsearch y mediante Spark Streaming he indexado todos los clicks capturados en Elasticsearch y mediante Kibana poder presentar gráficas con las estadísticas en tiempo real.

6. Por último, queda el procesamiento off-line o procesamiento batch, en la que a la vista de los buenos resultados de Elasticsearch y Kibana, decidí implementarlo con estas mismas tecnologías.


## Arquitectura big data

En la arquitectura que a continuación explicaré he incluido todos los puntos anteriores pese a que Elasticsearch y Kibana sustituyen a Cassandra y a la webapp como sistemas de almacenamiento y visualización.

La arquitectura esta dividida en 3 capas:
1. Procesamiento 
	* real-time
	* batch
2. Indexación y/o persistencia
3. Visualización de analíticas

1. **Procesamiento**

	#### real-time
	Antes del procesamiento suele haber una capa de ingestión que para este proyecto no ha sido necesario implementar. Por lo tanto he incluido la fase de ingestión en la capa de procesamiento ya que Spark Streaming recoge los eventos directamente de la fuente y los procesa.

	En el caso de usar Cassandra como sistema de persistencia de datos, Spark Streaming recibe el stream de datos del servicio [1usagov](http://developer.usa.gov/1usagov) y mediante Spark SQL se agrega la información y se inserta en Cassandra.

	En el caso de Elasticsearch, Spark Streaming recibe el stream de 1usagov de la misma forma pero en este caso se hacen una serie de transformaciones sobre cada evento, que permite a Elasticsearch, indexar los eventos basándose en un patrón time-based y así poder visualizar los datos en una linea temporal.

	El código de esta parte se encuentra en la carpeta "streaming". Es un proyecto IntelliJ Idea mavenizado y codificado en Scala.

	#### batch
	El proceso batch agregar la información almacenada en HDFS mediante Spark SQL y la indexa en Elasticsearch. El código de este paso se encuentra en la carpeta ssbatch. Es un proyecto Eclipse que utiliza Maven y está codificado en Scala.

2. **Indexación y/o persistencia**



    

3. **Visualización de analíticas**

    

## Elección de herramientas de procesamiento

Para los procesos offline que se han construido, se ha decidido utilizar Spark, ya que es una herramienta emergente que aspira a sustituir a MapReduce, al poder controlar el almacenamiento en memoria o en disco de las estructuras de datos distribuidos, llamados RDDs, ofreciendo una velocidad mayor puesto que ahorra accesos a memoria persistente.

En cuanto a la codificación de los jobs, se ha elegido utilizar Scala, un lenguaje sencillo que puede importar librerías Java y que tiene su propia API Spark.

Para conectar la base de datos Cassandra con Spark se ha utilizado el plugin cassandra-spark de la empresa Datastax con el que se puede almacenar directamente un RDD a una tabla de forma distribuida.



## Pruebas



## Resultados (capturas de pantalla de los dashboards)



## Analisis de escalabilidad



## Posibles mejoras



## Cosas aprendidas



## Ubicacion codigo fuente



## Explicacion codigo fuente

division en bloques funcionales


