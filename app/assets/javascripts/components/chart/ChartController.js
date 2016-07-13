/*
 * Copyright 2016 Technische Universitaet Darmstadt
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

define([
    'angular',
    'highcharts',
    'ngHighcharts',
    'drilldown',
    'angularMoment',
    'bootstrap'
], function (angular) {
    'use strict';

    angular.module('myApp.chart', ['play.routing', 'highcharts-ng', 'angularMoment', 'myApp.source', 'myApp.chartconfig', 'myApp.sourcefactory']);
    angular.module('myApp.chart').controller('ChartController', ['$scope', '$compile', '$timeout', 'playRoutes', 'moment', 'appData', 'sourceShareService', 'util', 'chartConfig','ObserverService',
        function ($scope, $compile, $timeout, playRoutes, moment, appData, sourceShareService, util, chartConfig, ObserverService) {

            /*
             There is an issue with the bar chart: Sometimes the document count is 0 which cannot be plotted.
             A workaround is to set these 0 values to null which is done in the getDrilldown() method.
             */
            var PSEUDO_ZERO_VALUE = null;

            $scope.loadedDocuments = {};

            $scope.observer = ObserverService;

            $scope.observer_subscribe = function(history) { $scope.history = history};
            $scope.observer.subscribeHistory($scope.observer_subscribe, "timeline");

            /**
             * We use this wrapper in order to override the drilldown method. This is necessary
             * because we don't want to drilldown on a bar click. Instead, the bar click fetches
             * documents of the clicked time range.
             * When clicking the corresponding labels, however, drilldown will be executed
             */
            (function (H) {
                H.wrap(H.Point.prototype, 'doDrilldown', function (p, hold, x) {
                    var UNDEFINED;
                    // x is defined when clicked on a category
                    if (x !== UNDEFINED) {
                        p.call(this, hold, x);
                    } else {
                        addTimeFilter(this.options.drilldown.toString());
                    }
                });
            })(Highcharts);

            /**
             * Add time range filter to observer
             *
             * @param range - The range delivers the information for which time frame data
             * shall be loaded (e. g. can be values like '1970-1979', '1970', 'Jan 1980').
             */
            function addTimeFilter(range) {
                $scope.observer.addItem({
                    type: 'time',
                    data: {
                        name: range
                    }
                });
            }

            /*
             The click on a label above a bar. I (Patrick) did not find a way to implement
             the click event using Highcharts. Thus, this solution is a little bit cumbersome
             because first, the corresponding label below the bar has to be found, then the
             text (time range) extracted and only then can the documents be fetched.
             */
            $(document).on('click', '#chartBarchart .stack-label', function () {
                // + 1 because nth-child is 1-based
                var x = parseInt($(this).attr('data-x')) + 1;
                var range = $("#chartBarchart .highcharts-xaxis-labels text:nth-child(" + x + ")")[0].childNodes[0];
                // We have to handle decade / year and month / day differently, as month / day are within
                // an additional tspan tag; this is for year / month
                if (range.data != undefined) {
                    range = range.data;
                }
                // For month / day
                else {
                    range = range.innerHTML;
                }
                addTimeFilter(range);


            });

            // set language related options
            Highcharts.setOptions(chartConfig.highchartsOptions);

            // set general options for the chart
            $scope.chartConfig = chartConfig.chartConfig;

            /**
             * This function loads the data with the amount of documents and
             * creates a bar chart that represents this data.
             */
            function loadAndDrawChart() {
                playRoutes.controllers.DocumentController.getFrequencySeries().get().then(function (response) {
                    var documentData = countDocuments(response.data);
                    $scope.chartConfig.series = [{
                        name: "Kissinger Cables",
                        data: getSeriesData(documentData)
                    }];
                    $scope.chartConfig.options.drilldown = getDrilldown(documentData, "Kissinger Cables");
                });
            }

            /**
             * This function creates the data for the initial barchart.
             *
             * @param countDocuments - The result of the function countDocuments.
             * @return The data for the initial bar chart.
             */
            function getSeriesData(countDocuments) {
                var seriesData = [];
                for (var i = 0; i < countDocuments.length; i++) {
                    seriesData.push({
                        name: countDocuments[i].name,
                        y: countDocuments[i].y,
                        drilldown: countDocuments[i].name
                    });
                }
                return seriesData;
            }

            /**
             * This function creates the drilldown with the given data.
             *
             * @param countDocuments - The result of the function countDocuments.
             * @param {String} name - The name of the series.
             * @return The drilldown for the bar chart.
             */
            function getDrilldown(countDocuments, name) {
                var drilldown = {
                    series: []
                };
                // Iterate over decades.
                for (var a = 0; a < countDocuments.length; a++) {
                    drilldown.series.push({
                        id: countDocuments[a].name,
                        name: name + ' ' + countDocuments[a].name,
                        data: countDocuments[a].data
                    });
                    // Iterate over years.
                    for (var b = 0; b < countDocuments[a].data.length; b++) {
                        var nameB = countDocuments[a].data[b].name;
                        if (countDocuments[a].data[b].y == 0) {
                            countDocuments[a].data[b].drilldown = null;
                            // Replace 0 with null (see comment above PSEUDO_ZERO_VALUE)
                            countDocuments[a].data[b].y = PSEUDO_ZERO_VALUE;
                        }
                        drilldown.series.push({
                            id: nameB,
                            name: name + ' ' + nameB,
                            data: countDocuments[a].subdata[b].data
                        });
                        // Iterate over months.
                        for (var c = 0; c < countDocuments[a].subdata[b].data.length; c++) {
                            var nameC = countDocuments[a].subdata[b].data[c].name;
                            if (countDocuments[a].subdata[b].data[c].y == 0) {
                                countDocuments[a].subdata[b].data[c].drilldown = null;
                                countDocuments[a].subdata[b].data[c].y = PSEUDO_ZERO_VALUE;  // Replace 0 with null
                            }
                            drilldown.series.push({
                                id: nameC,
                                name: name + ' ' + nameC,
                                data: countDocuments[a].subdata[b].subdata[c].data,
                                point: {
                                    events: {
                                        click: function () {
                                            addTimeFilter(this.series.data[this.x].name);
                                        },
                                    }
                                }
                            });
                            // Replace 0 with null
                            for (var i = 0; i < countDocuments[a].subdata[b].subdata[c].data.length; i++) {
                                if (countDocuments[a].subdata[b].subdata[c].data[i].y == 0) {
                                    countDocuments[a].subdata[b].subdata[c].data[i].y = PSEUDO_ZERO_VALUE;
                                }
                            }
                        }
                    }
                }
                return drilldown;
            }


            /**
             * This function returns the amount of days the month 'month' in the
             * year 'year' has.
             *
             * @param {Number} month - The month. A number from the set [1,12].
             * @param {Number} year - The year.
             * @return The amount of days the given month has in the given year.
             */
            function daysInMonth(month, year) {
                return new Date(year, month, 0).getDate();
            }


            /**
             * This function counts how many documents in the given data are on
             * which decade, year, month and day.
             *
             * @param data - An array containing arrays which contain the time
             *               in milliseconds and the amount of documents at this
             *               time.
             * @return A structur that contains the amount of documents on the
             *         decades, years, months and days.
             */
            function countDocuments(data) {
                var documentCount = [];
                var firstDecade = getDecade(data[0][0]);
                var numberOfdecades = (getDecade(data[data.length - 1][0]) - firstDecade) / 10;
                // Iterate over all decades, years, months and days and set
                // the amount of documents to 0.
                for (var decade = 0; decade <= numberOfdecades; decade++) {
                    // Push decade.
                    documentCount.push({
                        name: (firstDecade + 10 * decade) + '-' + (firstDecade + 9 + 10 * decade),
                        y: 0,
                        data: [],
                        subdata: []
                    });
                    for (var year = 0; year < 10; year++) {
                        // Push year.
                        documentCount[decade].data.push({
                            name: firstDecade + 10 * decade + year,
                            y: 0,
                            drilldown: firstDecade + 10 * decade + year
                        });
                        documentCount[decade].subdata.push({
                            data: [],
                            subdata: []
                        })
                        for (var month = 0; month < 12; month++) {
                            // Push month.
                            documentCount[decade].subdata[year].data.push({
                                name: chartConfig.monthAbbreviations[month] + ' ' + (firstDecade + 10 * decade + year),
                                y: 0,
                                drilldown: chartConfig.monthAbbreviations[month] + ' ' + (firstDecade + 10 * decade + year)
                            });
                            documentCount[decade].subdata[year].subdata.push({
                                data: [],
                                subdata: []
                            });
                            var numberOfDays = daysInMonth(month + 1, year);
                            for (var day = 0; day < numberOfDays; day++) {
                                // Push day.
                                documentCount[decade].subdata[year].subdata[month].data.push({
                                    name: (day + 1) + '.' + (month + 1) + '.' + (firstDecade + 10 * decade + year),
                                    y: 0
                                });
                            }

                        }
                    }
                }
                // Insert the amount of documents into documentCount.
                for (var i = 0; i < data.length; i++) {
                    var docs = data[i][1];
                    var date = new Date(data[i][0]);
                    var year = date.getFullYear();
                    var decade = year - (year % 10);
                    var decadeIndex = (decade - firstDecade) / 10;
                    var month = date.getMonth();  // Element of [0,11].
                    var day = date.getDate();  // Element of [1,31].
                    documentCount[decadeIndex].y += docs;
                    documentCount[decadeIndex].data[year - decade].y += docs;
                    documentCount[decadeIndex].subdata[year - decade].data[month].y += docs;
                    documentCount[decadeIndex].subdata[year - decade].subdata[month].data[day - 1].y += docs;
                }
                return documentCount;
            }


            /**
             * This function returns the decade (the year the decade starts)
             * from the given date. The date is in unix time * 1000.
             *
             * @param {Number} date - The date which decade is requested.
             * @return The decade (the year it starts) of the given date.
             */
            function getDecade(date) {
                var d = new Date(date);
                d = d.getFullYear() - (d.getFullYear() % 10);
                return d;
            }

            // draw the chart
            loadAndDrawChart();

        }])
    ;
})
;