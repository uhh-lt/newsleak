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