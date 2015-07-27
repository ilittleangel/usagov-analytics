package com.angelrojo.streaming

import java.io.{BufferedReader, FileNotFoundException}

import org.apache.spark.Logging
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.receiver.Receiver


class UsagovReceiver extends Receiver[String](StorageLevel.MEMORY_AND_DISK_2) with Logging {

  def onStart() {
    // Start the thread that receives data over a connection
    new Thread("Usagov Receiver") {
      override def run() {
        receive()
      }
    }.start()
  }

  def onStop() {
    // There is nothing much to do as the thread calling receive()
    // is designed to stop by itself isStopped() returns false
  }

  /** Create a BufferedReader and receive data until receiver is stopped */
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
}
