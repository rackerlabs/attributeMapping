<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:rstd="http://www.rackspace.com/docs/rstd"
    xmlns:mapping="http://docs.rackspace.com/identity/api/ext/MappingRules"
    exclude-result-prefixes="xs"
    version="2.0">

    <xsl:import href="util.xsl"/>

    <xsl:output  indent="no"/>

    <!-- Copy Everything -->
    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="rstd:directive[lower-case(@type)=('map','saml')]">
        <xsl:variable name="type" as="xs:string" select="lower-case(@type)"/>
        <xsl:variable name="otherType" as="xs:string" select="if ($type='map') then 'saml' else 'map'"/>
        <xsl:variable name="args" as="xs:string?" select="normalize-space(rstd:content[1])"/>
        <xsl:variable name="testCase" as="xs:string?" select="substring-before($args,'/')"/>
        <xsl:variable name="sample" as="xs:string?" select="substring-after($args,'/')"/>
        <xsl:variable name="caption"   as="node()?" select="rstd:getField(.,'caption')"/>
        <xsl:variable name="emphasize" as="node()?" select="rstd:getField(.,'emphasize-lines')"/>

        <xsl:if test="$testCase = ''">
            <xsl:call-template name="rstd:directive-fail">
                <xsl:with-param name="msg">Missing test case name.</xsl:with-param>
            </xsl:call-template>
        </xsl:if>
        <xsl:if test="$sample = ''">
            <xsl:call-template name="rstd:directive-fail">
                <xsl:with-param name="msg">Missing <xsl:value-of select="$type"/> sample.</xsl:with-param>
            </xsl:call-template>
        </xsl:if>

        <rstd:directive type="attribmap">
            <rstd:content><xsl:value-of select="$testCase"/></rstd:content>
            <rstd:field name="{$type}"><xsl:value-of select="$sample"/></rstd:field>
            <xsl:if test="exists($caption)">
                <rstd:field name="{$type}-caption"><xsl:value-of select="$caption"/></rstd:field>
            </xsl:if>
            <xsl:if test="exists($emphasize)">
                <rstd:field name="{$type}-emphasize-lines"><xsl:value-of select="$emphasize"/></rstd:field>
            </xsl:if>
            <!-- fake other, won't be rendered because show is false-->
            <rstd:field name="{$otherType}">fake.json</rstd:field>
            <rstd:field name="{$otherType}-show">false</rstd:field>
            <rstd:field name="results-show">false</rstd:field>
        </rstd:directive>
    </xsl:template>

</xsl:stylesheet>
