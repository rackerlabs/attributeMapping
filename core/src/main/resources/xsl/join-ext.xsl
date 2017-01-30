<?xml version="1.0" encoding="UTF-8"?>
<!--
   join-ext.xsl

   This stylesheet is responsible for joining an RAX-AUTH:extendedAttributes
   element into an access response.  The RAX-AUTH:extendedAttributes element
   is passed as the extAttributes parameter.

   Copyright 2017 Rackspace US, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:id="http://docs.openstack.org/identity/api/v2.0"
    xmlns:RAX-AUTH="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"
    xmlns="http://docs.openstack.org/identity/api/v2.0"
    exclude-result-prefixes="xs id"
    version="2.0">

    <xsl:param name="extAttributes" as="node()"/>

    <xsl:output indent="yes"/>

    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="id:access">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
            <xsl:if test="exists($extAttributes/RAX-AUTH:extendedAttributes/RAX-AUTH:group)">
                <xsl:copy-of select="$extAttributes"/>
            </xsl:if>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
