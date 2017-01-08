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
    './factory/metadata/MetaFactory',
    './factory/source/SourceFactory',
    './components/sources/SourceController',
    './components/sources/DocumentController',
    './components/network/NetworkController',
    './components/network/GraphConfig',
    './components/metadata/MetadataController',
    './components/sources/SearchController',
    './components/history/HistoryController',
    './components/histogram/HistogramController',
    './components/histogram/HistogramXController',
    './services/playRoutes',
    './services/EntityService',
    './services/ObserverService',
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

    var app = angular.module('myApp', [
            'ui.layout', 'ui.router', 'ui.bootstrap', 'play.routing','angularResizable', 'ngSanitize', 'ngMaterial',
            'underscore', 'myApp.observer', 'myApp.history', 'myApp.graphConfig', 'angularScreenfull',
            'myApp.network', 'myApp.search', 'myApp.metadata', 'myApp.source', 'myApp.sourcefactory', 'myApp.metafactory',
            'myApp.document', 'myApp.histogram', 'myApp.histogramX', 'ngVis', 'myApp.entityservice']
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
                    controller: 'NetworkController'
                },
                'histogram': {
                    templateUrl: 'assets/partials/histogram.html',
                    controller: 'HistogramController'
                },
                'histogramX': {
                    templateUrl: 'assets/partials/histogramX.html',
                    controller: 'HistogramXController'
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

    app.controller('AppController', ['$scope', '$state', '$timeout', '$window', '$mdDialog', 'uiShareService', 'ObserverService', 'playRoutes', 'historyFactory',
        function ($scope, $state, $timeout, $window, $mdDialog, uiShareService, ObserverService, playRoutes, historyFactory) {

            // Select graph tab on startup. In order to update the value from the child scope we need an object here.
            $scope.selectedTab = { index: 0 };
            $scope.selectedDataset = '';
            $scope.datasets = [];

            $scope.historyFactory = historyFactory;

            init();

            function init() {
                // Fetch datasets for dropdown list
                /*
                playRoutes.controllers.Application.getDatasets().get().then(function(response) {
                    $scope.datasets = response.data.available;
                    $scope.selectedDataset = response.data.current;
                });
                */
                $state.go('layout');
                // TODO Don't know what the resizing is about
                // $timeout in order to have the right values right from the beginning on
                /*$timeout(function() {
                    setUILayoutProperties(parseInt($('#network-maps-container').css('width')), parseInt($('#network-maps-container').css('height'))-96);
                }, 100); */
            }

            $scope.resizeUI = function() {
                $("#histogram").css("height",$("footer").height()-50);
                $("#histogramX").css("height",$("footer").height()-50);
                $("#vertical-container").css("height", $("#documents-view").height() - 110);
                $("#histogram").highcharts().reflow();
                $("#histogramX").highcharts().reflow();
                $(".scroll-chart").css("height",$("#metadata").height()-150);
                $("#metadata-view .md-active .meta-chart").highcharts().reflow();
            };

            $scope.$on("angular-resizable.resizeEnd", function (event, args) {
                if(args.id == 'center-box') setUILayoutProperties(args.width, false);

                //if(args.id == 'footer') setUILayoutProperties(false, parseInt($('#network-maps-container').css('height'))-96);
                $scope.resizeUI();

            });

            angular.element($window).bind('resize', function () {
               // setUILayoutProperties(parseInt($('#network-maps-container').css('width')), parseInt($('#network-maps-container').css('height'))-96);
                $scope.resizeUI();
            });

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

            $scope.showSettings = function() {
                $mdDialog.show({
                    templateUrl: 'assets/partials/settings.html',
                    controller: 'SettingsController'
                })
            };

            $scope.changeDataset = function() {
                console.log('Changed ' + $scope.selectedDataset);
                /*
                playRoutes.controllers.Application.changeDataset($scope.selectedDataset).get().then(function(response) {
                    // Update views with new data from the changed data collection
                    if(response.data.oldDataset != response.data.newDataset) {
                        ObserverService.reset();
                    }
                });
                */
            };
        }]);

    app.controller('SettingsController', ['$scope', '$mdDialog', 'playRoutes', '_', 'ObserverService', function ($scope, $mdDialog, playRoutes, _, ObserverService) {

            $scope.blacklist = [];
            $scope.mergelist = [];

            $scope.blacklistSelection = [];
            $scope.mergelistSelection = [];

            // Will be affected by the tab md-change event
            $scope.isBlacklist = false;

            $scope.init = function() {
                fetchBlacklist();
                fetchMergelist();
            };

            $scope.init();

            function fetchBlacklist() {
                 playRoutes.controllers.EntityController.getBlacklistedEntities().get().then(function (response) {
                     $scope.blacklist = response.data;
                });
            }

            function fetchMergelist() {
                playRoutes.controllers.EntityController.getMergedEntities().get().then(function (response) {
                    $scope.mergelist = response.data;
                });
            }

            $scope.joinDuplicates = function(duplicates) {
                return duplicates.map(function(d) { return '' + d.name + ' (' + d.entityType + ')'; }).join(', ')
            };

            $scope.toggle = function (item, list) {
                if($scope.exists(item, list)) {
                    // Remove element in-place from list
                    var index = list.indexOf(item);
                    list.splice(index, 1);
                } else { list.push(item); }
            };

            $scope.exists = function (item, list) {
                var index = list.indexOf(item);
                return index > -1;
            };

            $scope.removeFromBlacklist = function() {
                // TODO: Enhancement update only special parts of the application
                removeSelection(
                    $scope.blacklist,
                    $scope.blacklistSelection,
                    function(ids, observer) {
                        // Update network and frequency chart
                        playRoutes.controllers.EntityController.undoBlacklistingByIds(ids).get().then(function(response) {  observer.notifyObservers(); })
                    });
            };

            $scope.removeFromMergelist = function() {
                removeSelection(
                    $scope.mergelist,
                    $scope.mergelistSelection,
                    function(ids, observer) {
                        playRoutes.controllers.EntityController.undoMergeByIds(ids).get().then(function(response) {  observer.notifyObservers(); })
                    });
            };

            function removeSelection(list, selection, callback) {
                var ids = selection.map(function(e) { return e.id });
                callback(ids, ObserverService);
                // Remove selected items from the list in-place
                selection.forEach(function(el) {
                    var index = list.indexOf(el);
                    list.splice(index, 1);
                });
                selection.length = 0;
             }

            $scope.closeClick = function() { $mdDialog.cancel(); };
        }]);

    return app;
});
