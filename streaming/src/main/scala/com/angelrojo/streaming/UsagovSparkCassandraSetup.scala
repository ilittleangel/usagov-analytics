package com.angelrojo.streaming

import com.datastax.spark.connector.cql.CassandraConnector
import org.apache.spark._

object UsagovSparkCassandraSetup extends App {

  /* Spark inicialization */
  val sparkConf = new SparkConf()
    .setAppName(getClass.getSimpleName)
    .setMaster("local[2]")
    .set("spark.cassandra.connection.host", "localhost")
  val sc = new SparkContext(sparkConf)

  /* Cassandra setup */
  CassandraConnector(sparkConf).withSessionDo { session =>

    session.execute("DROP KEYSPACE IF EXISTS usagov")
    session.execute("CREATE KEYSPACE usagov WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 }")
    println("CREATE KEYSPACE usagov  -->  OK")

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
    println("CREATE TABLE usagov.topdomainseconds  -->  OK")

    session.execute("DROP TABLE IF EXISTS usagov.topdomainminutes")
    session.execute(
      """
        |CREATE TABLE usagov.topdomainminutes (
        | date VARCHAR,
        | domain VARCHAR,
        | time VARCHAR,
        | contador INT,
        |PRIMARY KEY(date, domain, time, contador))
      """.stripMargin)
    println("CREATE TABLE usagov.topdomainminutes  -->  OK")

    session.execute("DROP TABLE IF EXISTS usagov.topdomainhours")
    session.execute(
      """
        |CREATE TABLE usagov.topdomainhours (
        | date VARCHAR,
        | time VARCHAR,
        | url VARCHAR,
        | contador INT,
        |PRIMARY KEY(date, time, url, contador))
      """.stripMargin)
    println("CREATE TABLE usagov.topdomainhours  -->  OK")

    session.execute("DROP TABLE IF EXISTS usagov.topdomainbycountryseconds")
    session.execute(
      """
        |CREATE TABLE usagov.topdomainbycountryseconds (
        | date VARCHAR,
        | country_code VARCHAR,
        | domain VARCHAR,
        | time VARCHAR,
        | contador INT,
        |PRIMARY KEY(date, country_code, domain, time, contador))
      """.stripMargin)
    println("CREATE TABLE usagov.topdomainbycountryseconds  -->  OK")

    session.execute("DROP TABLE IF EXISTS usagov.topdomainbycountryminutes")
    session.execute(
      """
        |CREATE TABLE usagov.topdomainbycountryminutes (
        | date VARCHAR,
        | country_code VARCHAR,
        | domain VARCHAR,
        | time VARCHAR,
        | contador INT,
        |PRIMARY KEY(date, country_code, domain, time, contador))
      """.stripMargin)
    println("CREATE TABLE usagov.topdomainbycountryminutes  -->  OK")

    session.execute("DROP TABLE IF EXISTS usagov.topdomainbycountryhours")
    session.execute(
      """
        |CREATE TABLE usagov.topdomainbycountryhours (
        | date VARCHAR,
        | country_code VARCHAR,
        | domain VARCHAR,
        | time VARCHAR,
        | contador INT,
        |PRIMARY KEY(date, country_code, domain, time, contador))
      """.stripMargin)
    println("CREATE TABLE usagov.topdomainbycountryhours  -->  OK")

    session.execute("DROP TABLE IF EXISTS usagov.fullevent")
    session.execute(
      """
        |CREATE TABLE usagov.fullevent (
        |   date VARCHAR,
        |   id VARCHAR,
        |   user_agent VARCHAR,
        |   accept_language VARCHAR,
        |   country_code VARCHAR,
        |   geo_city_name VARCHAR,
        |   global_bitly_hash VARCHAR,
        |   geo_region VARCHAR,
        |   encoding_user_bitly_hash VARCHAR,
        |   time_hash_was_created INT,
        |   short_url_cname VARCHAR,
        |   encoding_user_login VARCHAR,
        |   known_user INT,
        |   referring_url VARCHAR,
        |   timestamp INT,
        |   timezone VARCHAR,
        |   long_url VARCHAR,
        |PRIMARY KEY (date, id))
      """.stripMargin)
    println("CREATE TABLE usagov.fullevent  -->  OK")

    session.execute("DROP TABLE IF EXISTS usagov.topcountryseconds")
    session.execute(
      """
        |CREATE TABLE usagov.topcountryseconds (
        |   date VARCHAR,
        |   country_code VARCHAR,
        |   time VARCHAR,
        |   contador INT,
        |PRIMARY KEY (date, country_code, time, contador))
      """.stripMargin)
    println("CREATE TABLE usagov.topcountryseconds  -->  OK")

    session.execute("DROP TABLE IF EXISTS usagov.topcountryminutes")
    session.execute(
      """
        |CREATE TABLE usagov.topcountryminutes (
        |   date VARCHAR,
        |   country_code VARCHAR,
        |   time VARCHAR,
        |   contador INT,
        |PRIMARY KEY (date, country_code, time, contador))
      """.stripMargin)
    println("CREATE TABLE usagov.topcountryminutes  -->  OK")

    session.execute("DROP TABLE IF EXISTS usagov.topcountryhours")
    session.execute(
      """
        |CREATE TABLE usagov.topcountryhours (
        |   date VARCHAR,
        |   country_code VARCHAR,
        |   time VARCHAR,
        |   contador INT,
        |PRIMARY KEY (date, country_code, time, contador))
      """.stripMargin)
    println("CREATE TABLE usagov.topcountryhours  -->  OK")

    session.execute("DROP TABLE IF EXISTS usagov.clicks2")
    session.execute(
      """
        |CREATE TABLE usagov.clicks2 (
        |   date VARCHAR,
        |   id VARCHAR,
        |PRIMARY KEY (date, id))
      """.stripMargin)
    println("CREATE TABLE usagov.clicks2  -->  OK")

    session.execute("DROP TABLE IF EXISTS usagov.clicks")
    session.execute(
      """
        |CREATE TABLE usagov.clicks (
        |   date VARCHAR,
        |   event VARCHAR,
        |PRIMARY KEY (date, event))
      """.stripMargin)
    println("CREATE TABLE usagov.clicks  -->  OK")

  }
  println("CASSANDRA SETUP SUCCEEDED")
}