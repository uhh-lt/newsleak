/*
 * Copyright 2016 Technische Universitaet Darmstadt
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

define([
    'angular',
    'ngSanitize',
    '../../factory/appData',
    '../../factory/util',
    'ui-bootstrap',
    'toggle-switch'
], function (angular) {
    'use strict';

    angular.module('myApp.source', ['play.routing', 'ngSanitize', 'toggle-switch', 'ui.bootstrap'])
        .config(['$uibTooltipProvider', function($uibTooltipProvider) {
            //$uibTooltipProvider.setTriggers({'mouseenter': 'mouseleave'});
            console.log("config");
        }])
        .controller('SourceController',
            [
                '$scope',
                '$http',
                '$compile',
                '$templateRequest',
                '$sce',
                'playRoutes',
                'util',
                '_',
                'sourceShareService',
                'highlightShareService',
                'graphPropertiesShareService',
                'uiShareService',
                'ObserverService',
                function ($scope,
                          $http,
                          $compile,
                          $templateRequest,
                          $sce,
                          playRoutes,
                          util,
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
                    $scope.noMoreDocs = false;

                    $scope.highlightState = {on: true};

                    $scope.observer = ObserverService;
                    /**
                     * subscribe entity and metadata filters
                     */
                    $scope.observer_subscribe_entity = function(items) { $scope.entityFilters = items};
                    $scope.observer_subscribe_metadata = function(items) { $scope.metadataFilters = items};
                    $scope.observer_subscribe_fulltext = function(items) { $scope.fulltextFilters = items};
                    $scope.observer.subscribeItems($scope.observer_subscribe_entity,"entity");
                    $scope.observer.subscribeItems($scope.observer_subscribe_metadata,"metadata");
                    $scope.observer.subscribeItems($scope.observer_subscribe_fulltext,"fulltext");

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
                    $scope.updateDocumentList = function() {
                        $scope.docsLoading = true;
                        console.log("reload doc list");
                        var entities = [];
                        angular.forEach($scope.entityFilters, function(item) {
                            entities.push(item.data.id);
                        });
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
                            facets = [{'key':'dummy','data': []}];
                        }
                        var fulltext = [];
                        angular.forEach($scope.fulltextFilters, function(item) {
                           fulltext.push(item.data.name);
                        });
                        playRoutes.controllers.DocumentController.getDocs(fulltext,facets,entities,$scope.observer.getTimeRange()).get().then(function(x) {
                            // console.log(x.data);
                            $scope.sourceShared.reset(0);
                            $scope.sourceShared.addDocs(x.data.docs);
                            $scope.hits = x.data.hits;
                            $(".docs-ul").scrollTop(0);
                            $scope.docsLoading = false;
                        });
                    };

                    //initial document list load
                    $scope.updateDocumentList();

                    $scope.loadMore = function () {
                        console.log("reload doc list");
                        $scope.docsLoading = true;
                        var entities = [];
                        angular.forEach($scope.entityFilters, function(item) {
                            entities.push(item.data.id);
                        });
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
                            facets = [{'key':'dummy','data': []}];
                        }
                        var fulltext = [];
                        angular.forEach($scope.fulltextFilters, function(item) {
                            fulltext.push(item.data.name);
                        });
                        playRoutes.controllers.DocumentController.getDocs(fulltext,facets,entities,$scope.observer.getTimeRange()).get().then(function(x) {
                            $scope.sourceShared.addDocs(x.data.docs);
                            $scope.docsLoading = false;
                        });
                    };

                    //subscribe to update document list on filter change
                    $scope.observer.registerObserverCallback($scope.updateDocumentList);


                    /**
                     * Whenever the array that holds the words to highlight is changed, update
                     * all open source documents where highlight mode is currently enabled.
                     */
                    $scope.$watch('highlightShared.wasChanged', function () {
                        if ($scope.highlightShared.wasChanged) {
                            for (var i = 0; i < $scope.sourceShared.openDocuments.contents.length; i++) {
                                if ($scope.sourceShared.openDocuments.displayHighlightedText[i]) {
                                    $scope.sourceShared.openDocuments.contents[i] =
                                        $sce.trustAsHtml(getHighlightedText($scope.sourceShared.openDocuments.nonHighlightedContents[i]));
                                }
                            }
                            $scope.highlightShared.wasChanged = false;
                        }
                    });


                    // Retrieve the content of the source view
                    $templateRequest('assets/partials/source.html').then(function (html) {
                        $scope.sourceShared.documentOperations = html;
                    });

                    /**
                     * Applies the highlighting and underlining to the given text.
                     *
                     * @param toFormat - The string to apply the highlighting and underlining to.
                     * @return string - Returns the highlighted and underlined text.
                     */
                    var getHighlightedText = function (toFormat) {
                        toFormat = toFormat.replace(/\n/g, "<br>");
                        var highlightedText = toFormat;
                        for (var i = 0; i < graphPropertiesShareService.categoryColors.length; i++) {
                            highlightedText = util.highlight(
                                highlightedText,
                                $scope.highlightShared.wordsToHighlight[i],
                                $scope.highlightShared.wordsToUnderline[i],
                                graphPropertiesShareService.categoryColors[i]
                            );
                        }
                        return highlightedText;
                    };


                    /**
                     * This function is used to format the source of a document
                     * in a way to display it on the website. At the moment, this
                     * means to insert line breaks.
                     *
                     * @param toFormat - The source to format.
                     * @return string - Returns the formatted text.
                     */
                    var getFormattedSource = function (toFormat) {
                        return toFormat.replace(/\n/g, '<br>');
                    };


                    /**
                     *  Target of the "Load more documents" button
                     */
                    $scope.retrieveNextDocSet = function () {
                        $scope.sourceShared.fetchNextDocs();
                    };

                    /**
                     * This method loads the body / content and writes it into the shared
                     * scope variable documentBody. As a result the source view will be updated.
                     * @param docId The ID of the document to get the content for.
                     */
                    $scope.loadFullDocument = function (docId) {
                        /**
                         * Determine the index of a tab by a document by ID.
                         * @param docId The document to get the index for by ID. Make sure that this ID
                         * exists in the $scope.sourceShared.openDocuments.IDs array.
                         * @return {number} Returns the position that the given document ID has. The
                         * returned value already respects that the first opened document is the third tab.
                         */
                        function getIndexForDocID(docId) {
                            var index = 2;
                            for (var i = 0; i < $scope.sourceShared.openDocuments.IDs.length; i++) {
                                if ($scope.sourceShared.openDocuments.IDs[i] != undefined) {
                                    if ($scope.sourceShared.openDocuments.IDs[i] == docId) {
                                        return index;
                                    } else {
                                        index++;
                                    }
                                }
                            }
                        }

                        /**
                         * Appends a new tab which gets the title of docId.
                         * @param docId The document ID which will be used as title as well as
                         * href attribute as it is unique.
                         */
                        function appendNewTab(docId) {
                            $('#network-maps-container .tab-content .active').removeClass('active').removeClass('in');
                            $('#network-maps-container .nav-tabs li.active').removeClass('active');
                            $('#network-maps-container .nav-tabs').append(
                                '<li class="active" role="presentation">' +
                                '<a aria-expanded="true" href="#' + docId + '" aria-controls="' + docId + '" role="tab" data-toggle="tab">' +
                                '<button class="close closeTab" type="button">×</button>' + docId +
                                '</a></li>'
                            );
                        }

                        /**
                         * Appends the content of the new tab to the HTML DOM.
                         * @param docId The document ID which will be used to connect the tab to
                         * this content pane.
                         */
                        function appendNewTabContent(docId) {
                            $('#network-maps-container .tab-content').append(
                                '<div role="tabpanel" class="tab-pane active" id="' + docId + '">' +
                                '</div>'
                            );
                            // The buttons need to be added separately because else the DOM elements would
                            // not be parsed
                            $('#network-maps-container .tab-content .tab-pane:last-child').append($compile($scope.sourceShared.documentOperations)($scope));
                            // -3 because there are 2 tabs by default and this which was just added
                            var index = $scope.sourceShared.openDocuments.IDs.length - 1;
                            var tabContent = "<div class='document-source-content' ng-bind-html='sourceShared.openDocuments.contents[" + index + "]'></div>";
                            $('#network-maps-container .tab-content .tab-pane:last-child').append($compile(tabContent)($scope));
                            if ($scope.highlightState.on) {
                                $scope.highlightShared.wasChanged = true;
                            }
                        }

                        // In case the document was already loaded (index > -1), open that tab
                        var index = $scope.sourceShared.openDocuments.IDs.indexOf(docId);
                        if (index > -1) {
                            index = getIndexForDocID(docId);
                            $('#network-maps-container ul.nav-tabs a:nth(' + index + ')').tab('show');
                        }
                        // Document body needs to be fetched
                        else {
                            playRoutes.controllers.DocumentController.getDocById(docId).get().then(function (response) {
                                $scope.sourceShared.openDocuments.IDs.push(docId);
                                $scope.sourceShared.openDocuments.contents.push(getFormattedSource(response.data[2]));
                                $scope.sourceShared.openDocuments.nonHighlightedContents.push(getFormattedSource(response.data[2]));
                                $scope.sourceShared.openDocuments.displayHighlightedText.push($scope.highlightState.on);
                                // Append a new tab and add the content
                                appendNewTab(docId);
                                appendNewTabContent(docId);
                            });
                        }
                    };


                    /**
                     * Target for the click of "Toggle Highlighting".
                     */
                    $scope.toggleHighlighting = function () {
                        // Show content depending on highlight.
                        // All documents have to be updated since the toggle is not
                        // specific for one document but for all simultaneously.
                        for (var i = 0; i < $scope.sourceShared.openDocuments.contents.length; i++) {
                            if ($scope.sourceShared.openDocuments.IDs[i] != undefined) {
                                if ($scope.highlightState.on) {
                                    $scope.sourceShared.openDocuments.contents[i] = $sce.trustAsHtml(getHighlightedText($scope.sourceShared.openDocuments.nonHighlightedContents[i]));
                                    $scope.sourceShared.openDocuments.displayHighlightedText[i] = true;
                                }
                                else {
                                    $scope.sourceShared.openDocuments.contents[i] = $sce.trustAsHtml($scope.sourceShared.openDocuments.nonHighlightedContents[i]);
                                    $scope.sourceShared.openDocuments.displayHighlightedText[i] = false;
                                }
                            }
                        }
                    };


                    /**
                     * With this function the user can create annotations in the document.
                     */
                    $scope.annotateDocument = function () {
                        console.log("annotateSelected TODO");
                        var annotation = $('#annotateDocumentInput').val();
                        alert("TODO: annotate selected --> " + annotation);
                    };

                    /**
                     * With this function the user can create a new entity with the selected
                     * text passage.
                     */
                    $scope.createEntity = function () {
                        console.log("createEntity TODO");
                        alert("TODO: create a entity");
                    };

                    /**
                     * This function is used for autocompleting the tags
                     */
                    $scope.autocomplete = function (query) {
                        // filter tags, only show those that contain query
                        $scope.searchTags = [];
                        $("#autocomplete").css('z-index','1000');

                        if(query.length >= 3) playRoutes.controllers.SearchController.getAutocomplete(query).get().then(
                            function (tags) {
                                var limit = 0;

                                tags.data.entities.forEach
                                (
                                    function (currentValue, index, array) {
                                        if (limit == 10) {
                                            return;
                                        }

                                        $scope.searchTags.push(
                                            {
                                                id: currentValue[0],
                                                text: currentValue[1],
                                                type: currentValue[2],
                                                color: graphPropertiesShareService.categoryColors[
                                                    graphPropertiesShareService.getIndexOfCategory(
                                                        currentValue[2]
                                                    )
                                                    ]
                                            });
                                        limit = limit + 1;
                                    }
                                );
                            }
                        );
                    };

                    $scope.resetAutoComplete = function() {
                        $scope.searchQuery = "";
                    };

                    $scope.addFilter = function(item) {
                        $scope.observer.addItem({
                            type: 'entity',
                            data: {
                                id: item.id,
                                name: item.text,
                                type: item.type,
                                view: 'search'
                            }
                        });
                        console.log("Added filter");

                        //TODO: replace tagService with observer
                        $scope.addedTag(item);
                        $("#autocomplete").css('z-index','-1');
                        $scope.searchTags = [];
                    };

                    $(".docs-ul").on('scroll',function() {
                        if(!$scope.docsLoading) {
                            if(($(this).find("ul").height() - $(this).scrollTop()) < 600)
                                $scope.loadMore();
                        }


                    });

                    // The close click on a tab
                    $(document).on('click', '.nav-tabs .closeTab', function () {
                        // The following lines were taken from
                        // http://stackoverflow.com/questions/18096724/how-to-add-a-close-icon-in-bootstrap-tabs
                        var tabContentId = $(this).parent().attr('href');
                        // Remove the leading #
                        tabContentId = tabContentId.substr(1, tabContentId.length);
                        $(this).parent().parent().remove();
                        $('#network-maps-container ul.nav-tabs a:last').tab('show');
                        $('#' + tabContentId).remove();
                        // Also update the array; no splice / remove because the content of the tabs
                        // depends on the index @see loadFullDocument
                        var index = $scope.sourceShared.openDocuments.IDs.indexOf(parseInt(tabContentId));
                        if (index > -1) {
                            $scope.sourceShared.openDocuments.IDs[index] = undefined;
                            $scope.sourceShared.openDocuments.contents[index] = undefined;
                            $scope.sourceShared.openDocuments.nonHighlightedContents[index] = undefined;
                            $scope.sourceShared.openDocuments.displayHighlightedText[index] = undefined
                        }
                    });
                }
            ]);

});
