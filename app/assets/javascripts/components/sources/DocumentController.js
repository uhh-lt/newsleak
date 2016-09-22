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
    '../../factory/util',
    'ui-bootstrap',
    'toggle-switch',
    'ngMaterial',
    'ngMdIcons'
], function (angular) {
    'use strict';

    angular.module('myApp.document', ['play.routing', 'ngSanitize', 'toggle-switch', 'ui.bootstrap', 'ngMaterial', 'ngMdIcons'])
        .controller('DocumentController',
            [
                '$scope',
                '$http',
                '$compile',
                '$templateRequest',
                '$sce',
                '$timeout',
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
                          $timeout,
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


                    $scope.tabs = $scope.sourceShared.tabs;

                    $scope.removeTab = function (tab) {
                        var index = $scope.tabs.indexOf(tab);
                        $scope.tabs.splice(index, 1);

                        $scope.sourceShared.openDocuments.IDs[tab.id] = undefined;
                        $scope.sourceShared.openDocuments.contents[tab.id] = undefined;
                        $scope.sourceShared.openDocuments.nonHighlightedContents[tab.id] = undefined;
                        $scope.sourceShared.openDocuments.displayHighlightedText[tab.id] = undefined;
                    };

                    $scope.highlightState = {on: true};

                    $scope.observer = ObserverService;

                    console.log("Instantiated DocumentController");
                    /**
                     * subscribe entity and metadata filters
                     */
                    $scope.observer_subscribe_entity = function(items) { $scope.entityFilters = items};
                    $scope.observer_subscribe_metadata = function(items) { $scope.metadataFilters = items};
                    $scope.observer_subscribe_fulltext = function(items) { $scope.fulltextFilters = items};
                    $scope.observer.subscribeItems($scope.observer_subscribe_entity,"entity");
                    $scope.observer.subscribeItems($scope.observer_subscribe_metadata,"metadata");
                    $scope.observer.subscribeItems($scope.observer_subscribe_fulltext,"fulltext");


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
                }
            ]);

});
