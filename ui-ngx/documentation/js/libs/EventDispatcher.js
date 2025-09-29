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
/**
 * @author mrdoob / http://mrdoob.com/
 */

var EventDispatcher=function(){};Object.assign(EventDispatcher.prototype,{addEventListener:function(i,t){void 0===this._listeners&&(this._listeners={});var e=this._listeners;void 0===e[i]&&(e[i]=[]),-1===e[i].indexOf(t)&&e[i].push(t)},hasEventListener:function(i,t){if(void 0===this._listeners)return!1;var e=this._listeners;return void 0!==e[i]&&-1!==e[i].indexOf(t)},removeEventListener:function(i,t){if(void 0!==this._listeners){var e=this._listeners[i];if(void 0!==e){var s=e.indexOf(t);-1!==s&&e.splice(s,1)}}},dispatchEvent:function(i){if(void 0!==this._listeners){var t=this._listeners[i.type];if(void 0!==t){i.target=this;var e=[],s=0,n=t.length;for(s=0;s<n;s++)e[s]=t[s];for(s=0;s<n;s++)e[s].call(this,i)}}}});