<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:rstd="http://www.rackspace.com/docs/rstd"
    xmlns:mapping="http://docs.rackspace.com/identity/api/ext/MappingRules"
    exclude-result-prefixes="xs"
    version="2.0">

    <xsl:import href="util.xsl"/>

    <xsl:output  indent="no"/>

    <xsl:variable name="pathToMappingTests">../../../../core/src/test/resources/tests/mapping-tests/</xsl:variable>
    <xsl:variable name="pathToMappingResults">../../../core/target/map-results/</xsl:variable>

    <xsl:variable name="defaultAttributes" as="xs:string*" select="('name','email','expire','domain','roles')"/>

    <!-- Copy Everything -->
    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="rstd:directive[lower-case(@type)='attribmap']">
        <xsl:variable name="testCase" as="xs:string?" select="normalize-space(rstd:content[1])"/>
        <xsl:variable name="showSAML" as="xs:boolean" select="rstd:getFieldAsBool(., 'saml-show')"/>
        <xsl:variable name="showMap" as="xs:boolean" select="rstd:getFieldAsBool(., 'map-show')"/>
        <xsl:variable name="showResults" as="xs:boolean" select="rstd:getFieldAsBool(., 'results-show')"/>
        <xsl:variable name="saml" as="node()?" select="rstd:getField(., 'saml')"/>
        <xsl:variable name="map" as="node()?" select="rstd:getField(., 'map')"/>
        <xsl:if test="$testCase = ''">
            <xsl:call-template name="rstd:directive-fail">
                <xsl:with-param name="msg">Missing test case name.</xsl:with-param>
            </xsl:call-template>
        </xsl:if>
        <xsl:if test="empty($saml)">
            <xsl:call-template name="rstd:directive-fail">
                <xsl:with-param name="msg">Missing required saml field.</xsl:with-param>
            </xsl:call-template>
        </xsl:if>
        <xsl:if test="empty($map)">
            <xsl:call-template name="rstd:directive-fail">
                <xsl:with-param name="msg">Missing required map field.</xsl:with-param>
            </xsl:call-template>
        </xsl:if>
        <xsl:if test="$showSAML">
            <xsl:call-template name="rstd:outputSample">
                <xsl:with-param name="sample" select="concat(' ../',$pathToMappingResults,$testCase,'/',$saml,'/',$saml)"/>
                <xsl:with-param name="type">saml</xsl:with-param>
            </xsl:call-template>
        </xsl:if>
        <xsl:if test="$showMap">
            <xsl:call-template name="rstd:outputSample">
                <xsl:with-param name="sample" select="concat(' ',$pathToMappingTests,$testCase,'/maps/',$map)"/>
                <xsl:with-param name="type">map</xsl:with-param>
            </xsl:call-template>
        </xsl:if>
        <xsl:if test="$showResults">
            <xsl:call-template name="rstd:outputResults">
                <xsl:with-param name="testCase" select="$testCase"/>
                <xsl:with-param name="saml" select="$saml"/>
                <xsl:with-param name="map" select="$map"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>


    <xsl:template name="rstd:outputResults">
        <xsl:param name="testCase" as="xs:string"/>
        <xsl:param name="saml" as="xs:string"/>
        <xsl:param name="map" as="xs:string"/>
        <xsl:variable name="caption"   as="node()?" select="rstd:getField(.,'results-caption')"/>
        <xsl:variable name="resultName" as="xs:string"
                      select="if (not(ends-with($map,'.xml'))) then concat($map,'.xml') else $map"/>
        <xsl:variable name="result" as="node()"
                      select="doc(concat($pathToMappingResults,'/',$testCase,'/',$saml,'/',$resultName))"/>
        <xsl:variable name="rows" as="node()*">
            <xsl:apply-templates select="$result" mode="result"/>
        </xsl:variable>
        <paragraph>
            <xsl:choose>
                <xsl:when test="exists($caption)">
                    <xsl:value-of select="$caption"/>
                </xsl:when>
                <xsl:otherwise>Resulting Attributes:</xsl:otherwise>
            </xsl:choose>
        </paragraph>
        <table>
            <tgroup cols="2">
                <colspec colwidth="{max(for $p in $rows/entry[1]/paragraph return string-length($p)+2)}"/>
                <colspec colwidth="{max(for $p in $rows/entry[2]/paragraph return string-length($p)+2)}"/>
                <tbody>
                    <!--
                        Make sure we get the standard rows in the
                        right order.
                    -->
                    <xsl:copy-of select="$rows[entry[1]='domain']"/>
                    <xsl:copy-of select="$rows[entry[1]='name']"/>
                    <xsl:copy-of select="$rows[entry[1]='email']"/>
                    <xsl:copy-of select="$rows[entry[1]='roles']"/>
                    <xsl:copy-of select="$rows[entry[1]='expire']"/>
                    <!--
                        Then copy all extended attributes in the order in
                        which they naturally appear.
                    -->
                    <xsl:copy-of select="$rows[not(entry[1]=('domain','name','email','roles','expire'))]"/>
                </tbody>
            </tgroup>
        </table>
    </xsl:template>

    <xsl:template match="mapping:*[@value]" mode="result">
        <row>
            <entry>
                <xsl:call-template name="rstd:attributeName"/>
            </entry>
            <entry>
                <xsl:call-template name="rstd:attributeValue"/>
            </entry>
        </row>
    </xsl:template>

    <xsl:template match="text()" mode="result"/>

    <xsl:function name="rstd:name" as="xs:string">
        <xsl:param name="in" as="node()"/>
        <xsl:choose>
            <xsl:when test="$in/@name"><xsl:value-of select="$in/@name"/></xsl:when>
            <xsl:otherwise><xsl:value-of select="name($in)"/></xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="rstd:isStandardAttrib" as="xs:boolean">
        <xsl:param name="in" as="node()"/>
        <xsl:sequence select="(local-name($in/..)='user') and
                              (namespace-uri($in/..)='http://docs.rackspace.com/identity/api/ext/MappingRules') and
                              (local-name($in) = $defaultAttributes)"/>
    </xsl:function>

    <xsl:template name="rstd:attributeName">
        <xsl:variable name="name" as="xs:string" select="rstd:name(.)"/>
        <xsl:variable name="prefix" as="xs:string?">
            <xsl:choose>
                <xsl:when test="not(rstd:isStandardAttrib(.))">
                    <xsl:value-of select="concat(rstd:name(..),'/')"/>
                </xsl:when>
                <!-- no prefix -->
                <xsl:otherwise></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <paragraph><xsl:value-of select="concat($prefix,$name)"/></paragraph>
    </xsl:template>

    <xsl:template name="rstd:attributeValue">
        <xsl:variable name="isMultiValue" as="xs:boolean"
                      select="(rstd:isStandardAttrib(.) and rstd:name(.)='roles') or (@multiValue and xs:boolean(@multiValue))"/>
        <xsl:variable name="rawValue" as="xs:string" select="@value"/>
        <xsl:choose>
            <xsl:when test="$isMultiValue">
                <xsl:variable name="values" as="xs:string*" select="tokenize($rawValue,' ')"/>
                <bullet_list bullet="-">
                    <xsl:for-each select="$values">
                        <list_item>
                            <paragraph><xsl:value-of select="."/></paragraph>
                        </list_item>
                    </xsl:for-each>
                </bullet_list>
            </xsl:when>
            <xsl:otherwise>
                <paragraph><xsl:value-of select="$rawValue"/></paragraph>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="rstd:outputSample">
        <xsl:param name="sample" as="xs:string"/>
        <xsl:param name="type" as="xs:string"/>
        <xsl:param name="format" as="xs:string">
            <xsl:choose>
                <xsl:when test="ends-with($sample, '.xml')">xml</xsl:when>
                <xsl:when test="ends-with($sample, '.yaml')">yaml</xsl:when>
                <xsl:when test="ends-with($sample, '.json')">json</xsl:when>
                <xsl:otherwise>
                    <xsl:message>[WARNING] Could not determine sample format. Assuming xml!</xsl:message>
                    <xsl:value-of select="xml"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:param>
        <xsl:variable name="caption"   as="node()?" select="rstd:getField(.,concat($type,'-caption'))"/>
        <xsl:variable name="emphasize" as="node()?" select="rstd:getField(.,concat($type,'-emphasize-lines'))"/>
        <rstd:directive type="highlight">
            <rstd:content> <xsl:value-of select="$format"/></rstd:content>
            <rstd:field name="linenothreshold">5</rstd:field>
        </rstd:directive>
        <rstd:directive type="literalinclude">
            <rstd:content> <xsl:value-of select="$sample"/></rstd:content>
            <rstd:field name="linenos" />
            <xsl:if test="not(empty($caption))">
                <rstd:field name="caption"><xsl:value-of select="$caption"/></rstd:field>
            </xsl:if>
            <xsl:if test="not(empty($emphasize))">
                <rstd:field name="emphasize-lines"><xsl:value-of select="$emphasize"/></rstd:field>
            </xsl:if>
        </rstd:directive>
    </xsl:template>
</xsl:stylesheet>
