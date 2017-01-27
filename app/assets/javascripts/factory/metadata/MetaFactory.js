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

define(['angular'],
    function (angular)
    {
        'use strict';
        /**
         * hold initial configuration for metadata/entity bar chart view
         */
        angular.module('myApp.metafactory', [])
            .factory('metaShareService', function()
            {
                var metaShareService =
                {
                    //Global Metadata bar chart config
                    chartConfig : {
                    chart: {
                        type: 'bar',
                        reflow: true
                    },
                    credits: false,
                    legend: {
                        enabled: false

                    },
                    plotOptions: {
                        series: {
                        },
                        bar: {
                            dataLabels: {
                                enabled: true
                              //  align: 'right'
                            },
                            grouping: false,
                            shadow: false
                        }

                    },
                    tooltip: {
                        enabled: false
                    },
                    title: {
                        text: ''
                    },
                    exporting: {
                        enabled: false
                    },
                    xAxis: {
                        categories: []
                    },
                    yAxis: {

                        title: {
                            text: ''
                        },
                        type: 'logarithmic',
                        tickInterval: 1,
                        tickAmount: 0,
                        labels: {
                            enabled: false
                        }
                    }

                }




                };

                return metaShareService;
            });
    }
);