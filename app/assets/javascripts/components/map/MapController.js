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
    'd3',
    'datamaps',
    'topojson'
], function (angular) {
    'use strict';

    angular.module('myApp.map', ['play.routing', 'myApp.source', 'myApp.sourcefactory']);
    angular.module('myApp.map')
        .controller('MapController', ['$scope', '$timeout', 'playRoutes', '$document', 'sourceShareService',
            function ($scope, $timeout, playRoutes, $document, sourceShareService) {
                var Datamap = require("datamaps");

                $scope.loaded = function () {
                    var container = document.getElementById('mapContainer');
                    // width and height are necessary
                    // see https://github.com/markmarkoh/datamaps/issues/65
                    var map = new Datamap({
                        width: 700,
                        height: 300,
                        element: container,
                        done: function (datamap) {
                            // Assign an click event to countries
                            datamap.svg.selectAll('.datamaps-subunit').on('click', function (geography) {
                                playRoutes.controllers.MapController.getDocsForCountry(geography.properties.name).get().then(function (response) {
                                    var count = response.data.length;
                                    if (count > 0) {
                                        sourceShareService.documentListWarning = '';
                                        sourceShareService.documentListInfo = 'Documents for ' + geography.properties.name + ' (#: ' + count + ')';
                                        sourceShareService.documentList = [];
                                        for (var i = 0; i < count; i++) {
                                            sourceShareService.documentList.push({
                                                title: response.data[i],
                                                id: 70873
                                            });
                                        }
                                    } else {
                                        sourceShareService.documentListWarning = 'There are no documents available for ' + geography.properties.name + '.'
                                        sourceShareService.documentList = [];
                                    }
                                });
                            });
                        }
                    });
                }

            }]);

});