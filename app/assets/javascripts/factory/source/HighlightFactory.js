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