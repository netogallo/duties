define(["hs"],function(hs){
    var hostname = "";//"http://localhost:8080"

    var onLogin = [];

    var onLogout = [];

    var users = undefined;

    var req = hs.curry(function(url,conf){

	if(!conf)
	    conf = {};

	if(!conf.contentType)
	    conf.contentType = 'application/json';

	if(!conf.dataType)
	    conf.dataType = 'json';

	if(typeof conf.data != typeof "")
	    conf.data = JSON.stringify(conf.data);

	if(!conf.type)
	    conf.type = 'POST';

	return $.ajax(url,conf);
    });

    var emptyReq = hs.curry(function(url,conf){
	conf.dataType = "* text";
	return req(url,conf);
    });

    var currUser;

    req(hostname + "/me",{type: 'GET'})
    .done(function(me){
	currUser = me;
	for(cb in onLogin){
	    onLogin[cb](currUser,"");
	}
    });

    var loginReq = function(conf){

	var data = conf.data;
	return req(hostname + "/login",conf)
	    .done(function(cookie){
		currUser = data;
		currUser.password = undefined;
		for(cb in onLogin){
		    onLogin[cb](currUser,cookie);
		}
	    })
	    .fail(function(error){
		
		for(cb in onLogin){
		    data.error = true;
		    onLogin[cb](data,error);
		}
	    });
    };

    return {

	api: {
	    
	    host: hostname,
	    user: hostname + "/user/form",
	    login: hostname + "/login",
	    loginReq: loginReq,
	    invite:  hostname + "/invite",
	    inviteReq: emptyReq(hostname + "/invite"),
	    invites: hostname + "/invites",
	    invitesReq: req(hostname + "/invites"),
	    mapTasks: hostname + "/tasks",
	    mapTasksReq: req(hostname + "/tasks"),
	    duties: hostname + "/duties",
	    dutiesReq: req(hostname + "/duties"),
	    duty: hostname + "/duty",
	    dutyReq: emptyReq(hostname + "/duty"),
	    address: hostname + "/address",
	    addressReq: req(hostname + "/address"),
	    report: hostname + "/report",
	    reportReq: emptyReq(hostname + "/report"),
	    getUsers: function(query,cb){
		console.log("get users");
		if(users)
		    cb(users);
		else{
		    req(hostname + "/users")({type: 'GET'})
			.done(function(res){
			    users = res;
			    cb(res);
			})
			.fail(function(error){
			    console.log("bad");
			    console.log(error);
			});
		}
	    }
	    
	},
	getUser: function(){return currUser;},

	onLogin: function(cb){

	    onLogin.push(cb);
	    if(currUser)
		cb(currUser,"");
	},

	onLogout: function(cb){
	    onLogout.push(cb);
	},

	getDuties: function(){
	    
	},
	
	getLoggedUser: function(){

	    return currUser;
	},

	host: "localhost"
    };
});
