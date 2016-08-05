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

        angular.module('myApp.metafactory', []);
        angular.module('myApp.metafactory')
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
                        categories: 'test'
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