package com.angelrojo.streaming

import com.angelrojo.streaming.UsagovUtils._
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.sql.SQLContext
import org.elasticsearch.spark.rdd.EsSpark

/**
 * En ES se va a indexar en distintos _types dentro del mismo index "usagov-batch"
 * Cada query que deseemos indexar en ES, correspondiente con el analisis de patrones,
 * se indexará bajo types distintos.
 *
 * Antes de lanzar este Spark job, es necesario crear el indice con el siguiente mapeo:
 *
      PUT /usagov-batch
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
      }
 * */


object UsagovBatchElasticsearch extends App {

  /* Spark configuration */
  val sparkConf = new SparkConf()
    .setAppName("usagov-batch")
    .setMaster("local[2]")
    .setJars(List("/Users/angelrojoperez/Desktop/usagov-analytics/batch/target/usagov-analytics-batch-1.0.jar"))
    .setSparkHome("$SPARK_HOME")
    .set("es.nodes", "localhost")
    .set("es.port", "9200")
    .set("es.index.auto.create", "true")
    .set("es.field.read.empty.as.null", "false")

  val sc = new SparkContext(sparkConf)
  val sqlContext = new SQLContext(sc)

  /* registration functions into sqlContext */
  sqlContext.registerFunction("getTimeFromEpoch", getTimeFromEpoch _)
  sqlContext.registerFunction("getToday", getToday _)

  /* path to data archive files */
  val files = "/Users/angelrojoperez/Desktop/datos-measured-voice/"

  /* read lines Data Archives usagov Measured Voice */
  val lines = sc.textFile(files)
    .filter(_.contains("\"h\":"))
    .map(_.replace(" ", ""))

  /* remove the "ll" field of each json as it fails when it does not come informed */
  lines.map(line => {
    var salida = line
    if (line.nonEmpty && line.contains("\"ll\":")) {
      val ll = line.split("\"ll\"")(0)
      salida = ll + "}"
    }
    salida.replace(",}", "}")
  }
  )

  /* SchemaRDD for the JSON dataset */
  val events = sqlContext.jsonRDD(lines)

  /* total events processed */
  //println("NUMERO DE EVENTOS: " + lines.count())

  /* registration temp table into sqlContext */
  events.registerTempTable("mytable")


  /* ************** query1 ******************* */
  println("query1: ¿A qué hora se acortan más URLs?")
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

  /* replacement timestamp to ES @timestamp */
  val query1RDD = query1.toJSON.map(_.replaceAll("\"day\":","\"@timestamp\":"))

  /* indexing into ES with "usagov-batch" index */
  query1RDD.collect().foreach(println)
  EsSpark.saveJsonToEs(query1RDD,"usagov-batch/query1")


  /* ************** query2 ******************* */
  println("query2: ¿Varía según la zona horaria?")
  val query2 = sqlContext.sql(
    """
      |SELECT
      |   getToday('dia') as day,
      |   tz as timezone,
      |   getTimeFromEpoch(t,'hora') as hour ,
      |   count(1) as counter
      |FROM mytable
      |GROUP BY getToday('dia'),tz, getTimeFromEpoch(t,'hora')
      |ORDER BY tz ASC, counter DESC
    """.stripMargin)

  val query2RDD = query2.toJSON.map(_.replaceAll("\"day\":","\"@timestamp\":"))
  query2RDD.collect().foreach(println)
  EsSpark.saveJsonToEs(query2RDD,"usagov-batch/query2")


  /* ************** query3 ******************* */
  println("query3: ¿Y el fin de semana?")
  val query3 = sqlContext.sql(
    """
      |SELECT
      |   getTimeFromEpoch(t,'dia') as day,
      |   getTimeFromEpoch(t,'hora') as hour,
      |   count(1) as counter
      |FROM mytable
      |GROUP BY getTimeFromEpoch(t,'dia'), getTimeFromEpoch(t,'hora')
      |ORDER BY counter DESC
    """.stripMargin)//.toJSON.take(20).foreach(println)

  val query3RDD = query3.toJSON.map(_.replaceAll("\"day\":","\"@timestamp\":"))
  query3RDD.take(20).foreach(println)
  EsSpark.saveJsonToEs(query3RDD,"usagov-batch/query3")


}
