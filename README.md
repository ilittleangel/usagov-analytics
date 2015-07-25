# usagov-analytics


Este repositorio contiene una aplicación para el análisis real-time y análisis batch del acortamiento mediante [bitly](https://bitly.com/) de url's de paginas web de EEUU, es decir paginas acabadas en .gov y en .mil. Estos datos los ofrece el gobierno de los EEUU en un stream continuo y en tiempo real, mediante eventos que contienen información del acortamiento. 

La información contenida en cada evento o click, es información sobre la geo-localización de un usuario que ha acortado una url, sobre el navegador utilizado, sobre el instante de tiempo en el que se produce el evento, la ciudad, el país y una serie de campos que permiten hacer analíticas en tiempo real. Estos eventos están serializados en formato JSON, lo que hace sencilla la interacción con las tecnologías de procesado y persistencia de los datos.

La aplicación hace uso de las siguientes tecnologías big data para el procesamiento, almacenamiento y renderización de resultados:

* [Hadoop Distributed File System](http://hadoop.apache.org/docs/r1.2.1/hdfs_design.html) HDFS como sistema de ficheros distribuido para almacenar los ficheros históricos que luego son procesados en la capa batch.
* [Spark](https://spark.apache.org/) como framework de procesamiento distribuido.
* [Spark Streaming](http://spark.apache.org/streaming/) dentro del framework de Spark, para la captura y procesado de los eventos que se están generando en tiempo real, mediante ventanas temporales.
* [Spark SQL](https://spark.apache.org/sql/) combinado con Spark Streaming, para hacer agregaciones de los eventos contenidos en una ventana temporal. Y para el análisis en batch, Spark SQL es usado para hacer analíticas en busca de patrones.
* [Cassandra](http://cassandra.apache.org/) como base de datos distribuida y motor NoSQL basado en un modelo columnar, Cassandra es usada para la persistencia de los datos analizados en real-time.
* [Elasticsearch](https://www.elastic.co/products/elasticsearch) como servidor de búsqueda distribuido usado para la renderización de los resultados y búsqueda de grupos de eventos.
* [Kibana](https://www.elastic.co/products/kibana) para la visualización mediante dashboards.


## Evolución del desarrollo del proyecto

Esta sección la voy a dividir en las diferentes fases en las que se ha desarrollado la realización del proyecto, ordenada de forma temporal:

1. El primer paso ha sido capturar mediante Spark Streaming cada uno de los clicks del pub/sub feed [http://developer.usa.gov/1usagov](http://developer.usa.gov/1usagov) e imprimirlo por la salida estándar.

2. El segundo paso ha sido procesar esta información con Spark SQL para obtener diferentes analíticas:
	* Número de clicks por segundo y minuto
	* Número de clicks por segundo y minuto agrupado por país
	* Contador total diario de clicks

3. El tercer paso ha consistido en escribir en tiempo real estos datos agregados en el motor NoSQL Cassandra para la persistencia y posterior renderización de los datos.

4. Una vez acabadas las 3 fases anteriores, llega la parte de presentación de resultados, en la que opté en primera instancia por crear una página web que hiciera peticiones a un servidor de aplicaciones mediante una webapp, la cual pudiera recuperar los datos de Cassandra y construir un JSON con los resultados, enviándolos de vuelta a la página web. Dichos resutlados podrian ser representados mediante librerías de visualización como [amcharts.js](http://www.amcharts.com/) y [D3.js](http://d3js.org/). Para ello monté esta arquitectura cliente-servidor que explicaré mas en detalle en el punto de "Codigo fuente".

5. Después de acabar con el modelo anterior y no salir convencido con la visualización final, opté por una herramienta que facilita la creación de cuadros de mando, Kibana. Para el uso de Kibana es necesario indexar los datos en Elasticsearch. Por lo tanto, en esta última etapa del proyecto he montado un servidor Elasticsearch y, mediante Spark Streaming, he indexado todos los clicks capturados, para finalmente usar Kibana para presentar estadísticas en tiempo real.

6. Por último, queda el procesamiento off-line o procesamiento batch, en la que a la vista de los buenos resultados de Elasticsearch y Kibana, decidí implementarlo con estas mismas tecnologías.


## Arquitectura Big Data

En la arquitectura que a continuación explicaré he incluido todos los puntos anteriores pese a que Elasticsearch y Kibana sustituyen a Cassandra y a la webapp como sistemas de almacenamiento y visualización.

La arquitectura esta dividida en 3 capas:

1. Procesamiento 
	* real-time
	* batch
2. Indexación y/o persistencia
3. Visualización de analíticas

![Screenshot](/screenshots/arquitectura-usagov-transparent.png?raw=true)

### 1. Procesamiento

#### REAL-TIME

Antes del procesamiento suele haber una capa de ingestión que para este proyecto no ha sido necesario implementar. Por lo tanto he incluido la fase de ingestión en la capa de procesamiento ya que Spark Streaming recoge los eventos directamente de la fuente y los procesa.

En el caso de usar **Cassandra** como sistema de persistencia de datos, tendríamos un job de Spark Streaming llamado `UsagovStreamingCassandra` que captura los eventos de 1usagov. Hace uso de Spark SQL para agregar la información según las unidades de tiempo (segundo y minuto) e igualmente por (segundo, minuto y país). Finalmente guarda en Cassandra en la conlumn familie que le corresponda, ya que para cada agregación existe una column familie distinta.

En el caso de **Elasticsearch**, hay otro job Spark Streaming llamado `UsagovStreamingElasticsearch` que recibe el stream de 1usagov de la misma forma pero en este caso se hacen una serie de transformaciones sobre cada evento, que permite a Elasticsearch, indexar los eventos basándose en un patrón time-based y así poder visualizar los datos en una linea temporal. Mas concretamente estas transformaciones consisten en enriquecer el dato de la siguiente forma: 
* remplazar el nombre de cada uno de los campos del JSON, por nombres intiutivos que luego se usaran para las busquedas en Elasticsearch
* añadir un campo `@timestamp` con la fecha del día que se esta procesando en formato `"yyyy-MM-dd'T'HH:mm:ss'Z'"`, para que Elasticsearch lo indexe como un campo tipo date y así configurar un `index pattern time-based`
* parsear el campo con la URL para obtener el dominio y así poder agrupar y sacar analíticas
* invertir las coordenadas del campo que contiene la geolocalización, ya que Elasticsearch invierte la latitud y la longitud si viene como un tipo array

El código de esta parte se encuentra en la carpeta **`streaming`**. Es un proyecto IntelliJ que utiliza maven como gestor de dependencias y está codificado en Scala.

#### BATCH

El proceso batch agrega la información almacenada en HDFS mediante Spark SQL para posteriormente indexarla con Elasticsearch. Igualmente se enriquece el dato añadiendo una campo `@timestamp` con la fecha del día de ejecución. A parte se usa un index distinto por cada una de las queries que se han implementado.

Esto se hace en un job llamdo `UsagovBatchElasticsearch` y el código se encuentra en la carpeta **`batch`**. Es un proyecto IntelliJ mavenizado y codificado en Scala.

### 2. Indexación y/o persistencia

Esta capa corresponde con el sistema de almacenamiento. Se ha dividido en una capa independiente para enfatizar la metodología usada para la persistencia y renderización de los datos. 

Como se han usado dos metodologías muy diferentes, explico las dos:

#### CASSANDRA

Cassandra se ha utilizado para persistir las agregaciones que se realizan en el job Spark llamado `UsagovStreamingCassandra`. Para ello se ha creado un job previo de configuración de Cassandra, también de Spark, llamado `UsagovSparkCassandraSetup` en el que se crea el `keyspace` `"usagov"` y una column family por cada una de las queries que la web necesita, ya que Cassandra usa el paradigma `query driven desing`.

El código se encuentra en la carpeta **`streaming`**.

Cassandra también se ha usado para la renderización de los resultados. Esta parte consiste en una webapp que hace uso de Servlets de Java para conectarse a Cassandra y serializar el resultado de las consultas en JSON y así poder enviar los datos a la web. 

El código se encuentra en la carpeta **`webapp`**. Es un proyecto IntelliJ mavenizado y codificado en Java.

#### ELASTICSEARCH

Se ha utilizado este motor de búsqueda distribuido para indexar cada uno de los eventos capturados por Spark Streaming.

Para dar respuesta tanto a la parte batch como a la parte real-time, se han creado previamente y mediante la API RESTfull de Elasticsearch, dos indices:

1. usagov-streaming
2. usagov-batch (este contiene dos `_type`, uno por cada query que se ha pretendido responder)

Antes de ejecutar la aplicación es necesario crear ambos índices mediante el siguiente las siguientes peticiones:

```
curl -XPUT 'localhost:9200/usagov-streaming?pretty' -d '
{
    "mappings" : {
      "data": {
        "properties": {
          "location": {
            "type": "geo_point",
            "lat_lon": true,
            "geohash": true
          }
        }
      }
    }
}'
```
Para el índice `usagov-streaming` solo es necesario indicar explícitamente a Elasticsearch que el datatype del campo `location` sea de tipo `geo_point` para que cuando lo indexe le asigne ese tipo. De esta forma se puede aplicar una función geo_hash que solo se aplica a campos de tipo `geo_point`. Esta función va a permitir agrupar puntos en un mapa y así poder contabilizar múltiples cliks en una misma celda dentro de un mapa.

```
curl -XPUT 'localhost:9200/usagov-batch?pretty' -d '
{
    "mappings" : {
      "query1": {
        "properties": {
          "hour": {
            "type": "date",
            "format": "HH"
          }
        }
      },
      "query2": {
        "properties": {
          "hour": {
            "type": "date",
            "format": "HH"
          }
        }
      }
    }
}'
```
Para el índice `usagov-batch` se ha mapeado el campo `hour` como tipo `date`, para luego poder hacer series temporales por hora.

### 3. Visualización de analíticas

Según la arquitectura hay dos formas de visualizar los datos: mediante una web y mediante Kibana.

#### WEBAPP

Esta parte consiste en

#### KIBANA



## Elección de herramientas

### Spark

Tanto para los procesos realtime como para los procesos offline que se han construido, se ha decidido utilizar Spark, ya que es una herramienta emergente que aspira a sustituir a MapReduce, al poder controlar el almacenamiento en memoria o en disco de las estructuras de datos distribuidos, llamados RDDs, ofreciendo una velocidad mayor puesto que ahorra accesos a memoria persistente.

Además de ser un framework de computación distribuida Open Source, tiene la ventaja de contener un stack de herramientas de alto nivel que incluye Spark SQL, MLlib para machine learning, GraphX para computación de grafos y Spark Streaming. Todas estas librerías se pueden combinar sin problemas en la misma aplicación.

En cuanto a la codificación de los jobs, se ha elegido utilizar Scala, ya que tiene su propia Api Spark y al cual se le puede importar librerías Java.

Para conectar Cassandra con Spark se ha utilizado la librería `spark-cassandra-connector` de Datastax con el que se puede almacenar directamente un RDD a una tabla de forma distribuida.

Para conectar Elasticsearch con Spark se ha utilizado la librería `elasticsearch-spark` de Elasticsearch con el que se puede almacenar directamente un RDD a un índice de forma distribuida.

### Spark Streaming


### Spark SQL


### HDFS


### Cassandra

La base de datos elegida en una primera instancia fue Cassandra, ya que aporta tiempos de escritura muy rápidos y permite escalabilidad sin tener un punto único de fallo. 

### Elasticsearch


### Java Servlets


### Kibana



## Pruebas



## Resultados

**STREAMING DASHBOARD**
![Screenshot](/screenshots/kibana-dashboard-usagov-streaming.png?raw=true)

**BATCH DASHBOARD**
![Screenshot](/screenshots/kibana-dashboard-usagov-batch1.png?raw=true)



## Análisis de escalabilidad



## Posibles mejoras

1. Un webcrapper que recoja todos los archivos históricos de measured voice y los persista en HDFS.
2. El job correspondiente con el proceso real-time, que persistiera los datos en HDFS, en ficheros compuestos por los eventos generados en una hora. Cada día un directorio en HDFS con 24 ficheros que contendrían todos los clicks de ese día.
3. El job correspondiente con el proceso batch, tendría que leer los eventos del directorio HDFS correspondiente con el día de ejecución e indexar en Elasticserach el análisis diario de accesos a dominios de EEUU.
4. Un job nuevo de tipo offline, que procesara los datos de todos los directorios almacenados en HDFS, es decir, de todos los días desde la primera vez que se ejecutó la aplicación. Esto seria para realizar procesamiento analítico de tipo predictivo con algoritmos de machine-learning aplicando técnicas de clustering como por ejemplo el método Kmeans. Las predicciones podrían ir desde predecir que ciudad del mundo tendrá mas accesos a una hora determindada, hasta por ejemplo determinar a que hora del día se llegará a una número de acortamientos de un dominio determinado.
5. Implementar un script de instalacion de todos los componentes necesarios para la ejecucion de la aplicación.
6. Implementar un script de ejecucion con el orden correcto en el que se tienen que lanzar los jobs para poder visualizar los resultados.


## Aprendizaje



## Ubicación código fuente



## Explicación código fuente

división en bloques funcionales


