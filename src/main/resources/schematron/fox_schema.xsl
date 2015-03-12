<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xsl:stylesheet xmlns:fm="http://www.og.dti.gov/fox_module" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" fm:dummy-for-xmlns="" xs:dummy-for-xmlns="" version="2.0"><!--Implementers: please note that overriding process-prolog or process-root is 
    the preferred method for meta-stylesheets to use where possible. The name or details of 
    this mode may change during 1Q 2007.-->


<!--PHASES-->


<!--PROLOG-->


<!--KEYS-->


<!--DEFAULT RULES-->


<!--MODE: SCHEMATRON-FULL-PATH-->
<!--This mode can be used to generate an ugly though full XPath for locators-->
<xsl:template match="*" mode="schematron-get-full-path">
		<xsl:apply-templates select="parent::*" mode="schematron-get-full-path"/>
		<xsl:text>/</xsl:text>
		<xsl:choose>
			<xsl:when test="namespace-uri()=''">
				<xsl:value-of select="name()"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>*:</xsl:text>
				<xsl:value-of select="local-name()"/>
				<xsl:text>[namespace-uri()='</xsl:text>
				<xsl:value-of select="namespace-uri()"/>
				<xsl:text>']</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:variable name="preceding" select="count(preceding-sibling::*[local-name()=local-name(current())                                   and namespace-uri() = namespace-uri(current())])"/>
		<xsl:text>[</xsl:text>
		<xsl:value-of select="1+ $preceding"/>
		<xsl:text>]</xsl:text>
	</xsl:template><xsl:template match="@*" mode="schematron-get-full-path">
		<xsl:apply-templates select="parent::*" mode="schematron-get-full-path"/>
		<xsl:text>/</xsl:text>
		<xsl:choose>
			<xsl:when test="namespace-uri()=''">@sch:schema</xsl:when>
			<xsl:otherwise>
				<xsl:text>@*[local-name()='</xsl:text>
				<xsl:value-of select="local-name()"/>
				<xsl:text>' and namespace-uri()='</xsl:text>
				<xsl:value-of select="namespace-uri()"/>
				<xsl:text>']</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

<!--MODE: SCHEMATRON-FULL-PATH-2-->
<!--This mode can be used to generate prefixed XPath for humans-->
<xsl:template match="node() | @*" mode="schematron-get-full-path-2">
		<xsl:for-each select="ancestor-or-self::*">
			<xsl:text>/</xsl:text>
			<xsl:value-of select="name(.)"/>
			<xsl:if test="preceding-sibling::*[name(.)=name(current())]">
				<xsl:text>[</xsl:text>
				<xsl:value-of select="count(preceding-sibling::*[name(.)=name(current())])+1"/>
				<xsl:text>]</xsl:text>
			</xsl:if>
		</xsl:for-each>
		<xsl:if test="not(self::*)"><xsl:text/>/@<xsl:value-of select="name(.)"/></xsl:if>
	</xsl:template>

<!--MODE: GENERATE-ID-FROM-PATH -->
<xsl:template match="/" mode="generate-id-from-path"/><xsl:template match="text()" mode="generate-id-from-path">
		<xsl:apply-templates select="parent::*" mode="generate-id-from-path"/>
		<xsl:value-of select="concat('.text-', 1+count(preceding-sibling::text()), '-')"/>
	</xsl:template><xsl:template match="comment()" mode="generate-id-from-path">
		<xsl:apply-templates select="parent::*" mode="generate-id-from-path"/>
		<xsl:value-of select="concat('.comment-', 1+count(preceding-sibling::comment()), '-')"/>
	</xsl:template><xsl:template match="processing-instruction()" mode="generate-id-from-path">
		<xsl:apply-templates select="parent::*" mode="generate-id-from-path"/>
		<xsl:value-of select="concat('.processing-instruction-', 1+count(preceding-sibling::processing-instruction()), '-')"/>
	</xsl:template><xsl:template match="@*" mode="generate-id-from-path">
		<xsl:apply-templates select="parent::*" mode="generate-id-from-path"/>
		<xsl:value-of select="concat('.@', name())"/>
	</xsl:template><xsl:template match="*" mode="generate-id-from-path" priority="-0.5">
		<xsl:apply-templates select="parent::*" mode="generate-id-from-path"/>
		<xsl:text>.</xsl:text>
		<xsl:choose>
			<xsl:when test="count(. | ../namespace::*) = count(../namespace::*)">
				<xsl:value-of select="concat('.namespace::-',1+count(namespace::*),'-')"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="concat('.',name(),'-',1+count(preceding-sibling::*[name()=name(current())]),'-')"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

<!--MODE: GENERATE-ID-2 -->
<xsl:template match="/" mode="generate-id-2">U</xsl:template><xsl:template match="*" mode="generate-id-2" priority="2">
		<xsl:text>U</xsl:text>
		<xsl:number level="multiple" count="*"/>
	</xsl:template><xsl:template match="node()" mode="generate-id-2">
		<xsl:text>U.</xsl:text>
		<xsl:number level="multiple" count="*"/>
		<xsl:text>n</xsl:text>
		<xsl:number count="node()"/>
	</xsl:template><xsl:template match="@*" mode="generate-id-2">
		<xsl:text>U.</xsl:text>
		<xsl:number level="multiple" count="*"/>
		<xsl:text>_</xsl:text>
		<xsl:value-of select="string-length(local-name(.))"/>
		<xsl:text>_</xsl:text>
		<xsl:value-of select="translate(name(),':','.')"/>
	</xsl:template><!--Strip characters--><xsl:template match="text()" priority="-1"/>

<!--SCHEMA METADATA-->
<xsl:template match="/">
		<xsl:apply-templates select="/" mode="M9"/>
		<xsl:apply-templates select="/" mode="M10"/>
		<xsl:apply-templates select="/" mode="M11"/>
		<xsl:apply-templates select="/" mode="M12"/>
		<xsl:apply-templates select="/" mode="M13"/>
		<xsl:apply-templates select="/" mode="M14"/>
		<xsl:apply-templates select="/" mode="M15"/>
	</xsl:template>

<!--SCHEMATRON PATTERNS-->
<xsl:variable name="fox-attrs-doc" select="document('file:///C:/pvcswork/Fox/source/schematron/schema/fox.xsd')"/><xsl:variable name="set-out-attrs" select="$fox-attrs-doc/xs:schema/xs:attributeGroup[@name = 'zzz-set-out-attr-grp']/xs:attribute/@ref"/><xsl:variable name="menu-out-attrs" select="$fox-attrs-doc/xs:schema/xs:attributeGroup[@name = 'zzz-set-menu-attr-group']/xs:attribute/@ref"/><xsl:variable name="action-out-attrs" select="$fox-attrs-doc/xs:schema/xs:attributeGroup[@name = 'zzz-action-out-attr-grp']/xs:attribute/@ref"/><xsl:variable name="action-attrs" select="$fox-attrs-doc/xs:schema/xs:attributeGroup[@name = 'zzz-action-out-attr-grp' or @name = 'zzz-action-attr-grp']/xs:attribute/@ref"/><xsl:variable name="element-attrs" select="$fox-attrs-doc/xs:schema/xs:attributeGroup[@name = 'zzz-schema-element-attr-grp']/xs:attribute/@ref"/><xsl:variable name="fox-attrs" select="$fox-attrs-doc/xs:schema/xs:attribute"/>

<!--PATTERN document-load-->


	<!--RULE -->
<xsl:template match="xs:schema" priority="101" mode="M9">

		<!--ASSERT -->
<xsl:choose>
			<xsl:when test="$fox-attrs-doc"/>
			<xsl:otherwise>Cannot load fox attributes document check location specified in fox_schema.sch <xsl:value-of select="string('&#xA;')"/></xsl:otherwise>
		</xsl:choose><xsl:apply-templates select="@*|*|comment()|processing-instruction()" mode="M9"/></xsl:template><xsl:template match="text()" priority="-1" mode="M9"/><xsl:template match="@*|node()" priority="-2" mode="M9">
		<xsl:choose>
			<!--Housekeeping: SAXON warns if attempting to find the attribute
                           of an attribute-->
			<xsl:when test="not(@*)">
				<xsl:apply-templates select="node()" mode="M9"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="@*|node()" mode="M9"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

<!--PATTERN set-out-attrs-->


	<!--RULE -->
<xsl:template match="fm:set-out/@*[not(local-name() = $set-out-attrs or namespace-uri() = '')]" priority="101" mode="M10">

		<!--REPORT -->
<xsl:if test="true()">Illegal attribute(s) present in set-out - <xsl:text/><xsl:value-of select="name(.)"/><xsl:text/><xsl:value-of select="string('&#xA;')"/></xsl:if><xsl:apply-templates select="@*|*|comment()|processing-instruction()" mode="M10"/></xsl:template><xsl:template match="text()" priority="-1" mode="M10"/><xsl:template match="@*|node()" priority="-2" mode="M10">
		<xsl:choose>
			<!--Housekeeping: SAXON warns if attempting to find the attribute
                           of an attribute-->
			<xsl:when test="not(@*)">
				<xsl:apply-templates select="node()" mode="M10"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="@*|node()" mode="M10"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

<!--PATTERN menu-out-attrs-->


	<!--RULE -->
<xsl:template match="fm:menu-out/@*[not(local-name() = $menu-out-attrs or namespace-uri() = '')]" priority="101" mode="M11">

		<!--REPORT -->
<xsl:if test="true()">Illegal attribute(s) present in menu-out - <xsl:text/><xsl:value-of select="name(.)"/><xsl:text/><xsl:value-of select="string('&#xA;')"/></xsl:if><xsl:apply-templates select="@*|*|comment()|processing-instruction()" mode="M11"/></xsl:template><xsl:template match="text()" priority="-1" mode="M11"/><xsl:template match="@*|node()" priority="-2" mode="M11">
		<xsl:choose>
			<!--Housekeeping: SAXON warns if attempting to find the attribute
                           of an attribute-->
			<xsl:when test="not(@*)">
				<xsl:apply-templates select="node()" mode="M11"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="@*|node()" mode="M11"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

<!--PATTERN action-out-attrs-->


	<!--RULE -->
<xsl:template match="fm:action-out/@*[not(local-name() = $action-out-attrs or namespace-uri() = '')]" priority="101" mode="M12">

		<!--REPORT -->
<xsl:if test="true()">Illegal attribute(s) present in action-out - <xsl:text/><xsl:value-of select="name(.)"/><xsl:text/><xsl:value-of select="string('&#xA;')"/></xsl:if><xsl:apply-templates select="@*|*|comment()|processing-instruction()" mode="M12"/></xsl:template><xsl:template match="text()" priority="-1" mode="M12"/><xsl:template match="@*|node()" priority="-2" mode="M12">
		<xsl:choose>
			<!--Housekeeping: SAXON warns if attempting to find the attribute
                           of an attribute-->
			<xsl:when test="not(@*)">
				<xsl:apply-templates select="node()" mode="M12"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="@*|node()" mode="M12"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

<!--PATTERN action-attrs-->


	<!--RULE -->
<xsl:template match="fm:action/@*[not(local-name() = $action-attrs or namespace-uri() = '')]" priority="101" mode="M13">

		<!--REPORT -->
<xsl:if test="true()">Illegal attribute(s) present in action - <xsl:text/><xsl:value-of select="name(.)"/><xsl:text/><xsl:value-of select="string('&#xA;')"/></xsl:if><xsl:apply-templates select="@*|*|comment()|processing-instruction()" mode="M13"/></xsl:template><xsl:template match="text()" priority="-1" mode="M13"/><xsl:template match="@*|node()" priority="-2" mode="M13">
		<xsl:choose>
			<!--Housekeeping: SAXON warns if attempting to find the attribute
                           of an attribute-->
			<xsl:when test="not(@*)">
				<xsl:apply-templates select="node()" mode="M13"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="@*|node()" mode="M13"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

<!--PATTERN element-attrs-->


	<!--RULE -->
<xsl:template match="xs:element/@*[not(local-name() = $element-attrs or namespace-uri() = '')]" priority="101" mode="M14">

		<!--REPORT -->
<xsl:if test="true()">Illegal attribute(s) present in element - <xsl:text/><xsl:value-of select="name(.)"/><xsl:text/><xsl:value-of select="string('&#xA;')"/></xsl:if><xsl:apply-templates select="@*|*|comment()|processing-instruction()" mode="M14"/></xsl:template><xsl:template match="text()" priority="-1" mode="M14"/><xsl:template match="@*|node()" priority="-2" mode="M14">
		<xsl:choose>
			<!--Housekeeping: SAXON warns if attempting to find the attribute
                           of an attribute-->
			<xsl:when test="not(@*)">
				<xsl:apply-templates select="node()" mode="M14"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="@*|node()" mode="M14"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

<!--PATTERN attr-types-->


	<!--RULE -->
<xsl:template match="@*[local-name() = $fox-attrs/@name and namespace-uri() != '']" priority="101" mode="M15"><xsl:variable name="attr" select="."/><xsl:variable name="fox-attr" select="$fox-attrs[@name = $attr/local-name()]"/>

		<!--REPORT -->
<xsl:if test="$fox-attr/@type = 'xs:integer' and not(. castable as xs:integer)">Illegal value for <xsl:text/><xsl:value-of select="name(.)"/><xsl:text/> - <xsl:text/><xsl:value-of select="."/><xsl:text/>, expecting <xsl:text/><xsl:value-of select="$fox-attr/@type"/><xsl:text/><xsl:value-of select="string('&#xA;')"/></xsl:if>

		<!--REPORT -->
<xsl:if test="$fox-attr/@type = 'xs:positiveInteger' and not(. castable as xs:integer and . &gt; 0)">Illegal value for <xsl:text/><xsl:value-of select="name(.)"/><xsl:text/> - <xsl:text/><xsl:value-of select="."/><xsl:text/>, expecting <xsl:text/><xsl:value-of select="$fox-attr/@type"/><xsl:text/><xsl:value-of select="string('&#xA;')"/></xsl:if>

		<!--REPORT -->
<xsl:if test="$fox-attr/@type = 'xs:unsignedInt' and not(. castable as xs:integer and . &gt;= 0)">Illegal value for <xsl:text/><xsl:value-of select="name(.)"/><xsl:text/> - <xsl:text/><xsl:value-of select="."/><xsl:text/>, expecting <xsl:text/><xsl:value-of select="$fox-attr/@type"/><xsl:text/><xsl:value-of select="string('&#xA;')"/></xsl:if>

		<!--REPORT -->
<xsl:if test="$fox-attr/@type = 'xs:boolean' and not(. castable as xs:boolean)">Illegal value for <xsl:text/><xsl:value-of select="name(.)"/><xsl:text/> - <xsl:text/><xsl:value-of select="."/><xsl:text/>, expecting <xsl:text/><xsl:value-of select="$fox-attr/@type"/><xsl:text/><xsl:value-of select="string('&#xA;')"/></xsl:if>

		<!--REPORT -->
<xsl:if test="$fox-attr/@type = 'entered-string' and not(string-length() &gt; 0)">Illegal value for <xsl:text/><xsl:value-of select="name(.)"/><xsl:text/> - <xsl:text/><xsl:value-of select="."/><xsl:text/>, expecting <xsl:text/><xsl:value-of select="$fox-attr/@type"/><xsl:text/><xsl:value-of select="string('&#xA;')"/></xsl:if>

		<!--REPORT -->
<xsl:if test="$fox-attr/@type = 'yn-flag' and not(index-of(('','Y','N'), . ))">Illegal value for <xsl:text/><xsl:value-of select="name(.)"/><xsl:text/> - <xsl:text/><xsl:value-of select="."/><xsl:text/>, expecting <xsl:text/><xsl:value-of select="$fox-attr/@type"/><xsl:text/><xsl:value-of select="string('&#xA;')"/></xsl:if>

		<!--REPORT -->
<xsl:if test="$fox-attr/xs:simpleType/xs:restriction/xs:enumeration and not(. = $fox-attr/xs:simpleType/xs:restriction/xs:enumeration/@value)">Illegal value for <xsl:text/><xsl:value-of select="name(.)"/><xsl:text/> - <xsl:text/><xsl:value-of select="."/><xsl:text/>, expecting one of - <xsl:text/><xsl:value-of select="$fox-attr/xs:simpleType/xs:restriction/xs:enumeration/@value"/><xsl:text/><xsl:value-of select="string('&#xA;')"/></xsl:if><xsl:apply-templates select="@*|*|comment()|processing-instruction()" mode="M15"/></xsl:template><xsl:template match="text()" priority="-1" mode="M15"/><xsl:template match="@*|node()" priority="-2" mode="M15">
		<xsl:choose>
			<!--Housekeeping: SAXON warns if attempting to find the attribute
                           of an attribute-->
			<xsl:when test="not(@*)">
				<xsl:apply-templates select="node()" mode="M15"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="@*|node()" mode="M15"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template></xsl:stylesheet>