/**
 * General methods to access REST content.
 */
WCMServices = function() {
	WCMServices.prototype.parseXML = function(text) {
		var xmlDoc;
		try //Internet Explorer
		  {
			  xmlDoc=new ActiveXObject("Microsoft.XMLDOM");
			  xmlDoc.async="false";
			  xmlDoc.loadXML(text);
		  }
		catch(e)
		  {
			  try //Firefox, Mozilla, Opera, etc.
			  {
				  parser=new DOMParser();
				  xmlDoc=parser.parseFromString(text,"text/xml");
			  }
			  catch(e)
			  {
				  alert(e.message);
				  return;
			  }
		  }
		return xmlDoc;
	};
}	
eXo.wcm.services = new WCMServices();

/**
 * Vote service
 */
Vote = function(){
	
	Vote.prototype.getVote = function(repository, workspace, path) {
		var retText = ajaxAsyncGetRequest(eXo.ecm.WCMUtils.getRestContext() + "/contents/vote/getVote/?repositoryName="+repository+"&workspaceName="+workspace+"&jcrPath="+path, false);
		var doc = parseXML(retText);
		rate = doc.getElementsByTagName("rate")[0].childNodes[0].nodeValue;
		total = doc.getElementsByTagName("total")[0].childNodes[0].nodeValue;
		
		document.all['result'].innerHTML = rate+' / '+total;
	};
	
	Vote.prototype.addVote = function(repository, workspace, path, vote, lang) { 
		ajaxAsyncGetRequest(eXo.ecm.WCMUtils.getRestContext() + "/contents/vote/postVote/?repositoryName="+repository+"&workspaceName="+workspace+"&jcrPath="+path+"&vote="+vote+"&lang="+lang, false);
	};

	Vote.prototype.showVoteWindow = function(div) {
		var target = document.getElementById(div);
		target.style.display = "inline";
//		var l1 = '<a href="#" onclick="javascript:eXo.wcm.vote.addVote("1.0");">1</a>';
		var html = '<div style="border:1px solid #FF0000;">';
		for (i=1 ; i<=5 ; i++) {
			html += '<a href="#" onclick="javascript:eXo.wcm.vote.addVote(\'repository\', \'collaboration\', \'/sites%20content/live/classic/categories/Classic/test\',  \''+i+'.0\', \'en\')">'+i+'</a>';
		}
		html += '</div>';
		target.innerHTML = html;

	};
	
}
eXo.wcm.vote = new Vote();
