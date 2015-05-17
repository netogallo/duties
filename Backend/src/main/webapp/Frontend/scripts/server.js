define(["hs"],function(hs){
    var hostname = "";//"http://localhost:8080"

    var req = hs.curry(function(url,conf){

	if(!conf.contentType)
	    conf.contentType = 'application/json';

	if(typeof conf.data != typeof "")
	    conf.data = JSON.stringify(conf.data)

	if(!conf.type)
	    conf.type = 'POST';

	return $.ajax(url,conf);
    });

    return {

	api: {
	    
	    host: hostname,
	    user: hostname + "/user/form",
	    login: hostname + "/login",
	    loginReq: req(hostname + "/login"),
	    invites: hostname + "/invites",
	    duties: hostname + "/duties",
	    duty: hostname + "/duty"

	},

	getDuties: function(){
	    
	},
	
	saveDuty: function(duty,cb){

	    cb({});
	},

	getLoggedUser: function(){

	    return {
		username: "user1"
	    };
	},

	host: "localhost"
    };
});
