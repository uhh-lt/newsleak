/****************************************************************
"bretteleben.de JavaScript Slideshow" - Version 20100412
License: http://www.gnu.org/copyleft/gpl.html
Author: Andreas Berger
Copyright (c) 2010 Andreas Berger - andreas_berger@bretteleben.de
Project page and Demo at http://www.bretteleben.de
Last update: 2010-04-12
*****************************************************************/
//*****parameters to set*****
//into this array insert the paths of your pics.
var def_imges=new Array ('pics/fws_001.JPG', 'pics/fws_002.JPG', 'pics/fws_003.JPG', 'pics/fws_004.JPG', 'pics/fws_005.JPG', 'pics/fws_006.JPG', 'pics/fws_007.JPG', 'pics/fws_008.JPG', 'pics/fws_009.JPG', 'pics/fws_010.JPG', 'pics/fws_011.JPG', 'pics/fws_012.JPG', 'pics/fws_013.JPG', 'pics/fws_014.JPG', 'pics/fws_015.JPG', 'pics/fws_016.JPG', 'pics/fws_017.JPG', 'pics/fws_018.JPG', 'pics/fws_019.JPG', 'pics/fws_020.JPG', 'pics/fws_021.JPG', 'pics/fws_022.JPG', 'pics/fws_023.JPG', 'pics/fws_024.JPG', 'pics/fws_025.JPG', 'pics/fws_026.JPG', 'pics/fws_027.JPG', 'pics/fws_028.JPG', 'pics/fws_029.JPG', 'pics/fws_030.JPG', 'pics/fws_031.JPG', 'pics/fws_032.JPG', 'pics/fws_033.JPG', 'pics/fws_034.JPG', 'pics/fws_035.JPG', 'pics/fws_036.JPG', 'pics/fws_037.JPG', 'pics/fws_038.JPG', 'pics/fws_039.JPG', 'pics/fws_040.JPG', 'pics/fws_041.JPG', 'pics/fws_042.JPG', 'pics/fws_043.JPG', 'pics/fws_044.JPG', 'pics/fws_045.JPG', 'pics/fws_046.JPG', 'pics/fws_047.JPG');
var def_divid="slideshow"; //the id of the div container that will hold the slideshow
var def_picwid=600; //set this to the width of your widest pic
var def_pichei=615; //... and this to the height of your highest pic
var def_backgr="#FFFFFF"; //set this to the background color you want to use for the slide-area
//(for example the body-background-color) if your pics are of different size
var def_sdur=3; //time to show a pic between fades in seconds
var def_fdur=1; //duration of the complete fade in seconds
var def_steps=20; //steps to fade from on pic to the next
var def_startwhen="y"; //start automatically at pageload? set it to "y" for on and to "n" for off
var def_shuffle="y"; //start with random image? set it to "y" for on and to "n" for off
var def_showcontr="y"; //do you want to show controls? set it to "y" for on and to "n" for off
//into this array insert the paths of your control-buttons or the text to display e.g. back,start,stop,fwrd.
var def_contr=new Array ('bwd.png', 'start.png', 'stop.png', 'fwd.png');
//****************************************************************

//daisychain onload-events
function daisychain(sl){if(window.onload) {var ld=window.onload;window.onload=function(){ld();sl();};}else{window.onload=function(){sl();};}}

function be_slideshow(be_slideid,be_imges,be_divid,be_picwid,be_pichei,be_backgr,be_sdur,be_fdur,be_steps,be_startwhen,be_shuffle,be_showcontr,be_contr){

//declarations and defaults
	var slideid=(be_slideid)?be_slideid:"0";
	var imges=(be_imges)?be_imges:def_imges;
	var divid=(be_divid)?be_divid:def_divid;
	var picwid=(be_picwid)?be_picwid:def_picwid;
	var pichei=(be_pichei)?be_pichei:def_pichei;
	var backgr=(be_backgr)?be_backgr:def_backgr;
	var sdur=(be_sdur)?be_sdur:def_sdur;
	var fdur=(be_fdur)?be_fdur:def_fdur;
	var steps=(be_steps)?be_steps:def_steps;
	var startwhen=(be_startwhen)?be_startwhen:def_startwhen;
			startwhen=(startwhen.toLowerCase()=="y")?1:0;
	var shuffle=(be_shuffle)?be_shuffle:def_shuffle;
			shuffle=(shuffle.toLowerCase()=="y")?1:0;
	var showcontr=(be_showcontr)?be_showcontr:def_showcontr;
			showcontr=(showcontr.toLowerCase()=="y")?1:0;
	var contr=(be_contr)?be_contr:def_contr;
	var ftim=fdur*1000/steps;
	var stim=sdur*1000;
	var emax=imges.length;
	var self = this;
	var stopit=1;
	var startim=1;
	var u=0;
	var parr = new Array();
	var ptofade,pnext,factor,mytimeout;
//check if there are at least 3 pictures, elswhere double the array
	if(imges.length<=2){imges=imges.concat(imges);}
//shuffle images if set
  if(shuffle){for(i=0;i<=Math.floor(Math.random()*imges.length);i++){imges.push(imges.shift());}}
  
//push images into array and get things going
	this.b_myfade = function(){
		var a,idakt,paktidakt,ie5exep;
		for(a=1;a<=emax;a++){
			idakt="img_"+slideid+"_"+a;paktidakt=document.getElementById(idakt);
    	ie5exep=new Array(paktidakt);parr=parr.concat(ie5exep);
    }
		if(startwhen){
			stopit=0;
 			mytimeout=setTimeout(function(){self.b_slide();},stim);
 		}
	}

//prepare current and next and trigger slide
	this.b_slide = function(){
		clearTimeout(mytimeout);
		u=0;
		ptofade=parr[startim-1];
		if(startim<emax){pnext=parr[startim];}
		else{pnext=parr[0];}
		pnext.style.zIndex=1;
		pnext.style.visibility="visible";
		pnext.style.filter="Alpha(Opacity=100)";
		try{pnext.style.removeAttribute("filter");} catch(err){}
		pnext.style.MozOpacity=1;
		pnext.style.opacity=1;
		ptofade.style.zIndex=2;
		ptofade.style.visibility="visible";
		ptofade.style.filter="Alpha(Opacity=100)";
		ptofade.style.MozOpacity=1;
		ptofade.style.opacity=1;
		factor=100/steps;
		if(stopit=="0"){
			this.b_slidenow();
		}
	}

//one step forward
	this.b_forw = function(){
		stopit=1;
		clearTimeout(mytimeout);
		ptofade=parr[startim-1];
		if(startim<emax){pnext=parr[startim];startim=startim+1;}
		else{pnext=parr[0];startim=1;}
		ptofade.style.visibility="hidden";
		ptofade.style.zIndex=1;
		pnext.style.visibility="visible";
		pnext.style.zIndex=2;
		self.b_slide();
	}

//one step back
	this.b_back = function(){
		stopit=1;
		clearTimeout(mytimeout);
		if(u==0){ //between two slides
			ptofade=parr[startim-1];
			if(startim<emax){pnext=parr[startim];}
			else{pnext=parr[0];}
			pnext.style.visibility="hidden";
			ptofade.style.zIndex=1;
			ptofade.style.visibility="visible";
			if(startim>=2){startim=startim-1;}
			else{startim=emax;}
			self.b_slide();
		}
		else{ //whilst sliding
			self.b_slide();
		}
	}

//slide as said, then give back
	this.b_slidenow = function(){
		var check1,maxalpha,curralpha;
		check1=ptofade.style.MozOpacity;
		maxalpha=(100-factor*u)/100*105;
		if(check1<=maxalpha/100){u=u+1;}
		curralpha=100-factor*u;
		ptofade.style.filter="Alpha(Opacity="+curralpha+")";
		ptofade.style.MozOpacity=curralpha/100;
		ptofade.style.opacity=curralpha/100;
		if(u<steps){ //slide not finished
			if(stopit=="0"){mytimeout=setTimeout(function(){self.b_slidenow();},ftim);}
			else {this.b_slide();}
		}
		else{ //slide finished
			if(startim<emax){
				ptofade.style.visibility="hidden";
				ptofade.style.zIndex=1;
				pnext.style.zIndex=2;
				startim=startim+1;u=0;
				mytimeout=setTimeout(function(){self.b_slide();},stim);
			}
			else{
				ptofade.style.visibility="hidden";
				ptofade.style.zIndex=1;
				pnext.style.zIndex=2;
				startim=1;u=0;
				mytimeout=setTimeout(function(){self.b_slide();},stim);
			}
		}
	}

//manual start
	this.b_start= function(){
		if(stopit==1){
 			stopit=0;
 			mytimeout=setTimeout(function(){self.b_slide();},stim);
 		}
	}

//manual stop
	this.b_stop= function(){
		clearTimeout(mytimeout);
		stopit=1;
		this.b_slide();
	}

//insert css and images
	this.b_insert= function(){
		var b, thestylid, thez, thevis, slidehei;
		slidehei=(showcontr)?(pichei+25):(pichei); //add space for the controls
		var myhtml="<div style='width:"+picwid+"px;height:"+slidehei+"px;'>";
   			myhtml+="<div style='position:absolute;width:"+picwid+"px;height:"+pichei+"px;'>";
		for(b=1;b<=emax;b++){
			thez=1;thevis='hidden';
			if(b<=1) {thez=2; thevis='visible';}
			  myhtml+="<div id='img_"+slideid+"_"+b+"' style='font-size:0;line-height:"+pichei+"px;margin:0;padding:0;text-align:center;visibility:"+thevis+";z-index:"+thez+";position:absolute;left:0;top:0;width:"+picwid+"px;height:"+pichei+"px;background-color:"+backgr+";'>";
				myhtml+="<img src='"+imges[(b-1)]+"' style='vertical-align:middle;border:0;' alt=''/></div>";
		}
   			myhtml+="</div>";
//show controls
		if(showcontr){
				for(b=1;b<=4;b++){
					var check=contr[b-1].substring(contr[b-1].length-3).toLowerCase(); //check for buttons
					contr[b-1]=(check=="jpg"||check=="gif"||check=="png")?("<img src='"+contr[b-1]+"' style='border:none;' alt=''/>"):(contr[b-1]);
				}
   			myhtml+="<div style='display:block;width:"+picwid+"px;padding-top:"+(pichei+3)+"px;text-align:left;'>";
   			myhtml+="<a href='javascript:be_"+slideid+".b_back();' style='text-decoration:none'>"+contr[0]+"</a>&nbsp;";
   			myhtml+="<a href='javascript:be_"+slideid+".b_start();' style='text-decoration:none'>"+contr[1]+"</a>&nbsp;";
   			myhtml+="<a href='javascript:be_"+slideid+".b_stop();' style='text-decoration:none'>"+contr[2]+"</a>&nbsp;";
   			myhtml+="<a href='javascript:be_"+slideid+".b_forw();' style='text-decoration:none'>"+contr[3]+"</a>";
		}
   			myhtml+="</div>";
		document.getElementById(divid).innerHTML=myhtml;
		self.b_myfade();
	}

//call autostart-function
daisychain(this.b_insert);
	
}
var be_0= new be_slideshow();