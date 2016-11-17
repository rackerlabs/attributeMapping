<?xml version="1.0" encoding="UTF-8"?>
<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:xslout="http://www.rackspace.com/repose/wadl/checker/Transform"
    xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
    xmlns:mapping="http://docs.rackspace.com/identity/api/ext/MappingRules"
    exclude-result-prefixes="xs"
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
            <xslout:template match="/">
                <xsl:comment>Launch templates!</xsl:comment>
            </xslout:template>
            <xsl:apply-templates />
        </xslout:transform>
    </xsl:template>
    
    <xsl:template match="mapping:rule">
        <xsl:variable name="remoteMappers" as="node()*" select="mapping:remote/element()"/>
        <xslout:template name="{generate-id(.)}">
            <xslout:choose>
                <!-- Exit out of this template if conditions are not met -->
                <xsl:apply-templates select="mapping:remote" mode="fireConditions" />
                <xslout:otherwise>
                   <xsl:apply-templates mode="genLocal" select="mapping:local">
                      <xsl:with-param name="remoteMappers" select="$remoteMappers"/>
                   </xsl:apply-templates> 
                </xslout:otherwise>
            </xslout:choose>
        </xslout:template>
    </xsl:template>
    
    <!-- remove superfluous text in all modes -->
    <xsl:template match="text()" mode="#all"/>
    
    <!-- genLocal mode generate a local view of the current rule -->

    <xsl:template match="node()" mode="genLocal" priority="10">
        <xsl:param name="remoteMappers" as="node()*"/>
        <xsl:copy>
            <xsl:apply-templates mode="genLocal" select="node() | @*">
                <xsl:with-param name="remoteMappers" as="node()*" select="$remoteMappers"/>
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="@*[not(contains(.,'{'))]" mode="genLocal">
        <xsl:copy/>
    </xsl:template>
    
    <xsl:template match="@*[contains(.,'{')]" mode="genLocal">
        <xsl:param name="remoteMappers" as="node()*"/>
        <xslout:attribute name="{name()}">
            <xsl:call-template name="mapping:handleAttribute">
                <xsl:with-param name="name" select="name()"/>
                <xsl:with-param name="in" select="."/>
                <xsl:with-param name="remoteMappers" select="$remoteMappers"/>
            </xsl:call-template>
        </xslout:attribute>
    </xsl:template>
    
    <xsl:template name="mapping:handleAttribute">
        <xsl:param name="name" as="xs:string"/>
        <xsl:param name="in" as="xs:string"/>
        <xsl:param name="remoteMappers" as="node()*"/>
        <xsl:analyze-string select="$in" regex="(.*)??(\{{.*\}})(.*)?">
            <xsl:matching-substring>
                <xsl:if test="not(empty(regex-group(1)))">
                    <xsl:sequence select="mapping:mapAttribute($name,regex-group(1),$remoteMappers)"/>
                </xsl:if>
                <xsl:sequence select="mapping:mapAttribute($name,regex-group(2),$remoteMappers)"/>
                <xsl:if test="not(empty(regex-group(3)))">
                    <xsl:call-template name="mapping:handleAttribute">
                        <xsl:with-param name="name" select="$name"/>
                        <xsl:with-param name="in" select="regex-group(3)"/>
                        <xsl:with-param name="remoteMappers" select="$remoteMappers"/>
                    </xsl:call-template>
                </xsl:if>
            </xsl:matching-substring>
            <xsl:non-matching-substring>
                <xsl:sequence select="mapping:mapAttribute($name,$in,$remoteMappers)"/>
            </xsl:non-matching-substring>
        </xsl:analyze-string>
    </xsl:template>
    
    <xsl:template match="mapping:attribute" mode="genLocal" priority="15">
        <xslout:value-of select="{mapping:attribute(@name)}"/>
    </xsl:template>
    
    <xsl:template match="mapping:attributes[not(@whitelist) and not(@blacklist)]" mode="genLocal" priority="15">
        <xslout:value-of select="{mapping:attributes(@name)}" separator=" "/>
    </xsl:template>
    
    <xsl:template match="mapping:attributes[@whitelist]" mode="genLocal" priority="15">
        <xslout:value-of select="for $i in {mapping:attributes(@name)} return if ($i = {mapping:quotedList(@whitelist)}) $i else ()" separator=" "/>
    </xsl:template>
    
    <xsl:template match="mapping:attributes[@blacklist]" mode="genLocal" priority="15">
        <xslout:value-of select="for $i in {mapping:attributes(@name)} return if ($i = {mapping:quotedList(@blacklist)}) () else $i" separator=" "/>
    </xsl:template>
    
    
    <xsl:template match="mapping:assertion" mode="genLocal" priority="15">
        <xslout:value-of select="{@path}"/>
    </xsl:template>
    
    <xsl:template match="mapping:assertions[not(@whitelist) and not(@blacklist)]" mode="genLocal" priority="15">
        <xslout:value-of select="{@path}" separator=" "/>
    </xsl:template>
    
    <xsl:template match="mapping:assertions[@whitelist]" mode="genLocal" priority="15">
        <xslout:value-of select="for $i in {@path} return if ($i = {mapping:quotedList(@whitelist)}) $i else ()" separator=" "/>
    </xsl:template>
    
    <xsl:template match="mapping:assertions[@blacklist]" mode="genLocal" priority="15">
        <xslout:value-of select="for $i in {@path} return if ($i = {mapping:quotedList(@blacklist)}) () else $i" separator=" "/>
    </xsl:template>
    
    <xsl:function name="mapping:mapAttribute" as="node()*">
        <xsl:param name="name" as="xs:string"/>
        <xsl:param name="part" as="xs:string"/>
        <xsl:param name="remoteMappers" as="node()*"/>
        
        <xsl:choose>
            <xsl:when test="$part=''"/>
            <xsl:when test="not(contains($part,'{'))">
                <xslout:text><xsl:value-of select="$part"/></xslout:text>
            </xsl:when>
            <xsl:when test="$part='{D}'">
                <xsl:sequence select="mapping:defaultForName($name)"/>
            </xsl:when>
            <xsl:when test="matches($part,'\{[0-9]*\}')">
                <xsl:sequence select="mapping:attributeByNumber($name, $part, $remoteMappers)"/>
            </xsl:when>
        </xsl:choose>
    </xsl:function>
    
    <xsl:function name="mapping:defaultForName" as="node()*">
        <xsl:param name="name" as="xs:string"/>
        <xsl:choose>
            <xsl:when test="$name='name'"><xslout:value-of select="/saml2:Assertion/saml2:Subject/saml2:NameID"/></xsl:when>
            <xsl:when test="$name='expire'"><xslout:value-of select="/saml2:Assertion/saml2:Subject/saml2:SubjectConfirmation/SubjectConfirmationData/@NotOnOrAfter"/></xsl:when>
            <xsl:when test="$name='email'"><xslout:value-of select="{mapping:attribute('email')}"/></xsl:when>
            <xsl:when test="$name='id'"><xslout:value-of select="{mapping:attribute('domain')}"/></xsl:when>
            <xsl:when test="$name='names'"><xslout:value-of select="{mapping:attributes('names')}" separator=" "/></xsl:when>
        </xsl:choose>
    </xsl:function>
    
    <xsl:function name="mapping:attributeByNumber" as="node()*">
        <xsl:param name="name" as="xs:string"/>
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
        <xsl:value-of select="concat('/saml2:Assertion/saml2:AttributeStatement/saml2:Attribute[@name=',mapping:quote($name),']/saml2:AttributeValue[1]')"/>
    </xsl:function>
    
    <xsl:function name="mapping:attributes" as="xs:string">
        <xsl:param name="name" as="xs:string"/>
        <xsl:value-of select="concat('/saml2:Assertion/saml2:AttributeStatement/saml2:Attribute[@name=',mapping:quote($name),']/saml2:AttributeValue')"/>
    </xsl:function>
    
    <!-- fireConditions mode these templates create conditions for notAnyOf and anyOneOf -->
    
    <xsl:template match="mapping:attributes[@notAnyOf and not(xs:boolean(@regex))]" mode="fireConditions">
        <xslout:when test="some $attr in /saml2:Assertion/saml2:AttributeStatement/saml2:Attribute[@name='{@name}']/saml2:AttributeValue satisfies $attr = {mapping:quotedList(@notAnyOf)}"/>
    </xsl:template>
    
    <xsl:template match="mapping:attributes[@anyOneOf and not(xs:boolean(@regex))]" mode="fireConditions">
        <xslout:when test="every $attr in /saml2:Assertion/saml2:AttributeStatement/saml2:Attribute[@name='{@name}']/saml2:AttributeValue satisfies not($attr = {mapping:quotedList(@anyOneOf)})"/>
    </xsl:template>
    
    <xsl:template match="mapping:attributes[@notAnyOf and xs:boolean(@regex)]" mode="fireConditions">
        <xslout:when test="some $attr in /saml2:Assertion/saml2:AttributeStatement/saml2:Attribute[@name='{@name}']/saml2:AttributeValue satisfies matches($attr, {mapping:quote(@notAnyOf)})"/>
    </xsl:template>
    
    <xsl:template match="mapping:attributes[@anyOneOf and xs:boolean(@regex)]" mode="fireConditions">
        <xslout:when test="every $attr in /saml2:Assertion/saml2:AttributeStatement/saml2:Attribute[@name='{@name}']/saml2:AttributeValue satisfies not(matches($attr, {mapping:quote(@anyOneOf)}))"/>
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