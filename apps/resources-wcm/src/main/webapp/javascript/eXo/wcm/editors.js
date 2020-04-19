require([ "SHARED/editorbuttons", "SHARED/jquery" ], function(editorbuttons, $) {
  var $editorsDiv = $(".editors-placeholder");
  var fileId = $editorsDiv.attr("data-fileId");
  var workspace = $editorsDiv.attr("data-workspace");
  var editorButtonsLoader = editorbuttons.initPreviewButtons(fileId, workspace, "dropdown");
  editorButtonsLoader.done(function($buttonsContainer) {
    $editorsDiv.append($buttonsContainer);
  });
});