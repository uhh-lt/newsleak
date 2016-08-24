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
                // Order: locations, orginaziations, persons, misc
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
        .controller('FancyNetworkController', ['$scope', '$timeout', 'VisDataSet', 'playRoutes', 'ObserverService', function ($scope, $timeout, VisDataSet, playRoutes, ObserverService) {

            var self = this;

            self.nodes = [];
            self.edges = [];

            self.nodesDataset = new VisDataSet([]);
            self.edgesDataset = new VisDataSet([]);


            $scope.options = {
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
                    }
                },
                physics: {
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
                },
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

            $scope.graphOptions = $scope.options;
            $scope.graphEvents = {
                // stabilizationIterationsDone
                "startStabilizing": stabilizationStart,
                "stabilized": stabilizationDone,
                "onload": onNetworkLoad
            };



            $scope.loaded = reload;


            $scope.observerService.registerObserverCallback(function() {
                console.log("update network");
                reload();
            });

            function onNetworkLoad(network) {
                self.network = network;
            }

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
                    // Activate physics for new graph simulation
                    $scope.options['physics'] = true;
                    console.log(response.data);
                    self.nodes = response.data.entities.map(function(n) {
                        return {id: n.id, label: n.label, value: n.count, group: n.group };
                    });
                    self.nodesDataset.clear();
                    self.nodesDataset.add(self.nodes);

                    self.edges = response.data.relations.map(function(n) {
                        return {from: n[0], to: n[1], value: n[2] };
                    });
                    self.edgesDataset.clear();
                    self.edgesDataset.add(self.edges);

                    // Bring graph in current viewport
                    self.network.fit();
                });
            }

            function stabilizationStart() {
                console.log("Stab start");
            }

            // Event Callbacks
            function stabilizationDone() {
                console.log("Stab done");
                // Do not disable physics for controller initialization
               // if(self.nodesDataset.length > 0 && self.edgesDataset.length > 0) {
                    $scope.options['physics'] = false;
                    console.log("Disabled physics");
              // }
            }
     }]);
});
