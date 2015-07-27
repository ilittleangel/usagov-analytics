package com.angelrojo.streaming

import org.apache.spark.sql.Row


object UsagovClasses {

  /* clases para almacenar el total de url's acortadas */
  case class TopDomain(date: String, domain: String, time: String, contador: Long) extends Serializable

  case class TopDomainByCountry(date: String, country_code: String, domain: String, time: String, contador: Long) extends Serializable

  case class TopCountry(date: String, country_code: String, time: String, contador: Long) extends Serializable

  case class Clicks(date: String, id: String) extends Serializable

  case class FullEvent(date: String,                              // este campo no pertenece al json original
                       id: String,                                // _id
                       user_agent: String,                        // a
                       accept_language: String,                   // al
                       country_code: String,                      // c
                       geo_city_name: String,                     // cy
                       global_bitly_hash: String,                 // g
                       geo_region: String,                        // gr
                       encoding_user_bitly_hash: String,          // h
                       time_hash_was_created: Int,                // hc
                       short_url_cname: String,                   // hh
                       encoding_user_login: String,               // l
                       known_user: Int,                           // nk
                       referring_url: String,                     // r
                       timestamp: Int,                            // t
                       timezone: String,                          // tz
                       long_url: String) extends Serializable     // u



  object TopDomain {
    def apply(r: Row): TopDomain = TopDomain(
      r.getString(0), r.getString(1), r.getString(2), r.getLong(3))
  }

  object TopDomainByCountry {
    def apply(r: Row): TopDomainByCountry = TopDomainByCountry(
      r.getString(0), r.getString(1), r.getString(2), r.getString(3), r.getLong(4))
  }

  object TopCountry {
    def apply(r: Row): TopCountry = TopCountry(
      r.getString(0), r.getString(1), r.getString(2), r.getLong(3))
  }

  object Clicks {
    def apply(r: Row): Clicks = Clicks(
      r.getString(0), r.getString(1))
  }

  object FullEvent {
    def apply(r: Row): FullEvent = FullEvent(
      r.getString(0), r.getString(1), r.getString(2), r.getString(3), r.getString(4), r.getString(5),
      r.getString(6), r.getString(7), r.getString(8), r.getInt(9), r.getString(10), //getArray
      r.getString(11), r.getInt(12), r.getString(13), r.getInt(14), r.getString(15), r.getString(16))
  }

}

/**
  registro de ejemplo (16 campos + "nk" que no siempre llega)
 {
     "h":"1KUFdDW",
     "g":"1KUFdDY",
     "l":"ifttt",
     "hh":"ift.tt",
     "u":"http://www.ncbi.nlm.nih.gov/pubmed/26056770?dopt=Abstract",
     "r":"http://t.co/Ffxx1HdiQp",
     "a":"Mozilla/5.0 (iPad; CPU OS 8_3 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Mobile/12F69 Twitter for iPhone",
     "t":1433933949,
     "hc":1433930577,
     "_id":"cf0995a4-7edb-694c-8dcf-47a620056cd3",
     "al":"en-gb",
     "c":"SE",
     "tz":"Europe/Stockholm",
     "gr":"28",
     "cy":"GÃ¶teborg",
     "ll":[57.7072,11.9668]
  }

  Metadatos (16 campos)
 {
        "a": USER_AGENT,
        "c": COUNTRY_CODE, # 2-character iso code
        "nk": KNOWN_USER,  # 1 or 0. 0=this is the first time we've seen this browser 
        "g": GLOBAL_BITLY_HASH,
        "h": ENCODING_USER_BITLY_HASH,
        "l": ENCODING_USER_LOGIN,
        "hh": SHORT_URL_CNAME,
        "r": REFERRING_URL,
        "u": LONG_URL,
        "t": TIMESTAMP,
        "gr": GEO_REGION,
        "ll": [LATITUDE, LONGITUDE],
        "cy": GEO_CITY_NAME,
        "tz": TIMEZONE # in http://en.wikipedia.org/wiki/Zoneinfo format
        "hc": TIMESTAMP OF TIME HASH WAS CREATED,
        "al": ACCEPT_LANGUAGE http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.4
    }

  Schema JsonRDD
 |-- _corrupt_record: string (nullable = true)
 |-- _id: string (nullable = true)
 |-- a: string (nullable = true)
 |-- al: string (nullable = true)
 |-- c: string (nullable = true)
 |-- cy: string (nullable = true)
 |-- g: string (nullable = true)
 |-- gr: string (nullable = true)
 |-- h: string (nullable = true)
 |-- hc: integer (nullable = true)
 |-- hh: string (nullable = true)
 |-- i: string (nullable = true)
 |-- k: string (nullable = true)
 |-- l: string (nullable = true)
 |-- ll: array (nullable = true)
 |    |-- element: double (containsNull = false)
 |-- mc: integer (nullable = true)
 |-- nk: integer (nullable = true)
 |-- r: string (nullable = true)
 |-- t: integer (nullable = true)
 |-- tz: string (nullable = true)
 |-- u: string (nullable = true)

  */
