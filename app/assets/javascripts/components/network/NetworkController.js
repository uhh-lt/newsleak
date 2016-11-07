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
    'angularMoment',
    'd3',
    'awesome-slider',
    'toggle-switch'
], function (angular) {
    'use strict';

    angular.module('myApp.network', ['play.routing', 'angularMoment', 'angularAwesomeSlider', 'toggle-switch']);
    angular.module('myApp.network')

        .controller('NetworkController',
        [
            '$scope',
            '$timeout',
            '$uibModal',
            '$window',
            'playRoutes',
            'appData',
            'moment',
            'graphPropertiesShareService',
            'highlightShareService',
            'uiShareService',
            'toolShareService',
            'ObserverService',
        function (
            $scope,
            $timeout,
            $uibModal,
            $window,
            playRoutes,
            appData,
            moment,
            graphPropertiesShareService,
            highlightShareService,
            uiShareService,
            toolShareService,
            ObserverService
            )
        {
            var sessionid = Math.random().toString(36).substr(2, 5);

            $scope.graphShared = graphPropertiesShareService;
            $scope.uiShareService = uiShareService;

            $scope.observer = ObserverService;
            $scope.observer_subscribe = function(history) { $scope.history = history};
            $scope.observer.subscribeHistory($scope.observer_subscribe, "");
            /**
             * subscribe entity and metadata filters
             */
            $scope.observer_subscribe_entity = function(items) { $scope.entityFilters = items};
            $scope.observer_subscribe_metadata = function(items) { $scope.metadataFilters = items};
            $scope.observer_subscribe_fulltext = function(items) { $scope.fulltextFilters = items};
            $scope.observer.subscribeItems($scope.observer_subscribe_entity,"entity");
            $scope.observer.subscribeItems($scope.observer_subscribe_metadata,"metadata");
            $scope.observer.subscribeItems($scope.observer_subscribe_fulltext,"fulltext");


            const GRAVITATION_HEIGHT_SUBTRACT_VALUE = 126;

            $scope.maxNodeFreq = 251287;
            $scope.maxEdgeFreq = 81337;

            $scope.minNodeRadius = 7;
            $scope.maxNodeRadius = 30;
            $scope.minEdgeWidth  = 3;
            $scope.maxEdgeWidth  = 10;

            $scope.categories = {
                'LOC': {full: 'Location', color: '#8dd3c7', img: 'place', singular: 'Location', number:2},
                'ORG': {full: 'Organization', color: '#fb8072', img: 'account_balance', singular: 'Organisation', number:1},
                'PER': {full: 'Person', color: '#bebada', img: 'person', singular: 'Person', number:0},
                'MISC': {full: 'Miscellaneous', color: '#ffffb3', img: 'reorder', singular: 'Miscellaneous', number:3}
            };

            $scope.showFullTooltip =  {active: false};

            function fireEvent_updateDocs() {
                $scope.$emit('updateDocs-up', selectedNodes.map(function(n){
                    return n.id
                }));
                console.log(selectedNodes);
            }

            // determines the display of most or least frequency entities/edges
            // gets updated by toggle
            $scope.freqSorting = {least: false};

            $scope.$on('startGuidance-down', function(event, arg) {
                console.log("received event startGuidance");
                console.log(arg);
                getGuidanceNodes(arg,false)});

            var selectedNodes = new Array();
            $scope.selectedNodes = selectedNodes;
            var selectedEdges = new Array();
            var selectionColor = '#FFB500';

            var oldFocusNodeId = 1232028; //speichert die Id des Knoten der zuletzt Fokus der Guidance war
            toolShareService.updateGuidance = function () {
                if (toolShareService.UIitemsChanged) {
                    toolShareService.UIitemsChanged = false;
                    getGuidanceNodes(oldFocusNodeId, false);
                }
            };
            toolShareService.undoGuidance = function () {
                undoStep(sessionid);
            };
            toolShareService.redoGuidance = function () {
                redoStep(sessionid);
            };

            $scope.observer.registerObserverCallback(function (){
                var history = $scope.observer.getHistory();
                if (["entity", "metadata", "time", "fulltext"].includes(history[history.length-1].type)){
                    /*
                    var disableNode = angular.copy(history).reverse().find(function (item) {
                        return item.type === "guidance" && item.data.id === oldFocusNodeId;
                    });
                    if (disableNode !== undefined) {
                        history.find(function (item) {
                            return disableNode.id === item.id
                        }).active = false;
                    }*/
                    getGuidanceNodes(oldFocusNodeId,false);
                }});


            $scope.edgePrios =[
                {class: "btn", color: "#FF1111", id:"eo0", number:0, img:"not_interested", text:"ignored", textcolor:"#FFFFFF", iconstyle:""},
                {class: "btn", color: "#FFFFFF"/*"#777777"*/, id:"eo1", number:1, img:"crop_square", text:"treated normally", textcolor:"#000000", iconstyle:"opacity: 0;"},
                {class: "btn", color: "#83a2d6", id:"eo2", number:2, img:"favorite_border", text:"preffered", textcolor:"#FFFFFF", iconstyle:""},
                {class: "btn", color: "#2759ac", id:"eo3", number:3, img:"favorite", text:"extremly preffered", textcolor:"#FFFFFF", iconstyle:""}];

            $scope.setEdgePrio = function(sourceType,targetType,state){
                if (!(toolShareService.UIitems[$scope.categories[sourceType].number][$scope.categories[targetType].number] == state)){
                    toolShareService.UIitems[$scope.categories[sourceType].number][$scope.categories[targetType].number] = state;
                    toolShareService.UIitems[$scope.categories[targetType].number][$scope.categories[sourceType].number] = state;
                    getGuidanceNodes(oldFocusNodeId, false);
                }
            };

            var tooltip = d3.select("body").append("div").attr("class", "tooltip");

            toolShareService.getSelectedNodes = function(){return selectedNodes;};
            toolShareService.getSelectedElementsText = selectedElementsText;

            toolShareService.editNameListener.push(editName);
            toolShareService.editNameListener.push(sendEditedName);
            toolShareService.editTypeListener.push(editType);
            toolShareService.editTypeListener.push(sendEditedType);

            toolShareService.annotateListener.push(addAnnotation);

            toolShareService.mergeListener.push(merge);
            //toolShareService.mergeListener.push(sendMerged);

            toolShareService.deleteListener.push(remove);

            toolShareService.hideListener.push(hideSelected);

            toolShareService.isViewLoading = function(){return $scope.isViewLoading};
            toolShareService.enableOrDisableButtons = enableOrDisableButtons;


            var nodes = [];
            var edges = [];
            $scope.contextCache = {};
            var guidanceStepCounter= 0;
            var color = d3.scale.ordinal()
                .domain(["LOC", "ORG", "PER", "MISC"])
                .range($scope.graphShared.categoryColors);
            // Order: locations, orginaziations, persons, misc

            var focusColor = "#FF0000";
            var neutralNodeColor = "#000000";
            var neutralEdgeColor = "#B0B0B0";

            //console.log([color("LOC"), color("ORG"), color("PER"), color("MISC")])
            /*var radius     = d3.scale.sqrt()
                                    .domain([1,$scope.maxNodeFreq])
                                    .range([$scope.minNodeRadius, $scope.maxNodeRadius]);*/
            var radius     = d3.scale.sqrt()
                                    .domain([1,$scope.maxNodeFreq])
                                    .range([$scope.minNodeRadius, $scope.maxNodeRadius]);
            var edgeScale  = d3.scale.log()
                                    .domain([1,$scope.maxEdgeFreq])
                                    .range([$scope.minEdgeWidth,$scope.maxEdgeWidth]);

            // For zooming
            var currentScale = 1;
            $scope.zm      = d3.behavior.zoom()
                                    .scaleExtent([0.5, 3])
                                    .on('zoom', function(){
                                        force.alpha(0.01)
                                        currentScale = d3.event.scale;
                                        svg.selectAll('g').attr('transform', 'translate('
                                            + (-viewBoxX) + ',' + (-viewBoxY) + ')' + 'scale('
                                            + d3.event.scale + ')');
                                        //force.alpha(0.0);
                                    });
            // For dragging
            var viewBoxX   = 0;
            var viewBoxY   = 0;
            var drag       = d3.behavior.drag()
                                    .on('drag', function(){
                                        force.alpha(0.01);
                                        viewBoxX -= d3.event.dx;
                                        viewBoxY -= d3.event.dy;
                                        svg.selectAll('g').attr('transform', 'translate('
                                            + (-viewBoxX) + ',' + (-viewBoxY) + ')' + 'scale('
                                            + currentScale + ')');
                                    });

            var svg;

            /*ObserverService.getEntityTypes().then(function(types)
            {
                console.log(types);
                types.forEach(function(e)
                {
                    $scope.graphShared.categories.push({id: e, full: e, color: '#ffffb3'});
                })
            });*/
            getGuidanceNodes(oldFocusNodeId,false);




            /**
             * This function updates the text with the name of the selected elements.
             */
            function selectedElementsText(){
                var selectedElements = "";
                for(var i=0; i<selectedNodes.length; i++){
                    selectedElements += selectedNodes[i].name;
                    if(i < selectedNodes.length - 2)
                        selectedElements += ", ";
                    else if(i == selectedNodes.length - 2)
                        if(selectedEdges.length == 0)
                            selectedElements += " and ";
                        else
                            selectedElements += ", ";
                }
                if(selectedNodes.length != 0){
                    if(selectedEdges.length == 1)
                        selectedElements += " and ";
                    else if(selectedEdges.length > 1)
                        selectedElements += ", ";
                }
                for(var i=0; i<selectedEdges.length; i++){
                    selectedElements += selectedEdges[i].target.name + " <--> " + selectedEdges[i].source.name;
                    if(i < selectedEdges.length - 2)
                        selectedElements += ", ";
                    else if(i == selectedEdges.length - 2)
                        selectedElements += " and ";
                }
                return selectedElements;
            }


            /**
             * This function unselects all nodes. As a result, the words to highlight
             * have to be updated as well
             */
            function unselectNodes(){
                // selectedNodes = new Array();
                // d3.selectAll('circle').each(function(d){
                //     d3.select(this).style('stroke-width', 1.5)
                //                    .style('stroke',function (e) {if (this.isFocusNode) {return '#FF0000'} else  {return '#000000'}});
                // });
                // console.log(node.select('circle'))
                // console.log(d3.selectAll('circle'))
                 node.select('circle').style('stroke',function (d) {// .log(d.name + " " + d.isFocusNode);
                     if (d.isFocusNode) {return focusColor} else  {return neutralNodeColor}})
                     .style('stroke-width', 1.5);

                enableOrDisableButtons();
                // For the highlighting
                for (var i = 0; i < highlightShareService.wordsToHighlight.length; i++) {
                    highlightShareService.wordsToHighlight[i] = [];
                }
                highlightShareService.wasChanged = true;
                selectedNodes = [];
                fireEvent_updateDocs();
            }

            /**
             * This function unselects all edges.
             */
            function unselectEdges(){
                link.style("stroke", function (d) {
                    if (d.uiLevel > 1) {return toolShareService.priorityToColor[d.uiLevel]} else {return neutralEdgeColor}});


                selectedEdges = new Array();
                link.style("stroke", function (d) {
                    if (d.uiLevel > 1) {return toolShareService.priorityToColor[d.uiLevel]} else {return neutralEdgeColor}})
                                   .style('opacity', .8);
                edgelabels.style('fill', '#000000').attr('font-weight', 'normal');
                enableOrDisableButtons();
            }

            /**
             * mark the specified node as selected
             *
             * @param node
             * 		the node to select
             */
            function selectNode(node)
			{
				selectedNodes.push(node);

				d3.select("#nodecircle_" + node.id)
					.style('stroke-width', 5)
              		.style('stroke', selectionColor);

              	// Add the selected node name to the words that get highlighted
                // However, add it only once to make deletion of that entry easier
                if (highlightShareService.wordsToHighlight[$scope.graphShared.getIndexOfCategory(node.type)].indexOf(node.name) < 0) {
                     highlightShareService.wordsToHighlight[$scope.graphShared.getIndexOfCategory(node.type)].push(node.name);
                     highlightShareService.wasChanged = true;
                }

                fireEvent_updateDocs();
                // console.log("doc update up");
                // console.log(selectedNodes)
            }

            /**
             * mark the specified node as not
             * selected
             *
             * @param node
             *		the node to unselect
             */
            function unselectNode(node)
            {
            	console.log("unselect "+node.name+node);
                selectedNodes.splice(selectedNodes.indexOf(node),1);
            	d3.select("#nodecircle_" + node.id)
            		.style('stroke-width', 1.5)
                    .style('stroke', '#000000');

                // Remove the selected node name from the words that get highlighted
                var index = highlightShareService.wordsToHighlight[$scope.graphShared.getIndexOfCategory(node.type)].indexOf(node.name);
                if (index > -1) {
                    highlightShareService.wordsToHighlight[$scope.graphShared.getIndexOfCategory(node.type)].splice(index, 1);
                    highlightShareService.wasChanged = true;
                }
                fireEvent_updateDocs();
            }

            function getNodeById(id)
            {
            	return nodes.filter(
            		function(element, index, array)
            		{
            			return element.id === id;
            		}
            	)[0];
            }

			/**
			 *	add an annotation to the specified Node
			 *
			 *	@param node
			 *		the node to attach the annotation
			 *	@param text
			 *		the text to attach to the annotation
			 */
            function addAnnotation(node, text)
            {
            	d3.select("#node_" + node.id)
            		.append('foreignObject')
            		.attr('id', function(d){return 'nodebuttonannotate_' + d.id})
            		.attr('width', '24')
            		.attr('height', '24')
            		.attr('x', function(d){return radius(d.docCount)-2;})
            		.attr('y', /*function(d){return (radius(d.freq));}*/-12)
            		.append('xhtml:body')
            		.append('button')
            		.attr('class', 'btn btn-xs btn-default neighbor-button')
            		//.html('<button type="button" id="nodebuttonannotate_" class="btn btn-xs btn-default neighbor-button"><i class="glyphicon glyphicon-comment"></i></button>')
            		.on('mouseup', function(){alert(text);})
            		.append('i')
            		.attr('class', 'glyphicon glyphicon-comment')

                //track annotating nodes within observer
                $scope.observer.addItem({
                    type: 'annotate',
                    data: {
                        id: node.id,
                        name: '<'+node.name+'-'+text+'>',
                        text: text
                    }
                });
            }


            // create new force layout
            var force;
            var link;
            var node;
            var edgepaths;
            var edgelabels;

            //TODO: refactor from uiShare to onresize angular material
            /**
             * Calculates and sets the new gravitation figures for the graph.
             */
            function calculateNewForceSize() {
                if (force != undefined) {
                    force.size([
                        $scope.uiShareService.mainContainerWidth,
                        $scope.uiShareService.mainContainerHeight
                    ]);
                    force.start();
                }
            }


            /**
             * Set the height of the legend popover.
             */
            function setLegendPopoverHeight() {
                var legendPopoverHeight = $scope.uiShareService.mainContainerHeight - 140;
                $('.popover-content > .legend-actual-content').css('max-height', legendPopoverHeight);
            }

            /**
             * Whenever the size of the graph container is changed, the gravitation parameter is updated.
             * At the moment, the entire graph needs to be redrawn in order for the changes to take effect.
             * TODO: There might be a better solution without redrawing the entire graph.
             * Also, the height of the popover is updated.
             */
            $scope.$watch('uiShareService', function() {
                calculateNewForceSize();
                //setLegendPopoverHeight();
            }, true);

            angular.element($window).bind('resize', function () {
                calculateNewForceSize();
            });


            /*
             * This function creates the SVG where the graph will be created in.
             */
            function createSVG(){
                d3.select('#network-graph').select("svg").remove();
                svg = d3.select('#network-graph')
                    .append('svg')
                    .attr('width', '100%')
                    .attr('height', '100%')
                    .append('g')
                    .call($scope.zm)
                    .on('dblclick.zoom', null)  // Disable doubleclicking zoom.
                    // Disable the call of the zoom-function when clicking and moving.
                    .on('mousedown.zoom', null)
                    .on('touchstart.zoom', null)
                    .on('touchmove.zoom', null)
                    .on('touchend.zoom', null);

                //Container for the gradients
                var defs = svg.append("defs");

                //Filter for the outside glow
                var filter = defs.append("filter")
                    .attr("id","glow");
                //filter.append("feColorMatrix")
                //    .attr("type", "matrix")
                //    .attr("values", "0 0 0 0 0.51372549019, 0 0 0 0 0.63529411764, 0 0 0 0 0.83921568627, 0 0 0 1 0");

                filter.append("feGaussianBlur")
                    .attr("stdDeviation","3.5")
                    .attr("result","coloredBlur");

                var feMerge = filter.append("feMerge");
                feMerge.append("feMergeNode")
                    .attr("in","coloredBlur");
                feMerge.append("feMergeNode")
                    .attr("in","SourceGraphic");

                var time;
                // Add "background". Otherwise zooming would only be possible with the cursor above nodes/edges.
                svg.append('rect')
                    .call(drag)
                    .attr('width', '100%')
                    .attr('height', '100%')
                    .attr('opacity', '0')
                    .on('mousedown', function(){
                        // On mousedown store the current time in milliseconds
                        time = moment();

                        var tooltip = d3.select("#node-tooltip").transition().duration(300).style("opacity", 0);
                        tooltip.style("pointer-events","none");

                        $scope.showFullTooltip =  {active: false};
                    })
                    .on('mouseup', function(d){
                        // Unselect nodes/edges when clicking on the background without dragging.
                        var now = moment();
                        if(time + 500 > now){
                            unselectNodes();
                            unselectEdges();
                        }
                    });

                link = svg.append('g').selectAll('.link');
                edgepaths = svg.append('g').selectAll(".edgepath");
                edgelabels = svg.append('g').selectAll(".edgelabel");
                node = svg.append('g').selectAll('.node');
            }

            /**
             * resets the graph and starts the force layout
             */
            function start(callback){//Graph zeichnen
                    console.log("START");
                    var time;

                    // update links
                    link = link.data(force.links(), function(d) { return d.source.id + "-" + d.target.id; });
                    link.enter()
                        .append('line')
                        .attr('id', function(d){return 'edgeline_' + d.id;})
                        .style('opacity', 0)  // Make new edges at first invisible.
                        .style('stroke-width', function (d) {
                            return edgeScale(d.freq)+'px';
                            //return '2px'
                        })
                        .on('mouseup', function (d) {  // when clicking on an edge
                            if (d3.event.button == 0){
                                var index = selectedEdges.indexOf(d);
                                if(index == -1){  // The edge is not selected, so select it.
                                    selectedEdges.push(d);
                                    d3.select(this).style('stroke', selectionColor)
                                                   .style('opacity', .9);
                                    d3.select('#edgelabel_' + d.id)
                                                .style('fill', selectionColor)
                                                .attr('font-weight', 'bold');
                                    selectedNodes.push(d.source,d.target);
                                }
                                else{  // The edge is already selected, so unselect it.
                                    selectedEdges.splice(index, 1);  // Remove the edge from the list.
                                    d3.select(this).style("stroke", function (d) {
                                        if (d.uiLevel > 1) {return toolShareService.priorityToColor[d.uiLevel]} else {return neutralEdgeColor}})
                                        .style('opacity', .8);
                                    d3.select('#edgelabel_' + d.id)
                                                .style('fill', '#000000')
                                                .attr('font-weight', 'normal');
                                    selectedNodes.splice(selectedNodes.indexOf(d.source),1);
                                    selectedNodes.splice(selectedNodes.indexOf(d.target),1);
                                }
                                enableOrDisableButtons();
                                fireEvent_updateDocs();
                            }
                        })
                        .on('contextmenu', function (d) {
                            d3.event.preventDefault();
                            $scope.showFullTooltip =  {active: true, id: d.id};
                            $scope.$apply();
                    });
                    // remove old links
                    link.exit().remove();

                    link.style("stroke", function (d) {
                        if (d.uiLevel > 1) {return toolShareService.priorityToColor[d.uiLevel]} else {return neutralEdgeColor}})
                        .classed("glow",function (d) {return d.uiLevel>1});

                    // add the new paths for the edge labels
                    edgepaths = edgepaths.data(force.links(), function(d) { return d.source.id + "-" + d.target.id; });
                    edgepaths.enter()
                                .append('path')
                                .attr('class', 'edgepath')
                                .attr('id', function(d, i){
                                    return 'edgepath_' + d.id;
                                })
                                .attr('d', function(d){
                                    return 'M ' + d.source.x + ' ' + d.source.y + ' L ' + d.target.x + ' ' + d.target.y;
                                })
                                .style('pointer-events', 'none');
                    edgepaths.exit().remove();  // Remove old paths.
                    // add the new edge labels
                    edgelabels = edgelabels.data(force.links(), function(d) { return d.source.id + "-" + d.target.id; });
                    /*edgelabels.enter()
                                .append('text')
                                .attr('id', function(d, i){
                                    return 'edgelabel_' + d.id;
                                })
                                .attr('class', 'edgelabel')
                                .attr('dx', 0)
                                .attr('dy', 15)
                                .attr('font-size', 11)
                                .attr('fill', '#000000')
                                .style('pointer-events', 'none')
                                .append('textPath')
                                .attr('xlink:href',function(d, i){
                                    return '#edgepath_' + d.id;
                                })
                                .style('text-anchor', 'middle')
                                .attr('startOffset', '50%')
                                .text(function(d,i){ return d.freq; })
                                .style('text-shadow', '-1px -1px 3px #FFFFFF, 1px -1px 3px #FFFFFF, -1px 1px 3px #FFFFFF, 1px 1px 3px #FFFFFF');*/
                    edgelabels.exit().remove();  // Remove old edge labels.

                    // update nodes
                    node = node.data(force.nodes(), function(d) { return d.id;});
                    var newNodes = node.enter()
                                        .append('g')
                                        .attr('id', function(d, i){return 'node_' + d.id})
                                        .attr('class', 'node')
                                        .call(force.drag);

                    newNodes
                        .append('path')
                        .attr('id', function(d){return 'nodeborder_' + d.id;})
                        .attr('d', d3.svg.arc());


                    newNodes.append('circle')
                            .attr('r', function (d) { return radius(d.freq) })
                            .style('fill', function (d) { return color(d.type); })
                            .style('opacity', 0)  // Make new nodes at first invisible.
                            .attr('id', function(d, i)
                            {
                            	return 'nodecircle_' + d.id;
                            })
                            .on('contextmenu', function (d) {
                                d3.event.preventDefault();
                                $scope.showFullTooltip =  {active: true, id: d.id};
                                $scope.$apply();
                            })
                            .on('mousedown', function () {  // make nodes clickable
                                // On mousedown store the current time in milliseconds
                                time = moment();
                            })
                            .on('mouseup', function (d) {
                                if (d3.event.button === 0){
                                    d3.select(this).classed("fixed", d.fixed = true);  // Fix nodes that where moved.
                                    // On mouseup check if it was a click or dragging a circle and in case of click invoke the callback
                                    var now = moment();
                                    if (time + 500 > now) {
                                        var index = selectedNodes.indexOf(d);
                                        var nodeValue = d.name;
                                        var categoryIndex = $scope.graphShared.getIndexOfCategory(d.type);
                                        if(index == -1){  // The node is not selected, so select it.
                                            selectNode(d);
                                            d3.select(this)
                                                .style('stroke-width', 5)
                                                .style('stroke', selectionColor);
                                            // Add the selected node name to the words that get highlighted
                                            // However, add it only once to make deletion of that entry easier
                                            if (highlightShareService.wordsToHighlight[categoryIndex].indexOf(nodeValue) < 0) {
                                                highlightShareService.wordsToHighlight[categoryIndex].push(d.name);
                                                highlightShareService.wasChanged = true;
                                            }
                                        }
                                        else{  // The node is already selected, so unselect it.
                                            unselectNode(d);  // Remove the node from the list.
                                            d3.select(this).style('stroke-width', 1.5)
                                                           .style('stroke', '#000000');
                                            // Remove the selected node name from the words that get highlighted
                                            var index = highlightShareService.wordsToHighlight[categoryIndex].indexOf(d.name);
                                            if (index > -1) {
                                                highlightShareService.wordsToHighlight[categoryIndex].splice(index, 1);
                                                highlightShareService.wasChanged = true;
                                            }
                                        }
                                        enableOrDisableButtons();
                                    }
                                }
                            });

                    // set the text so that it fits inside the nodes
                    newNodes.append('text')
                            .attr('dy', '.35em')
                            .attr('text-anchor', 'middle')
                            .attr('id', function(d, i){
                               	return 'nodetext_' + d.id;
                            })
                            .text(function (d){
                                var r = radius(d.docCount);

                                if(!(r/3 >= d.name.length - 1))
                                {
                                    //console.log(d3.select(this).style('font-size').split("px")[0])
                                    d3.select(this).attr('y', (-r-d3.select(this).style('font-size').split("px")[0]/2) + 'px')
                                }

                                //if(r/3 >= d.name.length - 1)  // If the text fits inside the node.
                                return d.name;
                                //else  // If the text doesn't fit inside the node the 3 characters "..." are added at the end.
                                //    return d.name.substring(0, r/3 - 3) + "...";
                            })
                            .style('font-size', function(d){
                                var r = radius(d.docCount);
                                var textLength;
                                var size;
                                if(r/3 >= d.name.length - 1){  // If the text fits inside the node.
                                    textLength = d.name.length;
                                    // If the text contains less than 3 characters: act as if
                                    // there where 3 characters so that the font doesn't become to big.
                                    if(textLength < 3)
                                        textLength = 3;
                                    size = r/3;
                                }
                                else{  // If the text doesn't fit inside the node the 3 characters "..." are added at the end.
                                    textLength = d.name.substring(0, r/3 - 3).length;
                                    size = r/3 - 3;
                                }
                                size *= 7 / textLength;  // This seems to work to get a good text size.
                                size = Math.max(10, Math.round(size));  // Min. text size = 10px
                                return size+'px';
                            });

                    node.select('circle').style('stroke',function (d) { //console.log(d.name + " " + d.isFocusNode);
                        if (d.isFocusNode) {return focusColor} else {return neutralNodeColor}});


                // add buttons to the nodes
                    var buttonlist = newNodes.append('foreignObject')
                        .attr('id', function(d){return 'nodebuttons_' + d.id})
                        .attr('width', '16')
                        .attr('height', '16')
                        .attr('x', -8)
                        .attr('y', function(d){
                            return radius(d.freq) - 1;
                        })
                        .append('xhtml:body')
                        .style('padding', '0')
                        .style('margin-top', '0px');


                    buttonlist
                        .append('button')
                        .attr('type', 'button')
                        .attr('class', 'btn btn-default btn-block btn-xs')
                        .style('width', '100%')
                        .style('height', '100%')
                        .style('padding', '0px 0px 0px 0px')
                        .on('click', function(d)//Wenn der Plus-Button gedrückt wird
                                                {
                                                    console.log("button clicked");
                                                    getGuidanceNodes(d.id,true);
                                                    //$scope.observer.addItem({type: 'entity', data: {id: d.id, name: d.name, type: d.type}});
                                                })

                        .append('i')
                        .attr('class',"material-icons")
                        .style('font-size','14px')//20
                        .text('adjust');

                /* zweiter Button zum expandieren, brauchen wir nicht wirklich
                buttonlist
                    .append('button')
                    .attr('type', 'button')
                    .attr('class', 'btn btn-default btn-block btn-xs')
                    .style('width', '100%')
                    .style('height', '100%')
                    .style('padding', '0px 0px 0px 0px')
                    .on('click', function(d)//Wenn der Plus-Button gedrückt wird
                    {
                        console.log("button clicked");
                        //getGuidanceNodes(d.id,true);
                    })
                    .append('i')
                    .attr('class',"material-icons")
                    .style('font-size','16px')//20
                    //.attr('fill','#555')
                    .text('add');
*/
                    //console.log("removing nodes:");
                    //console.log(node.exit().data());

                    node.exit().remove();

                    addTooltip(node, link);  // add the tooltips to the nodes and links
                    enableOrDisableButtons();

                    $scope.isViewLoading = false;

                    // use the force L...ayout!
                    force.start();

                    updateWordsForUnderlining();

                    // Set the opacity of all nodes and edges to the normal
                    // values (because the new ones are still invisible).
                    // values (because the new ones are still invisible).
                    // The tansition starts after 1.5 seconds and takes 1.5 seconds.
                    setTimeout(function(){
                        d3.selectAll('circle').each(function(d){
                            d3.select(this).transition().duration(1500).style('opacity', 1);
                        });
                        link.each(function(d){
                            d3.select(this).transition().duration(1500).style('opacity', .8);
                        });
                    }, 1500);

					//if we have a callback, we execute it
					if(callback != null)
					{
						callback();
					}
            }

            /**
             * This function adds tooltips to the nodes and links.
             * @param node The nodes
             * @param link The links
             */
            function addTooltip(node, link){

                function hideTooltip(name){
                    if (!$scope.showFullTooltip.active){
                        var tooltip = d3.select(name);//.style("left", (d3.event.pageX - 10) + "px").style("top", (d3.event.pageY - 100) + "px");
                        tooltip.transition()
                            .duration(300)
                            .style("opacity", 0);
                        tooltip.style("pointer-events","none");
                    }
                }

                node.on("mouseover", function(d) {
                    if ($scope.showFullTooltip.id !== d.id){
                        $scope.showFullTooltip =  {active: false};
                        hideTooltip("#edge-tooltip");

                        $scope.sNode = d;
                        if ($scope.contextCache[d.id] === undefined) {
                            // console.log("cache undefined for "+d.name);
                            $scope.contextCache[d.id] = {isComputing: true};
                            getAdditionalEdges(d.id);
                        } else if ($scope.contextCache[d.id].isComputing !== true && ($scope.contextCache[d.id].data === undefined )) {
                            $scope.contextCache[d.id].isComputing = true;
                            getAdditionalEdges(d.id);
                        } else if (!$scope.contextCache[d.id].isComputing){
                            drawBarGraph($scope.contextCache[d.id].data.bargraph)
                        }
                        $scope.$apply();
                        var tooltip = d3.select("#node-tooltip").style("left", (d3.event.pageX + 10) + "px").style("top", d3.event.pageY - 100 + "px");
                        /*tooltip.transition()
                         .duration(500)
                         .style("opacity", 0);*/
                        tooltip.transition()
                            .duration(300)
                            .style("opacity", 1);
                        tooltip.style("pointer-events","auto");
                    }
                });

                node.on("mouseout", function(d){
                    hideTooltip("#node-tooltip")
                });

                link.on("mouseover", function(d){
                    if ($scope.showFullTooltip.id !== d.id) {
                        $scope.showFullTooltip =  {active: false};
                        hideTooltip("#node-tooltip");

                        var tooltip = d3.select("#edge-tooltip");

                        $scope.sEdge = d;
                        tooltip.transition()
                            .duration(300)
                            .style("opacity", 1);
                        tooltip.style("pointer-events","auto");
                        tooltip.style("left", (d3.event.pageX - 75) + "px")
                            .style("top", (d3.event.pageY + 10) + "px");

                        var state = toolShareService.UIitems[$scope.categories[d.source.type].number][$scope.categories[d.target.type].number];
                        $('#eo' + state).parents('.btn').button('toggle');
                        $scope.$apply();
                    }

                });
                link.on("mouseout", function(d){
                    hideTooltip("#edge-tooltip");
                });
            }


            /**
             * This function updates the array of the words that will be
             * underlined in documents. The word are underlined in the color
             * the corresponding entity node has.
             */
            function updateWordsForUnderlining(){
                // Remove the old content of the arrays.
                for (var i = 0; i < highlightShareService.wordsToHighlight.length; i++) {
                    highlightShareService.wordsToUnderline[i] = [];
                }
                // Add all node names.
                nodes.forEach(function(node){
                    var categoryIndex = $scope.graphShared.getIndexOfCategory(node.type);
                    // Add every word only once (some nodes have the same name).
                    if(highlightShareService.wordsToUnderline[categoryIndex].indexOf(node.name) == -1){
                        highlightShareService.wordsToUnderline[categoryIndex].push(node.name);
                    }
                });
                highlightShareService.wasChanged = true;
            }


            /**
             * This function disables and enables the buttons for editing
             * the graph depending on the amount of selected nodes/edges.
             */
            function enableOrDisableButtons(){
                // Nothing is selected:
                if(selectedNodes.length == 0 && selectedEdges.length == 0){
                    $('#EgoNetworkButton').attr('disabled', 'disabled');
                    $('#EditButton').attr('disabled', 'disabled');
                    $('#MergeButton').attr('disabled', 'disabled');
                    $('#AnnotateButton').attr('disabled', 'disabled');
                    $('#DeleteButton').attr('disabled', 'disabled');
                    $('#HideButton').attr('disabled', 'disabled');
                }
                // Anything is selected:
                else{
                    $('#DeleteButton').removeAttr("disabled");
                    $('#HideButton').removeAttr("disabled");
                    // Either one node or one edge is selected:
                    if(selectedNodes.length == 1 && selectedEdges.length == 0
                            || (selectedNodes.length == 0 && selectedEdges.length == 1)){
                        $('#MergeButton').attr('disabled', 'disabled');
                        $('#AnnotateButton').removeAttr("disabled");
                        $('#EditButton').removeAttr("disabled");
                        // One node is selected:
                        if(selectedNodes.length == 1)
                            $('#EgoNetworkButton').removeAttr("disabled");
                        // One edge is selected:
                        else
                            $('#EgoNetworkButton').attr('disabled', 'disabled');
                    }
                    // Multiple Nodes and/or edges are selected:
                    else{
                        $('#EgoNetworkButton').attr('disabled', 'disabled');
                        $('#AnnotateButton').attr('disabled', 'disabled');
                        $('#EditButton').attr('disabled', 'disabled');
                        // Only nodes are selected:
                        if(selectedEdges.length == 0)
                            $('#MergeButton').removeAttr("disabled");
                        // Only edges are selected:
                        else
                            $('#MergeButton').attr('disabled', 'disabled');
                    }
                }
            }

            $scope.loaded = reload;

            /**
             * Create an example graph; we use a trick (namely the $timeout even with 0ms)
             * to render it only after the DOM is fully loaded.
             * Also, initialize the legend popover
             */
            function reload() {
                createSVG();

                force = d3.layout
                    .force()
                    .nodes(nodes)
                    .links(edges)
                    .size([400, 400])
                    .charge(-400)
                    .chargeDistance(400)
                    .linkStrength(0.4)
                    .linkDistance(function(d) {
                        return radius(d.source.docCount)
                            + radius(d.target.docCount)
                            + 100;
                    })
                    .on("tick", function() {
                        link.attr('x1', function (d) {return d.source.x;})
                            .attr('y1', function (d) {return d.source.y;})
                            .attr('x2', function (d) {return d.target.x;})
                            .attr('y2', function (d) {return d.target.y;});
                        node.attr('transform', function (d)
                        {
                            return 'translate(' + d.x + ',' + d.y + ')';
                        });

                        // rotate the edge labels in a way the text isn't upside down
                        edgepaths.attr('d', function(d){
                            return 'M ' + d.source.x + ' ' + d.source.y + ' L ' + d.target.x + ' ' + d.target.y;
                        });
                        edgelabels.attr('transform', function(d, i){
                            if(d.target.x < d.source.x){
                                var x1 = this.__data__.source.x;
                                var x2 = this.__data__.target.x;
                                var y1 = this.__data__.source.y;
                                var y2 = this.__data__.target.y;
                                var x;
                                var y;
                                if(x1 > x2)
                                    x = x1 - (x1 - x2) / 2;
                                else
                                    x = x2 - (x2 - x1) / 2;
                                if(y1 > y2)
                                    y = y1 - (y1 - y2) / 2;
                                else
                                    y = y2 - (y2 - y1) / 2;
                                return 'rotate(180 ' + x + ' ' + y + ')';
                            }
                            else
                                return 'rotate(0)';
                        });
                    })
                    /*.on("start", function()
                    {
                        node.attr('transform')
                    })*/

                    force.start();

                    console.log("reload force layout");
                //getGraph();
                // Extend the popover to a callback when loading is done
                var tmp = $.fn.popover.Constructor.prototype.show;
                $.fn.popover.Constructor.prototype.show = function () {
                    tmp.call(this);
                    if (this.options.callback) {
                        this.options.callback();
                    }
                }
                // On click of the 'Show legend' button show the legend
                $('#btn-show-legend').popover({
                    callback: function() {
                        setLegendPopoverHeight();
                    },
                    placement: 'bottom',
                    html: true,
                    content: function() {
                        return $('#legend-content').html();
                    }
                }).addClass('popover-legend');
            }

            /**
             * Hide the currently selected nodes/edges from the graph. The data
             * will still be in the database.
             */
            function hideSelected(){
                // Remove selected nodes.
                selectedNodes.forEach(function(d){
                    var editItem = {
                        type: 'hide',
                        data: {
                            id: d.id,
                            name: d.name + ': ' + d.type
                        }
                    };
                    $scope.observer.addItem(editItem);
                    // Remove the selected node name from the words that get highlighted
                    var categoryIndex = $scope.graphShared.getIndexOfCategory(d.type);
                    var index = highlightShareService.wordsToHighlight[categoryIndex].indexOf(d.name);
                    if(index >= 0){
                        highlightShareService.wordsToHighlight[categoryIndex].splice(index, 1);
                    }
                    nodes.splice(nodes.indexOf(d), 1);  // Remove the node.
                });
                updateWordsForUnderlining();
                highlightShareService.wasChanged = true;

                // Remove selected edges.
                selectedEdges.forEach(function(d){
                    edges.splice(edges.indexOf(d), 1);
                });
                // If the node of an edge was removed: remove the edge.
                for(var i=0; i<edges.length; i++){
                    if(selectedNodes.indexOf(edges[i].source) != -1
                        || selectedNodes.indexOf(edges[i].target) != -1){
                        edges.splice(i, 1);
                        i--;
                    }
                }
                selectedNodes = new Array();
                selectedEdges = new Array();
                enableOrDisableButtons();
                start();
            }

            /**
             * Hide the currently selected nodes/edges from the graph. The data
             * will still be in the database.
             */
            function remove(nodes)
            {

                //playRoutes.controllers.NetworkController.deleteEntityById(selectedNodes[0].id).get().then(function(result){alert(result)});

				nodes.forEach(
					function(n,i,a){
                        var editItem = {
                            type: 'delete',
                            data: {
                                id: n.id,
                                name: n.name + ': ' + n.type
                            }
                        };
                        $scope.observer.addItem(editItem);
						d3.select("#node_" + n.id).remove();
						edges.forEach(
							function(v,i,a)
							{
								if(v.source == n || v.target == n)
								{
									d3.select("#edgepath_" + v.id).remove();
									d3.select("#edgeline_" + v.id).remove();
									d3.select("#edgelabel_" + v.id).remove();
								}
							}
						);
					}
				);


                //alert("TODO: delete selected");
            }

            /**
             * merges the currently selected nodes into one
             */
            function merge(focalNode, nodes)
            {
                focalNode = getNodeById(Number(focalNode));

				var entityids = [];
				var freqsum = 0

				//save the ids and delete the merged nodes
				nodes.forEach(
					function(v,i,a)
					{
						if(v.id != focalNode.id)
						{
						    //console.log(v);
						    //console.log(focalNode);
							entityids.push(v.id);
							d3.select("#node_" + v.id).remove();
							freqsum = freqsum + v.freq;
						}
					}
				);

				//extend the focal node with the sum of the frequencies of
				focalNode.freq = focalNode.freq + freqsum;
				d3.select("#nodecircle_" + focalNode.id).attr('r', function(d){return radius(d.docCount);});

                var edgesToDelete = [];

				edges.forEach(
					function(v,i,a)
					{
					    /*if(entityids.indexOf(v.source.id) != -1 || entityids.indexOf(v.target.id) != -1)
					    {
					        console.log(v)
					    }*/
					    /*if((v.source.name == "USG" || v.target.name == "USG") && (v.source.name == "Commission" || v.target.name == "Commission"))
					    {
					        console.log(v)
					    }*/

						//if we have on both sides a node we want to delete or the focal node
						if(
							(entityids.indexOf(v.source.id) != -1 && entityids.indexOf(v.target.id) != -1) ||
							(entityids.indexOf(v.target.id) != -1 && v.source.id == focalNode.id) ||
							(entityids.indexOf(v.source.id) != -1 && v.target.id == focalNode.id)
						)
						{
							//we remove the edge
							d3.select("#edgepath_" + v.id).remove();
                           	d3.select("#edgeline_" + v.id).remove();
                            d3.select("#edgelabel_" + v.id).remove();
                            //console.log("both sides")
                            /*if(v.source.name == "Sipdis" || v.target.name == "Sipdis")
                            {
                                console.log(v)
                            }*/

                            edgesToDelete.push(v);
                            //edges.splice(i, 1)
						}
						else if(entityids.indexOf(v.source.id) != -1 || entityids.indexOf(v.target.id) != -1)
						{
							//console.log(v)
							//get an existing edge between the focalNode and the node not to delete
							var existingEdge = edges.filter(
								function(e,i,a)
								{
									if(entityids.indexOf(v.source.id) != -1 || entityids.indexOf(v.target.id) != -1)
									{
										return (e.source.id == v.target.id && e.target.id == focalNode.id) ||
										        (e.source.id == v.source.id && e.target.id == focalNode.id) ||
												(e.source.id == focalNode.id && e.target.id == v.target.id) ||
                                                (e.source.id == focalNode.id && e.target.id == v.source.id)
									}
									else
									{
										/*return (e.source.id == v.source.id && e.target.id == focalNode.id) ||
                                        		(e.source.id == focalNode.id && e.target.id == v.source.id);*/
                                        return false;
									}
								}
							);
							//console.log(v)
							//console.log(existingEdge);

							//if we havent found a node
							if(existingEdge.length == 0)
							{
								//we attach the edge to the focalNode
								if(entityids.indexOf(v.source.id) != -1) v.source = focalNode;
								else v.target = focalNode;
							}
							else
							{
								var edge = existingEdge[0];
								edge.freq = edge.freq + v.freq;

								//d3.select("#edgeline_" + edge.id).style('stroke-width', function (d) {return edgeScale(d.freq)+'px';});
								//d3.select("#edgelabel_" + edge.id).select("textPath").text(function(d,i){return d.freq;});

								//we remove the edge
                                d3.select("#edgepath_" + v.id).remove();
                                d3.select("#edgeline_" + v.id).remove();
                                d3.select("#edgelabel_" + v.id).remove();

                                edgesToDelete.push(v);
                                //edges.splice(i, 1)
							}

						}
					}
				);

				edgesToDelete.forEach(function(e){edges.splice(edges.indexOf(e), 1)});

				unselectNodes()
				selectNode(focalNode);

				enableOrDisableButtons();
				/*playRoutes.controllers.NetworkController.mergeEntitiesById($scope.merge.node,entityids).get().then(
					function(result)
					{
						alert(result)
					}
				);*/
            }

            function sendMerged(focalNode, nodes)
            {

                focalNode = getNodeById(Number(focalNode));

                console.log(focalNode);
                console.log(nodes);

                var entityids = [];

                //save the ids and delete the merged nodes
                nodes.forEach(
                	function(v,i,a)
                	{
                		if(v.id != focalNode.id)
                		{
                			entityids.push(v.id);
                		}
                	}
                );

                playRoutes.controllers.NetworkController.mergeEntitiesById(focalNode.id,entityids).get().then(
                	function(result)
                	{
                	    //console.log(result)
                	    //console.log("merge returned")
                		if(result.result === false)
                		{
                		    alert("Error while merging Entities")
                		}
                	}
                );
            }

            /**
             * gives the user the option to edit the name
             * of selected edges
             *
             * @param node the node to modify
             * @param text the new name of the node
             */
            function editName(node, text)
            {
            	//if the name ist the same we return
            	if(node.name == text)
            	{
            		return;
            	}
                var editItem = {
                    type: 'edit',
                    data: {
                        id: node.id,
                        name: '<'+node.name+' -> '+ text+'>'
                    }
                };
                $scope.observer.addItem(editItem);

                var edit = text;
                node.name = edit;
                d3.select("#nodetext_" + node.id).text(function (d){
                	var r = radius(d.docCount);

                	if(!(r/3 >= d.name.length - 1))
                    {
                        console.log(d3.select(this).style('font-size').split("px")[0])
                        d3.select(this).attr('y', (-r-d3.select(this).style('font-size').split("px")[0]/2) + 'px')
                    }

                	//if(r/3 >= d.name.length - 1)  // If the text fits inside the node.
                		return d.name;
                	/*else  // If the text doesn't fit inside the node the 3 characters "..." are added at the end.
                		return d.name.substring(0, r/3 - 3) + "...";*/
                });
            }

            /**
             *  send to the server a message, that the name of the node
             *  was edited
             *
             *  @param node
             *      the node with the edited name
             */
            function sendEditedName(node)
            {
                playRoutes.controllers.NetworkController.changeEntityNameById(node.id, node.name).get().then(
                    function(result)
                    {
                        if(result.result == false)
                        {
                            alert("Error while editing Entity")
                        }
                    }
                );
            }

            /**
             *	edit the type of the node
             *
             *	@param node the type of the node
             *	@param type to which the type of the node should be changed
             */
            function editType(node, type)
            {

            	if(node.type == type)
            	{
            		return;
            	}
                var editItem = {
                    type: 'edit',
                    data: {
                        id: node.id,
                        name: '<'+node.type+' -> '+type+'>'
                    }
                };
                $scope.observer.addItem(editItem);

            	node.type = type;
            	d3.select("#nodecircle_" + node.id)
            		.style("fill", function(d){return color(d.type)});
            }

            /**
             *  send a message to the server that the name of the entity
             *  was edited
             *
             *  @param node
             *      the node
             */
            function sendEditedType(node)
            {
                playRoutes.controllers.NetworkController.changeEntityTypeById(node.id, node.type).get().then(
                    function(result)
                    {
                        if(result.result == false)
                        {
                            alert("Error while editing Entity")
                        }
                    }
                )
            }

            function undoStep() {
                $scope.isViewLoading = true;
                playRoutes.controllers.NetworkController.undo(sessionid).get().then(
                    function (response) {
                        console.log(response);
                        computeNodesAndEdgesJSON(response.data.focusId,response);
                        toolShareService.sliderEdgesPerNode = function () {return response.data.epn};
                        toolShareService.updateToolDisplay(response.data.edgeAmount,response.data.epn);
                        toolShareService.UIitems = response.data.uiMatrix.split(";").map(
                            function(str) {return str.split(",").map(
                                function(intStr) {return parseInt(intStr)})});
                        console.log(toolShareService.UIitems);
                        start();
                        var hist = ObserverService.getHistory();
                        var state = "s-start";
                        var stopLoop = false;
                        var undoList = [];
                        for(var i = hist.length-1; i>=0; i--){
                            switch (state) {
                                case "s-start":
                                    if (hist[i].type == "guidance" && hist[i].active) {
                                        hist[i].active = false;
                                        hist[i].undone = true;
                                        state = "s1";
                                        undoList.push(i);
                                    } else if (hist[i].type == "addEdges") {
                                        hist[i].active = false;
                                        // undoList.push(i);
                                    }
                                    break;
                                case "s1":
                                    console.log(hist[i].type);
                                    if (hist[i].type == "guidance" && hist[i].active){
                                        state = "s3"
                                    }
                                    else if(["entity","metadata","fulltext","time"].includes(hist[i].type)) {
                                        if (hist[i].action != "removed") {
                                            hist[i].active = false;
                                            undoList.push(i);
                                        } else {
                                            hist[hist[i].reverts].active = true;
                                            undoList.push(hist[i].reverts);
                                        }
                                        state = "s1";
                                    }
                                    /*if (hist[i].type == "addEdges") {
                                     hist[i].active = true;
                                     } else*/
                                    else if (hist[i].type == "guidance" && !hist[i].undone){
                                        hist[i].active = true;
                                        state = "s3";
                                        undoList.push(i);
                                    }
                                    break;
                                case "s3":
                                    stopLoop = true;
                                    break;
                            }
                            if (stopLoop){
                                break;
                            }
                        }
                        ObserverService.addItem({type: "undo", active: false, data:{name: "undo", list: undoList}});
                        $scope.contextCache = {};
                        $scope.isViewLoading = false;
                    })
            }

            function redoStep() {
                $scope.isViewLoading = true;
                playRoutes.controllers.NetworkController.redo(sessionid).get().then(
                    function (response) {
                        console.log(response);
                        computeNodesAndEdgesJSON(response.data.focusId,response);
                        toolShareService.sliderEdgesPerNode = function () {return response.data.epn};
                        toolShareService.updateToolDisplay(response.data.edgeAmount,response.data.epn);
                        toolShareService.UIitems = response.data.uiMatrix.split(";").map(
                            function(str) {return str.split(",").map(
                                function(intStr) {return parseInt(intStr)})});
                        console.log(toolShareService.UIitems);
                        start();
                        var hist = ObserverService.getHistory();
                        for(var i = hist.length-1; i>=0; i--){
                            if (hist[i].type === "undo" && !hist[i].redone) {
                                ObserverService.addItem({
                                    type: "redo",
                                    active: false,
                                    data: {name: "redo", correspondingUndo: i}
                                });
                                hist[i].redone = true;
                                var undoObj = hist[i].data;
                                break;
                            }
                        }
                        for (var i = 0; i<undoObj.list.length; i++){
                            var item = hist[undoObj.list[i]];
                            if (item.type === "guidance"){
                                item.active = !item.active;
                                delete item.undone;
                            } else if (item.type === "addEdges"){
                                item.active = true;
                            } else if (["entity","metadata","fulltext","time"].includes(item.type)) {
                                item.active = !item.active;
                            }
                        }

                        //ObserverService.addItem({type: "undo", active: false, data:{name: "undo", list: undoList}});
                        $scope.contextCache = {};
                        $scope.isViewLoading = false;
                    })
            }

            function computeNodesAndEdgesJSON(focusNodeId,response) {
                //to prevent invisible selections
                unselectNodes();
                unselectEdges();

                toolShareService.undoAvailable = function () {return response.data.undoAvailable};
                toolShareService.redoAvailable = function () {return response.data.redoAvailable};

                //Object, welches alle alten KnotenId als Schlüssel enthält mit xy-Koordinaten als Wert (=HashMap)
                var oldNodes = nodes.reduce(function(o, node, index) {
                    o[node.id] = {x: node.x, y: node.y};
                    return o;
                }, {});
                //console.log(oldNodes);

                //delete all nodes and edges
                nodes = [];
                edges = [];

                response.data.nodes.forEach(
                    function(v)
                    {
                        /*var enode = tmpnodes.find(function(node){return node.id === v.id;});
                         if(enode != undefined)
                         {
                         enode.docCount = v.docCount;
                         nodes.push(enode);
                         return;
                         }*/

                        var fixedPos = oldNodes.hasOwnProperty(v.id);//Soll der Knoten die Position halten?
                        nodes.push({
                            id: v.id,
                            isFocusNode: v.id==focusNodeId,
                            name: v.name,
                            freq: v.docOcc,
                            type: v.type,
                            docCount: -1,
                            size: 2,
                            fixed: fixedPos,
                            x: fixedPos ? oldNodes[v.id].x : undefined,
                            y: fixedPos ? oldNodes[v.id].y : undefined
                        });

                        // force.links(edges);
                        // calculateNewForceSize();
                        // start();
                    });
                //console.log(nodes);
                response.data.links.forEach(
                    function(v)
                    {
                        var sourceNode = nodes.find(function(node){return v.sourceNode == node.id});
                        var targetNode = nodes.find(function(node){return v.targetNode == node.id});
                        if(sourceNode == undefined || targetNode == undefined)
                        {
                            return;
                        }
                        if(sourceNode.id > targetNode.id){//sourceNodes haben immer die kleinere Id. Das ist notwendig, damit Kanten korrekt neugezeichnet werden
                            var tempNode = sourceNode;
                            sourceNode = targetNode;
                            targetNode = tempNode
                        }
                        edges.push({id: sourceNode.id+"-"+targetNode.id, source: sourceNode, target: targetNode, freq: v.docOcc, uiLevel: v.uiLevel});
                    });
                //reload();

                force.nodes(nodes);
                force.links(edges);

                guidanceStepCounter++;
                oldFocusNodeId= focusNodeId;
            }

            function getGuidanceNodes(focusNodeId,useOldEdges){
                $scope.isViewLoading = true;
                var entities = [];
                angular.forEach($scope.entityFilters, function(item) {
                    entities.push(item.data.id);
                });
                var facets = $scope.observer.getFacets();
                var fulltext = [];
                angular.forEach($scope.fulltextFilters, function(item) {
                    fulltext.push(item.data.name);
                });

                var uiStr= "";
                for (var i=0;i<toolShareService.UIitems.length;i++){
                    uiStr+=toolShareService.UIitems[i].toString()+';';
                }

                playRoutes.controllers.NetworkController.getGuidanceNodes(fulltext, facets, entities, $scope.observer.getTimeRange(), focusNodeId, toolShareService.sliderEdgeAmount(), toolShareService.sliderEdgesPerNode(), uiStr, useOldEdges, sessionid).get().then(function(response) {

                    computeNodesAndEdgesJSON(focusNodeId,response);
                    //calculateNewForceSize();

                    if (!useOldEdges){
                        var history = $scope.observer.getHistory();
                        history.forEach(function (item) {
                            if (item.type ===  "guidance") {
                                item.active = false;
                            }
                        });
                    }
                    $scope.observer.addItem({
                        type: "guidance",
                        data: nodes.find(function(node){return focusNodeId == node.id})});
                    start();
                    $scope.isViewLoading = false;
                    $scope.contextCache = {};
                });
            };

            $scope.setFocusNode = function(nodeId) {
                getGuidanceNodes(nodeId, true);
                var tooltip = d3.select("#node-tooltip");
                tooltip.transition()
                    .duration(300)
                    .style("opacity",0);
                tooltip.style("pointer-events","none");

                $scope.showFullTooltip =  {active: false};
            };

            $scope.expandNode = function(nodeId) {
                $scope.observer.addItem({
                    type: "addEdges",
                    data: nodes.find(function(node){return nodeId == node.id})});
                nodes = nodes.concat($scope.contextCache[nodeId].data.expand.nodes);
                edges = edges.concat($scope.contextCache[nodeId].data.expand.edges);

                delete $scope.contextCache[nodeId].data;
                force.nodes(nodes);
                force.links(edges);
                start();
                var tooltip = d3.select("#node-tooltip").transition()
                    .duration(300)
                    .style("opacity",0);
                tooltip.style("pointer-events","none");
                $scope.showFullTooltip = {active: false};
            };

            function getAdditionalEdges(nodeId) {
                playRoutes.controllers.NetworkController.getContext(nodeId,toolShareService.sliderEdgesPerNode(),sessionid).get().then(function(response) {
                    var nNodes = [];
                    response.data.expand.nodes.forEach(
                        function (v) {
                            nNodes.push({
                                id: v.id,
                                isFocusNode: false,
                                name: v.name,
                                freq: v.docOcc,
                                type: v.type,
                                docCount: -1,
                                size: 2,
                            });
                        });
                    console.log(nNodes);
                    var allNodes = nodes.concat(nNodes);
                    var nEdges = [];
                    var addedCon = [];
                    response.data.expand.links.forEach(
                        function (v) {
                            var sourceNode = allNodes.find(function (node) {
                                return v.sourceNode == node.id
                            });
                            var targetNode = allNodes.find(function (node) {
                                return v.targetNode == node.id
                            });
                            addedCon.push(targetNode);
                            if (sourceNode == undefined || targetNode == undefined) {
                                return;
                            }
                            if (sourceNode.id > targetNode.id) {//sourceNodes haben immer die kleinere Id. Das ist notwendig, damit Kanten korrekt neugezeichnet werden
                                var tempNode = sourceNode;
                                sourceNode = targetNode;
                                targetNode = tempNode
                            }
                            //console.log(sourceNode.name+"-"+targetNode.name);
                            nEdges.push({
                                id: sourceNode.id + "-" + targetNode.id,
                                source: sourceNode,
                                target: targetNode,
                                freq: v.docOcc,
                                uiLevel: v.uiLevel
                            });
                        });
                    $scope.contextCache[nodeId] = {
                        data: {
                            guidance: response.data.guidance,
                            bargraph: response.data.bargraph,
                            expand: {nodes: nNodes, edges: nEdges, tooltipInfo: addedCon}
                        },
                        isComputing: false};
                    console.log($scope.contextCache);
                    setTimeout(function () {drawBarGraph(response.data.bargraph)},1);
                });
            }

            function drawBarGraph(data) {
                var bargraphscale = d3.scale.linear()
                    .domain([0, d3.max(data.map(function (elem){return elem.count}))])
                    .range([0, 170]);
                var formatter = d3.format(',');

                var bars = d3.select("#bargraph")
                    .selectAll("div")
                    .data(data);
                var newbars = bars.enter().append("div");
                newbars.append("i").classed("material-icons", true);
                newbars.append("span");

                bars.select("i").text(function(d){return $scope.categories[d.name].img});
                bars.select("span")
                    .style("width", function(d) { return bargraphscale(d.count) + "px"; })
                    .style("background-color", function(d) {return color(d.name)})
                    .text(function(d) { return formatter(d.count); });
            }


        }
    ]);


});
