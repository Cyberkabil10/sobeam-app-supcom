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
document.addEventListener('DOMContentLoaded', function() {
    var lazyGraphs = [].slice.call(document.querySelectorAll('[lazy]'));
    var active = false;

    var lazyLoad = function() {
        if (active === false) {
            active = true;

            setTimeout(function() {
                lazyGraphs.forEach(function(lazyGraph) {
                    if (
                        lazyGraph.getBoundingClientRect().top <= window.innerHeight &&
                        lazyGraph.getBoundingClientRect().bottom >= 0 &&
                        getComputedStyle(lazyGraph).display !== 'none'
                    ) {
                        lazyGraph.data = lazyGraph.getAttribute('lazy');
                        lazyGraph.removeAttribute('lazy');

                        lazyGraphs = lazyGraphs.filter(function(image) { return image !== lazyGraph});

                        if (lazyGraphs.length === 0) {
                            document.removeEventListener('scroll', lazyLoad);
                            window.removeEventListener('resize', lazyLoad);
                            window.removeEventListener('orientationchange', lazyLoad);
                        }
                    }
                });

                active = false;
            }, 200);
        }
    };

    // initial load
    lazyLoad();

    var container = document.querySelector('.container-fluid.modules');
    if (container) {
        container.addEventListener('scroll', lazyLoad);
        window.addEventListener('resize', lazyLoad);
        window.addEventListener('orientationchange', lazyLoad);
    }

});
