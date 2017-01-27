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
    'ngMaterial',
    'ngVis'
], function(angular) {
    'use strict';
    /**
     * network module:
     * visualization and interaction of network graph
     */
    angular.module('myApp.network', ['ngMaterial', 'ngVis', 'play.routing']);
    angular.module('myApp.network')
        // Network Legend Controller
        .controller('LegendController', ['$scope', '$timeout', 'VisDataSet', 'graphProperties', function ($scope, $timeout, VisDataSet, graphProperties) {

            $scope.legendNodes = new VisDataSet([]);
            this.legendOptions = graphProperties.legendOptions;
            this.legendData = { nodes: $scope.legendNodes, edges: [] };
            this.legendEvents = { "onload": function(network) { $scope.legendNetwork = network; } };

            // See style.css 'legend' for width and height of the render area
            $scope.addLegend = function(types) {
                var x = 0; var y = 0; var distance = 100;

                var nodes = types.map(function(t, i) {
                    return { id: -(i+1), x: x + i * distance, y: y, label: t.name, group: t.id, value: 1, fixed: true, physics: false };
                });
                $scope.legendNodes.add(nodes);
                $scope.legendNetwork.fit();
            };

            // Add nodes after the legend div is added to the dom
            $timeout(function(){
                $scope.observerService.getEntityTypes().then(function (types) {
                    $scope.addLegend(types);
                });
            });
        }])
        // Network Controller
        .controller('NetworkController', ['$scope', '$q', '$timeout', '$compile', '$mdDialog', 'VisDataSet', 'playRoutes', 'ObserverService', '_', 'physicOptions', 'graphProperties', 'EntityService', function ($scope, $q, $timeout, $compile, $mdDialog, VisDataSet, playRoutes, ObserverService, _, physicOptions, graphProperties, EntityService) {

            var self = this;

            /* Background collection */
            self.nodes = new VisDataSet([]);
            self.edges = new VisDataSet([]);

            /* Graph collection filtered during runtime */
            self.nodesDataset = new VisDataSet([]);
            self.edgesDataset = new VisDataSet([]);

            self.physicOptions = physicOptions;

            self.networkButtons = [
                { name: 'Fullscreen', template: '<div class="vis-button vis-fullscreen-button" ng-click="NetworkController.toggleFullscreen()"></div>' },
                { name: 'Legend', template: '<div class="vis-button vis-legend-button" ng-click="showLegend = !showLegend"></div>' }
            ];

            // Context menu for single node selection
            self.singleNodeMenu = [
                {
                    title: 'Add as filter',
                    action: function(value, nodeId) { addNodeFilter(nodeId); }
                }, {
                    title: 'Edit node',
                    action: function(value, nodeId) { editNode(nodeId); }
                }, {
                    title: 'Expand',
                    action: function(value, nodeId) { expandNode(nodeId); }
                },
                {
                    title: 'Hide',
                    action: function(value, nodeId) { hideNodes([nodeId]); }
                }, {
                    title: 'Blacklist',
                    action: function(value, nodeId) { EntityService.blacklist([nodeId]); }
                }
            ];

            // Context menu for multiple node selection
            self.multiNodeMenu = [
                {
                    title: 'Merge nodes',
                    action: function(value, nodeIds) { mergeNodes(nodeIds); }
                },
                {
                    title: 'Hide nodes',
                    action: function(value, nodeIds) { hideNodes(nodeIds); }
                },
                {
                    title: 'Blacklist nodes',
                    action: function(value, nodeIds) { EntityService.blacklist(nodeIds); }
                }
            ];

            self.edgeMenu = [{
                title: 'Add as filter',
                action: function(value, edgeId) { addEdgeFilter(edgeId); }
            }];

            $scope.observerService = ObserverService;
            $scope.entityService = EntityService;

            $scope.observer_subscribe_fulltext = function(items) { $scope.fulltextFilters = items; };
            $scope.observer_subscribe_metadata = function(items) { $scope.metadataFilters = items; };
            $scope.observer_subscribe_entity = function(items) { $scope.entityFilters = items; };
            $scope.observerService.subscribeItems($scope.observer_subscribe_fulltext, "fulltext");
            $scope.observerService.subscribeItems($scope.observer_subscribe_metadata, "metadata");
            $scope.observerService.subscribeItems($scope.observer_subscribe_entity, "entity");

            function currentFilter() {
                var fulltext = $scope.fulltextFilters.map(function(v) { return v.data.item; });
                var entities = $scope.entityFilters.map(function(v) { return v.data.id; });
                var timeRange = $scope.observerService.getTimeRange();
                var timeRangeX = $scope.observerService.getXTimeRange();
                var facets = $scope.observerService.getFacets();

                return { fulltext: fulltext, entities: entities, timeRange: timeRange, timeRangeX: timeRangeX, facets: facets };
            }

            $scope.graphOptions = graphProperties.options;
            $scope.graphEvents = {
                "startStabilizing": stabilizationStart,
                "stabilized": stabilized,
                "stabilizationIterationsDone": stabilizationDone,
                "onload": onNetworkLoad,
                "dragEnd": dragNodeDone,
                "oncontext": onContext,
                "click": clickEvent,
                "dragging": dragEvent,
                "hoverEdge": hoverEdge,
                "hoverNode": hoverNode
            };

            /* Consists of objects with entity types and their id */
            $scope.types = [];
            /* Current value of the edge significance slider */
            $scope.edgeImportance = 1;
            /* Maximum edge value of the current underlying graph collection. Updated in reload method */
            $scope.maxEdgeImportance = 80000;
            /* Indicates whether the network is initialized or new data is being loaded */
            $scope.loading = true;
            /* Determines how many keywords should be shown in the edge tooltip */
            self.numEdgeKeywords = 10;
            /* Determines whether the edge importance filter should be preserved during graph reloading */
            self.preserveEdgeImportance = false;

            $scope.$watch('edgeImportance', handleEdgeSlider);

            $scope.reloadGraph = function() {
                var promise = $q.defer();

                var filters = currentFilter();
                var fraction = $scope.types.map(function(t) { return { "key": t.name, "data": t.sliderModel }; });

                playRoutes.controllers.NetworkController.induceSubgraph(filters.fulltext, filters.facets, filters.entities, filters.timeRange, filters.timeRangeX, fraction).get().then(function(response) {
                        // Enable physics for new graph data when network is initialized
                        if(!_.isUndefined(self.network)) {
                            applyPhysicsOptions(self.physicOptions);
                        }
                        $scope.loading = true;

                        var nodes = response.data.entities.map(function(n) {
                            // See css property div.network-tooltip for custom tooltip styling
                            return { id: n.id, label: n.label, type: n.type, value: n.count, group: n.group };
                        });

                        self.nodesDataset.clear();
                        self.nodesDataset.add(nodes);
                        // Highlight new nodes after each filtering step
                        markNewNodes(nodes.map(function(n) { return n.id; }));
                        self.nodes.clear();

                        var edges = response.data.relations.map(function(n) {
                            return { from: n.source, to: n.dest, value: n.occurrence };
                        });

                        self.edges.clear();
                        self.edgesDataset.clear();
                        self.edgesDataset.add(edges);

                        console.log("" + self.nodesDataset.length + " nodes loaded");

                        // Initialize the graph
                        $scope.graphData = {
                            nodes: self.nodesDataset,
                            edges: self.edgesDataset
                        };
                });
                return  promise.promise;
            };

            /**
             * Reloads the graph and preserves the applied edge importance value. In case the new maximum is lower than
             * the current applied edgeImportance, the value is set to the maximum value.
             * **/
            $scope.reloadGraphWithEdgeImportance = function() {
                self.preserveEdgeImportance = true;
                $scope.reloadGraph();
            };


            function init() {
                // Fetch the named entity types
                $scope.observerService.getEntityTypes().then(function (types) {
                    $scope.types = types.map(function(t) { return _.extend(t, { sliderModel: 5 }) });
                    // Initialize graph
                    $scope.reloadGraph();
                });
            }
            // Init the network module
            init();

            $scope.observerService.registerObserverCallback({
                priority: 1,
                callback: function () {
                    console.log("Update network");
                    return $scope.reloadGraph();
                }
            });

            $scope.observerService.subscribeReset(function() {
                console.log("Network reset");
                // Do not use data from previous filtering steps when collection is changed
                self.nodes.clear();
                self.edges.clear();

                return $scope.reloadGraph();
            });

            function addNetworkButtons() {
                self.networkButtons.forEach(function(b) {
                    addNetworkButtonFromTemplate(b.template);
                });
            }

            function addNetworkButtonFromTemplate(template) {
                var panel = angular.element(document.getElementsByClassName('vis-navigation')[0]);
                var button = $compile(template)($scope);
                panel.append(button);
            }

            function applyPhysicsOptions(options) {
                console.log('Physics simulation turned on');
                $scope.graphOptions['physics'] = options;
            }

            function disablePhysics() {
                console.log('Physics simulation turned off');
                $scope.graphOptions['physics'] = false;
            }

            function markNewNodes(nextNodeIds) {
                // Don't highlight all nodes for the initial graph
                if(self.nodes.length > 0) {
                    // This dataset contains the nodes from the previous filtering step without dynamic changing information
                    var previousNodes = new VisDataSet(self.nodes.map(function(n)  { return _.omit(n, 'hidden', 'title', 'value'); }));

                    var currentNodeIds = previousNodes.getIds();
                    var sec = _.intersection(nextNodeIds, currentNodeIds);
                    // Remove old marking from the nodes in order to show only the new nodes for one single step
                    removeNodeHighlight(previousNodes, previousNodes.get(sec));
                    removeNodeHighlight(self.nodesDataset, previousNodes.get(sec));

                    var diff = _.difference(nextNodeIds, currentNodeIds);
                    // Give new nodes a white and dashed border
                    var modifiedNodes = diff.map(function(id) { return { id: id, shapeProperties: { borderDashes: [5, 5] }, color: { border: 'white' }, borderWidth: 2 }});
                    self.nodesDataset.update(modifiedNodes);

                    // TODO Move
                    // Fix nodes from the previous filtering step. This ensures that the node will always preserve its position.
                    // Also remove hidden state between filtering steps and (dynamic) tooltip.
                    var fixedNodes = previousNodes.get(sec).map(function(n) { return _.extend(n, { fixed: { x: true, y: true } })});
                    self.nodesDataset.update(fixedNodes);
                }
            }

            // ----------------------------------------------------------------------------------
            // Node and edge manipulation
            //
            // ----------------------------------------------------------------------------------

            function hideNodes(nodeIds) {
                removeNodes(nodeIds, self.nodesDataset, self.edgesDataset);
                // Hide given nodes in background collection
                self.nodes.update(nodeIds.map(function(n) { return { id: n, hidden: true }}));
                // Retrieve adjacent edges from the background collection
                var adjacentEdges = getAdjacentEdges(nodeIds, self.edges);
                // Hide adjacent edges in background collection
                var hiddenEdges = adjacentEdges.map(function(edge) { return _.extend(edge, { hidden: true }); });
                self.edges.update(hiddenEdges);
                updateImportanceSlider();
            }

            $scope.entityService.subscribeBlacklist($scope, function blacklisted(ev, arg) {
                // Remove node from the visual interface
                hideNodes(arg.parameter);
                // Fetch node replacements for the merged nodes and preserve the current applied edge importance
                $scope.reloadGraphWithEdgeImportance();
            });

            function addNodeFilter(nodeId) {
                var entity = self.nodes.get(nodeId);
                $scope.observerService.addItem({
                        type: 'entity',
                        data: {
                            id: entity.id,
                            description: entity.label,
                            item: entity.label,
                            type: entity.type
                        }
                });
            }

            function addEdgeFilter(edgeId) {
                var edge = self.edges.get(edgeId);
                var from = self.nodes.get(edge.from);
                var to = self.nodes.get(edge.to);
                // TODO This fires two events. Would be better to have a addItems method that fires once. For the time being this hacks solves the problem.
                $scope.observerService.addItem({ type: 'entity', data: { id: from.id, description: from.label, item: from.label, type: from.type }}, false);
                $scope.observerService.addItem({ type: 'entity', data: { id: to.id, description: to.label, item: to.label, type: to.type }});
            }

            function editNode(nodeId) {
                var entity = self.nodes.get(nodeId);
                $mdDialog.show({
                    templateUrl: 'assets/partials/editNode.html',
                    controller: ['$scope', '$mdDialog', 'playRoutes', 'e',
                        function($scope, $mdDialog, playRoutes, e) {
                            $scope.title = e.label;
                            $scope.entity = e;

                            $scope.apply = function () { $mdDialog.hide($scope.entity); };
                            $scope.closeClick = function() { $mdDialog.cancel(); };
                        }],
                    locals: { e: entity },
                    autoWrap: false,
                    parent: $scope.NetworkController.isFullscreen() ? angular.element(document.getElementById('network')) : angular.element(document.body)
                }).then(function(response) {
                    // Adapt tooltip and node color
                    var modified = _.extend(response, {
                        group: $scope.types[response.group].id,
                        // TODO Adapt to new tooltip
                        title: 'Co-occurrence: ' + response.value + '<br>Typ: ' + response.type
                    });
                    self.nodesDataset.update(modified);
                    self.nodes.update(modified);
                    // Store changes
                    playRoutes.controllers.NetworkController.changeEntityNameById(response.id, response.label).get().then(function(response) { /* Error handling */ });
                    playRoutes.controllers.NetworkController.changeEntityTypeById(response.id, response.type).get().then(function(response) { /* Error handling */ });
                }, function() { /* cancel click */ });
            }


            function expandNode(nodeId) {
                var entity = self.nodes.get(nodeId);
                var ids = self.nodes.getIds();
                $mdDialog.show({
                    templateUrl: 'assets/partials/expandNode.html',
                    controller: ['$scope', '$mdDialog', 'playRoutes', 'EntityService', 'e', 'networkIds',
                        function($scope, $mdDialog, playRoutes, EntityService, e, networkIds) {

                            $scope.title = e.label;
                            $scope.entity = e;
                            $scope.neighbors = [];

                            $scope.selection = [];

                            $scope.init = function() {
                                fetchNeighbors();
                            };

                            $scope.init();

                            function fetchNeighbors() {
                                var filters = currentFilter();
                                // Exclude current network nodes from the expansion list
                                playRoutes.controllers.NetworkController.getNeighbors(filters.fulltext, filters.facets, filters.entities, filters.timeRange, filters.timeRangeX, networkIds, $scope.entity.id).get().then(function(response) {
                                    $scope.neighbors = response.data;
                                });
                            }

                            $scope.blacklist = function(id, list) {
                                // Blacklist entity and inform other components
                                EntityService.blacklist([id]);
                                var index = _.findIndex(list, function(n) { return n.id == id; });
                                list.splice(index, 1);
                            };

                            // TODO duplicate in app.js
                            $scope.toggle = function (item, list) {
                                if($scope.exists(item, list)) {
                                    // Remove element in-place from list
                                    var index = list.indexOf(item);
                                    list.splice(index, 1);
                                } else { list.push(item); }
                            };

                            $scope.exists = function (item, list) {
                                var index = list.indexOf(item);
                                return index > -1;
                            };

                            $scope.apply = function () { $mdDialog.hide($scope.selection); };
                            $scope.closeClick = function() { $mdDialog.cancel(); };
                        }],
                    locals: { e: entity, networkIds: ids },
                    autoWrap: false,
                    parent: $scope.NetworkController.isFullscreen() ? angular.element(document.getElementById('network')) : angular.element(document.body)
                }).then(function(response) {
                    var filters = currentFilter();
                    var networkNodes = self.nodesDataset.getIds();

                    var n = response.map(function(n) { return n.id; });
                    playRoutes.controllers.NetworkController.addNodes(filters.fulltext, filters.facets, filters.entities, filters.timeRange, filters.timeRangeX, networkNodes, n).get().then(function(response) {
                        // Remove highlight from previous nodes including background collection
                        removeNodeHighlight(self.nodes, self.nodes);
                        removeNodeHighlight(self.nodesDataset, self.nodesDataset);

                        applyPhysicsOptions(self.physicOptions);
                        $scope.loading = true;
                        var nodes = response.data.entities.map(function(n) {
                            return {
                                id: n.id,
                                label: n.label,
                                type: n.type,
                                value: n.count,
                                group: n.group,
                                // highlight new nodes
                                shapeProperties: { borderDashes: [5, 5] },
                                color: { border: 'white' },
                                borderWidth: 2 };
                        });

                        var edges = response.data.relations.map(function(n) {
                            return { from: n.source, to: n.dest, value: n.occurrence };
                        });

                        self.nodesDataset.add(nodes);
                        self.edgesDataset.add(edges);

                        // Initialize the graph
                        $scope.graphData = {
                            nodes: self.nodesDataset,
                            edges: self.edgesDataset
                        };
                    });
                }, function() { /* cancel click */ });
            }

            function mergeNodes(nodeIds) {
                var nodes = self.nodesDataset.get(nodeIds);

                $mdDialog.show({
                    templateUrl: 'assets/partials/mergeNodes.html',
                    controller: ['$scope', '$mdDialog', 'playRoutes', 'entities',
                        function($scope, $mdDialog, playRoutes, entities) {

                            $scope.entities = entities;

                            $scope.apply = function () { $mdDialog.hide(parseInt($scope.target)); };
                            $scope.closeClick = function() { $mdDialog.cancel(); };
                        }],
                    locals: { entities: nodes },
                    autoWrap: false,
                    parent: $scope.NetworkController.isFullscreen() ? angular.element(document.getElementById('network')) : angular.element(document.body)
                }).then(function(response) {
                    var duplicates = _.without(nodeIds, response);
                    // Remove duplicates from the current network view
                    removeNodes(duplicates, self.nodesDataset, self.edgesDataset);
                    // Remove duplicates from the background collection
                    removeNodes(duplicates, self.nodes, self.edges);
                    playRoutes.controllers.NetworkController.mergeEntitiesById(response, duplicates).get().then(function(response) {
                        // Fetch node replacements for the merged nodes
                        $scope.reloadGraph();
                    });
                }, function() { /* cancel click */ });
            }

            /**
             * Adjust the maximum of the edge importance slider to the current maximum of the background collection.
             */
            function updateImportanceSlider() {
                // Update new edge max value from non hidden edges
                var max = new VisDataSet(self.edges.get({ filter: function(edge) { return !edge.hidden; }})).max("value").value;
                $scope.maxEdgeImportance = max;
            }

            // ----------------------------------------------------------------------------------
            // Network Util Helper
            //
            // ----------------------------------------------------------------------------------


            /**
             * Removes nodes from the given data collections including their adjacent edges
             * and returns the removed node and edge ids.
             */
            function removeNodes(nodeIds, nodeDataset, edgeDataset) {
                var removedNodeIds = nodeDataset.remove(nodeIds);
                var adjacentEdges = getAdjacentEdges(nodeIds, edgeDataset);
                edgeDataset.remove(adjacentEdges);
                // Once the network changes the slider needs to be updated
                updateImportanceSlider();
                return { nodes: removedNodeIds, edges: adjacentEdges.map(function(e) { return e.id; }) };
            }


            function getAdjacentEdges(nodeIds, edgeDataset) {
                var adjacentEdges = edgeDataset.get({
                    filter: function(edge) {
                        return (_.contains(nodeIds, edge.to) || _.contains(nodeIds, edge.from))
                    }
                });
                return adjacentEdges;
            }

            function removeNodeHighlight(dataset, nodesToBeUpdated) {
                var cleanNodes = nodesToBeUpdated.map(function(n) { return _.extend(n, { 'shapeProperties': { borderDashes: false }, color: undefined, borderWidth: 1 })});
                dataset.update(cleanNodes);
            }

            // ----------------------------------------------------------------------------------
            // Network Event Callbacks
            //
            // ----------------------------------------------------------------------------------

            function onNetworkLoad(network) {
                self.network = network;
                addNetworkButtons();
            }

            function stabilizationStart() {
                console.log("Stabilization start with " + self.nodesDataset.length + " nodes");
            }

            function stabilized(params) {
                console.log("Stabilization done after " + JSON.stringify(params, null, 4));
            }

            /**
             * Called when the internal iteration threshold is reached. See graphConfig.
             */
            function stabilizationDone() {
                // Hacky solution since this event seems to be fired twice.
                if(!$scope.loading) {
                    return;
                }
                console.log("Stabilization Iteration Done");
                // Release fixed nodes from the previous filter step
                var releasedNodes = self.nodesDataset.getIds().map(function(id) { return { id: id, fixed: false }});
                self.nodesDataset.update(releasedNodes);

                // Once the network is stabilized the node positions are stored and the
                // physics simulation is disabled.
                self.network.storePositions();
                self.nodes = new VisDataSet(self.nodesDataset.get());
                self.edges = new VisDataSet(self.edgesDataset.get());

                $scope.loading = false;
                disablePhysics();

                // Update new maximum value for the edge importance slider.
                updateImportanceSlider();
                // If the value is true, the current applied edge importance will be applied to the network. Otherwise the
                // edge importance is removed.
                if(self.preserveEdgeImportance) {
                    // If the current applied edge importance is larger than the new maximum, then adjust it accordingly.
                    if($scope.maxEdgeImportance < $scope.edgeImportance) $scope.edgeImportance = $scope.maxEdgeImportance;
                    handleEdgeSlider($scope.edgeImportance, $scope.edgeImportance - 1);
                } else {
                    $scope.edgeImportance = 1;
                }
                self.preserveEdgeImportance = false;
            }

            // TODO: Remove code duplication
            function hoverEdge(event) {
                var edge = self.edgesDataset.get(event.edge);
                // Only fetch keywords if not already fetched
                if(_.has(edge, "title")) {
                    return;
                }

                var filters = currentFilter();
                playRoutes.controllers.NetworkController.getEdgeKeywords(filters.fulltext, filters.facets, filters.entities, filters.timeRange, filters.timeRangeX, edge.from, edge.to, self.numEdgeKeywords).get().then(function(response) {
                    var formattedTerms = response.data.map(function(t) { return '' +  t.term + ': ' + t.score; });

                    var docTip = '<p>Occurs in <b>' + edge.value + '</b> documents</p>';
                    var keywordTip = '<p><b>Keywords</b></p><ul><li>' + formattedTerms.join('</li><li>') + '</li></ul>';
                    var tooltip = docTip + keywordTip;

                    self.edgesDataset.update({ id: edge.id, title: tooltip });
                    // Only update background collection after stabilization.
                    /* if(!$scope.loading) {
                        self.edges.update({ id: event.edge, title: keywords });
                    } */
                });
            }

            function hoverNode(event) {
                var node = self.nodesDataset.get(event.node);
                // Only fetch keywords if not already fetched
                if(_.has(node, "title")) {
                    return;
                }

                var filters = currentFilter();
                playRoutes.controllers.NetworkController.getNeighborCounts(filters.fulltext, filters.facets, filters.entities, filters.timeRange, filters.timeRangeX, node.id).get().then(function(response) {
                    var formattedTerms = response.data.map(function(t) { return '' +  t.type + ': ' + t.count; });

                    var docTip = '<p>Occurs in <b>' + node.value + ' </b>documents</p><p>Type: <b>' + node.type + '</b></p>';
                    var neighborTip = '<p><b>Neighbors</b></p><ul><li>' + formattedTerms.join('</li><li>') + '</li></ul>';
                    var tooltip = docTip + neighborTip;

                    self.nodesDataset.update({ id: node.id, title: tooltip });
                });
            }

            function clickEvent(event) {
                closeContextMenu();
            }

            function dragEvent(event) {
                closeContextMenu();
            }

            function dragNodeDone(event) {
                // Update node positions of the background collection whenever they are moved
                if(event.nodes.length == 1 && $scope.graphOptions['physics'] == false) {
                    var match = self.nodes.get(event.nodes[0]);
                    match.x = event.pointer.canvas.x;
                    match.y = event.pointer.canvas.y;
                    self.nodes.update(match);
                }
            }

            function handleEdgeSlider(newValue, oldValue) {
                console.log("Edge slider: " + newValue + ", " + oldValue);
                closeContextMenu();
                if(newValue > oldValue) {
                    var edgesToRemove = self.edgesDataset.get({
                        filter: function (item) {
                            return (item.value < newValue);
                        }
                    });
                    self.edgesDataset.remove(edgesToRemove);

                    var nodesToRemove = self.nodesDataset.get({
                        filter: function (node) {
                            var connecting = self.edgesDataset.get({
                                filter: function(edge) {
                                    return (node.id == edge.from || node.id == edge.to);
                                }
                            });
                            return (connecting.length == 0);
                        }
                    });
                    self.nodesDataset.remove(nodesToRemove);

                } else if(newValue < oldValue) {
                    var edgesToAdd = self.edges.get({
                        filter: function (item) {
                            return (item.value >= newValue && !item.hidden)
                        }
                    });

                    var nodeIds = [].concat.apply([], edgesToAdd.map(function(edge) { return [edge.to, edge.from]; }));
                    var nodesToAdd = self.nodes.get({
                        filter: function (item) {
                            // Add nodes to the network if there is a connecting edge and the
                            // node is not hidden by the user
                            return (_.contains(nodeIds, item.id) && !item.hidden);
                        }
                    });

                    self.edgesDataset.update(edgesToAdd);
                    self.nodesDataset.update(nodesToAdd);
                }
            }

            function onContext(params) {
                params.event.preventDefault();
                closeContextMenu();

                var position = { x: params.pointer.DOM.x, y: params.pointer.DOM.y };
                var nodeIdOpt = self.network.getNodeAt(position);
                var edgeIdOpt = self.network.getEdgeAt(position);

                var selection = self.network.getSelectedNodes();

                // Multiple nodes selected and the right-clicked node is in this selection
                if(!_.isUndefined(nodeIdOpt) && selection.length > 1 && _.contains(selection, nodeIdOpt)) {
                    showContextMenu(_.extend(position, { id: selection }), self.multiNodeMenu);
                }
                // Single node selected
                else if(!_.isUndefined(nodeIdOpt)) {
                    self.network.selectNodes([nodeIdOpt]);
                    showContextMenu(_.extend(position, { id: nodeIdOpt }), self.singleNodeMenu);
                // Edge selected
                } else if(!_.isUndefined(edgeIdOpt)) {
                    self.network.selectEdges([edgeIdOpt]);
                    showContextMenu(_.extend(position, { id: edgeIdOpt }), self.edgeMenu);
                }
                else {
                    // Nop
                }
            }

            function showContextMenu(params, menuEntries) {
                var container = document.getElementById('mynetwork');

                var offsetLeft = container.offsetLeft;
                var offsetTop = container.offsetTop;

                self.popupMenu = document.createElement("div");
                self.popupMenu.className = 'popupMenu';
                self.popupMenu.style.left = params.x + $("#documents-view").width() + 'px';
                //self.popupMenu.style.left = params.x - offsetLeft + 'px';
                self.popupMenu.style.top =  params.y + $("header").height() + $("#history").height()+'px';
                //self.popupMenu.style.top =  params.y - offsetTop +'px';

                var ul = document.createElement('ul');
                self.popupMenu.appendChild(ul);

                for (var i = 0; i < menuEntries.length; i++) {
                    var li = document.createElement('li');
                    ul.appendChild(li);
                    li.innerHTML = li.innerHTML + menuEntries[i].title;
                    (function(value, id, action){
                        li.addEventListener("click", function() {
                            closeContextMenu();
                            action(value, id);
                        }, false);})(menuEntries[i].title, params.id, menuEntries[i].action);
                }
                document.body.appendChild(self.popupMenu);
                //container.appendChild(self.popupMenu);
            }

            function closeContextMenu() {
                if (self.popupMenu !== undefined) {
                    self.popupMenu.parentNode.removeChild(self.popupMenu);
                    self.popupMenu = undefined;
                }
            }
        }]);
});
