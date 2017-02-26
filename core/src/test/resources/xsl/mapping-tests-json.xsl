<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:xslout="http://www.rackspace.com/repose/wadl/checker/Transform"
    xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
    xmlns:mapping="http://docs.rackspace.com/identity/api/ext/MappingRules"
    xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    version="2.0">

    <xsl:param name="base" as="xs:anyURI"/>

    <xsl:output method="text" />

    <xsl:template match="/">
        (:
          THIS IS A GENERATED QUERY  DON'T EDIT BY HAND
        :)

        xquery version "3.1" encoding "UTF-8";

        declare namespace output = "http://www.w3.org/2010/xslt-xquery-serialization";
        declare namespace map = "http://www.w3.org/2005/xpath-functions/map";
        declare namespace array = "http://www.w3.org/2005/xpath-functions/array";
        declare namespace mapping = "http://docs.rackspace.com/identity/api/ext/MappingRules";
        <xsl:for-each select="/saml2p:Response/namespace::*">
        <xsl:if test="name(.) != 'xml'">declare namespace <xsl:value-of select="name(.)"/> = "<xsl:value-of select="."/>";
        </xsl:if>
        </xsl:for-each>

        declare default element namespace "http://docs.rackspace.com/identity/api/ext/MappingRules";

        declare variable $__JSON__ external;
        declare variable $_ := parse-json($__JSON__);

        <xsl:apply-templates />
          &lt;mapping:success/&gt;
    </xsl:template>
    <xsl:template match="processing-instruction()[name()='json-assert']">
        <xsl:call-template name="failAssert">
            <xsl:with-param name="testAssertion" select="."/>
            <xsl:with-param name="message" select="preceding-sibling::comment()[1]"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="mapping:json-assert">
        <xsl:call-template name="failAssert">
            <xsl:with-param name="testAssertion" select="@test"/>
            <xsl:with-param name="message" select="normalize-space(.)"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="processing-instruction()[name()='include']">
        <xsl:variable name="includeURL" as="xs:string" select="normalize-space(string(.))"/>
        <xsl:variable name="urlParts" as="xs:string*"  select="tokenize($includeURL,'#')"/>
        <xsl:variable name="assertURL" as="xs:anyURI" select="resolve-uri($urlParts[1], $base)"/>
        <xsl:variable name="groupName" as="xs:string?" select="$urlParts[2]"/>

        <xsl:variable name="commonAsserts" as="node()" select="doc($assertURL)"/>
        <xsl:choose>
            <xsl:when test="not(empty($groupName))">
                <xsl:choose>
                    <xsl:when test="$commonAsserts/mapping:common-assertions/mapping:assert-group[@name=$groupName]">
                        <xsl:apply-templates select="$commonAsserts/mapping:common-assertions/mapping:assert-group[@name=$groupName]/mapping:json-assert"/>
                    </xsl:when>
                    <xsl:otherwise>
                        if (true()) then
                           &lt;mapping:fail assertion=""&gt;
                             &lt;mapping:message&gt;
                               Error in test: the assertion group '<xsl:value-of select="$groupName"/>' is not found in
                               <xsl:value-of select="$assertURL"/>
                             &lt;/mapping:message&gt;
                             &lt;mapping:onJSON&gt;
                                null
                             &lt;/mapping:onJSON&gt;
                            &lt;/mapping:fail&gt;
                         else
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="$commonAsserts//mapping:json-assert"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="text()"/>
    <xsl:function name="mapping:unquote" as="xs:string">
        <xsl:param name="in" as="xs:string"/>
        <xsl:variable name="quote" as="xs:string">"</xsl:variable>
        <xsl:variable name="squote" as="xs:string">'</xsl:variable>
        <xsl:value-of select="replace ($in, $quote, $squote)"/>
    </xsl:function>
    <xsl:template name="failAssert">
        <xsl:param name="testAssertion" as="xs:string"/>
        <xsl:param name="message" as="xs:string?"/>
        if (not(<xsl:value-of select="$testAssertion"/>)) then
        &lt;mapping:fail assertion="<xsl:value-of select="mapping:unquote($testAssertion)"/>"&gt;
          &lt;mapping:message&gt;
            <xsl:if test="not(empty($message))">
                <xsl:value-of select="$message"/>
            </xsl:if>
          &lt;/mapping:message&gt;
          &lt;mapping:onJSON&gt;
              {$__JSON__}
          &lt;/mapping:onJSON&gt;
        &lt;/mapping:fail&gt;
        else
    </xsl:template>
</xsl:stylesheet>
