xquery version "3.1" encoding "UTF-8";

declare namespace output = "http://www.w3.org/2010/xslt-xquery-serialization";
declare namespace map = "http://www.w3.org/2005/xpath-functions/map";
declare namespace array = "http://www.w3.org/2005/xpath-functions/array";
declare namespace mapping = "http://docs.rackspace.com/identity/api/ext/MappingRules";
declare namespace xs = "http://www.w3.org/2001/XMLSchema";

declare option output:method "json";
declare option output:indent "yes";

declare function mapping:convertAttributeList($in as xs:string) as array(xs:string) {
  let $inStrings := for $s in tokenize($in, ' ') return replace($s,'&#xA0;',' ')
  return array {$inStrings}
};

declare function mapping:convertAttributeMap($elem as element(), $multiValueAttribs as xs:string*) as map(*) {
  let $attribs := $elem/@*[namespace-uri(.) != 'http://www.w3.org/2001/XMLSchema-instance']
  return map:merge(for $attrib in $attribs return map:entry(local-name($attrib),
    if (local-name($attrib) = $multiValueAttribs) then mapping:convertAttributeList(string($attrib))
    else string($attrib)))
};

declare function mapping:convertLocalGroup($local as element()) as map(*) {
  let $elems := $local/element()
  return map:merge(for $elem in $elems return map:entry(local-name($elem),
  let $multiAttribs := (
    if ((local-name($elem) = 'roles') and (local-name($elem/..) = 'user') and count(tokenize($elem/@value,' ')) > 1) then "value" else (),
      if (exists($elem/@multiValue) and xs:boolean($elem/@multiValue) and count(tokenize($elem/@value,' ')) > 1) then "value" else ())
        return if ($elem/@*[name(.) = ('type','multivalue')]) then mapping:convertAttributeMap($elem, $multiAttribs) else
          if ($multiAttribs) then mapping:convertAttributeList (string($elem/@value)) else string($elem/@value)))
};

declare function mapping:convertLocalGroups($local as element()) as map(*) {
  let $elems := $local/element()
  return map:merge(for $elem in $elems return map:entry(local-name($elem),mapping:convertLocalGroup($elem)))
};

declare function mapping:convertRemote($remote as element()) as array(*) {
  let $elems := $remote/element()
  return array {for $elem in $elems return mapping:convertAttributeMap($elem,('blacklist','whitelist','notAnyOf','anyOneOf'))}
};

let $rules := for $rule in /mapping:rules/mapping:rule return map:merge((
  map:entry("local",mapping:convertLocalGroups($rule/mapping:local)),
  if (exists($rule/mapping:remote)) then map:entry("remote",mapping:convertRemote($rule/mapping:remote)) else ()))
return map {"rules" : array{$rules}}
