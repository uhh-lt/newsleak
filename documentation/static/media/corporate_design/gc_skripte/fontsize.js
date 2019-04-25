var cookieManager = new Cookiemanager('cookieManager',1,'years');

var initialeGroesse = 11;
var groessen = new Array();

groessen[0] = 10;
groessen[1] = 11;
groessen[2] = 12;
groessen[3] = 13;
groessen[4] = 14;

function fontsize(modus){
	switch(modus){
		case 'inkrement':
			for (i=0; i<groessen.length; i++){
				if (groessen[i] == initialeGroesse){
					if (i < groessen.length-1){
						document.getElementsByTagName('body')[0].style.fontSize = groessen[i+1] + 'px';
						cookieManager.setCookie('fontsize', groessen[i+1]);
						initialeGroesse = groessen[i+1];
						break;
					}
				}
			}
		break;
		case 'dekrement':
			for (i=groessen.length-1; i>=0; i--){
				if (groessen[i] == initialeGroesse){
				    if (i> 0){
					document.getElementsByTagName('body')[0].style.fontSize = groessen[i-1] + 'px';
					cookieManager.setCookie('fontsize', groessen[i-1]);
					initialeGroesse = groessen[i-1];
					break;
				    }
				}
			}
		break;
			default:
			document.getElementsByTagName('body')[0].style.fontSize = groessen[1] + 'px';
			cookieManager.setCookie('fontsize', groessen[1]);
			initialeGroesse = groessen[1];
	}
}

function setzeInitialeFontgroesse(){
	var groesse = initialeGroesse;
	var cookie = cookieManager.getCookie('fontsize');
	if (cookie){
		groesse = parseInt(cookie);
	}
	if(groesse!=initialeGroesse) document.getElementsByTagName('body')[0].style.fontSize = groesse + 'px';
}