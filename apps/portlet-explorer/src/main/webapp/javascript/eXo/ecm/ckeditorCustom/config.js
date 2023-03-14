/**
 * @license Copyright (c) 2003-2015, CKSource - Frederico Knabben. All rights reserved.
 * For licensing, see LICENSE.md or http://ckeditor.com/license
 */

// force compatible version in any case to make sure the editor will initialize
CKEDITOR.env.isCompatible = true;
// force env when using the eXo Android app (the eXo Android app uses a custom user agent which
// is not known by CKEditor and which makes it not initialize the editor)
var userAgent = navigator.userAgent.toLowerCase();
if(userAgent != null && userAgent.indexOf('exo/') == 0 && userAgent.indexOf('(android)') > 0) {
    CKEDITOR.env.mobile = true;
    CKEDITOR.env.chrome = true;
    CKEDITOR.env.gecko = false;
    CKEDITOR.env.webkit = true;
}

CKEDITOR.eXoPath = CKEDITOR.basePath.substr(0, CKEDITOR.basePath.indexOf("ckeditor/"));

CKEDITOR.editorConfig = function( config ) {

    // %REMOVE_START%
    // The configuration options below are needed when running CKEditor from source files.
    CKEDITOR.plugins.addExternal('simpleLink','/commons-extension/eXoPlugins/simpleLink/','plugin.js');
    CKEDITOR.plugins.addExternal('simpleImage','/commons-extension/eXoPlugins/simpleImage/','plugin.js');
	CKEDITOR.plugins.addExternal('content','/eXoWCMResources/eXoPlugins/content/','plugin.js');
	CKEDITOR.plugins.addExternal('insertPortalLink','/commons-extension/eXoPlugins/insertPortalLink/','plugin.js');
	CKEDITOR.plugins.addExternal('wcmImage','/eXoWCMResources/eXoPlugins/wcmImage/','plugin.js');
  
    //TODO we should ensure adding these plugins
    config.extraPlugins = 'simpleLink,simpleImage,content,insertPortalLink,wcmImage';

    // Move toolbar below the test area
    config.toolbarLocation = 'bottom';

    // style inside the editor
    config.contentsCss = '/commons-extension/ckeditorCustom/contents.css';

    config.enterMode = CKEDITOR.ENTER_BR;
	
	config.removePlugins = 'scayt,wsc';
	config.toolbarCanCollapse = false;
	config.skin = 'moono-exo,/commons-extension/ckeditor/skins/moono-exo/';
	config.allowedContent = true;
	config.resize_enabled = true;
	config.language = eXo.env.portal.language || 'en';
	config.pasteFromWordRemoveFontStyles = false;
	config.pasteFromWordRemoveStyles = false;
        config.syntaxhighlight_lang = 'java';
	config.syntaxhighlight_hideControls = true;
	CKEDITOR.dtd.$removeEmpty['i'] = false;

    config.toolbar = [
        ['Bold','Italic','RemoveFormat',],
        ['-','NumberedList','BulletedList','Blockquote'],
        ['-','simpleLink', 'simpleImage'],
    ] ;
	
	config.toolbar_CompleteWCM = [
		['Source','Templates'],
		['Cut','Copy','Paste','PasteText','PasteFromWord','-','Find','Replace','SelectAll','-','Undo','Redo'],
		['Flash','Table','SpecialChar', 'content.btn', 'WcmImage'], 		['Bold','Italic','Underline','Strike','-','JustifyLeft','JustifyCenter','JustifyRight','JustifyBlock','-','NumberedList','BulletedList','-','TextColor','BGColor','-','RemoveFormat'],
		['Link','insertPortalLink.btn','Unlink','Anchor'],		
		['Styles','Format','Font','FontSize', '-' ,'Maximize']
	] ;
	
	config.toolbar_BasicWCM = [
		['Source','-','Bold','Italic','Underline','Strike'],
    ['-','NumberedList','BulletedList','Outdent','Indent'],
		['JustifyLeft','JustifyCenter','JustifyRight','JustifyBlock','Blockquote'],
		['-','Link','Unlink','insertPortalLink.btn','content.btn', 'WcmImage'],
    ['-','Maximize','ShowBlocks','Styles','Format','Font','FontSize']
	] ;

	config.toolbar_SuperBasicWCM = [
		   ['Source','-','Bold','Italic','Underline'],
		   ['-','JustifyLeft','JustifyCenter','JustifyRight','JustifyBlock'],
		   ['-','Link','Unlink','insertPortalLink.btn','content.btn', 'WcmImage'],
	] ;

    config.height = 80;

    config.autoGrow_onStartup = true;
    config.autoGrow_minHeight = 80;

    config.language = eXo.env.portal.language || 'en';
};
