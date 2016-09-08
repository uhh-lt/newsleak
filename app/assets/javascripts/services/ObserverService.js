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
    'jquery-json',
    '../factory/appData'
], function (angular) {
    'use strict';

    angular.module("myApp.observer", ['play.routing', 'angularMoment'])
        .factory('ObserverService', ['playRoutes', '$q', '$timeout', function(playRoutes, $q, $timeout) {
            //holds all observer functions
            var observerCallbacks = [];
            //all items in order
            var history = [];
            //all item structured by type
            var items = [];
            //types of tracked items
            var types = ["entity", "metadata", "time", "expandNode", "egoNetwork", "merge", "hide", "edit", "annotate", "fulltext", "reset", "delete", "openDoc"];
            var metadataTypes = [];
            var entityTypes = [];
            var histogramLoD = [];
            types.forEach(function(type) {
                items[type] = [];
            });
            items[types[1]] = metadataTypes;

            //fetch metadata Types dynamically
            function updateMetadataTypes() {
                var deferred = $q.defer();
                metadataTypes = [];
                playRoutes.controllers.MetadataController.getMetadataTypes().get().then(function (response) {
                    //TODO: hack for ES issue
                    metadataTypes = [];
                    angular.forEach(response.data, function(type){
                        metadataTypes.push(type.replace(".","_"));
                    });

                    angular.forEach(metadataTypes, function (type) {
                        items['metadata'].push(type);
                        items['metadata'][type] = [];
                    });
                    deferred.resolve(metadataTypes);
                    //TODO: how to add metadata filter
                    //items['metadata']['Tags'].push('PREF');
                });
                return deferred.promise;
            }
            //fetch entity Types dynamically
            function updateEntityTypes() {
                var deferred = $q.defer();
                entityTypes = [];
                playRoutes.controllers.EntityController.getEntityTypes().get().then(function (response) {
                    entityTypes = angular.copy(response.data);
                    deferred.resolve(entityTypes);
                });
                return deferred.promise;
            }
            //fetch levels of detail for histogram
            function updateLoD() {
                var deferred = $q.defer();
                playRoutes.controllers.HistogramController.getHistogramLod().get().then(function (response) {
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
                 * register an observer with callback function
                 */
                registerObserverCallback: function(callback){
                    observerCallbacks.push(callback);

                },
                /**
                 * call all observer callback functions
                 */
                notifyObservers: function(){
                    angular.forEach(observerCallbacks, function(callback){
                        $timeout(callback,0);
                    });
                },
                
                addItem: function (item) {


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
                        //reset
                        case types[10]:
                            item.active = false;
                            items[item.type].push(item);
                            break;
                        default:
                            items[item.type].push(item);
                            break;
                    }


                    this.notifyObservers();
                    console.log("added to history: " + item.data.name);
                    return (lastAdded);
                },

                removeItem: function (id, type) {
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
                    this.notifyObservers();
                    console.log("removed from history: " + lastRemoved);
                },




                //TODO: replace type by array to more then one type can be subscribed in a merged item array
                subscribeItems: function (_subscriber, type) {
                    _subscriber(items[type]);
                },

                subscribeAllItems: function(_subscriber) {
                    _subscriber(items);
                },

                subscribeHistory: function (_subscriber) {
                    _subscriber(history);
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
                    if(items["time"].length == 0) return ""; else return items["time"][items["time"].length-1].data.name;
                },
                drillUpTimeFilter: function() {
                    this.removeItem(items["time"][items["time"].length-1].id,'time');
                    while(items["time"][items["time"].length-1] && items["time"][items["time"].length-1].data.lod == "month")
                        this.removeItem(items["time"][items["time"].length-1].id,'time');
                },
                /**
                 * after async type load, you get the types (promise.then(function(lod) [}))
                 * @returns promise lod are fetched
                 */
                getHistogramLod: function() {
                    return promiseLoD;
                },

                reset: function() {
                    var rootThis = this;
                    history.forEach(function(item) {
                        if(item.active)
                            rootThis.removeItem(item.id, item.type);
                    });

                    this.addItem({
                        type: 'reset',
                        active: false,
                        data: {
                            name: "Filter reseted"
                        }
                    });
                    types.forEach(function(type) {
                        if(type != types[1])
                            items[type].splice(0,items[type].length);
                        else {
                            angular.forEach(metadataTypes, function (mtype) {
                                items[type][mtype].splice(0, items[type][mtype].length);
                            });
                        }
                    });
                    this.notifyObservers();
                },

                loadState: function(input) {
                    history.splice(0,  history.length);
                    items.splice(0,  history.length);
                    angular.forEach(input.history, function(item) {
                        history.push(item);
                    });
                    angular.forEach(input.items, function(item) {
                        items.push(item);
                    });
                    this.notifyObservers();
                }
            }
        }]);

});
