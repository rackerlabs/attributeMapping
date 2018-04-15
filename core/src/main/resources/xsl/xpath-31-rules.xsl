<?xml version="1.0" encoding="UTF-8"?>
<!--
   xpath-31-rules.xsl

   This stylesheet simply does the work of parsing the
   xpath-31-rules.xml file and placing the data in convenient
   variables.

   Copyright 2018 Rackspace US, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->
<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               xmlns:xs="http://www.w3.org/2001/XMLSchema"
               xmlns:map="http://www.w3.org/2005/xpath-functions/map"
               xmlns:mapping="http://docs.rackspace.com/identity/api/ext/MappingRules"
               version="3.0">

    <!--
        Don't expect the location of the config to change,
        but safer to put this as a param.
    -->
    <xsl:param name="xpathRulesConfig" as="xs:string">xpath-31-rules.xml</xsl:param>

    <xsl:variable name="xpathRules" as="document-node()" select="doc($xpathRulesConfig)"/>

    <!--
        Common namespaces
    -->
    <xsl:variable name="commonNS" as="map(xs:string, xs:anyURI)"
                  select="let
                          $common   := $xpathRules/mapping:xpath-validate-rules/mapping:common,
                          $prefixes := tokenize(normalize-space($common/@ns),' ')
                          return
                          map:merge(for $p in $prefixes return
                          map:entry($p, namespace-uri-for-prefix($p, $common)),
                          map {
                              'duplicates' : 'use-any'
                          })"/>

    <!--
        A set of functions to block.
    -->
    <xsl:variable name="blockFuns" as="map(xs:QName, xs:boolean)"
                  select="let
                          $blocked := $xpathRules/mapping:xpath-validate-rules/mapping:blocked,
                          $funs    := tokenize(normalize-space($blocked/@funs),' ')
                          return
                          map:merge(for $f in $funs return
                          map:entry(resolve-QName($f, $blocked), true()),
                          map {
                              'duplicates' : 'use-any'
                          })
                          "/>

    <!--
        Set of namespace URIs that we allow in functions
    -->
    <xsl:variable name="legalFunNS" as="map(xs:anyURI, xs:boolean)"
                  select="let
                          $allowed  := $xpathRules/mapping:xpath-validate-rules/mapping:allowed,
                          $prefixes := tokenize(normalize-space($allowed/@ns),' ')
                          return
                          map:merge(for $p in $prefixes return
                          map:entry(namespace-uri-for-prefix($p, $allowed), true()),
                          map {
                              'duplicates' : 'use-any'
                          })
                          "/>
</xsl:transform>
