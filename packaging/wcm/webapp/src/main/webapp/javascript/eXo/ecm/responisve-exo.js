require(["SHARED/jquery"], function($) {
	// add toggle right button
	$(".OfficeMiddleTDContainer").append("<a href='javascript:void(0)' class='mobile-visible  toggle-right-bar'><i class='uiIconArrowLeft'></i><i class='uiIconArrowRight' ></i></a>");
	
	var taApp = {}; 

	// Toggle left bar
	$('.toggle-left-bar').on('click',function(){
		
		if($('body').hasClass('open-left-bar')) {
			taApp.hideLeftPanel();	
		}
		else {
			taApp.showLeftPanel();	
		}
	})

	$('.RightBodyTDContainer').on('click',function(){
		taApp.hideLeftPanel();	
	})

	// Toggle right  bar
	$('.toggle-right-bar').on('click',function(){
		if($('body').hasClass('hidden-right-bar')) {
			taApp.hideRightPanel();		
		}
		else {
			taApp.showRightPanel();
		}
	})



	// show left navigation	
  	taApp.showLeftPanel = function() {
    	var leftNavi= $('.LeftNavigationTDContainer');
    	$('body').addClass('open-left-bar');    
    	$(leftNavi).addClass('expanded');			
    };

    // hidden left navigation
 	taApp.hideLeftPanel = function() {
    	var leftNavi= $('.LeftNavigationTDContainer');
    	$('body').removeClass('open-left-bar');	
    	$(leftNavi).removeClass('expanded');		
    };
    // Show right  navigation	
    taApp.showRightPanel = function() {
    	var RightNavi= $('.OfficeRightTDContainer');
    	$('body').addClass('hidden-right-bar');    	
    	$(RightNavi).addClass('expanded');			
    };
    // hidden Right navigation
 	taApp.hideRightPanel = function() {
    	var RightNavi= $('.OfficeRightTDContainer');
    	$('body').removeClass('hidden-right-bar');	
    	$(RightNavi).removeClass('expanded');		
    };


    // display sub menu on mobile

     $('.dropdown-submenu > a').on('click', function(evt) {     	  
		 var _w = Math.max($(window).width());
		 
		 if ( _w < 1025 ) {
		 	 evt.stopPropagation();
     	     evt.preventDefault();
		 	var parent = $(this).parent().addClass('current').parent('ul');
		 	parent.find('>li').hide();	
		 	parent.append($('<li class="back-item"><a href="javascript:void(0)" ><i class="uiIconArrowLeft" style=" margin-right: 2px;"></i>Back</a></li>')
		 		.on('click', function(evt) {
		 		  	evt.stopPropagation();
			 		parent.find('.current').removeClass('current').find('ul.dropdown-menu:first')
			 		.append(parent.find('.current-child').removeClass('current-child'));
			 		$(this).remove();
			 		parent.find('>li').show();
			}));
		 	var sub = $(this).parent().find('.dropdown-menu:first > li').addClass('current-child');
		 	parent.append(sub);
		 }
	})
    
   /*taApp.equalHeight= function (group) {
	   tallest = 0;
	   group.each(function() {
	      thisHeight = $(this).height();
	      if(thisHeight > tallest) {
	         tallest = thisHeight;
	      }
	   });
   	group.height(tallest);

	}
	taApp.equalHeight($(".UITableColumn td"));*/


	// function accordion for left navigation
    
 taApp.left_nav_accordion=function(){
		var companyNav = $('.uiCompanyNavigationPortlet .title.accordionBar').addClass('active');	
		$('.title.accordionBar').prepend('<i class="uiIconArrowRight pull-right"></i>');	
		$('.uiCompanyNavigationPortlet .accordionCont').addClass('active').show();
		$('#LeftNavigation .accordionBar').click(function(){	
				
			var subContent = $(this).next();
			if (subContent.is(':visible')) {
				return false;
			}
			else {
				 $('.accordionBar').removeClass('active');
			 	$(this).addClass('active')
				$('.accordionCont').removeClass('active').hide();
				$(this).next().addClass('active').slideDown();
			}
			return false;
		});
	}
	// call accordion function
	taApp.left_nav_accordion();
})

