(function ($) {
     var CommentForm = function() {}
    //
     CommentForm.prototype.init = function() {
        // TODO this line is mandatory when a custom skin is defined, it should not be mandatory
        CKEDITOR.basePath = '/commons-extension/ckeditor/';
        $('textarea#comment').ckeditor({
            //TODO we should ensure adding external plugins for link and image
            customConfig: '/ecmexplorer/ecmexplorer/config.js',
            //placeholder: UIActivity.commentPlaceholder != null ? UIActivity.commentPlaceholder : window.eXo.social.I18n.mentions.defaultMessage,
            on: {
                instanceReady: function (evt) {
                    // Hide the editor top bar.
                    document.getElementById(evt.editor.id + '_bottom').style.display = 'none';
                    document.getElementById(evt.editor.id + '_contents').style.height = '47px';
                },
                focus: function (evt) {
                    // Show the editor top bar.
                    document.getElementById(evt.editor.id + '_bottom').style.display = 'block';
                    document.getElementById(evt.editor.id + '_contents').style.height = '150px';
                },
                blur: function (evt) {
                    // Show the editor top bar.
                    document.getElementById(evt.editor.id + '_bottom').style.display = 'none';
                    document.getElementById(evt.editor.id + '_contents').style.height = '47px';
                },
                change: function (evt) {
                    var newData = evt.editor.getData();
                    if (newData && newData.length > 0) {
                        var elId = this.element.$.id.replace('CommentTextarea', '');
                        $('#CommentButton' + elId).removeAttr("disabled");
                    } else {
                        $('#CommentButton' + elId).prop("disabled", true);
                    }
                }
            }
        });
    }
    eXo.ecm.CommentForm = new CommentForm();
    return {
        CommentForm : eXo.ecm.CommentForm
    };
})($);