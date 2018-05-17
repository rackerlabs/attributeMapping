<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:rstd="http://www.rackspace.com/docs/rstd"
    xmlns:mapping="http://docs.rackspace.com/identity/api/ext/MappingRules"
    exclude-result-prefixes="xs"
    version="2.0">

    <xsl:function name="rstd:getField" as="node()?">
        <xsl:param name="directive" as="node()"/>
        <xsl:param name="name" as="xs:string"/>
        <xsl:sequence select="$directive/rstd:field[lower-case(@name) = $name]"/>
    </xsl:function>

    <xsl:function name="rstd:getFieldAsBool" as="xs:boolean">
        <xsl:param name="directive" as="node()"/>
        <xsl:param name="name" as="xs:string"/>
        <xsl:variable name="field" as="node()?" select="rstd:getField($directive,$name)"/>
        <xsl:sequence select="empty($field) or xs:boolean(string($field))"/>
    </xsl:function>

    <xsl:template name="rstd:directive-fail">
        <xsl:param name="msg" as="xs:string"/>
        <xsl:message terminate="yes">Error in <xsl:value-of select="@type"/> directive. <xsl:value-of
        select="concat($msg,' ',@source,':',@line)"/></xsl:message>
    </xsl:template>

</xsl:stylesheet>
