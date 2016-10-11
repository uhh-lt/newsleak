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
    'angular'
], function(angular) {
    'use strict';

    // TODO Figure out how to use myApp.network module here
    angular.module('myApp.graphConfig', [])
        .constant('physicOptions', {
            forceAtlas2Based: {
                gravitationalConstant: -220,
                centralGravity: 0.01,
                springConstant: 0.02,
                springLength: 110,
                damping: 0.4,
                avoidOverlap: 0
            },
            barnesHut: {
                gravitationalConstant: -50,
                centralGravity: 0.01,
                springConstant: 0.08,
                damping: 0.4
            },
            maxVelocity: 146,
            solver: 'forceAtlas2Based',
            stabilization: {
                fit: true,
                iterations: 1000//20
            }//,
           // adaptiveTimestep: true
        })
        .constant('generalOptions', {
                nodes : {
                    shape: 'dot',
                    size: 10,
                    shadow: true,
                    //mass: 1.7,
                    scaling: {
                        label: {
                            min: 30,
                            max: 45
                        }
                    }
                },
                edges: {
                    color: {
                        color: 'rgb(169,169,169)',
                        highlight: 'blue'
                    },
                    smooth: { type:'continuous' }
                },
                layout: {
                    improvedLayout: false
                },
                interaction: {
                    tooltipDelay: 200,
                    hover: true,
                    hideEdgesOnDrag: true,
                    navigationButtons: true,
                    keyboard: false
                },
                groups: {
                    '0': { color:  {border: "#2B7CE9", background: "#97C2FC", highlight: {border: "#2B7CE9", background: "#D2E5FF"}, hover: {border: "#2B7CE9", background: "#D2E5FF"}}}, // 0: blue
                    '1': { color:  {border: "#FFA500", background: "#FFFF00", highlight: {border: "#FFA500", background: "#FFFFA3"}, hover: {border: "#FFA500", background: "#FFFFA3"}}}, // 1: yellow
                    '2': { color:  {border: "#FA0A10", background: "#FB7E81", highlight: {border: "#FA0A10", background: "#FFAFB1"}, hover: {border: "#FA0A10", background: "#FFAFB1"}}}, // 2: red
                    '3': { color:  {border: "#41A906", background: "#7BE141", highlight: {border: "#41A906", background: "#A1EC76"}, hover: {border: "#41A906", background: "#A1EC76"}}}  // 3: green
                }
            }
        )
        // Constants can't have dependencies. Inject 'graphProperties' and use options to obtain complete graph config
        .service('graphProperties', function(generalOptions, physicOptions, _) {
            // General options with additional physic configuration
            this.options = _.extend(generalOptions, { physics: physicOptions });

            this.legendOptions = _.extend({}, generalOptions, { interaction: { dragNodes: false, dragView: false, selectable: false, zoomView: false, hover: false, navigationButtons: false } });
        })
});