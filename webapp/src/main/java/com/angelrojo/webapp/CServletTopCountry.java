package com.angelrojo.webapp;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.BoundStatement;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map.Entry;


@WebServlet("/CServletTopCountry")
public class CServletTopCountry extends HttpServlet{

    public CServletTopCountry() {
        super();
    }

    private static final long serialVersionUID = 1L;
    private static final Locale lang = Locale.ENGLISH;


    /* Metodo INIT */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /* Metodo GET */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        TimeZone.setDefault(TimeZone.getTimeZone("UTF"));
        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd");
        Date date = new Date();
        String today = dateFormat.format(date);

        /* Cassandra conection */
        Cluster cluster;
        Session session;
        cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
        session = cluster.connect("usagov");

        /* Cassandra query con parametros*/
        PreparedStatement statement = session.prepare(
                "SELECT country_code, contador " +
                        "FROM topcountryminutes " +
                        "WHERE date = ?  ");
        BoundStatement boundStatement = new BoundStatement(statement);
        ResultSet results = session.execute(boundStatement.bind(today));

        /* HashMap para agregar por pais y sumar sus contadores */
        HashMap<String, ArrayList<Integer>> map = new HashMap<String, ArrayList<Integer>>();

        /* Insertar el resultado de la query en el HashMap */
        for (Row row: results) {
            String key = row.getString("country_code");
            int number = row.getInt("contador");
            if (map.get(key) == null) map.put(key, new ArrayList<Integer>());
            map.get(key).add(number);
        }

        /* Recorrer el HashMap y parsear el resultado a json*/
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
            /* suma de los contadores */
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

        /* http response */
        response.setContentType("aplication/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonStr.toString());
        response.getWriter().flush();
        response.getWriter().close();

        // Set standard HTTP/1.1 no-cache headers.
        //response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
        // Set standard HTTP/1.0 no-cache header.
        //response.setHeader("Pragma", "no-cache");

        cluster.close();
    }

    public static void main(String[] args) {

        TimeZone.setDefault(TimeZone.getTimeZone("UTF"));
        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd");
        Date date = new Date();
        String today = dateFormat.format(date);

        /* Cassandra conection */
        Cluster cluster;
        Session session;
        cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
        session = cluster.connect("usagov");

        /* Cassandra query con parametros*/
        PreparedStatement statement = session.prepare(
                "SELECT country_code, contador " +
                        "FROM topcountryseconds " +
                        "WHERE date = ?  ");
        BoundStatement boundStatement = new BoundStatement(statement);
        ResultSet results = session.execute(boundStatement.bind(today));

        /* HashMap para agregar por pais y sumar sus contadores */
        HashMap<String, ArrayList<Integer>> map = new HashMap<String, ArrayList<Integer>>();

        /* Insertar el resultado de la query en el HashMap */
        for (Row row: results) {
            String key = row.getString("country_code");
            int number = row.getInt("contador");
            if (map.get(key) == null) {
                map.put(key, new ArrayList<Integer>());
            }
            map.get(key).add(number);
        }

        /* Recorrer el HashMap y parsear el resultado a json*/
        StringBuffer jsonStr = new StringBuffer();
        jsonStr.append("{\r\n");
        jsonStr.append("  \"name_query\": \"topcountry\",\r\n");
        jsonStr.append("  \"description\": \"Accesos usa.gov por paises\",\r\n");
        jsonStr.append("  \"results\":\r\n");
        jsonStr.append("  [\r\n");
        for (Entry<String, ArrayList<Integer>> ee : map.entrySet()) {
            String key = ee.getKey();
            ArrayList<Integer> values = ee.getValue();
            //System.out.println(key+": "+values);
            jsonStr.append("    {\r\n");
            jsonStr.append("      \"country\": \"" + Countries.getCountry(key, lang) + "\",\r\n");
            /* suma de los contadores */
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

        System.out.println(jsonStr);

        cluster.close();
    }


}
