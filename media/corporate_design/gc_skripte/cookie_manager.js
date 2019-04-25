function Cookiemanager(name,defaultExpiration,expirationUnits,defaultDomain,defaultPath){
	this.name = name;
	this.defaultExpiration = this.getExpiration(defaultExpiration,expirationUnits);
	this.defaultDomain = (defaultDomain)?defaultDomain:(document.domain.search(/[a-zA-Z]/) == -1)?document.domain:document.domain.substring(document.domain.indexOf('.') + 1,document.domain.length);
	this.defaultPath = (defaultPath)?defaultPath:'/';
	this.cookies = new Object();
	this.expiration = new Object();
	this.domain = new Object();
	this.path = new Object();
	window.onunload = new Function (this.name+'.setDocumentCookies();');
	this.getDocumentCookies();
}

Cookiemanager.prototype.getExpiration = function(expiration,units){
	expiration = (expiration)?expiration:7;
	units = (units)?units:'days';
	var date = new Date();

	switch(units) {
		case 'years':
			date.setFullYear(date.getFullYear() + expiration);
			break;
		case 'months':
			date.setMonth(date.getMonth() + expiration);
			break;
		case 'days':
			date.setTime(date.getTime()+(expiration*24*60*60*1000));
			break;
		case 'hours':
			date.setTime(date.getTime()+(expiration*60*60*1000));
			break;
		case 'minutes':
			date.setTime(date.getTime()+(expiration*60*1000));
			break;
		case 'seconds':
			date.setTime(date.getTime()+(expiration*1000));
			break;
		default:
			date.setTime(date.getTime()+expiration);
			break;
		}
	return date.toGMTString();
}

Cookiemanager.prototype.getDocumentCookies = function(){
	var cookie,pair;
	var cookies = document.cookie.split(';');
	var len = cookies.length;


	for(var i=0;i<len;i++) {
		cookie = cookies[i];
		while (cookie.charAt(0)==' ') cookie = cookie.substring(1,cookie.length);
		pair = cookie.split('=');
		this.cookies[pair[0]] = pair[1];
	}
}


Cookiemanager.prototype.setDocumentCookies = function() {
	var expires = '';
	var cookies = '';
	var domain = '';
	var path = '';

	for(var name in this.cookies) {
		expires = (this.expiration[name])?this.expiration[name]:this.defaultExpiration;
		path = (this.path[name])?this.path[name]:this.defaultPath;
		domain = (this.domain[name])?this.domain[name]:this.defaultDomain;
		cookies = name + '=' + this.cookies[name] + '; expires=' + expires + '; path=' + path + ';';
		document.cookie = cookies;
		}
	return true;
}

Cookiemanager.prototype.getCookie = function(cookieName) {
	var cookie = this.cookies[cookieName]
	return (cookie)?cookie:false;
}

Cookiemanager.prototype.setCookie = function(cookieName,cookieValue,expiration,expirationUnits,domain,path){
	this.cookies[cookieName] = cookieValue;
	if (expiration) this.expiration[cookieName] = this.getExpiration(expiration,expirationUnits);
	if (domain) this.domain[cookieName] = domain;
	if (path) this.path[cookieName] = path;

	return true;
}