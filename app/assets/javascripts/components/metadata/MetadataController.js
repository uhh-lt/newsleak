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

                    $scope.frequencies = [];
                    $scope.labels = [];
                    $scope.ids = [];
                    $scope.chartConfigs = [];
                    $scope.metaCharts = [];
                    $scope.chartConfig = metaShareService.chartConfig;
                    $scope.observer = ObserverService;

                    $scope.observer.getEntityTypes().then(function(types) {$scope.entityTypes  = types});
                    $scope.observer.getMetadataTypes().then(function(types) {$scope.metadataTypes  = types});

                    /**
                     * subscribe entity and metadata filters
                     */
                    $scope.observer_subscribe_entity = function(items) { $scope.entityFilters = items};
                    $scope.observer_subscribe_metadata = function(items) { $scope.metadataFilters = items};
                    $scope.observer.subscribeItems($scope.observer_subscribe_entity,"entity");
                    $scope.observer.subscribeItems($scope.observer_subscribe_metadata,"metadata");

                    $scope.updateEntityCharts = function () {

                    };

                    $scope.updateMetadataCharts = function() {
                        var fulltext = undefined;
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
                            if(facets == 0) facets = [{'key':'dummy','data': []}];

                        } else {
                            facets = [{'key':'dummy','data': []}];
                        }
                        angular.forEach($scope.metadataTypes, function(type) {
                            var instances = $scope.chartConfigs[type].xAxis["categories"];
                            playRoutes.controllers.MetadataController.getSpecificMetadata(fulltext,type,facets,entities,instances,$scope.observer.getTimeRange()).get().then(
                                function(result) {
                                    //result.data[type].forEach(function(x) {
                                    //    console.log(x.key + ": " + x.count);
                                    //});
                                    var data = [];
                                    angular.forEach(result.data[type], function(x) {
                                        data.push(x.count);
                                    });
                                    var newBase = [];
                                    $.each($scope.chartConfigs[type].series[0].data, function(index, value) {
                                        if(data[index] == undefined)
                                            newBase.push($scope.chartConfigs[type].series[0].data[index]);
                                        else
                                            newBase.push($scope.chartConfigs[type].series[0].data[index] - data[index]);
                                    });
                                    $scope.metaCharts[type].series[0].setData(newBase);
                                    if($scope.metaCharts[type].series[1] == undefined) {
                                        $scope.metaCharts[type].addSeries({
                                            name: 'Filter',
                                            data: data,
                                            color: 'black'
                                        });
                                    } else {
                                        $scope.metaCharts[type].series[1].setData(data);
                                    }
                                }
                            );
                        });
                        //TODO: on adding fulltext filter doc count grows

                    };

                    $scope.initEntityCharts = function () {
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
                                                        $scope.entityFilters.forEach(function(x) {
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
                                                        //playRoutes.controllers.EntityController
                                                        //    .getEntitiesDocCountWithFilter($scope.filters).get().then(function (res) {
                                                        //    console.log(res.data);
                                                            //TODO: entity filter series currently not available through ES
                                                            //$scope.metaCharts[x].addSeries({
                                                            //    name: 'Filter',
                                                            //    data: [res.data],
                                                            //    color: 'black'
                                                            //});
                                                        //})
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

                    $scope.initMetadataCharts = function () {
                        $scope.observer.getMetadataTypes().then(function(types) {



                                playRoutes.controllers.MetadataController.getMetadata(undefined,[{'key':'dummy','data': []}]).get().then(
                                    function (result) {

                                        $.each(result.data,function() {
                                            $.each(this, function(key, value) {
                                                //console.log(value);

                                            $scope.chartConfigs[key] = angular.copy($scope.chartConfig);


                                        $scope.frequencies[key] = [];
                                        $scope.labels[key] = [];
                                        $scope.ids[key] = [];
                                                value.forEach(function (metadata) {
                                            $scope.frequencies[key].push(metadata.count);
                                            $scope.labels[key].push(metadata.key);
                                            //$scope.ids[key].push(entity.id);
                                        });


                                        $scope.chartConfigs[key].xAxis["categories"] = $scope.labels[key];

                                        $scope.chartConfigs[key]["series"] = [{
                                            name: 'Total',
                                            data: $scope.frequencies[key],
                                            cursor: 'pointer',
                                            point: {
                                                events: {
                                                    click: function () {
                                                        /*
                                                        $scope.filters = [];
                                                        $scope.filterItems.forEach(function(x) {
                                                            $scope.filters.push(x.data.id);
                                                        });
                                                        $scope.filters.push($scope.ids[x][$scope.labels[x].indexOf(this.category)]);
                                                        */
                                                        $scope.observer.addItem({
                                                            type: 'metadata',
                                                            data: {
                                                                //id: $scope.ids[x][$scope.labels[x].indexOf(this.category)]
                                                                name: this.category,
                                                                type: key
                                                            }
                                                        });
                                                        console.log('Category: ' + this.category + ', value: ' + this.y + ', filters: ' + $scope.filters);

                                                    }
                                                }
                                            }

                                        }];
                                        $scope.chartConfigs[key].chart.renderTo = "chart_" + key.toLowerCase();

                                        $scope.metaCharts[key] = new Highcharts.Chart($scope.chartConfigs[key]);
                                            });

                                        }
                                );
                            })
                        })
                    };

                    $scope.updateMetadataView = function() {
                        $scope.updateEntityCharts();
                        $scope.updateMetadataCharts();
                    };

                    $scope.initMetadataView = function() {
                        $scope.initEntityCharts();
                        $scope.initMetadataCharts();
                    };

                    $scope.observer.registerObserverCallback($scope.updateMetadataView);
                    //load another 50 entities for specific metadata
                    $scope.loadMore = function (ele) {
                        if (ele.mcs.top != 0) {
                            var type = $(ele).find(".meta-chart").attr("id");
                            console.log('load more metadata: ' + type);

                        }
                    };

                    $scope.initMetadataView();

                    $scope.reflow = function(type) {
                        $timeout(function() {
                            $("#chart_" + type.toLowerCase()).highcharts().reflow();
                        }, 100);
                    };

                    /** entry point here **/
                    $scope.metaShareService = metaShareService;
                    $scope.sourceShareService = sourceShareService;
                    $scope.filterShareService = filterShareService;


                    //TODO: calc height on bar count -> scroll bar
                    $scope.tabHeight = $("#metadata").height() - 100;
                    //console.log($scope.tabHeight);
                    //console.log(sourceShareService);
                    //console.log(filterShareService);

                }
            ]
        );
});