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
                    priorityToColor : ["#ff1111","white","#83a2d6","#2759ac"],
                    priorityToColorBorder : ["#FFFFFF","#000000","#83a2d6","#2759ac"],
                    priorityToIcon : ['not_interested','crop_square','favorite_border','favorite'],


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
                    UIitemsChanged: false,
                    //zur Kommunikation zwischen Toolcontroller und Networkcontroller
                    updateToolDisplay: function(){},
                    updateGuidance: function (){},

                    undoGuidance: function (){},
                    redoGuidance: function (){},


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

                    getSelectedNodes: function(){return []},
                    getSelectedElementsText: function(){return ""}
                };

                return toolShareService;
            });
    }
);