$(document).ready(function() {
    
    /* CONTADOR GLOBAL */
    $.get('http://localhost:8080/usagov-analytics-webapp-1.0/CServletGlobalCounter', function(responseText) {

        $('#description2').text(responseText.description);
        $('#today').text(responseText.date);
        $('#globalCount').text(responseText.results[0].contador);
        document.getElementById("globalCount").style.fontSize = "xx-large";
    });



    /* DIAGRAMAS DE BARRAS */
    $.get('http://localhost:8080/usagov-analytics-webapp-1.0/CServletTopCountry', function(responseText) {


        /****************************
         DIAGRAMA DE BARRAS DE PAISES
         ****************************/

        var data = responseText.results;

        // sort descending to show the top
        data.sort(function(a, b){return b.contador - a.contador});

        // remove the elements except the first 20
        data.splice(30,data.length);

        // SERIAL CHART
        chart = new AmCharts.AmSerialChart();
        chart.dataProvider = data;
        chart.categoryField = "country";
        chart.startDuration = 1;
        chart.addLabel = chart.allLabels;
        chart.categoryAxis.forceShowField = "forceShow";

        // AXES
        // category
        var categoryAxis = chart.categoryAxis;
        categoryAxis.labelRotation = 45;
        categoryAxis.gridPosition = "start";

        // value
        // in case you don't want to change default settings of value axis,
        // you don't need to create it, as one value axis is created automatically.

        // GRAPH
        var graph = new AmCharts.AmGraph();
        graph.valueField = "contador";
        graph.balloonText = "[[category]]: <b>[[value]]</b>";
        graph.type = "column";
        graph.lineAlpha = 0;
        graph.fillAlphas = 0.8;
        chart.addGraph(graph);

        // CURSOR
        var chartCursor = new AmCharts.ChartCursor();
        chartCursor.cursorAlpha = 0;
        chartCursor.zoomable = false;
        chartCursor.categoryBalloonEnabled = false;
        chart.addChartCursor(chartCursor);

        chart.creditsPosition = "top-right";

        chart.write("chartdiv");



        /*************************************
         DIAGRAMA DE BARRAS DE PAISES SIN EEUU
         *************************************/

        // remove EEUU
        Array.prototype.removeValue = function(name, value) {
           var array = $.map(this, function(v,i) {
              return v[name] === value ? null : v;
           });
           this.length = 0; //clear original array
           this.push.apply(this, array); //push all elements except the one we want to delete
        }
        data.removeValue("country", "United States");

        // remove the elements except the first 30
        //data2.splice(30,data2.length);

        // SERIAL CHART
        chart2 = new AmCharts.AmSerialChart();
        chart2.theme = "light";
        chart2.dataProvider = data;
        chart2.categoryField = "country";
        chart2.startDuration = 1;
        chart2.addLabel = chart2.allLabels;
        chart2.categoryAxis.forceShowField = "forceShow";

        // AXES
        // category
        var categoryAxis2 = chart2.categoryAxis;
        categoryAxis2.labelRotation = 45;
        categoryAxis2.gridPosition = "start";

        // GRAPH
        var graph2 = new AmCharts.AmGraph();
        graph2.valueField = "contador";
        graph2.balloonText = "[[category]]: <b>[[value]]</b>";
        graph2.type = "column";
        graph2.lineAlpha = 0;
        graph2.fillAlphas = 0.8;
        chart2.addGraph(graph2);

        // CURSOR
        var chartCursor2 = new AmCharts.ChartCursor();
        chartCursor2.cursorAlpha = 0;
        chartCursor2.zoomable = false;
        chartCursor2.categoryBalloonEnabled = false;
        chart2.addChartCursor(chartCursor2);

        chart2.creditsPosition = "top-right";

        chart2.write("chartdiv2");



    });

});