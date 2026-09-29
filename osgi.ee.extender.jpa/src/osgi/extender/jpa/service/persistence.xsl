<?xml version="1.0"?>
<!--
  Transforms a persistence.xml into a simple list of unit definitions. The persistence schema
  lives in a different namespace depending on its version:
  - http://java.sun.com/xml/ns/persistence   (JPA 1.0, 2.0)
  - http://xmlns.jcp.org/xml/ns/persistence  (JPA 2.1, 2.2)
  - https://jakarta.ee/xml/ns/persistence    (Jakarta Persistence 3.x)
  All of them are accepted. The elements used here are the same in all versions.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" indent="yes"/>

  <xsl:template match="/">
    <units>
      <xsl:apply-templates select="*[local-name() = 'persistence' and
          (namespace-uri() = 'http://java.sun.com/xml/ns/persistence' or
           namespace-uri() = 'http://xmlns.jcp.org/xml/ns/persistence' or
           namespace-uri() = 'https://jakarta.ee/xml/ns/persistence')]/*[local-name() = 'persistence-unit']"/>
    </units>
  </xsl:template>

  <!-- Children of a unit are always in the namespace of the unit itself. -->
  <xsl:template match="*[local-name() = 'persistence-unit']">
    <xsl:variable name="ns" select="namespace-uri()"/>
    <object class="osgi.extender.jpa.service.PersistenceUnitDefinition">
      <name><xsl:value-of select="@name"/></name>
      <version><xsl:value-of select="parent::*/@version"/></version>
      <transactionType><xsl:value-of select="@transaction-type"/></transactionType>
      <description><xsl:value-of select="*[local-name() = 'description' and namespace-uri() = $ns]"/></description>
      <provider><xsl:value-of select="normalize-space(*[local-name() = 'provider' and namespace-uri() = $ns])"/></provider>
      <jtaDs><xsl:value-of select="normalize-space(*[local-name() = 'jta-data-source' and namespace-uri() = $ns])"/></jtaDs>
      <nonJtaDs><xsl:value-of select="normalize-space(*[local-name() = 'non-jta-data-source' and namespace-uri() = $ns])"/></nonJtaDs>
      <mappingFiles>
        <xsl:apply-templates select="*[local-name() = 'mapping-file' and namespace-uri() = $ns]" mode="string"/>
      </mappingFiles>
      <jarFiles>
        <xsl:apply-templates select="*[local-name() = 'jar-file' and namespace-uri() = $ns]" mode="string"/>
      </jarFiles>
      <classes>
        <xsl:apply-templates select="*[local-name() = 'class' and namespace-uri() = $ns]" mode="string"/>
      </classes>
      <!-- The schema default of the element is true: an empty element means "exclude".
           An absent element means that unlisted classes are included. -->
      <xsl:variable name="exclude" select="*[local-name() = 'exclude-unlisted-classes' and namespace-uri() = $ns]"/>
      <excludeUnlisted>
        <xsl:choose>
          <xsl:when test="not($exclude)">false</xsl:when>
          <xsl:when test="normalize-space($exclude) = ''">true</xsl:when>
          <xsl:otherwise><xsl:value-of select="normalize-space($exclude)"/></xsl:otherwise>
        </xsl:choose>
      </excludeUnlisted>
      <scope><xsl:value-of select="normalize-space(*[local-name() = 'scope' and namespace-uri() = $ns])"/></scope>
      <qualifiers>
        <xsl:apply-templates select="*[local-name() = 'qualifier' and namespace-uri() = $ns]" mode="string"/>
      </qualifiers>
      <cachingType><xsl:value-of select="normalize-space(*[local-name() = 'shared-cache-mode' and namespace-uri() = $ns])"/></cachingType>
      <validationMode><xsl:value-of select="normalize-space(*[local-name() = 'validation-mode' and namespace-uri() = $ns])"/></validationMode>
      <properties>
        <xsl:for-each select="*[local-name() = 'properties' and namespace-uri() = $ns]/*[local-name() = 'property' and namespace-uri() = $ns]">
          <object class="osgi.extender.jpa.service.Property">
            <key><xsl:value-of select="@name"/></key>
            <value><xsl:value-of select="@value"/></value>
          </object>
        </xsl:for-each>
      </properties>
    </object>
  </xsl:template>

  <xsl:template match="*" mode="string">
    <string><xsl:value-of select="normalize-space(.)"/></string>
  </xsl:template>
</xsl:stylesheet>
