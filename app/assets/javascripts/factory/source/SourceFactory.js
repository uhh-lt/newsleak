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
    'angular'
], function (angular) {
    'use strict';
    angular.module('myApp.sourcefactory', [])
        // We use this factory to share source objects between this module and the chart module
        .factory('sourceShareService', [function () {
            var sourceShareService = {
                documentList: [],
                documentsInDB: -1,
                openDocuments: {IDs: [], contents: [], nonHighlightedContents: [], displayHighlightedText: []},
                reset: function () {
                    this.documentList = [];
                },
                /**
                 * Append documents to the scope variable.
                 *
                 * @param data - The documents to add.
                 */
                addDocs: function (data) {
                    if (data.length > 0) {
                        angular.forEach(data, function(doc) {
                        var currentDoc = {
                            id: doc.id,
                            metadata: {}
                        };
                        angular.forEach(doc.metadata, function(metadata) {
                                currentDoc.metadata[metadata.key] = metadata.val;
                        });
                        sourceShareService.documentList.push(currentDoc);
                    });
                    }
                }
            };
            return sourceShareService;
        }])
});