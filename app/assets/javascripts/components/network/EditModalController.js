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
], function(angular)
{
	angular.module('myApp.editmodal', ['play.routing', 'angularMoment']);
	angular.module('myApp.editmodal')
		.controller('EditModalController',
		[
			'$scope',
			'$uibModalInstance',
			'text',
			'type',
			'node',
			'graphPropertiesShareService',
			function($scope, $uibModalInstance, text, type, node, graphPropertiesShareService)
			{
				var usednode = node;
				$scope.text = text;
				$scope.type = type;
				$scope.types = graphPropertiesShareService.categories;

				$scope.ok = function()
				{
					$uibModalInstance.close({text: $scope.text, type: $scope.type, node: usednode});
				}

				$scope.cancel = function()
				{
					$uibModalInstance.dismiss('cancel');
				}
			}
		]);
});