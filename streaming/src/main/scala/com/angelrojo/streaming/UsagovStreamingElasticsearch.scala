package com.angelrojo.streaming


import java.util.Date
import java.util.TimeZone
import java.text.SimpleDateFormat

import com.angelrojo.streaming.UsagovUtils._
import org.apache.spark._
import org.apache.spark.streaming._
import org.elasticsearch.spark.rdd.EsSpark


/**
 * It must be added a "@timestamp" field to each line of the RDD,
 * so when indexing ES is recognized as a date and then Kibana can
 * configure an index time-based pattern.
 *
 * In addition, we must be added a "location" field with inverted
 * coordinates for Kibana geohash function can localize on the map each
 * one of events.
 *
 * Therefore, before start with indexing in ES, we must create an index
 * with a schema with mappings of "location" field with an specifics types
 *
 * PUT /usagov-streaming
 * {
 *    "mappings" : {
 *        "data": {
 *            "properties": {
 *                "location": {
 *                    "type": "geo_point",
 *                    "lat_lon": true,
 *                    "geohash": true
 *                }
 *            }
 *        }
 *    }
 * }
 *
 * After launch the Spark job, we can check the mapping of ES has done
 * to each field of our JSON, with the following instr:
 *
 * GET usagov-streaming/_mapping
 *
 * */


object UsagovStreamingElasticsearch extends App {

  /* Set default timezone GMT+0 */
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  val format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

  /* Spark */
  val sparkConf = new SparkConf()
    .setAppName(getClass.getSimpleName)
    .setMaster("local[2]")
    .set("es.nodes", "localhost")
    .set("es.port", "9200")
    .set("es.index.auto.create", "true")
    .set("es.field.read.empty.as.null", "false")

  val sc = new SparkContext(sparkConf)
  val ssc = new StreamingContext(sc, Seconds(5))

  /* custom reciever */
  val usagovDStream = ssc.receiverStream(new UsagovReceiver())
    .foreachRDD { rdd =>

     val documentsRDD = rdd.filter(_.nonEmpty)
       .map(_.replaceAll("\"_id\":", "\"id\":")) // cambio "_id" por "id" para que a la hora de indexar, ES no se vuelva loco
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
        val location = linea.split("\"ll\":")(1).split("}")(0) // me quedo con [LATITUDE, LONGITUDE] del campo "ll"
        val latitude = location.split(",")(0).replace("[","") // me quedo con LATITUDE
        val longitude = location.split(",")(1).replace("]","") // me quedo con LONGITUDE
        val locationInv = "\"location\":[" + longitude + "," + latitude + "]" // string con el campo "location" con lat long invertidas
        // domain
        val url = linea.split("\"url\":")(1).split(",")(0)
        val dominio = "\"domain\":\"" + getDomain(url) + "\""
        // timestamp
        val ts = linea.split("\"t\":")(1).split(",")(0) // me quedo con el valor del campo "t"
        val date = format.format(new Date(ts.toLong * 1000L))  // lo convierto en un Date
        val timestamp = "\"@timestamp\":\"" + date + "\"" // string con el timestamp calculado
        // salida
        val sinllave = linea.substring(1) // elimino la llave inicial del json de entrada
        val salida = "{" + timestamp + "," + locationInv + "," + dominio + "," + sinllave // string de salida concatenado con el json completo
        salida // devuelvo el contenido de la variable salida
      })


    /* Index the RDD explicitly through EsSpark */
    documentsRDD.collect.foreach(println)
    EsSpark.saveJsonToEs(documentsRDD,"usagov-streaming/data")


  }

  ssc.start()
  ssc.awaitTermination(60 * 60 * 1000) // 60 min
  ssc.stop(true,true)


}

