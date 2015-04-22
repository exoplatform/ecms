require(["/eXoResources/javascript/jquery-1.7.1.js"], function(gj) {
	$("#HomeLink").prepend("<a href='javascript:void(0)' class='mobile-visible  toggle-mobile'>&#8211;</a>");
	$("#OfficeRight").prepend("<a href='javascript:void(0)' class='mobile-visible  toggle-rightbar'>&nbsp;</a>");

	// toggle left menu
	$('.toggle-mobile').on('click',function(){
		var leftNavi= $('.LeftNavigationTDContainer');
		$('body').toggleClass('open-left-bar');		
	})
})
