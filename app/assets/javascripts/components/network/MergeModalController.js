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
    'd3'
], function(angular)
{
	angular.module('myApp.mergemodal', ['play.routing', 'angularMoment']);
	angular.module('myApp.mergemodal')
		.controller('MergeModalController',
		[
			'$scope',
			'$uibModalInstance',
			'selectedNodes',
			'graphPropertiesShareService',
			function($scope, $uibModalInstance, selectedNodes, graphPropertiesShareService)
			{
				$scope.nodes = selectedNodes;
				$scope.focalNode = null;
				var graphShared = graphPropertiesShareService;

				$scope.maxNodeFreq = 592035;
                            $scope.maxEdgeFreq = 81337;

                            $scope.minNodeRadius = 15;
                            $scope.maxNodeRadius = 30;
                            $scope.minEdgeWidth  = 3;
                            $scope.maxEdgeWidth  = 10;

				var radius     = d3.scale.sqrt()
                                                    .domain([1,$scope.maxNodeFreq])
                                                    .range([$scope.minNodeRadius, $scope.maxNodeRadius]);

                var color      = d3.scale.category10().range(graphShared.categoryColors);

				$scope.setMergeNode = function(node)
                {
                    d3.select("#merge-node")
                    	.selectAll("*")
                    	.remove();

                    d3.select('#merge-node')
                    	.data([node])
                      	.append("circle")
                      	.attr('r', function (d) { return radius(d.freq); })
                        .style('fill', function (d) { return color(d.type); })
                        .style('stroke-width', 1.5)
                        .style('stroke', '#000000');

                    d3.select('#merge-node')
                    	.data([node])
                        .append('text')
                        .attr('dy', '.35em')
                        .attr('text-anchor', 'middle')
                        .text(function (d)
                        {
                    		   	var r = radius(d.freq);
                    		if(r/3 >= d.name.length - 1)  // If the text fits inside the node.
                				return d.name;
                			else  // If the text doesn't fit inside the node the 3 characters "..." are added at the end.
                				return d.name.substring(0, r/3 - 3) + "...";
                		})
                		.style('font-size', function(d)
                		{
                			var r = radius(d.freq);
                			var textLength;
                			var size;
                			if(r/3 >= d.name.length - 1){  // If the text fits inside the node.
                				textLength = d.name.length;
                				// If the text contains less than 3 characters: act as if
                				// there where 3 characters so that the font doesn't become to big.
                				if(textLength < 3)
                					textLength = 3;
                				size = r/3;
                			}
                			else
                			{  // If the text doesn't fit inside the node the 3 characters "..." are added at the end.
                				textLength = d.name.substring(0, r/3 - 3).length;
                				size = r/3 - 3;
                			}
                			size *= 7 / textLength;  // This seems to work to get a good text size.
                			size = Math.max(10, Math.round(size));  // Min. text size = 10px
                			return size+'px';
                        });
                }

				$scope.ok = function()
				{
					$uibModalInstance.close({focalNode: $scope.focalNode, nodes: $scope.nodes});
				}

				$scope.cancel = function()
				{
					$uibModalInstance.dismiss('cancel');
				}
			}
		]);
});