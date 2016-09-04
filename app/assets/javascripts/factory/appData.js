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