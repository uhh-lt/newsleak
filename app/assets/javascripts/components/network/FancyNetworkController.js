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


            self.options = {
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
            $scope.observerService.subscribeItems($scope.observer_subscribe_fulltext, "fulltext");


            $scope.graphOptions = self.options;
            $scope.graphEvents = {
                // stabilizationIterationsDone
                "stabilized": stabilizationDone,
                "onload": onNetworkLoad
            };

            $scope.graphData = {
                nodes: self.nodesDataset,
                edges: self.edgesDataset
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

                var fulltext = $scope.fulltextFilters.map(function(f) { return f.data.name; });
                // TODO: Why do we need this dummy?!
                playRoutes.controllers.EntityController.getEntities(fulltext,[{'key':'dummy','data': []}],[],$scope.observerService.getTimeRange(),10,"").get().then(function(response) {
                    self.nodes = response.data.map(function(n) {
                        return {id: n.id, label: n.name };
                    });
                    /*self.nodes = [
                        {id: 1, label: 'Node 1'},
                        {id: 2, label: 'Node 2'},
                        {id: 3, label: 'Node 3'},
                        {id: 4, label: 'Node 4'},
                        {id: 5, label: 'Node 5'}
                    ];*/
                    self.nodesDataset.add(self.nodes);

                   // self.edges = [{from: 323644, to: 902475}];
                    // Not connected nodes slows the convergence down :(
                    //self.edges = [{from: 1, to: 2}];
                    /*self.edges = [
                        {from: 1, to: 3},
                        {from: 1, to: 2},
                        {from: 2, to: 4},
                        {from: 2, to: 5}
                    ];*/
                    //self.edgesDataset.add(self.edges);

                    playRoutes.controllers.NetworkController.getRelations(self.nodesDataset.getIds(), 1, 81337).get().then(function(response) {
                        var ids = self.nodesDataset.getIds();
                        self.edges = response.data.map(function(v) {
                                var sourceNode = ids.find(function(node){return v[1] == node});
                                var targetNode = ids.find(function(node){return v[2] == node});
                                if(sourceNode == undefined || targetNode == undefined) {
                                    return;
                                }
                                console.log({from: sourceNode, to: targetNode });
                                return {from: sourceNode, to: targetNode };
                            }
                        );
                        console.log(self.edges);
                        self.edgesDataset.add(self.edges);
                    });


                    //$timeout(function() { self.options['physics'] = false; }, 0, false);
                    self.network.fit();
                });




                  /*  self.nodes = [
                        {id: 1, label: 'Node 1'},
                        {id: 2, label: 'Node 2'},
                        {id: 3, label: 'Node 3'},
                        {id: 4, label: 'Node 4'},
                        {id: 5, label: 'Node 5'}
                    ];
                self.nodesDataset.add(self.nodes);

                self.edges = [
                    {from: 1, to: 3},
                    {from: 1, to: 2},
                    {from: 2, to: 4},
                    {from: 2, to: 5}
                ];
                self.edgesDataset.add(self.edges);*/


            }

            // Event Callbacks
            function stabilizationDone() {
                if(self.nodesDataset.length > 0 && self.edgesDataset.length > 0) {
                    self.options['physics'] = false;
                    console.log("Disabled physics");
                }
            }
     }]);
});
