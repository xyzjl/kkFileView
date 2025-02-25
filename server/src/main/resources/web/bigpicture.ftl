<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <title>预览 - ${file.name}</title>
    <script src="openseadragon/openseadragon.min.js"></script>
    <#include "*/commonHeader.ftl">
    <style>
        body {
            background-color: #404040;
        }

        #openseadragon1 {
            width: 100%;
            height: 100%
        }

        .loading {
            position: absolute;
            left: 50px;
            top: 50px;
            font-size: 30px;
            z-index: 10;
        }
    </style>
</head>
<body>
<div id="openseadragon1"></div>
<div class="loading">LOADING!</div>
<script>
  // If you like my work, please consider supporting it at https://www.patreon.com/iangilman

  // Drag an image to move it around. Drag the background to pan.

  var currentType = '${currentType}';

  var source;
  if (currentType === 'tile') {
    source = {
      Image: {
        xmlns: 'http://schemas.microsoft.com/deepzoom/2008',
        Url: "${dziBaseUrl!}",
        Format: 'png',
        Overlap: '0',
        TileSize: '512',
        Size: {
          Width: "${currentWidth!}",
          Height: "${currentHeight!}"
        }
      }
    };
  } else {
    source = {
      type: 'image',
      url: '${currentUrl}',
      buildPyramid: false
    };
  }

  var viewer = OpenSeadragon({
    id: 'openseadragon1',
    prefixUrl: './openseadragon/images/',
    // Initial rotation angle
    degrees: 0,
    // Show rotation buttons
    showRotationControl: true,
    // Enable touch rotation on tactile devices
    tileSources: source
  });

  function areAllFullyLoaded () {
    var tiledImage;
    var count = viewer.world.getItemCount();
    for (var i = 0; i < count; i++) {
      tiledImage = viewer.world.getItemAt(i);
      if (!tiledImage.getFullyLoaded()) {
        return false;
      }
    }
    return true;
  }

  var isFullyLoaded = false;

  function updateLoadingIndicator () {
    // Note that this function gets called every time isFullyLoaded changes, which it will do as you
    // zoom and pan around. All we care about is the initial load, though, so we are just hiding the
    // loading indicator and not showing it again.
    if (isFullyLoaded) {
      document.querySelector('.loading').style.display = 'none';
    }
  }

  viewer.world.addHandler('add-item', function (event) {
    var tiledImage = event.item;
    tiledImage.addHandler('fully-loaded-change', function () {
      var newFullyLoaded = areAllFullyLoaded();
      if (newFullyLoaded !== isFullyLoaded) {
        isFullyLoaded = newFullyLoaded;
        updateLoadingIndicator();
      }
    });
  });

  /*初始化水印*/
  window.onload = function () {
    initWaterMark();
  };
</script>

</body>

</html>
