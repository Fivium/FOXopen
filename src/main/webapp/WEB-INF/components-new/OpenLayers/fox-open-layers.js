/*
 * FOX Spatial
 *
 * Copyright 2015, Fivium Ltd.
 * Released under the BSD 3-Clause license.
 *
 * Dependencies:
 *   OpenLayers v3.9.0
 *   jQuery v1.11
 */

// Can be run through JSDoc (https://github.com/jsdoc3/jsdoc) and JSHint (http://www.jshint.com/)

/*jshint laxcomma: true, laxbreak: true, strict: false */

var FOXspatial = {
  /**
   * Initialise a target element as an OpenLayers map
   *
   * @param {object} opt_options JSON that should contain at least a target element ID, an imageURL, an eventURL
   */
  init: function(opt_options) {
    var options = opt_options || {};

    // Make our custom control inherit prototype methods from Control
    ol.inherits(FOXspatial._tryAgainControl, ol.control.Control);

    var target = $("#" + options.target);

    var extent = [
      0,
      0,
      target.width(),
      target.height()
    ];

    var viewExtentBuffer = 10; // Pixels of layer to keep on screen despite dragging
    var viewExtent = [
      (extent[0] - (target.width()/2)) + viewExtentBuffer,
      (extent[1] - (target.height()/2)) + viewExtentBuffer,
      (extent[2] + (target.width()/2)) - viewExtentBuffer,
      (extent[3] + (target.height()/2)) - viewExtentBuffer
    ];

    var projection = new ol.proj.Projection({
      code: "canvas-image",
      units: "pixels",
      extent: extent
    });

    var imageSource = new ol.source.ImageStatic({
      url: options.imageURL,
      projection: projection,
      imageExtent: extent
    });

    imageSource.on('imageloaderror', function (event) {
      FOXspatial._handleError(map);
    });

    var view = new ol.View({
      center: ol.extent.getCenter(extent),
      zoom: 1,
      resolution: 1,
      resolutions: [1.5, 1, 0.5],
      projection: projection,
      extent: viewExtent,
      enableRotation: false
    });

    var map = new ol.Map({
      target: options.target,
      layers: [
        new ol.layer.Image({
          source: imageSource
        })
      ],
      view: view,
      controls: [new ol.control.Zoom()]
    });
    map.setProperties(options);

    map.set("tryAgainControl", new FOXspatial._tryAgainControl());
    map.set("originalView", view);

    map.on("moveend", FOXspatial._handleMoveEnd, map);

    target.data("ol-map", map);
  },

  /**
   * Handle the moveend event which is fired from the map object.
   * If an actual map move has happened check to see if the map has been zoomed at all and then send an AJAX request to
   * the maps eventURL property before refreshing the image.
   *
   * @param e Event raise by the Map object after a moveend
   * @private
   */
  _handleMoveEnd : function(e) {
    var frameState = e.frameState;
    if (frameState) {
      var center = frameState.viewState.center;
      var resolution = frameState.viewState.resolution;

      // Only do something when the centre has moved and the resolution (zoom-level) has changed
      if (!(center[0] == (frameState.size[0]/2) && center[1] == (frameState.size[1]/2) && resolution == 1)) {
        var zoomDirection = "none";
        if (resolution < 1) {
          zoomDirection = "in";
        }
        else if (resolution > 1) {
          zoomDirection = "out";
        }

        // POST some JSON about this event to the eventURL
        $.ajax({
          method: "POST",
          url: e.target.get("eventURL"),
          data: {
            "event": "move",
            "zoomDirection": zoomDirection,
            "coord": center[0] + ',' + ((frameState.size[1]/2)+((frameState.size[1]/2)-center[1])),
            "canvasUsageID": e.map.get("canvasUsageID"),
            "canvasHash": e.map.get("canvasHash"),
            "imageWidth": frameState.size[0],
            "imageHeight": frameState.size[1]
          },
          cache: false,
          dataType: "json"
        })
        .done(function(result) {
          if (result.result === "OK") {
            // Update the hash if it's changed
            if (result.canvasHash && result.canvasHash !== e.map.get("canvasHash")) {
              e.map.set("canvasHash", result.canvasHash);
            }
            FOXspatial._refreshImage(e.map, result.changeNumber);
          }
          else {
            FOXspatial._handleError(e.map);
          }
        })
        .fail(function() {
          FOXspatial._handleError(e.map);
        });
      }
    }
  },

  /**
   * Refresh the static image layer in OpenLayers. This isn't quite what OpenLayers is designed for but via this method
   * it can be made to work the way FOX's spatial packages are designed.
   *
   * @param {ol.Map} map Map instance to refresh the image on
   * @param {string} uniqueHash Unique string to append to the render imageURL for unique images
   * @private
   */
  _refreshImage: function(map, uniqueHash) {
    var imageURL = map.get("imageURL");
    if (uniqueHash) {
      imageURL = imageURL + (imageURL.indexOf("?")>0?"&":"?") + uniqueHash;
    }
    else {
      imageURL = imageURL + (imageURL.indexOf("?")>0?"&":"?") + (new Date().getTime());
    }

    var view = map.get("originalView");
    var projection = view.getProjection();
    var extent = projection.getExtent();

    // Un-set the moveend handler temporarily so that the view reset later doesn't trigger another event
    map.un("moveend", FOXspatial._handleMoveEnd, map);

    // Create a new static image source, appending timestamp to the URL to make sure we don't get a cached copy
    var newSource = new ol.source.ImageStatic({
      url: imageURL,
      projection: projection,
      imageExtent: extent
    });

    // When the new image loads have an event handler to make sure everything is reset for this image
    newSource.on("imageloadend", function(event) {
      // Remove all but the newSource layer
      while (map.getLayers().getLength() > 1) {
        map.removeLayer(map.getLayers().item(0));
      }

      // Reset view
      view.setZoom(1);
      view.setResolution(1);
      view.setCenter(ol.extent.getCenter(extent));
      map.setView(view);

      // Remove the Try Again control on successful load
      map.removeControl(map.get("tryAgainControl"));

      // Put the event handler back on the map so users can pan & zoom
      map.on("moveend", FOXspatial._handleMoveEnd, map);
    });

    // If an error happens during image load, handle that gracefully
    newSource.on('imageloaderror', function (event) {
      FOXspatial._handleError(map);
    });

    // Add the new static image layer to the map
    map.addLayer(new ol.layer.Image({
      source: newSource
    }));
  },

  /**
   * Gracefully handle errors by creating a vector layer for the map letting the user know there was an issue
   *
   * @param {ol.Map} map Map instance to display an error layer
   * @private
   */
  _handleError: function(map) {
    // Un-set the moveend handler so we don't get more errors
    map.un("moveend", FOXspatial._handleMoveEnd, map);

    // Error image is 60x60px
    var errorExtent = [
      0,
      0,
      60,
      60
    ];

    var errorProjection = new ol.proj.Projection({
      code: "canvas-image",
      units: "pixels",
      extent: errorExtent
    });

    var errorView = new ol.View({
      center: ol.extent.getCenter(errorExtent),
      zoom: 1,
      resolution: 1,
      resolutions: [1],
      projection: errorProjection,
      extent: [30,30,30,30],
      enableRotation: false
    });

    // Create a new static image source, appending timestamp to the URL to make sure we don't get a cached copy
    var errorImageSource = new ol.source.ImageStatic({
      url: map.get("errorImageURL"),
      projection: errorProjection,
      imageExtent: errorExtent
    });

    map.setView(errorView);

    // Remove all layers
    while (map.getLayers().getLength() > 0) {
      map.removeLayer(map.getLayers().item(0));
    }

    // Add the new static image layer to the map
    map.addLayer(new ol.layer.Image({
      source: errorImageSource
    }));

    map.addControl(map.get("tryAgainControl"));
  },

  /**
   * Custom control to refresh the image in case of an error
   *
   * @param opt_options
   */
  _tryAgainControl: function(opt_options) {
    var options = opt_options || {};

    var this_ = this;
    var handleTryAgain = function(e) {
      FOXspatial._refreshImage(this_.getMap());
    };

    var element = $("<div></div>")
        .addClass("try-again ol-unselectable ol-control")
        .append($("<button>")
          .text("Error, Try Again?")
          .on("click", handleTryAgain));

    ol.control.Control.call(this, {
      element: element[0],
      target: options.target
    });
  }
};
