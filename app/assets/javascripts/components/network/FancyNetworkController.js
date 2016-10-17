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
    'ngVis',
    'angularMoment'
], function(angular) {
    'use strict';

    angular.module('myApp.network', ['ngMaterial', 'ngVis', 'play.routing', 'angularMoment']);
    angular.module('myApp.network')
        // TODO This factory is crap, but referenced in the code ... remove it!
        // This factory is used to share graph properties between this module and the app.js
        .factory('graphPropertiesShareService', function () {
            var graphProperties = {
                // Order: locations, organizations, persons, miscellaneous
                categoryColors: ["#8dd3c7", "#fb8072","#bebada", "#ffffb3"],
                categories:
                    [
                        {id: 'LOC', full: 'Location'},
                        {id: 'ORG', full: 'Organization'},
                        {id: 'PER', full: 'Person'},
                        {id: 'MISC', full: 'Miscellaneous'}
                    ],
                CATEGORY_COLOR_INDEX_LOC: 0,
                CATEGORY_COLOR_INDEX_ORG: 1,
                CATEGORY_COLOR_INDEX_PER: 2,
                CATEGORY_COLOR_INDEX_MISC: 3,
                // Delivers the index of a given category name
                getIndexOfCategory: function (category) {
                    switch (category) {
                        case 'LOC':
                            return this.CATEGORY_COLOR_INDEX_LOC;
                        case 'ORG':
                            return this.CATEGORY_COLOR_INDEX_ORG;
                        case 'PER':
                            return this.CATEGORY_COLOR_INDEX_PER;
                        default:
                            return this.CATEGORY_COLOR_INDEX_MISC;
                    }
                }
            };
            return graphProperties;
        })
        // Network Legend Controller
        .controller('LegendController', ['$scope', '$timeout', 'VisDataSet', 'graphProperties', function ($scope, $timeout, VisDataSet, graphProperties) {

            $scope.legendNodes = new VisDataSet([]);
            this.legendOptions = graphProperties.legendOptions;
            this.legendData = { nodes: $scope.legendNodes, edges: [] };
            this.legendEvents = { "onload": function(network) { $scope.legendNetwork = network; } };

            $scope.addLegend = function() {
                var x = 0; var y = 0; var distance = 100;
                $scope.legendNodes.add([
                    { id: -1, x: x, y: y, label: 'Per', group: 0, value: 1, fixed: true, physics: false },
                    { id: -2, x: x + distance, y: y, label: 'Org', group: 1, value: 1, fixed: true, physics: false },
                    { id: -3, x: x + 2*distance, y: y, label: 'Loc', group: 2, value: 1, fixed: true, physics: false },
                    { id: -4, x: x + 3*distance, y: y, label: 'Misc', group: 3, value: 1, fixed: true, physics: false }
                ]);
                $scope.legendNetwork.fit();
            };

            // Add nodes after the legend div is added to the dom
            $timeout(function(){
                $scope.addLegend();
            });
        }])
        // Network Controller
        .controller('FancyNetworkController', ['$scope', '$timeout', '$compile', '$mdDialog', 'VisDataSet', 'playRoutes', 'ObserverService', '_', 'physicOptions', 'graphProperties', function ($scope, $timeout, $compile, $mdDialog, VisDataSet, playRoutes, ObserverService, _, physicOptions, graphProperties) {

            var self = this;

            /* Entity types to their id */
            self.types = {};

            /* Background collection */
            self.nodes = new VisDataSet([]);
            self.edges = new VisDataSet([]);

            /* Graph collection filtered during runtime */
            self.nodesDataset = new VisDataSet([]);
            self.edgesDataset = new VisDataSet([]);

            self.physicOptions = physicOptions;

            self.networkButtons = [
                { name: 'Fullscreen', template: '<div class="vis-button vis-fullscreen-button" ng-click="FancyNetworkController.toggleFullscreen()"></div>' },
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
                    action: function(value, nodeId) { hideNode(nodeId); }
                }, {
                    title: 'Blacklist',
                    action: function(value, nodeId) { blacklistNode(nodeId); }
                }
            ];

            // Context menu for multiple node selection
            self.multiNodeMenu = [
                {
                    title: 'Merge nodes',
                    action: function(value, nodeIds) { mergeNodes(nodeIds); }
                }
            ];

            self.edgeMenu = [{
                title: 'Add as filter',
                action: function(value, edgeId) { addEdgeFilter(edgeId); }
            }];

            $scope.observerService = ObserverService;

            // TODO: would be cool to have the event type as argument i.e. remove or add
            $scope.observer_subscribe_fulltext = function(items) {
                // TODO: check why map does not work here
                $scope.fulltextFilters = items;//items.map(function(f) { return f.data.name; });
            };
            $scope.observer_subscribe_metadata = function(items) { $scope.metadataFilters = items};
            $scope.observer_subscribe_entity = function(items) { $scope.entityFilters = items};
            $scope.observerService.subscribeItems($scope.observer_subscribe_fulltext, "fulltext");
            $scope.observerService.subscribeItems($scope.observer_subscribe_metadata, "metadata");
            $scope.observerService.subscribeItems($scope.observer_subscribe_entity, "entity");

            function currentFilter() {
                var fulltext = $scope.fulltextFilters.map(function(v) { return v.data.name; });
                var entities = $scope.entityFilters.map(function(v) { return v.data.id; });
                var timeRange = $scope.observerService.getTimeRange();
                var facets = $scope.observerService.getFacets();

                return { fulltext: fulltext, entities: entities, timeRange: timeRange, facets: facets };
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

            /* Current value of the edge significance slider */
            $scope.edgeImportance = 1;
            /* Maximum edge value of the current underlying graph collection. Updated in reload method */
            $scope.maxEdgeImportance = 80000;
            /* */
            $scope.numPer = 5;
            $scope.numOrg = 5;
            $scope.numLoc = 5;
            $scope.numMisc = 5;

            /* Indicates whether the network is initialized or new data is being loaded */
            $scope.loading = true;

            $scope.$watch('edgeImportance', handleEdgeSlider);

            $scope.reloadGraph = function() {
                var filters = currentFilter();

                 var fraction = [
                    {"key": "PER", "data": $scope.numPer },
                    {"key": "ORG", "data": $scope.numOrg },
                    {"key": "LOC", "data": $scope.numLoc },
                    {"key": "MISC", "data": $scope.numMisc }
                ];

                playRoutes.controllers.NetworkController.induceSubgraph(filters.fulltext, filters.facets, filters.entities, filters.timeRange, fraction, []).get().then(function(response) {
                        // Enable physics for new graph data when network is initialized
                        if(!_.isUndefined(self.network)) {
                            applyPhysicsOptions(self.physicOptions);
                        }
                        $scope.loading = true;

                        // Assignment from entity types to their id for coloring
                        self.types = response.data.types;

                        //var originalMax = _.max(response.data.entities, function(n) { return n.count; }).count;
                        //var originalMin = _.min(response.data.entities, function(n) { return n.count; }).count;

                        var nodes = response.data.entities.map(function(n) {
                            // See css property div.network-tooltip for custom tooltip styling
                            // map counts to interval [1,2] for nodes mass
                            /*var mass = ((n.count - originalMin) / (originalMax - originalMin)) * (2 - 1) + 1;
                            // If all nodes have the same occurrence assign same mass. This also prevents errors
                            // for wrong interval mappings e.g. [1,1] to [1,2] yields NaN for the mass.
                            if(originalMin == originalMax) mass = 1;*/
                            return { id: n.id, label: n.label, type: n.type,  value: n.count, group: n.group };
                        });

                        self.nodesDataset.clear();
                        self.nodesDataset.add(nodes);
                        // Highlight new nodes after each filtering step
                        markNewNodes(nodes.map(function(n) { return n.id; }));
                        self.nodes.clear();

                        var edges = response.data.relations.map(function(n) {
                            return {from: n[0], to: n[1], value: n[2] };
                        });

                        self.edges.clear();
                        self.edgesDataset.clear();
                        self.edgesDataset.add(edges);

                        // Update the maximum edge importance slider value
                        $scope.maxEdgeImportance = (self.edgesDataset.length > 0) ? self.edgesDataset.max("value").value : 0;
                        console.log("" + self.nodesDataset.length + " nodes loaded");

                        // Initialize the graph
                        $scope.graphData = {
                            nodes: self.nodesDataset,
                            edges: self.edgesDataset
                        };
                });
            };


            // Initialize graph
            $scope.reloadGraph();

            $scope.observerService.registerObserverCallback(function() {
                console.log("Update network");
                $scope.reloadGraph();
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
                    var cleanNodes = previousNodes.get(sec).map(function(n) { return _.extend(n, { 'shapeProperties': { borderDashes: false }, color: undefined, borderWidth: 1 })});
                    previousNodes.update(cleanNodes);
                    self.nodesDataset.update(cleanNodes);

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

            function hideNode(nodeId) {
                removeNodes([nodeId], self.nodesDataset, self.edgesDataset);
                // Hide given node in background collection
                self.nodes.update({ id: nodeId, hidden: true });
                // Retrieve adjacent edges from the background collection
                var adjacentEdges = getAdjacentEdges([nodeId], self.edges);
                // Hide adjacent edges in background collection
                var hiddenEdges = adjacentEdges.map(function(edge) { return _.extend(edge, { hidden: true }); });
                self.edges.update(hiddenEdges);
                updateImportanceSlider();
            }

            function blacklistNode(nodeId) {
                // Remove node from the visual interface
                hideNode(nodeId);
                // Mark node as blacklisted
                playRoutes.controllers.NetworkController.deleteEntityById(nodeId).get().then(function(response) { /* Error handling */ });
            }

            function addNodeFilter(nodeId) {
                var entity = self.nodes.get(nodeId);
                $scope.observerService.addItem({ type: 'entity', data: { id: entity.id, name: entity.label, type: entity.type }});
            }

            function addEdgeFilter(edgeId) {
                var edge = self.edges.get(edgeId);
                var from = self.nodes.get(edge.from);
                var to = self.nodes.get(edge.to);
                // TODO This fires two events. Would be better to have a addItems method
                $scope.observerService.addItem({ type: 'entity', data: { id: from.id, name: from.label, type: from.type }});
                $scope.observerService.addItem({ type: 'entity', data: { id: to.id, name: to.label, type: to.type }});
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
                    parent: $scope.FancyNetworkController.isFullscreen() ? angular.element(document.getElementById('network')) : angular.element(document.body)
                }).then(function(response) {
                    // Adapt tooltip and node color
                    var modified = _.extend(response, {
                        group: self.types[response.type],
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
                var neighbors = [
                    {id: 1, label: "Titan", type: "God", freq: 10 },
                    {id: 2, label: "Dr. Who", type: "Timelord", freq: 7 },
                    {id: 3, label: "Cinderella", type: "Girl", freq: 3 }
                ];
                $mdDialog.show({
                    templateUrl: 'assets/partials/expandNode.html',
                    controller: ['$scope', '$mdDialog', 'playRoutes', 'e', 'n',
                        function($scope, $mdDialog, playRoutes, e, n) {

                            $scope.columns = ['Name', 'Type'];

                            $scope.title = e.label;
                            $scope.entity = e;
                            $scope.neighbors = n;

                            $scope.apply = function () { $mdDialog.hide(); };
                            $scope.closeClick = function() { $mdDialog.cancel(); };
                        }],
                    locals: { e: entity, n: neighbors },
                    autoWrap: false,
                    parent: $scope.FancyNetworkController.isFullscreen() ? angular.element(document.getElementById('network')) : angular.element(document.body)
                }).then(function(response) { /* apply click */ }, function() { /* cancel click */ });
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
                    parent: $scope.FancyNetworkController.isFullscreen() ? angular.element(document.getElementById('network')) : angular.element(document.body)
                }).then(function(response) {
                    var duplicates = _.without(nodeIds, response);
                    // Remove duplicates from the current network view
                    removeNodes(duplicates, self.nodesDataset, self.edgesDataset);
                    // Remove duplicates from the background collection
                    removeNodes(duplicates, self.nodes, self.edges);
                    playRoutes.controllers.NetworkController.mergeEntitiesById(response, duplicates).get().then(function(response) { /* Error handling */ });
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
                console.log("Stabilization Iteration Done");
                // Release fixed nodes from the previous filter step
                var releasedNodes = self.nodesDataset.getIds().map(function(id) { return { id: id,  fixed: false }});
                self.nodesDataset.update(releasedNodes);

                // Once the network is stabilized the node positions are stored and the
                // physics simulation is disabled.
                self.network.storePositions();
                self.nodes = new VisDataSet(self.nodesDataset.get());
                self.edges = new VisDataSet(self.edgesDataset.get());

                $scope.loading = false;
                // Reset the current edge slider position, because the new
                // maximum value could be smaller than the current value.
                $scope.edgeImportance = 1;
                disablePhysics();
            }

            // TODO: Remove code duplication
            function hoverEdge(event) {
                var edge = self.edgesDataset.get(event.edge);
                // Only fetch keywords if not already fetched
                if(_.has(edge, "title")) {
                    return;
                }

                var filters = currentFilter();
                playRoutes.controllers.NetworkController.getEdgeKeywords(filters.fulltext, filters.facets, filters.entities, filters.timeRange, edge.from, edge.to, 4).get().then(function(response) {
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
                playRoutes.controllers.NetworkController.getNeighborCounts(filters.fulltext, filters.facets, filters.entities, filters.timeRange, node.id).get().then(function(response) {
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
                self.popupMenu.style.left = params.x - offsetLeft + 'px';
                self.popupMenu.style.top =  params.y - offsetTop +'px';

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
                container.appendChild(self.popupMenu);
            }

            function closeContextMenu() {
                if (self.popupMenu !== undefined) {
                    self.popupMenu.parentNode.removeChild(self.popupMenu);
                    self.popupMenu = undefined;
                }
            }
        }]);
});
