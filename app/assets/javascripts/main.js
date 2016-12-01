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
			'ngAnimate': 'libs/angular-animate/angular-animate',
			'ngAria': 'libs/angular-aria/angular-aria',
			'ngMaterial': 'libs/angular-material/angular-material.min',
			'bootstrap': 'libs/bootstrap/dist/js/bootstrap.min',
			'ui-bootstrap': 'libs/angular-bootstrap/ui-bootstrap-tpls.min',
			'ngSanitize': 'libs/angular-sanitize/angular-sanitize.min',
			'ui-layout': 'libs/angular-ui-layout/src/ui-layout',
			'ui-router': 'libs/angular-ui-router/release/angular-ui-router.min',
			'screenfull': 'libs/screenfull/dist/screenfull',
			'angularScreenfull': 'libs/angular-screenfull/dist/angular-screenfull.min',
			'highcharts': 'libs/highcharts-release/highcharts',
			'ngFileSaver': 'libs/angular-file-saver/dist/angular-file-saver.bundle.min',
			'drilldown' : 'libs/highcharts-release/modules/drilldown',
			'underscore': 'libs/underscore/underscore-min',
			'angularResizable': 'libs/angular-resizable/angular-resizable.min',
			'bootstrapFileField': 'libs/angular-bootstrap-file-field/dist/angular-bootstrap-file-field.min',
			'vis': 'libs/vis/dist/vis.min',
			'ngVis': 'directives/angular-vis'
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
			'angularScreenfull': {
				deps: ['angular', 'screenfull']
			},
			'angular': {
				exports: 'angular',
				// Force angular to use jquery instead of jqLite library, which provides only a small subset of features
				deps: ['jquery']
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
			'ngSanitize': {
				exports: 'angular',
				deps: ['angular']
			},
			'ui-router': {
				exports: 'angular',
				deps: ['angular']
			},
			'highcharts': {
				exports: 'Highcharts',
				deps: ['jquery']
			},
			'ngMaterial': {
				deps: ['angular', 'ngAria', 'ngAnimate']
			},
			'drilldown': {
				exports: 'drilldown',
				deps: ['highcharts']
			},
			'underscore': {
				exports: '_'
			},
			'jquery.mousewheel' :{
				exports: 'jquery.mousewheel',
				deps: ['jquery']
			},
			'angularResizable' : {
				exports: 'angularResizable',
				deps: ['angular']
			},
			'bootstrapFileField' : {
				deps: ['angular', 'bootstrap']
			},
			'ngFileSaver': {
				deps: ['angular']
			},
			'ngVis': {
				deps: ['angular', 'vis']
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
			'app'
		], function(angular, app) {
			angular.bootstrap(document, ['myApp']);
		}
	);
})(requirejs);
