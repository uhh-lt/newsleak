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
    '../../factory/util'
], function (angular) {
    'use strict';

    angular.module('myApp.highlightfactory', []);
    angular.module('myApp.highlightfactory')
        .factory('highlightShareService', function () {
            var highlightShareService = {
                // Order has to match the one defined in NetworkController#graphPropertiesShareService#graphProperties#categoryColors
                wordsToHighlight: [
                    [], [], [], []
                ],
                wordsToUnderline: [
                    [], [], [], []
                ],
                // This is used to watch whether the wordsToHighlight array or the
                // wordsToUnderline array was changed as $watchCollection doesn't work
                // (at least for me). Obviously, this is not the best solution
                // as wasChanged always has to be set when wordsToHighlight or
                // wordsToUnderline is modified
                wasChanged: false
            };

            return highlightShareService;
        })
});