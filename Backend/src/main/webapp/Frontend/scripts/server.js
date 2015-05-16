define(function(){
    var hostname = "";//"http://localhost:8080"

    return {

	api: {
	    
	    host: hostname,
	    user: hostname + "/user/form",
	    login: hostname + "/auth/form",

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
