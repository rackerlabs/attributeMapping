xquery version "3.1" encoding "UTF-8";

declare namespace output = "http://www.w3.org/2010/xslt-xquery-serialization";
declare namespace map = "http://www.w3.org/2005/xpath-functions/map";
declare namespace array = "http://www.w3.org/2005/xpath-functions/array";
declare namespace mapping = "http://docs.rackspace.com/identity/api/ext/MappingRules";
declare namespace xs = "http://www.w3.org/2001/XMLSchema";
declare namespace fn = "http://www.w3.org/2005/xpath-functions";

declare option output:method "json";
declare option output:indent "yes";

declare variable $matchMarker as xs:string := "{Pts((:-))}";
declare variable $knownPrefixes as xs:string* := ('saml2p','saml2','xs','xsi','mapping','xml','');

declare function mapping:convertAttributeValue($name as xs:string, $in as xs:string, $flags as xs:string*) as xs:anyAtomicType? {
  let $retValue := if ($in = '') then () else replace($in,'&#xA0;',' ')
  return if ($name = $flags) then xs:boolean($retValue) else normalize-space($retValue)
};


declare function mapping:convertAttributeValues($tokenized as xs:string*, $matches as xs:string*,
                                                $tokPos as xs:integer, $matchPos as xs:integer,
                                                $name as xs:string, $flags as xs:string*) as xs:string* {
   if ($tokPos > count($tokenized)) then () else
     if(contains($tokenized[$tokPos],$matchMarker)) then mapping:convertAttributeValues(insert-before(remove($tokenized, $tokPos),$tokPos,concat(substring-before($tokenized[$tokPos],$matchMarker),
                                                        $matches[$matchPos],substring-after($tokenized[$tokPos],$matchMarker))), $matches, $tokPos,$matchPos +1, $name, $flags) else
     (mapping:convertAttributeValue($name, $tokenized[$tokPos], $flags),  mapping:convertAttributeValues($tokenized, $matches, $tokPos + 1,$matchPos, $name, $flags))
};

declare function mapping:tokenizeListValues($in as xs:string, $name as xs:string, $flags as xs:string*) as xs:string* {
  let $asResult := analyze-string($in,'\{(A|P)ts?\(.*?\)\}','ms')//(fn:match|fn:non-match)
  let $matches := for $m in $asResult return if(local-name($m) = 'match') then string($m) else ()
  let $tokenized := tokenize(normalize-space(string-join(for $pm in $asResult return
     if (local-name($pm) = 'match') then $matchMarker else string($pm), '')),' ')
  return mapping:convertAttributeValues($tokenized, $matches,1, 1, $name, $flags)
};

declare function mapping:convertAttributeList($name as xs:string, $in as xs:string, $flags as xs:string*) as item() {
  let $inStrings := mapping:tokenizeListValues($in, $name, $flags)
  return if (count($inStrings) = 1) then $inStrings[1] else array {$inStrings}
};

declare function mapping:convertAttributeMap($elem as element(),
                   $multiValueAttribs as xs:string*,
                   $excludeAttribs as xs:string*,
                   $flags as xs:string*) as map(*) {
  let $attribs := $elem/@*[namespace-uri(.) != 'http://www.w3.org/2001/XMLSchema-instance'][not(local-name() = $excludeAttribs)]
  return map:merge(for $attrib in $attribs return map:entry(local-name($attrib),
    if (local-name($attrib) = $multiValueAttribs) then mapping:convertAttributeList(local-name($attrib), string($attrib), $flags)
    else mapping:convertAttributeValue(local-name($attrib), string($attrib), $flags)))
};

declare function mapping:convertLocalGroup($local as element()) as map(*) {
  let $elems := $local/element()
  return map:merge(for $elem in $elems return map:entry(mapping:local-name($elem),
  let $multiAttribs := (
    if ((mapping:local-name($elem) = 'roles') and (mapping:local-name($elem/..) = 'user')) then "value" else (),
      if (exists($elem/@multiValue) and xs:boolean($elem/@multiValue)) then "value" else ())
        return if ($elem/@*[name(.) = ('type','multiValue')]) then mapping:convertAttributeMap($elem, $multiAttribs, ('name'), ('multiValue')) else
          if ($multiAttribs) then mapping:convertAttributeList (mapping:local-name($elem), string($elem/@value), ('multiValue')) else
            mapping:convertAttributeValue(mapping:local-name($elem), string($elem/@value), ('multiValue'))))
};

declare function mapping:convertLocalGroups($local as element()) as map(*) {
  let $elems := $local/element()
  return map:merge(for $elem in $elems return map:entry(mapping:local-name($elem),mapping:convertLocalGroup($elem)))
};

declare function mapping:convertRemote($remote as element()) as array(*) {
  let $elems := $remote/element()
  return array {for $elem in $elems return mapping:convertAttributeMap($elem,('blacklist','whitelist','notAnyOf','anyOneOf'),(),('multiValue','regex'))}
};

declare function mapping:convertNamespaces($rules as element(), $prefixes as xs:string*) as map(*) {
  map:merge (for $p in $prefixes return map:entry($p , namespace-uri-for-prefix ($p, $rules)))
};

declare function mapping:local-name($elem as element()) as xs:string {
  if ($elem/@name) then $elem/@name else local-name($elem)
};


if (exists(/mapping:mapping)) then
  let $nsPrefixes := for $prefix in in-scope-prefixes(/mapping:mapping) return if ($prefix = $knownPrefixes) then () else $prefix
  let $rules := for $rule in /mapping:mapping/mapping:rules/mapping:rule return map:merge((
    map:entry("local",mapping:convertLocalGroups($rule/mapping:local)),
    if (exists($rule/mapping:remote)) then map:entry("remote",mapping:convertRemote($rule/mapping:remote)) else ()))
      return
        map:merge(
          map:entry("mapping", map:merge(
            (map:entry("rules", array{$rules}),
            if (exists(/mapping:mapping/@version)) then map:entry("version", string(/mapping:mapping/@version)) else (),
              if (exists(/mapping:mapping/mapping:description)) then map:entry("description", string(/mapping:mapping/mapping:description)) else(),
                if (not(empty($nsPrefixes))) then map:entry("namespaces", mapping:convertNamespaces(/mapping:mapping,$nsPrefixes)) else ()))))
              else map { "local" : mapping:convertLocalGroups(mapping:local) }
