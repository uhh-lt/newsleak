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
                'ObserverService',
                function ($scope, $timeout, playRoutes, appData, metaShareService, sourceShareService, ObserverService) {

                    $scope.initialized = false;

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
                    $scope.observer_subscribe_fulltext = function(items) { $scope.fulltextFilters = items};
                    $scope.observer.subscribeItems($scope.observer_subscribe_entity,"entity");
                    $scope.observer.subscribeItems($scope.observer_subscribe_metadata,"metadata");
                    $scope.observer.subscribeItems($scope.observer_subscribe_fulltext,"fulltext");

                    $scope.clickedItem = function(category, type, key) {
                        /*
                         $scope.filters = [];
                         $scope.filterItems.forEach(function(x) {
                         $scope.filters.push(x.data.id);
                         });
                         $scope.filters.push($scope.ids[x][$scope.labels[x].indexOf(this.category)]);
                         */
                        var id = -1;
                        if(type == 'entity')
                            id = $scope.ids[key][$scope.labels[key].indexOf(category.category)];
                        $scope.observer.addItem({
                            type: type,
                            data: {
                                id: id,
                                name: category.category,
                                type: key
                            }
                        });
                    };

                    $scope.updateEntityCharts = function () {
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
                        var fulltext = [];
                        angular.forEach($scope.fulltextFilters, function(item) {
                            fulltext.push(item.data.name);
                        });
                        var entityType = "";
                        angular.forEach($scope.entityTypes, function(type) {
                            $scope.metaCharts[type].showLoading('Loading ...');
                            var instances = $scope.ids[type];
                            playRoutes.controllers.EntityController.getEntities(fulltext,facets,entities,$scope.observer.getTimeRange(),50,entityType,instances).get().then(
                                function(result) {
                                    //result.data[type].forEach(function(x) {
                                    //    console.log(x.key + ": " + x.count);
                                    //});
                                    var data = [];
                                    angular.forEach(result.data, function(x) {
                                        if(x.docCount <= 0)
                                            data.push(null);
                                        else
                                            data.push(x.docCount);
                                    });
                                    var newBase = [];
                                    $.each($scope.chartConfigs[type].series[0].data, function(index, value) {
                                        if(data[index] == undefined)
                                            newBase.push($scope.chartConfigs[type].series[0].data[index]);
                                        else
                                            newBase.push($scope.chartConfigs[type].series[0].data[index] - data[index]);
                                    });
                                    //$scope.metaCharts[type].series[0].setData(newBase);
                                    if($scope.metaCharts[type].series[1] == undefined) {
                                        $scope.metaCharts[type].addSeries({
                                            name: 'Filter',
                                            data: data,
                                            color: 'black',
                                            cursor: 'pointer',
                                            point: {
                                                events: {
                                                    click: function () {
                                                        $scope.clickedItem(this, 'entity', type);
                                                    }
                                                }
                                            },
                                            dataLabels: {
                                                inside: true,
                                                align: 'left',
                                                useHTML: true,
                                                formatter : function() {
                                                    return $('<div/>').css({
                                                        'color' : 'white'
                                                    }).text(this.y)[0].outerHTML;
                                                }
                                            }
                                        });
                                    } else {
                                        $scope.metaCharts[type].series[1].setData(data);
                                    }
                                    $scope.metaCharts[type].hideLoading();
                                }
                            );
                        });
                        //TODO: on adding fulltext filter doc count grows

                    };

                    $scope.updateMetadataCharts = function() {
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
                        var fulltext = [];
                        angular.forEach($scope.fulltextFilters, function(item) {
                            fulltext.push(item.data.name);
                        });
                        angular.forEach($scope.metadataTypes, function(type) {
                            $scope.metaCharts[type].showLoading('Loading ...');
                            var instances = $scope.chartConfigs[type].xAxis["categories"];
                            playRoutes.controllers.MetadataController.getSpecificMetadata(fulltext,type,facets,entities,instances,$scope.observer.getTimeRange()).get().then(
                                function(result) {
                                    //result.data[type].forEach(function(x) {
                                    //    console.log(x.key + ": " + x.count);
                                    //});
                                    var data = [];
                                    angular.forEach(result.data[type], function(x) {
                                        if(x.count <= 0)
                                            data.push(null);
                                        else
                                            data.push(x.count);
                                    });
                                    var newBase = [];
                                    $.each($scope.chartConfigs[type].series[0].data, function(index, value) {
                                        if(data[index] == undefined)
                                            newBase.push($scope.chartConfigs[type].series[0].data[index]);
                                        else
                                            newBase.push($scope.chartConfigs[type].series[0].data[index] - data[index]);
                                    });
                                    //$scope.metaCharts[type].series[0].setData(newBase);
                                    if($scope.metaCharts[type].series[1] == undefined) {
                                        $scope.metaCharts[type].addSeries({
                                            name: 'Filter',
                                            data: data,
                                            color: 'black',
                                            cursor: 'pointer',
                                            point: {
                                                events: {
                                                    click: function () {
                                                        $scope.clickedItem(this, 'metadata', type);
                                                    }
                                                }
                                            },
                                            dataLabels: {
                                                inside: true,
                                                align: 'left',
                                                useHTML: true,
                                                formatter : function() {
                                                    return $('<div/>').css({
                                                        'color' : 'white'
                                                    }).text(this.y)[0].outerHTML;
                                                }
                                            }
                                        });
                                    } else {
                                        $scope.metaCharts[type].series[1].setData(data);
                                    }
                                    $scope.metaCharts[type].hideLoading();
                                }
                            );
                        });
                    };

                    $scope.initEntityCharts = function () {
                        var facets = [{'key':'dummy','data': []}];
                        var entities = [];
                        var fulltext = [];
                        var timeRange = "";

                        $scope.observer.getEntityTypes().then(function(types) {
                            types.forEach(function (x) {
                                $scope.chartConfigs[x] = angular.copy($scope.chartConfig);
                                playRoutes.controllers.EntityController.getEntities(fulltext,facets,entities, timeRange,50,x).get().then(function(result) {

                                    $scope.frequencies[x] = [];
                                    $scope.labels[x] = [];
                                    $scope.ids[x] = [];
                                    result.data.forEach(function (entity) {
                                        $scope.frequencies[x].push(entity.docCount);
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
                                                    $scope.clickedItem(this, 'entity', x);
                                                }
                                            }
                                        }
                                    },{
                                        name: 'Filter',
                                        data: $scope.frequencies[x],
                                        color: 'black',
                                        cursor: 'pointer',
                                        point: {
                                            events: {
                                                click: function () {
                                                    $scope.clickedItem(this, 'entity', x);
                                                }
                                            }
                                        },
                                        dataLabels: {
                                            inside: true,
                                            align: 'left',
                                            useHTML: true,
                                            formatter : function() {
                                                return $('<div/>').css({
                                                    'color' : 'white'
                                                }).text(this.y)[0].outerHTML;
                                            }
                                        }
                                    }];
                                    $scope.chartConfigs[x].chart.renderTo = "chart_" + x.toLowerCase();
                                    $("#chart_" + x.toLowerCase()).css("height",$scope.frequencies[x].length * 35);
                                    $scope.metaCharts[x] = new Highcharts.Chart($scope.chartConfigs[x]);
                                });
                            });
                        });
                    };

                    $scope.initMetadataCharts = function () {
                        $scope.observer.getMetadataTypes().then(function() {
                                playRoutes.controllers.MetadataController.getMetadata(undefined,[{'key':'dummy','data': []}]).get().then(
                                    function (result) {

                                        $.each(result.data,function() {
                                            $.each(this, function(key, value) {
                                                //console.log(value);
                                            if($scope.metadataTypes.indexOf(key) != -1) {

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
                                                        $scope.clickedItem(this, 'metadata', key);
                                                    }
                                                }
                                            }

                                        },{
                                            name: 'Filter',
                                            data: $scope.frequencies[key],
                                            color: 'black',
                                            cursor: 'pointer',
                                            point: {
                                                events: {
                                                    click: function () {
                                                        $scope.clickedItem(this, 'metadata', key);
                                                    }
                                                }
                                            },
                                            dataLabels: {
                                                inside: true,
                                                align: 'left',
                                                useHTML: true,
                                                formatter : function() {
                                                    return $('<div/>').css({
                                                        'color' : 'white'
                                                    }).text(this.y)[0].outerHTML;
                                                }
                                            }
                                        }];
                                        $scope.chartConfigs[key].chart.renderTo = "chart_" + key.toLowerCase();
                                        $("#chart_" + key.toLowerCase()).css("height",$scope.frequencies[key].length * 35);
                                        $scope.metaCharts[key] = new Highcharts.Chart($scope.chartConfigs[key]);
                                            }

                                            });

                                        }
                                );

                                    });
                            $scope.initialized = true;
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
                    $scope.reflow = function() {
                        $timeout(function() {
                            $("#metadata-view .active .active .meta-chart").highcharts().reflow();
                        }, 100);
                    };

                    $('#metadata-view .nav-tabs a').on('shown.bs.tab', function(event){
                        $("#metadata-view .active .active .meta-chart").highcharts().reflow();
                    });




                    /** entry point here **/
                    $scope.metaShareService = metaShareService;
                    $scope.sourceShareService = sourceShareService;


                    //TODO: calc height on bar count -> scroll bar
                    $scope.tabHeight = $("#metadata").height() - 100;
                    //console.log($scope.tabHeight);
                    //console.log(sourceShareService);
                    //console.log(filterShareService);

                }
            ]
        );
});