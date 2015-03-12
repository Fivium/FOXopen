<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:key name="keyQuery" match="//FoxCommand[CommandInfo/Name = 'uk.gov.dti.og.fox.command.RunQueryCommand']" use="concat(CommandInfo/interface, CommandInfo/query)"/>
  <xsl:key name="keyApi" match="//FoxCommand[CommandInfo/Name = 'uk.gov.dti.og.fox.command.RunApiCommand']" use="concat(CommandInfo/interface, CommandInfo/api)"/>
  <xsl:key name="keyAction" match="//do_block[@actionname != '(Unnamed Command List)']" use="@actionname"/>
  <xsl:key name="keyXPathSetOut" match="//HtmlGenCmd[@cmd = 'fm:set-out' or @cmd = 'fm:expr-out']" use="substring-after(substring-before(./@cmdArgs, ' cmd.id'), 'match=')"/>
  <xsl:key name="keyXPathIf" match="//HtmlGenCmd[@cmd = 'fm:if']" use="substring-after(./@cmdArgs, 'test=')"/>
  <xsl:key name="keyXPathForEach" match="//HtmlGenCmd[@cmd = 'fm:for-each']" use="substring-after(./@cmdArgs, 'xpath=')"/>

  <xsl:output method="html" doctype-system="http://www.w3.org/TR/html4/strict.dtd" doctype-public="-//W3C//DTD HTML 4.01//EN" indent="yes"/>
  <xsl:template match="/FoxHttpPost | /FoxHttpGet">
    <html>
      <head>
        <script type="text/javascript" src="../js/excanvas"/>
        <script type="text/javascript" src="../js/jquery"/>
        <script type="text/javascript" src="../js/jqplot"/>
        <script type="text/javascript" src="../js/jqplotpie"/>
        <script type="text/javascript" src="../js/track"/>
        <link rel="StyleSheet" href="../css/track" type="text/css"/>   
        <link rel="StyleSheet" href="../css/jqplot" type="text/css"/>
      </head>
      <body>
        <p class="jumpMenu">
          Jump to: <a href="#">Tracking Report</a>
          | <a href="#DatabaseSummary">Database Summary</a>
          | <a href="#ActionSummary">Action Summary</a>
          | <a href="#ActionLog">Action Track</a>
          | <a href="#HTMLGenXpathSummary">HTML Generation Xpath Summary</a>
          | <a href="#HTMLLog">HTML Generation Track</a>
          | <a href="?view=original">Original Track View</a>
        </p>
        <h1>Tracking Report</h1>
        <xsl:variable name="summaryActionsMS" select="sum(//do_block[not(ancestor::node()[name() = 'do_block' ])]/@MS)"/>
        <xsl:variable name="summaryDatabaseMS" select="sum(//FoxCommand[CommandInfo/Name = 'uk.gov.dti.og.fox.command.RunQueryCommand' or CommandInfo/Name = 'uk.gov.dti.og.fox.command.RunApiCommand']/@MS)"/>
        <xsl:variable name="summaryHTMLGenMS" select="//HtmlGenerate/@MS"/>
        <table id="tracking-report-data">
          <tr>
            <td width="200px">Actions</td>
            <td>
              <xsl:value-of select="$summaryActionsMS"/> ms
            </td>
          </tr>
          <tr class="plot-point">
            <td><span class="plot-label">Actions (not including queries)</span></td>
            <td>
              <span class="plot-value"><xsl:value-of select="$summaryActionsMS - $summaryDatabaseMS"/></span>
              ms
            </td>
          </tr>
          <tr class="plot-point">
            <td><span class="plot-label">Database</span></td>
            <td>
              <span class="plot-value"><xsl:value-of select="$summaryDatabaseMS"/></span>
              ms
            </td>
          </tr>
          <tr class="plot-point">
            <td><span class="plot-label">HTML Generation</span></td>
            <td>
              <span class="plot-value"><xsl:value-of select="$summaryHTMLGenMS"/></span>
              ms
            </td>
          </tr>
          <tr>
            <td><b>Total</b></td>
            <td>
              <b><xsl:value-of select="$summaryActionsMS + $summaryHTMLGenMS"/> ms</b>
            </td>
          </tr>
        </table>
        <table>
          <tr>
            <td>
              <div id="tracking-report-pie" style="margin-top:10px; margin-left:10px; width:250px; height:250px;"></div>
            </td>
            <td>
              <div id="database-summary-pie" style="margin-top:10px; margin-left:10px; width:250px; height:250px;"></div>
            </td>
            <td>
              <div id="html-summary-pie" style="margin-top:10px; margin-left:10px; width:250px; height:250px;"></div>
            </td>
          </tr>
        </table>
        <a name="DatabaseSummary"/>
        <h1>Database Summary</h1>
        <table>
          <thead>
            <tr>
              <th scope="col" width="50px">Type</th>
              <th scope="col">db-interface</th>
              <th scope="col">Name</th>
              <th scope="col" id="numberCol">Run Count</th>
              <th scope="col" id="numberCol">Total Time (ms)</th>
              <th scope="col" id="numberCol">Average Time (ms)</th>
              <th scope="col" id="numberCol">Max. Time (ms)</th>
            </tr>
          </thead>
          <tbody id="database-summary-data">
            <xsl:for-each select="//FoxCommand[(CommandInfo/Name = 'uk.gov.dti.og.fox.command.RunQueryCommand' and generate-id() = generate-id(key('keyQuery', concat(./CommandInfo/interface, ./CommandInfo/query)))) or (CommandInfo/Name = 'uk.gov.dti.og.fox.command.RunApiCommand' and generate-id() = generate-id(key('keyApi', concat(./CommandInfo/interface, ./CommandInfo/api))))]">
              <xsl:sort select="@MS" data-type="number" order="descending"/>
              <xsl:variable name="cmdInfo" select="CommandInfo"/>
              <xsl:variable name="cmdRunCount" select="count(//FoxCommand[CommandInfo/Name = $cmdInfo/Name and CommandInfo/interface = $cmdInfo/interface and (CommandInfo/query = $cmdInfo/query or CommandInfo/api = $cmdInfo/api)])"/>
              <xsl:variable name="cmdTotalTime" select="sum(//FoxCommand[CommandInfo/Name = $cmdInfo/Name and CommandInfo/interface = $cmdInfo/interface and (CommandInfo/query = $cmdInfo/query or CommandInfo/api = $cmdInfo/api)]/@MS)"/>
              <xsl:variable name="qryApiUniqueName" select="concat(./CommandInfo/interface, '++', ./CommandInfo/query, ./CommandInfo/api)"/>
              <xsl:variable name="qryApiInterface" select="CommandInfo/interface"/>
              <tr class="plot-point">
                <td>
                  <xsl:choose>
                    <xsl:when test="CommandInfo/Name = 'uk.gov.dti.og.fox.command.RunQueryCommand'">
                      QUERY
                    </xsl:when>
                    <xsl:when test="CommandInfo/Name = 'uk.gov.dti.og.fox.command.RunApiCommand'">
                      API
                    </xsl:when>
                  </xsl:choose>
                </td>
                <td><xsl:value-of select="$qryApiInterface"/></td>
                <!-- #{$qryApiName} javascript:expandCommand('{$qryApiName}')-->
                <td>
                  <a href="#{$qryApiUniqueName}" onClick="javascript:expandCommand('{$qryApiUniqueName}')">
                    <span class="plot-label"><xsl:value-of select="concat(CommandInfo/query, CommandInfo/api)"/></span>
                  </a>
                </td>
                <td><xsl:value-of select="$cmdRunCount"/></td>
                <td><span class="plot-value"><xsl:value-of select="$cmdTotalTime"/></span></td>
                <td><xsl:value-of select="format-number($cmdTotalTime div $cmdRunCount, '######.##')"/></td>
                <td>
                  <xsl:for-each select="//FoxCommand[CommandInfo/Name = $cmdInfo/Name and CommandInfo/interface = $cmdInfo/interface and (CommandInfo/query = $cmdInfo/query or CommandInfo/api = $cmdInfo/api)]">
                    <xsl:sort select="@MS" data-type="number" order="descending"/>
                    <xsl:if test="position()=1"><xsl:value-of select="./@MS"/></xsl:if>
                  </xsl:for-each>
                </td>
              </tr>
            </xsl:for-each>
          </tbody>
        </table>

        <a name="ActionSummary"/>
        <h1>Action Summary</h1>
        <table>
          <thead>
            <tr>
              <th scope="col">Name</th>
              <th scope="col" id="numberCol">Run Count</th>
              <th scope="col" id="numberCol">Total Time (ms)</th>
              <th scope="col" id="numberCol">Average Time (ms)</th>
              <th scope="col" id="numberCol">Max. Time (ms)</th>
            </tr>
          </thead>
          <tbody>
            <xsl:for-each select="//do_block[@actionname != '(Unnamed Command List)']">
              <xsl:sort select="@MS" data-type="number" order="descending"/>
              <xsl:variable name="action" select="."/>
              <xsl:variable name="actionRunCount" select="count(//do_block[@actionname = $action/@actionname])"/>
              <xsl:variable name="actionTotalTime" select="sum(//do_block[@actionname = $action/@actionname]/@MS)"/>
              <xsl:variable name="actionName" select="./@actionname"/>
              <xsl:choose>
                <xsl:when test="contains(./@actionname, 'auto-action') or contains(./@actionname, 'auto-state')">
                  <tr id="autoAction">
                    <td><xsl:value-of select="@actionname"/></td>
                    <td><xsl:value-of select="$actionRunCount"/></td>
                    <td><xsl:value-of select="$actionTotalTime"/></td>
                    <td><xsl:value-of select="format-number($actionTotalTime div $actionRunCount, '######.##')"/></td>
                    <td>
                      <xsl:for-each select="//do_block[@actionname = $action/@actionname]">
                        <xsl:sort select="@MS" data-type="number" order="descending"/>
                        <xsl:if test="position()=1"><xsl:value-of select="./@MS"/></xsl:if>
                      </xsl:for-each>
                    </td>
                  </tr>
                </xsl:when>
                <xsl:otherwise>
                  <tr>
                    <td><a href="#{$actionName}"><xsl:value-of select="@actionname"/></a></td>
                    <td><xsl:value-of select="$actionRunCount"/></td>
                    <td><xsl:value-of select="$actionTotalTime"/></td>
                    <td><xsl:value-of select="$actionTotalTime div $actionRunCount"/></td>
                    <td>
                      <xsl:for-each select="//do_block[@actionname = $action/@actionname]">
                        <xsl:sort select="@MS" data-type="number" order="descending"/>
                        <xsl:if test="position()=1"><xsl:value-of select="./@MS"/></xsl:if>
                      </xsl:for-each>
                    </td>
                  </tr>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:for-each>
          </tbody>
        </table>

        <a name="ActionLog"/>
        <h1><xsl:value-of select="ModuleStatusAtStart/Name" />/ <xsl:value-of select="ModuleStatusAtStart/Theme" /></h1>
        <div class="expandCollapseControls">
          <a href="javascript:expandAll('cmdDetails')">Expand All</a> / <a href="javascript:collapseAll('cmdDetails')">Collapse All</a>
        </div>
        <table>
          <thead>
            <tr>
              <th>Command</th>
              <th id="numberCol">Time (ms)</th>
              <th id="numberCol">zmem</th>
              <th id="numberCol">zmemdelta</th>
            </tr>
          </thead>
          <tbody>
            <xsl:apply-templates select="TopLevelUserAction"/>
            <xsl:apply-templates select="TopLevelEntryTheme"/>
          </tbody>
        </table>


        <a name="HTMLGenXpathSummary"/>
        <h1>HTML Generation XPath Summary</h1>
        <table>
          <thead>
            <tr>
              <th scope="col">XPath</th>
              <th width="100px" scope="col">Usage</th>
              <th scope="col" id="numberCol">Run Count</th>
              <th scope="col" id="numberCol">Total Time (ms)</th>
              <th scope="col" id="numberCol">Average Time (ms)</th>
              <th scope="col" id="numberCol">Max. Time (ms)</th>
            </tr>
          </thead>
          <tbody id="html-summary-data">
            <xsl:for-each select=".//HtmlGenerate//HtmlGenCmd[((@cmd = 'fm:set-out' or @cmd = 'fm:expr-out') and generate-id() = generate-id(key('keyXPathSetOut', substring-after(substring-before(./@cmdArgs, ' cmd.id'), 'match=')))) or (@cmd = 'fm:if' and generate-id() = generate-id(key('keyXPathIf', substring-after(./@cmdArgs, 'test=')))) or (@cmd = 'fm:for-each' and generate-id() = generate-id(key('keyXPathForEach', substring-after(./@cmdArgs, 'xpath='))))]">
              <xsl:sort select="@MS" data-type="number" order="descending"/>
              <xsl:variable name="cmd" select="./@cmd"/>
              <xsl:choose>
                <xsl:when test="$cmd = 'fm:expr-out' or $cmd = 'fm:set-out'">
                  <xsl:variable name="xpath" select="substring-after(substring-before(./@cmdArgs, ' cmd.id'), 'match=')"/>
                  <xsl:variable name="xpathRunCount" select="count(//HtmlGenerate//HtmlGenCmd[@cmd = $cmd and substring-after(substring-before(./@cmdArgs, ' cmd.id'), 'match=') = $xpath])"/>
                  <xsl:variable name="xpathTotalTime" select="sum(//HtmlGenerate//HtmlGenCmd[@cmd = $cmd and substring-after(substring-before(./@cmdArgs, ' cmd.id'), 'match=') = $xpath]/@MS)"/>
                  <tr class="plot-point">
                    <td><span class="plot-label"><xsl:value-of select="$xpath"/></span></td>
                    <td><xsl:value-of select="$cmd"/></td>
                    <td><xsl:value-of select="$xpathRunCount"/></td>
                    <td><span class="plot-value"><xsl:value-of select="$xpathTotalTime"/></span></td>
                    <td><xsl:value-of select="format-number($xpathTotalTime div $xpathRunCount, '######.##')"/></td>
                    <td>
                      <xsl:for-each select="//HtmlGenerate//HtmlGenCmd[@cmd = $cmd and substring-after(substring-before(./@cmdArgs, ' cmd.id'), 'match=') = $xpath]">
                        <xsl:sort select="@MS" data-type="number" order="descending"/>
                        <xsl:if test="position()=1"><xsl:value-of select="./@MS"/></xsl:if>
                      </xsl:for-each>
                    </td>
                  </tr>
                </xsl:when>
                <xsl:when test="$cmd = 'fm:if'">
                  <xsl:variable name="xpath" select="substring-after(./@cmdArgs, 'test=')"/>
                  <xsl:variable name="xpathRunCount" select="count(//HtmlGenerate//HtmlGenCmd[@cmd = $cmd and substring-after(./@cmdArgs, 'test=') = $xpath])"/>
                  <xsl:variable name="xpathTotalTime" select="sum(//HtmlGenerate//HtmlGenCmd[@cmd = $cmd and substring-after(./@cmdArgs, 'test=') = $xpath]/@MS)"/>
                  <tr class="plot-point">
                    <td><span class="plot-label"><xsl:value-of select="$xpath"/></span></td>
                    <td><xsl:value-of select="$cmd"/></td>
                    <td><xsl:value-of select="$xpathRunCount"/></td>
                    <td><span class="plot-value"><xsl:value-of select="$xpathTotalTime"/></span></td>
                    <td><xsl:value-of select="format-number($xpathTotalTime div $xpathRunCount, '######.##')"/></td>
                    <td>
                      <xsl:for-each select="//HtmlGenerate//HtmlGenCmd[@cmd = $cmd and substring-after(./@cmdArgs, 'test=') = $xpath]">
                        <xsl:sort select="@MS" data-type="number" order="descending"/>
                        <xsl:if test="position()=1"><xsl:value-of select="./@MS"/></xsl:if>
                      </xsl:for-each>
                    </td>
                  </tr>
                </xsl:when>
                <xsl:when test="$cmd = 'fm:for-each'">
                  <xsl:variable name="xpath" select="substring-after(./@cmdArgs, 'xpath=')"/>
                  <xsl:variable name="xpathRunCount" select="count(//HtmlGenerate//HtmlGenCmd[@cmd = $cmd and substring-after(./@cmdArgs, 'xpath=') = $xpath])"/>
                  <xsl:variable name="xpathTotalTime" select="sum(//HtmlGenerate//HtmlGenCmd[@cmd = $cmd and substring-after(./@cmdArgs, 'xpath=') = $xpath]/@MS)"/>
                  <tr>
                    <td><xsl:value-of select="$xpath"/></td>
                    <td><xsl:value-of select="$cmd"/></td>
                    <td><xsl:value-of select="$xpathRunCount"/></td>
                    <td><xsl:value-of select="$xpathTotalTime"/></td>
                    <td><xsl:value-of select="format-number($xpathTotalTime div $xpathRunCount, '######.##')"/></td>
                    <td>
                      <xsl:for-each select="//HtmlGenerate//HtmlGenCmd[@cmd = $cmd and substring-after(./@cmdArgs, 'xpath=') = $xpath]">
                        <xsl:sort select="@MS" data-type="number" order="descending"/>
                        <xsl:if test="position()=1"><xsl:value-of select="./@MS"/></xsl:if>
                      </xsl:for-each>
                    </td>
                  </tr>
                </xsl:when>
              </xsl:choose>
            </xsl:for-each>
          </tbody>
        </table>

        <a name="HTMLLog"/>
        <h1>HTML Generate</h1>
        <table>
          <thead>
            <tr>
              <th>Command</th>
              <th id="numberCol">Time (ms)</th>
              <th id="numberCol">zmem</th>
              <th id="numberCol">zmemdelta</th>
            </tr>
          </thead>
          <tbody>
            <xsl:apply-templates select="//HtmlGenerate"/>
          </tbody>
        </table>

      </body>
    </html>

  </xsl:template>


  <xsl:template match="HtmlGenCmd">
    <xsl:param name="foxGenName"/>

    <xsl:variable name="level" select="count(ancestor::HtmlGenCmd)"/>
    <xsl:variable name="followingSiblings" select="count(../following-sibling::HtmlGenCmd)"/>
    <xsl:variable name="parentFollowingSiblings" select="count(../../following-sibling::HtmlGenCmd)"/>
    <xsl:variable name="commandId" select="generate-id(.)"/>

    <tr class="clickabletr" onclick="javascript:toggleCommandDetailVisibility('{$commandId}')">
      <td>
        <xsl:call-template name="treeImage">
          <xsl:with-param name="level" select="$level"/>
          <xsl:with-param name="followingSiblings" select="$followingSiblings"/>
          <xsl:with-param name="parentFollowingSiblings" select="$parentFollowingSiblings"/>
        </xsl:call-template>
        <xsl:value-of select="@cmd"/> <span class="queryApiName"><xsl:value-of select="@cmdArgs"/></span>
        <br/>
        <div id="{$commandId}" name="cmdGen" style="display: none;">
        <pre>
          <xsl:apply-templates select="./node()[name() != 'HtmlGenCmd']" mode="escape"/>
        </pre>
        </div>
      </td>
      <td><xsl:value-of select="@MS"/></td>
      <td><xsl:value-of select="@zmem"/></td>
      <td><xsl:value-of select="@zmemdelta"/></td>
    </tr>

    <xsl:apply-templates select="./HtmlGenCmd"/>
  </xsl:template>

  <xsl:template match="ActionName"/>

  <xsl:template match="ThemeName">
    <tr id="newModule">
      <td><xsl:value-of select="."/></td>
      <td></td>
      <td></td>
      <td></td>
    </tr>
  </xsl:template>

  <xsl:template match="do_block">
    <xsl:param name="foxCommandName"/>

    <xsl:variable name="level" select="count(ancestor-or-self::do_block[@actionname != 'fm:eval']) - 1"/>
    <xsl:variable name="followingSiblings" select="count(../following-sibling::do_block) + count(../following-sibling::FoxCommand)"/>
    <xsl:variable name="parentFollowingSiblings" select="count(../../following-sibling::do_block)"/>
    <xsl:variable name="actionName" select="./@actionname"/>

    <xsl:choose>
      <xsl:when test="./@actionname = '(Unnamed Command List)'">
      </xsl:when>
      <xsl:when test="contains(./@actionname, 'auto-action') or contains(./@actionname, 'auto-state')">
        <tr id="autoAction">
          <td id="actionBegin">
            <a name="{$actionName}"/>
            <xsl:call-template name="treeImage">
              <xsl:with-param name="level" select="$level"/>
              <xsl:with-param name="followingSiblings" select="$followingSiblings"/>
              <xsl:with-param name="parentFollowingSiblings" select="$parentFollowingSiblings"/>
            </xsl:call-template>
            <xsl:value-of select="./@actionname"/>
          </td>
          <td id="actionBegin"><xsl:value-of select="@MS"/></td>
          <td id="actionBegin"><xsl:value-of select="@zmem"/></td>
          <td id="actionBegin"><xsl:value-of select="@zmemdelta"/></td>
        </tr>
      </xsl:when>
      <xsl:when test="./@actionname = /FoxHttpPost/TopLevelUserAction/ActionName or ./@actionname = /FoxHttpGet/TopLevelUserAction/ActionName">
        <tr id="initialAction">
          <td id="actionBegin">
            <a name="{$actionName}"/>
            <xsl:call-template name="treeImage">
              <xsl:with-param name="level" select="$level"/>
              <xsl:with-param name="followingSiblings" select="$followingSiblings"/>
              <xsl:with-param name="parentFollowingSiblings" select="$parentFollowingSiblings"/>
            </xsl:call-template>
            <xsl:value-of select="./@actionname"/>
          </td>
          <td id="actionBegin"><xsl:value-of select="@MS"/></td>
          <td id="actionBegin"><xsl:value-of select="@zmem"/></td>
          <td id="actionBegin"><xsl:value-of select="@zmemdelta"/></td>
        </tr>
      </xsl:when>
      <xsl:when test="$foxCommandName = 'uk.gov.dti.og.fox.command.ActionCallCommand'">
        <tr>
          <td id="actionBegin">
            <a name="{$actionName}"/>
            <xsl:call-template name="treeImage">
              <xsl:with-param name="level" select="$level"/>
              <xsl:with-param name="followingSiblings" select="$followingSiblings"/>
              <xsl:with-param name="parentFollowingSiblings" select="$parentFollowingSiblings"/>
            </xsl:call-template>
            <xsl:value-of select="substring-before(substring-after($foxCommandName, 'uk.gov.dti.og.fox.command.'), 'Command')"/> - <xsl:value-of select="./@actionname"/>
          </td>
          <td id="actionBegin"><xsl:value-of select="@MS"/></td>
          <td id="actionBegin"><xsl:value-of select="@zmem"/></td>
          <td id="actionBegin"><xsl:value-of select="@zmemdelta"/></td>
        </tr>
      </xsl:when>
      <xsl:otherwise>
        <tr>
          <td>
            <xsl:call-template name="treeImage">
              <xsl:with-param name="level" select="$level"/>
              <xsl:with-param name="followingSiblings" select="$followingSiblings"/>
              <xsl:with-param name="parentFollowingSiblings" select="$parentFollowingSiblings"/>
            </xsl:call-template>
            <xsl:value-of select="./@actionname"/>
          </td>
          <td><xsl:value-of select="@MS"/></td>
          <td><xsl:value-of select="@zmem"/></td>
          <td><xsl:value-of select="@zmemdelta"/></td>
        </tr>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates/>
  </xsl:template>


  <xsl:template match="FoxCommand">
    <xsl:if test="./CommandInfo/Name != 'uk.gov.dti.og.fox.command.ActionCallCommand'">
      <xsl:variable name="commandId" select="generate-id(.)"/>
      <tr class="clickabletr" onclick="javascript:toggleCommandDetailVisibility('{$commandId}')">
        <td>
          <xsl:call-template name="treeImage">
            <xsl:with-param name="level" select="count(ancestor-or-self::FoxCommand[./CommandInfo and ./CommandInfo/Name != 'uk.gov.dti.og.fox.command.ActionCallCommand']) + count(ancestor-or-self::do_block[@actionname != '(Unnamed Command List)' and @actionname != 'fm:eval']) - 1"/>
            <xsl:with-param name="followingSiblings" select="count(following-sibling::FoxCommand)"/>
            <xsl:with-param name="parentFollowingSiblings" select="count(../../following-sibling::FoxCommand)"/>
          </xsl:call-template>
          <xsl:choose>
            <xsl:when test="./CommandInfo/Name = 'uk.gov.dti.og.fox.command.Cmd'">
              <xsl:value-of select="substring-after(./CommandInfo/Name, 'uk.gov.dti.og.fox.command.')"/>
            </xsl:when>
            <xsl:when test="./CommandInfo/Name = 'uk.gov.dti.og.fox.XDo'">
              <xsl:value-of select="substring-after(./CommandInfo/Name, 'uk.gov.dti.og.fox.')"/>
            </xsl:when>
            <xsl:when test="./CommandInfo/Name = 'uk.gov.dti.og.fox.command.RunQueryCommand'">
              <xsl:value-of select="substring-before(substring-after(./CommandInfo/Name, 'uk.gov.dti.og.fox.command.'), 'Command')"/> <span class="queryApiName">(<xsl:value-of select="./CommandInfo/query"/>)</span>
            </xsl:when>
            <xsl:when test="./CommandInfo/Name = 'uk.gov.dti.og.fox.command.RunApiCommand'">
              <xsl:value-of select="substring-before(substring-after(./CommandInfo/Name, 'uk.gov.dti.og.fox.command.'), 'Command')"/> <span class="queryApiName">(<xsl:value-of select="./CommandInfo/api"/>)</span>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="substring-before(substring-after(./CommandInfo/Name, 'uk.gov.dti.og.fox.command.'), 'Command')"/>
            </xsl:otherwise>
          </xsl:choose>
          <br/>
          <div id="{$commandId}" name="cmdDetails" style="display: none;">
          <xsl:if test="./CommandInfo/Name = 'uk.gov.dti.og.fox.command.RunQueryCommand' or ./CommandInfo/Name = 'uk.gov.dti.og.fox.command.RunApiCommand'">
            <xsl:variable name="qryApiName" select="concat(./CommandInfo/interface, '++', ./CommandInfo/query, ./CommandInfo/api)"/>
            <a name="{$qryApiName}"/>
          </xsl:if>
            <pre>
              <xsl:apply-templates select="./node()[name() != 'do_block' and name() != 'FoxCommand']" mode="escape"/>
            </pre>
          </div>
        </td>
        <td><xsl:value-of select="@MS"/></td>
        <td><xsl:value-of select="@zmem"/></td>
        <td><xsl:value-of select="@zmemdelta"/></td>
      </tr>
    </xsl:if>
    <xsl:apply-templates select="./do_block">
      <xsl:with-param name="foxCommandName" select="./CommandInfo/Name"/>
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="*" mode="escape">
    <!-- Begin opening tag -->
    <xsl:text>&lt;</xsl:text>
    <xsl:value-of select="name()"/>

    <!-- Attributes -->
    <xsl:for-each select="@*">
      <xsl:text> </xsl:text>
      <xsl:value-of select="name()"/>
      <xsl:text>="</xsl:text>
      <xsl:value-of select="."/>
      <xsl:text>"</xsl:text>
    </xsl:for-each>

    <xsl:choose>
      <xsl:when test=". = ''">
        <xsl:text>/&gt;</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <!-- End opening tag -->
        <xsl:text>&gt;</xsl:text>

        <!-- Content (child elements, text nodes, and PIs) -->
        <xsl:apply-templates select="node()" mode="escape" />

        <!-- Closing tag -->
        <xsl:text>&lt;/</xsl:text>
        <xsl:value-of select="name()"/>
        <xsl:text>&gt;</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="treeImage">
    <xsl:param name="level"/>
    <xsl:param name="followingSiblings"/>
    <xsl:param name="parentFollowingSiblings"/><div class="spacer"/><xsl:if test="$level > 0">
      <xsl:call-template name="treeImage">
        <xsl:with-param name="level" select="$level - 1"/>
        <xsl:with-param name="followingSiblings" select="$followingSiblings"/>
        <xsl:with-param name="parentFollowingSiblings" select="$parentFollowingSiblings"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>