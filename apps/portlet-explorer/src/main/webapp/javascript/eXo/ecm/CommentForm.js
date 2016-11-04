(function ($, _) {
     var CommentForm = function() {}
    //
     CommentForm.prototype.init = function(placeholder) {
        // TODO this line is mandatory when a custom skin is defined, it should not be mandatory
        CKEDITOR.basePath = '/commons-extension/ckeditor/';
        $('textarea#comment').ckeditor({
            //TODO we should ensure adding external plugins for link and image
            customConfig: '/ecmexplorer/javascript/eXo/ecm/ckeditorCustom/config.js',
            placeholder: placeholder,
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
    
    var peopleSearchCached = {};
    var lastNoResultQuery = false;
    
    $('body').suggester('addProvider', 'exo:people', function(query, callback) {
        if (lastNoResultQuery && query.length > lastNoResultQuery.length) {
            if (query.substr(0, lastNoResultQuery.length) === lastNoResultQuery) {
                callback.call(this, []);
                return;
            }
        }
        if (peopleSearchCached[query]) {
            callback.call(this, peopleSearchCached[query]);
        } else {
            var url = window.location.protocol + '//' + window.location.host + '/' + eXo.social.portal.rest + '/social/people/getprofile/data.json?search=' + query;
            $.getJSON(url, function(responseData) {
                responseData = _.filter(responseData, function(item) {
                    return item.name.toLowerCase().indexOf(query.toLowerCase()) > -1;
                });

                var result = [];
                for (var i = 0; i < responseData.length; i++) {
                    var d = responseData[i];
                    var item = {
                        uid: d.id.substr(1),
                        name: d.name,
                        avatar: d.avatar
                    };
                    result.push(item);
                }

                peopleSearchCached[query] = result;
                if (peopleSearchCached[query].length == 0) {
                    lastNoResultQuery = query;
                } else {
                    lastNoResultQuery = false;
                }
                callback.call(this, peopleSearchCached[query]);
            });
        }
    });
    eXo.ecm.CommentForm = new CommentForm();
    return {
        CommentForm : eXo.ecm.CommentForm
    };

})($, mentions._);