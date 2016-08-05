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
    'angular'
], function (angular) {
    'use strict';

    angular.module('myApp.search', ['play.routing']);
    angular.module('myApp.search')
        .controller('SearchController',
            [
                '$scope',
                '$window',
                'playRoutes',
                'sourceShareService',
                'graphPropertiesShareService',
                'ObserverService',
                function ($scope,
                          $window,
                          playRoutes,
                          sourceShareService,
                          graphPropertiesShareService,
                          ObserverService) {

                    $scope.sourceShared = sourceShareService;

                    $scope.observer = ObserverService;

                    $scope.updateSearchHeight = function() {
                        $('#autocomplete').css("height",$(window).height() * 0.75);
                    };

                    angular.element($window).bind('resize', function() {
                        $scope.updateSearchHeight();
                    });

                    $scope.updateSearchHeight();

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

                        //TODO: replace tagService with observer
                        //$scope.addedTag(item);
                        $("#autocomplete").css('z-index','-1');
                        $scope.searchTags = [];
                    };

                    $scope.addFulltext = function(input) {
                        if(input.length > 2) {
                            $scope.observer.addItem({
                                type: 'fulltext',
                                data: {
                                    id: -1,
                                    name: angular.copy(input),
                                    view: 'search'
                                }
                            });
                            $scope.fulltextInput = "";
                        }
                    };


                    /**
                     * This function is called whenever a tag is added
                     */
                    $scope.addedTag = function (tagName) {
                        console.log("added " + tagName.text);

                        var idx = tagSelectShareService.tagsToUnselect.indexOf(tagName.text);
                        if (idx > -1) {
                            tagSelectShareService.tagsToUnselect.splice(idx, 1);
                        }

                        tagSelectShareService.tagsToSelect.push(tagName.text);
                        tagSelectShareService.wasChanged = true;


                    };



                }
            ]);

});
