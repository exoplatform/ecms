require(["/eXoResources/javascript/jquery-1.7.1.js"], function(gj) {
	$("#HomeLink").prepend("<a href='javascript:void(0)' class='mobile-visible pull-left toggle-left-bar'><span>&#45;</span><span>&#45;</span><span>&#45;</span></a>");
	$("#OfficeRight").prepend("<a href='javascript:void(0)' class='mobile-visible  toggle-right-bar'><span>&#45;</span><span>&#45;</span><span>&#45;</span></a>");

	// Toggle left bar
	$('.toggle-left-bar').on('click',function(){
		var leftNavi= $('.LeftNavigationTDContainer');
		$('body').toggleClass('open-left-bar');		
		$(leftNavi).toggleClass('expanded');		
		left_nav_accordion();
	})
	// Toggle right bar
	$('.toggle-right-bar').on('click',function(){
		var leftNavi= $('.OfficeRightTDContainer ');
		$('body').toggleClass('open-right-bar');		
		$(leftNavi).toggleClass('expanded');		
	})
})

function left_nav_accordion(){
	var companyNav = $('.uiCompanyNavigationPortlet .title');
	var spaceNav = $('.uiSpaceNavigationPortlet .title');

	companyNav.addClass('accordionBar clearfix');
	companyNav.prepend('<i class="uiIconArrowDown pull-right"></i>');

	spaceNav.addClass('accordionBar clearfix');
	spaceNav.prepend('<i class="uiIconArrowRight pull-right"></i>');

	$('.uiCompanyNavigations').addClass('accordionCont');
	$('.spaceCont').addClass('accordionCont');
	
	var allAcc = $('#LeftNavigation .accordionCont').hide();
	$('.uiCompanyNavigationPortlet .accordionCont').show();

	$('#LeftNavigation .accordionBar').click(function(){
		var subContent = $(this).next();
		if (subContent.is(':visible')) {
			return false;
		}
		else {
			allAcc.prev().find('.uiIconArrowDown').attr('class','uiIconArrowRight pull-right');
			allAcc.slideUp();
			$(this).find('.uiIconArrowRight').attr('class','uiIconArrowDown pull-right');
			$(this).next().slideDown();
		}
	});
}
