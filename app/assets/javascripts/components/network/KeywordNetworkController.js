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
    'elasticsearch',
    'ngVis'
], function(angular) {
    'use strict';
    /**
     * network keyword module:
     * visualization and interaction of keywords in network graph
     */
    angular.module('myApp.keywordNetwork', ['ngMaterial', 'ngVis', 'play.routing', 'elasticsearch'])
        .service('client', function (esFactory) {
            return esFactory({
                host: 'localhost:9500',
                apiVersion: '5.5',
                log: 'trace'
            });
        });
    angular.module('myApp.keywordNetwork')
        // Keyword Network Legend Controller
        .controller('KeywordLegendController', ['$scope', '$timeout', 'VisDataSet', 'graphProperties', function ($scope, $timeout, VisDataSet, graphProperties) {

            $scope.legendNodes = new VisDataSet([]);
            $scope.legendTypes = [{
                id: -1,
                name: 'KEY',
                sliderModel: 20
            },{
                id: -2,
                name: 'TAG',
                sliderModel: $scope.currentTags.length
            }];
            this.legendOptions = graphProperties.legendOptions;
            this.legendData = { nodes: $scope.legendNodes, edges: [] };
            this.legendEvents = { "onload": function(network) { $scope.legendNetwork = network; } };

            // See style.css 'legend' for width and height of the render area
            $scope.addLegend = function(types) {
                var x = 40; var y = 20; var distance = 80;

                var nodes = types.map(function(t, i) {
                    return { id: -(i+1), x: x + i * distance, y: y, label: t.name, group: t.id, value: 1, fixed: true, physics: false };
                });
                $scope.legendNodes.add(nodes);
                // $scope.legendNetwork.fit();
            };

            // Add nodes after the legend div is added to the dom
            $timeout(function(){
                $scope.addLegend($scope.legendTypes);
            });
        }])
        // Keyword Network Controller
        .controller('KeywordNetworkController', ['$scope', '$q', '$timeout', '$compile', '$mdDialog', 'VisDataSet', 'playRoutes', 'ObserverService', '_', 'physicOptions', 'graphProperties', 'EntityService', 'client', 'esFactory', function ($scope, $q, $timeout, $compile, $mdDialog, VisDataSet, playRoutes, ObserverService, _, physicOptions, graphProperties, EntityService, client, esFactory) {

            var self = this;

            $scope.tagsSelected = false;

            $scope.currentTags = [];

            $scope.areTagsSelected = false;

            $scope.selectTags = function (toggle = false) {

                if(!toggle){
                    $scope.areTagsSelected = ! $scope.areTagsSelected;
                }

                if($scope.areTagsSelected){
                    playRoutes.controllers.KeywordNetworkController.resetTagKeywordRelation().get().then(function () {
                        playRoutes.controllers.KeywordNetworkController.getTags().get().then(function (response) {

                            $scope.tagCount = 0;
                            $scope.currentTags = response.data.result;

                            for(let res of $scope.currentTags) {
                                esSearchKeywords('' + res.documentId, res.label);
                            }
                        });
                    });
                }
                else {
                    EntityService.toggleTags($scope.areTagsSelected, $scope);
                }
            };

            $scope.getTagsSelected = function () {
                return $scope.areTagsSelected;
            };

            function esSearchKeywords(res, tag) {

                client.search({
                    index: $scope.indexName,
                    type: 'document',
                    id: res,
                    body: {
                        query: {
                            match: {
                                _id: res
                            }
                        }
                    }

                }).then(function (resp) {

                    if(resp.hits.hits[0]._source.Keywords){
                        let keywords = [];
                        let frequencies = [];

                        for(let item of resp.hits.hits[0]._source.Keywords){
                            keywords.push(item.Keyword);
                            frequencies.push(item.TermFrequency);
                        }

                        playRoutes.controllers.KeywordNetworkController.setTagKeywordRelation(tag, keywords, frequencies).get().then(function () {
                            $scope.tagCount++;

                            if($scope.tagCount == $scope.currentTags.length){
                                EntityService.toggleTags($scope.areTagsSelected, $scope);
                            }
                        });
                    }
                    else {
                        $scope.tagCount++;
                    }

                }, function (error) {
                    console.trace(error.message);
                });
            }

            /* Background collection */
            self.nodes = new VisDataSet([]);
            self.edges = new VisDataSet([]);

            /* Graph collection filtered during runtime */
            self.nodesDataset = new VisDataSet([]);
            self.edgesDataset = new VisDataSet([]);

            self.physicOptions = physicOptions;

            $scope.showKeywordLegend = false;

            $scope.getToggleKeywordLegend = function () {
                return $scope.showKeywordLegend;
            };

            $scope.toggleKeywordLegend = function () {

                $scope.showKeywordLegend = ! $scope.showKeywordLegend;
            };

            self.keywordNetworkButtons = [
                { name: 'Fullscreen', template: '<div class="vis-button vis-fullscreen-button" ng-click="KeywordNetworkController.toggleFullscreen()"></div>' },
                { name: 'Legend', template: '<div class="vis-button vis-legend-button" ng-click="toggleKeywordLegend()"></div>' }
            ];

            // Context menu for single node selection
            self.singleNodeMenu = [
                {
                    title: 'Add as filter',
                    action: function(value, nodeId) { addNodeFilter(nodeId); }

                },
                {
                    title: 'Hide',
                    action: function(value, nodeId) { hideNodes([nodeId]); }
                }, {
                    title: 'Blacklist Keyword',
                    action: function (value, nodeId) {
                        EntityService.blacklistKeyword(self.nodesDataset._data[nodeId].label, nodeId);
                    }
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
                    title: 'Blacklist keywords',
                    action: function(value, nodeIds) {
                        nodeIds.forEach(id => EntityService.blacklistKeyword(self.nodesDataset._data[id].label, id)); }
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
            $scope.observer_subscribe_keyword = function(items) { $scope.keywordFilters = items; };
            $scope.observerService.subscribeItems($scope.observer_subscribe_fulltext, "fulltext");
            $scope.observerService.subscribeItems($scope.observer_subscribe_metadata, "metadata");
            $scope.observerService.subscribeItems($scope.observer_subscribe_entity, "entity");
            $scope.observerService.subscribeItems($scope.observer_subscribe_keyword, "keyword");

            function currentFilter() {
                var fulltext = $scope.fulltextFilters.map(function(v) { return v.data.item; });
                var entities = $scope.entityFilters.map(function(v) { return v.data.id; });
                var keywords = $scope.keywordFilters.map(function(v) { return v.data.item; });
                var timeRange = $scope.observerService.getTimeRange();
                var timeRangeX = $scope.observerService.getXTimeRange();
                var facets = $scope.observerService.getFacets();

                return { fulltext: fulltext, entities: entities, keywords: keywords, timeRange: timeRange, timeRangeX: timeRangeX, facets: facets };
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
                "hoverNode": hoverNode,
                "blurNode": blurNode
            };

            /* Consists of objects with entity types and their id */
            $scope.keywordTypes = [];
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

            $scope.resultNodes = [];
            $scope.resultRelations = [];

            $scope.highlightKeywordNodes = function (nodes) {
                let graphNodes = Object.values($scope.graphData.nodes._data);
                for(let node of nodes){
                    for(let graphNode of graphNodes){
                        if(node.Keyword == graphNode.label){
                            hoverHighlight(graphNode);
                        }
                    }
                }
            };

            function clearGraph() {

                var promise = $q.defer();

                if(!_.isUndefined(self.network)) {
                    applyPhysicsOptions(self.physicOptions);
                }

                self.nodes.clear();
                self.nodesDataset.clear();

                self.edges.clear();
                self.edgesDataset.clear();

                // Initialize the graph
                $scope.graphData = {
                    nodes: self.nodesDataset,
                    edges: self.edgesDataset
                };

                return promise.promise;
            }

            $scope.buildGraph = function() {
                var promise = $q.defer();

                var filters = currentFilter();
                var fraction = $scope.keywordTypes.map(function(t) { return { "key": t.name, "data": t.sliderModel }; });

                playRoutes.controllers.KeywordNetworkController.induceSubgraphKeyword(filters.fulltext, filters.facets, filters.entities, filters.keywords, filters.timeRange, filters.timeRangeX, fraction).get().then(function(response) {

                        // Enable physics for new graph data when network is initialized
                        if(!_.isUndefined(self.network)) {
                            applyPhysicsOptions(self.physicOptions);
                        }

                        $scope.loading = true;

                        $scope.resultNodes = response.data.entities.map(function(n) {
                            // See css property div.network-tooltip for custom tooltip styling
                            if(n.termType == 'TAG') {
                                return {id: n.id, label: n.label, group: -2, type: 'TAG', value: n.count, borderWidth: 2};
                            }
                            else {
                                return {id: n.id, label: n.label, group: -1, type: 'KEYWORD', value: n.count, borderWidth: 3};
                            }
                        });

                        $scope.resultRelations = response.data.relations.map(function(n) {
                            return {
                                from: getEdgeByLabel(n.source, response.data.entities),
                                to: getEdgeByLabel(n.dest, response.data.entities),
                                value: n.occurrence
                            };
                        });

                        self.nodes.clear();
                        self.nodesDataset.clear();
                        self.nodesDataset.add($scope.resultNodes);

                        // Highlight new nodes after each filtering step
                        markNewNodes($scope.resultNodes.map(function(n) { return n.id; }));

                        self.edges.clear();
                        self.edgesDataset.clear();
                        self.edgesDataset.add($scope.resultRelations);

                        // Initialize the graph
                        $scope.graphData = {
                            nodes: self.nodesDataset,
                            edges: self.edgesDataset
                        };
                });
                return promise.promise;
            };

            $scope.reloadGraph = function () {
                // Clear Graph first to avoid wrong structuring of graph on load
                clearGraph();
                $scope.buildGraph();
            };

            $scope.checkTags = function (toggle = false) {
                $scope.selectTags(toggle);
            };

            $scope.checkChange = function (event) {
                // if click event reload after two seconds
                if(event && event.type){
                    setTimeout(function () {
                        $scope.reloadGraph();
                    }, 2000);
                }
                else {
                    $scope.reloadGraph();
                }
            };

            /**
             * Reloads the graph and preserves the applied edge importance value. In case the new maximum is lower than
             * the current applied edgeImportance, the value is set to the maximum value.
             * **/
            $scope.reloadKeywordGraphWithEdgeImportance = function () {
                self.preserveEdgeImportance = true;
                $scope.reloadGraph();
            };

            $scope.indexName = '';
            function getIndexName() {
                playRoutes.controllers.DocumentController.getIndexName().get().then(function(response) {
                    $scope.indexName = response.data.index;
                });
            }

            function initES() {
                playRoutes.controllers.KeywordNetworkController.getHostAddress().get().then(function (response) {
                    if(response && response.data){
                        client.indices.transport._config.host = response.data;
                        client.indices.transport._config.hosts = response.data;
                    }
                });
            }

            function init() {
                // init graph
                $scope.keywordTypes = [{
                    name: "KEY",
                    sliderModel: 20
                }];
                $scope.reloadGraph();
                // get index name from the back end
                getIndexName();
                EntityService.setKeywordScope($scope);

                initES();
            }
            // Init the network module
            init();

            $scope.observerService.registerObserverCallback({
                priority: 1,
                callback: function () {
                    return $scope.reloadGraph();
                }
            });

            $scope.observerService.subscribeReset(function() {
                // Do not use data from previous filtering steps when collection is changed
                self.nodes.clear();
                self.edges.clear();

                return $scope.reloadGraph();
            });

            function addNetworkButtons() {
                self.keywordNetworkButtons.forEach(function(b) {
                    addNetworkButtonFromTemplate(b.template);
                });
            }

            function addNetworkButtonFromTemplate(template) {
                var panel = angular.element(document.getElementsByClassName('vis-navigation')[1]);
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
                    var modifiedNodes = diff.map(function(id) { return { id: id, shapeProperties: { borderDashes: [5, 5] }, color: { border: 'red' }, borderWidth: 3 }});
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

            $scope.entityService.subscribeBlacklistKeyword($scope, function blacklisted(ev, arg) {
                // Remove node from the visual interface
                hideNodes(arg.parameter);
                // Fetch node replacements for the merged nodes and preserve the current applied edge importance
                $scope.reloadKeywordGraphWithEdgeImportance();
            });

            function addNodeFilter(nodeId) {
                var keyword = self.nodes.get(nodeId);
                $scope.observerService.addItem({
                    type: 'keyword',
                        data: {
                            id: keyword.id,
                            description: keyword.label,
                            item: keyword.label,
                            type: keyword.type
                        }
                });
            }

            function addEdgeFilter(edgeId) {
                var edge = self.edges.get(edgeId);
                var from = self.nodes.get(edge.from);
                var to = self.nodes.get(edge.to);
                // TODO This fires two events. Would be better to have a addItems method that fires once. For the time being this hacks solves the problem.
                $scope.observerService.addItem({
                    type: 'keyword',
                    data: {id: from.id, description: from.label, item: from.label, type: from.type}
                }, false);
                $scope.observerService.addItem({
                    type: 'keyword',
                    data: {id: to.id, description: to.label, item: to.label, type: to.type}
                });
            }

            function mergeNodes(nodeIds) {
                var nodes = self.nodesDataset.get(nodeIds);

                $mdDialog.show({
                    templateUrl: 'assets/partials/mergeNodes.html',
                    controller: ['$scope', '$mdDialog', 'playRoutes', 'entities',
                        function($scope, $mdDialog, playRoutes, entities) {

                            $scope.entities = entities;

                            $scope.apply = function () { $mdDialog.hide(self.network.body.nodes[$scope.target].options.label); };
                            $scope.closeClick = function() { $mdDialog.cancel(); };
                        }],
                    locals: { entities: nodes },
                    autoWrap: false,
                    parent: $scope.KeywordNetworkController.isFullscreen() ? angular.element(document.getElementById('keywordNetwork')) : angular.element(document.body)
                }).then(function(response) {
                    var nodeLabels = [];
                    nodeIds.forEach(item => nodeLabels.push(self.network.body.nodes[item].options.label));
                    var duplicates = _.without(nodeLabels, response);
                    // Remove duplicates from the current network view
                    removeNodes(duplicates, self.nodesDataset, self.edgesDataset);
                    // Remove duplicates from the background collection
                    removeNodes(duplicates, self.nodes, self.edges);

                    playRoutes.controllers.KeywordNetworkController.mergeKeywords(response, duplicates).get().then(function(response) {
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
                var max = new VisDataSet(self.edges.get({ filter: function(edge) { return !edge.hidden; }})).max("value");
                if(max){
                    $scope.maxEdgeImportance = max.value;
                }
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
                var cleanNodes = nodesToBeUpdated.map(function(n) { return _.extend(n, { 'shapeProperties': { borderDashes: false }, color: undefined, borderWidth: 3 })});
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
                /*
                playRoutes.controllers.KeywordNetworkController.getEdgeKeywordsKeyword(filters.fulltext, filters.facets, filters.entities, filters.timeRange, filters.timeRangeX, edge.from, edge.to, self.numEdgeKeywords).get().then(function(response) {
                    var formattedTerms = response.data.map(function(t) { return '' +  t.term + ': ' + t.score; });

                    var docTip = '<p>Occurs in <b>' + edge.value + '</b> documents</p>';
                    var keywordTip = '<p><b>Keywords</b></p><ul><li>' + formattedTerms.join('</li><li>') + '</li></ul>';
                    var tooltip = docTip + keywordTip;

                    self.edgesDataset.update({ id: edge.id, title: tooltip });
                    // Only update background collection after stabilization.
                    // if(!$scope.loading) {
                    //    self.edges.update({ id: event.edge, title: keywords });
                    //}
                });
                */
            }

            function hoverHighlight(node){
                // Give new nodes a white and dashed border
                var modifiedNodes = [{ id: node.id, shapeProperties: { borderDashes: [5, 5] }, color: { border: 'red' }, borderWidth: 4 }];
                self.nodesDataset.update(modifiedNodes);
            }

            function hoverNode(event) {
                var node = self.nodesDataset.get(event.node);
                var nodeId = '' + nodeId;
                // Only fetch keywords if not already fetched
                // if(_.has(node, "title")) {
                //     return;
                // }

                var filters = currentFilter();
                playRoutes.controllers.KeywordNetworkController.getNeighborCountsKeyword(filters.fulltext, filters.facets, filters.entities, filters.timeRange, filters.timeRangeX, node.id).get().then(function(response) {
                    // var formattedTerms = response.data.map(function(t) { return '' +  t.type + ': ' + t.count; });

                    if(node.type != 'TAG'){
                        var docTip = '<p>Occurs in <b>' + node.value + ' </b>documents</p>'; // <p>Type: <b>' + node.type + '</b></p>';
                        // var neighborTip = '<p><b>Neighbors</b></p><ul><li>' + formattedTerms.join('</li><li>') + '</li></ul>';
                        var tooltip = docTip; // + neighborTip;
                    }

                    self.nodesDataset.update({ id: node.id, title: tooltip });
                });

                if(node.type == 'KEYWORD'){
                    client.search({
                        index: $scope.indexName,
                        type: 'document',
                        body: {
                            query: {
                                bool: {
                                    should: [
                                        {
                                            term: {
                                                "Entities.EntId": {
                                                    value: node.id
                                                }
                                            }
                                        }
                                    ]
                                }
                            }
                        }
                    }).then(function (resp) {
                        if(resp.hits.hits[0]) {
                            let entities = resp.hits.hits[0]._source.Entities;
                            if (entities) {
                                EntityService.highlightEntities(entities);
                            }
                        }
                    }, function (error) {
                        console.trace(error.message);
                    });
                }
                else if (node.type == 'TAG'){
                    for(let tag of $scope.currentTags){
                        if(tag.label == node.label){
                            client.search({
                                index: $scope.indexName,
                                type: 'document',
                                id: tag.documentId,
                                body: {
                                    query: {
                                        match: {
                                            _id: tag.documentId
                                        }
                                    }
                                }

                            }).then(function (resp) {
                                if(resp.hits.hits[0]) {
                                    let entities = resp.hits.hits[0]._source.Entities;
                                    if (entities) {
                                        EntityService.highlightEntities(entities);
                                    }
                                }
                            });
                        }
                    }
                }
            }

            function blurNode(event) {
                EntityService.removeEntityNodeHighlight();
            }

            $scope.removeKeywordNodeHighlight = function(){
                removeNodeHighlight(self.nodes, self.nodes);
                removeNodeHighlight(self.nodesDataset, self.nodesDataset);
            };

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
                else if(!_.isUndefined(nodeIdOpt) && self.network && self.network.body.nodes[nodeIdOpt].options.type != 'TAG') {
                    self.network.selectNodes([nodeIdOpt]);
                    showContextMenu(_.extend(position, { id: nodeIdOpt }), self.singleNodeMenu);
                // Edge selected
                } else if(!_.isUndefined(edgeIdOpt)) {
                    self.network.selectEdges([edgeIdOpt]);
                    if(self.network && self.network.body.edges[edgeIdOpt].from.options.type != 'TAG' && self.network.body.edges[edgeIdOpt].to.options.type != 'TAG'){
                        showContextMenu(_.extend(position, { id: edgeIdOpt }), self.edgeMenu);
                    }
                }
                else {
                    // Nop
                }
            }

            function showContextMenu(params, menuEntries) {
                var container = document.getElementById('mykeywordnetwork');

                var offsetLeft = container.offsetLeft;
                var offsetTop = container.offsetTop;

                self.popupMenu = document.createElement("div");
                self.popupMenu.className = 'popupMenu';
                self.popupMenu.style.left = params.x + $("#documents-view").width() + $("#mynetwork").width() + 'px';
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

            function getEdgeByLabel(label, dataset) {
                for (var i = 0; i < dataset.length; i++) {
                    if (dataset[i].label == label) {
                        return dataset[i].id
                    }
                }
            }

            function getKeywordById(id, dataset) {

                for(let item of dataset._data) {
                    if(item.id == id) {
                        return item.label;
                    }
                }

                for (var i = 0; i < dataset._data.length; i++) {
                    if (dataset._data[i].id == id) {
                        return dataset._data[i].label
                    }
                }
            }
        }]);
});
