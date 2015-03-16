<!DOCTYPE html>
<%@page import="net.foxopen.fox.XFUtil,java.util.Map" %>
<%@ page import="net.foxopen.fox.entrypoint.uri.RequestURIBuilder" %>
<%@ page import="net.foxopen.fox.entrypoint.uri.RequestURIBuilderImpl" %>
<%@ page import="net.foxopen.fox.entrypoint.servlets.FoxBootServlet" %>
<%@ page contentType="text/html;charset=UTF-8"%>
<%

  RequestURIBuilder lURIBuilder = RequestURIBuilderImpl.createFromHttpRequest(request);

  String lDBURL = (String) request.getAttribute("db_url");
  String lDBUser = (String) request.getAttribute("db_user");
  String lDBPassword = (String) request.getAttribute("db_password");
  String lIsDevelopment = (String) request.getAttribute("is_development");
  String lIsProduction = (String) request.getAttribute("is_production");
  Boolean lIsConfigured = (Boolean) request.getAttribute("is_configured");
  String lFoxServiceList = (String) request.getAttribute("fox_service_list");
  String lFoxEnvironment = (String) request.getAttribute("fox_environment");
  String lSupportUser = (String) request.getAttribute("support_user");
  String lAdminUser = (String) request.getAttribute("admin_user");
  String lFoxEnginePort = (String) request.getAttribute("fox_engine_port");
  Map<String, String> lFoxDBUserMap = (Map<String, String>) request.getAttribute("fox_db_user_map");
%>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>Fox Engine Configuration</title>
    <script type="text/javascript" src="<%= lURIBuilder.buildStaticResourceURI("js/jquery.js") %>"></script>
    <script type="text/javascript" src="<%= lURIBuilder.buildStaticResourceURI("js/html5shiv.js")  %>"></script>
    <script type="text/javascript">
      function isAllNBSPs(passValue) {
        return passValue != null && passValue.length > 0 && (passValue.match(new RegExp(String.fromCharCode(160), "g")) || []).length == passValue.length
      }
      // If a password field is full of obfuscated chars, blank it out on focus and restore blank characters on blur if not overwritten
      $(document).ready(function () {
        $("input[type='password']").focus(function() {
          var passValue = $(this).val();
          if(isAllNBSPs(passValue)) {
            $(this).data('restore_nbsps', passValue);
            $(this).val('');
          }
        });

        $("input[type='password']").blur(function() {
          var passValue = $(this).val();
          if(isAllNBSPs($(this).data('restore_nbsps')) && passValue.length == 0) {
            $(this).val($(this).data('restore_nbsps'));
            $(this).data('restore_nbsps', '');
          }
        });

        $('#saveConfig').click(function() {

          $(this).addClass("icon-animated-spinner");
          var that = $(this);

          $.post('<%= lURIBuilder.buildServletURI(FoxBootServlet.BOOT_SERVLET_PATH) %>/!HANDLECONFIGURE', $('#config').serialize())
            .done(function(data) {
              alert(data.message);
            })
            .fail(function(jqXHR) {
              alert('Failed to save configuration: ' + jqXHR.statusText);
            })
            .always(function() {
              that.removeClass("icon-animated-spinner");
            });

          return false;
        });
      });

      function getSecurityKey() {
        $("#generateSecurityKeyStatus").text("");

        $("#generateSecurityKey").addClass("icon-animated-spinner");

        $.ajax({
          url: '<%= lURIBuilder.buildServletURI(FoxBootServlet.BOOT_SERVLET_PATH) %>/!SECURITY/generate',
          datatype: "json"
        })
        .done(function (jsonData) {
          if (jsonData.status == 'true') {
            $("#generateSecurityKeyStatus").text("Success");
            $("#encryptionKey").val(jsonData['generated_key']);
            $("#testConnection").prop('disabled', false);
          }
          else {
            $("#generateSecurityKeyStatus").text("Failure");
            if (jsonData.hasOwnProperty("message")) {
              alert("Security file generate failed!\r\n"+jsonData['message']);
            }
            else {
              alert("Security file generate failed, see console for detailed info");
              console.error("Security generate failed, response:");
              console.error(jsonData);
            }
          }
        })
        .fail(function(jqXHR) {
          alert('Failed to generate security key: ' + jqXHR.statusText);
        })
        .always(function(){
          $("#generateSecurityKey").removeClass("icon-animated-spinner");
        })
      }

        function enableTestDBConnection() {
        if ($("#encryptionKey").val() != "") {
          $("#testConnection").prop('disabled', false);
        }
        else {
          $("#testConnection").prop('disabled', true);
        }
      }

      function testDBConnection() {
        $("#test_connection_status").text("");
        $("#fox_environment").text("");

        $("#testConnection").addClass("icon-animated-spinner");

        $.ajaxSetup({
          data: {
            'db_url': $("#db_url").val()
          , 'db_user': $("#db_user").val()
          , 'db_password': $("#db_password").val()
          }
        });

        $.ajax({
          url: '<%= lURIBuilder.buildServletURI(FoxBootServlet.BOOT_SERVLET_PATH) %>/!TESTCONNECTION',
          datatype: "json"
        }).done(function (jsonData) {

          if (jsonData.status == 'success') {
            $("#testConnectionStatus").text("Success");
            if (jsonData.hasOwnProperty("fox_environment_list")) {

              var foxEnvironmentListSplit = jsonData['fox_environment_list'];

              $.each(foxEnvironmentListSplit, function(index, obj) {
                $("#fox_environment").append($("<option></option>").attr("value", foxEnvironmentListSplit[index]).text(foxEnvironmentListSplit[index]));
              });
            }
            else {
              $("#testConnectionStatus").text("Failed");
              alert("Missing environment list in returned data, see console for more");
              console.error(jsonData);
            }
          }
          else {
            $("#testConnectionStatus").text("Failed");
            if (jsonData.hasOwnProperty("message")) {
              alert(jsonData.message);
            }
            console.error(jsonData);
          }
        })
        .fail(function(jqXHR){
          alert('Failed to test connection: ' + jqXHR.statusText);
        })
        .always(function(){
          $("#testConnection").removeClass("icon-animated-spinner");
        });
      }

      function addNewDbConnection() {
        var foxConnectionCount = parseInt($("#fox_connection_count").val());

        $("#dbConnectionsContainer").append('<div class="row"><div class="two columns"><label id="pdb_username_' + foxConnectionCount + '" for="db_username_' + foxConnectionCount + '" class="prompt west">Username</label></div><div class="four columns"><input id="db_username_' + foxConnectionCount + '" name="db_username_' + foxConnectionCount + '" type="text" value="" /></div><div class="two columns"><label id="pdb_password_' + foxConnectionCount + '" for="db_password_' + foxConnectionCount + '" class="prompt west">Password</label></div><div class="four columns"><input id="db_password_' + foxConnectionCount + '" name="db_password_' + foxConnectionCount + '" type="password" value=""/></div></div>');

        foxConnectionCount = foxConnectionCount + 1;

        $("#fox_connection_count").attr("value", foxConnectionCount);
      }
    </script>
    <link rel="stylesheet" type="text/css" href="<%= lURIBuilder.buildStaticResourceURI("css/fox.css") %>" />
    <link rel="stylesheet" type="text/css" href="<%= lURIBuilder.buildContextResourceURI("/OpenSans/OpenSans.css") %>" />
    <link rel="stylesheet" type="text/css" href="<%= lURIBuilder.buildContextResourceURI("/icomoon/icomoon.css") %>" />
  </head>
  <body onload="javascript:enableTestDBConnection();" style="margin-left: 1.5em;"> <!-- required so that the connection button is not disabled when laoding the configuration from a file -->
    <h1>FOX engine configuration</h1>
    <h2>Boot-config</h2>
  <!-- [atwd] move all parameters to variables and get them locally, escaping all information with util methods -->
    <form name='config' id="config" method='post' id="foxConfigureForm" action="<%= lURIBuilder.buildServletURI(FoxBootServlet.BOOT_SERVLET_PATH) %>/!HANDLECONFIGURE">

      <h3>Encryption</h3>
      <div class="container setoutForm">
        <div class="row">
          <div class="two columns">
            <label id="pencryptionKey" for="encryptionKey" class="prompt west">Public Encryption Key</label>
          </div>
          <div class="eight columns">
            <textarea rows="6" cols="78" id="encryptionKey" name="encryptionKey" onchange="javascript:enableTestDBConnection();"></textarea>
          </div>
          <div class="two columns">
            <button type="button" id="generateSecurityKey" name="generateSecurityKey" onclick="javascript:getSecurityKey();">Generate Security Key</button>
            <span id="generateSecurityKeyStatus"></span>
          </div>
        </div>
      </div>

      <h3>Database Details</h3>
      <div class="container setoutForm">
        <div class="row">
          <div class="two columns">
            <label id="pdb_url" for="db_url" class="prompt west">Database URL</label>
          </div>
          <div class="ten columns">
            <input type="text" id="db_url" name="db_url" size="100" value="<%= lDBURL %>" />
          </div>
        </div>
        <div class="row">
          <div class="two columns">
            <label id="pdb_user" for="db_user" class="prompt west">Username</label>
          </div>
          <div class="ten columns">
            <input type="text" id="db_user" name="db_user" size="100" value="<%= lDBUser %>" />
          </div>
        </div>
        <div class="row">
          <div class="two columns">
            <label id="pdb_password" for="db_password" class="prompt west">Password</label>
          </div>
          <div class="ten columns">
            <input type="password" id="db_password" name="db_password" size="100" value="<%= XFUtil.obfuscateValue(lDBPassword) %>" />
          </div>
        </div>
        <div class="row">
          <div class="two columns">
          </div>
          <div class="ten columns">
            <button type="button" disabled="disabled" id="testConnection" name="testConnection" onclick="javascript:testDBConnection();">Test Connection and Get Fox Environments</button>
            <span id="testConnectionStatus"></span>
          </div>
        </div>
      </div>


      <h3>Database Connections</h3>
      <div id="dbConnectionsContainer" class="container setoutForm">
        <div class="row">
          <div class="twelve columns">
            <button type="button" id="addFoxConnection" onclick="javascript:addNewDbConnection();">Add</button>
          </div>
        </div>
<%
  if (lFoxDBUserMap.size() == 0) {
    // Add typical connections to the map if none already defined
    lFoxDBUserMap.put("APPENV", "");
    lFoxDBUserMap.put("DECMGR", "");
  }

  int lFoxConnectionCount = 0;

  for (Map.Entry<String, String> lFoxDBUser : lFoxDBUserMap.entrySet()) {
    String lFoxUsername = lFoxDBUser.getKey();
    String lFoxDatabasePassword = lFoxDBUser.getValue();
%>
        <div class="row">
          <div class="two columns">
            <label id="pdb_username_<%= lFoxConnectionCount %>" for="db_username_<%= lFoxConnectionCount %>" class="prompt west">Username</label>
          </div>
          <div class="four columns">
            <input id="db_username_<%= lFoxConnectionCount %>" name="db_username_<%= lFoxConnectionCount %>" type="text" value="<%= lFoxUsername %>" />
          </div>
          <div class="two columns">
            <label id="pdb_password_<%= lFoxConnectionCount %>" for="db_password_<%= lFoxConnectionCount %>" class="prompt west">Password</label>
          </div>
          <div class="four columns">
            <input id="db_password_<%= lFoxConnectionCount %>" name="db_password_<%= lFoxConnectionCount %>" type="password" value="<%= XFUtil.obfuscateValue(lFoxDatabasePassword) %>"/>
          </div>
        </div>
<%
    lFoxConnectionCount++;
  }
%>
      </div>

      <h3>Fox Environment</h3>
      <div class="container setoutForm">
        <div class="row">
          <div class="twelve columns">
            <select id="fox_environment" name="fox_environment">
<%
  if (!XFUtil.isNull(lFoxEnvironment)) {
%>
              <option value="<%= lFoxEnvironment %>"><%= lFoxEnvironment %></option>
<%
  }
%>
            </select>
          </div>
        </div>
      </div>

      <h3>Engine User Details</h3>
      <div class="container setoutForm">
        <div class="row">
          <div class="two columns">
            <label id="psupport_user" for="support_user" class="prompt west">Support Username</label>
          </div>
          <div class="three columns">
            <input id="support_user" name="support_user" type="text" value="<%= XFUtil.nvl(lSupportUser, "support") %>" />
          </div>
          <div class="one columns">
            <label id="psupport_password" for="support_password" class="prompt west">Password</label>
          </div>
          <div class="three columns">
            <input id="support_password" name="support_password" type="password" value="<%=  lIsConfigured ? XFUtil.obfuscateValue("stubbedpass") : "" /* this outputs nbsp's and if detected on post, kept the same as before */ %>"/>
          </div>
          <div class="three columns">
          </div>
        </div>
        <div class="row">
          <div class="two columns">
            <label id="padmin_user" for="admin_user" class="prompt west">Admin Username</label>
          </div>
          <div class="three columns">
            <input id="admin_user" name="admin_user" type="text" value="<%= XFUtil.nvl(lAdminUser, "admin") %>" />
          </div>
          <div class="one columns">
            <label id="padmin_password" for="admin_password" class="prompt west">Password</label>
          </div>
          <div class="three columns">
            <input id="admin_password" name="admin_password" type="password" value="<%= lIsConfigured ? XFUtil.obfuscateValue("stubbedpass") : "" /* this outputs nbsp's and if detected on post, kept the same as before */ %>"/>
          </div>
          <div class="three columns">
            <input id="admin_password_2" name="admin_password_2" type="password" value="<%= lIsConfigured ? XFUtil.obfuscateValue("stubbedpass") : ""/* this outputs nbsp's and if detected on post, kept the same as before */ %>"/>
          </div>
        </div>
      </div>

      <h3>Fox Environment</h3>
      <div class="container setoutForm">
        <div class="row">
          <div class="two columns">
            <label id="pstatus" for="status" class="prompt west">Purpose</label>
          </div>
          <div class="ten columns">
            <select id="status" name="status">
              <option value="DEVELOPMENT" <% if (!XFUtil.isNull(lIsDevelopment)) { %> selected="<%= lIsDevelopment %>" <%}%> >Development</option>
              <option value="PRODUCTION" <% if (!XFUtil.isNull(lIsProduction)) { %> selected="<%= lIsProduction %>" <%}%> >Production</option>
            </select>
          </div>
        </div>
        <div class="row">
          <div class="two columns">
            <label id="pfox_service_list" for="fox_service_list" class="prompt west">Service List</label>
          </div>
          <div class="ten columns">
            <input id="fox_service_list" name="fox_service_list" type="text" size="80" value="<%= lFoxServiceList %>" />
          </div>
        </div>
        <div class="row">
          <div class="two columns">
            <label id="pfox_engine_port" for="fox_engine_port" class="prompt west">Engine Port</label>
          </div>
          <div class="ten columns">
            <input id="fox_engine_port" name="fox_engine_port" type="text" size="20" value="<%= lFoxEnginePort %>" />
          </div>
        </div>
      </div>

      <div class="container setoutForm">
        <div class="row">
          <div class="twelve columns">
            <button id="saveConfig" name="saveConfig" type="submit" class="positive-button" style="margin: 2em 0; float:right;">Save Configuration</button>
          </div>
        </div>
      </div>

      <input type="hidden" name="fox_connection_count" id="fox_connection_count" value="<%= lFoxConnectionCount %>" />
    </form>
  </body>
</html>
