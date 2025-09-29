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
!function(e){if(e&&!("innerHTML"in e.prototype)){var t=function(e,r){var i=e.nodeType;if(3==i)r.push(e.textContent.replace(/&/,"&amp;").replace(/</,"&lt;").replace(">","&gt;"));else if(1==i){if(r.push("<",e.tagName),e.hasAttributes())for(var n=e.attributes,s=0,o=n.length;s<o;++s){var a=n.item(s);r.push(" ",a.name,"='",a.value,"'")}if(e.hasChildNodes()){r.push(">");for(var h=e.childNodes,s=0,o=h.length;s<o;++s)t(h.item(s),r);r.push("</",e.tagName,">")}else r.push("/>")}else{if(8!=i)throw"Error serializing XML. Unhandled node of type: "+i;r.push("\x3c!--",e.nodeValue,"--\x3e")}};Object.defineProperty(e.prototype,"innerHTML",{get:function(){for(var e=[],r=this.firstChild;r;)t(r,e),r=r.nextSibling;return e.join("")},set:function(e){for(;this.firstChild;)this.removeChild(this.firstChild);try{var t=new DOMParser;t.async=!1,sXML="<svg xmlns='http://www.w3.org/2000/svg'>"+e+"</svg>";for(var r=t.parseFromString(sXML,"text/xml").documentElement.firstChild;r;)this.appendChild(this.ownerDocument.importNode(r,!0)),r=r.nextSibling}catch(e){throw new Error("Error parsing XML string")}}})}}((0,eval)("this").SVGElement);