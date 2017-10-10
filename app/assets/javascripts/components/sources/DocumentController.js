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
    'ngMaterial',
    'contextMenu',
    'elasticsearch',
    'ui-bootstrap'
], function (angular) {
    'use strict';
    /**
     * document module:
     * - render document content
     * - load additional metdata/keywords for loaded document
     */
    angular.module('myApp.document', ['play.routing', 'ngSanitize', 'ngMaterial', 'ui.bootstrap.contextMenu', 'elasticsearch', 'ui.bootstrap'])
        .service('client', function (esFactory) {
            return esFactory({
              host: 'localhost:9500',
              apiVersion: '5.5',
              log: 'trace'
            });
          })
        .directive('docContent', ['$compile', 'ObserverService', 'EntityService', 'graphProperties',  '_', function($compile, ObserverService, EntityService, graphProperties, _) {
            return {
                restrict: 'E',
                transclude: true,
                //replace: true,
                scope: {
                    // Need to set-up a bi-directional binding in order to pass an object not a string
                    document: '=',
                    reloadDoc: '&withparam'
                },
                link: function(scope, element, attrs) {
                    var content = scope.document.content;
                    var entities = scope.document.entities;
                    scope.addEntityFilter = function(id) {
                        var el = _.find(entities, function(e) { return e.id == id });
                        ObserverService.addItem({ type: 'entity', data: { id: id, description: el.name, item: el.name, type: el.type }});
                    };

                    scope.renderDoc = function() {

                        var highlights = scope.document.highlighted !== null ? calcHighlightOffsets(scope.document.highlighted, '<em>', '</em>') : [];
                        // The plain highlighter with query_string search highlights phrases as multiple words
                        // i.e. "Angela Merkel" -> <em> Angela </em> <em> Merkel </em>. Thus, we need to group
                        // those subsequent elements
                        var merged = groupSubsequentHighlights(highlights);
                        var marks = identifyOverlappingHighlights(entities, merged);

                        var container = element;
                        var offset = 0;
                        marks.forEach(function(e) {
                            var fragments = content.slice(offset, e.start).split('\n');
                            fragments.forEach(function(f, i) {
                                container.append(document.createTextNode(f));
                                if(fragments.length > 1 && i != fragments.length - 1) container.append(angular.element('<br />'));
                            });

                            var highlightElement = undefined;
                            if(_.has(e, 'nested')) {
                                // Two overlapping cases exist. In the first case, we need to append the inner element before the outer one.
                                // In the second case, we need to append it after the outer element.
                                // Case 1
                                // Angela Merkel
                                // Angela
                                // => <span> <span> Angela </span> Merkel </span>
                                // Case 2
                                // Angela Merkel
                                // ...... Merkel
                                // => <span> Angela <span> Merkel </span> </span>
                                if(e.type == 'full-text') {
                                    //Expectation: Nested is always the inner or same element e.g. [[Angela] Merkel]
                                    var innerElement = createNeHighlight(e.nested.id, e.nested.typeId, e.nested.name);
                                    if(e.nested.start == e.start) {
                                        highlightElement = createFulltextHighlight(e.name.substring(e.nested.end - e.start));
                                        highlightElement.prepend(innerElement);
                                    } else {
                                        highlightElement = createFulltextHighlight(e.name.substring(0, e.nested.start - e.start));
                                        highlightElement.append(innerElement)
                                    }
                                } else {
                                    var innerElement = createFulltextHighlight(e.nested.name);
                                    if(e.nested.start == e.start) {
                                        highlightElement = createNeHighlight(e.id, e.typeId, e.name.substring(e.nested.end - e.start));
                                        highlightElement.prepend(innerElement);

                                    } else {
                                        highlightElement = createNeHighlight(e.id, e.typeId, e.name.substring(0, e.nested.start - e.start));
                                        highlightElement.append(innerElement);
                                    }
                                }
                            } else if(e.type == 'full-text')  {
                                highlightElement = createFulltextHighlight(e.name);
                            } else {
                                highlightElement = createNeHighlight(e.id, e.typeId, e.name);
                            }
                            // Append marked element to DOM
                            var compiledElement = $compile(highlightElement)(scope);
                            container.append(compiledElement);
                            // Move the cursor
                            offset = _.has(e, 'nested') && e.nested.end > e.end ? e.nested.end : e.end;
                        });
                        container.append(document.createTextNode(content.slice(offset, content.length)));
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

                    function groupSubsequentHighlights(elements) {
                        var grouped = elements.reduce(function(acc, el) {
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
                        return merged;
                    }

                    function identifyOverlappingHighlights(entities, fulltextElements) {
                        var sortedSpans = entities.concat(fulltextElements).sort(function(a, b) { return a.start - b.start; });
                        var res = sortedSpans.reduce(function(acc, e, i) {
                            // If it's not the last element and full-text match overlaps NE match (subsequent element is always the overlapping one)
                            //if(!_.isUndefined(sortedSpans[i+1]) && (sortedSpans[i+1].start == e.start || sortedSpans[i+1].end == e.end)) {
                            if(!_.isUndefined(sortedSpans[i+1]) && sortedSpans[i+1].start >= e.start && sortedSpans[i+1].start <= e.end) {
                                // Make sure the longest span is the parent
                                if (e.name.length > sortedSpans[i + 1].name.length) {
                                    e.nested = sortedSpans[i + 1];
                                    acc.push(e);
                                    // Mark next as skip
                                    sortedSpans[i + 1].omit = true;
                                } else {
                                    sortedSpans[i + 1].nested = e;
                                }
                            // No overlapping full-text and NE
                            } else if(!_.has(e, 'omit')) {
                                acc.push(e);
                            }
                            return acc;
                        }, []);
                        return res;
                    }

                    function createNeHighlight(id, typeId, name) {
                        var color = graphProperties.options['groups'][typeId]['color']['background'];
                        var innerElement = angular.element('<span ng-style="{ padding: 0, margin: 0, \'text-decoration\': none, \'border-bottom\': \'3px solid ' + color + '\'}"></span>');
                        innerElement.className = 'highlight-general';
                        var addFilter = angular.element('<a id='+ id +' ng-click="addEntityFilter(' + id +')" context-menu="contextMenu" style="text-decoration: none;"></a>');

                        addFilter.append(document.createTextNode(name));
                        innerElement.append(addFilter);
                        return innerElement;
                    }

                    function createFulltextHighlight(name) {
                        var outerElement = angular.element('<span style="padding: 0; margin: 0; background-color: #FFFF00"></span>');
                        outerElement.className = 'highlight-general';
                        outerElement.append(document.createTextNode(name));
                        return outerElement;
                    }
                    // contextMenu for Blacklisting
                    scope.contextMenu = [
                      ['Blacklist', function (scope, event) {
                        EntityService
                          .blacklist([
                            event.target.id
                          ]);
                        scope.$parent.observer.notifyObservers();
                        scope.$parent.reloadDoc(scope.document);
                      }],
                    ];

                    scope.$parent.loadBlacklists(scope.document);

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
                'ObserverService',
                'EntityService',
                'client',
                'esFactory',
                '$uibModal',
                '$log',
                '$document',
                function ($scope,
                          $http,
                          $templateRequest,
                          $sce,
                          $timeout,
                          playRoutes,
                          _,
                          sourceShareService,
                          ObserverService,
                          EntityService,
                          client,
                          esFactory,
                          $uibModal,
                          $log,
                          $document) {

                    var self = this;

                    $scope.sourceShared = sourceShareService;

                    $scope.tabs = $scope.sourceShared.tabs;

                    // Maps from doc id to list of tags
                    $scope.tags = {};
                    $scope.labels = $scope.sourceShared.labels;


                    self.numKeywords = 15;

                    $scope.removeTab = function (tab) {
                        var index = $scope.tabs.indexOf(tab);
                        $scope.tabs.splice(index, 1);
                        if($scope.tabs.length == 0){
                            EntityService.setToggleKeywordGraph();
                        }
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

                    $scope.open = function ($scope, doc, backdrop) {
                      var modalInstance = $uibModal.open({
                        templateUrl: 'whitelistModal.html',
                        animation: true,
                        component: 'modalComponent',
                        size: 'sm',
                        backdrop: backdrop,
                        resolve: {
                          parentScope: function() {
                            return $scope;
                          },
                          doc: function() {
                            return doc;
                          }
                        },
                        controller: ('ModalController', ['$scope', function($scope) {

                            var selectedEntity = $scope.$resolve.parentScope.selectedEntity;
                            var entityTypes = $scope.$resolve.parentScope.entityTypes;
                            var doc = $scope.$resolve.doc;

                            $scope.entityName = selectedEntity.text
                            $scope.entityTypes = entityTypes;
                            $scope.selectedType = '';
                            $scope.isEntityInDoc = $scope.$resolve.parentScope.isEntityInDoc;
                            $scope.isKeyword = $scope.$resolve.parentScope.isKeyword;
                            $scope.isKeyword = false;

                            $scope.toggleType = function (state) {
                              this.$resolve.parentScope.isNewType = !state;
                              $scope.selectedType = '';
                            }

                            $scope.toggleKeyword = function (state) {
                              this.$resolve.parentScope.isKeyword = !state;
                              $scope.selectedType = state === false ? 'key' : '';
                            }

                            $scope.ok = function () {
                              this.$resolve.parentScope.whitelist(selectedEntity, $scope.selectedType, doc);
                              this.modalClose();
                            };

                            $scope.cancel = function () {
                              this.$resolve.parentScope.isKeyword = false;
                              this.$resolve.parentScope.isNewType = false;
                              this.modalClose();
                            };

                            $scope.modalClose = function() {
                              this.$close();
                            };
                          }
                        ])
                      });
                      modalInstance.result.then(function () {
                      }, function () {
                        $log.info('Modal dismissed at: ' + new Date());
                      });
                    };

                    $scope.retrieveKeywords = function(doc) {
                        var terms =  [];
                        // playRoutes.controllers.DocumentController.getKeywordsById(doc.id, self.numKeywords).get().then(function(response) {
                        //    response.data.forEach(function(t) { return terms.push(t.term); });
                        //});
                        client.search({
                            index: $scope.indexName,
                            type: 'document',
                            id: doc.id,
                            body: {
                                query: {
                                    match: {
                                        _id: doc.id
                                    }
                                }
                            }

                        }).then(function (resp) {
                            if(resp.hits.hits[0]._source.Keywords){
                                for(let keyword of resp.hits.hits[0]._source.Keywords) {
                                    terms.push(keyword.Keyword);
                                }
                            }
                        });
                        return terms;
                    };

                    $scope.retrieveLinks = function(doc) {
                        var links = _.pick(doc.meta, function(value, key, obj) {
                            return _.every(_.pluck(value, 'type'), function(t) { return t == 'Link'; })
                        });
                        return links;
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

                    $scope.indexName = '';
                    function getIndexName() {
                      playRoutes.controllers.DocumentController.getIndexName().get().then(function(response) {
                          $scope.indexName = response.data.index;
                          console.log('index name: ' + $scope.indexName);
                      });
                    }

                    // get index name from the back end and print to the console
                    getIndexName();

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
                            if(EntityService.getTagsSelected()){
                                EntityService.reloadKeywordGraph(true);
                            }
                        });
                    };

                    $scope.removeTag = function(tag) {
                        playRoutes.controllers.DocumentController.removeTagById(tag.id).get().then(function(response) {
                            // Update labels
                            updateTagLabels();
                            if(EntityService.getTagsSelected()){
                                EntityService.reloadKeywordGraph(true);
                            }
                        });
                    };

                    $scope.querySearch = function(doc, query) {
                        var results = query ? $scope.labels.filter(createFilterFor(query)) : [];
                        return results;
                    };

                    $scope.isNewType = false;
                    $scope.isEntityInDoc = false;
                    $scope.isKeyword = false;

                    // Enable to select Entity and activate whitelisting modal
                    $scope.showSelectedEntity = function(doc) {
                        $scope.selectedEntity =  $scope.getSelectionEntity(doc.content);
                        var selectedDoc = $scope.tabs.find((t) => { return t.id === doc.id; });
                        var isInDoc = isEntityInDoc(selectedDoc, $scope.selectedEntity);
                        if (!isInDoc && ($scope.selectedEntity.text.length) > 0 && ($scope.selectedEntity.text !== ' ')) {
                          $scope.isEntityInDoc = false;
                          $scope.open($scope, doc, 'static');
                        } else if (isInDoc && ($scope.selectedEntity.text.length) > 0 && ($scope.selectedEntity.text !== ' ')){
                          $scope.isEntityInDoc = true;
                          $scope.open($scope, doc, 'true');
                        }
                    };

                    function isEntityInDoc(selectedDoc, selectedEntity) {
                      var entities = selectedDoc.entities.filter((e) =>
                        {
                          //avoids new entity contains / intersects / inside an existed entity
                          if (((e.start >= selectedEntity.start) &&
                              (e.end <= selectedEntity.end)) ||
                              ((e.start <= selectedEntity.start) &&
                              (e.end >= selectedEntity.end)) ||
                              ((e.start <= selectedEntity.start) &&
                              (e.end >= selectedEntity.start)) ||
                              ((e.start <= selectedEntity.end) &&
                              (e.end >= selectedEntity.end))
                            ) {
                              return e;
                            }
                        }
                      );
                      return entities.length > 0 ? true : false;
                    }

                    $scope.whitelist = function(entity, type, doc){
                      type = type.replace(/\s/g,'');
                      $scope.isKeyword;
                      var blacklists = isBlacklisted(entity, type, $scope.isKeyword);
                      ;
                      if ($scope.isKeyword === true) {
                        if (blacklists.length > 0) {
                          playRoutes.controllers.KeywordNetworkController.undoBlacklistingKeywords(blacklists).get().then(function (response) {
                            $scope.observer.notifyObservers();
                            $scope.reloadDoc(doc);
                            EntityService.reloadKeywordGraph(true);
                          });
                        } else {
                          $scope.esKeyWhitelist(entity.text, doc);
                        }
                      } else {
                        if (blacklists.length > 0) {
                          // Update network and frequency chart
                          playRoutes.controllers.EntityController.undoBlacklistingByIds([blacklists[0].id]).get().then(function(response) {
                            $scope.observer.notifyObservers();
                            $scope.reloadDoc(doc);
                            EntityService.reloadEntityGraph();
                          });
                        } else {
                          playRoutes.controllers.EntityController.getRecordedEntity(entity.text, type).get().then(function (response) {
                            if (response.data.length > 0) {
                              $scope.createNewEntity(entity, type, doc, response.data[0].id);
                              EntityService.whitelist(entity, type, doc.id, response.data[0].id);
                            } else {
                              $scope.esWhitelist(entity, type, doc);
                              EntityService.whitelist(entity, type, doc.id);
                            }
                          });
                        }
                      }
                    };

                    // Get entityTypes from observer service
                    $scope.entityTypes = [];
                    $scope.observer.getEntityTypes().then(function (types) {
                        types.map(function (e) {
                            if (e.name !== null) {
                                $scope.entityTypes.push(e);
                            }
                        });
                    });

                    $scope.selectedType = '';
                    var doc = $scope.tabs;
                    $scope.getSelectionEntity = function(doc) {
                      var text = "";
                      var start = 0;
                      var end = 0;
                      if (window.getSelection) {
                         text = window.getSelection().toString();
                         start = doc.match(text).index;
                         end = start + text.length;
                      } else if (document.selection && document.selection.type != "Control") {
                         text = document.selection.createRange().text;
                      }
                      text = text.trim();
                      return {
                        text,
                        start,
                        end
                      };
                    };

                    $scope.esKeyWhitelist = function(keyword, doc) {
                      client.get({
                        index: $scope.indexName,
                        type: 'document',
                        id: doc.id,
                        source: 'Keywords'
                      }).then(function (response) {
                        var key = response._source.Keywords;
                        if (key !== undefined) {
                          $scope.createNewKeyword(keyword, doc);
                        } else {
                          $scope.createInitKeyword(keyword, doc);
                        }
                      }, function (err, response) {
                        console.trace(err.message);
                      });
                    }

                    $scope.createInitKeyword = function(keyword, doc) {
                      client.update({
                        index: $scope.indexName,
                        type: 'document',
                        id: doc.id,
                        body: {
                          script: "ctx._source.Keywords = [(keyword)]",
                          params: {
                            keyword:  {
                              Keyword: keyword,
                              EntFrequency: 1
                            }
                          }
                        }
                      }).then(function (resp) {
                          $scope.observer.notifyObservers();
                          $scope.reloadDoc(doc);
                          EntityService.reloadKeywordGraph(true);
                      }, function (err) {
                          console.trace(err.message);
                      });
                    }

                    $scope.createNewKeyword = function(keyword, doc) {
                      client.update({
                        index: $scope.indexName,
                        type: 'document',
                        id: doc.id,
                        body: {
                          script: "ctx._source.Keywords.add(keyword)",
                          params: {
                            keyword:  {
                              Keyword: keyword,
                              EntFrequency: 1
                            }
                          }
                        }
                      }).then(function (resp) {
                          $scope.observer.notifyObservers();
                          $scope.reloadDoc(doc);
                          EntityService.reloadKeywordGraph(true);
                      }, function (err) {
                          console.trace(err.message);
                      });
                    }

                    $scope.esWhitelist = function(entity, typeEnt, doc) {
                      client.search({
                        index: $scope.indexName,
                        type: 'document',
                        size: 0,
                        body: {
                          aggs: {
                            max_id: {
                              max: {
                                field: "Entities.EntId"
                              }
                            }
                          }
                        }
                      }).then(function (resp) {
                        $scope.esNewId = (resp.aggregations.max_id.value) + 1;
                        $scope.createNewEntity(entity, typeEnt, doc);
                      }, function (error, response) {
                        $scope.esNewId = null;
                        console.trace(err.message);
                      });
                    }

                    $scope.createNewEntity = function(entity, typeEnt, doc, entId = null) {
                      client.update({
                        index: $scope.indexName,
                        type: 'document',
                        id: doc.id,
                        body: {
                          script: "ctx._source.Entities.add(Entities)",
                          params: {
                            Entities:  {
                              EntId: entId === null ? $scope.esNewId : entId,
                              Entname: entity.text,
                              EntType: typeEnt,
                              EntFrequency: 1
                            }
                          }
                        }
                      }).then(function (resp) {
                          $scope.esNewEntity = resp;
                          $scope.isNewType === false ?
                            $scope.checkNewEntityType(entity, typeEnt, doc, entId)
                            :
                            $scope.createNewEntityType(entity, typeEnt, doc);
                      }, function (err) {
                          $scope.esNewEntity = null;
                          console.trace(err.message);
                      });
                    }

                    $scope.checkNewEntityType = function(entity, typeEnt, doc, entId = null) {
                      var suffixType = typeEnt.toLowerCase();
                      var newType = 'Entities' + suffixType;
                      client.get({
                        index: $scope.indexName,
                        type: 'document',
                        id: doc.id,
                        source: 'Entities'+ suffixType
                      }).then(function (response) {
                        var type = response._source[newType];
                        if (type !== undefined) {
                          $scope.insertNewEntityType(entity, typeEnt, doc)
                        } else {
                          $scope.createNewEntityType(entity, typeEnt, doc);
                        }
                      }, function (err, response) {
                        console.trace(err.message);
                      });
                    }

                    $scope.insertNewEntityType = function(entity, typeEnt, doc, entId = null) {
                      var suffixType = typeEnt.toLowerCase();
                      client.update({
                        index: $scope.indexName,
                        type: 'document',
                        id: doc.id,
                        body: {
                          script: "ctx._source.Entities" + suffixType + ".add(Entities)",
                          params: {
                            Entities:  {
                              EntId: entId === null ? $scope.esNewId : entId,
                              Entname: entity.text,
                              EntFrequency: 1
                            }
                          }
                        }
                      }).then(function (resp) {
                          $scope.esNewEntityType = resp;
                          $scope.observer.notifyObservers();
                          $scope.reloadDoc(doc);
                          EntityService.reloadEntityGraph();
                      }, function (err) {
                          $scope.esNewEntityType = null;
                          console.trace(err.message);
                      });
                    }


                    $scope.createNewEntityType = function(entity, typeEnt, doc) {
                      var suffixType = typeEnt.toLowerCase();
                      client.update({
                        index: $scope.indexName,
                        type: 'document',
                        id: doc.id,
                        body: {
                          script: "ctx._source.Entities" + suffixType + " = [(Entities)]",
                          params: {
                            Entities:  {
                              EntId: $scope.esNewId,
                              Entname: entity.text,
                              EntFrequency: 1
                            }
                          }
                        }
                      }).then(function (resp) {
                          $scope.esNewEntityType = resp;
                          $scope.observer.notifyObservers();
                          $scope.reloadDoc(doc);
                          EntityService.reloadEntityGraph();
                      }, function (err) {
                          $scope.esNewEntityType = null;
                          console.trace(err.message);
                      });
                    }

                    $scope.blacklists = [];
                    $scope.loadBlacklists = function(doc) {
                      playRoutes.controllers.EntityController.getBlacklistsByDoc(doc.id).get().then(function (response) {
                        $scope.blacklists = response.data;
                        playRoutes.controllers.EntityController.getBlacklistedKeywords(doc.id).get().then(function (response) {
                            let i = 1;
                            for(let item of response.data){
                                $scope.blacklists.push({
                                    // id: Long, name: String, entityType: String, freq: Int
                                    id: i,
                                    name: item,
                                    entityType: 'KEYWORD',
                                    freq: 1
                                });
                                i++;
                            }
                        });
                      });
                    }

                    function isBlacklisted(entity, type, isKeyword) {
                      var isBlacklisted = [];
                      if (isKeyword) {
                        $scope.blacklists.filter((e) =>
                          {
                            if((e.entityType === 'KEYWORD') && (e.name === entity.text)){
                                return isBlacklisted.push(e.name);
                            }
                          }
                        );
                      } else {
                        isBlacklisted = $scope.blacklists.filter((e) =>
                          {
                            if ((e.name === entity.text) &&
                                (e.start === entity.start) &&
                                (e.end === entity.end) &&
                                (e.type === type)
                              ) {
                                return e;
                              }
                          }
                        );
                      }
                      return isBlacklisted;
                    }

                    $scope.reloadDoc = function(doc) {
                      $scope.removeTab(doc);
                      var editItem = {
                          type: 'openDoc',
                          data: {
                              id: doc.id,
                              description: "#" + doc.id,
                              item: "#" + doc.id
                          }
                      };

                      $scope.observer.addItem(editItem);

                      playRoutes.controllers.EntityController.getEntitiesByDoc(doc.id).get().then(function (response) {
                          // Provide document controller with document information
                          $scope.sourceShared.tabs.push({
                              id: doc.id,
                              title: doc.id,
                              content: doc.content,
                              highlighted: doc.highlighted,
                              meta: doc.metadata,
                              entities: response.data
                          });
                      });
                    }

                    function createFilterFor(query) {
                        var lowercaseQuery = angular.lowercase(query);
                        return function filterFn(label) {
                            return (label.toLowerCase().indexOf(lowercaseQuery) === 0);
                        };
                    }
                }
            ]);
});
