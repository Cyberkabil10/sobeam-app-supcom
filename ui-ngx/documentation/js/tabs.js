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
    var tabs = document.getElementsByClassName('nav-tabs'),
        updateAddress = function(e) {
            if(history.pushState && e.target.dataset.link) {
                history.pushState(null, null, '#' + e.target.dataset.link);
            }
        };
    if (tabs.length > 0) {
        tabs = tabs[0].querySelectorAll('li');
        for (var i = 0; i < tabs.length; i++) {
            tabs[i].addEventListener('click', updateAddress);
            var linkTag = tabs[i].querySelector('a');
            if (location.hash !== '') {
                var currentHash = location.hash.substr(1);
                if (currentHash === linkTag.dataset.link) {
                    linkTag.click();
                }
            }
        }
    }
});
