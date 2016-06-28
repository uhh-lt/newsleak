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
], function(angular)
{
	'use strict';

	angular.module("myApp.filter", ['play.routing', 'angularMoment', 'ngFileSaver']);
	angular.module("myApp.filter")
		.controller('FilterController',
        	[
        		'$scope',
        		'$timeout',
        		'playRoutes',
        		'appData',
        		'moment',
        		'FileSaver',
        		'filterShareService',
				'ObserverService',
				function($scope, $timeout, playRoutes, appData, moment, FileSaver, filterShareService, ObserverService)
				{
					$scope.filterShared = filterShareService;
					$scope.observer = ObserverService;


					$scope.observer_subscribe = function(items) { filterShareService.wordFilters = items};
					$scope.observer.subscribeItems($scope.observer_subscribe, "entity");

					/**
					 *	delete a word filter from the filter list
					 *
					 *	@param word
					 *		the filter word to delete
					 */
					$scope.deleteWordFilter = function(filter)
					{
						$scope.observer.removeItem(filter.id, filter.type);
						/*
						filterShareService.wordFilters.splice(
							filterShareService.wordFilters.findIndex(
								function(element, index, array)
								{
									return element.name == word;
								}
							),
							1
						);
						*/
					};

					/**
					 *	save the filters and return it as file
					 */
					$scope.saveFilters = function()
					{
						var savedata = {wordFilters: []};

						filterShareService.wordFilters.forEach(function(e)
						{
							savedata.wordFilters.push(e.name);
						});

						FileSaver.saveAs(new Blob([$.toJSON(savedata)], {type: 'text/plain;charset=UTF-8'}), "filters_" + Date.now() + ".txt")
					}

					$scope.loadFilters = function()
					{
						alert("TODO: load Filter");
					}
				}
            ]
        );
});