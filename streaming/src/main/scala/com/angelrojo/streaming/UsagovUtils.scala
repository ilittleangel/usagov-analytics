package com.angelrojo.streaming

import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Calendar


object UsagovUtils {

  /* Funcion que recibe una URL y devuelve el dominio */
  def getDomain(url: String): String = {
    var domain = ""
    if (url.length.equals(0) || url.equals(null) ) {
      domain = "N/A"
    } else {
      domain = url.split("/")(2)
    }
    domain
  }

  /* Función que recibe un epoch y una unida de tiempo y devuelve la unidad de tiempo del epoch */
  def getTimeFromEpoch(t: Int, unidad: String): String = {
    unidad match {
      case "dia" => {
        val date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(t * 1000L))
        date.toString
      }
      case "hora" => {
        val date = new SimpleDateFormat("HH").format(new Date(t * 1000L))
        date.toString
      }
      case "minuto" => {
        val date = new SimpleDateFormat("HH:mm").format(new Date(t * 1000L))
        date.toString
      }
      case "segundo" => {
        val date = new SimpleDateFormat("HH:mm:ss").format(new Date(t * 1000L))
        date.toString
      }
      case _ => "1970-01-01"
    }
  }

  /* Función que recibe un epoch y una unida de tiempo y devuelve la unidad de tiempo del epoch */
  def getTimeFromEpoch2(t: String, unidad: String): String = {
    if (t.length.equals(0) || t.equals(null) || t.equals("()")) {
      "N/A"
    } else {
      unidad match {
        case "dia" => {
          val date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(t.toInt * 1000L))
          date.toString
        }
        case "hora" => {
          val date = new SimpleDateFormat("HH").format(new Date(t.toInt * 1000L))
          date.toString
        }
        case "minuto" => {
          val date = new SimpleDateFormat("HH:mm").format(new Date(t.toInt * 1000L))
          date.toString
        }
        case "segundo" => {
          val date = new SimpleDateFormat("HH:mm:ss").format(new Date(t.toInt * 1000L))
          date.toString
        }
        case _ => "1970-01-01"
      }
    }
  }

  /* Función que recibe una unidad de tiempo y devuelve sysdate en esa unidad */
  def getToday(unidad: String): String = {
    unidad match {
      case "horas" => {
        val date = new SimpleDateFormat("yyyy-MM-dd HH").format(Calendar.getInstance().getTime())
        date.toString
      }
      case "minutos" => {
        val date = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(Calendar.getInstance().getTime())
        date.toString
      }
      case "segundos" => {
        val date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())
        date.toString
      }
      case _ => "1970-01-01 00:00:00"
    }
  }

  /* Función que recibe una unidad de tiempo y devuelve sysdate-3 minutos en esa unidad */
  def getToday2 (unidad: String): String = {
    val cal = Calendar.getInstance()
    cal.setTime(Calendar.getInstance().getTime())
    cal.add(Calendar.MINUTE, -3)
    val threeMinutesBack = cal.getTime()
    unidad match {
      case "horas" => {
        val date = new SimpleDateFormat("yyyy-MM-dd HH").format(threeMinutesBack)
        date.toString
      }
      case "minutos" => {
        val date = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(threeMinutesBack)
        date.toString
      }
      case "segundos" => {
        val date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(threeMinutesBack)
        date.toString
      }
      case _ => "1970-01-01 00:00:00"
    }
  }
}
