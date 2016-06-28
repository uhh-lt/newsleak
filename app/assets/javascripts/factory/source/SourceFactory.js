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
], function (angular) {
    'use strict';

    angular.module('myApp.sourcefactory', ['play.routing']);
    angular.module('myApp.sourcefactory')
        // We use this factory to share source objects between this module and the chart module
        .factory('sourceShareService', ['playRoutes' ,function (playRoutes) {
            var sourceShareService = {
                documentOperations: undefined,
                documentList: [],
                documentListInfo: '',
                documentListWarning: '',
                openDocuments: {IDs: [], contents: [], nonHighlightedContents: [], displayHighlightedText: []},
                /*
                 For loading more documents (used when a decade, year or month bar was clicked).
                 index is the start value that is passed to the backend and used as the first
                 LIMIT parameter.
                 lastCategoryFetched is 0 when a decade bar was clicked, 1 on year click and 2 on month click,
                 -1 otherwise.
                 */
                index: -1,
                numberOfDocsToFetch: 100,
                lastCategoryFetched: -1,
                showReloadButton: 0,
                fromYear: 0,
                toYear: 0,
                month: 0,
                day: 0,
                // The next values should be read-only!
                CATEGORY_DECADE: 0,
                CATEGORY_YEAR: 1,
                CATEGORY_MONTH: 2,
                CATEGORY_DAY: 3,
                reset: function (category) {
                    this.documentList = [];
                    this.showReloadButton = -1;
                    this.lastCategoryFetched = category;
                    this.index = 0;
                    this.numberOfDocsToFetch = 100;
                },
                fetchNextDocs: function () {
                    var toIndex = this.index + this.numberOfDocsToFetch - 1;
                    if (this.lastCategoryFetched == this.CATEGORY_DECADE) {
                        playRoutes.controllers.DocumentController.getDocsForYearRange(
                            this.fromYear, this.toYear, this.index, toIndex
                        ).get().then(function (response) {
                            this.addDocs(response.data);
                            }
                        );
                    } else if (this.lastCategoryFetched == this.CATEGORY_YEAR) {
                        playRoutes.controllers.DocumentController.getDocsForYearRange(
                            this.fromYear, this.fromYear, this.index, toIndex
                        ).get().then(function (response) {
                            this.addDocs(response.data);
                            }
                        );
                    } else if (this.lastCategoryFetched == this.CATEGORY_MONTH) {
                        playRoutes.controllers.DocumentController.getDocsForMonth(
                            this.fromYear, this.month, this.index, toIndex
                        ).get().then(function (response) {
                            this.addDocs(response.data);
                            }
                        );
                    } else if (this.lastCategoryFetched == this.CATEGORY_DAY) {
                        playRoutes.controllers.DocumentController.getDocsByDate(this.day).get().then(function (response) {
                            this.addDocs(response.data);
                        });
                    }
                },
                /**
                 * Append documents to the scope variable.
                 *
                 * @param data - The documents to add.
                 */
                addDocs: function (data) {
                if (data.length > 0) {
                    for (var i = 0; i < data.length; i++) {
                        sourceShareService.documentList.push({
                            id: data[i][0],
                            title: data[i][1]
                        });
                    }
                    sourceShareService.showReloadButton = 1;
                    // Compute the next range of documents to fetch
                    sourceShareService.index += sourceShareService.numberOfDocsToFetch;
                    sourceShareService.numberOfDocsToFetch = sourceShareService.numberOfDocsToFetch * 2;
                } else {
                    sourceShareService.showReloadButton = 0;
                }
            }
            };



            return sourceShareService;
        }])
});