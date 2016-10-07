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