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
    './factory/filter/FilterFactory',
    './factory/network/ToolFactory',
    './factory/metadata/MetaFactory',
    './factory/source/SourceFactory',
    './factory/source/HighlightFactory',
    './components/sources/SourceController',
    './components/network/NetworkController',
    './components/network/ToolController',
    './components/network/TextModalController',
    './components/network/MergeModalController',
    './components/network/EditModalController',
    './components/network/ConfirmModalController',
    './components/metadata/MetadataController',
    './components/filter/FilterController',
    './components/sources/SearchController',
    './components/history/HistoryController',
    './components/histogram/HistogramController',
    './components/map/MapController',
    './services/playRoutes',
    './services/ObserverService',
    './factory/appData',
    './factory/util',
    './services/underscore-module',
    'ui-layout',
    'ui-router',
    'ui-bootstrap',
    'ng-tags-input',
    'angularResizable',
    'ngMaterial'
], function (angular) {
    'use strict';

    var app = angular.module('myApp',
        [
            'ui.layout', 'ui.router', 'ui.bootstrap', 'ngTagsInput', 'play.routing','angularResizable','ngMaterial',
            'angularMoment', 'underscore', 'myApp.data', 'myApp.observer', 'myApp.util', 'myApp.filter', 'myApp.history',
            'myApp.tools',
            'myApp.textmodal', 'myApp.mergemodal', 'myApp.editmodal', 'myApp.confirmmodal',
            'myApp.network', 'myApp.metadata', 'myApp.map', 'myApp.source', 'myApp.sourcefactory', 'myApp.highlightfactory',
            'myApp.filterfactory','myApp.metafactory', 'myApp.toolfactory',
            'myApp.histogram', 'myApp.search'
        ]
    );

    app.config(['$stateProvider', '$urlRouterProvider', function($stateProvider, $urlRouterProvider) {
        $stateProvider
        .state('layout', {
            views: {
                'header': {
                    templateUrl: 'assets/partials/header.html'
                },
                'documentlist': {
                    templateUrl: 'assets/partials/document_list.html',
                    controller: 'SourceController'
                },
                'network': {
                    templateUrl: 'assets/partials/network.html',
                    controller: 'NetworkController'
                },
                'tools': {
                    templateUrl: 'assets/partials/tools.html',
                    controller: 'ToolController'
                },
                'source': {
                    templateUrl: 'assets/partials/source.html',
                    controller: 'SourceController'
                },
                'histogram': {
                    templateUrl: 'assets/partials/histogram.html',
                    controller: 'HistogramController'
                },
                'map': {
                    templateUrl: 'assets/partials/map.html',
                    controller: 'MapController'
                },
                'metadata': {
                    templateUrl: 'assets/partials/metadata.html',
                    controller: 'MetadataController'
                },
                'filter': {
                	templateUrl: 'assets/partials/filter.html',
                	controller: 'FilterController'
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

    app.controller('AppController', ['$scope', '$state', '$timeout', '$window', 'moment', 'appData', 'uiShareService',
        function ($scope, $state, $timeout, $window, moment, appData, uiShareService) {

            init();

            function init() {
                $state.go('layout');
                // $timeout in order to have the right values right from the beginning on
                $timeout(function() {
                    setUILayoutProperties(parseInt($('#network-maps-container').css('width')), parseInt($('#network-maps-container').css('height'))-96);
                }, 100);
            };

            //TODO: WHEN USING THIS FOR WATCH NO DEEP COPY IS NEEDED!!!!
            $scope.$watch(function () {
                return appData.getDate();
            }, function (newVal, oldVal) {
                if (!angular.equals(newVal, oldVal)) {
                    $scope.date = newVal;
                }
            });

            $scope.$on("angular-resizable.resizeEnd", function (event, args) {
                if(args.id == 'center-box') setUILayoutProperties(args.width, false);
                if(args.id == 'footer') setUILayoutProperties(false, parseInt($('#network-maps-container').css('height'))-96);
                $("#histogram").highcharts().reflow();
                $("#metadata-view .active .active .meta-chart").highcharts().reflow();
            });

            angular.element($window).bind('resize', function () {
                setUILayoutProperties(parseInt($('#network-maps-container').css('width')), parseInt($('#network-maps-container').css('height'))-96);
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

        }]);

    return app;
});
