<%@ page import="net.foxopen.fox.enginestatus.EngineStatus" %>
<%@ page import="net.foxopen.fox.enginestatus.StatusCategory" %>
<%@ page import="net.foxopen.fox.entrypoint.uri.RequestURIBuilder" %>
<%@ page import="net.foxopen.fox.entrypoint.uri.RequestURIBuilderImpl" %>
<%@ page import="java.util.Collection" %>
<%@ page import="net.foxopen.fox.enginestatus.StatusBangHandler" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
  RequestURIBuilder lURIBuilder = RequestURIBuilderImpl.createFromHttpRequest(request);
  Collection<StatusCategory> lCategories = (Collection<StatusCategory>) request.getAttribute(EngineStatus.ALL_CATEGORIES_ATTRIBUTE);
%>
<html>
<head>
  <title>FOX Engine Status</title>
  <script type="text/javascript" src="<%= lURIBuilder.buildStaticResourceURI("js/jquery.js") %>"></script>
  <script type="text/javascript" src="<%= lURIBuilder.buildServletURI("status/bootstrap.min.js")  %>"></script>
  <link rel="stylesheet" href="<%= lURIBuilder.buildServletURI("status/bootstrap.min.css")  %>"/>
  <link rel="stylesheet" type="text/css" href="<%= lURIBuilder.buildContextResourceURI("/icomoon/icomoon.css") %>" />
  <style type="text/css">
    body {
      margin: 1em;
    }

    * {
      font-size: 13px;
    }

    h1 {
      font-size: 22px;
    }

    h2 {
      font-size: 19px;
    }

    ul, ol {
      padding: 0 0 0 1em;
      margin: 0;
    }

    .modal-dialog { width: 90%; }

    .message-success {
      color: green;
    }

    .message-warning {
      color: #ec971f;
    }

    .message-error {
      color: orangered;
    }

    .categoryIcon {
      display: none;
      font-size: 18px;
      cursor: pointer;
    }

    .messageTitle {
      font-weight: bold;
      width: 15%;
    }

    .categoryContainer {
      border: 1px solid #eee;
      margin-bottom: 1em;
      padding: 0em 0.5em 0.5em 0.5em;
    }

  </style>
  <script type="text/javascript">

    function loadHTML(target, href, doAfter) {
      target.html("Loading...");

      target.load(href, null, function(response, status, xhr) {
        if (status == "error") {
          target.html("Error retrieving page: " + xhr.status + " " + xhr.statusText);
        }
        if(doAfter != null){
          doAfter();
        }
      });
    }

    function attachDetailListeners() {
      //Attach event listeners for "view detail" links
      $('*[data-status-type=detail]').unbind('click').click(function(event) {
        $('#detailModal').modal(true);
        loadHTML($('#detailModal .modal-body'), $(this).attr('href'));
        event.preventDefault();
      });
    }

    function loadCategoryHTML(categoryContainer, restoreScroll) {
      var scrollPos = $(document).scrollTop();
      loadHTML(categoryContainer.find('.categoryContent'), categoryContainer.data('category-href'), function() {
        //Make sure we attach listeners for any detail links which have come out
        attachDetailListeners();
        //Reset the scroll position to mitigate annoying page jumps
        if(restoreScroll) {
          $(document).scrollTop(scrollPos);
        }
      });
    }

    $(document).ready(function() {

      $('*[data-status-type=showCategory]').click(function(event) {

        var container = $(this).parents('.categoryContainer');

        loadCategoryHTML(container);

        container.find('*[data-status-type=showCategory]').toggle(false);
        container.find('*[data-status-type=hideCategory]').toggle(true);
        container.find('*[data-status-type=refreshCategory]').toggle(true);

        event.preventDefault();
      });

      $('*[data-status-type=refreshCategory]').click(function(event) {
        var container = $(this).parents('.categoryContainer');
        loadCategoryHTML(container, true);
        event.preventDefault();
      });

      $('*[data-status-type=hideCategory]').click(function(event) {
        var container = $(this).parents('.categoryContainer');
        container.find('.categoryContent').empty();

        container.find('*[data-status-type=showCategory]').toggle(true);
        container.find('*[data-status-type=hideCategory]').toggle(false);
        container.find('*[data-status-type=refreshCategory]').toggle(false);

        event.preventDefault();
      });

      $('#expandAll').click(function(event) {
        $('*[data-status-type=showCategory]').click();
        event.preventDefault();
      });

      $('#contractAll').click(function(event) {
        $('*[data-status-type=hideCategory]').click();
        event.preventDefault();
      });

      //Call show/hide handlers based on whether the server said the category should be expanded by default
      $('.categoryContainer[data-expanded=true]').find('*[data-status-type=showCategory]').click();
      $('.categoryContainer[data-expanded=false]').find('*[data-status-type=hideCategory]').click();

    });

  </script>
</head>
<body>
  <div class="modal" id="detailModal" tabindex="-1" role="dialog" aria-hidden="true">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
          <h4 class="modal-title" id="myModalLabel">View Detail</h4>
        </div>
        <div class="modal-body">
          Loading...
        </div>
      </div>
    </div>
  </div>
  <a href="#" id="expandAll">Expand all</a> / <a href="#" id="contractAll">Contract all</a>
<%
  for(StatusCategory lCategory : lCategories) {
    lURIBuilder.setParam(StatusBangHandler.CATEGORY_PARAM_NAME, lCategory.getMnem());
%>
  <div class="categoryContainer" data-expanded="<%=lCategory.isExpanded()%>" data-category-href="<%=lURIBuilder.buildBangHandlerURI(StatusBangHandler.instance())%>">
    <h1><%=lCategory.getTitle()%> <% if(lCategory.getMaxMessageSeverity().requiresAttention()) { %>[<%=lCategory.getMaxMessageSeverity().asHTML()%>]<%}%>
      <span class="categoryIcon icon-plus" data-status-type="showCategory"></span>
      <span class="categoryIcon icon-minus" data-status-type="hideCategory"></span>
      <span class="categoryIcon icon-loop2" data-status-type="refreshCategory"></span>
    </h1>
    <div class="categoryContent"></div>
  </div>
<%
  }
%>

</body>
</html>
