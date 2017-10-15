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
    angular.module('myApp.entityservice', ['play.routing'])
        .factory('EntityService', ['$rootScope', 'playRoutes', '$timeout', function ($rootScope, playRoutes, $timeout) {
            var keywordScope = null;
            var entityScope = null;

            var toggleEntityGraph = true;
            var toggleKeywordGraph = true;

            return {

                subscribeBlacklist: function(scope, callback) {
                    var handler = $rootScope.$on('notifying-service-event', callback);
                    scope.$on('$destroy', handler);
                },

                subscribeBlacklistKeyword: function(scope, callback) {
                    var handler = $rootScope.$on('notifying-service-event2', callback);
                    scope.$on('$destroy', handler);
                },

                blacklist: function(ids) {
                    // TODO: Move to EntityController
                    playRoutes.controllers.NetworkController.blacklistEntitiesById(ids).get().then(function(response) {
                        $rootScope.$emit('notifying-service-event', { parameter: ids, response: response });
                    });
                },

                blacklistKeyword: function (keyword, id) {
                    playRoutes.controllers.KeywordNetworkController.blacklistKeywordByKeyTerm(keyword).get().then(function (response) {
                        $rootScope.$emit('notifying-service-event2', {parameter: [id], response: response});
                    });
                },

                whitelist: function(entity, type, docId, entId = "empty") {
                    playRoutes.controllers.EntityController.whitelistEntity(
                      entity.text,
                      entity.start,
                      entity.end,
                      type,
                      docId,
                      entId
                    ).get().then(function(response) {
                        $rootScope.$emit('notifying-service-event', { parameter: entity, response: response });
                    });
                },

                toggleTags: function (state, scope) {
                    playRoutes.controllers.KeywordNetworkController.toggleTags(state).get().then(function () {
                        scope.reloadGraph();
                    })
                },

                reloadKeywordGraph: function (toggle = false) {
                    if(keywordScope != null){
                        keywordScope.checkTags(toggle);
                    }
                },

                reloadEntityGraph: function () {
                    if(entityScope){
                      entityScope.reloadGraph();
                    }
                },

                setKeywordScope: function (scope) {
                    keywordScope = scope;
                },

                setEntityScope: function (scope) {
                    entityScope = scope;
                },

                highlightKeywords: function (keywords) {
                    if(keywordScope != null){
                        keywordScope.highlightKeywordNodes(keywords);
                    }
                },

                removeKeywordNodeHighlight: function () {
                    if(keywordScope != null) {
                        keywordScope.removeKeywordNodeHighlight();
                    }
                },

                highlightEntities: function (entities) {
                    if(entityScope != null){
                        entityScope.highlightEntityNodes(entities);
                    }
                },

                removeEntityNodeHighlight: function () {
                    if(entityScope != null) {
                        entityScope.removeEntityNodeHighlight();
                    }
                },

                setToggleEntityGraph: function (state) {
                    toggleEntityGraph = state;
                    if(keywordScope && entityScope){
                        if(toggleEntityGraph){
                            setTimeout(function() {
                                entityScope.resizeNetwork();
                            }, 500);
                        }
                        else {
                            $timeout(function() {
                                keywordScope.resizeNetwork();
                            }, 500);
                        }
                    }
                    else {
                        console.log('scopes not defined');
                    }
                },

                toggleEntityGraph: function () {
                    toggleEntityGraph = ! toggleEntityGraph;
                    if(keywordScope && entityScope){
                        if(toggleEntityGraph){
                            setTimeout(function() {
                                entityScope.resizeNetwork();
                            }, 500);
                        }
                        else {
                            $timeout(function() {
                                keywordScope.resizeNetwork();
                            }, 500);
                        }
                    }
                    else {
                        console.log('scopes not defined');
                    }
                },

                getToggleEntityGraph: function () {
                    return toggleEntityGraph;
                },

                setToggleKeywordGraph: function (state) {
                    toggleKeywordGraph = state;
                    if(keywordScope && entityScope){
                        if(toggleKeywordGraph) {
                            $timeout(function() {
                                keywordScope.resizeNetwork();
                            }, 500);
                        }
                        else {
                            setTimeout(function() {
                                entityScope.resizeNetwork();
                            }, 500);
                        }
                    }
                    else {
                        console.log('scopes not defined');
                    }
                },

                toggleKeywordGraph: function () {
                    toggleKeywordGraph = ! toggleKeywordGraph;
                    if(keywordScope && entityScope){
                        if(toggleKeywordGraph){
                            $timeout(function() {
                                keywordScope.resizeNetwork();
                            }, 500);
                        }
                        else {
                            setTimeout(function() {
                                entityScope.resizeNetwork();
                            }, 500);
                        }
                    }
                    else {
                        console.log('scopes not defined');
                    }
                },

                getToggleKeywordGraph: function () {
                    return toggleKeywordGraph;
                },

                getTagsSelected() {
                    return keywordScope.getTagsSelected();
                },

                getHostAddress() {
                    playRoutes.controllers.KeywordNetworkController.getHostAddress().get().then(function (response) {
                        return response;
                    });
                }
            };
        }])
});
