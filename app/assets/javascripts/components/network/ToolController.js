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
    'd3',
    'awesome-slider',
    'toggle-switch'
], function (angular) {
    'use strict';

    angular.module('myApp.tools', ['play.routing', 'angularMoment', 'angularAwesomeSlider', 'toggle-switch']);
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

            toolShareService.sliderEdgeAmount = function () {return $scope.sliderEdgeAmount.value}

            $scope.toolShareService = toolShareService;

            $scope.categories = [{name:'-', img: '-'},{name:'PER', img: 'person'/*'face'*/, val: 1},{name:'ORG', img: 'account_balance', val: 1},
                {name:'LOC', img: 'place', val: 1},{name:'MISC', img: 'reorder', val: 1}];

            var UIgeneralItems = [1,1,1,1];
            // var priorityToColor = ["white","white","#83a2d6","#2759ac"];

            $scope.setUI = function (x,y) {
                if ((x==0 || y==0) && !(x==0 && y==0)){
                    var prio = (UIgeneralItems[x + y -1] + 1) % 4;
                    UIgeneralItems[x + y -1] = prio;
                    x = x+y;
                    for (var i = 0; i<UIgeneralItems.length+1; i++){
                        if (i != UIgeneralItems.length) {
                            toolShareService.UIitems[i][x - 1] = prio;
                            toolShareService.UIitems[x - 1][i] = prio;
                        }
                        if (i != 0){
                            if (prio == 0){
                                document.getElementById(x + '.' + i + '.icon').style.visibility = "visible";
                                document.getElementById(i + '.' + x + '.icon').style.visibility = "visible";
                            } else {
                                document.getElementById(x + '.' + i + '.icon').style.visibility = "hidden";
                                document.getElementById(i + '.' + x + '.icon').style.visibility = "hidden";
                            }
                        }
                        document.getElementById(x + '.' + i).style.backgroundColor = toolShareService.priorityToColor[prio];
                        document.getElementById(i + '.' + x).style.backgroundColor = toolShareService.priorityToColor[prio];
                    }
                } else {
                    var prio = (toolShareService.UIitems[x - 1][y - 1] + 1) % 4;
                    if (prio == 0){
                        document.getElementById(x + '.' + y + '.icon').style.visibility = "visible";
                        document.getElementById(y + '.' + x + '.icon').style.visibility = "visible";
                    } else if (prio == 1){
                        document.getElementById(x + '.' + y + '.icon').style.visibility = "hidden";
                        document.getElementById(y + '.' + x + '.icon').style.visibility = "hidden";
                    }
                    toolShareService.UIitems[y - 1][x - 1] = prio;
                    toolShareService.UIitems[x - 1][y - 1] = prio;
                    document.getElementById(x + '.' + y).style.backgroundColor = toolShareService.priorityToColor[prio];
                    document.getElementById(y + '.' + x).style.backgroundColor = toolShareService.priorityToColor[prio];
                }
            //console.log(toolShareService.UIitems);
            };

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
            $scope.sliderEdgeAmount = {
                //value: "1500;"+$scope.maxEdgeFreq,
                value: "20",
                options: {
                    from: 5,
                    to: 40,
                    step: 1,
                    dimension: "displayed Edges",
                    limits: false,
                    scale: [5, /*$scope.maxEdgeFreq*/ 40],
                    css: {
                        background: {"background-color": "silver"},
                        range: {"background-color": "#7CB5EC"},
                        default: {"background-color": "silver"},
                        after: {"background-color": "#7CB5EC"},
                        pointer: {"background-color": "#2759AC"}
                    },
                    callback: function(value, released){
                        //toolShareService.updateGraph();
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