/*
 * Copyright © 2016-2025 The Thingsboard Authors
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
 */
(function (compodoc) {
    function LunrSearchEngine() {
        this.index = undefined;
        this.store = {};
        this.name = 'LunrSearchEngine';
    }

    LunrSearchEngine.prototype.init = function () {
        var that = this,
            d = new promise.Promise();

        that.index = lunr.Index.load(COMPODOC_SEARCH_INDEX.index);
        that.store = COMPODOC_SEARCH_INDEX.store;
        d.done();

        return d;
    };

    LunrSearchEngine.prototype.search = function (q, offset, length) {
        var that = this,
            results = [],
            d = new promise.Promise();

        if (this.index) {
            results = this.index.search('*' + q + '*').map(function (result) {
                var doc = that.store[result.ref];

                return {
                    title: doc.title,
                    url: doc.url,
                    body: doc.summary || doc.body
                };
            });
        }

        d.done({
            query: q,
            results: length === 0 ? results : results.slice(0, length),
            count: results.length
        });

        return d;
    };

    compodoc.addEventListener(compodoc.EVENTS.READY, function (event) {
        var engine = new LunrSearchEngine(),
            initialized = false;

        function query(q, offset, length) {
            if (!initialized) throw new Error('Search has not been initialized');
            return engine.search(q, offset, length);
        }

        compodoc.search = {
            query: query
        };

        engine.init().then(function () {
            initialized = true;
            compodoc.dispatchEvent({
                type: compodoc.EVENTS.SEARCH_READY
            });
        });
    });
})(compodoc);
