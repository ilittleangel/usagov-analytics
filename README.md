# usagov-analytics

## Análisis real-time y análisis batch de urls acortadas del gobierno de EEUU

Este repositorio contiene una aplicación para el análisis real-time y análisis batch del acortamiento mediante [bitly](https://bitly.com/) de url's de paginas web de EEUU, es decir paginas acabadas en .gov y en .mil. Estos datos los ofrece el gobierno de los EEUU en un stream continuo y en tiempo real, mediante eventos que contienen información del acortamiento. 

La información contenida en cada evento o click, es información sobre la geo-localización de un usuario que ha acortado una url, sobre el navegador utilizado, sobre el instante de tiempo en el que se produce el evento, la ciudad, el país y una serie de campos que permiten hacer analíticas en tiempo real. Estos eventos están serializados en formato JSON, lo que hace sencilla la interacción con las tecnologías de procesado y persistencia de los datos.

### Tabla de contenidos
- **[Tecnologías Big Data utilizadas](#tecnologías-big-data-utilizadas)**
- **[Evolución en el desarrollo del proyecto](#evolución-en-el-desarrollo-del-proyecto)**
- **[Elección de herramientas](#elección-de-herramientas)**
- **[Arquitectura Big Data](#arquitectura-big-data)**
- **[Pruebas](#pruebas)**
- **[Resultados con Kibana](#resultados-con-kibana)**
- **[Resultados con la Webapp](#resultados-con-la-webapp)**
- **[Análisis de patrones](#análisis-de-patrones)**
- **[Análisis de escalabilidad](#análisis-de-escalabilidad)**
- **[Posibles mejoras](#posibles-mejoras)**
- **[Aprendizaje](#aprendizaje)**
- **[Ubicación código fuente](#ubicación-código-fuente)**
- **[Análisis del código fuente](#análisis-del-código-fuente)**
- **[Conclusiones](#conclusiones)**

## Tecnologías Big Data utilizadas

La aplicación hace uso de las siguientes tecnologías big data para el procesamiento, almacenamiento y renderización de resultados:

* [Hadoop Distributed File System](http://hadoop.apache.org/docs/r1.2.1/hdfs_design.html) HDFS como sistema de ficheros distribuido para almacenar los ficheros históricos que luego son procesados en la capa batch.
* [Spark](https://spark.apache.org/) como framework de procesamiento distribuido.
* [Spark Streaming](http://spark.apache.org/streaming/) dentro del framework de Spark, para la captura y procesado de los eventos que se están generando en tiempo real, mediante ventanas temporales.
* [Spark SQL](https://spark.apache.org/sql/) combinado con Spark Streaming, para hacer agregaciones de los eventos contenidos en una ventana temporal. Y para el análisis en batch, Spark SQL es usado para hacer analíticas en busca de patrones.
* [Cassandra](http://cassandra.apache.org/) como base de datos distribuida y motor NoSQL basado en un modelo columnar, Cassandra es usada para la persistencia de los datos analizados en real-time.
* [Elasticsearch](https://www.elastic.co/products/elasticsearch) como servidor de búsqueda distribuido usado para la persistencia de los datos y búsqueda de grupos de eventos.
* [Kibana](https://www.elastic.co/products/kibana) como plataforma de visualizacion real-time y de analítica mediante gráficas y dashboards. Se ha usado para la renderizacion de los resultados bajo Elasticsearch.


## Evolución en el desarrollo del proyecto

Esta sección la voy a dividir en las diferentes fases en las que se ha desarrollado la realización del proyecto, ordenada de forma temporal:

1. El primer paso ha sido capturar mediante Spark Streaming cada uno de los clicks del pub/sub feed [http://developer.usa.gov/1usagov](http://developer.usa.gov/1usagov) e imprimirlo por la salida estándar.

2. El segundo paso ha sido procesar esta información con Spark SQL para obtener diferentes analíticas:
	* Número de clicks por segundo y minuto
	* Número de clicks por segundo y minuto agrupado por país
	* Contador total diario de clicks

3. El tercer paso ha consistido en escribir en tiempo real estos datos agregados en el motor NoSQL Cassandra para la persistencia y posterior renderización de los datos.

4. Una vez acabadas las 3 fases anteriores, llega la parte de presentación de resultados, en la que opté en primera instancia por crear una página web que hiciera peticiones a un servidor de aplicaciones mediante una webapp, la cual pudiera recuperar los datos de Cassandra y construir un JSON con los resultados, enviándolos de vuelta a la página web. Dichos resultados podrían ser representados mediante librerías de visualización como [amcharts.js](http://www.amcharts.com/) y [D3.js](http://d3js.org/). Para ello monté esta arquitectura cliente-servidor que explicaré mas en detalle en el punto "Análisis del Codigo fuente".

5. Después de acabar con el modelo anterior y no salir convencido con la visualización final, opté por una herramienta que facilita la creación de cuadros de mando, Kibana. Para el uso de Kibana es necesario indexar los datos en Elasticsearch. Por lo tanto, en esta última etapa del proyecto he montado un servidor Elasticsearch y, mediante Spark Streaming, he indexado todos los clicks capturados, para finalmente usar Kibana para presentar estadísticas en tiempo real.

6. Por último, queda el procesamiento off-line o procesamiento batch, en la que a la vista de los buenos resultados de Elasticsearch y Kibana, decidí implementarlo con estas mismas tecnologías.


## Elección de herramientas

#### Spark

Tanto para los procesos realtime como para los procesos offline que se han construido, se ha decidido utilizar Spark, ya que es una herramienta emergente que aspira a sustituir a MapReduce, al poder controlar el almacenamiento en memoria o en disco de las estructuras de datos distribuidos, llamados RDDs (Resilient Distributed Dataset), ofreciendo una velocidad mayor puesto que ahorra accesos a memoria persistente.

Además de ser un framework de computación distribuida Open Source, tiene la ventaja de contener un stack de herramientas de alto nivel que incluye Spark SQL, MLlib para machine learning, GraphX para computación de grafos y Spark Streaming. Todas estas librerías se pueden combinar sin problemas en la misma aplicación.

En cuanto a la codificación de los jobs, se ha elegido utilizar Scala, ya que tiene su propia Api Spark y al cual se le puede importar librerías Java.

Para conectar Cassandra con Spark se ha utilizado la librería `spark-cassandra-connector` de la empresa [Datastax](https://github.com/datastax/spark-cassandra-connector), con el que se puede almacenar directamente un RDD a una tabla de forma distribuida.

La librería `elasticsearch-Hadoop` de la empresa [Elastic](https://www.elastic.co/guide/en/elasticsearch/hadoop/master/spark.html) ofrece integración nativa entre Elasticsearch y Apache Spark, con lo que se puede almacenar directamente un RDD a un índice de forma distribuida.

#### Spark Streaming

Dentro del framework Spark, Spark Streaming es la librería de procesamiento real-time y por tanto ofrece todo el lenguaje del API de Spark. Ha sido elegido porque permite implementar streaming jobs de la misma forma que se escriben batch jobs. 

Al igual que Spark, Spark Streaming ofrece todas las características de un framework Big Data, es decir, procesamiento distribuido, escalabilidad y tolerancia a fallos, gracias a la principal abstracción de la tecnología Spark, los RDDs.

La elección de Spark Streaming como framework de computación real-time, es porque se ajusta perfectamente a las necesidades de analítica que este proyecto necesitaba. 

Para el caso de Cassandra como sistema de persistencia, Spark Streaming ha permitido capturar los eventos del feed `1usagov` y procesarlos en ventanas temporales para luego insertar el resultado en la column familie correspondiente. Esto es gracias a la integración de Spark con Cassandra que ofrece `spark-cassandra-connector`.

Para el caso de Elasticsearch, Spark tiene una integración perfecta mediante la librería `elasticsearch-spark`. Esto permite indexar RDDs en Elasticsearch de forma muy sencilla.

#### Spark SQL

Spark SQL me ha permitido hacer los cálculos sobre las agrupaciones de eventos con lenguaje SQL. Gracias a la abstracción que ofrece la estructura `SchemaRDD` de Spark, se pueden crear tablas a partir de objetos JSON y usar cada campo como elementos de una tabla.

#### HDFS

Spark también ofrece total integración con Hadoop por lo que es posible almacenar los ficheros históricos de clicks en HDFS y distribuirlos por el cluster. Spark permite de forma también muy sencilla leer de HDFS.

#### Cassandra

La primera opción al elegir el sistema para persistir los datos fue Cassandra ya que es una base de datos distribuida y descentralizada que aporta tiempos de escritura muy rápidos y permite escalabilidad transparente sin tener un punto único de fallo. 

Ademas tiene consistencia configurable en 3 niveles `quorum` (la mitad + 1 de las maquinas tienen que responder), `all` y `one`. Esto ofrece alta disponibilidad, pero con consistencia débil.

Tambien porque Spark ofrece, como ya se ha comentado antes, una integración completa con Cassandra. Esto incluye la interacción mediante `CQL` (Cassandra Query Language). 

#### Elasticsearch

Elasticsearch es un servidor de búsqueda distribuido, es decir un buscador Big Data, basado en Lucene. Esto permite la indexación y búsqueda de texto completo. Elasticsearch provee una interfaz web RESTfull que funciona mediante documentos JSON. 

Permite escalabilidad horizontal, alta disponibilidad y analíticas en tiempo real gracias a su velocidad de respuesta. 

Por todas estas características y por la necesidad de indexar la información en este motor de búsqueda para poder construir gráficas analíticas en Kibana, elegí Elasticsearch como sistema de persistencia de los datos.

#### Kibana

Kibana permite la visualización en tiempo real de datos indexados en Elasticsearch. Permite construir dashboards analíticos de forma muy sencilla. La elección de Kibana es por ser una herramienta de referencia en visualización real-time, perfecto para el caso de uso de este proyecto.


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

#### Real-time

Antes del procesamiento suele haber una capa de ingestión que para este proyecto no ha sido necesario implementar. Por lo tanto he incluido la fase de ingestión en la capa de procesamiento ya que Spark Streaming recoge los eventos directamente de la fuente y los procesa.

En el caso de usar **Cassandra** como sistema de persistencia de datos, tendríamos un job de Spark Streaming llamado `UsagovStreamingCassandra` que captura los eventos de `1usagov`. Hace uso de Spark SQL para agregar la información según las unidades de tiempo (segundo y minuto) e igualmente por (segundo, minuto y país). Finalmente guarda en Cassandra en la conlumn familie que le corresponda, ya que para cada agregación existe una column familie distinta.

En el caso de **Elasticsearch**, hay otro job Spark Streaming llamado `UsagovStreamingElasticsearch` que recibe el stream de 1usagov de la misma forma pero en este caso se hacen una serie de transformaciones sobre cada evento, que permite a Elasticsearch, indexar los eventos basándose en un patrón _time-based_ y así poder visualizar los datos en una linea temporal. Mas concretamente estas transformaciones consisten en enriquecer el dato de la siguiente forma: 
* remplazar el nombre de cada uno de los campos del JSON, por nombres intiutivos que luego se usaran para las busquedas en Elasticsearch
* añadir un campo `@timestamp` con la fecha del día que se esta procesando en formato `"yyyy-MM-dd'T'HH:mm:ss'Z'"`, para que Elasticsearch lo indexe como un campo tipo date y así configurar un _index pattern time-based_
* parsear el campo con la URL para obtener el dominio y así poder agrupar y sacar analíticas
* invertir las coordenadas del campo que contiene la geolocalización, ya que Elasticsearch invierte la latitud y la longitud si viene como un tipo array

El código de esta parte se encuentra en la carpeta **`streaming`**. Es un proyecto IntelliJ que utiliza Maven como gestor de dependencias y está codificado en Scala.

#### Batch

El proceso batch agrega la información almacenada en HDFS mediante Spark SQL para posteriormente indexarla con Elasticsearch. Igualmente se enriquece el dato añadiendo una campo `@timestamp` con la fecha del día de ejecución. A parte se usa un index distinto por cada una de las queries que se han implementado.

Esto se hace en un job llamdo `UsagovBatchElasticsearch` y el código se encuentra en la carpeta **`batch`**. Es un proyecto IntelliJ mavenizado y codificado en Scala.

### 2. Indexación y/o persistencia

Esta capa corresponde con el sistema de almacenamiento. Se ha dividido en una capa independiente para enfatizar en la metodología usada para la persistencia e indexación de los datos. 

Como se han usado dos metodologías muy diferentes, explico las dos:

#### Base de datos NoSQL Cassandra

Cassandra se ha utilizado para persistir las agregaciones que se realizan en el job Spark llamado `UsagovStreamingCassandra`. Para ello se ha creado un job previo de configuración de Cassandra, también de Spark, llamado `UsagovSparkCassandraSetup` en el que se crea el `keyspace` y una `column family` por cada una de las queries que la web necesita, ya que Cassandra usa un paradigma tipo _query driven desing_.

El código se encuentra en la carpeta **`streaming`**.

Cassandra también se ha usado para la renderización de los resultados. Esta parte consiste en una webapp que hace uso de Servlets de Java para conectarse a Cassandra y serializar el resultado de las consultas en JSON y así poder enviar los datos a la web. 

El código se encuentra en la carpeta **`webapp`**. Es un proyecto IntelliJ mavenizado y codificado en Java.

La `keyspace` de Cassandra creado para esta aplicación es `usagov` y la su definicion de las `Partition Key` y `Clustering Key` de sus `column families` se encuentra en la clase `UsagovSparkCassandraSetup`. El modelo contiene las siguiente column families:
- clicks
- topcountryminutes
- topcountryseconds
- topdomainbycountryminutes
- topdomainbycountryseconds
- topdomainminutes
- topdomainseconds

#### Buscador distruido Elasticsearch

Se ha utilizado este motor de búsqueda distribuido para indexar cada uno de los eventos capturados por Spark Streaming.

Para dar respuesta tanto a la parte batch como a la parte real-time, se han creado previamente y mediante la RESTful API de Elasticsearch, dos indices:

1. `usagov-streaming`
2. `usagov-batch` (este contiene dos `_type`, uno por cada query que se ha pretendido responder)

Antes de ejecutar la aplicación sería necesario crear ambos índices mediante las siguientes peticiones:

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
Para el índice `usagov-streaming` solo es necesario indicar explícitamente a Elasticsearch que el datatype del campo `location` sea de tipo `geo_point` para que cuando lo indexe le asigne ese tipo. De esta forma se puede aplicar una función `geo_hash` que solo se aplica a campos de tipo `geo_point`. Esta función va a permitir agrupar puntos en un mapa y así poder contabilizar múltiples cliks en una misma celda dentro de un mapa.

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

Según la arquitectura, la capa de visualización contiene dos formas de visualizar los datos: mediante una web y mediante Kibana.

#### Webapp

Este módulo está divido en los siguientes componentes:
* Pagina Web donde se muestran los datos.
* Webapp con Servlets de Java que recuperan los datos de Cassandra.
* Código Javascript que hace las peticiones a los servlets y renderizan los datos en gráficas.
* Servidor de aplicaciones Tomcat 7 donde se ejecutan las webapps.

El código se encuentra en la carpeta **`webapp`**. Es un proyecto IntelliJ mavenizado y esta codificado en Java, Javascript y HTML.

En esta parte se han obtenido gráficas con ciertas limitaciones respecto a la siguiente opción de visualización.

#### Dashboards en Kibana

Kibana es una herramienta que tiene una integración perfecta con Elasticsearch. De ahí la necesidad de indexar cada evento para poder construir de una forma sencilla, dashboards intuitivos y agradables para el análisis de datos. 

En los screenshots que se muestran mas adelante se explicará cada una de las analíticas en tiempo real que Elastisearch y Kibana permiten construir.

Una vez instalado y arrancado, Kibana se encuentra escuchando en el puerto 5601, por lo que podemos acceder escribiendo en el navagador `http://localhost:5601` 


## Pruebas

#### Pruebas en el entorno de desarrollo local:


![Screenshot](/screenshots/pruebas-elasticsearch.png?raw=true) 

**Streaming**

| jobs				 | tiempo de ejecución mantenido | hora de ejecución | clicks cargados |
| ------------------------------ |-------------------------------| ----------------- | --------------- |
| `UsagovStreamingElasticsearch` | 1 h                           | 12:45 - 13:45     | 4.613           |
| `UsagovStreamingElasticsearch` | 1 h                           | 19:30 - 20:30     | 4.738	       |
| `UsagovStreamingCassandra`     | 10 min       	         | 10:51 - 11:01     | 715             |
| `UsagovStreamingCassandra`     | 20 min                        | 21:00 - 21:20     | 1468	       |


**Batch**

| jobs		             | tiempo de ejecución  | clicks procesados |
| ---------------------------|----------------------| ----------------- |
| `UsagovBatchElasticsearch` | 3 min 17 sg  	    | 429.289           |



## Resultados con Kibana

#### STREAMING DASHBOARD
Este dashboard nos muestra en tiempo real, los documentos que se están indexando en cada instante en Elasticsearch en el indice `usagov-streaming`. Se puede definir el tiempo de refresco de los datos al igual que la franja temporal que se desea mostrar. 

Las gráficas de este dashboard son: 

- Histograma con el total de documentos indexados por minuto
- Contador con el total de clicks indexados durante la hora de ejecución
- Piechart del método de acortamiento mas utilizado
- Diagrama de barras con el top 20 por timezone
- Diagrama de barras con el top 50 de dominios mas acortados
- Mapa de calor de acortamientos de urls
- Usuarios Conocidos (1) VS Usuarios nuevos (0)

El screenshot correponde con la ejecución de 1h 
![Screenshot](/screenshots/kibana-dashboard-usagov-streaming.png?raw=true)


#### BATCH DASHBOARD
Este dashboard nos muestra las analíticas batch sobre datos históricos que han sido indexadas en Elasticsearch en el indice `usagov-batch`. Los gráficos mostrados en el screenshots son:

- Histograma con el total de clicks en cada hora del día. Esto nos permite saber a que hora del dia se producen mas accesos.
- Contador total de clicks procesados en el job 
- Un linecharts con el total de clicks en cada hora, por cada timezone

![Screenshot](/screenshots/kibana-dashboard-usagov-batch1.png?raw=true)



## Resultados con la Webapp

- Contador con el total de clicks diario
- diagramas de barras con el total de clicks diarios por país

![Screenshot](/screenshots/webapp-barchart-toppaises.png?raw=true)

- Top list de los accesos a dominios representando un contador total por dominio

![Screenshot](/screenshots/webapp-toplist-dominios.png?raw=true)

- Al hacer click en un dominio, se representa la evolución diaria de clicks para ese dominio

![Screenshot](/screenshots/webapp-areachart-evoluciondominio.png?raw=true)


## Análisis de patrones

Para esta parte se ha decidido hacer una gráfica por cada timezone. Se han elegido los timezones que he considerado mas relevantes por el numero de clicks que contienen. 

En el dashboard están colocadas las gráficas verticalmente por timezone. De esta forma podemos comparar en la primera columna de gráficas, los clicks en Norte América, en la segunda columna los clicks en Asia, la tercera los clicks en Australia y en la última columna los clicks en Europa.

Se observan algunos patrones de acortamientos de URLs dependiendo del timezone:
- **Norte América**: se ve claramente que hay un aumento notable de clicks entre las **18:00** y las **20:00**.
- **Asia**: se ve que es bastante irregular la comparación de clicks en timezones de Asia.
- **Australia**: hay un patrón muy definido en todos los timezone de Australia. Ese pico muestra el aumento de clicks a las **10:00**.
- **Europa**: se observa que en todas los timezone de Europa hay in valle en las horas de sueño, lo que indica una disminución muy notable en los clicks en las principales ciudades de Europa. Además también se observa un pequeño aumento entre las **10:00** y las **12:00**. Se observa tambien que todas las gráficas tiene un aumento de clicks en las horas de la tarde cercanas a las **19:00**, algo que de primeras no parece relevante. Pero si parece mas relevante el patrón entre las **22:00** y las **00:00**, en el que hay un descenso y justo a continuación un pico de acortamientos o clicks.

![Screenshot](/screenshots/kibana-dashboard-usagov-batch1.png?raw=true)



## Análisis de escalabilidad

Una arquitectura Big Data suele ser siempre mas o menos escalable, pues las tecnologías que la componen escalan por definición. Concretamente en este caso, la arquitectura es escalable por ese motivo pero se puede producir una situación en la que el sistema no escale debidamente por no haber incluido en la arquitectura una capa de ingestión de datos. Dado el volumen de clicks generados en cada instante, durante los periodos de pruebas, no ha sido necesario esta capa. Si se produjera un pico muy alto de acortamientos de URLs posiblemente si habría que incluirla para evitar dicho problema de escalabilidad.

Concretamente con Cassandra y Elasticsearch la escalabilidad está asegurada pues una de sus principales propiedades son la gran velocidad en escrituras.

Sobre Kibana, al funcionar bajo Elasticsearch, tambien escalaría perfectamente. 


## Posibles mejoras

1. Incluir un sistema de ingestion de datos mediante colas, que asegure el procesamiento de grandes picos de clicks.
2. Un webcrapper que recoja todos los archivos históricos de measured voice y los persista en HDFS.
3. El job correspondiente con el proceso real-time, que persistiera los datos en HDFS, en ficheros compuestos por los eventos generados en una hora. Cada día un directorio en HDFS con 24 ficheros que contendrían todos los clicks de ese día.
4. El job correspondiente con el proceso batch, tendría que leer los eventos del directorio HDFS correspondiente con el día de ejecución e indexar en Elasticserach el análisis diario de accesos a dominios de EEUU.
5. Un job nuevo de tipo offline, que procesara los datos de todos los directorios almacenados en HDFS, es decir, de todos los días desde la primera vez que se ejecutó la aplicación. Esto seria para realizar procesamiento analítico de tipo predictivo con algoritmos de machine-learning aplicando técnicas de clustering como por ejemplo el método Kmeans. Las predicciones podrían ir desde predecir que ciudad del mundo tendrá mas accesos a una hora determindada, hasta por ejemplo determinar a que hora del día se llegará a una número de acortamientos de un dominio determinado.
6. Implementar un script de instalacion de todos los componentes necesarios para la ejecucion de la aplicación.
7. Implementar un script de ejecucion con el orden correcto en el que se tienen que lanzar los jobs para poder visualizar los resultados.


## Aprendizaje

Durante el diseño e implementacion de este proyecto, se han adquirido diferentes habilidades:
- Diseñar aplicaciones Big Data de captura y procesado de logs, como lo son los clicks de 1usagov
- Implementar aplicaciones Big Data con el Framework Spark, Spark Streaming y Spark SQL
- Implementar aplicaciones con Elasticsearch y Spark
- Desarrollar visualizaciones en tiempo real con Kibana 

## Ubicación código fuente

El código fuente del proyecto se encuentra en el repositorio github [usagov-analytics](https://github.com/ilittleangel/usagov-analytics)

Esta dividido en 3 proyectos Maven 
![Screenshot](/screenshots/usagov-analytics-project-structure.png?raw=true)

- proyecto **`usagov-analytics-batch`** en la carpeta **`batch`**
- proyecto **`usagov-analytics-streaming`** en la carpeta **`streaming`**
- proyecto **`usagov-analytics-webapp`** en la carpeta **`webapp`**

Su estructura es la siguiente
![Screenshot](/screenshots/usagov-analytics-project-structure2.png?raw=true)
![Screenshot](/screenshots/usagov-analytics-project-structure3.png?raw=true)


## Análisis del código fuente

### Proyecto `usagov-analytics-batch`

Antes de empezar con el código quiero indicar las librerías incluidas en el ´pom.xml´:
```
    <properties>
        <scala.version>2.10.4</scala.version>
        <spark.version>1.2.1</spark.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.apache.spark</groupId>
            <artifactId>spark-core_2.10</artifactId>
            <version>${spark.version}</version>
        </dependency>
        <dependency>
            <groupId>org.scala-lang</groupId>
            <artifactId>scala-library</artifactId>
            <version>${scala.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.spark</groupId>
            <artifactId>spark-sql_2.10</artifactId>
            <version>${spark.version}</version>
        </dependency>
        <dependency>
            <groupId>org.elasticsearch</groupId>
            <artifactId>elasticsearch-spark_2.10</artifactId>
            <version>2.1.0</version>
        </dependency>
    </dependencies>
```


#### Job `UsagovBatchElasticsearch`

Este job se encarga de procesar los ficheros históricos almacenados en HDFS e indexar los resultados en ES.

Lo primero es la creacion del `SparkContext` donde hay que indicar la ubicación del servidor ES, ip y puerto. En este caso `localhost:9200`. A parte también he indicado que cree el index, en el caso de no encontrar donde indexar los documentos. Puesto que vamos a usar Spark SQL es necesario crear el `SQLContext`:
```scala
val sparkConf = new SparkConf()
    .setAppName("usagov-batch")
    .setMaster("local[2]")
    .setJars(List("/home/cloudera/Desktop/usagov-analytics-angelrojo/batch/target/usagov-analytics-batch-1.0.jar"))
    .setSparkHome("$SPARK_HOME")
    .set("es.nodes", "localhost")
    .set("es.port", "9200")
    .set("es.index.auto.create", "true")
    .set("es.field.read.empty.as.null", "false")

val sc = new SparkContext(sparkConf)
val sqlContext = new SQLContext(sc)
```

A continuación he registrado en `SQLContext` las funciones que voy a usar dentro de las sentencias `sql`. Estas funciones se encuentran en otro `object` de Scala llamado `UsagovUtils` dentro del mismo `package`:
```scala
sqlContext.registerFunction("getTimeFromEpoch", getTimeFromEpoch _)
sqlContext.registerFunction("getToday", getToday _)
```

Lectura de los ficheros históricos Measured Voice, alamcenados en HDFS:
```scala
val files = "hdfs://quickstart.cloudera:8020/user/cloudera/usagov/"
```

Filtrado de los eventos que no sean clicks de acortaminto de URLs:
```scala
val lines = sc.textFile(files)
    		  .filter(_.contains("\"h\":"))
              .map(_.replace(" ", ""))
```

Eliminar el campo `"ll"` del JSON correspondiente con un click, pues produce un error cuando no existe el campo:
```scala
lines.map(line => {
	var salida = line
	if (line.nonEmpty && line.contains("\"ll\":")) {
		val ll = line.split("\"ll\"")(0)
		salida = ll + "}"
	}
	salida.replace(",}", "}")
})
```

Ahora hay que crear un `SchemaRDD` mediante nuestro `SQLContext`. Para ello hacemos uso de la función `jsonRDD()` puesto que hemos convertido en RDDs, las lineas de los ficheros de HDFS, contenidas en `lines`. Estas lineas son JSONs, por lo que la función lo interpretará perfectamente:
```scala
val events = sqlContext.jsonRDD(lines)
```

Registramos una tabla temporal que usaremos para hacer las queries sobre nuestros RDDs:
```scala
events.registerTempTable("mytable")
```

Realizamos la query sobre cada RDD, En este caso hacemos uso de las funciones registradas en el `SQLContext`. Esta query devuelve un contador por hora de todos los clicks procesados. Obtenemos así la hora a la que se hacen mas acortamientos:
```scala
val query1 = sqlContext.sql(
    """
      |SELECT
      |   getToday('dia') as day,
      |   getTimeFromEpoch(t,'hora') as hour,
      |   count(1) as counter
      |FROM mytable
      |GROUP BY getToday('dia'), getTimeFromEpoch(t,'hora')
      |ORDER BY counter DESC
    """.stripMargin)
```

El campo day, simplemente se usa para ES y corresponde con el sysdate en formato ""yyyy-MM-dd"". sustituimos el campo devuelto `dia` por `@timestamp` para que ES lo indexe como `"type": "date"` y `"format": "dateOptionalTime"`:
```scala
 val query1RDD = query1.toJSON.map(_.replaceAll("\"day\":","\"@timestamp\":"))
```

Por último, se indexa el RDD en ES en el indice `usagov-batch` tipo `query1`
```scala
EsSpark.saveJsonToEs(query1RDD,"usagov-batch/query1")
```

Para el resto de queries seria igual.



### Proyecto `usagov-analytics-streaming`

En el `pom.xml` hay que añadir las librerías que se van a usar:
```
    <properties>
        <scala.version>2.10.4</scala.version>
        <spark.version>1.2.1</spark.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.apache.spark</groupId>
            <artifactId>spark-core_2.10</artifactId>
            <version>${spark.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.spark</groupId>
            <artifactId>spark-streaming_2.10</artifactId>
            <version>${spark.version}</version>
        </dependency>
        <dependency>
            <groupId>org.scala-lang</groupId>
            <artifactId>scala-library</artifactId>
            <version>${scala.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.spark</groupId>
            <artifactId>spark-sql_2.10</artifactId>
            <version>${spark.version}</version>
        </dependency>
        <dependency>
            <groupId>com.datastax.spark</groupId>
            <artifactId>spark-cassandra-connector_2.10</artifactId>
            <version>1.2.0-rc3</version>
        </dependency>
        <dependency>
            <groupId>org.elasticsearch</groupId>
            <artifactId>elasticsearch-spark_2.10</artifactId>
            <version>2.1.0</version>
        </dependency>
    </dependencies>
```


#### Clase `UsagovReceiver`

Primero comienzo explicando que la clase `UsagovReciever` es un custom reciever de Spark Streaming y necesaria para capturar los eventos del feed 1usagov. Para implementar un custom recierver la clase tiene que extender de Reciever: 
```scala
class UsagovReceiver extends Receiver[String](StorageLevel.MEMORY_AND_DISK_2) with Logging {
```

Contiene un método `onStart()` que inicia el hilo que recibe los datos:
```scala
def onStart() {
    new Thread("Usagov Receiver") {
      override def run() {
        receive()
      }
    }.start()
}
``` 

Contiene otro método `receive()` que crea un `BufferedReader` para recibir los datos de la URL donde se encuentra el feed `1usagov`. Recibe datos del BufferedReader hasta que es parado:
```scala
private def receive() {
	var userInput: String = null
    try {
      val reader = new BufferedReader(scala.io.Source.fromURL("http://developer.usa.gov/1usagov").bufferedReader())
      userInput = reader.readLine()
      while (!isStopped && userInput != null) {
        store(userInput)
        userInput = reader.readLine()
      }
      reader.close()
      logInfo("Stopped receiving")
      restart("Trying to connect again")
    } catch {
      case e: java.net.ConnectException =>
        restart("Error connecting to ", e)
      case t: Throwable =>
        restart("Error receiving data", t)
      case ex: FileNotFoundException => println("Couldn't find that file.")
    }
}
``` 


#### Job `UsagovStreamingElasticsearch`

Este job captura los eventos del feed `1usagov` y los indexa en ES.

Creamos el `SparkContext` con la configuración necesaria para poder conectar ES. Indicamos que el servidor está en `localhost:9200` y que el cree un indice si no existe. Ademas creamos nuestro `StreamingContext` con batches de 5 segundos:
```scala
val sparkConf = new SparkConf()
    .setAppName(getClass.getSimpleName)
    .setMaster("local[2]")
    .set("es.nodes", "localhost")
    .set("es.port", "9200")
    .set("es.index.auto.create", "true")
    .set("es.field.read.empty.as.null", "false")

val sc = new SparkContext(sparkConf)
val ssc = new StreamingContext(sc, Seconds(5))
```

Configuramos el timezone a GMT+0 que es con el que llegan los eventos. Así a la hora de hacer `new Date()` no nos lo hace con el timezone de nuestro entorno. Ademas almacenamos el formato con el que vamos a crear los `Date`:
```scala
TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
val format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
```

Creamos un `DStream` (Discretized Stream) llamado `usagovDSStream` instanciando la clase `UsagovReciever`. Este DStream contiene los RDDs que a su vez contendran los eventos o JSONs recuperados por el DStream. Por cada RDD hacemos una serie de transformaciones:
- reemplazar el nombre original de cada campo del JSON por un nombre descriptivo.
- reemplazar `_id` por `id` para que ES no lo intente indexar con ese `_id` y genere uno aleatorio.
- invertir las coordenadas del campo `location`
- parsear el campo `url` para obtener el campo `dominio`
- generar un campo `@timestamp` 
- construir el string `salida`  
```scala
val usagovDStream = ssc.receiverStream(new UsagovReceiver())
    .foreachRDD { rdd =>

     val documentsRDD = rdd.filter(_.nonEmpty)
       .map(_.replaceAll("\"_id\":", "\"id\":")) 
       .map(_.replaceAll("\"a\":", "\"user_agent\":"))
       .map(_.replaceAll("\"al\":", "\"accept_language\":"))
       .map(_.replaceAll("\"c\":", "\"country_code\":"))
       .map(_.replaceAll("\"cy\":", "\"geo_city_name\":"))
       .map(_.replaceAll("\"g\":", "\"global_bitly_hash\":"))
       .map(_.replaceAll("\"gr\":", "\"geo_region\":"))
       .map(_.replaceAll("\"h\":", "\"encoding_user_bitly_hash\":"))
       .map(_.replaceAll("\"hc\":", "\"time_hash_was_created\":"))
       .map(_.replaceAll("\"hh\":", "\"short_url_cname\":"))
       .map(_.replaceAll("\"l\":", "\"encoding_user_login\":"))
       .map(_.replaceAll("\"nk\":", "\"known_user\":"))
       .map(_.replaceAll("\"r\":", "\"referring_url\":"))
       .map(_.replaceAll("\"tz\":", "\"timezone\":"))
       .map(_.replaceAll("\"u\":", "\"url\":"))
       .map( linea => {
        // location
        val location = linea.split("\"ll\":")(1).split("}")(0) 
        val latitude = location.split(",")(0).replace("[","") 
        val longitude = location.split(",")(1).replace("]","") 
        val locationInv = "\"location\":[" + longitude + "," + latitude + "]" 
        // domain
        val url = linea.split("\"url\":")(1).split(",")(0)
        val dominio = "\"domain\":\"" + getDomain(url) + "\""
        // timestamp
        val ts = linea.split("\"t\":")(1).split(",")(0) 
        val date = format.format(new Date(ts.toLong * 1000L))  
        val timestamp = "\"@timestamp\":\"" + date + "\"" 
        // salida
        val sinllave = linea.substring(1)
        val salida = "{" + timestamp + "," + locationInv + "," + dominio + "," + sinllave 
        salida 
	})
```

Dentro del bucle `foreachRDD` indexamos los RDD en formato JSON en el índice `usagov-streaming` type `data`:
```scala
documentsRDD.collect.foreach(println)
EsSpark.saveJsonToEs(documentsRDD,"usagov-streaming/data")
```

Finalmente, indicar al `StreamingContext` que puede comenzar. Tambien indicaremos que se ejecute durante 60 minutos. Posteriormente le decimos que cuando le llegue la señal de `stop` que finalice de forma segura y sin perder eventos:
```scala
ssc.start()
ssc.awaitTermination(60 * 60 * 1000)
ssc.stop(true,true)
```


#### Job `UsagovSparkCassandraSetup`

Este job se encarga de crear en Cassandra el Keyspace y las column families. Por lo tanto este job tiene que ser ejecutado al lanzar la aplicación por primera vez.

Como en todos, configuramos el SparkContext donde indicamos la ubicación del servidor de Cassandra, en este caso `localhost` y por defecto el puerto `9000` que no es necesario especificar:
```scala
val sparkConf = new SparkConf()
    .setAppName(getClass.getSimpleName)
    .setMaster("local[2]")
    .set("spark.cassandra.connection.host", "localhost")
val sc = new SparkContext(sparkConf)
```

Abrimos una `session` en Cassandra:
```scala
CassandraConnector(sparkConf).withSessionDo { session =>
```

Y ejecutamos las sentencia `CQL` para crear el keyspace y las column families:
```scala
session.execute("DROP KEYSPACE IF EXISTS usagov")
session.execute("CREATE KEYSPACE usagov WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 }")
session.execute("DROP TABLE IF EXISTS usagov.topdomainseconds")
session.execute(
      """
        |CREATE TABLE usagov.topdomainseconds (
        | date VARCHAR,
        | domain VARCHAR,
        | time VARCHAR,
        | contador INT,
        |PRIMARY KEY(date, domain, time, contador))
      """.stripMargin)
```

Y así con todas las column families de nuestro modelo.


#### Object `UsagovClasses`

Este object de Scala contiene las `case class` que se necesitan para insertar los RDDs en Cassandra. Las clases tienen el mismo esquema que la column family de Cassandra, por lo tanto, hay una clase por cada column familie diferente. Las clases tiene que extender de `Serializable`:
```scala
case class TopDomain(date: String, domain: String, time: String, contador: Long) extends Serializable
```

Tambien contiene un metodo `apply(Row)` por cada ´case class´ con el que se hace referencia a cada campo que va ser insertado en Cassandra. Este método recibe un parámetro tipo `Row` que recupera de cada registro compuesto en el SchemaRDD, la información que contiene. 
```scala
object TopDomain {
    def apply(r: Row): TopDomain = TopDomain(
      r.getString(0), r.getString(1), r.getString(2), r.getLong(3))
}
```


#### Job `UsagovStreamingCassandra`

Este job se encarga de capturar los eventos del feed `1usagov` y almacenar los resultados de las agregaciones sobre dichos eventos en Cassandra.

Configuramos el timezone "UTC" con el que llegan los eventos:
```scala
TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
```

Configuración de Spark. Indicamos el host de Cassandra:
```scala
val sparkConf = new SparkConf()
    .setAppName(getClass.getSimpleName)
    .setMaster("local[2]")
    .set("spark.cassandra.connection.host", "localhost")
```

Creamos el `SparkContext`, el `StreamingContext` y el `SQLContext`:
```scala
val sc = new SparkContext(sparkConf)
val ssc = new StreamingContext(sc, Seconds(10))
val sqlContext = new SQLContext(sc)
```

Registro de las funciones que vamos a usar en el `SQLContext`:
```scala
sqlContext.registerFunction("getDomain", getDomain _)
sqlContext.registerFunction("getTimeFromEpoch", getTimeFromEpoch _)
```

Creamos el `DStream` a partir del custom reciever implementado en la clase `UsagovReciever`:
```scala
val usagovDStream = ssc.receiverStream(new UsagovReceiver())
```

Creamos ventanas de 10 y 60 segundos, que se deslizan cada 10 y 60 segundos respectivamente:
```scala
val windowDStream1s = usagovDStream.window(Seconds(10), Seconds(10))
val windowDStream60s = usagovDStream.window(Seconds(60), Seconds(60))
```

Por cada RDD de nuestra ventana temporal:
```scala
windowDStream1s.foreachRDD{ rdd =>
```

Creamos un `SchemaRDD` en nuestro `SQLContext` y registramos una tabla temporal para poder hacer queries SQL sobre dicha tabla:
```scala
sqlContext.jsonRDD(rdd).registerTempTable("mytable")
```

Alamcenamos el resultado de aplicar la query SQL sobre nuestro `SchemaRDD`, haciendo uso de las clases citadas anteriormente. En este ejemplo sobre `topdomain`. Y por último salvamos en Cassandra en el keyspace `usagov` y column family `topdomainseconds`:
```scala
sqlContext.sql(
        """
          |SELECT
          | getTimeFromEpoch(t,'dia'),
          | getDomain(u),
          | getTimeFromEpoch(t,'segundo'),
          | COUNT(1)
          |FROM mytable
          |WHERE
          | t is not null and
          | u is not null
          |GROUP BY getTimeFromEpoch(t,'dia'), getDomain(u), getTimeFromEpoch(t,'segundo')
        """.stripMargin)
        .map(TopDomain(_))
        .saveToCassandra("usagov", "topdomainseconds")
```
Lo mismo para cada column familie de nuestro modelo Cassandra.

Para almacenar todos los clicks, se ha optado por no usar SparkSQL, simplemente con el API de Spark se almacena el JSON entero mas un campo fecha `"yyyy-MM-dd"` que es nuestra `partition key`:
```scala
rdd.map(x =>
        (getTimeFromEpoch2(x.split(",")
        .find(_.contains("\"t\":"))
        .map(_.split(":")(1))
        .getOrElse()
        .toString,"dia"), x))
        .saveToCassandra("usagov", "clicks")
```

Finalmente cerramos el bucle `foreachRDD` de nuestro `windowDSStream` de 10 segundos.

Para el `DStream` de 60 segundos se hace los mismo.

Por último, indicar al `StreamingContext` que puede comenzar. En este caso indicamos que se ejecute durante 10 minutos:
```scala
ssc.start()
ssc.awaitTermination(10 * 60 * 1000) // 10 min
ssc.stop(true,true)
```



### Proyecto `usagov-analytics-webapp`

Este proyecto Maven es la aplicación web que muestra los datos en una pagina HTML. Contiene:
- el codigo HTML
- los Servlets de Java que recuperan los resultados de Cassandra y serializan los datos en JSON para enviarlo a la pagina web
- los javascript que llaman a los Servlets y renderizan los datos

El `pom.xml`:
```
   <dependencies>
        <dependency>
            <groupId>com.datastax.spark</groupId>
            <artifactId>spark-cassandra-connector_2.10</artifactId>
            <version>1.2.0-rc3</version>
        </dependency>
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>javax.servlet-api</artifactId>
            <version>3.1.0</version>
        </dependency>
    </dependencies>
```

Explicaré uno de los Servlets ya que todos hacen casi lo mismo con diferentes datos.

#### Clase `CServletTopCountry` 

Este Servlet utiliza el driver `cassandra-driver-core` para Java de la empresa [Datastax](http://docs.datastax.com/en/developer/java-driver/2.0/java-driver/whatsNew2.html). 

La clase extiende de `HttpServlet`:
```java
public class CServletTopCountry extends HttpServlet
```

Contiene el método `doGet` que se ejecuta cuando le llega una peticion `request`:
```java
protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException 
```

Dentro del método `doGet` se abre una `session` con Cassandra haciendo uso del driver. Establecemos la conexion con el keyspace `usagov`:
```java
Cluster cluster;
Session session;
cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
session = cluster.connect("usagov");
```

Preparamos la query `CQL` que se va lanzar sobre nuestra session, haciendo uso de la clase `BoundStatement` que nos permite usar parametros dinamicos dentro de la sentencia `CQL`. En este caso se usa para filtrar por el dia actual:
```java
PreparedStatement statement = session.prepare(
                "SELECT country_code, contador " +
                "FROM topcountryminutes " +
                "WHERE date = ?  ");
BoundStatement boundStatement = new BoundStatement(statement);
ResultSet results = session.execute(boundStatement.bind(today));
```

Hacemos uso de un `HashMap` para agregar por país y sumar sus contadores:
```java
HashMap<String, ArrayList<Integer>> map = new HashMap<String, ArrayList<Integer>>();
```

Recorrer el `HashMap` y parsear el resultado a JSON:
```java
StringBuffer jsonStr = new StringBuffer();
jsonStr.append("{\r\n");
jsonStr.append("  \"name_query\": \"topcountry\",\r\n");
jsonStr.append("  \"description\": \"Top urls acortadas por pais\",\r\n");
jsonStr.append("  \"results\":\r\n");
jsonStr.append("  [\r\n");
for (Entry<String, ArrayList<Integer>> ee : map.entrySet()) {
	String key = ee.getKey();
    ArrayList<Integer> values = ee.getValue();
    jsonStr.append("    {\r\n");
    jsonStr.append("      \"country\": \"" + Countries.getCountry(key, lang) + "\",\r\n");
    // suma de los contadores
    int sum = 0;
    for (int i : values) {
    	sum += i;
    }
    jsonStr.append("      \"contador\": \"" + sum + "\"\r\n");
    jsonStr.append("    },\r\n");
}
jsonStr.append("  ]\r\n");
jsonStr.append("}\r\n");
jsonStr.deleteCharAt(jsonStr.length()-11);
```

Se envía en el `http response` el JSON construido:
```java
response.setContentType("aplication/json");
response.setCharacterEncoding("UTF-8");
response.getWriter().write(jsonStr.toString());
response.getWriter().flush();
response.getWriter().close();
```


## Conclusiones

Existen cada día mas tecnologías que ofrecen soluciones Big Data. Tecnologías que se centran en resolver algún o algunos de los problemas que trae procesar grandes volumetrías de datos y ademas hacerlo en el instante de creación. En este proyecto solo se han utilizado una ínfima parte de ellas, aunque algunas de las mas importantes en este momento, como es Spark. 

Una de las conclusiones que obtengo de haber realizado el proyecto es la velocidad con la que cambian estas tecnologías. En pocos meses, la tecnología con la que te sentías cómodo para desarrollar diferentes soluciones, ha cambiado su paradigma o directamente se ha cerrado su proyecto en la Apache Software Fundation, por lo que su desarrollo se puede ver muy comprometido. Por lo tanto la conclusión es que en este mundo hay estar activo para no quedarte desactualizado tecnológicamente.

Otra conclusión que se puede sacar es sobre la gran posibilidad que nos brindan estas tecnologías. Con el crecimiento exponencial de creación de datos en el mundo, existen millones de posibilidades de análisis y servicios que se pueden implementar haciendo uso del Big Data.

