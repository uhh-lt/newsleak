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
    'jquery-json'
], function (angular) {
    'use strict';

    /**
     * observer module:
     * holds all done interaction information and current set filters
     */
    angular.module("myApp.observer", ['play.routing'])
        .factory('ObserverService', ['playRoutes', '$q', function(playRoutes, $q) {
            // Stores callback instances consisting of a callback method and a priority number
            var observerCallbacks = [];
            //holds subscriber functions
            var subscriber = [];
            //all items in order
            var history = [];
            //all item structured by type
            var items = {};
            //types of tracked items
            // TODO: expandNode, egoNetwork not used. Remove them. Make sure index of types still fits! Better don't access via index ...
            var types = ["entity", "metadata", "time", "expandNode", "egoNetwork", "merge", "hide", "edit", "annotate", "fulltext", "reset", "delete", "openDoc", "timeX"];
            var notfiyTypes = ["entity", "metadata", "time", "timeX", "fulltext", "reset"];
            var metadataTypes = [];
            var entityTypes = [];
            var histogramLoD = [];
            types.forEach(function(type) {
                if(type != types[1])
                    items[type] = [];
                else
                    items[type] = {};
            });
            //items[types[1]] = metadataTypes;

            //fetch metadata Types dynamically
            function updateMetadataTypes() {
                var deferred = $q.defer();
                metadataTypes = [];
                playRoutes.controllers.MetadataController.getMetadataTypes().get().then(function (response) {
                    //TODO: hack for ES issue
                    metadataTypes = [];
                    angular.forEach(response.data, function(type){
                        //metadataTypes.push(type.replace(".","_").toLowerCase());
                        metadataTypes.push(type.replace("sender","Sender"));
                    });

                    angular.forEach(metadataTypes, function (type) {
                        items['metadata'][type] = [];
                    });
                    deferred.resolve(metadataTypes);
                    //TODO: how to add metadata filter
                    //items['metadata']['Tags'].push('PREF');
                });
                return deferred.promise;
            }
            // fetch entity types dynamically
            function updateEntityTypes() {
                var deferred = $q.defer();
                entityTypes = [];
                playRoutes.controllers.EntityController.getEntityTypes().get().then(function (response) {
                    entityTypes = angular.copy(response.data);
                    deferred.resolve(entityTypes);
                });
                return deferred.promise;
            }
            // fetch levels of detail for histogram
            function updateLoD() {
                var deferred = $q.defer();
                playRoutes.controllers.HistogramController.getTimelineLOD().get().then(function (response) {
                    histogramLoD = angular.copy(response.data);
                    deferred.resolve(histogramLoD);
                });
                return deferred.promise;
            }

            var lastAdded = -1;
            var lastRemoved = -1;

            //promises.then() waits for factory ready to use
            var promiseMetadata = updateMetadataTypes();
            var promiseEntities = updateEntityTypes();
            var promiseLoD = updateLoD();
            //var promise = $q.all([updateMetadataTypes(), updateEntityTypes()]);

            return {
                /**
                 * Register an observer with callback function and priority for updating the views. The
                 * callbackInstance has the following format: { priority: 1, callback: foo }
                 * IMPORTANT: the callback function has to return a promise a
                 */
                registerObserverCallback: function(callbackInstance){
                    observerCallbacks.push(callbackInstance);
                },
                /**
                 * call all observer callback functions
                 */
                notifyObservers: function(){
                    var callBackPromises = [];
                    var prioritized = observerCallbacks.sort(function(a, b) { return a.priority - b.priority; });

                    angular.forEach(prioritized, function(callbackInstance){
                        callBackPromises.push(callbackInstance.callback());
                    });
                    var promise = $q.all(callBackPromises);
                    return promise;
                },

                /**
                 * add item to history
                 * @param item interaction to add to history
                 * @param notify weather subscribers should be notified something changed
                 * @returns {number} id of added item
                 */
                addItem: function (item, notify = true) {


                    //looking for already existing items
                    var  isDup =false;
                    var action = "added";
                    switch(item.type) {
                        //entity
                        case types[0]:
                            items[item.type].forEach(function(x) {
                                if (item.data.id == x.data.id) isDup = true;
                            });
                            break;
                        //metadata
                        case types[1]:
                            //history.forEach(function(x) {
                            //    if (item.data.id == x.data.id) isDup = true;
                            //});
                            break;
                        //time filter
                        case types[2]:
                            if(items[item.type].length > 0) action = "replaced";
                            break;
                        //time filter
                        case types[13]:
                            if(items[item.type].length > 0) action = "replaced";
                            break;
                        case types[5]:
                            action = "other";
                            break;
                        case types[6]:
                            action = "other";
                            break;
                        case types[7]:
                            action = "other";
                            break;
                        case types[11]:
                            action = "other";
                            break;
                        case types[12]:
                            action = "other";
                            break;
                    }

                    if(isDup) return  -1;

                    lastAdded++;
                    item["action"] = action;
                    item["id"] = angular.copy(lastAdded);
                    item["active"] = true;

                    history.push(item);
                    //if(items.indexOf(item.type) == -1) items[item.type] = [];
                    //adding item structured
                    switch(item.type) {
                        //entity
                        case types[0]:
                            items[item.type].push(item);
                            break;
                        //metadata
                        case types[1]:
                            items[item.type][item.data.type].push(item);
                            break;
                        //time filter
                        case types[2]:
                            items[item.type].push(item);
                            break;
                        //time filter
                        case types[13]:
                            items[item.type].push(item);
                            break;
                        //reset
                        case types[10]:
                            item.active = false;
                            items[item.type].push(item);
                            break;
                        case types[12]:
                            item.active = false;
                            items[item.type].push(item);
                            break;
                        default:
                            items[item.type].push(item);
                            break;
                    }

                    if(notfiyTypes.indexOf(item.type) >= 0 && notify)
                        this.notifyObservers();
                    console.log("added to history: " + item.data.item);
                    return (lastAdded);
                },

                /**
                 * remove item from history and current filter state
                 * @param id
                 * @param type
                 * @param notify
                 */
                removeItem: function (id, type, notify = true) {
                    var toBeRemoved = history[history.findIndex(function (item) {
                        return id == item.id;
                    })];
                    toBeRemoved.active = false;
                    var item = angular.copy(toBeRemoved);
                    lastAdded++;
                    item["id"] = angular.copy(lastAdded);
                    item["action"] = "removed";
                    history.push(item);
                    switch(item.type) {


                        //metadata
                        case types[1]:
                            items[type][item.data.type].splice(items[type][item.data.type].findIndex(function (item) {
                                return id == item.id;
                            }), 1);
                            break;

                        default:
                            items[type].splice(items[type].findIndex(function (item) {
                                return id == item.id;
                            }), 1);
                    }
                    lastRemoved = id;
                    if(notify)
                        this.notifyObservers();
                    console.log("removed from history: " + lastRemoved);
                },




                //TODO: replace type by array to more then one type can be subscribed in a merged item array
                subscribeItems: function (_subscriber, type) {
                    subscriber.push({
                        func: _subscriber,
                        type: type
                    });
                    _subscriber(items[type]);
                },

                subscribeAllItems: function(_subscriber) {
                    subscriber.push({
                        func: _subscriber,
                        type: 'all'
                    });
                    _subscriber(items);
                },

                subscribeHistory: function (_subscriber) {
                    subscriber.push({
                        func: _subscriber,
                        type: 'history'
                    });
                    _subscriber(history);
                },

                /**
                 * IMPORTANT: the callback function has to return an promise
                 * @param _subscriber
                 */
                subscribeReset: function(_subscriber) {
                    subscriber.push({
                        func: _subscriber,
                        type: 'reset'
                    });
                },

                refreshSubscribers: function(reset = true) {
                    var proms = [];
                  angular.forEach(subscriber, function(_subscriber) {
                      switch(_subscriber.type) {
                          case 'reset':
                              if(reset) proms.push(_subscriber.func());
                              break;
                          case 'all':
                              _subscriber.func(items);
                              break;
                          case 'history':
                              _subscriber.func(history);
                              break;
                          default:
                              _subscriber.func(items[_subscriber.type]);
                      }
                  });
                    return $q.all(proms);
                },

                getTypes: function() {
                    return types;
                },

                /**
                 * after async type load, you get the types (promise.then(function(types) [}))
                 * @returns promise types are fetched
                 */
                getMetadataTypes: function() {
                    return promiseMetadata;
                },
                /**
                 * after async type load, you get the types (promise.then(function(types) [}))
                 * @returns promise types are fetched
                 */
                getEntityTypes: function() {
                    return promiseEntities;
                },

                getTimeRange: function() {
                    if(items["time"].length == 0) return ""; else return items["time"][items["time"].length-1].data.item;
                },

                getXTimeRange: function() {
                    if(items["timeX"].length == 0) return ""; else return items["timeX"][items["timeX"].length-1].data.item;
                },

                /**
                 *
                 * @returns {Array} current Filters etc. as (key, val) json for backend request
                 */
                getFacets: function() {
                    var facets = [];
                    if(items.metadata) {
                            metadataTypes.forEach(function(metaType) {
                            if(items.metadata[metaType].length > 0) {
                                var keys = [];
                                angular.forEach(items.metadata[metaType], function(x) {
                                    keys.push(x.data.item);
                                });
                                facets.push({key: metaType.replace(".","_"), data: keys});
                            }
                        });
                        if(facets == 0) facets = [{'key':'dummy','data': []}];

                    } else {
                        facets = [{'key':'dummy','data': []}];
                    }
                    return facets;
                },

                /**
                 * called from timeLines on drillUp to drill up time filters as well
                 */
                drillUpTimeFilter: function() {
                    this.removeItem(items["time"][items["time"].length-1].id,'time');
                    while(items["time"][items["time"].length-1] && items["time"][items["time"].length-1].data.lod == "month")
                        this.removeItem(items["time"][items["time"].length-1].id,'time',false);
                },

                drillUpXTimeFilter: function() {
                    this.removeItem(items["timeX"][items["timeX"].length-1].id,'timeX');
                    while(items["timeX"][items["timeX"].length-1] && items["timeX"][items["timeX"].length-1].data.lod == "month")
                        this.removeItem(items["timeX"][items["timeX"].length-1].id,'timeX',false);
                },
                /**
                 * after async type load, you get the types (promise.then(function(lod) [}))
                 * @returns promise lod are fetched
                 */
                getHistogramLod: function() {
                    return promiseLoD;
                },

                initTypes: function() {
                    promiseMetadata = updateMetadataTypes();
                    promiseEntities = updateEntityTypes();
                    promiseLoD = updateLoD();
                },

                reset: function() {
                    var rootThis = this;
                    history.forEach(function(item) {
                        if(item.active)
                            rootThis.removeItem(item.id, item.type, false);
                    });
                    items = {};
                    types.forEach(function(type) {
                        items[type] = [];
                    });
                    this.initTypes();
                    $q.all([
                        promiseEntities, promiseLoD, promiseMetadata
                    ]).then(function(values) {
                        rootThis.refreshSubscribers().then(function(val) {
                            rootThis.notifyObservers();
                        });

                        rootThis.addItem({
                            type: 'reset',
                            active: false,
                            data: {
                                item: "",
                                description: "Filter has been reset"
                            }
                        }, false);
                    });

                },

                /**
                 * load history/filter state from file
                 * @param input json object holding items and history array
                 */
                loadState: function(input) {
                    console.log(input);
                    var rootThis = this;
                    history.splice(0);
                    history.length = 0;
                    items =  {};
                    types.forEach(function(type) {
                        if(type != types[1])
                            items[type] = [];
                        else
                            items[type] = {};
                    });
                    this.initTypes();

                    //history = angular.copy(input.history);
                    console.log(history);
                    $q.all([
                        promiseEntities, promiseLoD, promiseMetadata
                    ]).then(function() {
                        angular.forEach(input.history, function (item) {
                            history.push(angular.copy(item));
                        });
                            types.forEach(function (type) {
                                $.each(input.items[type], function (index, item) {
                                    if (type != types[1])
                                        items[type].push(item);
                                    else {
                                        if(item[0])
                                            items[type][item[0].data.type] = angular.copy(item);

                                    }
                                });
                            });
                        console.log(items);

                        lastAdded = history[history.length-1].id;
                        lastRemoved = -1;
                        rootThis.refreshSubscribers(false).then(function(val) {
                            rootThis.notifyObservers();
                        });
                        }
                    );
                }
            }
        }]);

});
