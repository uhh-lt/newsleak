/*
 * Copyright (C) 2016 Language Technology Group and Interactive Graphics Systems Group, Technische Universität Darmstadt, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

define([
    'angular',
    'highcharts',
    'drilldown'
], function (angular) {
    'use strict';
    /**
     * histogramX module:
     * time line bar charts for the occurring time expressions in documents
     * interaction:
     * - drill down/up level of detail on filtering at same time
     * (details see HistogramController.js)
     */
    angular.module('myApp.histogramX', ['play.routing'])
        .factory('HistogramXFactory', [
            function() {
                var config = {
                    // Highcharts options
                    highchartsOptions: {
                        lang: {
                            drillUpText: 'Back to {series.name}',
                            loading: 'Loading...'
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
                                    text: 'No. documents<br>(logarithmic)',
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
        .controller('HistogramXController', [
            '$scope',
            '$compile',
            '$timeout',
            '$q',
            'playRoutes',
            'HistogramXFactory',
            'ObserverService',
            function ($scope, $compile, $timeout, $q, playRoutes, HistogramXFactory, ObserverService) {

                $scope.initialized = false;
                $scope.drilldown = false;
                $scope.drillup = false;

                $scope.emptyFacets = [{'key':'dummy','data': []}];
                $scope.factory = HistogramXFactory;

                $scope.observer = ObserverService;

                $scope.initController = function() {
                    $scope.initialized = false;
                    var defer = $q.defer();
                    $scope.chartConfig = angular.copy($scope.factory.chartConfig.options);

                    $scope.data = [];
                    $scope.dataFilter = [];
                    //current Level of Detail in Histogram
                    $scope.currentLoD = "";
                    $scope.currentRange = "";

                    $scope.observer.getHistogramLod().then(function(lod) {
                        $scope.lod  = angular.copy(lod);
                        $scope.currentLoD = $scope.lod[0];
                        $scope.updateHistogram().then(function(val) {
                            defer.resolve("init");
                        });
                    });
                    return defer.promise;
                };
                // fetch levels of detail from the backend

                $scope.initController();

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
                 * Add time range filter to observer
                 *
                 * @param range - The range delivers the information for which time frame data
                 * shall be loaded (e. g. can be values like '1970-1979', '1970', 'Jan 1980').
                 */
                $scope.addTimeFilter = function(range) {
                    $scope.observer.addItem({
                        type: 'timeX',
                        data: {
                            description: range,
                            item: range,
                            lod: $scope.currentLoD
                        }
                    });
                };

                // set language related options
                Highcharts.setOptions($scope.factory.highchartsOptions);

                $scope.clickedItem = function (category) {
                    $scope.addTimeFilter(category);
                };

                $scope.initHistogram = function() {
                    if($scope.histogram)
                        $scope.histogram.destroy();

                    $scope.chartConfig["series"] = [{
                        name: 'Overview',
                        data: $scope.data,
                        cursor: 'pointer',
                        point: {

                        }
                    },{
                        name:  'Overview',
                        data: $scope.dataFilter,
                        color: 'black',
                        cursor: 'pointer',
                        point: {

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
                    }];

                    $scope.chartConfig.chart.events = {
                        drilldown: function(e) {
                            $scope.drillDown(e, this)
                        },
                        drillup: function(e) {
                            $scope.drillUp(e)
                        }
                    };
                    $scope.chartConfig.chart.renderTo = "histogramX";
                    $("#histogramX").css("height",$("footer").height()-50);

                    $scope.histogram = new Highcharts.Chart($scope.chartConfig);

                    $scope.initialized = true;
                };

                /**
                 * updated on filter changes
                 */
                $scope.updateHistogram = function() {
                    if($scope.histogram)
                        $scope.histogram.showLoading('Loading ...');
                    console.log("reload histogramX");
                     var promise = $q.defer();

                    var entities = [];
                    angular.forEach($scope.entityFilters, function(item) {
                        entities.push(item.data.id);
                    });
                    var facets = $scope.observer.getFacets();
                    var fulltext = [];
                    angular.forEach($scope.fulltextFilters, function(item) {
                        fulltext.push(item.data.item);
                    });
                    playRoutes.controllers.HistogramController.getTimeExprTimeline(fulltext,facets,entities,"",$scope.currentRange,$scope.currentLoD).get().then(function(respone) {
                        var overallPromise = $q.defer();
                        if($scope.drilldown ||  $scope.drillup) {
                            playRoutes.controllers.HistogramController.getTimeExprTimeline("",$scope.emptyFacets,[],"",$scope.currentRange,$scope.currentLoD).get().then(function(responeAll) {
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
                                                if($scope.lod.indexOF$scope.currentLoD == $scope.lod[$scope.lod.length -1])
                                                    $scope.clickedItem(this.name);
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

                            promise.resolve('suc: histogram');
                        });

                    });
                    return  promise.promise;
                };

                $scope.updateLoD = function(lod) {
                    $scope.currentLoD = lod;
                };

                $scope.observer.registerObserverCallback({
                    priority: 3,
                    callback: function() {
                        if(!$scope.drilldown && !$scope.drillup) {
                            return $scope.updateHistogram();
                        }
                    }
                });

                $scope.observer.subscribeReset(function() {
                    return $scope.initController();
                });

                $scope.drillDown = function(e, chart) {
                    if (!e.seriesOptions && !$scope.drilldown) {
                        console.log("histogramX drilldown");
                        $scope.drilldown = true;
                        if($scope.lod[$scope.lod.length -1] != $scope.currentLoD) {
                            $scope.currentLoD = $scope.lod[$scope.lod.indexOf($scope.currentLoD) + 1];
                        }
                        if($scope.lod[$scope.lod.length -1] != $scope.currentLoD)
                            $scope.clickedItem(e.point.name);
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
                                            if($scope.lod[$scope.lod.length -1] == $scope.currentLoD)
                                                $scope.clickedItem(this.name);
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
                                            if($scope.lod[$scope.lod.length -1] == $scope.currentLoD)
                                                $scope.clickedItem(this.name);
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
                    } else {
                        console.log('canceled dup drilldown');
                    }
                };

                $scope.drillUp = function(e) {
                    if (!$scope.drillup) {
                        console.log("histogramX drillup");
                        $scope.drillup = true;
                        $scope.currentLoD = $scope.lod[$scope.lod.indexOf($scope.currentLoD) - 1];
                        $scope.observer.drillUpXTimeFilter();
                        if ($scope.lod.indexOf($scope.currentLoD) == 0)
                            $scope.currentRange = "";
                        else
                            $scope.currentRange = $scope.observer.getXTimeRange();
                        $scope.updateHistogram().then(function () {
                            $scope.histogram.series[0].setData($scope.data);
                            var series = {
                                data: $scope.dataFilter,
                                name:  $scope.currentLoD,
                                cursor: 'pointer',
                                point: {
                                    events: {

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

