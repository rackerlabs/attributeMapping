<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:xslout="http://www.rackspace.com/repose/wadl/checker/Transform"
    xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
    xmlns:mapping="http://docs.rackspace.com/identity/api/ext/MappingRules"
    xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    exclude-result-prefixes="xs"
    version="2.0">
    
    <xsl:namespace-alias stylesheet-prefix="xslout" result-prefix="xsl"/>
 
    <xsl:output method="xml" indent="yes" />
    
    <xsl:template match="/">
        <xsl:comment>
            -                                                       -
            -  THIS IS A GENERATED TRANSFORM  DON'T EDIT BY HAND    -
            -                                                       -
        </xsl:comment>
        <xslout:transform version="2.0" xmlns="http://docs.rackspace.com/identity/api/ext/MappingRules">
            <xsl:copy-of select="/saml2p:Response/namespace::*"/>
            <xslout:output method="xml" indent="yes"/>
            <xslout:variable name="assert"  select="/" as="node()"/>
            <xslout:template match="/">
                <xslout:choose>
                   <xsl:apply-templates />
                    <xslout:otherwise><mapping:success/></xslout:otherwise>
                </xslout:choose>
            </xslout:template>
            <xslout:function name="mapping:get-attributes" as="xs:string*">
                <xslout:param name="name" as="xs:string"/>
                <xslout:sequence
                    select="$assert//saml2:Assertion[1]/saml2:AttributeStatement/saml2:Attribute[@Name=$name]/saml2:AttributeValue"/>
            </xslout:function>
            <xslout:function name="mapping:get-attribute" as="xs:string">
                <xslout:param name="name" as="xs:string"/>
                <xslout:sequence select="mapping:get-attributes($name)[1]"/>
            </xslout:function>
        </xslout:transform>
    </xsl:template>
    
    <xsl:template match="processing-instruction()[name()='assert']">
        <xslout:when test="not({string(.)})">
            <mapping:fail assertion="{string(.)}">
                <mapping:message>
                <xsl:if test="exists(preceding-sibling::comment())">
                    <xsl:value-of select="preceding-sibling::comment()[1]"/>
                </xsl:if>
                </mapping:message>
                <mapping:onAssertion>
                    <xslout:copy-of select="$assert"/>
                </mapping:onAssertion>
            </mapping:fail>
        </xslout:when>
    </xsl:template>
    
    <xsl:template match="text()"/>
</xsl:stylesheet>