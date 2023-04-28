/**
 *
 * (c) Copyright Ascensio System SIA 2023
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

let cache = {};

// write the key value and its creation time to the cache
exports.put = function put(key, value) {
  cache[key] = { value, time: new Date().getTime() };
};

// check if the given key is in the cache
exports.containsKey = function containsKey(key) {
  if (typeof cache[key] === 'undefined') {
    return false;
  }

  const secondsCache = 30;

  // get the creation time of the given key and add 30 seconds to it
  const t1 = new Date(cache[key].time + (1000 * secondsCache));
  const t2 = new Date(); // get the current time
  if (t1 < t2) { // if the current time is greater
    delete cache[key]; // delete the given key from the cache
    return false;
  }

  return true;
};

// get the given key from the cache
exports.get = function get(key) {
  return cache[key];
};

// delete the given key from the cache
exports.delete = function deleteKey(key) {
  delete cache[key];
};

// clear the cache
exports.clear = function clear() {
  cache = {};
};
