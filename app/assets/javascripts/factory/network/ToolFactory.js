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

                    updateGraph: function(){},
                    getSelectedNodes: function(){return []},
                    getSelectedElementsText: function(){return ""}
                };

                return toolShareService;
            });
    }
);