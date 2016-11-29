xquery version "3.1" encoding "UTF-8";

declare namespace output = "http://www.w3.org/2010/xslt-xquery-serialization";
declare namespace map = "http://www.w3.org/2005/xpath-functions/map";
declare namespace array = "http://www.w3.org/2005/xpath-functions/array";
declare namespace mapping = "http://docs.rackspace.com/identity/api/ext/MappingRules";
declare namespace xs = "http://www.w3.org/2001/XMLSchema";
declare namespace xsi = "http://www.w3.org/2001/XMLSchema-instance";

declare default element namespace "http://docs.rackspace.com/identity/api/ext/MappingRules";

declare option output:method "xml";
declare option output:indent "yes";

(: declare variable $__JSON__ external; :)
declare variable $__JSON__ external;
declare variable $policyJSON := parse-json($__JSON__);
declare variable $defaultAttributes as xs:string* := ('name','email','expire','domain','roles');

declare function mapping:convertValue($in as xs:string) as xs:string {
  replace($in,' ','&#xA0;')
};


declare function mapping:attributeFromValue ($attName as xs:string, $value as item()) as attribute()* {
  typeswitch ($value)
    case $s as xs:string return attribute {$attName} {mapping:convertValue($s)}
    case $b as xs:boolean return attribute {$attName} {mapping:convertValue(string($b))}
    case $a as array(*) return attribute {$attName} {string-join(for $i in $a?* return mapping:convertValue($i),' ')}
    case $o as map(*) return for $att in map:keys($o) return mapping:attributeFromValue($att, $o?($att))
    default return ()
};

declare function mapping:convertLocalAttribute($attribName as xs:string, $attribValue as item(), $userGroup as xs:boolean) as element() {
  element {$attribName} {
    mapping:attributeFromValue("value",$attribValue),
    if ($userGroup and $attribName = $defaultAttributes) then () else attribute {"xsi:type"} {"LocalAttribute"}
  }
};

declare function mapping:convertRemoteAttribute($remoteAttribute as item()) as element() {
  element {"attribute"} {mapping:attributeFromValue("", $remoteAttribute)}
};

<rules xmlns="http://docs.rackspace.com/identity/api/ext/MappingRules"
       xmlns:xs="http://www.w3.org/2001/XMLSchema"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
       {
       if (map:contains($policyJSON,"namespaces")) then
         let $namespaces := $policyJSON?namespaces
         for $key in map:keys($namespaces) return namespace {$key} {$namespaces?($key)}
       else ()
       }

       {
         for $rule in $policyJSON?rules?* return
         <rule>
             <local>
                 {
                   let $local := $rule?local
                   for $localGroupName in map:keys($local) return element {$localGroupName} {
                     if ($localGroupName != 'user') then attribute {"xsi:type"} {"LocalAttributeGroup"} else (),
                     let $localGroup := $local?($localGroupName)
                     for $attributeName in map:keys($localGroup) return
                       mapping:convertLocalAttribute($attributeName, $localGroup?($attributeName), $localGroupName = 'user')
                   }
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

