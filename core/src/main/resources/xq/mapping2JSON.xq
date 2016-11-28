xquery version "3.1" encoding "UTF-8";

declare namespace output = "http://www.w3.org/2010/xslt-xquery-serialization";
declare namespace map = "http://www.w3.org/2005/xpath-functions/map";
declare namespace array = "http://www.w3.org/2005/xpath-functions/array";
declare namespace mapping = "http://docs.rackspace.com/identity/api/ext/MappingRules";
declare namespace xs = "http://www.w3.org/2001/XMLSchema";

declare option output:method "json";
declare option output:indent "yes";

declare function mapping:convertAttributeMap($elem as element()) as map(*) {
  let $attribs := $elem/@*[namespace-uri(.) != 'http://www.w3.org/2001/XMLSchema-instance']
  return map:merge(for $attrib in $attribs return map:entry(local-name($attrib), string($attrib)))
};

declare function mapping:convertLocalGroup($local as element()) as map(*) {
  let $elems := $local/element()
  return map:merge(for $elem in $elems return map:entry(local-name($elem),
    if ($elem/@*[name(.) = ('type','multivalue')]) then mapping:convertAttributeMap($elem) else string($elem/@value)))
};

declare function mapping:convertLocalGroups($local as element()) as map(*) {
  let $elems := $local/element()
  return map:merge(for $elem in $elems return map:entry(local-name($elem),mapping:convertLocalGroup($elem)))
};

let $rules := for $rule in /mapping:rules/mapping:rule return map {
  "local" : mapping:convertLocalGroups($rule/mapping:local),
  "remote" : "remotePart"
}
return map {"rules" : array{$rules}}
