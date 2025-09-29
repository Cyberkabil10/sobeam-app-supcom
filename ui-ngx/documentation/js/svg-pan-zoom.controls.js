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
    if (document.getElementById('module-graph-svg')) {
        panZoom = svgPanZoom(document.getElementById('module-graph-svg').querySelector('svg'), {
            zoomEnabled: true,
            minZoom: 1,
            maxZoom: 5
        });
    
        document.getElementById('zoom-in').addEventListener('click', function(ev) {
            ev.preventDefault();
            panZoom.zoomIn();
        });
    
        document.getElementById('zoom-out').addEventListener('click', function(ev) {
            ev.preventDefault();
            panZoom.zoomOut();
        });
    
        document.getElementById('reset').addEventListener('click', function(ev) {
            ev.preventDefault();
            panZoom.resetZoom();
            panZoom.resetPan();
        });
    
        var overviewFullscreen = false,
            originalOverviewHeight;
    
        document.getElementById('fullscreen').addEventListener('click', function(ev) {
            if (overviewFullscreen) {
                document.getElementById('module-graph-svg').style.height = originalOverviewHeight;
                overviewFullscreen = false;
                if (ev.target) {
                    ev.target.classList.remove('ion-md-close');
                    ev.target.classList.add('ion-ios-resize');
                }
            } else {
                originalOverviewHeight = document.getElementById('module-graph-svg').style.height;
                document.getElementById('module-graph-svg').style.height = '85vh';
                overviewFullscreen = true;
                if (ev.target) {
                    ev.target.classList.remove('ion-ios-resize');
                    ev.target.classList.add('ion-md-close');
                }
            }
            document.getElementById('module-graph-svg').querySelector('svg').style.height = document.getElementById('module-graph-svg').clientHeight;
            setTimeout(function() {
                panZoom.resize();
                panZoom.fit();
                panZoom.center();
            }, 0)
        });
    }
});
