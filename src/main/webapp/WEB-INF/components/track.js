  /**** EXPAND/COLLAPSE JS FUNCTIONALITY ****/
  function toggleCommandDetailVisibility(commandId) {
   $('#'+commandId).toggle();
  }
  
  function expandCommand(commandName) {
    $('*[name="'+commandName+'"]').parent().show();
  }
  
  function expandAll(blockName) {
    $('*[name="'+blockName+'"]').show();
  }
  
  function collapseAll(blockName) {
    $('*[name="'+blockName+'"]').hide();
  }

/**** JQPLOT FUNCTIONS ****/
$(document).ready(function(){

  var jumpMenuHeight = $('.jumpMenu').outerHeight();

  function extractDataWithMaxSegments(dataWrapper, maxSegments){
    var pointArray = [];
    dataWrapper.find('.plot-point').each(function(index){
      if (index < maxSegments) {
        hasOne = true;
        var label = $($(this).find('.plot-label')[0]).text();
        var value = parseInt($($(this).find('.plot-value')[0]).text());
        pointArray[index] = [label,value];
      }
      else {
        pointArray[maxSegments-1][0] = ['Others'];
        pointArray[maxSegments-1][1] += parseInt($($(this).find('.plot-value')[0]).text());
      }
    });
    if (pointArray.length < 1) {
      pointArray[0] = ['No Data',0];
    }
    return pointArray;
  };
  
  function bindClicks(canvasWrapper, dataWrapper){
    canvasWrapper.bind('jqplotDataClick', 
      function (ev, seriesIndex, pointIndex, data) {
        var top = $($(dataWrapper.find('.plot-point')[pointIndex]).find('.plot-label')[0]).position().top;
        $(window).scrollTop(top-jumpMenuHeight); 
      });
  };
    
  function plotPieMaxSegments(canvasWrapper, dataWrapper, title, maxSegments){
    var canvasWrapperId = canvasWrapper.attr('id');
    var pointArray = extractDataWithMaxSegments(dataWrapper, maxSegments);
    var returnValue = $.jqplot(canvasWrapperId, [pointArray], {
      title:{text:title}
    , grid:{shadow:false}
    , seriesDefaults:{
        renderer:$.jqplot.PieRenderer
      , trendline:{show:false}
      , rendererOptions: {
          padding: 5
        , showDataLabels: true
        , dataLabels: 'value'
        , dataLabelFormatString: '%dms'
        , dataLabelPositionFactor: 0.7
        } 
      }
    , legend:{
        show:true
      , placement: 'outside'
      , rendererOptions: {
          numberRows: pointArray.length
        }
      , location:'e'
      }
    });
    bindClicks(canvasWrapper, dataWrapper);
    return returnValue;
  };
  
  function plotPie(canvasWrapper, dataWrapper, title){
    return plotPieMaxSegments(canvasWrapper, dataWrapper, title, Number.POSITIVE_INFINITY);
  };

  trackingPie = plotPie($('#tracking-report-pie'), $('#tracking-report-data'), 'Tracking Report');
  databasePie = plotPieMaxSegments($('#database-summary-pie'), $('#database-summary-data'), 'Database Summary',6);
  htmlPie = plotPieMaxSegments($('#html-summary-pie'), $('#html-summary-data'), 'HTML Generation Summary',6);
});