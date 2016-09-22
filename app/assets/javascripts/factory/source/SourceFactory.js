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