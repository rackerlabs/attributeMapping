<?xml version="1.0" encoding="UTF-8"?>
<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:xslout="http://www.rackspace.com/repose/wadl/checker/Transform"
    xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
    xmlns:mapping="http://docs.rackspace.com/identity/api/ext/MappingRules"
    xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    version="2.0">
    
    <xsl:namespace-alias stylesheet-prefix="xslout" result-prefix="xsl"/>
    <xsl:output method="xml" encoding="UTF-8" indent="yes"/>
    
    <xsl:template match="/">
        <xsl:comment>
            -                                                       -
            -  THIS IS A GENERATED TRANSFORM  DON'T EDIT BY HAND    -
            -                                                       -
        </xsl:comment>
        <xslout:transform version="2.0" xmlns="http://docs.rackspace.com/identity/api/ext/MappingRules">
            <xsl:copy-of select="/mapping:rules/namespace::*"/>
            <xslout:param name="outputSAML" as="xs:boolean" select="false()"/>
            <xslout:output method="xml" encoding="UTF-8" indent="yes"/>
            <xslout:variable name="locals" as="node()*">
                <xsl:for-each select="/mapping:rules/mapping:rule">
                    <xslout:call-template name="{generate-id(.)}"/>
                </xsl:for-each>
            </xslout:variable>
            <xslout:variable name="assert" as="node()" select="/"/>
            <xslout:variable name="specialAttributes" as="xs:string*" select="('email', 'domain', 'roles')"/>
            <xslout:variable name="skipAttributes" as="xs:string*" select="('name','expire')"/>
            <xslout:template match="/">
                <xslout:choose>
                    <xslout:when test="$outputSAML">
                        <xslout:apply-templates/>
                    </xslout:when>
                    <xslout:otherwise>
                        <xslout:call-template name="mapping:outLocal"/>
                    </xslout:otherwise>
                </xslout:choose>
            </xslout:template>
            <xsl:apply-templates />
            
            <!-- output templates -->
            <xslout:template match="node() | @*">
               <xslout:copy>
                   <xslout:apply-templates select="@* | node()"/>
               </xslout:copy>
            </xslout:template>

            <xslout:template match="saml2:Assertion[(position() = 1) and not(empty($locals))]">
                <xslout:variable name="outLocal" as="node()*">
                    <xslout:call-template name="mapping:outLocal"/>
                </xslout:variable>
                <saml2:Assertion>
                    <xslout:apply-templates select="@*[not(local-name() = 'ID')]"/>
                    <xslout:attribute name="ID" select="concat(@ID,'__RAX__')"/>
                    <xslout:copy-of select="saml2:Issuer"/>
                    <saml2:Subject>
                        <saml2:NameID Format="urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified"><xslout:value-of select="$outLocal/mapping:user/mapping:name/@value"/></saml2:NameID>
                        <saml2:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
                            <saml2:SubjectConfirmationData>
                                <xslout:attribute name="NotOnOrAfter" select="$outLocal/mapping:user/mapping:expire/@value"/>
                            </saml2:SubjectConfirmationData>
                        </saml2:SubjectConfirmation>
                    </saml2:Subject>
                    <xslout:copy-of select="saml2:AuthnStatement"/>
                    <saml2:AttributeStatement>
                        <xslout:apply-templates select="$outLocal//element()[@value]" mode="samlout"/>
                    </saml2:AttributeStatement>
                </saml2:Assertion>
                <xslout:copy>
                   <xslout:apply-templates select="@* | node()"/>
                </xslout:copy>
            </xslout:template>
            
            <xslout:template match="element()[@value]" mode="samlout">
                <xslout:variable name="groupName" as="xs:string" select="../local-name()"/>
                <xslout:variable name="isMultiValue" select="if ($groupName = 'user' and local-name() = 'roles') then true() else 
                                                             if (exists(@multiValue)) then xs:boolean(@multiValue) else false()" as="xs:boolean"/>
                <xslout:variable name="attribName" as="xs:string"
                    select="if (local-name() = $specialAttributes and $groupName='user') then local-name() else concat($groupName,'/',local-name())"/>
                <xslout:variable name="attribValues" as="xs:string*"
                         select="if ($isMultiValue) then tokenize(@value,' ') else (@value)"/>
                <xslout:variable name="type" as="attribute()?" select="@type"/>
                <xslout:choose>
                    <xslout:when test="local-name() = $skipAttributes and $groupName = 'user'"/>
                    <xslout:otherwise>
                        <saml2:Attribute>
                            <xslout:attribute name="Name" select="$attribName"/>
                            <xslout:for-each select="$attribValues">
                                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                                    <xslout:choose>
                                        <xslout:when test="exists($type)">
                                            <xslout:attribute name="xsi:type" select="$type"/>
                                        </xslout:when>
                                        <xslout:otherwise>
                                            <xslout:attribute name="xsi:type">xs:string</xslout:attribute>
                                        </xslout:otherwise>
                                    </xslout:choose>
                                    <xslout:value-of select="mapping:transformNBSP(.)"/>
                                </saml2:AttributeValue>
                            </xslout:for-each>
                        </saml2:Attribute>
                    </xslout:otherwise>
                </xslout:choose>
            </xslout:template>
               
            <xslout:template name="mapping:outLocal">
                <local>
                    <xslout:if test="not(empty($locals))">
                        <user>
                            <name>
                                <xslout:attribute name="value" select="($locals//mapping:user/mapping:name)[1]/@value"/>
                            </name>
                            <email>
                                <xslout:attribute name="value" select="($locals//mapping:user/mapping:email)[1]/@value"/>
                            </email>
                            <expire>
                                <xslout:attribute name="value" select="($locals//mapping:user/mapping:expire)[1]/@value"/>
                            </expire>
                            <domain>
                                <xslout:attribute name="value" select="($locals//mapping:user/mapping:domain)[1]/@value"/>
                            </domain>
                            <roles>
                                <xslout:attribute name="value" select="string-join($locals//mapping:user/mapping:roles/@value,' ')"/>
                            </roles>
                            <xslout:call-template name="mapping:outLocalExt">
                                <xslout:with-param name="groups" select="$locals//mapping:user"/>
                                <xslout:with-param name="exclude" select="('name','email','expire','domain','roles')"/>
                            </xslout:call-template>
                        </user>
                        <xslout:variable name="extendedAttributeGroupNames" as="xs:string*"
                            select="distinct-values(for $group in $locals/mapping:* return 
                                     for $groupName in local-name($group) return if ($groupName = 'user') then () else $groupName)"/>
                        <xslout:for-each select="$extendedAttributeGroupNames">
                            <xslout:variable name="extendedAttributeGroupName" as="xs:string" select="."/>
                            <xslout:element>
                                <xsl:attribute name="name">{$extendedAttributeGroupName}</xsl:attribute>
                                <xslout:call-template name="mapping:outLocalExt">
                                  <xslout:with-param name="groups" select="$locals/element()[local-name(.) = $extendedAttributeGroupName]"/>
                                </xslout:call-template>
                            </xslout:element>
                        </xslout:for-each>
                    </xslout:if>
                </local>
            </xslout:template>
            <xslout:template name="mapping:outLocalExt">
                <xslout:param name="groups" as="node()*"/>
                <xslout:param name="exclude" as="xs:string*" select="()"/>
                <xslout:variable name="distinctExts" as="xs:string*">
                    <xslout:sequence select="distinct-values(for $g in $groups/element() return if (local-name($g) = $exclude) then () else local-name($g))"/>
                </xslout:variable>
                <xslout:for-each select="$distinctExts">
                    <xslout:variable name="extName" as="xs:string" select="."/>
                    <xslout:variable name="multiValueAttrib" as="attribute()?" select="($groups/element()[local-name(.)=$extName]/@multiValue)[1]"/>
                    <xslout:variable name="isMultiValue" select="if (exists($multiValueAttrib)) then xs:boolean($multiValueAttrib) else false()" as="xs:boolean"/>
                    <xslout:element>
                        <xsl:attribute name="name">{$extName}</xsl:attribute>
                        <xslout:choose>
                            <xslout:when test="$isMultiValue">
                              <xslout:attribute name="value" select="string-join($groups/element()[local-name(.)=$extName]/@value,' ')"/>
                            </xslout:when>
                            <xslout:otherwise>
                              <xslout:attribute name="value" select="($groups/element()[local-name(.)=$extName])[1]/@value"/>
                            </xslout:otherwise>
                        </xslout:choose>
                        <xslout:for-each select="($groups/element()[local-name(.)=$extName])[1]/@*[not(local-name() = 'value')]">
                            <xslout:attribute>
                                <xsl:attribute name="name">{name(.)}</xsl:attribute>
                                <xslout:value-of select="."/>
                            </xslout:attribute>
                        </xslout:for-each>
                    </xslout:element>
                </xslout:for-each>
            </xslout:template>
            <xslout:function name="mapping:get-attributes" as="xs:string*">
                <xslout:param name="name" as="xs:string"/>
                <xslout:sequence
                    select="$assert//saml2:Assertion/saml2:AttributeStatement/saml2:Attribute[@Name=$name]/saml2:AttributeValue"/>
            </xslout:function>
            <xslout:function name="mapping:get-attribute" as="xs:string">
                <xslout:param name="name" as="xs:string"/>
                <xslout:sequence select="mapping:get-attributes($name)[1]"/>
            </xslout:function>
            <xslout:function name="mapping:transformNBSP" as="xs:string">
                <xslout:param name="in" as="xs:string"/>
                <xslout:value-of select="replace($in,'&#xA0;',' ')"/>
            </xslout:function>
        </xslout:transform>
    </xsl:template>
    
    <xsl:template match="mapping:rule">
        <xsl:variable name="remoteMappers" as="node()*" select="mapping:remote/element()"/>
        <xslout:template name="{generate-id(.)}">
            <xsl:variable name="fireConditions" as="node()*">
                <!-- Exit out of this template if conditions are not met -->
                <xsl:apply-templates select="mapping:remote" mode="fireConditions" />
            </xsl:variable>
            <xsl:variable name="genLocal" as="node()*">
                <xsl:apply-templates mode="genLocal" select="mapping:local">
                    <xsl:with-param name="remoteMappers" select="$remoteMappers"/>
                </xsl:apply-templates> 
            </xsl:variable>
            <xsl:choose>
                <xsl:when test="empty($fireConditions)">
                    <xsl:sequence select="$genLocal"/>
                </xsl:when>
                <xsl:otherwise>
                    <xslout:choose>
                        <xsl:sequence select="$fireConditions"/>
                        <xslout:otherwise>
                            <xsl:sequence select="$genLocal"/>
                        </xslout:otherwise>
                    </xslout:choose>
                </xsl:otherwise>
            </xsl:choose>
        </xslout:template>
    </xsl:template>
    
    <!-- remove superfluous text in all modes -->
    <xsl:template match="text()" mode="#all"/>
    
    <!-- genLocal mode generate a local view of the current rule -->

    <xsl:template match="node()" mode="genLocal" priority="10">
        <xsl:param name="remoteMappers" as="node()*"/>
        <xsl:copy>
            <xsl:apply-templates mode="genLocal" select="@* | node()">
                <xsl:with-param name="remoteMappers" as="node()*" select="$remoteMappers"/>
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="@*[not(name() = 'value')]" mode="genLocal">
       <xslout:attribute name="{name()}"><xsl:value-of select="."/></xslout:attribute>
    </xsl:template>
    
    <xsl:template match="@value[not(contains(.,'{'))]" mode="genLocal">
        <xslout:attribute name="{name()}"><xsl:value-of select="."/></xslout:attribute>
    </xsl:template>
    
    <xsl:template match="@value[contains(.,'{')]" mode="genLocal">
        <xsl:param name="remoteMappers" as="node()*"/>
        <xslout:attribute name="value">
            <xsl:call-template name="mapping:handleAttribute">
                <xsl:with-param name="parent" select=".."/>
                <xsl:with-param name="in" select="."/>
                <xsl:with-param name="remoteMappers" select="$remoteMappers"/>
            </xsl:call-template>
        </xslout:attribute>
    </xsl:template>
    
    <xsl:template match="@value[(local-name(..)='expire' and local-name(../..)='user') or
                                (if (exists(../@type)) then
                                  for $type in resolve-QName(../@type, ..) return
                                    (local-name-from-QName($type) = 'dateTime' and
                                    namespace-uri-from-QName($type) = 'http://www.w3.org/2001/XMLSchema')
                                 else false())]" priority="10" mode="genLocal">
        <xsl:param name="remoteMappers" as="node()*"/>
        <xslout:attribute name="value">
            <xslout:variable name="durationText" as="node()">
                <xsl:call-template name="mapping:handleAttribute">
                    <xsl:with-param name="parent" select=".."/>
                    <xsl:with-param name="in" select="."/>
                    <xsl:with-param name="remoteMappers" select="$remoteMappers"/>
                </xsl:call-template>
            </xslout:variable>
            <xslout:choose>
                <xslout:when test="$durationText castable as xs:dayTimeDuration">
                    <xslout:variable name="duration" as="xs:duration" select="xs:dayTimeDuration($durationText)"/>
                    <xslout:value-of select="current-dateTime()+$duration"/>
                </xslout:when>
                <xslout:otherwise>
                    <xslout:value-of select="$durationText"/>
                </xslout:otherwise>
            </xslout:choose>
        </xslout:attribute>
    </xsl:template>
    
    <xsl:template name="mapping:handleAttribute">
        <xsl:param name="parent" as="element()"/>
        <xsl:param name="in" as="xs:string"/>
        <xsl:param name="remoteMappers" as="node()*"/>
        <xsl:analyze-string select="$in" regex="(.*)??(\{{.*\}})(.*)?">
            <xsl:matching-substring>
                <xsl:if test="not(empty(regex-group(1)))">
                    <xsl:sequence select="mapping:mapAttribute($parent,regex-group(1),$remoteMappers)"/>
                </xsl:if>
                <xsl:sequence select="mapping:mapAttribute($parent,regex-group(2),$remoteMappers)"/>
                <xsl:if test="not(empty(regex-group(3)))">
                    <xsl:call-template name="mapping:handleAttribute">
                        <xsl:with-param name="parent" select="$parent"/>
                        <xsl:with-param name="in" select="regex-group(3)"/>
                        <xsl:with-param name="remoteMappers" select="$remoteMappers"/>
                    </xsl:call-template>
                </xsl:if>
            </xsl:matching-substring>
            <xsl:non-matching-substring>
                <xsl:sequence select="mapping:mapAttribute($parent,$in,$remoteMappers)"/>
            </xsl:non-matching-substring>
        </xsl:analyze-string>
    </xsl:template>
    
    <xsl:template match="mapping:attribute[not(xs:boolean(@regex))]" mode="genLocal" priority="15">
        <xsl:variable name="cond">
            <xsl:choose>
                <xsl:when test="@name">
                    <xsl:choose>
                        <xsl:when test="xs:boolean(@multiValue)">
                            <xsl:value-of select="mapping:attributes(@name)"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="mapping:attribute(@name)"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:when test="@path"><xsl:value-of select="@path"/></xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="separator" as="xs:string" select="if (xs:boolean(@multiValue)) then ' ' else ''"/>
        <xsl:choose>
            <xsl:when test="@whitelist">
                <xslout:value-of select="for $i in {$cond} return if ($i = {mapping:quotedList(@whitelist)}) then $i else ()" separator="{$separator}"/>
            </xsl:when>
            <xsl:when test="@blacklist">
                <xslout:value-of select="for $i in {$cond} return if ($i = {mapping:quotedList(@blacklist)}) then () else $i" separator="{$separator}"/>
            </xsl:when>
            <xsl:otherwise>
                <xslout:value-of select="{$cond}" separator="{$separator}"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="mapping:attribute[xs:boolean(@regex)]" mode="genLocal" priority="15">
        <xsl:variable name="cond">
            <xsl:choose>
                <xsl:when test="@name">
                    <xsl:choose>
                        <xsl:when test="xs:boolean(@multiValue)">
                            <xsl:value-of select="mapping:attributes(@name)"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="mapping:attribute(@name)"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:when test="@path"><xsl:value-of select="@path"/></xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="separator" as="xs:string" select="if (xs:boolean(@multiValue)) then ' ' else ''"/>
        <xsl:choose>
            <xsl:when test="@whitelist">
                <xslout:value-of select="for $i in {$cond} return if (matches ($i, {mapping:quote(@whitelist)})) then $i else ()" separator="{$separator}"/>
            </xsl:when>
            <xsl:when test="@blacklist">
                <xslout:value-of select="for $i in {$cond} return if (matches ($i, {mapping:quote(@blacklist)})) then () else $i" separator="{$separator}"/>
            </xsl:when>
            <xsl:otherwise>
                <xslout:value-of select="{$cond}" separator="{$separator}"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:function name="mapping:mapAttribute" as="node()*">
        <xsl:param name="parent" as="element()"/>
        <xsl:param name="part" as="xs:string"/>
        <xsl:param name="remoteMappers" as="node()*"/>
        
        <xsl:choose>
            <xsl:when test="$part=''"/>
            <xsl:when test="not(contains($part,'{'))">
                <xslout:text><xsl:value-of select="$part"/></xslout:text>
            </xsl:when>
            <xsl:when test="$part='{D}'">
                <xsl:choose>
                    <xsl:when test="(local-name($parent/..) = 'user') and
                                    (namespace-uri($parent/..)='http://docs.rackspace.com/identity/api/ext/MappingRules') ">
                        <xsl:sequence select="mapping:defaultForName(local-name($parent))"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:message terminate="yes">[ERROR] The attribute <xsl:value-of select="concat(name($parent/..),'/',name($parent))"/> does not have a default value.</xsl:message>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:when test="matches($part,'\{D\(.*\)\}')">
                <xsl:analyze-string select="$part" regex="\{{D\((.*)\)\}}">
                    <xsl:matching-substring>
                        <xsl:sequence select="mapping:defaultForName(regex-group(1))"/>
                    </xsl:matching-substring>
                </xsl:analyze-string>
            </xsl:when>
            <xsl:when test="matches($part,'\{[0-9]*\}')">
                <xsl:sequence select="mapping:attributeByNumber($parent, $part, $remoteMappers)"/>
            </xsl:when>
            <xsl:otherwise>
                <xslout:text><xsl:value-of select="$part"/></xslout:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
    
    <xsl:function name="mapping:defaultForName" as="node()*">
        <xsl:param name="name" as="xs:string"/>
        <xsl:choose>
            <xsl:when test="$name='name'"><xslout:value-of select="(/saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID)[1]"/></xsl:when>
            <xsl:when test="$name='expire'"><xslout:value-of select="(/saml2p:Response/saml2:Assertion/saml2:Subject/saml2:SubjectConfirmation/saml2:SubjectConfirmationData/@NotOnOrAfter)[1]"/></xsl:when>
            <xsl:when test="$name='email'"><xslout:value-of select="{mapping:attribute('email')}"/></xsl:when>
            <xsl:when test="$name='domain'"><xslout:value-of select="{mapping:attribute('domain')}"/></xsl:when>
            <xsl:when test="$name='roles'"><xslout:value-of select="{mapping:attributes('roles')}" separator=" "/></xsl:when>
            <xsl:otherwise>
                <xsl:message terminate="yes">[ERROR] No default value for attribute <xsl:value-of select="$name"/></xsl:message>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
    
    <xsl:function name="mapping:attributeByNumber" as="node()*">
        <xsl:param name="parent" as="element()"/>
        <xsl:param name="part" as="xs:string"/>
        <xsl:param name="remoteMappers" as="node()*"/>
        <xsl:analyze-string select="$part" regex="\{{([0-9]*)\}}">
            <xsl:matching-substring>
                <xsl:variable name="idx" as="xs:integer" select="xs:integer(regex-group(1))+1"/>
                <xsl:variable name="mapper" as="element()" select="$remoteMappers[$idx]"/>
                <xsl:apply-templates select="$mapper" mode="genLocal"/>
            </xsl:matching-substring>
        </xsl:analyze-string>
    </xsl:function>
    
    <xsl:function name="mapping:attribute" as="xs:string">
        <xsl:param name="name" as="xs:string"/>
        <xsl:value-of select="concat('(/saml2p:Response/saml2:Assertion/saml2:AttributeStatement/saml2:Attribute[@Name=',mapping:quote($name),']/saml2:AttributeValue)[1]')"/>
    </xsl:function>
    
    <xsl:function name="mapping:attributes" as="xs:string">
        <xsl:param name="name" as="xs:string"/>
        <xsl:value-of select="concat('/saml2p:Response/saml2:Assertion/saml2:AttributeStatement/saml2:Attribute[@Name=',mapping:quote($name),']/saml2:AttributeValue')"/>
    </xsl:function>
    
    <!-- fireConditions mode these templates create conditions for notAnyOf and anyOneOf -->
    
    <xsl:template match="mapping:attribute[@name and @notAnyOf and not(xs:boolean(@regex))]" mode="fireConditions">
        <xslout:when test="some $attr in /saml2p:Response/saml2:Assertion/saml2:AttributeStatement/saml2:Attribute[@Name='{@name}']/saml2:AttributeValue satisfies $attr = {mapping:quotedList(@notAnyOf)}"/>
    </xsl:template>
    
    <xsl:template match="mapping:attribute[@name and @anyOneOf and not(xs:boolean(@regex))]" mode="fireConditions">
        <xslout:when test="every $attr in /saml2p:Response/saml2:Assertion/saml2:AttributeStatement/saml2:Attribute[@Name='{@name}']/saml2:AttributeValue satisfies not($attr = {mapping:quotedList(@anyOneOf)})"/>
    </xsl:template>
    
    <xsl:template match="mapping:attribute[@name and @notAnyOf and xs:boolean(@regex)]" mode="fireConditions">
        <xslout:when test="some $attr in /saml2p:Response/saml2:Assertion/saml2:AttributeStatement/saml2:Attribute[@Name='{@name}']/saml2:AttributeValue satisfies matches($attr, {mapping:quote(@notAnyOf)})"/>
    </xsl:template>
    
    <xsl:template match="mapping:attribute[@name and @anyOneOf and xs:boolean(@regex)]" mode="fireConditions">
        <xslout:when test="every $attr in /saml2p:Response/saml2:Assertion/saml2:AttributeStatement/saml2:Attribute[@Name='{@name}']/saml2:AttributeValue satisfies not(matches($attr, {mapping:quote(@anyOneOf)}))"/>
    </xsl:template>
    
    <xsl:template match="mapping:attribute[@path and @notAnyOf and not(xs:boolean(@regex))]" mode="fireConditions">
        <xslout:when test="some $attr in {@path} satisfies $attr = {mapping:quotedList(@notAnyOf)}"/>
    </xsl:template>
    
    <xsl:template match="mapping:attribute[@path and @anyOneOf and not(xs:boolean(@regex))]" mode="fireConditions">
        <xslout:when test="every $attr in {@path} satisfies not($attr = {mapping:quotedList(@anyOneOf)})"/>
    </xsl:template>
    
    <xsl:template match="mapping:attribute[@path and @notAnyOf and xs:boolean(@regex)]" mode="fireConditions">
        <xslout:when test="some $attr in {@path} satisfies matches($attr, {mapping:quote(@notAnyOf)})"/>
    </xsl:template>
    
    <xsl:template match="mapping:attribute[@path and @anyOneOf and xs:boolean(@regex)]" mode="fireConditions">
        <xslout:when test="every $attr in {@path} satisfies not(matches($attr, {mapping:quote(@anyOneOf)}))"/>
    </xsl:template>
    
    <!-- Util Functions -->
    <xsl:function name="mapping:quote" as="xs:string">
        <xsl:param name="in" as="xs:string"/>
        <xsl:variable name="sq" as="xs:string">'</xsl:variable>
        <xsl:value-of select="concat($sq,$in,$sq)"/>
    </xsl:function>
    
    <xsl:function name="mapping:quotedList" as="xs:string">
        <xsl:param name="in" as="xs:string"/>
        <xsl:variable name="list" as="xs:string"><xsl:value-of select="for $m in tokenize($in,' ') return mapping:quote($m)" separator=", "/></xsl:variable>
        <xsl:value-of select="concat('(',$list,')')"/>
    </xsl:function>
    
</xsl:transform>
