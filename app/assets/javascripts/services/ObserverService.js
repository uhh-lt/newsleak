/**
 * Created by flo on 6/10/16.
 *
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
    'jquery-json',
    '../factory/appData'
], function (angular) {
    'use strict';

    angular.module("myApp.observer", ['play.routing', 'angularMoment']);
    angular.module("myApp.observer")
        .factory('ObserverService', ['playRoutes', '$q', function(playRoutes, $q) {
            //holds all observer functions
            var observerCallbacks = [];
            //all items in order
            var history = [];
            //all item structured by type
            var items = [];
            //types of tracked items
            var types = ["entity", "metadata", "time", "expandNode", "egoNetwork", "merge", "hide", "edit", "annotate"];
            var metadataTypes = [];
            var entityTypes = [];
            types.forEach(function(type) {
                items[type] = [];
            });
            items[types[1]] = metadataTypes;

            //fetch metadata Types dynamically
            function updateMetadataTypes() {
                var deferred = $q.defer();
                playRoutes.controllers.MetadataController.getMetadataTypes().get().then(function (response) {
                    metadataTypes = angular.copy(response.data);
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
            //fetch metadata Types dynamically
            function updateEntityTypes() {
                var deferred = $q.defer();
                playRoutes.controllers.EntityController.getEntityTypes().get().then(function (response) {
                    entityTypes = angular.copy(response.data);
                    deferred.resolve(entityTypes);
                    //angular.forEach(metadataTypes, function(type) {
                    //    items['metadata'].push(type);
                    //    items['metadata'][type] = [];
                    //});
                    //TODO: how to add metadata filter
                    //items['metadata']['Tags'].push('PREF');
                });
                return deferred.promise;
            }

            var lastAdded = -1;
            var lastRemoved = -1;

            //promises.then() waits for factory ready to use
            var promiseMetadata = updateMetadataTypes();
            var promiseEntities = updateEntityTypes();

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
                        callback();
                    });
                },
                
                addItem: function (item) {


                    //looking for already existing items
                    var  isDup =false;
                    var action = "added";
                    switch(item.type) {
                        //entity
                        case types[0]:
                            history.forEach(function(x) {
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
                    }

                    if(isDup) return  -1;

                    lastAdded++;
                    item["action"] = action;
                    item["id"] = angular.copy(lastAdded);

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
                    }


                    this.notifyObservers();
                    console.log("added to history: " + item.data.name);
                    return (lastAdded);
                },

                removeItem: function (id, type) {
                    var item = angular.copy(history[history.findIndex(function (item) {
                        return id == item.id;
                    })]);
                    item["action"] = "removed";
                    history.push(item);

                    items[type].splice(items[type].findIndex(function (item) {
                        return id == item.id;
                    }), 1);
                    lastRemoved = id;
                    this.notifyObservers();
                    console.log("removed from history: " + lastRemoved);
                },




                
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
                }
            }
        }]);

});
