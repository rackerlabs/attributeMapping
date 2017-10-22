(:
   ext2JSON.xq

   This query converts an XML RAX-AUTH:extendededAttributes to JSON.

   Copyright 2017 Rackspace US, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
:)
xquery version "3.1" encoding "UTF-8";

declare namespace output = "http://www.w3.org/2010/xslt-xquery-serialization";
declare namespace map = "http://www.w3.org/2005/xpath-functions/map";
declare namespace array = "http://www.w3.org/2005/xpath-functions/array";
declare namespace xs = "http://www.w3.org/2001/XMLSchema";
declare namespace auth = "http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0";

declare option output:method "json";
declare option output:indent "yes";

declare function auth:addAttributeValues ($a as element()) as item() {
  let $values := for $v in $a/auth:value return string($v),
      $multiValue := if (exists($a/@multiValue)) then xs:boolean($a/@multiValue) else false()
    return if ($multiValue) then array {$values} else $values[1]
};

declare function auth:addAttributes ($g as element()) as map(*) {
  map:merge(for $a in $g/auth:attribute return map:entry($a/@name, auth:addAttributeValues($a)))
};

declare function auth:addGroups($extAtts as element()) as map(*) {
  map:merge(for $g in $extAtts/auth:group return map:entry($g/@name,auth:addAttributes($g)))
};

map:merge(map:entry("RAX-AUTH:extendedAttributes", auth:addGroups(auth:extendedAttributes)))
