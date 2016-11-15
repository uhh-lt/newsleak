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
                            elements.push({ start: startElement - markerChars, end: startElement - markerChars + match.length, name: match, type: 'full-text' });
                            // Adjust pointer
                            offset = (endTag + delimiterEnd.length);
                            markerChars += (delimiterStart.length + delimiterEnd.length);
                        }
                        return elements;
                    }

                    function createNeHighlight(id, type, name, nested) {
                        var innerElement = angular.element('<span ng-style="{ padding: 0, margin: 0, \'text-decoration\': none, \'border-bottom\': \'3px solid ' + categoryColors[type] + '\'}"></span>');
                        innerElement.className = 'highlight-general';
                        var addFilter = angular.element('<a ng-click="addEntityFilter(' + id +')" style="text-decoration: none;"></a>');

                        addFilter.append(document.createTextNode(name));
                        if(!_.isUndefined(nested)) {
                            innerElement.append(nested);
                        }
                        innerElement.append(addFilter);

                        return innerElement;
                    }
                    
                    function createFulltextHighlight(name, nested) {
                        var outerElement = angular.element('<span style="padding: 0; margin: 0; background-color: #FFFF00"></span>');
                        outerElement.className = 'highlight-general';
                        if(!_.isUndefined(nested)) {
                            outerElement.append(nested);
                        }
                        outerElement.append(document.createTextNode(name));

                        return outerElement;
                    }

                    scope.renderDoc = function() {
                        // The plain highlighter with query_string search highlights phrases as multiple words
                        // i.e. "Angela Merkel" -> <em> Angela </em> <em> Merkel </em>
                        var highlights = scope.document.highlighted !== null ? calcHighlightOffsets(scope.document.highlighted, '<em>', '</em>') : [];

                        var grouped = highlights.reduce(function(acc, el) {
                            var seq = acc.length > 0 ? acc.pop(): [];
                            // Running sequence
                            if(seq.length > 0 && _.last(seq).end == el.start - 1) {
                                seq.push(el);
                                acc.push(seq)
                            } else {
                                // End of sequence
                                if(seq.length > 0) acc.push(seq);
                                // Create new starting sequence
                                acc.push([el]);
                            }
                            return acc;
                        }, []);

                        var merged = grouped.map(function(seq) {
                            var start = _.first(seq).start;
                            var end = _.last(seq).end;
                            var name = _.pluck(seq, 'name').join(' ');
                            return { start: start, end: end, name: name, type: "full-text" };
                        });

                        var container =  element;
                        var offset = 0;

                        var sortedSpans = entities.concat(merged).sort(function(a, b) { return a.start - b.start; });
                        var ne = sortedSpans.reduce(function(acc, e, i) {
                            // If it's not the last element and full-text match overlaps NE match
                            if(!_.isUndefined(sortedSpans[i+1]) && sortedSpans[i+1].start == e.start) {
                                // Make sure the longest span is the parent
                                if(e.name.length > sortedSpans[i+1].name.length) {
                                    e.nested = sortedSpans[i + 1];
                                    acc.push(e);
                                    // Mark next as skip
                                    sortedSpans[i + 1].omit = true;
                                } else {
                                    sortedSpans[i+1].nested = e;
                                }
                            // No overlapping full-text and NE
                            } else if(!_.has(e, 'omit')) {
                                acc.push(e)
                            }
                            return acc;
                        }, []);

                        ne.forEach(function(e) {
                            var textEntity = content.slice(e.start, e.end);
                            var fragments = content.slice(offset, e.start).split('\n');

                            fragments.forEach(function(f, i) {
                                container.append(document.createTextNode(f));
                                if(fragments.length > 1 && i != fragments.length - 1) container.append(angular.element('<br />'));
                            });

                            if(_.has(e, 'nested')) {
                                console.log(e);
                                if(e.type == 'full-text') {
                                    //Expectation: Nested is always the inner or same element e.g. [[Angela] Merkel]
                                    var innerElement = createNeHighlight(e.nested.id, e.nested.type, e.nested.name, undefined);
                                    var outerElement = createFulltextHighlight(e.name.substring(e.nested.end - e.start), innerElement);
                                    var highlightElement = $compile(outerElement)(scope);
                                    container.append(highlightElement);
                                } else {
                                    var innerElement = createFulltextHighlight(e.nested.name, undefined);
                                    var outerElement = createNeHighlight(e.id, e.type, e.name.substring(e.nested.end - e.start), innerElement);
                                    var highlightElement = $compile(outerElement)(scope);
                                    container.append(highlightElement);
                                }
                            } else if(e.type == 'full-text')  {
                                var highlight = createFulltextHighlight(textEntity, undefined);
                                var highlightElement = $compile(highlight)(scope);
                                container.append(highlightElement);
                            } else {
                                var highlight = createNeHighlight(e.id, e.type, e.name);
                                var highlightElement = $compile(highlight)(scope);
                                container.append(highlightElement);
                            }
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
