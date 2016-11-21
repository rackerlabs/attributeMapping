<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:mapping="http://docs.rackspace.com/identity/api/ext/MappingRules"
    xmlns="http://docs.rackspace.com/identity/api/ext/MappingRules"
    version="2.0"
    exclude-result-prefixes="mapping">
    
    <xsl:output indent="yes" method="xml" encoding="UTF-8"></xsl:output>
    
    <xsl:template match="mapping:rules">
        <xsl:copy>
            <xsl:namespace name="xs" select="'http://www.w3.org/2001/XMLSchema'"/>
            <xsl:namespace name="xsi" select="'http://www.w3.org/2001/XMLSchema-instance'"/>
            <xsl:apply-templates />
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="mapping:local">
        <xsl:copy>
            <user>
                <xsl:if test="exists(mapping:user/@name)">
                    <name value="{mapping:user/@name}"/>
                </xsl:if>
                <xsl:if test="exists(mapping:user/@email)">
                    <email value="{mapping:user/@email}"/>
                </xsl:if>
                <xsl:if test="exists(mapping:user/@expire)">
                    <expire value="{mapping:user/@expire}"/>
                </xsl:if>
                <xsl:if test="exists(mapping:user/@expireAfter)">
                    <expire value="{mapping:user/@expireAfter}"/>
                </xsl:if>
                <xsl:if test="exists(mapping:domain/@id)">
                    <domain value="{mapping:domain/@id}"/>
                </xsl:if>
                <xsl:if test="exists(mapping:role/@names)">
                    <roles value="{mapping:role/@names}"/>
                </xsl:if>
            </user>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="mapping:user | mapping:domain | mapping:role"/>
    
    <xsl:template match="mapping:assertion | mapping:attribute | 
                         mapping:assertions[@notAnyOf or @anyOneOf] |
                         mapping:attributes[@notAnyOf or @anyOneOf]">
        <attribute>
            <xsl:apply-templates select="@*"></xsl:apply-templates>
        </attribute>
    </xsl:template>
    
    <xsl:template match="mapping:assertions | mapping:attributes">
        <attribute multiValue="true">
            <xsl:apply-templates select="@*"></xsl:apply-templates>
        </attribute>
    </xsl:template>
    
    <xsl:template match="node() | @*">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
    
</xsl:stylesheet>