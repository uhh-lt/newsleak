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
    'angularMoment',
    'jquery-json',
    '../../factory/appData',
    'ngFileSaver'
], function (angular) {
    'use strict';

    angular.module("myApp.metadata", ['play.routing', 'angularMoment', 'ngFileSaver', 'highcharts-ng']);
    angular.module("myApp.metadata")
        .controller('MetadataController',
            [
                '$scope',
                '$timeout',
                'playRoutes',
                'appData',
                'metaShareService',
                'sourceShareService',
                'filterShareService',
                'ObserverService',
                function ($scope, $timeout, playRoutes, appData, metaShareService, sourceShareService, filterShareService, ObserverService) {

                    //broadcast reflow on highcharts
                    /*$scope.reflow = function() {
                     $scope.$broadcast('highchartsng.reflow');

                     };*/

                    $scope.frequencies = [];
                    $scope.labels = [];
                    $scope.ids = [];
                    $scope.chartConfigs = [];
                    $scope.metaCharts = [];
                    $scope.chartConfig = metaShareService.chartConfig;


                    $scope.observer = ObserverService;

                    $scope.observer.getEntityTypes().then(function(types) {$scope.metadataTypes  = types});

                    $scope.observer_subscribe = function(items) { $scope.filterItems = items};
                    $scope.observer.subscribeItems($scope.observer_subscribe, "entity");

                    //need to reflow the chart using bootstrap tabs
                    $('#metadata .nav-tabs a').on('shown.bs.tab', function (event) {
                        $(event.target.hash).find(".meta-chart").highcharts().reflow();
                    });
                    $scope.updateEntityCharts = function () {
                        $scope.observer.getEntityTypes().then(function(types) {
                            types.forEach(function (x) {
                                $scope.chartConfigs[x] = angular.copy($scope.chartConfig);

                                playRoutes.controllers.EntityController.getEntitiesByType(x).get().then(
                                    function (result) {

                                        $scope.frequencies[x] = [];
                                        $scope.labels[x] = [];
                                        $scope.ids[x] = [];
                                        result.data.forEach(function (entity) {
                                            $scope.frequencies[x].push(entity.freq);
                                            $scope.labels[x].push(entity.name);
                                            $scope.ids[x].push(entity.id);
                                        });


                                        $scope.chartConfigs[x].xAxis["categories"] = $scope.labels[x];
                                        $scope.chartConfigs[x]["series"] = [{
                                            name: 'Total',
                                            data: $scope.frequencies[x],
                                            cursor: 'pointer',
                                            point: {
                                                events: {
                                                    click: function () {
                                                        $scope.filters = [];
                                                        $scope.filterItems.forEach(function(x) {
                                                            $scope.filters.push(x.data.id);
                                                        });
                                                        $scope.filters.push($scope.ids[x][$scope.labels[x].indexOf(this.category)]);
                                                        $scope.observer.addItem({
                                                            type: 'entity',
                                                            data: {
                                                                id: $scope.ids[x][$scope.labels[x].indexOf(this.category)],
                                                                name: this.category,
                                                                type: x
                                                            }
                                                        });
                                                        console.log('Category: ' + this.category + ', id:' + $scope.ids[x][$scope.labels[x].indexOf(this.category)]+', value: ' + this.y + ', filters: ' + $scope.filters);
                                                        playRoutes.controllers.EntityController
                                                            .getEntitiesDocCountWithFilter($scope.filters).get().then(function (res) {
                                                            console.log(res.data);
                                                            //TODO: entity filter series currently not available through ES
                                                            //$scope.metaCharts[x].addSeries({
                                                            //    name: 'Filter',
                                                            //    data: [res.data],
                                                            //    color: 'black'
                                                            //});
                                                        })
                                                    }
                                                }
                                            }
                                        }];
                                        $scope.chartConfigs[x].chart.renderTo = "chart_" + x.toLowerCase();

                                        $scope.metaCharts[x] = new Highcharts.Chart($scope.chartConfigs[x]);
                                    });

                            });
                        }
                        )

                    };

                    $scope.updateMetadataView = function() {
                        $scope.updateEntityCharts();
                    };

                    $scope.observer.registerObserverCallback($scope.updateMetadataView);
                    //load another 50 entities for specific metadata
                    $scope.loadMore = function (ele) {
                        if (ele.mcs.top != 0) {
                            var type = $(ele).find(".meta-chart").attr("id");
                            console.log('load more metadata: ' + type);

                        }
                    };


                    $scope.updateMetadataView();

                    $scope.reflow = function() {
                       //TODO: reflow on bart charts visible

                    };

                    /** entry point here **/
                    $scope.metaShareService = metaShareService;
                    $scope.sourceShareService = sourceShareService;
                    $scope.filterShareService = filterShareService;


                    //TODO: ui-view height -> scroll bar
                    $scope.tabHeight = $("#metadata").height() - 100;
                    //console.log($scope.tabHeight);
                    //console.log(sourceShareService);
                    //console.log(filterShareService);

                }
            ]
        );
});