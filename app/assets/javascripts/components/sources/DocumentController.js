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
    'ngSanitize',
    'ngMaterial'
], function (angular) {
    'use strict';

    angular.module('myApp.document', ['play.routing', 'ngSanitize', 'ngMaterial'])
        .directive('docContent', ['$compile', 'ObserverService', '_', function($compile, ObserverService, _) {
            return {
                restrict: 'E',
                transclude: true,
                //replace: true,
                scope: {
                    // Need to set-up a bi-directional binding in order to pass an object not a string
                    document: '='
                },
                link: function(scope, element, attrs) {
                    var content = scope.document.content;
                    var entities = scope.document.entities;

                    var categoryColors = { 'PER': '#d73027', 'ORG': '#fee090', 'LOC': '#abd9e9', 'MISC': '#4575b4'};


                    scope.addEntityFilter = function(id) {
                        var el = _.find(entities, function(e) { return e.id == id });
                        ObserverService.addItem({ type: 'entity', data: { id: id, description: el.name, item: el.name, type: el.type }});
                    };

                    function calcHighlightOffsets(text, delimiterStart, delimiterEnd) {
                        var offset = 0;
                        var markerChars = delimiterStart.length;
                        var elements = [];
                        while(true) {
                            var startTag = text.indexOf(delimiterStart, offset);
                            var endTag = text.indexOf(delimiterEnd, offset);
                            // If no more elements stop matching
                            if(startTag == -1 || endTag == -1) break;

                            var startElement = startTag + delimiterStart.length;
                            var match = text.substring(startElement, endTag);
                            elements.push({ startRaw: startElement - markerChars, endRaw: startElement - markerChars + match.length, match: match });
                            // Adjust pointer
                            offset = (endTag + delimiterEnd.length);
                            markerChars += (delimiterStart.length + delimiterEnd.length);
                        }

                        return elements;
                    }

                    scope.renderDoc = function() {

                        if(scope.document.highlighted !== null) {
                            var highlights = calcHighlightOffsets(scope.document.highlighted, '<em>', '</em>');
                            console.log(highlights);
                        }

                        var container =  element;
                        var offset = 0;

                        var sortedSpans = entities.sort(function(a, b) { return a.start - b.start; });
                        sortedSpans.forEach(function(e) {
                            var textEntity = content.slice(e.start, e.end);
                            var fragments = content.slice(offset, e.start).split('\n');

                            fragments.forEach(function(f, i) {
                                container.append(document.createTextNode(f));
                                if(fragments.length > 1 && i != fragments.length - 1) container.append(angular.element('<br />'));
                            });

                            var highlight = angular.element('<span ng-style="{ padding: 0, margin: 0, \'text-decoration\': none,  \'border-bottom\': \'3px solid ' + categoryColors[e.type] + '\'}"></span>');
                            highlight.className = 'highlight-general';
                            var addFilter = angular.element('<a ng-click="addEntityFilter(' + e.id +')" style="text-decoration: none"></a>');

                            addFilter.append(document.createTextNode(textEntity));
                            highlight.append(addFilter);
                            var highlightElement = $compile(highlight)(scope);
                            container.append(highlightElement);

                            offset = e.end;
                        });
                        container.append(document.createTextNode(content.slice(offset, content.length)));
                    };

                    // Init component
                    scope.renderDoc();
                }
            };
        }])
        .controller('DocumentController',
            [
                '$scope',
                '$http',
                '$templateRequest',
                '$sce',
                '$timeout',
                'playRoutes',
                '_',
                'sourceShareService',
                'uiShareService',
                'ObserverService',
                function ($scope,
                          $http,
                          $templateRequest,
                          $sce,
                          $timeout,
                          playRoutes,
                          _,
                          sourceShareService,
                          uiShareService,
                          ObserverService) {

                    var self = this;

                    $scope.sourceShared = sourceShareService;
                    $scope.uiShareService = uiShareService;

                    $scope.tabs = $scope.sourceShared.tabs;

                    // Maps from doc id to list of tags
                    $scope.tags = {};
                    $scope.labels = $scope.sourceShared.labels;


                    self.numKeywords = 15;

                    $scope.removeTab = function (tab) {
                        var index = $scope.tabs.indexOf(tab);
                        $scope.tabs.splice(index, 1);
                    };

                    $scope.observer = ObserverService;

                    $scope.observer.subscribeReset(function() {
                        // Remove all open documents when reset is issued, but keep old bindings to the array.
                        $scope.tabs.length = 0;
                        // Update the document labels when collection is changed
                        updateTagLabels();
                    });


                    init();

                    function init() {
                        updateTagLabels();
                    }

                    $scope.retrieveKeywords = function(doc) {
                        var terms =  [];
                        playRoutes.controllers.DocumentController.getKeywordsById(doc.id, self.numKeywords).get().then(function(response) {
                            response.data.forEach(function(t) { return terms.push(t.term); });
                        });
                        return terms;
                    };

                    $scope.transformTag = function(tag, doc) {
                        // If it is an object, it's already a known tag
                        if (angular.isObject(tag)) {
                            return tag;
                        }
                        // Otherwise try to create new tag
                        $scope.addTag({ label: tag }, doc);
                        // Prevent the chip from being added. We add it with an id in
                        // the addTag method above.
                        return null;
                    };

                    function updateTagLabels() {
                        $scope.labels.length = 0;
                        // Fetch all available document labels for auto-complete
                        playRoutes.controllers.DocumentController.getTagLabels().get().then(function(response) {
                            response.data.labels.forEach(function(l) { $scope.labels.push(l); });
                        });
                    }

                    $scope.initTags = function(doc) {
                        $scope.tags[doc.id] = [];
                        playRoutes.controllers.DocumentController.getTagsByDocId(doc.id).get().then(function(response) {
                            response.data.forEach(function(tag) {
                                $scope.tags[doc.id].push({ label: tag.label, id: tag.id });
                            });
                        });
                    };

                    $scope.addTag = function(tag, doc) {
                        // Do not add tag if already present
                        var match = _.findWhere($scope.tags[doc.id], { label: tag.label });
                        if(match) {
                            return;
                        }

                        playRoutes.controllers.DocumentController.addTag(doc.id, tag.label).get().then(function(response) {
                            $scope.tags[doc.id].push({ id: response.data.id , label: tag.label });
                            // Update labels
                            updateTagLabels();
                        });
                    };

                    $scope.removeTag = function(tag) {
                        playRoutes.controllers.DocumentController.removeTagById(tag.id).get().then(function(response) {
                            // Update labels
                            updateTagLabels();
                        });
                    };

                    $scope.querySearch = function(doc, query) {
                        var results = query ? $scope.labels.filter(createFilterFor(query)) : [];
                        return results;
                    };

                    function createFilterFor(query) {
                        var lowercaseQuery = angular.lowercase(query);
                        return function filterFn(label) {
                            return (label.toLowerCase().indexOf(lowercaseQuery) === 0);
                        };
                    }
                }
            ]);
});
