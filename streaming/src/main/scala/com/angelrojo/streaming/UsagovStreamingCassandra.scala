package com.angelrojo.streaming


import com.angelrojo.streaming.UsagovClasses._
import com.angelrojo.streaming.UsagovUtils._
import java.util.TimeZone
import com.datastax.spark.connector._
import org.apache.spark._
import org.apache.spark.sql.SQLContext
import org.apache.spark.streaming._


/**
 * Before the first execution we must to run "UsagovSparkCassandraSetup" job
 * to create in Cassandra the "usagov" keyspace and the specific column families
 * */


object UsagovStreamingCassandra extends App {

  /* Set the default timezone as GMT+0, because the timestamps we receive coming with GMT+0 */
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

  /* Spark configuration with cassandra conection cluster property*/
  val sparkConf = new SparkConf()
    .setAppName(getClass.getSimpleName)
    .setMaster("local[2]")
    .set("spark.cassandra.connection.host", "localhost")
    //.set("spark.cassandra.input.metrics", "true")

  /* Spark context and streaming context inicialization */
  val sc = new SparkContext(sparkConf)
  val ssc = new StreamingContext(sc, Seconds(10))
  val sqlContext = new SQLContext(sc)

  /* registration of functions in the SQLcontext */
  sqlContext.registerFunction("getDomain", getDomain _)
  sqlContext.registerFunction("getTimeFromEpoch", getTimeFromEpoch _)


  /* create 2 streams by a custon reciever with 1 and 60 seconds of window */
  val usagovDStream = ssc.receiverStream(new UsagovReceiver())
  val windowDStream1s = usagovDStream.window(Seconds(10), Seconds(10))
  val windowDStream60s = usagovDStream.window(Seconds(60), Seconds(60))

  //val lines1s = windowDStream1s.flatMap(_.split("\\n"))
  //val lines60s = windowDStream60s.flatMap(_.split("\\n"))

  windowDStream1s.foreachRDD{ rdd =>

    //sqlContext.jsonRDD(rdd).printSchema()

    /* this check is here to handle the empty collection error */
    if (rdd.toLocalIterator.nonEmpty) {
      sqlContext.jsonRDD(rdd).registerTempTable("mytable")

      /* store all events */
      sqlContext.sql(
        """
          |SELECT
          | getTimeFromEpoch(t,'dia'), _id
          |FROM mytable
          |WHERE t is not null
        """.stripMargin)
        .map(Clicks(_))
        .saveToCassandra("usagov", "clicks2")

      /* clicks on domains per seconds */
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
        .map(TopDomain(_))//.foreach(println)
        .saveToCassandra("usagov", "topdomainseconds")

      /* clicks on domains per seconds by country */
      sqlContext.sql(
        """
          |SELECT
          | getTimeFromEpoch(t,'dia'),
          | c,
          | getDomain(u),
          | getTimeFromEpoch(t,'segundo'),
          | COUNT(1)
          |FROM mytable
          |WHERE
          | t is not null and
          | c is not null and
          | u is not null
          |GROUP BY getTimeFromEpoch(t,'dia'), c, getDomain(u), getTimeFromEpoch(t,'segundo')
        """.stripMargin)
        .map(TopDomainByCountry(_))
        .saveToCassandra("usagov", "topdomainbycountryseconds")

      /* clicks per seconds by country */
      sqlContext.sql(
        """
          |SELECT
          | getTimeFromEpoch(t,'dia'),
          | c,
          | getTimeFromEpoch(t,'segundo'),
          | COUNT(1)
          |FROM mytable
          |WHERE
          | t is not null and
          | c is not null
          |GROUP BY getTimeFromEpoch(t,'dia'), c, getTimeFromEpoch(t,'segundo')
        """.stripMargin)
        .map(TopCountry(_))
        .saveToCassandra("usagov", "topcountryseconds")


      /* Save everything in Cassandra */
      rdd.map(x =>
        (getTimeFromEpoch2(x.split(",")
        .find(_.contains("\"t\":"))
        .map(_.split(":")(1))
        .getOrElse()
        .toString,"dia"), x))
        .saveToCassandra("usagov", "clicks")

    }
  }

  windowDStream60s.foreachRDD{ rdd =>

    println("********************************* INICIO foreachRDD 60 sg ******************************")

    if (rdd.toLocalIterator.nonEmpty) {
      sqlContext.jsonRDD(rdd).registerTempTable("mytable")

      /* clicks on domains per minute */
      sqlContext.sql(
        """
          |SELECT
          | getTimeFromEpoch(t,'dia'),
          | getDomain(u),
          | getTimeFromEpoch(t,'minuto'),
          | COUNT(1)
          |FROM mytable
          |WHERE
          | t is not null and
          | u is not null
          |GROUP BY getTimeFromEpoch(t,'dia'), getDomain(u), getTimeFromEpoch(t,'minuto')
        """.stripMargin)
        .map(TopDomain(_))
        .saveToCassandra("usagov", "topdomainminutes")

      /* clicks on domains per minute by country */
      sqlContext.sql(
        """
          |SELECT
          | getTimeFromEpoch(t,'dia'),
          | c,
          | getDomain(u),
          | getTimeFromEpoch(t,'minuto'),
          | COUNT(1)
          |FROM mytable
          |WHERE
          | t is not null and
          | c is not null and
          | u is not null
          |GROUP BY getTimeFromEpoch(t,'dia'), c, getDomain(u), getTimeFromEpoch(t,'minuto')
        """.stripMargin)
        .map(TopDomainByCountry(_))
        .saveToCassandra("usagov", "topdomainbycountryminutes")

      /* clicks per minute by country */
      sqlContext.sql(
        """
          |SELECT
          | getTimeFromEpoch(t,'dia'),
          | c,
          | getTimeFromEpoch(t,'minuto'),
          | COUNT(1)
          |FROM mytable
          |WHERE
          | t is not null and
          | c is not null
          |GROUP BY getTimeFromEpoch(t,'dia'), c, getTimeFromEpoch(t,'minuto')
        """.stripMargin)
        .map(TopCountry(_))
        .saveToCassandra("usagov", "topcountryminutes")

    }
    println("********************************* FIN foreachRDD 60 sg ******************************")
  }


  ssc.start()
  ssc.awaitTermination(20 * 60 * 1000) // 10 min
  ssc.stop(true,true)

  /* handle the SIGTERM signal that is sent to the driver process when you kill it */
  //sys.ShutdownHookThread { ssc.stop(true, true) }

}

