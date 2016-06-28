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
	'angularMoment'
], function (angular) {
  "use strict";

	var mod = angular.module('myApp.data', ['angularMoment']);

	mod.factory('appData', ['moment', function(moment) {
		var d = new Date();
		var n = {};
		var f = {};
		var l = {};

		var appService = {};


		appService.setDate = function(date) {
		    d = date;
		};

		/**
			Get date as YYY-MM-dd
		**/
		appService.getDate = function() {
			return moment(d).format('YYYY-MM-DD');
		};

		appService.setNodes = function(nodes) {
			n = nodes;
		}

		appService.getNodes = function() {
			return n;
		}

		appService.setFocus = function(focus) {
			f = focus;
		}

		appService.getFocus = function() {
			return f;
		}

		appService.setLabel = function(data) {
			l = data;
		}

		appService.getLabel = function() {
			return l;
		}

		return appService;
	}]);
});