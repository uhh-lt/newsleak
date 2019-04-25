/* NAVI */
aktULoldclass="";
aktUL=false;
aktNode=false;
aktThat=false;
newThat=false;
naviOnTimeout=false;
naviOffTimeout=false;

function naviOver(that) {
	newThat=that;
	naviOnTimeout=window.setTimeout("_naviOver()", 400);
}

function _naviOver() {
	if(aktNode && newThat != aktThat) {
		_naviOut();
	}

	aktThat = newThat;
	aktNode = document.getElementById(aktThat.id+"_sub");

	if(aktNode) aktNode.style.display="block";
}

function keep() {
	window.clearTimeout(naviOffTimeout);
}

function naviOut() {
	window.clearTimeout(naviOnTimeout);
	naviOffTimeout = window.setTimeout("_naviOut()", 250);
}

function _naviOut() {
	window.clearTimeout(naviOffTimeout);
	
	if(aktNode) {
		aktNode.style.display = "none";
		aktNode = false;
	}
}