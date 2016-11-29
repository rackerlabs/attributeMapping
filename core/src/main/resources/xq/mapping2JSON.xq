xquery version "3.1" encoding "UTF-8";

declare namespace output = "http://www.w3.org/2010/xslt-xquery-serialization";
declare namespace map = "http://www.w3.org/2005/xpath-functions/map";
declare namespace array = "http://www.w3.org/2005/xpath-functions/array";
declare namespace mapping = "http://docs.rackspace.com/identity/api/ext/MappingRules";
declare namespace xs = "http://www.w3.org/2001/XMLSchema";

declare option output:method "json";
declare option output:indent "yes";

declare variable $knownPrefixes as xs:string* := ('saml2p','saml2','xs','xsi','mapping','xml','');

declare function mapping:convertAttributeValue($name as xs:string, $in as xs:string, $flags as xs:string*) as xs:anyAtomicType {
  let $retValue := replace($in,'&#xA0;',' ')
  return if ($name = $flags) then xs:boolean($retValue) else $retValue
};

declare function mapping:convertAttributeList($name as xs:string, $in as xs:string, $flags as xs:string*) as item() {
  let $inStrings := for $s in tokenize($in, ' ') return mapping:convertAttributeValue($name, $s, $flags)
  return if (count($inStrings) = 1) then $inStrings[1] else array {$inStrings}
};

declare function mapping:convertAttributeMap($elem as element(), $multiValueAttribs as xs:string*, $flags as xs:string*) as map(*) {
  let $attribs := $elem/@*[namespace-uri(.) != 'http://www.w3.org/2001/XMLSchema-instance']
  return map:merge(for $attrib in $attribs return map:entry(local-name($attrib),
    if (local-name($attrib) = $multiValueAttribs) then mapping:convertAttributeList(local-name($attrib), string($attrib), $flags)
    else mapping:convertAttributeValue(local-name($attrib), string($attrib), $flags)))
};

declare function mapping:convertLocalGroup($local as element()) as map(*) {
  let $elems := $local/element()
  return map:merge(for $elem in $elems return map:entry(local-name($elem),
  let $multiAttribs := (
    if ((local-name($elem) = 'roles') and (local-name($elem/..) = 'user')) then "value" else (),
      if (exists($elem/@multiValue) and xs:boolean($elem/@multiValue)) then "value" else ())
        return if ($elem/@*[name(.) = ('type','multiValue')]) then mapping:convertAttributeMap($elem, $multiAttribs, ('multiValue')) else
          if ($multiAttribs) then mapping:convertAttributeList (local-name($elem), string($elem/@value), ('multiValue')) else
            mapping:convertAttributeValue(local-name($elem), string($elem/@value), ('multiValue'))))
};

declare function mapping:convertLocalGroups($local as element()) as map(*) {
  let $elems := $local/element()
  return map:merge(for $elem in $elems return map:entry(local-name($elem),mapping:convertLocalGroup($elem)))
};

declare function mapping:convertRemote($remote as element()) as array(*) {
  let $elems := $remote/element()
  return array {for $elem in $elems return mapping:convertAttributeMap($elem,('blacklist','whitelist','notAnyOf','anyOneOf'),('multiValue','regex'))}
};

declare function mapping:convertNamespaces($rules as element(), $prefixes as xs:string*) as map(*) {
  map:merge (for $p in $prefixes return map:entry($p , namespace-uri-for-prefix ($p, $rules)))
};


if (exists(/mapping:rules)) then
  let $nsPrefixes := for $prefix in in-scope-prefixes(/mapping:rules) return if ($prefix = $knownPrefixes) then () else $prefix
  let $rules := for $rule in /mapping:rules/mapping:rule return map:merge((
    map:entry("local",mapping:convertLocalGroups($rule/mapping:local)),
    if (exists($rule/mapping:remote)) then map:entry("remote",mapping:convertRemote($rule/mapping:remote)) else ()))
      return
        map:merge((map:entry("rules", array{$rules}),
        if (not(empty($nsPrefixes))) then map:entry("namespaces", mapping:convertNamespaces(/mapping:rules,$nsPrefixes)) else ()))
else map { "local" : mapping:convertLocalGroups(mapping:local) }
