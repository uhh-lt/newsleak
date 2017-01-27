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
    /**
     * search module:
     * - full text search input
     * - load a text file holding a search term list
     */
    angular.module('myApp.search', [])
        .directive('onReadFile', function ($parse) {
            return {
                restrict: 'A',
                scope: false,
                link: function(scope, element, attrs) {
                    var fn = $parse(attrs.onReadFile);

                    element.on('change', function(onChangeEvent) {
                        var reader = new FileReader();
                        var file = (onChangeEvent.srcElement || onChangeEvent.target).files[0];

                        reader.onload = function(onLoadEvent) {
                            scope.$apply(function() {
                                fn(scope, { $fileContent:onLoadEvent.target.result, $fileName:file.name });
                            });
                        };
                        reader.readAsText(file);
                        element.val(null);  // clear input
                    });
                }
            };
        })
        .controller('SearchController', ['$scope', 'ObserverService', 'historyFactory',
                function ($scope, ObserverService, historyFactory) {

                    $scope.observer = ObserverService;
                    $scope.historyFactory = historyFactory;

                    $scope.addFulltext = function(input) {
                        if(input.length > 2) {
                            $scope.observer.addItem({
                                type: 'fulltext',
                                data: {
                                    id: -1,
                                    description: angular.copy(input),
                                    item: angular.copy(input),
                                    view: 'search'
                                }
                            });
                            $scope.fulltextInput = "";
                        }
                    };

                    $scope.loadTermList = function(fileContent, fileName) {
                        var query = fileContent.split('\n').filter(function(t) { return t != "" }).join(' OR ');
                        $scope.observer.addItem({
                            type: 'fulltext',
                            data: {
                                id: -1,
                                description: fileName + ' List',
                                item: query,
                                view: 'search'
                            }
                        });
                    }
                }
            ]);
});
