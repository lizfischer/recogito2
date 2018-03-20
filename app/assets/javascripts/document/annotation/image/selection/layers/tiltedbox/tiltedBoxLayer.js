define([
  'document/annotation/image/selection/layers/geom2D',
  'document/annotation/image/selection/layers/layer',
  'document/annotation/image/selection/layers/style',
  'document/annotation/image/selection/layers/tiltedbox/tiltedBox'
], function(Geom2D, Layer, Style, TiltedBox) {

      /** Shorthand **/
  var TWO_PI = 2 * Math.PI;

  var TiltedBoxLayer = function(olMap) {

    var self = this,

        annotations = [],

        computeSize = function(annotation) {
          var anchor = TiltedBox.parseAnchor(annotation.anchor);
          return Geom2D.getPolygonArea(anchor.coords);
        },

        getAnnotationAt = function(e) {
          var coord = e.coordinate, found = [];

          jQuery.each(annotations, function(idx, annotation) {
            var anchor = TiltedBox.parseAnchor(annotation.anchor, 5);
            if(Geom2D.intersects(coord[0], coord[1], anchor.coords))
              found.push({
                annotation: annotation,
                imageBounds: Geom2D.coordsToBounds(rect)
              });
          });

          // Sort by size, ascending
          found.sort(function(a, b) {
            var sizeA = computeSize(a.annotation),
                sizeB = computeSize(b.annotation);

            return sizeA - sizeB;
          });
          return found[0];
        },

        findById = function(id) {
          var foundAnnotation;
          jQuery.each(annotations, function(idx, annotation) {
            if (annotation.annotation_id === id) {
              foundAnnotation = annotation;
              return false; // Break loop
            }
          });

          if (foundAnnotation)
            return {
              annotation: foundAnnotation,
              imageBounds: Geom2D.coordsToBounds(TiltedBox.parseAnchor(foundAnnotation.anchor).coords)
            };
        },

        addAnnotation = function(annotation, renderImmediately) {
          annotations.push(annotation);
          if (renderImmediately) layer.getSource().changed();
        },

        render = function() {
          layer.getSource().changed();
        },

        drawOne = function(annotation, extent, scale, ctx, color) {
          var setStyles = function() {
                ctx.fillStyle = color;
                ctx.strokeStyle = color;
                ctx.lineWidth = 1;
              },

              // Helper function to trace a rectangle path
              traceRect = function() {
                ctx.moveTo(rect[0][0], rect[0][1]);
                ctx.lineTo(rect[1][0], rect[1][1]);
                ctx.lineTo(rect[2][0], rect[2][1]);
                ctx.lineTo(rect[3][0], rect[3][1]);
                ctx.lineTo(rect[0][0], rect[0][1]);
              },

              drawBox = function() {
                // Fill
                ctx.globalAlpha = Style.BOX_FILL_OPACITY;
                ctx.beginPath();
                traceRect();
                ctx.fill();
                ctx.closePath();

                // Outline
                ctx.globalAlpha = Style.BOX_STROKE_OPACITY;
                ctx.beginPath();
                traceRect();
                ctx.stroke();
                ctx.closePath();
              },

              // Note: currently not used, but we may change drawing style later!
              drawBaseLine = function() {

                ctx.lineWidth = Style.BOX_BASELINE_WIDTH;
                ctx.beginPath();
                ctx.moveTo(rect[0][0], rect[0][1]);
                ctx.lineTo(rect[1][0], rect[1][1]);
                ctx.stroke();
                ctx.closePath();
              },

              // Note: currently not used, but we may change drawing style later!
              drawAnchorDot = function() {
                ctx.beginPath();
                ctx.arc(rect[0][0], rect[0][1], Style.BOX_ANCHORDOT_RADIUS, 0, TWO_PI);
                ctx.fill();
                ctx.closePath();
              },

              rect = TiltedBox.parseAnchor(annotation.anchor).coords.map(function(pt) {
                return [ scale * (pt[0] - extent[0]), scale * (extent[3] - pt[1]) ];
              });

          rect.push(rect[0]); // Close the rect path

          setStyles();
          drawBox();
        },

        /** Drawing loop that renders all annotations to the drawing area **/
        redrawAll = function(extent, resolution, pixelRatio, size, projection) {
          var canvas = document.createElement('canvas'),
              ctx = canvas.getContext('2d');

          canvas.width = size[0];
          canvas.height = size[1];

          // TODO optimize so that stuff outside the visible area isn't drawn
          jQuery.each(annotations, function(idx, annotation) {
            drawOne(annotation, extent, pixelRatio / resolution, ctx, Style.COLOR_RED);
          });

          return canvas;
        },

        layer = new ol.layer.Image({
          source: new ol.source.ImageCanvas({
            canvasFunction: redrawAll,
            projection: 'ZOOMIFY'
          })
        }),

        refreshAnnotation = function(annotation) {
          // TODO style change depending on annotation properties
        },

        removeAnnotation = function(annotation) {
          var idx = annotations.indexOf(annotation);
          if (idx >= 0) {
            annotations.splice(idx, 1);
            render();
          }
        },

        convertSelectionToAnnotation = function(selection) {
          addAnnotation(selection.annotation, true);
        },

        emphasiseAnnotation = function(annotation) {
          // TODO style change?
        };

    olMap.addLayer(layer);

    this.computeSize = computeSize;
    this.getAnnotationAt = getAnnotationAt;
    this.findById = findById;
    this.addAnnotation = addAnnotation;
    this.render = render;
    this.refreshAnnotation = refreshAnnotation;
    this.removeAnnotation = removeAnnotation;
    this.convertSelectionToAnnotation = convertSelectionToAnnotation;
    this.emphasiseAnnotation = emphasiseAnnotation;

    Layer.apply(this, [ olMap ]);
  };
  TiltedBoxLayer.prototype = Object.create(Layer.prototype);

  return TiltedBoxLayer;

});
