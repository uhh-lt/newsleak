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
    'ngMaterial',
    'ngMdIcons'
], function (angular) {
    'use strict';

    angular.module('myApp.document', ['play.routing', 'ngSanitize', 'ngMaterial', 'ngMdIcons'])
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
                    };

                    $scope.observer = ObserverService;


                    /**
                     * Applies the highlighting and underlining to the given text.
                     *
                     * @param toFormat - The string to apply the highlighting and underlining to.
                     * @return string - Returns the highlighted and underlined text.
                     */
                    var getHighlightedText = function (toFormat, wordsToUnderline) {
                        toFormat = toFormat.replace(/\n/g, "<br>");
                        var highlightedText = toFormat;
                        for (var i = 0; i < graphPropertiesShareService.categoryColors.length; i++) {
                            highlightedText = util.highlight(
                                highlightedText,
                                [],
                                wordsToUnderline[i],
                                graphPropertiesShareService.categoryColors[i]
                            );
                        }
                        return highlightedText;
                    };

                    $scope.renderDoc = function(doc) {

                        var marker = [[], [], [], []];
                        doc.entities.forEach(function(e) {
                            var index = graphPropertiesShareService.getIndexOfCategory(e.type);
                            marker[index].push(e.name);
                        });
                        console.log("Caööööö");

                        //return $sce.trustAsHtml(getFormattedSource(doc.content));
                        return $sce.trustAsHtml(getHighlightedText(doc.content, marker));
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
                }
            ]);
});
