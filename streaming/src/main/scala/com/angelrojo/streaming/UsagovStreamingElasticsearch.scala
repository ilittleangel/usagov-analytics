package com.angelrojo.streaming


import java.util.Date
import java.util.TimeZone
import java.text.SimpleDateFormat
import java.util.Locale

import com.angelrojo.streaming.UsagovUtils._
import org.apache.spark._
import org.apache.spark.streaming._
import org.elasticsearch.spark.rdd.EsSpark


/**
 * Antes de lanzar, hay que crear el index "usagov-streaming" 
 * y mapear el campo "location" como "geo_point"
 *
      PUT /usagov-streaming
      {
          "mappings" : {
            "data": {
              "properties": {
                "location": { "type": "geo_point", "lat_lon": true, "geohash": true },
                "country": { "type": "string", "index": "not_analyzed" },
                "timezone": { "type": "string", "index": "not_analyzed" },
                "geo_city_name": { "type": "string", "index": "not_analyzed" },
                "global_bitly_hash": { "type": "string", "index": "no" },
                "geo_region": { "type": "string", "index": "not_analyzed" },
                "encoding_user_bitly_hash": { "type": "string", "index": "no" },
                "time_hash_was_created": { "type": "string", "index": "no" },
                "encoding_user_login": { "type": "string", "index": "no" }
              }
            }
          }
      }
 * */


object UsagovStreamingElasticsearch extends App {

  /* Set default timezone GMT+0 */
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  private val format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
  private val lang: Locale = Locale.ENGLISH

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
        // country
        val country_code = linea.split("\"country_code\":")(1).split(",")(0).replace("\"","")
        val country = "\"country\":\"" + getCountry(country_code, lang) + "\""
        // salida
        val sinllave = linea.substring(1) 
        val salida = "{" + timestamp + "," + locationInv + "," + dominio + "," + country + "," + sinllave
        salida 
      })


    /* Index the RDD explicitly through EsSpark */
    documentsRDD.collect.foreach(println)
    EsSpark.saveJsonToEs(documentsRDD,"usagov-streaming/data")


  }

  ssc.start()
  ssc.awaitTermination(60 * 60 * 1000) // 60 min
  ssc.stop(true,true)


}

