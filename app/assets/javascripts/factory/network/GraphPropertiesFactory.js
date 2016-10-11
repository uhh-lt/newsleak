define([
    'angular',
    'angularMoment'
], function (angular)
{
    'use strict';

    angular.module('myApp.graphpropertiesfactory', []);
    angular.module('myApp.graphpropertiesfactory')
        // This factory is used to share graph properties between this module and the app.js
        .factory(
        'graphPropertiesShareService',
        [

        function
        (
        )
        {
            var ready = false;
            var graphProperties = {
                // Order: locations, orginaziations, persons, misc
                //categoryColors: ["#8dd3c7", "#fb8072","#bebada", "#ffffb3"],
                categoryColors: ["#8dd3c7", "#fb8072","#bebada", "#ffffb3"],
                categories:
                [
                    {id: 'LOC', full: 'Location', color: '#8dd3c7',img: 'place', singular: 'Location'},
                    {id: 'ORG', full: 'Organization', color: '#fb8072', img: 'account_balance', singular: 'Organisation'},
                    {id: 'PER', full: 'Person', color: '#bebada', img: 'person', singular: 'Person'},
                    {id: 'MISC', full: 'Miscellaneous', color: '#ffffb3', img: 'reorder', singular: 'Miscellaneous'}
                ],
                // Delivers the index of a given category name

                getIndexOfCategory: function (category) {
                    return graphProperties.categories.findIndex(function(e){return e.id == category});
                },
                getCategoryObj: function (category) {
                    console.log(graphProperties.categories);
                    return graphProperties.categories.find(function(e){return e.id == category});
                }
            };

            return graphProperties;
        }])
});
