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
	angular.module('myApp.confirmmodal', ['play.routing', 'angularMoment']);
	angular.module('myApp.confirmmodal')
		.controller('ConfirmModalController',
		[
			'$scope',
			'$uibModalInstance',
			'text',
			'nodes',
			function($scope, $uibModalInstance, text, nodes)
			{
				$scope.text = text;
				$scope.nodes = nodes

				$scope.ok = function()
				{
					$uibModalInstance.close({nodes: $scope.nodes});
				}

				$scope.cancel = function()
				{
					$uibModalInstance.dismiss('cancel');
				}
			}
		]);
})