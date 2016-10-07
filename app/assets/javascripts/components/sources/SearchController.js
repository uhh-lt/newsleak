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
    'angular'
], function (angular) {
    'use strict';

    angular.module('myApp.search',  ['play.routing'])
        .controller('SearchController',
            [
                '$scope',
                '$window',
                'playRoutes',
                'graphPropertiesShareService',
                'ObserverService',
                function ($scope,
                          $window,
                          playRoutes,
                          graphPropertiesShareService,
                          ObserverService) {

                    $scope.observer = ObserverService;

                    $scope.updateSearchHeight = function() {
                        $('#autocomplete').css("height",$(window).height() * 0.75);
                    };

                    angular.element($window).bind('resize', function() {
                        $scope.updateSearchHeight();
                    });

                    $scope.updateSearchHeight();

                    var categories = {PER: '\uE7FD' /*'face'*/, ORG: '\uE84F',
                        LOC: '\uE55F',MISC: '\uE8FE'};
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
                                                img: categories[currentValue[2]],
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

                    $scope.startGuidance = function (item) {
                        console.log("send event startGuidance");
                        $("#autocomplete").css('z-index','-1');
                        $scope.searchTags = [];
                        
                        $scope.$emit('startGuidance-up', item.id);
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
                }
            ]);

});
