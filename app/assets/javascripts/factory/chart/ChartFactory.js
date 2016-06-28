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
], function (angular) {
    'use strict';

    angular.module('myApp.chartconfig', ['angularMoment', 'myApp.util']);
    angular.module('myApp.chartconfig')
        .factory('chartConfig', ['moment', 'util', function (moment, util) {
            /**
             * This function adds a line representing the current date into the chart.
             *
             * @param {Highchart Object} chart - The highchart object.
             */
            function addPlotLineForNow(chart) {
                chart.xAxis[0].addPlotLine({
                    id: 'now',
                    label: {
                        text: 'now',
                        rotation: 270,
                        x: 15,
                        y: 25
                    },
                    color: '#FF3500',
                    value: moment(),
                    width: 5
                });
            };

            var config = {
                weekdays: [
                    'Sunday', 'Monday', 'Tuesday', 'Wednesday',
                    'Thursday', 'Friday', 'Saturday'
                ],
                monthAbbreviations: [
                    'Jan', 'Feb', 'March', 'Apr', 'May', 'June',
                    'July', 'Aug', 'Sept', 'Oct', 'Nov', 'Dez'
                ],
                // Highcharts options
                highchartsOptions: {
                    lang: {
                        resetZoom: 'Reset view',
                        drillUpText: 'Back to {series.name}',
                        loading: 'Loading...',
                        weekdays: this.weekdays,
                        shortMonths: this.monthAbbreviations
                    }
                },
                // General configuration for the chart
                chartConfig: {
                    options: {
                        chart: {
                            type: 'column',
                           // height: 200,
                            backgroundColor: 'rgba(0,0,0,0)',
                            zoomType: 'xy',
                            resetZoomButton: {
                                position: {
                                    align: 'left',
                                    y: -5
                                }
                            },
                            reflow: true,
                            events: {
                                // Click event, when drill up button is clicked
                                drillup: function () {
                                    // Destroy popovers
                                    $('.popover[role="tooltip"]').popover('destroy');
                                },
                                /*
                                 When a bar is clicked two things shall happen:
                                 1. Load the documents that belong to that bar (i. e. the time frame)
                                 2. Do not drilldown
                                 */
                                drilldown: function (column) {

                                }
                            }
                        },
                        credits: false,
                        xAxis: {
                            title: {
                                text: 'Date'
                            },
                            type: 'category'
                        },
                        yAxis: {
                            title: {
                                text: 'Number of Documents<br>(logarithmic)',
                                offset: 60
                            }
                            ,
                            type: 'logarithmic',
                            tickInterval: 2,
                            stackLabels: {
                                enabled: true,
                                useHTML: true,
                                formatter: function () {
                                    // Only display if not 0
                                    if (this.total > 0) {
                                        return '<span class="stack-label"" data-x="' + this.x + '" style="cursor: pointer;">' + util.formatNumber(this.total) + '</span>';
                                    } else {
                                        return '';
                                    }
                                },
                            },
                            gridLineWidth: 0
                        },
                        plotOptions: {
                            column: {
                                stacking: 'normal',
                            },
                        },
                        legend: {
                            enabled: false,
                        },
                        tooltip: {
                            useHTML: true,
                            formatter: function () {
                                var s = '' + this.series.data[this.x].name + '<br/>'
                                    + '<span style="color:' + this.series.color + '">' + this.series.name
                                    + '</span>: <b>' + this.y + ' Documents</b>';
                                return s;
                            }
                        }
                    },
                    title: {
                        text: ''
                    }
                    ,
                    loading: false,
                    //chart logic
                    func: function (chart) {
                        addPlotLineForNow(chart);
                    }
                }
            }
            return config;
        }])
});