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
        .controller('FancyNetworkController', ['$scope', '$timeout', '$mdDialog', 'VisDataSet', 'playRoutes', 'ObserverService', '_', function ($scope, $timeout, $mdDialog, VisDataSet, playRoutes, ObserverService, _) {

            var self = this;

            /* Entity types to their id */
            self.types = {};

            /* Background collection */
            self.nodes = new VisDataSet([]);
            self.edges = new VisDataSet([]);

            /* Graph collection filtered during runtime */
            self.nodesDataset = new VisDataSet([]);
            self.edgesDataset = new VisDataSet([]);

            self.physicOptions = {
                forceAtlas2Based: {
                    gravitationalConstant: -220,
                    centralGravity: 0.01,
                    springConstant: 0.02,
                    springLength: 110,
                    damping: 0.4,
                    avoidOverlap: 0
                },
                barnesHut: {
                    gravitationalConstant: -50,
                    centralGravity: 0.01,
                    springConstant: 0.08,
                    damping: 0.4
                },
                maxVelocity: 146,
                solver: 'forceAtlas2Based',
                //solver: 'barnesHut',
               // timestep: 0.35,
                stabilization: {
                    enabled: true,
                    fit: false,
                    iterations: 2000
                    //updateInterval: 25
                },
                adaptiveTimestep: true
            };

            self.options = {
                nodes : {
                    shape: 'dot',
                    size: 10,
                    shadow: true,
                    //mass: 1.7,
                    font: {
                        /*strokeWidth: 3,
                        strokeColor: 'white'*/
                    },
                    scaling: {
                        label: {
                            min: 30,
                            max: 45
                        }
                    }
                },
                edges: {
                    color: {
                        color: 'rgb(169,169,169)', //'rgb(220,220,220)',
                        highlight: 'blue',//,
                        //opacity: 0.5
                    },
                    smooth: {type:'continuous'}
                },
                physics: self.physicOptions,
                layout: {
                    improvedLayout: false
                },
                interaction: {
                    tooltipDelay: 200,
                    hideEdgesOnDrag: true,
                    navigationButtons: true,
                    keyboard: false
                }
            };

            self.nodeMenu = [{
                    title: 'Add as filter',
                    action: function(value, nodeId) { addNodeFilter(nodeId); }
                }, {
                    title: 'Edit node',
                    action: function(value, nodeId) { editNode(nodeId); }
                }, {
                    title: 'Hide',
                    action: function(value, nodeId) { hideNode(nodeId); }
                }, {
                    title: 'Blacklist',
                    action: function(value, nodeId) { alert(value + nodeId); }
            }];

            self.edgeMenu = [{
                title: 'Add as filter',
                action: function(value, edgeId) { addEdgeFilter(edgeId); }
            }];

            $scope.observerService = ObserverService;
            // TOdo camelcase, only add if the event was add

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

            $scope.graphData = {
                nodes: self.nodesDataset,
                edges: self.edgesDataset
            };

            $scope.graphOptions = self.options;
            $scope.graphEvents = {
                "startStabilizing": stabilizationStart,
                "stabilized": stabilizationDone,
                //"stabilizationIterationsDone": stabilizationDone,
                "onload": onNetworkLoad,
                "dragEnd": dragNodeDone,
                //"oncontext": showContextMenu,
                "oncontext": onContext,
                "click": clickEvent,
                "dragging": dragEvent
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
                var fulltext = $scope.fulltextFilters.map(function(v) { return v.data.name; });
                var entities = $scope.entityFilters.map(function(v) { return v.data.id; });
                var facets = $scope.observerService.getFacets();

                 var fraction = [
                    {"key": "PER", "data": $scope.numPer },
                    {"key": "ORG", "data": $scope.numOrg },
                    {"key": "LOC", "data": $scope.numLoc },
                    {"key": "MISC", "data": $scope.numMisc }
                ];

                playRoutes.controllers.NetworkController.induceSubgraph(fulltext, facets, entities, $scope.observerService.getTimeRange(), fraction, []).get().then(function(response) {
                        // Enable physics for new graph data
                        applyPhysicsOptions(self.physicOptions);
                        $scope.loading = true;

                        // Assignment from entity types to their id for coloring
                        self.types = response.data.types;

                        var originalMax = _.max(response.data.entities, function(n) { return n.count; }).count;
                        var originalMin = _.min(response.data.entities, function(n) { return n.count; }).count;

                        var nodes = response.data.entities.map(function(n) {
                            // See css property div.network-tooltip for custom tooltip styling
                            var title = 'Co-occurrence: ' + n.count + '<br>Typ: ' + n.type;
                            // map counts to interval [1,2] for nodes mass
                            var mass = ((n.count - originalMin) / (originalMax - originalMin)) * (2 - 1) + 1;
                            // If all nodes have the same occurrence assign same mass. This also prevents errors
                            // for wrong interval mappings e.g. [1,1] to [1,2] yields NaN for the mass.
                            if(originalMin == originalMax) mass = 1;
                            return {
                                id: n.id,
                                label: n.label,
                                type: n.type,
                                value: n.count,
                                group: n.group,
                                title: title,
                                mass: mass
                            };
                        });
                        self.nodes.clear();
                        self.nodesDataset.clear();
                        self.nodesDataset.add(nodes);

                        var edges = response.data.relations.map(function(n) {
                            return {from: n[0], to: n[1], value: n[2] };
                        });

                        self.edges.clear();
                        self.edgesDataset.clear();
                        self.edgesDataset.add(edges);

                        // Update the maximum edge importance slider value
                        $scope.maxEdgeImportance = (self.edgesDataset.length > 0) ? self.edgesDataset.max("value").value : 0;
                        console.log("" + self.nodesDataset.length + " nodes loaded");
                    });
                // Bring graph before stabilization in current viewport
                $timeout(function() { self.network.fit(); }, 0, false);
            };


            // Initialize graph
            $scope.reloadGraph();

            $scope.observerService.registerObserverCallback(function() {
                console.log("update network");
                $scope.reloadGraph();
            });


            function hideNode(nodeId) {
                // Hide given node
                self.nodesDataset.remove(nodeId);
                self.nodes.update({id: nodeId, hidden: true});

                var adjacentEdges = self.edges.get({
                    filter: function(edge) {
                        return (edge.to == nodeId || edge.from == nodeId)
                    }
                }).map(function(edge) { return _.extend(edge, { hidden: true }); });
                // Hide adjacent edges
                self.edges.update(adjacentEdges);
                self.edgesDataset.remove(adjacentEdges);
                // Update new edge max value from non hidden edges
                var max = new VisDataSet(self.edges.get({ filter: function(edge) { return !edge.hidden; }})).max("value").value;
                $scope.maxEdgeImportance = max;
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
                    locals: { e: entity }
                }).then(function(response) {
                    // Adapt tooltip and node color
                    var modified = _.extend(response, {
                        group: self.types[response.type],
                        title: 'Co-occurrence: ' + response.value + '<br>Typ: ' + response.type
                    });
                    self.nodesDataset.update(modified);
                    self.nodes.update(modified);
                    // Store changes
                    playRoutes.controllers.NetworkController.changeEntityNameById(response.id, response.label).get().then(function(response) { /* Error handling */ });
                    playRoutes.controllers.NetworkController.changeEntityTypeById(response.id, response.type).get().then(function(response) { /* Error handling */ });
                }, function() { /* cancel click */ });
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
                // Bring graph in current viewport
                $timeout(function() { self.network.fit(); }, 0, false);
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
                    self.nodes = new VisDataSet(self.nodesDataset.get());
                    self.edges = new VisDataSet(self.edgesDataset.get());

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
                }
            }

            function handleEdgeSlider(newValue, oldValue) {
                closeContextMenu();
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
                    self.network.stabilize();
                }
            }

            function onContext(params) {
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
            }

            function showContextMenu(params, menu) {
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
            }

            function closeContextMenu() {
                if (self.popupMenu !== undefined) {
                    self.popupMenu.parentNode.removeChild(self.popupMenu);
                    self.popupMenu = undefined;
                }
            }
        }]);
});
