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
    'angularMoment',
    'jquery-json',
    'ngFileSaver',
    'ngMaterial'
], function (angular) {
    'use strict';

    angular.module("myApp.metadata", ['play.routing', 'angularMoment', 'ngMaterial']);
    angular.module("myApp.metadata")
        .controller('MetadataController',
            [
                '$scope',
                '$timeout',
                '$q',
                'playRoutes',
                'metaShareService',
                'sourceShareService',
                'ObserverService',
                function ($scope, $timeout, $q, playRoutes, metaShareService, sourceShareService, ObserverService) {


                    $scope.chartConfig = metaShareService.chartConfig;
                    $scope.observer = ObserverService;


                    /**
                     * subscribe entity and metadata filters
                     */
                    $scope.observer_subscribe_entity = function (items) {
                        $scope.entityFilters = items
                    };
                    $scope.observer_subscribe_metadata = function (items) {
                        $scope.metadataFilters = items
                    };
                    $scope.observer_subscribe_fulltext = function (items) {
                        $scope.fulltextFilters = items
                    };
                    $scope.observer.subscribeItems($scope.observer_subscribe_entity, "entity");
                    $scope.observer.subscribeItems($scope.observer_subscribe_metadata, "metadata");
                    $scope.observer.subscribeItems($scope.observer_subscribe_fulltext, "fulltext");

                    $scope.updateHeight = function() {
                        $(".scroll-chart").css("height",$("#metadata").height()-150);
                    };

                    $scope.initMetadataView = function () {
                        $scope.initializedMeta = false;
                        $scope.initializedEntity = false;
                        $scope.promiseMetaCharts = undefined;
                        $scope.promiseEntityCharts = undefined;

                        $scope.frequencies = [];
                        $scope.labels = [];
                        $scope.ids = [];
                        $scope.chartConfigs = [];
                        $scope.metaCharts = [];

                        $timeout($scope.updateHeight(), 1000) ;
                        var defer1 = $q.defer();
                        var defer2 = $q.defer();
                        var prom = $q.all([defer1.promise, defer2.promise]);
                        $scope.observer.getEntityTypes().then(function (types) {
                            $scope.entityTypes = types;
                            $scope.initEntityCharts().then(function() {
                                defer1.resolve("initEntity");
                            });
                        });
                        $scope.observer.getMetadataTypes().then(function (types) {
                            $scope.metadataTypes = types;
                            $scope.initMetadataCharts().then(function() {
                                defer2.resolve("initMeta");
                            });
                        });
                        return prom;
                    };

                    $scope.observer.subscribeReset($scope.initMetadataView);

                    $scope.clickedItem = function (category, type, key) {
                        /*
                         $scope.filters = [];
                         $scope.filterItems.forEach(function(x) {
                         $scope.filters.push(x.data.id);
                         });
                         $scope.filters.push($scope.ids[x][$scope.labels[x].indexOf(this.category)]);
                         */
                        var id = -1;
                        if (type == 'entity')
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
                        if ($scope.initializedEntity) {
                            $scope.promiseEntityCharts =  $q.defer();
                            var entities = [];
                            angular.forEach($scope.entityFilters, function (item) {
                                entities.push(item.data.id);
                            });
                            var facets = $scope.observer.getFacets();
                            var fulltext = [];
                            angular.forEach($scope.fulltextFilters, function (item) {
                                fulltext.push(item.data.name);
                            });
                            var entityType = "";
                            $scope.entityPromises = [];
                            $scope.entityPromisesLocal = [];
                            angular.forEach($scope.entityTypes, function (type) {
                                if($scope.metaCharts[type])
                                     $scope.metaCharts[type].showLoading('Loading ...');
                                var instances = $scope.ids[type];
                                var promise = $q.defer();
                                $scope.entityPromisesLocal[type] = promise;
                                $scope.entityPromises.push(promise.promise);
                                playRoutes.controllers.EntityController.getEntities(fulltext, facets, entities, $scope.observer.getTimeRange(), 50, entityType, instances).get().then(
                                    function (result) {
                                        //result.data[type].forEach(function(x) {
                                        //    console.log(x.key + ": " + x.count);
                                        //});
                                        var data = [];
                                        angular.forEach(result.data, function (x) {
                                            if (x.docCount <= 0)
                                                data.push(null);
                                            else
                                                data.push(x.docCount);
                                        });
                                        var newBase = [];
                                        $.each($scope.chartConfigs[type].series[0].data, function (index, value) {
                                            if (data[index] == undefined)
                                                newBase.push($scope.chartConfigs[type].series[0].data[index]);
                                            else
                                                newBase.push($scope.chartConfigs[type].series[0].data[index] - data[index]);
                                        });
                                        //$scope.metaCharts[type].series[0].setData(newBase);
                                        if ($scope.metaCharts[type].series[1] == undefined) {
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
                                                    formatter: function () {
                                                        return $('<div/>').css({
                                                            'color': 'white'
                                                        }).text(this.y)[0].outerHTML;
                                                    }
                                                }
                                            });
                                        } else {
                                            $scope.metaCharts[type].series[1].setData(data);
                                        }
                                        $scope.metaCharts[type].hideLoading();
                                        $scope.entityPromisesLocal[type].resolve("suc: " + type);
                                    }
                                );
                            });
                            //TODO: on adding fulltext filter doc count grows
                        }
                    };

                    $scope.updateMetadataCharts = function () {
                        if ($scope.initializedMeta) {

                            var entities = [];
                            angular.forEach($scope.entityFilters, function (item) {
                                entities.push(item.data.id);
                            });
                            var facets = $scope.observer.getFacets();
                            var fulltext = [];
                            angular.forEach($scope.fulltextFilters, function (item) {
                                fulltext.push(item.data.name);
                            });
                            $scope.metaPromises = [];
                            $scope.metaPromisesLocal = [];
                            angular.forEach($scope.metadataTypes, function (type) {
                                var promise = $q.defer();
                                $scope.metaPromisesLocal[type] = promise;
                                $scope.metaPromises.push(promise.promise);
                                //console.log(type);
                                $scope.metaCharts[type].showLoading('Loading ...');
                                var instances = $scope.chartConfigs[type].xAxis["categories"];
                                playRoutes.controllers.MetadataController.getSpecificMetadata(fulltext, type, facets, entities, instances, $scope.observer.getTimeRange()).get().then(
                                    function (result) {
                                        //result.data[type].forEach(function(x) {
                                        //    console.log(x.key + ": " + x.count);
                                        //});
                                        var data = [];
                                        angular.forEach(result.data[type], function (x) {
                                            if (x.count <= 0)
                                                data.push(null);
                                            else
                                                data.push(x.count);
                                        });
                                        var newBase = [];
                                        $.each($scope.chartConfigs[type].series[0].data, function (index, value) {
                                            if (data[index] == undefined)
                                                newBase.push($scope.chartConfigs[type].series[0].data[index]);
                                            else
                                                newBase.push($scope.chartConfigs[type].series[0].data[index] - data[index]);
                                        });
                                        //$scope.metaCharts[type].series[0].setData(newBase);
                                        if ($scope.metaCharts[type].series[1] == undefined) {
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
                                                    formatter: function () {
                                                        return $('<div/>').css({
                                                            'color': 'white'
                                                        }).text(this.y)[0].outerHTML;
                                                    }
                                                }
                                            });
                                        } else {
                                            $scope.metaCharts[type].series[1].setData(data);
                                        }
                                        $scope.metaCharts[type].hideLoading();
                                        $scope.metaPromisesLocal[type].resolve("suc: " + type);
                                    }
                                );
                            });
                            return $scope.metaPromises;
                        }

                    };

                    $scope.initEntityCharts = function () {
                        var facets = [{'key': 'dummy', 'data': []}];
                        var entities = [];
                        var fulltext = [];
                        var timeRange = "";
                        var deferred = [];
                        var proms = [];
                        $scope.observer.getEntityTypes().then(function (types) {
                            types.forEach(function (x) {
                                deferred[x] = $q.defer();
                                proms.push(deferred[x].promise);
                                $scope.chartConfigs[x] = angular.copy($scope.chartConfig);
                                playRoutes.controllers.EntityController.getEntities(fulltext, facets, entities, timeRange, 50, x).get().then(function (result) {

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
                                    }, {
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
                                            formatter: function () {
                                                return $('<div/>').css({
                                                    'color': 'white'
                                                }).text(this.y)[0].outerHTML;
                                            }
                                        }
                                    }];
                                    $scope.chartConfigs[x].chart.renderTo = "chart_" + x.toLowerCase();
                                    $("#chart_" + x.toLowerCase()).css("height", $scope.frequencies[x].length * 35);
                                    $scope.metaCharts[x] = new Highcharts.Chart($scope.chartConfigs[x]);
                                    deferred[x].resolve(x);
                                });
                            });
                            $scope.initializedEntity = true;

                        });
                        return $q.all(proms);
                    };

                    $scope.initMetadataCharts = function () {
                        var defer = $q.defer();
                        $scope.observer.getMetadataTypes().then(function () {
                            playRoutes.controllers.MetadataController.getMetadata(undefined, [{
                                'key': 'dummy',
                                'data': []
                            }]).get().then(
                                function (result) {

                                    $.each(result.data, function () {
                                            $.each(this, function (key, value) {
                                                //console.log(value);
                                                if ($scope.metadataTypes.indexOf(key) != -1) {

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

                                                    }, {
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
                                                            formatter: function () {
                                                                return $('<div/>').css({
                                                                    'color': 'white'
                                                                }).text(this.y)[0].outerHTML;
                                                            }
                                                        }
                                                    }];
                                                    $scope.chartConfigs[key].chart.renderTo = "chart_" + key.toLowerCase();
                                                    $("#chart_" + key.toLowerCase()).css("height", $scope.frequencies[key].length * 35);
                                                    $scope.metaCharts[key] = new Highcharts.Chart($scope.chartConfigs[key]);
                                                }

                                            });

                                        }
                                    );
                                    defer.resolve("metainit");
                                });
                            $scope.initializedMeta = true;
                        });
                        return defer.promise;
                    };

                    $scope.updateMetadataView = function () {
                        $scope.updateEntityCharts();
                        $scope.updateMetadataCharts();
                        $scope.promise = $q.all($scope.entityPromises.concat($scope.metaPromises));
                        return $scope.promise;
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

                    $scope.reflow = function (type) {
                        $timeout(function () {
                            if ($("#chart_" + type.toLowerCase()).highcharts())
                                $("#chart_" + type.toLowerCase()).highcharts().reflow();
                        }, 100);
                    };
                    $scope.reflow = function () {
                        $timeout(function () {
                            if ($("#metadata-view .active .active .meta-chart").highcharts())
                                $("#metadata-view .active .active .meta-chart").highcharts().reflow();
                        }, 100);
                    };

                    $('#metadata-view .nav-tabs a').on('shown.bs.tab', function (event) {
                        if ($("#metadata-view .active .active .meta-chart").highcharts())
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