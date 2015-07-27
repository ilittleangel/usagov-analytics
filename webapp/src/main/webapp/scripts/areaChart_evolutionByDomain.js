$(document).ready(function() {

    var param = String(window.location.href).split('?')[1]
    var domain = param.split('=')[1]

    $.get('http://localhost:8080/usagov-analytics-webapp-1.0/CServletDomainByDay?domain='+domain, function(responseText) {

        $('#dominio').text(responseText.results[0].dominio);

        var chart;

        var chartData = [];

        generateChartData();

        chartData.sort(function(a, b){return a.date - b.date});

        // SERIAL CHART
        chart = new AmCharts.AmSerialChart();

        chart.dataProvider = chartData;
        chart.categoryField = "date";

        // data updated event will be fired when chart is first displayed,
        // also when data will be updated. We'll use it to set some
        // initial zoom
        chart.addListener("dataUpdated", zoomChart);

        // AXES
        // Category
        var categoryAxis = chart.categoryAxis;
        categoryAxis.parseDates = true; // in order char to understand dates, we should set parseDates to true
        categoryAxis.minPeriod = "mm"; // as we have data with minute interval, we have to set "mm" here.
        categoryAxis.gridAlpha = 0.07;
        categoryAxis.axisColor = "#DADADA";

        // Value
        var valueAxis = new AmCharts.ValueAxis();
        valueAxis.gridAlpha = 0.07;
        valueAxis.title = "Unique visitors";
        chart.addValueAxis(valueAxis);

        // GRAPH
        var graph = new AmCharts.AmGraph();
        graph.type = "line"; // try to change it to "column"
        graph.title = "red line";
        graph.valueField = "visits";
        graph.lineAlpha = 1;
        graph.lineColor = "#d1cf2a";
        graph.fillAlphas = 0.3; // setting fillAlphas to > 0 value makes it area graph
        chart.addGraph(graph);

        // CURSOR
        var chartCursor = new AmCharts.ChartCursor();
        chartCursor.cursorPosition = "mouse";
        chartCursor.categoryBalloonDateFormat = "JJ:NN, DD MMMM";
        chart.addChartCursor(chartCursor);

        // SCROLLBAR
        var chartScrollbar = new AmCharts.ChartScrollbar();
        chartScrollbar.graph = "graph21";

        chart.addChartScrollbar(chartScrollbar);

        // WRITE
        chart.write("chartdiv");


        // generate some random data, quite different range
        function generateChartData() {

            // and generate 1000 data items
            for (var i = 0; i < responseText.results.length; i++) {

                //console.log(responseText.results[i].date);

                var newDate = new Date(Date.parseExact(responseText.results[i].date, "yyyy-MM-dd HH:mm"));
                // each time we add one minute
                //newDate.setMinutes(newDate.getMinutes() + i);
                // some random number
                var visits = responseText.results[i].contador;
                // add data item to the array
                chartData.push({
                    date: newDate,
                    visits: visits
                });
            }
        }

        // this method is called when chart is first inited as we listen for "dataUpdated" event
        function zoomChart() {
            // different zoom methods can be used - zoomToIndexes, zoomToDates, zoomToCategoryValues
            chart.zoomToIndexes(chartData.length - 40, chartData.length - 1);
        }

    });

});