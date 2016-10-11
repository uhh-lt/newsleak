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
    'ngSanitize',
    'ngMaterial',
    'ngMdIcons'
], function (angular) {
    'use strict';

    angular.module('myApp.source', ['play.routing', 'ngSanitize', 'ngMaterial', 'ngMdIcons'])
        .config(['$uibTooltipProvider', function($uibTooltipProvider) {
            //$uibTooltipProvider.setTriggers({'mouseenter': 'mouseleave'});
            console.log("config");
        }])
        .controller('SourceController',
            [
                '$scope',
                '$http',
                '$timeout',
                '$mdToast',
                'playRoutes',
                '_',
                'sourceShareService',
                'highlightShareService',
                'graphPropertiesShareService',
                'uiShareService',
                'ObserverService',
                function ($scope,
                          $http,
                          $timeout,
                          $mdToast,
                          playRoutes,
                          _,
                          sourceShareService,
                          highlightShareService,
                          graphPropertiesShareService,
                          uiShareService,
                          ObserverService) {

                    $scope.sourceShared = sourceShareService;
                    $scope.highlightShared = highlightShareService;
                    $scope.uiShareService = uiShareService;
                    $scope.graphPropertiesShared = graphPropertiesShareService;
                    $scope.docsLoading = false;
                    $scope.showLoading = false;
                    $scope.iteratorEmpty = false;

                    $scope.popover = {
                        template: 'doc_tooltip_tmpl',
                        placement: 'right',
                        trigger: 'None',
                        isOpen: [],
                        promises: []
                    };

                    $scope.labels = $scope.sourceShared.labels;

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

                    /*

                     //TODO: examples of fetching documents with and without facet
                     playRoutes.controllers.DocumentController.getDocs('Clinton Snowden',[{'key':'Tags','data':['PTER','PREF']},{'key':'Classification','data':['CONFIDENTIAL']}]).get().then(function(x) {
                     //    console.log(x.data);
                     });
                     playRoutes.controllers.DocumentController.getDocs('',[{'key':'dummy','data': []}]).get().then(function(x) {
                     console.log(x.data);
                     });
                     */
                    /**
                     * load document list for current filtering
                     */
                    $scope.updateDocumentList = function () {
                        $scope.docsLoading = true;
                        $scope.showLoading = true;
                        console.log("reload doc list");
                        var entities = [];
                        angular.forEach($scope.entityFilters, function (item) {
                            entities.push(item.data.id);
                        });
                        var facets = $scope.observer.getFacets();
                        var fulltext = [];
                        angular.forEach($scope.fulltextFilters, function (item) {
                            fulltext.push(item.data.name);
                        });

                        playRoutes.controllers.DocumentController.getDocs(fulltext, facets, entities, $scope.observer.getTimeRange()).get().then(function (x) {
                            // console.log(x.data);
                            $scope.sourceShared.reset();
                            $scope.sourceShared.addDocs(x.data.docs);
                            $scope.hits = x.data.hits;
                            if ($scope.sourceShared.documentsInDB == -1){
                                $scope.sourceShared.documentsInDB = $scope.hits;
                            }
                            $(".docs-ul").scrollTop(0);
                            $scope.docsLoading = false;
                            $scope.showLoading = false;
                            $scope.iteratorEmpty = x.data.hits <= 50;
                        });
                    };

                    //initial document list load
                    $scope.updateDocumentList();

                    $scope.loadMore = function () {
                        console.log("reload doc list");
                        if (!$scope.iteratorEmpty) {
                            $scope.docsLoading = true;
                            var entities = [];
                            angular.forEach($scope.entityFilters, function (item) {
                                entities.push(item.data.id);
                            });
                            var facets = [];
                            if ($scope.metadataFilters.length > 0) {
                                angular.forEach($scope.metadataFilters, function (metaType) {
                                    if ($scope.metadataFilters[metaType].length > 0) {
                                        var keys = [];
                                        angular.forEach($scope.metadataFilters[metaType], function (x) {
                                            keys.push(x.data.name);
                                        });
                                        facets.push({key: metaType, data: keys});
                                    }
                                });
                                if (facets == 0) facets = [{'key': 'dummy', 'data': []}];

                            } else {
                                facets = [{'key': 'dummy', 'data': []}];
                            }
                            var fulltext = [];
                            angular.forEach($scope.fulltextFilters, function (item) {
                                fulltext.push(item.data.name);
                            });
                            playRoutes.controllers.DocumentController.getDocs(fulltext, facets, entities, $scope.observer.getTimeRange()).get().then(function (x) {
                                if (x.data.docs.length == 0)
                                    $scope.iteratorEmpty = true;
                                else
                                    $scope.sourceShared.addDocs(x.data.docs);
                                $scope.docsLoading = false;

                            });
                        }
                    };

                    //subscribe to update document list on filter change
                    $scope.observer.registerObserverCallback($scope.updateDocumentList);

                    $scope.loadFullDocument = function (doc) {
                        // Focus open tab if document is already opened
                        if($scope.isDocumentOpen(doc.id)) {
                            var index = _.findIndex($scope.sourceShared.tabs, function(t) { return t.id == doc.id; });
                            // Skip first network tab
                            $scope.selectedTab.index = index + 1;
                        } else {
                            var editItem = {
                                type: 'openDoc',
                                data: {
                                    id: doc.id,
                                    name: "#" + doc.id
                                }
                            };
                            $scope.observer.addItem(editItem);

                            playRoutes.controllers.DocumentController.getDocById(doc.id).get().then(function (response) {
                                var content = response.data[2];
                                playRoutes.controllers.EntityController.getEntitiesByDoc(doc.id).get().then(function (response) {
                                    // Provide document controller with document information
                                    $scope.sourceShared.tabs.push({
                                        id: doc.id,
                                        title: doc.id,
                                        content: content,
                                        meta: doc.metadata,
                                        entities: response.data
                                    });
                                });
                            });
                        }
                    };


                    $scope.isDocumentOpen = function (id) {
                        var index = _.findIndex($scope.sourceShared.tabs, function(t) { return t.id == id; });
                        return index != -1;
                    };

                    $scope.selectedTagChange = function(label) {
                        // Search tag was removed
                        if(_.isUndefined(label)) {
                            // Restore list to match filters again
                            $scope.updateDocumentList();
                        // Adjust list to match selected label
                        } else {
                            playRoutes.controllers.DocumentController.getDocsByLabel(label).get().then(function (response) {
                                $scope.sourceShared.reset();
                                $scope.sourceShared.addDocs(response.data.docs);
                                $scope.hits = response.data.hits;
                                $(".docs-ul").scrollTop(0);
                                $scope.iteratorEmpty = $scope.hits <= 50;
                            });
                        }
                    };

                    $scope.querySearch = function(query) {
                        var result = query ? $scope.labels.filter(createFilterFor(query)) : $scope.labels;
                        return result;
                    };

                    function createFilterFor(query) {
                        var lowercaseQuery = angular.lowercase(query);
                        return function filterFn(label) {
                            return (label.toLowerCase().indexOf(lowercaseQuery) === 0);
                        };
                    }

                    $(".docs-ul").on('scroll', function () {
                        if (!$scope.docsLoading) {
                            if (($(this).find("ul").height() - $(this).scrollTop()) < 1000)
                                $scope.loadMore();
                        }
                    });

                    $scope.showAllLoaded = function() {
                        $mdToast.show(
                            $mdToast.simple()
                                .textContent('All matching Documents loaded!')
                                .position('bottom left')
                                .parent(angular.element('#document'))
                                .hideDelay(3000)
                        );
                    };

                    $scope.hidePopover = function (id) {
                        $scope.popover.promises[id] = $timeout(function () {
                            $scope.hideFunction(id)
                        }, 10);
                    };

                    $scope.showPopover = function (id) {
                        if ($scope.popover.promises[id] != undefined) $timeout.cancel($scope.popover.promises[id]);
                        $scope.popover.isOpen[id] = true;
                    };

                    $scope.hideFunction = function (x) {
                        $scope.popover.isOpen[x] = false;
                    };
                }
            ]);
});
