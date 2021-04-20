/**
 *
 * (c) Copyright Ascensio System SIA 2020
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

var cache = {};

exports.put = function (key, value) {
    cache[key] = { value:value, time: new Date().getTime()};
}

exports.containsKey = function (key) {
    if (typeof cache[key] == "undefined"){
        return false;
    }

    var secondsCache = 30;

    var t1 = new Date(cache[key].time + (1000 * secondsCache));
    var t2 = new Date();
    if (t1 < t2 ){
        delete cache[key];
        return false;
    }

    return true;
}

exports.get = function (key) {
    return cache[key];
}

exports.delete = function (key) {
    delete cache[key];
}

exports.clear = function () {
    cache = {};
}