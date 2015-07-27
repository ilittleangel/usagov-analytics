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
import java.util.Date;
import java.util.TimeZone;


@WebServlet("/CServletGlobalCounter")
public class CServletGlobalCounter extends HttpServlet{

    public CServletGlobalCounter() {
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
        String today = dateFormat.format(new Date());

        /* Cassandra conection */
        Cluster cluster;
        Session session;
        cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
        session = cluster.connect("usagov");

        /* Cassandra query con parametros*/
        PreparedStatement statement = session.prepare(
                        "SELECT count(*) as contador " +
                        "FROM clicks " +
                        "WHERE date = ? ");
        BoundStatement boundStatement = new BoundStatement(statement);
        ResultSet results = session.execute(boundStatement.bind(today));

        /* Parser del resultado a json */
        StringBuffer jsonStr = new StringBuffer();
        jsonStr.append("{\r\n");
        jsonStr.append("  \"name_query\": \"globalcounter\",\r\n");
        jsonStr.append("  \"description\": \"Contador de accesos diarios\",\r\n");
        jsonStr.append("  \"date\": \"" + new SimpleDateFormat("EEE, d MMM yyyy").format(new Date()) + "\",\r\n");
        jsonStr.append("  \"results\":\r\n");
        jsonStr.append("  [\r\n");
        for (Row row : results) {
            jsonStr.append("    {\r\n");
            jsonStr.append("      \"contador\": \"" + row.getLong("contador") + "\"\r\n");
            jsonStr.append("    }\r\n");
        }
        jsonStr.append("  ]\r\n");
        jsonStr.append("}\r\n");

        /* http response */
        response.setContentType("aplication/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonStr.toString());
        response.getWriter().flush();
        response.getWriter().close();

        cluster.close();
    }

    /* Metodo POST */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    }


}
