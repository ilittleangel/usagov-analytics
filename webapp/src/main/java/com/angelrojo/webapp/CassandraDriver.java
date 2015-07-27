package com.angelrojo.webapp;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * Clase para recuperar de Cassandra todos los eventos de la column famile "clicks"
 * en la cual se almacenan todos los eventos y montar un fichero json
 * */


public class CassandraDriver {


    public static void main(String[] args) {

        Cluster cluster;
        Session session;
        ResultSet results;

        cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
        session = cluster.connect("usagov");


        try {
            PrintWriter writer = new PrintWriter("/home/cloudera/Desktop/clicks34534.json", "UTF-8");
            results = session.execute("SELECT date, event FROM clicks limit 10");
            String event;
            String date;
            //int i = 3;
            for (Row row : results) {
                //System.out.format("%s %s\n", row.getString("date"), row.getString("event"));
                event = row.getString("event");
                date = row.getString("date");
                //writer.write("{\"index\":{\"_id\":\""+i+"\"}}\n");
                writer.write("{\"date\":\"" + date + "\"," + event.substring(1) + "\n");
                //i = i + 1;
            }
            writer.close();
        } catch (IOException ex) {
            System.out.println(ex);
        }

        cluster.close();


    }
}