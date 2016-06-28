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

// `main.js` is the file that sbt-web will use as an entry point
(function (requirejs) {
    'use strict';

	requirejs.config({
		packages: ['libs'],
		baseUrl: './assets/javascripts',
		paths: {
			'jsRoutes': ['jsroutes'],
			'angular': 'libs/angular/angular.min',
			'jquery': 'libs/jquery/dist/jquery.min',
			'jquery-json': 'libs/jquery-json/dist/jquery.json.min',
			'awesome-slider': 'libs/angular-awesome-slider/dist/angular-awesome-slider.min',
			'ngAnimate': 'libs/angular-animate/angular-animate',
			'ngAria': 'libs/angular-aria/angular-aria',
			'ngMessages': 'libs/angular-messages/angular-messages.min',
			'ngMaterial': 'libs/angular-material/angular-material.min',
			'bootstrap': 'libs/bootstrap/dist/js/bootstrap.min',
			'ui-bootstrap': 'libs/angular-bootstrap/ui-bootstrap-tpls.min',
			'toggle-switch': 'libs/angular-toggle-switch/angular-toggle-switch.min',
			'ng-tags-input': 'libs/ng-tags-input/ng-tags-input.min',
			'ngSanitize': 'libs/angular-sanitize/angular-sanitize.min',
			'scDateTime': 'libs/sc-date-time/dist/sc-date-time',
			'moment': 'libs/moment/min/moment.min',
			'ui-layout': 'libs/angular-ui-layout/src/ui-layout',
			'ui-router': 'libs/angular-ui-router/release/angular-ui-router.min',
			'angularMoment': 'libs/angular-moment/angular-moment.min',
			'screenfull': 'libs/screenfull/dist/screenfull',
			'angularScreenfull': 'libs/angular-screenfull/dist/angular-screenfull.min',
			'highcharts': 'libs/highcharts-release/highcharts',
			'ngHighcharts': 'libs/highcharts-ng/dist/highcharts-ng.min',
			'ngFileSaver': 'libs/angular-file-saver/dist/angular-file-saver.bundle.min',
			'drilldown' : 'libs/highcharts-release/modules/drilldown',
			'datamaps' : 'libs/datamaps/dist/datamaps.world',
			'topojson' : 'libs/topojson/topojson',
			'underscore': 'libs/underscore/underscore-min',
			'd3': 'libs/d3/d3.min',
			'angularResizable': 'libs/angular-resizable/angular-resizable.min'
		},
		shim: {
			'jsRoutes': {
				exports: 'jsRoutes'
			},
			'jquery': {
				exports: 'JQuery'
			},
			'ui-layout': {
				exports: 'angular',
				deps: ['angular', 'ngAnimate']
			},
			'angularMoment': {
				deps: ['angular', 'moment']
			},
			'angularScreenfull': {
				deps: ['angular', 'screenfull']
			},
			'angular': {
				exports: 'angular'
			},
			'awesome-slider': {
				exports: 'angular',
				deps: ['angular']
			},
			'ngAnimate': {
				exports: 'angular',
				deps: ['angular']
			},
			'ngAria': {
				exports: 'angular',
				deps: ['angular']
			},
			'bootstrap': {
				deps: ['jquery']
			},
			'ui-bootstrap': {
				deps: ['angular', 'bootstrap', 'ngAnimate']
			},
			'toggle-switch': {
				exports: 'angular',
				deps: ['angular']
			},
			'ng-tags-input': {
				deps: ['angular']
			},
			'ngSanitize': {
				exports: 'angular',
				deps: ['angular']
			},
			'ui-router': {
				exports: 'angular',
				deps: ['angular']
			},
			'scDateTime': {
				deps: ['angular']
			},
			'highcharts': {
				exports: 'Highcharts',
				deps: ['jquery']
			},
			'ngHighcharts': {
				deps: ['highcharts', 'angular']
			},
			'ngMaterial': {
				deps: ['angular','ngAria','ngMessages','ngAnimate']
			},
			'drilldown': {
				exports: 'drilldown',
				deps: ['highcharts']
			},
			'datamaps': {
				exports: 'datamaps',
				deps: ['topojson', 'd3']
			},
			'topojson': {
				exports: 'topojson',
				deps: ['d3']
			},
			'underscore': {
				exports: '_'
			},
			'jquery.mousewheel' :{
				exports: 'jquery.mousewheel',
				deps: ['jquery']
			},
			'd3': {
				exports: 'd3'
			},
			'angularResizable' : {
				exports: 'angularResizable',
				deps: ['angular']
			}
		},
		priority: [
			'jquery',
			'angular',
		],
		deps: ['angular','jquery'],
		waitSeconds: 5

	});

	requirejs.onError = function (err) {
		console.log(err);
	};

	require([
			'angular',
			'app',
		], function(angular, app) {
			angular.bootstrap(document, ['myApp']);
		}
	);
	
	
})(requirejs);
