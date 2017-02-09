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
    'jquery-json',
    'ngFileSaver',
    'ngMaterial'
], function (angular) {
    'use strict';

    /**
     * metadata module:
     * visualization of metadata and entity bar charts
     * interaction:
     * - add entity/metadata key as filter
     */
    angular.module("myApp.metadata", ['play.routing', 'ngMaterial']);
    angular.module("myApp.metadata")
        .controller('MetadataController',
            [
                '$scope',
                '$timeout',
                '$q',
                'playRoutes',
                'historyFactory',
                'graphProperties',
                'metaShareService',
                'sourceShareService',
                'ObserverService',
                '_',
                function ($scope, $timeout, $q, playRoutes, historyFactory, graphProperties, metaShareService, sourceShareService, ObserverService, _) {

                    /*(function (H, $) {
                        var fireEvent = H.fireEvent;

                        H.wrap(H.Pointer.prototype, 'init', function (proceed) {
                            proceed.apply(this, Array.prototype.slice.call(arguments, 1));

                            var pointer = this,
                                container = pointer.chart.container,
                                DELAY = 500, clicks = 0, timer = null;

                            container.oncontextmenu = function (e) {
                                pointer.onContainerContextMenu(e);
                                e.stopPropagation();
                                e.preventDefault();
                                return false;
                            };

                            container.onwheel = function (e) {
                                pointer.onWheel(e);
                                e.stopPropagation();
                                e.preventDefault();
                            };

                            // Override default click event handler by adding delay to handle double-click event
                            container.onclick = function (e) {
                                clicks++;
                                if(clicks === 1) {
                                    timer = setTimeout(function() {
                                        pointer.onContainerClick(e);
                                        clicks = 0;
                                    }, DELAY);
                                } else {
                                    clearTimeout(timer);
                                    clicks = 0;
                                }
                            };

                            container.ondblclick = function (e) {
                                pointer.onDblClick(e);
                                clicks = 0;
                            };
                        });

                        if(!H.Pointer.prototype.hasOwnProperty('onContainerContextMenu')) {
                            H.Pointer.prototype.onContainerContextMenu = function (e) {
                                var pointer = this,
                                    chart = pointer.chart,
                                    hoverPoint = chart.hoverPoint,
                                    plotLeft = chart.plotLeft,
                                    plotTop = chart.plotTop;

                                e = this.normalize(e);

                                if (!chart.cancelClick) {
                                    // On tracker click, fire the series and point events. #783, #1583
                                    if (hoverPoint && this.inClass(e.target, 'tracker')) {

                                        // the series click event
                                        fireEvent(hoverPoint.series, 'contextmenu', $.extend(e, {
                                            point: hoverPoint
                                        }));

                                        // the point click event
                                        if (chart.hoverPoint) {
                                            hoverPoint.firePointEvent('contextmenu', e);
                                        }
                                    } else {
                                        $.extend(e, this.getCoordinates(e));
                                        if (chart.isInsidePlot(e.chartX - plotLeft, e.chartY - plotTop)) {
                                            fireEvent(chart, 'contextmenu', e);
                                        }
                                    }
                                }
                            };
                        }

                        if(!H.Pointer.prototype.hasOwnProperty('onWheel')) {
                            H.Pointer.prototype.onWheel = function (e) {
                                var pointer = this,
                                    chart = pointer.chart,
                                    plotLeft = chart.plotLeft,
                                    plotTop = chart.plotTop;

                                e = this.normalize(e);

                                $.extend(e, this.getCoordinates(e));
                                if (chart.isInsidePlot(e.chartX - plotLeft, e.chartY - plotTop)) {
                                    fireEvent(chart, 'wheel', e);
                                }
                            };
                        }

                        var applyClickEventHandler = function( eventtype, propertyName ) {
                            if(!H.Pointer.prototype.hasOwnProperty(propertyName)) {
                                H.Pointer.prototype[propertyName] = function (e) {
                                    var pointer = this,
                                        chart = pointer.chart,
                                        hoverPoint = chart.hoverPoint,
                                        plotLeft = chart.plotLeft,
                                        plotTop = chart.plotTop;

                                    e = this.normalize(e);

                                    if (!chart.cancelClick) {
                                        // On tracker click, fire the series and point events. #783, #1583
                                        if (hoverPoint && this.inClass(e.target, 'tracker')) {

                                            // the series click event
                                            fireEvent(hoverPoint.series, eventtype, $.extend(e, {
                                                point: hoverPoint
                                            }));

                                            // the point click event
                                            if (chart.hoverPoint) {
                                                hoverPoint.firePointEvent(eventtype, e);
                                            }
                                        } else {
                                            $.extend(e, this.getCoordinates(e));
                                            if (chart.isInsidePlot(e.chartX - plotLeft, e.chartY - plotTop)) {
                                                fireEvent(chart, eventtype, e);
                                            }
                                        }
                                    }
                                };
                            }
                        };

                        applyClickEventHandler('contextmenu', 'onContainerContextMenu');

                        applyClickEventHandler('dblclick', 'onDblClick');


                    }(Highcharts, jQuery));*/


                    $scope.chartConfig = metaShareService.chartConfig;
                    $scope.historyFactory = historyFactory;
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

                    //resize height of visible bars in 'metadata' view
                    $scope.updateHeight = function() {
                        $(".scroll-chart").css("height",$("#metadata").height()-150);
                    };

                    $scope.emptyFacets = [{'key':'dummy','data': []}];

                    // order by black bars (default = true)
                    $scope.reorder = true;

                    $scope.initMetadataView = function () {
                        $scope.initializedMeta = false;
                        $scope.initializedEntity = false;

                        $scope.promiseMetaCharts = undefined;
                        $scope.promiseEntityCharts = undefined;

                        $scope.chartConfigs = [];
                        $scope.metaCharts = [];

                        $scope.metaData = [];
                        $scope.entityData = [];

                        var defer1 = $q.defer();
                        var defer2 = $q.defer();
                        var prom = $q.all([defer1.promise, defer2.promise]);
                        $scope.observer.getEntityTypes().then(function (types) {
                            $scope.entityTypes = types.map(function(t) { return t.name; });
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
                        prom.then($scope.updateHeight);
                        return prom;
                    };

                    $scope.observer.subscribeReset($scope.initMetadataView);

                    $scope.clickedItem = function (category, type, key) {
                        var id = -1;
                        if (type == 'entity')
                            id = category.id;
                        $scope.observer.addItem({
                            type: type,
                            data: {
                                id: id,
                                description: category.name,
                                item: category.name,
                                type: key
                            }
                        });
                    };

                    /*$scope.contextMenu = function (category, e, type) {
                        var posx = e.clientX + window.pageXOffset + 'px'; //Left Position of Mouse Pointer
                        var posy = e.clientY + window.pageYOffset + 'px'; //Top Position of Mouse Pointer
                        $('#constext-menu-div').css({top: posy , left: posx });
                        $('#constext-menu-div').show();
                        $scope.contextItem = {
                            name: category.category,
                            type: type
                        };
                    };

                    $scope.closeContextMenu = function() {
                        $('#constext-menu-div').hide();
                    };

                    $('#constext-menu-div ul li').click(function() {
                        $scope.closeContextMenu();
                        console.log($(this).attr("action"));
                        $scope.contextItem.id = $scope.ids[$scope.contextItem.type][$scope.labels[$scope.contextItem.type].indexOf($scope.contextItem.name)];
                        console.log( $scope.contextItem);
                        switch($(this).attr("action")) {
                            case 'filter':
                                $scope.observer.addItem({
                                    type: 'entity',
                                    data: {
                                        id: $scope.contextItem.id,
                                        name: $scope.contextItem.name,
                                        type: $scope.contextItem.type
                                    }
                                });
                                break;
                        }

                    });*/

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
                                fulltext.push(item.data.item);
                            });
                            var entityType = "";
                            var timeRange = $scope.observer.getTimeRange();
                            var timeRangeX = $scope.observer.getXTimeRange();
                            $scope.entityPromises = [];
                            $scope.entityPromisesLocal = [];
                            angular.forEach($scope.entityTypes, function (type) {
                                if($scope.metaCharts[type])
                                     $scope.metaCharts[type].showLoading('Loading ...');

                                var promise = $q.defer();
                                $scope.entityPromisesLocal[type] = promise;
                                $scope.entityPromises.push(promise.promise);
                                var defReorder = $q.defer();
                                var instances = [];

                                if($scope.reorder) {
                                    playRoutes.controllers.EntityController.getEntitiesByType(fulltext, facets, entities, timeRange ,timeRangeX, 50, type).get().then(
                                        function (result) {
                                            var data = [];
                                            angular.forEach(result.data, function (x) {
                                                if (x.docCount <= 0)
                                                    data.push({
                                                        y: null,
                                                        name: x.name,
                                                        id: x.id
                                                    });
                                                else
                                                    data.push({
                                                        y: x.docCount,
                                                        name: x.name,
                                                        id: x.id
                                                    });
                                            });
                                            $scope.metaCharts[type].xAxis[0].setCategories(_.pluck(data, 'name'));
                                            $scope.metaCharts[type].series[1].setData(data);
                                            $scope.metaCharts[type].series[1].data.forEach(function(e) {
                                                instances.push(e.id);
                                            });
                                            fulltext = [];
                                            facets = $scope.emptyFacets;
                                            entities = [];
                                            timeRange = "";
                                            timeRangeX = "";
                                            defReorder.resolve("reorder");
                                        });
                                } else {
                                    $scope.metaCharts[type].series[0].data.forEach(function(e) {
                                        instances.push(e.id);
                                    });
                                    defReorder.resolve("reorder");
                                }
                                defReorder.promise.then(function() {
                                playRoutes.controllers.EntityController.getEntitiesByType(fulltext, facets, entities, timeRange ,timeRangeX, 50, type, instances).get().then(
                                    function (result) {
                                        var data = [];
                                        angular.forEach(result.data, function (x) {
                                            if (x.docCount <= 0)
                                                data.push({
                                                    y: null,
                                                    name: x.name,
                                                    id: x.id
                                                });
                                            else
                                                data.push({
                                                    y: x.docCount,
                                                    name: x.name,
                                                    id: x.id
                                                });
                                        });
                                        if($scope.reorder)
                                            $scope.metaCharts[type].series[0].setData(data);
                                        else
                                            $scope.metaCharts[type].series[1].setData(data);

                                        $scope.metaCharts[type].hideLoading();
                                        $scope.entityPromisesLocal[type].resolve("suc: " + type);
                                    }
                                );
                                });

                            });
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
                            var timeRange = $scope.observer.getTimeRange();
                            var timeRangeX = $scope.observer.getXTimeRange();
                            angular.forEach($scope.fulltextFilters, function (item) {
                                fulltext.push(item.data.item);
                            });
                            $scope.metaPromises = [];
                            $scope.metaPromisesLocal = [];
                            angular.forEach($scope.metadataTypes, function (type) {
                                var promise = $q.defer();
                                $scope.metaPromisesLocal[type] = promise;
                                $scope.metaPromises.push(promise.promise);
                                //console.log(type);
                                $scope.metaCharts[type].showLoading('Loading ...');
                                var instances = [];
                                var defReorder = $q.defer();
                                if($scope.reorder) {
                                    playRoutes.controllers.MetadataController.getSpecificMetadata(fulltext, type.replace(".","_"), facets, entities, undefined, timeRange,timeRangeX).get().then(
                                        function (result) {
                                            var data = [];
                                            angular.forEach(result.data[type.replace(".","_")], function (x) {
                                                if (x.count <= 0)
                                                    data.push({
                                                        y: null,
                                                        name: x.key
                                                    });
                                                else
                                                    data.push({
                                                        y: x.count,
                                                        name: x.key
                                                    });
                                            });
                                            $scope.metaCharts[type].series[1].setData(data);
                                            $scope.metaCharts[type].xAxis[0].setCategories(_.pluck(data, 'name'));
                                            $scope.chartConfigs[type].series[1].data.forEach(function(m) {
                                                instances.push(m.name);
                                            });
                                            fulltext = [];
                                            facets = $scope.emptyFacets;
                                            entities = [];
                                            timeRange = "";
                                            timeRangeX = "";
                                            defReorder.resolve("suc");
                                        });
                                } else {
                                    $scope.chartConfigs[type].series[0].data.forEach(function(m) {
                                        instances.push(m.name);
                                    });
                                    defReorder.resolve("suc");
                                }
                               defReorder.promise.then(function() {

                                playRoutes.controllers.MetadataController.getSpecificMetadata(fulltext, type.replace(".","_"), facets, entities, instances, timeRange,timeRangeX).get().then(
                                    function (result) {
                                        var data = [];
                                        angular.forEach(result.data[type.replace(".","_")], function (x) {
                                            if (x.count <= 0)
                                                data.push({
                                                    y: null,
                                                    name: x.key
                                                });
                                            else
                                                data.push({
                                                    y: x.count,
                                                    name: x.key
                                                });
                                        });
                                        if($scope.reorder)
                                            $scope.metaCharts[type].series[0].setData(data);
                                        else
                                            $scope.metaCharts[type].series[1].setData(data);
                                        $scope.metaCharts[type].hideLoading();
                                        $scope.metaPromisesLocal[type].resolve("suc: " + type);
                                    }
                                );
                               });

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
                            types.forEach(function (t) {
                                deferred[t.name] = $q.defer();
                                proms.push(deferred[t.name].promise);
                                $scope.chartConfigs[t.name] = angular.copy($scope.chartConfig);
                                playRoutes.controllers.EntityController.getEntitiesByType(fulltext, facets, entities, timeRange, timeRange, 50, t.name).get().then(function (result) {
                                    $scope.entityData[t.name] = [];
                                    result.data.forEach(function (entity) {
                                        $scope.entityData[t.name].push({
                                            y: entity.docCount,
                                            name: entity.name,
                                            id: entity.id
                                        });
                                    });

                                    $scope.chartConfigs[t.name]["xAxis"]["categories"] = _.pluck($scope.entityData[t.name], 'name');
                                    $scope.chartConfigs[t.name]["series"] = [{
                                        name: 'Total',
                                        data: $scope.entityData[t.name],
                                        cursor: 'pointer',
                                        point: {
                                            events: {
                                                click: function () {
                                                    $scope.clickedItem(this, 'entity', t.name);
                                                },
                                                contextmenu: function (e) {
                                                    $scope.contextMenu(this, e, t.name);
                                                }
                                            }
                                        }
                                    }, {
                                        name: 'Filter',
                                        data: $scope.entityData[t.name],
                                        color: 'black',
                                        cursor: 'pointer',
                                        point: {
                                            events: {
                                                click: function () {
                                                    $scope.clickedItem(this, 'entity', t.name);
                                                },
                                                contextmenu: function (e) {
                                                    $scope.contextMenu(this, e, t.name);
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
                                    $scope.chartConfigs[t.name].chart.renderTo = "chart_" + t.name.toLowerCase();
                                    // Set background color according to the represented entity type
                                    var color = graphProperties.options['groups'][t.id]['color']['background'];
                                    //$scope.chartConfigs[t.name].chart.backgroundColor = graphProperties.convertHex(color, 55);

                                    $("#chart_" + t.name.toLowerCase()).css("height", $scope.entityData[t.name].length * 35);
                                    $scope.metaCharts[t.name] = new Highcharts.Chart($scope.chartConfigs[t.name]);
                                    deferred[t.name].resolve(t.name);
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
                                            $.each(this, function (key2, value) {
                                                //console.log(value);
                                                var key = key2.replace("_",".");
                                                if ($scope.metadataTypes.indexOf(key) != -1) {

                                                    $scope.chartConfigs[key] = angular.copy($scope.chartConfig);
                                                    $scope.metaData[key] = [];
                                                    value.forEach(function (metadata) {
                                                        $scope.metaData[key].push({
                                                            y: metadata.count,
                                                            name: metadata.key
                                                        });
                                                    });

                                                    $scope.chartConfigs[key]["xAxis"]["categories"] = _.pluck($scope.metaData[key], 'name');
                                                    $scope.chartConfigs[key]["series"] = [{
                                                        name: 'Total',
                                                        data: $scope.metaData[key],
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
                                                        data: $scope.metaData[key],
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
                                                    $scope.chartConfigs[key].chart.renderTo = "chart_" + key.toLowerCase().replace(".","_");
                                                    $("#chart_" + key.toLowerCase().replace(".","_")).css("height", $scope.metaData[key].length * 35);
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

                    $scope.resetMetadataView = function() {
                        $scope.initMetadataView().then(function() {
                            $scope.updateMetadataView();

                        });
                    };
                    $scope.updateMetadataView = function () {
                        $scope.updateEntityCharts();
                        $scope.updateMetadataCharts();
                        $scope.promise = $q.all($scope.entityPromises.concat($scope.metaPromises));
                        return $scope.promise;
                    };


                    $scope.observer.registerObserverCallback({ priority: 10, callback: $scope.updateMetadataView });
                    //load another 50 entities for specific metadata
                    $scope.loadMore = function (ele) {
                        if (ele.mcs.top != 0) {
                            var type = $(ele).find(".meta-chart").attr("id");
                            console.log('load more metadata: ' + type);

                        }
                    };

                    $scope.initMetadataView();

                    // TODO: reflow function were needed with bootstrap tabs. On tab switch highcharts bar charts had to reflowed.
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

                    /*$scope.onContext = function(params) {
                        params.event.preventDefault();
                        closeContextMenu();

                        var position = { x: params.pointer.DOM.x, y: params.pointer.DOM.y };
                        var nodeIdOpt = self.network.getNodeAt(position);
                        var edgeIdOpt = self.network.getEdgeAt(position);

                        // Node selected
                        if(!_.isUndefined(nodeIdOpt)) {
                            self.network.selectNodes([nodeIdOpt]);
                            showContextMenu(_.extend(position, { id: nodeIdOpt }), self.nodeMenu);
                        } else if(!_.isUndefined(edgeIdOpt)) {
                            self.network.selectEdges([edgeIdOpt]);
                            showContextMenu(_.extend(position, { id: edgeIdOpt }), self.edgeMenu);
                        }
                        else {
                            // Nop
                        }
                    };

                    $scope.showContextMenu = function(params, menu) {
                        var container = document.getElementById('mynetwork');

                        var offsetLeft = container.offsetLeft;
                        var offsetTop = container.offsetTop;

                        self.popupMenu = document.createElement("div");
                        self.popupMenu.className = 'popupMenu';
                        self.popupMenu.style.left = params.x - offsetLeft + 'px';
                        self.popupMenu.style.top =  params.y - offsetTop +'px';

                        var ul = document.createElement('ul');
                        self.popupMenu.appendChild(ul);

                        for (var i = 0; i < menu.length; i++) {
                            var li = document.createElement('li');
                            ul.appendChild(li);
                            li.innerHTML = li.innerHTML + menu[i].title;
                            (function(value, id, action){
                                li.addEventListener("click", function() {
                                    closeContextMenu();
                                    action(value, id);
                                }, false);})(menu[i].title, params.id, menu[i].action);
                        }
                        container.appendChild(self.popupMenu);
                    };*/

                }
            ]
        );
});