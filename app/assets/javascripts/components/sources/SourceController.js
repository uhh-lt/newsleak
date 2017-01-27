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
    'ngMaterial'
], function (angular) {
    'use strict';
    /**
     * source module:
     * list of documents for current sub collection.
     * - infinite scroll
     * - open full document
     */
    angular.module('myApp.source', ['play.routing', 'ngSanitize', 'ngMaterial'])
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
                '$mdDialog',
                '$q',
                'playRoutes',
                '_',
                'sourceShareService',
                'ObserverService',
                function ($scope,
                          $http,
                          $timeout,
                          $mdToast,
                          $mdDialog,
                          $q,
                          playRoutes,
                          _,
                          sourceShareService,
                          ObserverService) {

                    $scope.allDocumentsLoadedMsg = 'All matching Documents loaded';
                    $scope.noDocumentsMsg = 'No document found for current applied filters.';

                    $scope.sourceShared = sourceShareService;
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

                    $scope.observer.subscribeReset(function() {
                        $scope.sourceShared.documentsInDB = -1;
                    });


                    /**
                     * load document list for current filtering
                     */
                    $scope.updateDocumentList = function () {
                        $scope.docsLoading = true;
                        $scope.showLoading = true;
                        console.log("reload doc list");
                        $scope.defered = $q.defer();
                        var entities = [];
                        angular.forEach($scope.entityFilters, function (item) {
                            entities.push(item.data.id);
                        });
                        var facets = $scope.observer.getFacets();
                        var fulltext = [];
                        angular.forEach($scope.fulltextFilters, function (item) {
                            fulltext.push(item.data.item);
                        });

                        playRoutes.controllers.DocumentController.getDocs(fulltext, facets, entities, $scope.observer.getTimeRange(),$scope.observer.getXTimeRange()).get().then(function (x) {
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
                            $scope.defered.resolve("suc: docs");
                            $scope.iteratorEmpty = x.data.hits <= 50;
                            $("#vertical-container").css("height", $("#documents-view").height() - 110);

                        });
                        return $scope.defered.promise;
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
                            var facets = $scope.observer.getFacets();
                            var fulltext = [];
                            angular.forEach($scope.fulltextFilters, function (item) {
                                fulltext.push(item.data.item);
                            });
                            playRoutes.controllers.DocumentController.getDocs(fulltext, facets, entities, $scope.observer.getTimeRange()).get().then(function (x) {
                                if (x.data.docs.length == 0 || x.data.docs.length <=50)
                                    $scope.iteratorEmpty = true;
                                else
                                    $scope.sourceShared.addDocs(x.data.docs);
                                $scope.docsLoading = false;

                            });
                        }
                    };

                    $scope.virtualScroll = {
                        getItemAtIndex: function(index) {
                            if (!$scope.docsLoading && !$scope.iteratorEmpty && $scope.sourceShared.documentList.length >= 50 && index >  $scope.sourceShared.documentList.length-3) {
                                $scope.loadMore();
                                return "Loading ...";
                            }
                            return  $scope.sourceShared.documentList[index];
                        },
                        getLength: function() {
                            return  $scope.sourceShared.documentList.length;
                        }
                    };

                    //subscribe to update document list on filter change
                    $scope.observer.registerObserverCallback({ priority: 2, callback: $scope.updateDocumentList });

                    $scope.observer.subscribeReset(function() {
                        return $scope.updateDocumentList();
                    });

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
                                    description: "#" + doc.id,
                                    item: "#" + doc.id
                                }
                            };
                            $scope.observer.addItem(editItem);

                            playRoutes.controllers.EntityController.getEntitiesByDoc(doc.id).get().then(function (response) {
                                // Provide document controller with document information
                                $scope.sourceShared.tabs.push({
                                    id: doc.id,
                                    title: doc.id,
                                    content: doc.content,
                                    highlighted: doc.highlighted,
                                    meta: doc.metadata,
                                    entities: response.data
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

                    $scope.showDocumentToast = function(message) {
                        $mdToast.show(
                            $mdToast.simple()
                                .textContent(message)
                                .position('bottom left')
                                .parent(angular.element('#document'))
                                .hideDelay(3000)
                        );
                    };
                }
            ]);
});
