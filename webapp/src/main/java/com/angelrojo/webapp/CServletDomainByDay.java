package com.angelrojo.webapp;

import com.datastax.driver.core.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TimeZone;


@WebServlet("/CServletDomainByDay")
public class CServletDomainByDay extends HttpServlet{

    public CServletDomainByDay() {
        super();
    }

    private static final long serialVersionUID = 1L;

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

        /* obtener el parametro de la url del servlet asignado en el javascript areaChar_evolutionByDomain.js
        * que corresponde con el dominio por el cual se va a filtrar en la query a Cassandra */
        String domain = request.getParameter("domain");
        System.out.println("DOMINIO RECOGIDO DE LA URL   -->   "+domain);

        /* Cassandra connection */
        Cluster cluster;
        Session session;
        cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
        session = cluster.connect("usagov");

        /* Cassandra query */
        PreparedStatement statement = session.prepare(
                "SELECT date, time, domain, contador " +
                        "FROM topdomainminutes " +
                        "WHERE date = ?  and domain = ? " );
        BoundStatement boundStatement = new BoundStatement(statement);
        ResultSet results = session.execute(boundStatement.bind(today,domain));

        /* HashMap para agregar por hora y minuto y sumar sus contadores. Esto sucede porque el contador
        * es parte de la pk en la tabla de cassandra y aunque la ventana de spark streaming es de 60sg y
        * se desliza 60sg, es decir, escribe en Cassandra cada minuto, a veces hay varias escrituras para
        * un mismo minuto */
        HashMap<String, ArrayList<Integer>> map = new HashMap<String, ArrayList<Integer>>();

        /* Insertar el resultado de la query en el HashMap */
        for (Row row : results) {
            String key = row.getString("date") + " " + row.getString("time") + "/" + row.getString("domain");
            int number = row.getInt("contador");
            if (map.get(key) == null) {
                map.put(key, new ArrayList<Integer>());
            }
            map.get(key).add(number);
        }

        /* Recorrer el HashMap y parsear el resultado a json*/
        StringBuffer jsonStr = new StringBuffer();
        jsonStr.append("{\r\n");
        jsonStr.append("  \"name_query\": \"EvolutionByDomain\",\r\n");
        jsonStr.append("  \"description\": \"Evolución diaria de un dominio\",\r\n");
        jsonStr.append("  \"results\":\r\n");
        jsonStr.append("  [\r\n");
        for(Entry < String, ArrayList < Integer >> ee: map.entrySet()) {
            String key = ee.getKey();
            ArrayList<Integer> values = ee.getValue();

        System.out.println(key + ": " + values);

            jsonStr.append("    {\r\n");
            jsonStr.append("      \"date\": \"" + key.split("/")[0] + "\",\r\n");
            jsonStr.append("      \"dominio\": \"" + key.split("/")[1] + "\",\r\n");
            /* suma de contadores */
            int sum = 0;
            for (int i : values) {
                sum += i;
            }
            jsonStr.append("      \"contador\": " + sum + "\r\n");
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

        cluster.close();
    }

}
