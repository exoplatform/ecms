// set thumbnail size;
FCKConfig.thumbnailWidth = 80;
FCKConfig.thumbnailHeight = 80;

// set eXo plugin path;
FCKConfig.eXoPath = FCKConfig.BasePath.substr(0, FCKConfig.BasePath.length - 7) + "exo/" ;
FCKConfig.Plugins.Add('insertImage', 'en,fr,vi', FCKConfig.eXoPath + "plugins/") ;
FCKConfig.Plugins.Add('insertDocument', 'en,fr,vi', FCKConfig.eXoPath + "plugins/") ;
FCKConfig.Plugins.Add('insertPortalLink', 'en,fr,vi', FCKConfig.eXoPath + "plugins/") ;
FCKConfig.Plugins.Add('insertGadget', 'en,fr,vi', FCKConfig.eXoPath + "plugins/") ;
FCKConfig.Plugins.Add('insertContent', 'en,fr,vi', FCKConfig.eXoPath + "plugins/") ;

//config style
FCKConfig.EditorAreaCSS = '';
FCKConfig.EditorAreaStyles  = 'body{background: #ffffff; margin: 0px;}' ;

FCKConfig.SkinPath = FCKConfig.BasePath + 'skins/default/' ;

FCKConfig.ToolbarSets["CompleteWCM"] = [
	['Source','Templates','-','FitWindow','ShowBlocks'],
	['Cut','Copy','PasteText','-','SpellCheck','-','Undo','Redo'],
	['WCMInsertGadget','Flash','Table','SpecialChar', 'WCMInsertContent'],
	'/',
	['Bold','Italic','Underline','StrikeThrough','-','JustifyLeft','JustifyCenter','JustifyRight','JustifyFull','-','OrderedList','UnorderedList','-','TextColor','BGColor','-','RemoveFormat'],
	['Link','WCMInsertPortalLink','Unlink','Anchor'],
	'/',
	['Style','FontFormat','FontName','FontSize']
] ;

FCKConfig.ToolbarSets["BasicWCM"] = [
	['Source','-','Bold','Italic','Underline','StrikeThrough','-','OrderedList','UnorderedList','Outdent','Indent'],
	['JustifyLeft','JustifyCenter','JustifyRight','JustifyFull'],
	['Blockquote','-','Link','Unlink','WCMInsertPortalLink','WCMInsertContent','-','FitWindow','ShowBlocks'],	
	['Style','FontFormat','FontName','FontSize']
] ;

FCKConfig.ToolbarSets["SuperBasicWCM"] = [
     ['Source','-','Bold','Italic','Underline'],
     ['-','JustifyLeft','JustifyCenter','JustifyRight','JustifyFull'],
     ['-','Link','Unlink','WCMInsertPortalLink','WCMInsertGadget','WCMInsertContent'],	
] ;

//eXoPlugin config
window.eXoPlugin = {
	init: function() {
		with (window.parent.eXo.env.portal) {
			this.portalName = portalName;
			this.context = context;
			this.accessMode = accessMode;
			this.userId = userName;
			this.WCMUtils = window.parent.eXo.ecm.WCMUtils;
		}
		
		var parentLocation = window.parent.location;
		this.hostName = parentLocation.href.substring(0, parentLocation.href.indexOf(parentLocation.pathname));
		
		this.eXoFileManager = {
			Connector: eXoPlugin.WCMUtils.getRestContext() + "/fckconnector/jcr/",
			ResourceType : "File"
		};				
		this.ExoPortalLinkBrowserURL = FCKConfig.eXoPath + 'explorer/explorer.html?Type=PortalLink&Connector=' + eXoPlugin.WCMUtils.getRestContext() + '/portalLinks/&disableUploading=true&disableCreatingFolder=true' ;		
//-Set link for Insert/Edit Link button
		FCKConfig.LinkBrowser = true ;
		FCKConfig.LinkBrowserURL = FCKConfig.eXoPath + 'explorer/explorer.html?Type=PortalLink&Connector=' + eXoPlugin.WCMUtils.getRestContext() + '/portalLinks/&disableUploading=true&disableCreatingFolder=true' ;
		FCKConfig.LinkBrowserWindowWidth	= FCKConfig.ScreenWidth * 0.7 ;		// 70%
		FCKConfig.LinkBrowserWindowHeight	= FCKConfig.ScreenHeight * 0.7 ;	// 70%		
//Set Link for browser URL
		FCKConfig.ImageBrowser = true ;
		FCKConfig.ImageBrowserURL = FCKConfig.eXoPath + 'content/content.html?Type=File&Connector=' + eXoPlugin.WCMUtils.getRestContext() + '/wcmDriver/getDrivers?repositoryName=repository&workspaceName=collaboration&browserType=txtUrl';
		FCKConfig.ImageBrowserWindowWidth  = FCKConfig.ScreenWidth * 0.7 ;	// 70% ;
		FCKConfig.ImageBrowserWindowHeight = FCKConfig.ScreenHeight * 0.7 ;	// 70% ;
//Set Link for browser image when right click on an image and select Image Properties
		FCKConfig.ImageBrowserLink = true ;
		FCKConfig.ImageBrowserLink = FCKConfig.eXoPath + 'content/content.html?Type=File&Connector=' + eXoPlugin.WCMUtils.getRestContext() + '/wcmDriver/getDrivers?repositoryName=repository&workspaceName=collaboration&browserType=txtLnkUrl';
		FCKConfig.ImageBrowserLinkWindowWidth  = FCKConfig.ScreenWidth * 0.7 ;	// 70% ;
		FCKConfig.ImageBrowserLinkWindowHeight = FCKConfig.ScreenHeight * 0.7 ;	// 70% ;
//Set link for Insert/edit Flash button links
		FCKConfig.FlashBrowser = true ;
		FCKConfig.FlashBrowserURL = FCKConfig.eXoPath + 'content/content.html?Type=File&Connector=' + eXoPlugin.WCMUtils.getRestContext() + '/wcmDriver/getDrivers?repositoryName=repository&workspaceName=collaboration&browserType=txtUrl';		
		FCKConfig.FlashBrowserWindowWidth  = FCKConfig.ScreenWidth * 0.7 ;	//70% ;
		FCKConfig.FlashBrowserWindowHeight = FCKConfig.ScreenHeight * 0.7 ;	//70% ;		
//detect user language
		this.userLanguage = FCK.Language.GetActiveLanguage() || "en";
	},
	switchToolBar: function(r) {
		var Setting = {
			oldBar: r.oldBar || "" ,
			newBar: r.newBar || "",
			useBar: r.useBar || []
		};
		with (Setting) {
			if (oldBar && newBar && FCKConfig.ToolbarSets[newBar] && FCKConfig.ToolbarSets[oldBar]) {
				FCKConfig.ToolbarSets[oldBar] = FCKConfig.ToolbarSets[newBar];
			}
		}
		//demo =>  eXoPlugin.switchToolBar({oldBar: "Basic", newBar: "eXoBar"});
	},
	addBar: function(r) {
		var Setting = {
			newBar: r.newBar || "",
			targetBar: r.targetBar || ""
		}

		with (Setting) {
			if (newBar == targetBar) return;
			if (newBar && targetBar && FCKConfig.ToolbarSets[newBar] && FCKConfig.ToolbarSets[targetBar]) {
				FCKConfig.ToolbarSets[targetBar].push("/");
				for (var o = 0; o < FCKConfig.ToolbarSets[newBar].length; ++o) {
					FCKConfig.ToolbarSets[targetBar].push(FCKConfig.ToolbarSets[newBar][o]);
				}
			}
		}
		//demo => eXoPlugin.addBar({newBar: "eXoBar", targetBar: "Basic" });
	},
	getContent: function() {
		var content = "";
		if (document.selection) {
			var range = FCK.EditorWindow.document.selection.createRange();
			content = range.text;
		} else  {
			var range = FCK.EditorWindow.getSelection();
			content = range.getRangeAt(0);
		}
		if (content) content = content.toString().replace(/^\s+|\s+$/g, "");
		if (content != "") return content;
		else return null;
	},
	loadScript: function() {
		if (arguments.length < 2) {
			return;
		} else {
			var win = arguments[0];
			var src = arguments[1];
		}
		if (!win || !win.document) return;
		var eScript = win.document.createElement("script");
		eScript.setAttribute("src", src);
		var eHead = win.document.getElementsByTagName("head")[0];
		eHead.appendChild(eScript);
	}
};

/**
	FCKCommentsProcessor
	---------------------------
	It's run after a document has been loaded, it detects all the protected source elements

	In order to use it, you add your comment parser with 
	FCKCommentsProcessor.AddParser( function )
*/
if (typeof FCKCommentsProcessor === 'undefined')
{
	FCKCommentsProcessor = FCKDocumentProcessor.AppendNew() ;
	FCKCommentsProcessor.ProcessDocument = function( oDoc )
	{
		if ( FCK.EditMode != FCK_EDITMODE_WYSIWYG )
			return ;

		if ( !oDoc )
			return ;

	//Find all the comments: <!--{PS..0}-->
	//try to choose the best approach according to the browser:
		if ( oDoc.evaluate )
			this.findCommentsXPath( oDoc );
		else
		{
			if (oDoc.all)
				this.findCommentsIE( oDoc.body ) ;
			else
				this.findComments( oDoc.body ) ;
		}

	}

	FCKCommentsProcessor.findCommentsXPath = function(oDoc) {
		var nodesSnapshot = oDoc.evaluate('//body//comment()', oDoc.body, null, XPathResult.UNORDERED_NODE_SNAPSHOT_TYPE, null );

		for ( var i=0 ; i < nodesSnapshot.snapshotLength; i++ )
		{
			this.parseComment( nodesSnapshot.snapshotItem(i) ) ;
		}
	}

	FCKCommentsProcessor.findCommentsIE = function(oNode) {
		var aComments = oNode.getElementsByTagName( '!' );
		for(var i=aComments.length-1; i >=0 ; i--)
		{
			var comment = aComments[i] ;
			if (comment.nodeType == 8 ) // oNode.COMMENT_NODE) 
				this.parseComment( comment ) ;
		}
	}

	// Fallback function, iterate all the nodes and its children searching for comments.
	FCKCommentsProcessor.findComments = function( oNode ) 
	{
		if (oNode.nodeType == 8 ) // oNode.COMMENT_NODE) 
		{
			this.parseComment( oNode ) ;
		}
		else 
		{
			if (oNode.hasChildNodes()) 
			{
				var children = oNode.childNodes ;
				for (var i = children.length-1; i >=0 ; i--) 
					this.findComments( children[ i ] );
			}
		}
	}

	// We get a comment node
	// Check that it's one that we are interested on:
	FCKCommentsProcessor.parseComment = function( oNode )
	{
		var value = oNode.nodeValue ;

		// Difference between 2.4.3 and 2.5
		var prefix = ( FCKConfig.ProtectedSource._CodeTag || 'PS\\.\\.' ) ;

		var regex = new RegExp( "\\{" + prefix + "(\\d+)\\}", "g" ) ;

		if ( regex.test( value ) ) 
		{
			var index = RegExp.$1 ;
			var content = FCKTempBin.Elements[ index ] ;

			// Now call the registered parser handlers.
			var oCalls = this.ParserHandlers ;
			if ( oCalls )
			{
				for ( var i = 0 ; i < oCalls.length ; i++ )
					oCalls[ i ]( oNode, content, index ) ;

			}

		}
	}

	/**
		The users of the object will add a parser here, the callback function gets two parameters:
			oNode: it's the node in the editorDocument that holds the position of our content
			oContent: it's the node (removed from the document) that holds the original contents
			index: the reference in the FCKTempBin of our content
	*/
	FCKCommentsProcessor.AddParser = function( handlerFunction )
	{
		if ( !this.ParserHandlers )
			this.ParserHandlers = [ handlerFunction ] ;
		else
		{
			// Check that the event handler isn't already registered with the same listener
			// It doesn't detect function pointers belonging to an object (at least in Gecko)
			if ( this.ParserHandlers.IndexOf( handlerFunction ) == -1 )
				this.ParserHandlers.push( handlerFunction ) ;
		}
	}
}
/**
	END of FCKCommentsProcessor
	---------------------------
*/

FCK["eXoPlugin"] = eXoPlugin;

eXoPlugin.init();

var WCMDialogCommand = function( name, title, url, width, height, getStateFunction, getStateParam, customValue ) {
	this.Name	= name ;
	this.Title	= title ;
	this.Url	= url ;
	this.Width	= width ;
	this.Height	= height ;
	this.CustomValue = customValue ;

	this.GetStateFunction	= getStateFunction ;
	this.GetStateParam		= getStateParam ;

	this.Resizable = false ;
}

WCMDialogCommand.prototype.Execute = function() {
	var newWindow = window.open( this.Url, this.Title, this.CustomValue + ",width=" + this.Width + ",height=" + this.Height);
	newWindow.focus();
}

WCMDialogCommand.prototype.GetState = function() {
	if ( this.GetStateFunction ) return this.GetStateFunction( this.GetStateParam ) ;
	else return FCK.EditMode == FCK_EDITMODE_WYSIWYG ? FCK_TRISTATE_OFF : FCK_TRISTATE_DISABLED ;
}
