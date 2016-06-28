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