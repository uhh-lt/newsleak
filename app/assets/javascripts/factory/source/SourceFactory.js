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
    'ngMaterial'
], function (angular) {
    'use strict';
    /**
     * sourceFactory:
     * Holds all loaded Document information and current Document List
     */
    angular.module('myApp.sourcefactory', ['ngMaterial'])
        .factory('sourceShareService', ['$mdDialog', '_', function($mdDialog, _) {
            var sourceShareService = {
                documentList: [],
                documentsInDB: -1,
                // Distinct document labels
                labels: [],
                tabs: [],
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
                            content: doc.content,
                            highlighted: doc.highlighted,
                            metadata: {}
                        };
                        angular.forEach(doc.metadata, function(metadata) {
                            if(!currentDoc.metadata.hasOwnProperty(metadata.key)) {
                                currentDoc.metadata[metadata.key] = [];
                            }
                            currentDoc.metadata[metadata.key].push({'val': metadata.val, 'type': metadata.type });
                        });
                        sourceShareService.documentList.push(currentDoc);
                    });
                    }
                },
                pluckMetaValues: function(metalist) {
                    return _.pluck(metalist, 'val');
                },
                showMetaDialog: function($event, metadata) {
                    var parentEl = angular.element(document.body);
                    $mdDialog.show({
                        parent: parentEl,
                        targetEvent: $event,
                        template:
                        '<md-dialog aria-label="Metadata">' +
                        '  <md-dialog-content>'+
                        '<md-subheader class="md-no-sticky">Metadata</md-subheader>'+
                        '    <md-list class="md-dense">'+
                        '      <md-list-item ng-repeat="(key, value) in items">'+
                        '       <p class="md-body-2"><b>{{ key }}</b>: {{ pluckMetaValues(value).join(", ") }}</p>' +
                        '      '+
                        '    </md-list-item></md-list>'+
                        '  </md-dialog-content>' +
                        '  <md-dialog-actions>' +
                        '    <md-button ng-click="closeDialog()" class="md-primary">' +
                        '      Close' +
                        '    </md-button>' +
                        '  </md-dialog-actions>' +
                        '</md-dialog>',
                        locals: {
                            items: metadata
                        },
                        controller: DialogController
                    });
                    function DialogController($scope, $mdDialog, items) {
                        $scope.items = items;
                        $scope.closeDialog = function() {
                            $mdDialog.hide();
                        };
                        $scope.pluckMetaValues = function(metadatalist) { return sourceShareService.pluckMetaValues(metadatalist); }
                    }
                }
            };
            return sourceShareService;
        }])
});