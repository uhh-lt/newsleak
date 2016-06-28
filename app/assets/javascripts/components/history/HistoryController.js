/**
 * Created by flo on 6/10/16.
 */
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
    'angularMoment',
    'jquery-json',
    'ngFileSaver'
], function(angular) {
    'use strict';

    angular.module("myApp.history", ['play.routing', 'angularMoment', 'ngFileSaver']);
    angular.module("myApp.history")
        .controller('HistoryController',
            [
                '$scope',
                '$timeout',
                'playRoutes',
                'appData',
                'moment',
                'FileSaver',
                'filterShareService',
                'ObserverService',
                function ($scope, $timeout, playRoutes, appData, moment, FileSaver, filterShareService, ObserverService) {
                    $scope.observer = ObserverService;


                    $scope.observer_subscribe = function(history) { $scope.history = history};
                    $scope.observer.subscribeHistory($scope.observer_subscribe);
                    
                    $scope.removeItem = function(item) {
                        $scope.observer.removeItem(item.id, item.type);
                    }
                }
            ]
        )
});