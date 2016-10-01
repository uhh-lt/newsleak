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
],
    function (angular)
    {
        'use strict';

        angular.module('myApp.toolfactory', []);
        angular.module('myApp.toolfactory')
            .factory('toolShareService', function()
            {
                var toolShareService =
                {
                    priorityToColor : ["white","white","#83a2d6","#2759ac"],

                    deleteListener: [],
                    //(node, newText)
                    annotateListener: [],
                    //(node, newName)
                    editNameListener: [],
                    //(node, newType)
                    editTypeListener: [],
                    //(focalNode, Nodes)
                    mergeListener: [],
                    //(node)
                    getEgoNetworkListener: [],
                    //(nodeArray)
                    hideListener: [],

                    UIitems : [
                        [1, 1, 1, 1],
                        [1, 1, 1, 1],
                        [1, 1, 1, 1],
                        [1, 1, 1, 1]
                    ],

                    freqSortingLeast: false,
                    isViewLoading: function(){return true;},
                    getEgoNetwork: function(){},
                    enableOrDisableButtons: function(){},

                    //the slider values
                    sliderLocationsValue: function(){return 3},
                    sliderOrganizationsValue: function(){return 3},
                    sliderPersonsValue: function(){return 3},
                    sliderMicellaneousValue: function(){return 3},
                    sliderEdgeMinFreq: function(){return 1500},
                    sliderEdgeMaxFreq: function(){return 81337},

                    sliderEdgeAmount: function () {return 20},
                    sliderEdgesPerNode: function () {return 4},

                    updateGraph: function(){},
                    getSelectedNodes: function(){return []},
                    getSelectedElementsText: function(){return ""}
                };

                return toolShareService;
            });
    }
);