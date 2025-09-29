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
    var $tabSource = document.querySelector('#source-tab'),
        $tabInfo = document.querySelector('#info-tab'),
        $tabReadme = document.querySelector('#readme-tab'),
        $tabTemplate = document.querySelector('#templateData-tab'),
        $tabTree = document.querySelector('#tree-tab'),
        $tabExample = document.querySelector('#example-tab'),
        $prismPre = document.querySelector('pre.compodoc-sourcecode');
    if ($tabSource && $prismPre) {
        $prismCode = $prismPre.querySelector('code'),
        $content = document.querySelector('.content'),
        prismLinks = document.querySelectorAll('.link-to-prism')

        for (var i = 0; i < prismLinks.length; i++) {
            prismLinks[i].addEventListener('click', linkToPrism, false);
        }

        function linkToPrism(event) {
            var targetLine = event.target.getAttribute('data-line');
            event.preventDefault();

            $prismPre.setAttribute('data-line', targetLine);
            Prism.highlightElement($prismCode, function() {});

            $tabSource.click();

            setTimeout(function() {
                var $prismHighlightLine = document.querySelector('.line-highlight'),
                    top = parseInt(getComputedStyle($prismHighlightLine)['top']);
                $content.scrollTop = top;
            }, 500);
        };

        window.onhashchange = function(event) {
            switch (window.location.hash) {
                case '':
                case '#info':
                    $tabInfo.click();
                    break;
                case '#readme':
                    $tabReadme.click();
                    break;
                case '#source':
                    $tabSource.click();
                    break;
                case '#template':
                    $tabTemplate.click();
                    break;
                case '#dom-tree':
                    $tabTree.click();
                    break;
                case '#example':
                    $tabExample.click();
                    break;
            }
        }
    }
});
