require(["/eXoResources/javascript/jquery-1.7.1.js"], function(gj) {
	$("#HomeLink").prepend("<a href='javascript:void(0)' class='mobile-visible pull-left toggle-left-bar'><span>&#45;</span><span>&#45;</span><span>&#45;</span></a>");
	$("#OfficeRight").prepend("<a href='javascript:void(0)' class='mobile-visible  toggle-right-bar'><span>&#45;</span><span>&#45;</span><span>&#45;</span></a>");

	// Toggle left bar
	$('.toggle-left-bar').on('click',function(){
		var leftNavi= $('.LeftNavigationTDContainer');
		$('body').toggleClass('open-left-bar');		
		$(leftNavi).toggleClass('expanded');		
	})
	// Toggle right bar
	$('.toggle-right-bar').on('click',function(){
		var leftNavi= $('.OfficeRightTDContainer ');
		$('body').toggleClass('open-right-bar');		
		$(leftNavi).toggleClass('expanded');		
	})
})
