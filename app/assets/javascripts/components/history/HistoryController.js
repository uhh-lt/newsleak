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
    'ui-bootstrap',
    'ngAnimate'
], function(angular) {
    'use strict';

    angular.module("myApp.history", ['play.routing', 'angularMoment', 'ngFileSaver', 'ui.bootstrap','ngAnimate'])
        .config(function($uibTooltipProvider) {
        })
        .factory('historyFactory', [
            function() {
                return {
                    icons: {
                        'entity' : 'filter',
                        'metadata': 'filter',
                        'time': 'time',
                        "expandNode": 'plus',
                        "collapseNode": 'plus',
                        "egoNetwork": 'asterisk',
                        "merge": 'resize-small',
                        "hide": 'eye-close',
                        "edit": 'pencil',
                        "annotate": 'comment'
                    },
                    typeDescriptions: {
                      'entity': 'Entity Filter',
                      'metadata': 'Metadata Filter',
                      'time': 'Time Range',
                      'annotate': 'Entity Annotated'
                    },
                    actions: {
                        'added': 'plus',
                        'removed': 'minus',
                        'replaced': 'refresh'
                    },
                    popover: {
                        template: 'tooltip_tmpl',
                        placement: 'bottom',
                        trigger: 'None',
                        isOpen: [],
                        promises: []
                    }
                }
            }
        ])
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
                'historyFactory',
                function ($scope, $timeout, playRoutes, appData, moment, FileSaver, filterShareService, ObserverService, historyFactory) {
                    $scope.observer = ObserverService;
                    $scope.factory = historyFactory;

                    $scope.observer_subscribe = function(history) { $scope.history = history};
                    $scope.observer.subscribeHistory($scope.observer_subscribe);
                    
                    $scope.removeItem = function(item) {
                        $scope.observer.removeItem(item.id, item.type);
                    };

                    $scope.getIcon = function(type) {
                        return $scope.factory.icons[type];
                    };

                    $scope.getActionIcon = function(type) {
                        return $scope.factory.actions[type];
                    };

                    $scope.removeItem = function(filter) {
                        $scope.observer.removeItem(filter.id, filter.type);
                    };

                    $scope.hidePopover = function(id) {
                        $scope.factory.popover.promises[id] = $timeout(function() { $scope.hideFunction(id)}, 500);
                    };

                    $scope.showPopover = function(id) {
                        if($scope.factory.popover.promises[id] != undefined) $timeout.cancel($scope.factory.popover.promises[id]);
                        $scope.factory.popover.isOpen[id] = true;
                    };

                    $scope.hideFunction = function(x) {
                        $scope.factory.popover.isOpen[x] = false;
                    };

                    $scope.getTypeDescription = function(x) {
                        return $scope.factory.typeDescriptions[x];
                    }
                }
            ]
        )
});