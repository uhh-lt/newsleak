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
        // Network Controller
        .controller('FancyNetworkController', ['$scope', '$timeout', 'VisDataSet', 'playRoutes', 'ObserverService', '_', function ($scope, $timeout, VisDataSet, playRoutes, ObserverService, _) {

            var self = this;

            self.nodes = [];
            self.edges = [];

            self.nodesDataset = new VisDataSet([]);
            self.edgesDataset = new VisDataSet([]);

            self.physicOptions = {
                forceAtlas2Based: {
                    gravitationalConstant: -50,
                    centralGravity: 0.01,
                    springConstant: 0.08,
                    springLength: 100,
                    damping: 0.4,
                    avoidOverlap: 0
                },
                maxVelocity: 146,
                solver: 'forceAtlas2Based',
                timestep: 0.35,
                stabilization: {
                    enabled: true,
                    iterations: 2000,
                    updateInterval: 25
                }
            };

            self.options = {
                nodes : {
                    shape: 'dot',
                    size: 10,
                    shadow: true,
                    font: {
                        strokeWidth: 3,
                        strokeColor: 'white'
                    }
                },
                edges: {
                    color: {
                        color: 'rgb(220,220,220)',
                        highlight: 'lightblue',
                        opacity: 0.5
                    }//,
                    //smooth: {type:'continuous'}
                },
                physics: self.physicOptions,
                interaction: {
                    tooltipDelay: 200,
                    hideEdgesOnDrag: true,
                    navigationButtons: true,
                    keyboard: false
                }
            };

            $scope.observerService = ObserverService;
            // TOdo camelcase, only add if the event was add

            // TODO: would be cool to have the event type as argument i.e. remove or add
            $scope.observer_subscribe_fulltext = function(items) {
                // TODO: check why map does not work here
                $scope.fulltextFilters = items;//items.map(function(f) { return f.data.name; });
            };
            $scope.observer_subscribe_metadata = function(items) { $scope.metadataFilters = items};
            $scope.observerService.subscribeItems($scope.observer_subscribe_fulltext, "fulltext");
            $scope.observerService.subscribeItems($scope.observer_subscribe_metadata, "metadata");

            $scope.graphData = {
                nodes: self.nodesDataset,
                edges: self.edgesDataset
            };

            $scope.graphOptions = self.options;
            $scope.graphEvents = {
                "startStabilizing": stabilizationStart,
                "stabilized": stabilizationDone,
                "onload": onNetworkLoad,
                "dragEnd": dragNodeDone
            };

            /* Current value of the edge significance slider */
            $scope.edgeImportance = 1;
            /* Maximum edge value of the current underlying graph collection. Updated in reload method */
            $scope.maxEdgeImportance = 80000;
            /* Indicates whether the network is initialized or new data is being loaded */
            $scope.loading = true;

            $scope.loaded = reload;

            $scope.observerService.registerObserverCallback(function() {
                console.log("update network");
                reload();
            });

            $scope.$watch('edgeImportance', handleEdgeSlider);


            function reload() {
                // TODO: We need this in several components helper methods would be nice ... (copied from metadataController)
                var fulltext = $scope.fulltextFilters.map(function(f) { return f.data.name; });
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
                    // TODO Can we get rid of this dummy?!
                    facets = [{'key':'dummy','data': []}];
                }

                playRoutes.controllers.NetworkController.induceSubgraph(fulltext, facets,[],$scope.observerService.getTimeRange(),18,"").get().then(function(response) {
                    // Enable physics for new graph data
                    applyPhysicsOptions(self.physicOptions);
                    $scope.loading = true;

                    var nodes = response.data.entities.map(function(n) {
                        // See css property div.network-tooltip for custom tooltip styling
                        var title = 'Co-occurrence: ' + n.count + '<br>Typ: ' + n.type;
                        return {id: n.id, label: n.label, value: n.count, group: n.group, title: title };
                    });
                    self.nodes = [];
                    self.nodesDataset.clear();
                    self.nodesDataset.add(nodes);

                    var edges = response.data.relations.map(function(n) {
                        return {from: n[0], to: n[1], value: n[2] };
                    });
                    self.edges = [];
                    self.edgesDataset.clear();
                    self.edgesDataset.add(edges);

                    // Update the maximum edge importance slider value
                    $scope.maxEdgeImportance = self.edgesDataset.max("value").value;
                    console.log("" + self.nodesDataset.length + " nodes loaded");

                    //self.network.fit();
                });
                // Bring graph in current viewport
                $timeout(function() { self.network.fit(); }, 0, false);
            }

            function applyPhysicsOptions(options) {
                console.log('Physics simulation on');
                $scope.graphOptions['physics'] = options;
                self.network.setOptions($scope.graphOptions);
            }

            function disablePhysics() {
                console.log('Physics simulation off');
                $scope.graphOptions['physics']  = false;
                // Need to explicitly apply the new options since the automatic
                // watchCollection from angular-visjs seems to be outside of the
                // regular angular update event cycle. It also requires to remove
                // the watchCollection from the options entirely!
                self.network.setOptions($scope.graphOptions);
            }


            // ----------------------------------------------------------------------------------
            // Event Callbacks
            //
            // These callbacks seem to be outside of the angular $digest
            // cycles. To force new $digest use $scope.$apply(function() { /* scope action */ })
            // ---------------------------------------------------------------------------------

            function onNetworkLoad(network) {
                self.network = network;
            }

            function stabilizationStart() {
                console.log("Stabilization start with " + self.nodesDataset.length + " nodes");
            }

            function stabilizationDone() {
                console.log("Stabilization done");
                if(self.nodes.length == 0 && self.edges.length == 0) {
                    self.network.storePositions();
                    self.nodes = self.nodesDataset.get();
                    self.edges = self.edgesDataset.get();

                    // Once the stabilized event is called the first time
                    // the network is initialized. Other stabilizing events
                    // are triggered by the handleEdgeSlider method and only
                    // simulate dynamic edges.
                    $scope.$apply(function() {
                        $scope.loading = false;
                        // reset the current edge slider position, because the new
                        // maximum value could be smaller than the current value.
                        $scope.edgeImportance = 1;
                    });
                }
                disablePhysics();
            }

            function dragNodeDone(event) {
                // Update node positions of the background collection whenever they are moved
                if(event.nodes.length == 1 && $scope.graphOptions['physics'] == false) {
                    var match = _.find(self.nodes, function (obj) {
                        return obj.id == event.nodes[0]
                    });
                    match.x = event.pointer.x;
                    match.y = event.pointer.y;
                }
            }

            function handleEdgeSlider(newValue, oldValue) {
                console.log("Handle slider " + newValue + ", " + oldValue);
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
                    var option = self.physicOptions;
                    option['stabilization'] = { onlyDynamicEdges: true };
                    applyPhysicsOptions(option);

                    var edgesToAdd = new VisDataSet(self.edges).get({
                        filter: function (item) {
                            return (item.value >= newValue)
                        }
                    });

                    var nodeIds = [].concat.apply([], edgesToAdd.map(function(edge) { return [edge.to, edge.from]; }));
                    var nodesToAdd = new VisDataSet(self.nodes).get({
                        filter: function (item) {
                            return (_.contains(nodeIds, item.id));
                        }
                    });

                    self.edgesDataset.update(edgesToAdd);
                    self.nodesDataset.update(nodesToAdd);
                    self.network.stabilize();
                }
            }
        }]);
});
