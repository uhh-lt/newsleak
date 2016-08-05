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

    angular.module('myApp.histogram', ['play.routing', 'highcharts-ng', 'angularMoment', 'myApp.source', 'myApp.sourcefactory'])
        .factory('HistogramFactory', [
            'util',
            'moment',
            function(util, moment) {
                var config = {
                    weekdays: [
                        'Sunday', 'Monday', 'Tuesday', 'Wednesday',
                        'Thursday', 'Friday', 'Saturday'
                    ],
                    monthAbbreviations: [
                        'Jan', 'Feb', 'March', 'Apr', 'May', 'June',
                        'July', 'Aug', 'Sept', 'Oct', 'Nov', 'Dec'
                    ],
                    // Highcharts options
                    highchartsOptions: {
                        lang: {
                            resetZoom: 'Reset view',
                            drillUpText: 'Back to {series.name}',
                            loading: 'Loading...',
                            weekdays: this.weekdays,
                            shortMonths: this.monthAbbreviations
                        }
                    },
                    // General configuration for the chart
                    chartConfig: {
                        options: {
                            title: {
                                text: null
                            },
                            chart: {
                                type: 'column',
                                // height: 200,
                                backgroundColor: 'rgba(0,0,0,0)',
                                zoomType: 'xy',
                                resetZoomButton: {
                                    position: {
                                        align: 'left',
                                        y: -5
                                    }
                                },
                                reflow: true,
                                events: ""
                            },
                            credits: false,
                            xAxis: {
                                title: {
                                    text: 'Date',
                                    enabled: false
                                },
                                type: 'category'
                            },
                            yAxis: {
                                title: {
                                    text: 'Number of Documents<br>(logarithmic)',
                                    offset: 60
                                }
                                ,
                                type: 'logarithmic',
                                tickInterval: 2,
                                stackLabels: {
                                    enabled: true
                                },
                                gridLineWidth: 0
                            },
                            plotOptions: {
                                column: {
                                    grouping: false,
                                    shadow: false,
                                    dataLabels: {
                                        enabled: true,
                                        padding: 0
                                    }
                                }

                            },
                            legend: {
                                enabled: false

                            }
                        },
                        title: {
                            text: ''
                        },
                        drilldown: {
                            series: []
                        },
                        loading: false
                        //chart logic

                    }
                };
                return config;
            }
        ])
        /******************************** CONTROLLER ************************************/
        .controller('HistogramController', [
            '$scope',
            '$compile',
            '$timeout',
            '$q',
            'playRoutes',
            'moment',
            'appData',
            'sourceShareService',
            'util',
            'HistogramFactory',
            'ObserverService',
            function ($scope, $compile, $timeout, $q, playRoutes, moment, appData, sourceShareService, util, HistogramFactory, ObserverService) {

                /*
                 There is an issue with the bar chart: Sometimes the document count is 0 which cannot be plotted.
                 A workaround is to set these 0 values to null which is done in the getDrilldown() method.
                 */
                var PSEUDO_ZERO_VALUE = null;

                $scope.initialized = false;
                $scope.drilldown = false;
                $scope.drillup = false;
                $scope.factory = HistogramFactory;
                $scope.chartConfig = angular.copy(HistogramFactory.chartConfig.options);
                $scope.observer = ObserverService;

                $scope.data = [];
                $scope.dataFilter = [];
                //current Level of Detail in Histogram
                $scope.currentLoD = "";
                $scope.currentRange = "";

                $scope.emptyFacets = [{'key':'dummy','data': []}];

                // fetch levels of detail from the backend
                $scope.observer.getHistogramLod().then(function(lod) {
                    $scope.lod  = angular.copy(lod);
                    $scope.currentLoD = $scope.lod[0];
                    $scope.updateHistogram();
                });


                /**
                 * subscribe entity and metadata filters
                 */
                $scope.observer_subscribe_entity = function(items) { $scope.entityFilters = items};
                $scope.observer_subscribe_metadata = function(items) { $scope.metadataFilters = items};
                $scope.observer_subscribe_fulltext = function(items) { $scope.fulltextFilters = items};
                $scope.observer.subscribeItems($scope.observer_subscribe_entity,"entity");
                $scope.observer.subscribeItems($scope.observer_subscribe_metadata,"metadata");
                $scope.observer.subscribeItems($scope.observer_subscribe_fulltext,"fulltext");



                /**
                 * We use this wrapper in order to override the drilldown method. This is necessary
                 * because we don't want to drilldown on a bar click. Instead, the bar click fetches
                 * documents of the clicked time range.
                 * When clicking the corresponding labels, however, drilldown will be executed

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
                */
                /**
                 * Add time range filter to observer
                 *
                 * @param range - The range delivers the information for which time frame data
                 * shall be loaded (e. g. can be values like '1970-1979', '1970', 'Jan 1980').
                 */
                $scope.addTimeFilter = function(range) {
                    $scope.observer.addItem({
                        type: 'time',
                        data: {
                            name: range,
                            lod: $scope.currentLoD
                        }
                    });
                };

                /*
                 The click on a label above a bar. I (Patrick) did not find a way to implement
                 the click event using Highcharts. Thus, this solution is a little bit cumbersome
                 because first, the corresponding label below the bar has to be found, then the
                 text (time range) extracted and only then can the documents be fetched.
                 */
                $(document).on('click', '#histogram .stack-label', function () {
                    // + 1 because nth-child is 1-based
                    var x = parseInt($(this).attr('data-x')) + 1;
                    var range = $("#histogram .highcharts-xaxis-labels text:nth-child(" + x + ")")[0].childNodes[0];
                    // We have to handle decade / year and month / day differently, as month / day are within
                    // an additional tspan tag; this is for year / month
                    if (range.data != undefined) {
                        range = range.data;
                    }
                    // For month / day
                    else {
                        range = range.innerHTML;
                    }
                    console.log(range);
                    $scope.addTimeFilter(range);


                });

                // set language related options
                Highcharts.setOptions($scope.factory.highchartsOptions);

                $scope.clickedItem = function (category) {
                    $scope.addTimeFilter(category.name);
                };


                $scope.initHistogram = function() {
                    $scope.chartConfig["series"] = [{
                        name: 'Overview',
                        data: $scope.data,
                        cursor: 'pointer',
                        point: {
                            events: {
                                click: function(e) {
                                    $scope.clickedItem(this);
                                }
                            }
                        }
                    }];

                    $scope.chartConfig.chart.events = {
                        drilldown: function(e) {
                            $scope.drillDown(e, this)
                        },
                        drillup: function(e) {
                            $scope.drillUp(e)
                        }
                    };
                    $scope.chartConfig.chart.renderTo = "histogram";

                    $scope.histogram = new Highcharts.Chart($scope.chartConfig);
                    $scope.initialized = true;
                };

                /**
                 * updated on filter changes
                 */
                $scope.updateHistogram = function() {
                    if($scope.histogram)
                        $scope.histogram.showLoading('Loading ...');
                    console.log("reload histogram");
                    var deferred = $q.defer();
                    var entities = [];
                    angular.forEach($scope.entityFilters, function(item) {
                        entities.push(item.data.id);
                    });
                    var facets = [];
                    if($scope.metadataFilters.length > 0) {
                        angular.forEach($scope.metadataFilters, function(metaType) {
                            if($scope.metadataFilters[metaType].length > 0) {
                                var keys = [];
                                angular.forEach($scope.metadataFilters[metaType], function(x) {
                                    keys.push(x.data.name);
                                });
                                facets.push({key: metaType, data: keys});
                            }
                        });
                        if(facets == 0) facets = $scope.emptyFacets;

                    } else {
                        facets = $scope.emptyFacets;
                    }
                    var fulltext = [];
                    angular.forEach($scope.fulltextFilters, function(item) {
                        fulltext.push(item.data.name);
                    });
                    //TODO: figure out: time filter vs. time range for histogram
                    //playRoutes.controllers.HistogramController.getHistogram(fulltext,facets,entities,$scope.observer.getTimeRange(),$scope.currentLoD).get().then(function(respone) {
                    playRoutes.controllers.HistogramController.getHistogram(fulltext,facets,entities,$scope.currentRange,$scope.currentLoD).get().then(function(respone) {
                        var overallPromise = $q.defer();
                        if($scope.drilldown ||  $scope.drillup) {
                            playRoutes.controllers.HistogramController.getHistogram("",$scope.emptyFacets,[],$scope.currentRange,$scope.currentLoD).get().then(function(responeAll) {
                                $scope.data = [];
                                angular.forEach(responeAll.data.histogram, function(x) {
                                    var count = x.count;
                                    if(x.count == 0)
                                        count = null;
                                    var drilldown = x.range;
                                    if($scope.lod.indexOf($scope.currentLoD) == $scope.lod.length -1) drilldown = false;
                                    $scope.data.push({
                                        name: x.range,
                                        y: count,
                                        drilldown: drilldown,
                                        title: x.range
                                    });
                                });
                                overallPromise.resolve('success');
                            });
                        } else {
                            overallPromise.resolve('success');
                        }

                        overallPromise.promise.then(function() {
                            $scope.dataFilter = [];
                            angular.forEach(respone.data.histogram, function(x) {
                                var count = x.count;
                                if(x.count == 0)
                                    count = null;
                                var drilldown = x.range;
                                if($scope.lod.indexOf($scope.currentLoD) == $scope.lod.length -1) drilldown = false;
                                $scope.dataFilter.push({
                                    name: x.range,
                                    y: count,
                                    drilldown: drilldown,
                                    title: x.range
                                });
                            });
                            if(!$scope.initialized)  {
                                $scope.data = angular.copy($scope.dataFilter);
                                $scope.initHistogram();
                            }
                            else if(!$scope.drilldown && !$scope.drillup) {
                                var name = "Overview";
                                if($scope.currentRange) name = $scope.currentRange;
                                var series = {
                                    data: $scope.dataFilter,
                                    name: name,
                                    dataLabels: {
                                        inside: true,
                                        verticalAlign: "top",
                                        useHTML: true,
                                        formatter : function() {
                                            return $('<div/>').css({
                                                'color' : 'white'
                                            }).text(this.y)[0].outerHTML;
                                        }
                                    },
                                    color: 'black',
                                    cursor: 'pointer',
                                    point: {
                                        events: {
                                            click: function () {
                                                $scope.clickedItem(this);
                                            }
                                        }
                                    }
                                };
                                if($scope.histogram.series[1])
                                    $scope.histogram.series[1].setData($scope.dataFilter);
                                else
                                    $scope.histogram.addSeries(series);
                            }


                            $scope.histogram.hideLoading();

                            deferred.resolve('success');
                        });

                    });
                    return deferred.promise;
                };

                $scope.updateLoD = function(lod) {
                    $scope.currentLoD = lod;
                };



                $scope.observer.registerObserverCallback(function() {
                    if(!$scope.drilldown && !$scope.drillup)
                        $scope.updateHistogram()
                });

                $scope.drillDown = function(e, chart) {
                    console.log("histogram drilldown");

                    if (!e.seriesOptions) {
                        $scope.drilldown = true;
                        $scope.currentLoD = $scope.lod[$scope.lod.indexOf($scope.currentLoD) + 1];
                        if($scope.lod.indexOf($scope.currentLoD) == 0)
                            $scope.currentRange = "";
                        else
                            $scope.currentRange = e.point.name;
                        //$scope.addTimeFilter(e.point.name);
                        $scope.updateHistogram().then(function (res) {

                            $scope.drilldown = false;
                            var series = [{
                                name: e.point.name,
                                data: $scope.data,
                                color: 'rgb(149, 206, 255)',
                                cursor: 'pointer',
                                point: {
                                    events: {
                                        click: function(e) {
                                            $scope.clickedItem(this);
                                        }
                                    }
                                }
                            },
                                {
                                name: e.point.name,
                                data: $scope.dataFilter,
                                color: 'black',
                                cursor: 'pointer',
                                point: {
                                    events: {
                                        click: function(e) {
                                            $scope.clickedItem(this);
                                        }
                                    }
                                },
                                dataLabels: {
                                    inside: true,
                                    verticalAlign: "top",
                                    useHTML: true,
                                    formatter : function() {
                                        return $('<div/>').css({
                                            'color' : 'white'
                                        }).text(this.y)[0].outerHTML;
                                    }
                                }
                            }
                            ];
                            chart.addSingleSeriesAsDrilldown(e.point, series[0]);
                            chart.addSingleSeriesAsDrilldown(e.point, series[1]);
                            chart.applyDrilldown();
                        });
                    }
                };

                $scope.drillUp = function(e) {
                    if (!$scope.drillup) {
                        console.log("histogram drillup");
                        $scope.drillup = true;
                        $scope.currentLoD = $scope.lod[$scope.lod.indexOf($scope.currentLoD) - 1];
                        $scope.observer.drillUpTimeFilter();
                        if ($scope.lod.indexOf($scope.currentLoD) == 0)
                            $scope.currentRange = "";
                        else
                            $scope.currentRange = $scope.observer.getTimeRange();
                        $scope.updateHistogram().then(function () {
                            $scope.histogram.series[0].setData($scope.data);
                            var series = {
                                data: $scope.dataFilter,
                                name:  $scope.currentLoD,
                                cursor: 'pointer',
                                point: {
                                    events: {
                                        click: function () {
                                            $scope.clickedItem(this);
                                        }
                                    }
                                }
                            };
                            if ($scope.histogram.series[1])
                                $scope.histogram.series[1].setData($scope.dataFilter);
                            else
                                $scope.histogram.addSeries(series);
                            $scope.drillup = false;

                        });
                    }
                }
            }
        ])
});

