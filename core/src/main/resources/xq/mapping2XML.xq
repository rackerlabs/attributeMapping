xquery version "3.1" encoding "UTF-8";

declare namespace output = "http://www.w3.org/2010/xslt-xquery-serialization";
declare namespace map = "http://www.w3.org/2005/xpath-functions/map";
declare namespace array = "http://www.w3.org/2005/xpath-functions/array";
declare namespace mapping = "http://docs.rackspace.com/identity/api/ext/MappingRules";
declare namespace xs = "http://www.w3.org/2001/XMLSchema";
declare namespace xsi = "http://www.w3.org/2001/XMLSchema-instance";
declare namespace fn = "http://www.w3.org/2005/xpath-functions";

declare default element namespace "http://docs.rackspace.com/identity/api/ext/MappingRules";

declare option output:method "xml";
declare option output:indent "yes";

(: declare variable $__JSON__ external; :)
declare variable $__JSON__ external;
declare variable $policyJSON := parse-json($__JSON__);
declare variable $multiValueDefaultAttributes as xs:string* := ('roles', 'groups');
declare variable $defaultAttributes as xs:string* := ('name','email','expire','domain', $multiValueDefaultAttributes);
declare variable $remoteMultiValues as xs:string* := ('whitelist','blacklist','notAnyOf','anyOneOf');

declare function mapping:convertMatchValue($in as xs:string) as xs:string {

 (:
   Only the following should not be escaped :  {At(...)}, {Ats(...)}, {Pt(...)}, {Pts(...)}.
 :)
 if (matches($in,'\{(A|P)ts?\(.*?\)\}','ms')) then $in else mapping:convertNonMatchValue($in)
};

declare function mapping:convertNonMatchValue($in as xs:string) as xs:string {
  replace($in,' ','&#xA0;')
};

declare function mapping:convertValue($in as xs:string) as xs:string {
  let $strings as xs:string* :=
    for $pm in analyze-string($in,'\{.*?\}','ms')//(fn:match|fn:non-match) return if (local-name($pm) = 'match') then mapping:convertMatchValue(string($pm))
                                                                                                                            else mapping:convertNonMatchValue(string($pm))
  return string-join($strings,'')
};

declare function mapping:convertValue($name as xs:string, $in as xs:string, $multiValues as xs:string*) as xs:string {
  if ($name = $multiValues) then mapping:convertValue($in) else $in
};


declare function mapping:attributeFromValue ($attName as xs:string, $value as item(), $multiValues as xs:string*) as attribute()* {
  typeswitch ($value)
    case $s as xs:string return attribute {$attName} {mapping:convertValue($attName, $s, $multiValues)}
    case $b as xs:boolean return attribute {$attName} {string($b)}
    case $a as array(*) return attribute {$attName} {string-join(for $i in $a?* return mapping:convertValue($attName, $i, $multiValues),' ')}
    case $o as map(*) return for $att in map:keys($o) return mapping:attributeFromValue($att, $o?($att),$multiValues)
    default return ()
};

declare function mapping:convertLocalGroup($localGroupName as xs:string, $localGroup as item()) as element() {
  try {
    let $name := xs:Name($localGroupName) return
    element {$name} {
                     if ($localGroupName != 'user') then attribute {"xsi:type"} {"LocalAttributeGroup"} else (),
                     for $attributeName in map:keys($localGroup) return
                       mapping:convertLocalAttribute($attributeName, $localGroup?($attributeName), $localGroupName = 'user')
    }
  } catch err:XQDY0074 {
    element {'attributeGroup'} {
      attribute {"name"} {$localGroupName},
      for $attributeName in map:keys($localGroup) return
        mapping:convertLocalAttribute($attributeName, $localGroup?($attributeName), false())
      }
  }
};

declare function mapping:convertLocalAttribute($attribName as xs:string, $attribValue as item(), $userGroup as xs:boolean) as element() {
  let $multiValues as xs:string* :=
    (
    if ($userGroup and ($attribName=$multiValueDefaultAttributes)) then "value" else (),
      typeswitch ($attribValue)
        case $o as map(*) return if (map:contains($o, "multiValue") and $o?multiValue) then "value" else ()
        case $a as array(*) return "value"
        default return ()
    )
    return try {
      element {$attribName} {
        mapping:attributeFromValue("value",$attribValue,$multiValues),
        if ($userGroup and $attribName = $defaultAttributes) then () else attribute {"xsi:type"} {"LocalAttribute"},
          if (not(empty($multiValues))) then
            typeswitch ($attribValue)
              case $o as map(*) return if (not(map:contains($o, "multiValue"))) then attribute {"multiValue"} {"true"} else ()
              default return attribute {"multiValue"} {"true"}
            else ()
      }
    } catch err:XQDY0074 {
      element{'attribute'} {
        attribute {"name"} {$attribName},
        mapping:attributeFromValue("value",$attribValue,$multiValues),
        if (not(empty($multiValues))) then
          typeswitch ($attribValue)
            case $o as map(*) return if (not(map:contains($o, "multiValue"))) then attribute {"multiValue"} {"true"} else ()
            default return attribute {"multiValue"} {"true"}
          else ()
      }
    }
};

declare function mapping:convertRemoteAttribute($remoteAttribute as item()) as element() {
  element {"attribute"} {mapping:attributeFromValue("", $remoteAttribute, $remoteMultiValues)}
};

<mapping xmlns="http://docs.rackspace.com/identity/api/ext/MappingRules"
       xmlns:xs="http://www.w3.org/2001/XMLSchema"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
       {
         attribute {"version"} {$policyJSON?mapping?version}
       }
       {
       if (map:contains($policyJSON?mapping,"namespaces")) then
         let $namespaces := $policyJSON?mapping?namespaces
         for $key in map:keys($namespaces) return namespace {$key} {$namespaces?($key)}
       else ()
       }
       {
         if (map:contains($policyJSON?mapping,"description")) then
           element {"description"} {$policyJSON?mapping?description}
         else ()
       }
       <rules>
       {
         for $rule in $policyJSON?mapping?rules?* return
         <rule>
             <local>
                 {
                   let $local := $rule?local
                   for $localGroupName in map:keys($local) return mapping:convertLocalGroup($localGroupName, $local?($localGroupName))
                 }
             </local>
             {
               if (map:contains($rule,"remote")) then
               <remote>
                 { for $remoteAttribute in $rule?remote?* return mapping:convertRemoteAttribute($remoteAttribute) }
               </remote>
               else ()
             }
         </rule>
       }
       </rules>
</mapping>

