function oeffneFenster(adresse,fenster,breite,hoehe,optionen){
	var scrollbars=false;
	if(breite> screen.availWidth){
		breite=screen.availWidth;
		scrollbars=true;
	}
	if(hoehe> screen.availHeight){
		hoehe=screen.availHeight;
		scrollbars=true;
	}	
	if(scrollbars)  optionen+=",scrollbars=yes";
	else  optionen+=",scrollbars=no";
	
	fenster = window.open(adresse,fenster,"width="+breite+",height="+hoehe+","+optionen);
	fenster.focus();
}