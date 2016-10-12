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
    'angularMoment',
    './factory/network/ToolFactory',
    './factory/metadata/MetaFactory',
    './factory/source/SourceFactory',
    './factory/source/HighlightFactory',
    './components/sources/SourceController',
    './components/sources/DocumentController',
    './components/network/FancyNetworkController',
    './components/network/GraphConfig',
    './components/network/ToolController',
    './components/network/TextModalController',
    './components/network/MergeModalController',
    './components/network/EditModalController',
    './components/network/ConfirmModalController',
    './components/metadata/MetadataController',
    './components/sources/SearchController',
    './components/history/HistoryController',
    './components/histogram/HistogramController',
    './services/playRoutes',
    './services/ObserverService',
    './factory/util',
    './services/underscore-module',
    'ui-layout',
    'ui-router',
    'ui-bootstrap',
    'angularResizable',
    'ngSanitize',
    'ngMaterial',
    'angularScreenfull',
    'ngVis',
    'vis'
], function (angular) {
    'use strict';

    var app = angular.module('myApp',
        [
            'ui.layout', 'ui.router', 'ui.bootstrap', 'play.routing','angularResizable', 'ngSanitize', 'ngMaterial',
            'angularMoment', 'underscore', 'myApp.observer', 'myApp.util', 'myApp.history', 'myApp.graphConfig',
            'myApp.tools', 'angularScreenfull',
            'myApp.textmodal', 'myApp.mergemodal', 'myApp.editmodal', 'myApp.confirmmodal',
            'myApp.network', 'myApp.metadata', 'myApp.source', 'myApp.sourcefactory', 'myApp.highlightfactory',
            'myApp.metafactory', 'myApp.toolfactory', 'myApp.document',
            'myApp.histogram', 'myApp.search',
            'ngVis'
        ]
    );

    app.config(['$stateProvider', '$urlRouterProvider', '$mdThemingProvider', function($stateProvider, $urlRouterProvider, $mdThemingProvider) {

        $mdThemingProvider.theme('control-theme')
            .primaryPalette('yellow')
            .dark()
            .backgroundPalette('blue-grey', {
                'default': '200'
            });

        $stateProvider
        .state('layout', {
            views: {
                'header': {
                    templateUrl: 'assets/partials/header.html',
                    controller: 'AppController'
                },
                'documentlist': {
                    templateUrl: 'assets/partials/document_list.html',
                    controller: 'SourceController'
                },
                'document': {
                    templateUrl: 'assets/partials/document.html',
                    controller: 'DocumentController'
                },
                'network': {
                    templateUrl: 'assets/partials/network.html',
                    controller: 'FancyNetworkController'
                },
                'tools': {
                    templateUrl: 'assets/partials/tools.html',
                    controller: 'ToolController'
                },
                'histogram': {
                    templateUrl: 'assets/partials/histogram.html',
                    controller: 'HistogramController'
                },
                'metadata': {
                    templateUrl: 'assets/partials/metadata.html',
                    controller: 'MetadataController'
                },
                'history': {
                    templateUrl: 'assets/partials/history.html',
                    controller: 'HistoryController'
                },
                'search' : {
                    templateUrl: 'assets/partials/search.html',
                    controller: 'SearchController'
                }
            }
        });
        $urlRouterProvider.otherwise('/');

    }]);

    app.factory('uiShareService', function() {
        var uiProperties = {
            mainContainerHeight: -1,
            mainContainerWidth: -1,
            footerHeight: -1
        };
        return uiProperties;
    });

    app.controller('AppController', ['$scope', '$state', '$timeout', '$window', '$mdDialog', 'moment', 'uiShareService', 'ObserverService', 'playRoutes', '_',
        function ($scope, $state, $timeout, $window, $mdDialog, moment, uiShareService, ObserverService, playRoutes, _) {

            /* Select graph tab on startup. In order to update the value from the child scope we need
             * an object here. */
            $scope.selectedTab = { index: 0 };
            $scope.selectedDataset = '';
            $scope.datasets = ['cable', 'enron'];

            init();

            function init() {
                $state.go('layout');
                // TODO Don't know what the resizing is about
                // $timeout in order to have the right values right from the beginning on
                /*$timeout(function() {
                    setUILayoutProperties(parseInt($('#network-maps-container').css('width')), parseInt($('#network-maps-container').css('height'))-96);
                }, 100); */
            }

            $scope.$on("angular-resizable.resizeEnd", function (event, args) {
                if(args.id == 'center-box') setUILayoutProperties(args.width, false);
                //if(args.id == 'footer') setUILayoutProperties(false, parseInt($('#network-maps-container').css('height'))-96);
                $("#histogram").highcharts().reflow();
                $("#metadata-view .active .active .meta-chart").highcharts().reflow();
            });

            /*angular.element($window).bind('resize', function () {
                setUILayoutProperties(parseInt($('#network-maps-container').css('width')), parseInt($('#network-maps-container').css('height'))-96);
            });*/

            /**
             * This function sets properties that describe the dimensions of the UI layout.
             */
            function setUILayoutProperties(mainWidth, mainHeight) {
                if(mainHeight != false) uiShareService.mainContainerHeight = mainHeight;
                if(mainWidth != false) uiShareService.mainContainerWidth = mainWidth;
                // NaN check is necessary because when loading the page the bar chart isn't rendered yet
                uiShareService.footerHeight = parseInt($('#chartBarchart').css('height'));
            }

            // On change event of the UI layout
            $scope.$on('ui.layout.resize', function (e, beforeContainer, afterContainer) {
                //setUILayoutProperties();
            });


            // TODO: Refactor move to own file
            $scope.showSettings = function() {
                var blacklisted = playRoutes.controllers.EntityController.getBlacklistedEntities().get().then(function(response) {
                    return response.data;
                });

                $mdDialog.show({
                templateUrl: 'assets/partials/settings.html',
                controller: ['$scope', '$mdDialog', 'playRoutes', 'b',
                    function($scope, $mdDialog, playRoutes, b) {
                        $scope.blacklisted = b;
                        $scope.selected = [];

                        $scope.toggle = function (item) {
                            if($scope.exists(item, $scope.selected)) { $scope.selected = _.without($scope.selected, item); }
                            else { $scope.selected.push(item); }
                        };

                        $scope.exists = function (item, list) {
                            var idx = _.findIndex(list, function(el) { return el.id == item.id; });
                            return idx > -1;
                        };

                        $scope.removeItems = function() {
                            var ids = $scope.selected.map(function(e) { return e.id });
                            playRoutes.controllers.EntityController.undoBlacklistingByIds(ids).get().then(function(response) { });
                            $scope.blacklisted = _.reject($scope.blacklisted, function(e) { return $scope.exists(e, $scope.selected); });
                            $scope.selected.length = 0;
                        };

                        $scope.closeClick = function() { $mdDialog.cancel(); };
                    }],
                    locals: { b: blacklisted }
                }).then(function(response) {
                }, function() { /* cancel click */ });
            };

            $scope.changeDataset = function() {
                console.log('Changed ' + $scope.selectedDataset);
                playRoutes.controllers.Application.changeDataset($scope.selectedDataset).get().then(function(response) {
                    // Update views with new data from the changed data collection
                    if(response.data.oldDataset != response.data.newDataset) {
                        ObserverService.reset();
                    }
                });
            };
        }]);

    return app;
});
