/*
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

define(['angular'], function (angular) {
    "use strict";

    var mod = angular.module('myApp.util', []);

    mod.factory('util', function () {

        var utilService = {};

        utilService.shortenUrl = function (url) {
            var match = url.match(/^(?:ftp|https?):\/\/(?:[^@:\/]*@)?([^:\/]+)/);

            if (typeof match === "undefined" || match === null) {
                return url;
            }
            else {
                if (url.length > match[0].length) {
                    return match[0] + '/...';
                }
                else {
                    return match[0];
                }
            }
        };

        /**
         * This function returns the text, but with all words in highlights
         * array highlighted and all words in the underlining array underlined.
         * @param  text         a text
         * @param  highlights   an array of texts (words) to highlight
         * @param  underlining  an array of texts (words) to underline
         * @param  color        a color in which the given words will be
         *                      highlighted / underlined;
         *                      the passed parameter should be of a type
         *                      that the CSS property color can interpret
         */
        utilService.highlight = function (text, highlights, underlining, color) {
            function quoteRe(str) {
                return (str + '').replace(/([.?*+^$[\]\\(){}|-])/g, "\\$1");
            };

            for (var j = 0, len = underlining.length; j < len; j++) {
                var hl = underlining[j];
                var tokens = hl.split(' ');
                while (tokens.length > 0) {
                    var re = new RegExp('(?:^|\\b)(' + quoteRe(tokens.join(' ')) + ')(?=\\b|$)', 'gi');
                    if (text.match(re)) {
                        if(highlights.indexOf(hl) == -1)  // only underlining, no highlighting
                            text = text.replace(
                                re,
                                '<span class="highlight-general" style="text-decoration: none; border-bottom: 3px solid ' + color + '">$1</span>'
                            );
                        else  // highlight the text, don't underline it
                            text = text.replace(
                                re,
                                '<span class="highlight-general" style="background-color: ' + color + '">$1</span>'
                            );
                        break;
                    } else {
                        tokens.shift();
                    }
                }
            }
            return text;
        }

        /**
         * Takes a number and inserts '.' as separator.
         * Taken from: http://www.mredkj.com/javascript/numberFormat.html.
         *
         * @param n The number to format, e. g. 123456.
         * @return {string} Returns the formatted number, e. g. 123.456.
         */
        utilService.formatNumber = function (n) {
            n += '';
            var x = n.split('.');
            var x1 = x[0];
            var x2 = x.length > 1 ? '.' + x[1] : '';
            var rgx = /(\d+)(\d{3})/;
            while (rgx.test(x1)) {
                x1 = x1.replace(rgx, '$1' + '.' + '$2');
            }
            return x1 + x2;
        }

        return utilService;
    });

        mod.directive('flexResizer', function() {

            return {
                scope: {
                    resizeDirection: '@'
                },
                link: function(scope, element, attr) {
                    var jElement = angular.element(element);
                    var prev = $(jElement).prev();
                    var next = $(jElement).next();
                    console.log(prev);
                    console.log(next);
                    var parent = $(jElement).parent();
                    var sizeProp = 'height';
                    var posProp = 'pageY';
                    var prevSize = $(prev).height();
                    var nextSize = $(next).height();
                    var parentSize =  $(parent).height();
                    if (scope.resizeDirection == 'horizontal') {
                        prevSize = $(prev).width();
                        nextSize = $(next).width();
                        var parentSize =  $(parent).width();
                        sizeProp = 'width';
                        posProp = 'pageX';
                    }
                    var lastPos = 0;

                    var dragging = function(e) {

                        var pos = e[posProp];

                        var d = pos - lastPos;
                        prevSize += d;
                        nextSize -= d;

                        if (prevSize < 0) {
                            nextSize += prevSize;
                            pos -= prevSize;
                            prevSize = 0;
                        }
                        if (nextSize < 0) {
                            prevSize += nextSize;
                            pos += nextSize;
                            nextSize = 0;
                        }
                        console.log(prevSize);
                        console.log(parentSize);
                        var prevGrowNew =  (prevSize / parentSize)*100;
                        var nextGrowNew =  (nextSize / parentSize)*100;
                        console.log(prevGrowNew);
                        $(prev).css('flex-basis', prevGrowNew + '%');
                        $(next).css('flex-basis', nextGrowNew + '%');

                        lastPos = pos;

                    };
                    var dragEnd = function(e) {
                        console.log("end");
                        $(window).off("mousemove");
                    };
                    var dragStart = function(e, direction) {
                        console.log("start");

                        prevSize = $(prev).height();
                        nextSize = $(next).height();
                        parentSize = $(parent).height();
                        if (scope.resizeDirection == 'horizontal') {
                            prevSize = $(prev).width();
                            nextSize = $(next).width();
                            parentSize = $(parent).width();
                        }
                        lastPos = e[posProp];
                        $(window).mousemove(dragging);
                        $(window).mouseup(dragEnd);
                    };

                    $(jElement).mousedown(dragStart);


                }
            };
        });
});