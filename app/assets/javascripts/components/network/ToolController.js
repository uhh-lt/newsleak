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
    'd3',
    'awesome-slider'
], function (angular) {
    'use strict';

    angular.module('myApp.tools', ['play.routing', 'angularMoment', 'angularAwesomeSlider']);
    angular.module('myApp.tools')
    .controller('ToolController',
    [
        '$scope',
        '$uibModal',
        'toolShareService',
        function(
            $scope,
            $uibModal,
            toolShareService
        )
        {
            toolShareService.enableOrDisableButtons();

            function createSliderOptions(value, text)
            {
                return {
                    value: "3",
                    options: {
                        from: 1,
                        to: 100,
                        step: 1,
                        dimension: text,
                        limits: false,
                        scale: [1, 100],
                        css: {
                            background: {"background-color": "silver"},
                            before: {"background-color": "#7CB5EC"},
                            default: {"background-color": "silver"},
                            after: {"background-color": "#7CB5EC"},
                            pointer: {"background-color": "#2759AC"}
                        },
                        callback: function(value, released){
                            toolShareService.updateGraph();
                        }
                    }
                }
            }

            $scope.sliderCountriesCitiesAmount = createSliderOptions("3", " countries/cities");
            $scope.sliderOrganizationsAmount = createSliderOptions("3", " organizations");
            $scope.sliderPersonsAmount = createSliderOptions("3", " persons");
            $scope.sliderMiscellaneousAmount = createSliderOptions("3", " miscellaneous")

            toolShareService.sliderLocationsValue = function(){return $scope.sliderCountriesCitiesAmount.value;}
            toolShareService.sliderOrganizationsValue = function(){return $scope.sliderOrganizationsAmount.value;}
            toolShareService.sliderPersonsValue = function(){return $scope.sliderPersonsAmount.value;}
            toolShareService.sliderMiscellaneousValue = function(){return $scope.sliderMiscellaneousAmount.value;}

            toolShareService.sliderEdgeMinFreq = function(){return $scope.sliderEdgeFrequency.value.split(";")[0]}
            toolShareService.sliderEdgeMaxFreq = function(){return $scope.sliderEdgeFrequency.value.split(";")[1]}

            $scope.toolShareService = toolShareService;

            // A slider for choosing the minimum and maximum frequency of a displayed edge.
            $scope.sliderEdgeFrequency = {
                //value: "1500;"+$scope.maxEdgeFreq,
                value: "1500;81337",
                options: {
                    from: 1,
                    to: 81337,
                    step: 1,
                    dimension: " Connections between entities",
                    limits: false,
                    scale: [1, /*$scope.maxEdgeFreq*/ 81337],
                    css: {
                        background: {"background-color": "silver"},
                        range: {"background-color": "#7CB5EC"},
                        default: {"background-color": "silver"},
                        after: {"background-color": "#7CB5EC"},
                        pointer: {"background-color": "#2759AC"}
                    },
                    callback: function(value, released){
                        toolShareService.updateGraph();
                    }
                }
            }

            $scope.getEgoNetwork = function()
            {
                var listener = toolShareService.getEgoNetworkListener

                listener.forEach
                (
                    function(l)
                    {
                        l(toolShareService.getSelectedNodes()[0]);
                    }
                )
            }

            $scope.hide = function()
            {
                var listener = toolShareService.editNameListener

                listener.forEach
                (
                    function(l)
                    {
                        l(toolShareService.getSelectedNodes());
                    }
                )
            }

            $scope.editOpen = function()
            {
            	var modal = $uibModal.open(
            		{
            			animation: true,
            			templateUrl: 'editModal',
            			controller: 'EditModalController',
            			size: 'sm',
            			resolve:
            			{
            				text: function(){return toolShareService.getSelectedElementsText();},
            				type: function(){return toolShareService.getSelectedNodes()[0].type;},
            				node: function(){return toolShareService.getSelectedNodes()[0];}
            			}
            		}
            	);

            	modal.result.then(function(result)
            	{
            	    if(result.node.name != result.text)
            	    {
            	        var listener = toolShareService.editNameListener

            	        listener.forEach
            	        (
            	            function(l)
            	            {
            	                l(result.node, result.text);
            	            }
            	        )
            	    }

            	    if(result.node.type != result.type)
                    {
                        var listener = toolShareService.editTypeListener

                        listener.forEach
                        (
                            function(l)
                            {
                                l(result.node, result.type);
                            }
                        )
                    }
            	});
            }

            $scope.annotateOpen = function()
            {
                var modal = $uibModal.open(
            	{
                    animation: true,
                    templateUrl: 'annotateModal',
                    controller: 'TextModalController',
                    size: 'lg',
                    resolve:
                    {
                        text: function(){return ""},
                        node: function(){return toolShareService.getSelectedNodes()[0];}
                    }
                }
                );

                modal.result.then(function(result)
                {
                    var listener = toolShareService.annotateListener;

                    listener.forEach
                    (
                        function(l)
                        {
                            l(result.node, result.text)
                        }
                    )
                });
            }

            $scope.mergeOpen = function()
            {
            	var modal = $uibModal.open(
                	{
                		animation: true,
                		templateUrl: 'mergeModal',
                		controller: 'MergeModalController',
                		size: 'lg',
                		resolve:
                		{
                			selectedNodes: function(){return toolShareService.getSelectedNodes();}
                		}
                	}
                );

                modal.result.then(function(result)
                {
                    var listener = toolShareService.mergeListener;
                    var nodes = toolShareService.getSelectedNodes()

                    listener.forEach
                    (
                        function(l)
                        {
                            l(result.focalNode, nodes)
                        }
                    )
                }
                );
            }

            $scope.deleteOpen = function()
            {
                var modal = $uibModal.open(
                	{
                		animation: true,
                		templateUrl: 'deleteModal',
                		controller: 'ConfirmModalController',
                		size: 'sm',
                		resolve:
                		{
                			text: function(){return "Do you really want to delete these Elements?";},
                			nodes: function(){return toolShareService.getSelectedNodes();}
                		}
                	}
                );

                modal.result.then(function(result)
                {
                    var listener = toolShareService.deleteListener;

                    listener.forEach
                    (
                        function(l)
                        {
                            l(result.nodes);
                        }
                    );
                });
            }
        }
    ]);
});