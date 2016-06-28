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
], function(angular)
{
	angular.module('myApp.textmodal', ['play.routing', 'angularMoment']);
	angular.module('myApp.textmodal')
		.controller('TextModalController',
		[
			'$scope',
			'$uibModalInstance',
			'text',
			'node',
			function($scope, $uibModalInstance, text, node)
			{
				var usednode = node;
				$scope.text = text;

				$scope.ok = function()
				{
					$uibModalInstance.close({text: $scope.text, node: usednode});
				}

				$scope.cancel = function()
				{
					$uibModalInstance.dismiss('cancel');
				}
			}
		]);
});