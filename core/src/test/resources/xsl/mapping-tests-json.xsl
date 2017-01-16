<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:xslout="http://www.rackspace.com/repose/wadl/checker/Transform"
    xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
    xmlns:mapping="http://docs.rackspace.com/identity/api/ext/MappingRules"
    xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    version="2.0">


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
        if (not(<xsl:value-of select="."/>)) then
        &lt;mapping:fail assertion="<xsl:value-of select="mapping:unquote(.)"/>"&gt;
          &lt;mapping:message&gt;
            <xsl:if test="exists(preceding-sibling::comment())">
                <xsl:value-of select="preceding-sibling::comment()[1]"/>
            </xsl:if>
          &lt;/mapping:message&gt;
          &lt;mapping:onJSON&gt;
              {$__JSON__}
          &lt;/mapping:onJSON&gt;
        &lt;/mapping:fail&gt;
        else
    </xsl:template>
    <xsl:template match="text()"/>
    <xsl:function name="mapping:unquote" as="xs:string">
        <xsl:param name="in" as="xs:string"/>
        <xsl:variable name="quote" as="xs:string">"</xsl:variable>
        <xsl:variable name="squote" as="xs:string">'</xsl:variable>
        <xsl:value-of select="replace ($in, $quote, $squote)"/>
    </xsl:function>
</xsl:stylesheet>
