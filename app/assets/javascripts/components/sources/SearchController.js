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

    angular.module('myApp.search', [])
        .controller('SearchController',
            [
                '$scope',
                '$window',
                'ObserverService',
                function ($scope,
                          $window,
                          ObserverService) {

                    $scope.observer = ObserverService;

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
