<?xml version="1.0" encoding="UTF-8"?>
<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:xslout="http://www.rackspace.com/repose/wadl/checker/Transform"
    xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
    xmlns:mapping="http://docs.rackspace.com/identity/api/ext/MappingRules"
    exclude-result-prefixes="xs mapping"
    version="2.0">
    
    <xsl:namespace-alias stylesheet-prefix="xslout" result-prefix="xsl"/>
    <xsl:output method="xml" encoding="UTF-8" indent="yes"/>
    
    <xsl:template match="/">
        <xsl:comment>
            -                                                       -
            -  THIS IS A GENERATED TRANSFORM  DON'T EDIT BY HAND    -
            -                                                       -
        </xsl:comment>
        <xslout:transform version="2.0">
            <xslout:variable name="assertion" as="node()" select="/"/>
            
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
                <xsl:apply-templates select="mapping:remote" mode="fireConditions" />
            </xslout:choose>
        </xslout:template>
    </xsl:template>
    
    
    <xsl:template match="mapping:attributes[@notAnyOf and not(xs:boolean(@regex))]" mode="fireConditions">
        <xslout:when test="some $attr in /saml2:Assertion/saml2:AttributeStatement/saml2:Attribute[@name={@name}]/saml2:AttributeValue satisfies $attr = tokenize(@notAnyOf,' ')"/>
    </xsl:template>
    
    <xsl:template match="mapping:attributes[@anyOneOf and not(xs:boolean(@regex))]" mode="fireConditions">
        <xslout:when test="every $attr in /saml2:Assertion/saml2:AttributeStatement/saml2:Attribute[@name={@name}]/saml2:AttributeValue satisfies not($attr = tokenize(@notAnyOf,' '))"/>
    </xsl:template>
    
</xsl:transform>