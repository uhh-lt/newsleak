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
});