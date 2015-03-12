<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" media-type="text/xml"/>
  <xsl:template match="/REPORT_PART">
    <FOX_SPREADSHEET>
      <WORKSHEET_METADATA>
        <TYPE>XLS</TYPE>
        <DOCUMENT_PROPERTIES/>
        <DATASOURCE_LIST>
          <DATASOURCE name="report_part_datasource" auto-size="true">
            <HEADER_FORMATTING font-name="font1"/>
            <TYPE>REPORT_PART_QUERY</TYPE>
            <REPORT_PART_ID>
              <xsl:value-of select="REPORT_PART_ID"/>
            </REPORT_PART_ID>
            <COL_LIST>
              <xsl:for-each select="SPREADSHEET_FORMATTING/COLUMN_LIST/COLUMN">
                <COL>
                  <xsl:attribute name="column-id">
                    <xsl:value-of select="COLUMN_NAME"/>
                  </xsl:attribute>
                  <xsl:if test="COLUMN_TITLE">
                    <xsl:attribute name="column-prompt">
                      <xsl:value-of select="COLUMN_TITLE"/>
                    </xsl:attribute>
                  </xsl:if>
                  <xsl:if test="COLUMN_STYLE">
                    <xsl:attribute name="style">
                      <xsl:value-of select="COLUMN_STYLE"/>
                    </xsl:attribute>
                  </xsl:if>
                </COL>
              </xsl:for-each>
            </COL_LIST>
            <DATA_QUERY>
              <SELECT>
                <xsl:if test="SQL">
                  <xsl:value-of select="SQL"/>
                </xsl:if>
              </SELECT>
            </DATA_QUERY>
          </DATASOURCE>
        </DATASOURCE_LIST>
        <FORMAT_MASK_LIST>
          <xsl:for-each select="SPREADSHEET_FORMATTING/STYLE_LIST/STYLE">
            <xsl:if test="NUMBER_FORMAT_ATTRIBUTES">
              <FORMAT_MASK>
                <xsl:attribute name="name">
                  <xsl:value-of select="STYLE_NAME"/>
                </xsl:attribute>
                <PRIMARY>
                  <xsl:value-of select="substring-before(substring-after(NUMBER_FORMAT_ATTRIBUTES/text(), '&quot;'), '&quot;')"/>
                </PRIMARY>
              </FORMAT_MASK>
            </xsl:if>
          </xsl:for-each>
        </FORMAT_MASK_LIST>
        <DATA_VALIDATION_LIST/>
        <AUTO_SIZE_LIST/>
        <FONT_LIST>
          <FONT name="font1">
            <NAME>Calibri</NAME>
            <SIZE>13</SIZE>
            <BOLD>true</BOLD>
            <ITALIC>false</ITALIC>
            <UNDERLINE>false</UNDERLINE>
          </FONT>
        </FONT_LIST>
        <STYLE_LIST>
          <xsl:for-each select="SPREADSHEET_FORMATTING/STYLE_LIST/STYLE">
            <STYLE>
              <xsl:attribute name="name">
                <xsl:value-of select="STYLE_NAME"/>
              </xsl:attribute>
              <xsl:if test="ALIGNMENT_ATTRIBUTES">
                <ALIGNMENT>
                  <xsl:choose>
                    <xsl:when test="substring-before(substring-after(ALIGNMENT_ATTRIBUTES/text(), '&quot;'), '&quot;')='Left'">
                    LEFT
                  </xsl:when>
                    <xsl:when test="substring-before(substring-after(ALIGNMENT_ATTRIBUTES/text(), '&quot;'), '&quot;')='Right'">
                    RIGHT
                  </xsl:when>
                    <xsl:when test="substring-before(substring-after(ALIGNMENT_ATTRIBUTES/text(), '&quot;'), '&quot;')='Center'">
                    CENTRE
                  </xsl:when>
                  </xsl:choose>
                </ALIGNMENT>
              </xsl:if>
              <xsl:if test="NUMBER_FORMAT_ATTRIBUTES">
                <FORMAT_MASK_NAME>
                  <xsl:value-of select="STYLE_NAME"/>
                </FORMAT_MASK_NAME>
              </xsl:if>
            </STYLE>
          </xsl:for-each>
        </STYLE_LIST>
      </WORKSHEET_METADATA>
      <WORKSHEET_LIST>
        <WORKSHEET>
          <xsl:attribute name="name">
            <xsl:value-of select="TITLE"/>
          </xsl:attribute>
          <ROW_LIST>
            <ROW datasource="report_part_datasource"/>
          </ROW_LIST>
        </WORKSHEET>
      </WORKSHEET_LIST>
    </FOX_SPREADSHEET>
  </xsl:template>
  <xsl:template match="/REPORT_DEFINITION">
    <FOX_SPREADSHEET>
      <WORKSHEET_METADATA>
        <TYPE>XLS</TYPE>
        <DOCUMENT_PROPERTIES/>
        <DATASOURCE_LIST>
          <xsl:for-each select="REPORT_PART_LIST/REPORT_PART[NAME/text()=/REPORT_DEFINITION/WORKSHEET_LIST/WORKSHEET/ANTECEDENT/text()]">
            <DATASOURCE auto-size="true">
              <xsl:attribute name="name">
                <xsl:value-of select="NAME"/>
              </xsl:attribute>
              <HEADER_FORMATTING font-name="font1"/>
              <TYPE>REPORT_PART_QUERY</TYPE>
              <REPORT_PART_ID>
                <xsl:value-of select="/REPORT_DEFINITION/WORKSHEET_LIST/WORKSHEET[ANTECEDENT/text()=current()/NAME]/RP_ID/text()"/>
                <!-- self::node()/NAME -->
              </REPORT_PART_ID>
              <COL_LIST>
                <xsl:for-each select="SPREADSHEET_FORMATTING/COLUMN_LIST/COLUMN">
                  <COL>
                    <xsl:attribute name="column-id">
                      <xsl:value-of select="COLUMN_NAME"/>
                    </xsl:attribute>
                    <xsl:if test="COLUMN_TITLE">
                      <xsl:attribute name="column-prompt">
                        <xsl:value-of select="COLUMN_TITLE"/>
                      </xsl:attribute>
                    </xsl:if>
                    <xsl:if test="COLUMN_STYLE">
                      <xsl:attribute name="style">
                        <xsl:value-of select="COLUMN_STYLE"/>
                      </xsl:attribute>
                    </xsl:if>
                  </COL>
                </xsl:for-each>
              </COL_LIST>
              <DATA_QUERY>
                <SELECT>
                  <xsl:if test="SQL">
                    <xsl:value-of select="SQL"/>
                  </xsl:if>
                </SELECT>
              </DATA_QUERY>
            </DATASOURCE>
          </xsl:for-each>
        </DATASOURCE_LIST>
        <FORMAT_MASK_LIST>
          <xsl:for-each select="REPORT_PART_LIST/REPORT_PART[NAME/text()=/REPORT_DEFINITION/WORKSHEET_LIST/WORKSHEET/ANTECEDENT/text()]">
            <xsl:for-each select="SPREADSHEET_FORMATTING/STYLE_LIST/STYLE">
              <xsl:if test="NUMBER_FORMAT_ATTRIBUTES">
                <FORMAT_MASK>
                  <xsl:attribute name="name">
                    <xsl:value-of select="STYLE_NAME"/>
                  </xsl:attribute>
                  <PRIMARY>
                    <xsl:value-of select="substring-before(substring-after(NUMBER_FORMAT_ATTRIBUTES/text(), '&quot;'), '&quot;')"/>
                  </PRIMARY>
                </FORMAT_MASK>
              </xsl:if>
            </xsl:for-each>
          </xsl:for-each>
        </FORMAT_MASK_LIST>
        <DATA_VALIDATION_LIST/>
        <AUTO_SIZE_LIST/>
        <FONT_LIST>
          <FONT name="font1">
            <NAME>Calibri</NAME>
            <SIZE>13</SIZE>
            <BOLD>true</BOLD>
            <ITALIC>false</ITALIC>
            <UNDERLINE>false</UNDERLINE>
          </FONT>
        </FONT_LIST>
        <STYLE_LIST>
          <xsl:for-each select="REPORT_PART_LIST/REPORT_PART[NAME/text()=/REPORT_DEFINITION/WORKSHEET_LIST/WORKSHEET/ANTECEDENT/text()]">
            <xsl:for-each select="SPREADSHEET_FORMATTING/STYLE_LIST/STYLE">
              <STYLE>
                <xsl:attribute name="name">
                  <xsl:value-of select="STYLE_NAME"/>
                </xsl:attribute>
                <xsl:if test="ALIGNMENT_ATTRIBUTES">
                  <ALIGNMENT>
                    <xsl:choose>
                      <xsl:when test="substring-before(substring-after(ALIGNMENT_ATTRIBUTES/text(), '&quot;'), '&quot;')='Left'">
                        LEFT
                      </xsl:when>
                      <xsl:when test="substring-before(substring-after(ALIGNMENT_ATTRIBUTES/text(), '&quot;'), '&quot;')='Right'">
                        RIGHT
                      </xsl:when>
                      <xsl:when test="substring-before(substring-after(ALIGNMENT_ATTRIBUTES/text(), '&quot;'), '&quot;')='Center'">
                        CENTRE
                      </xsl:when>
                    </xsl:choose>
                  </ALIGNMENT>
                </xsl:if>
                <xsl:if test="NUMBER_FORMAT_ATTRIBUTES">
                  <FORMAT_MASK_NAME>
                    <xsl:value-of select="STYLE_NAME"/>
                  </FORMAT_MASK_NAME>
                </xsl:if>
              </STYLE>
            </xsl:for-each>
          </xsl:for-each>
        </STYLE_LIST>
      </WORKSHEET_METADATA>
      <WORKSHEET_LIST>
        <xsl:for-each select="REPORT_PART_LIST/REPORT_PART[NAME/text()=/REPORT_DEFINITION/WORKSHEET_LIST/WORKSHEET/ANTECEDENT/text()]">
          <WORKSHEET>
            <xsl:attribute name="name">
              <xsl:value-of select="TITLE"/>
            </xsl:attribute>
            <ROW_LIST>
              <ROW>
                <xsl:attribute name="datasource">
                  <xsl:value-of select="NAME"/>
                </xsl:attribute>
              </ROW>
            </ROW_LIST>
          </WORKSHEET>
        </xsl:for-each>
      </WORKSHEET_LIST>
    </FOX_SPREADSHEET>
  </xsl:template>
</xsl:stylesheet>
