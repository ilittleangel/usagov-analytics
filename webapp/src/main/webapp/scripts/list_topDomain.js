/* TOP LIST DOMINIOS */

        $.get('http://localhost:8080/usagov-analytics-webapp-1.0/CServletTopDomain', function(responseText) {

            $('#description').text(responseText.description);

            var dataset = responseText.results;

    	    var container = d3.select("body").selectAll('.searchcontainer')
              .attr({
                    xmlns: "http://www.w3.org/2000/svg",
                    xlink: "http://www.w3.org/1999/xlink",
                    width: 300,
                    height: 300
                })
              .data(dataset.sort(function(a,b) {
                  return b.contador - a.contador; }), function(d) { return d.dominio; });

            var div = container.enter()
              .append("div")
              .attr('class', 'searchcontainer');

            container.exit().remove();

            div.append('a')
              //.attr('class', 'dominio')
              .style('float','right')
              .style('margin-right','100px')
              .style('width','80%')
              .style({font: "15px sans-serif"})
              .attr("xlink:href",
                function(d) { return "http://localhost:8080/usagov-analytics-webapp-1.0/evolutionByDomain.html?domain="+d.dominio })
              .html(function(d) { return d.dominio; })
              ;

            div.append('div')
              .attr('class', 'contador')
              .style('float','right')
              .style('width','5%')
              .style({font: "15px sans-serif"})
              .html(function(d) { return d.contador; });
        });
