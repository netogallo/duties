requirejs(["server","defs","widgets","hs"],function(server,defs,widgets,hs){

    var InviteTask = React.createClass({displayName: "InviteTask",
	
	onChange: function(value){
	    
	    this.props.task.update({selected: !this.props.task.selected});
	},

	render: function(){
	    var task = this.props.task.task;

	    return (
		React.createElement("div", {className: "task col-md-4"}, 
		React.createElement("div", {className: "taskHead"}, 
		React.createElement("h3", null, React.createElement("input", {onChange: this.onChange, type: "checkbox", value: this.props.task.selected}, "Test"))
		), 
		React.createElement("div", {className: "taskBody"}, 
		React.createElement("div", {className: "taskStatus"}, 
		React.createElement("span", {className: "label label-success"}, React.createElement("span", {className: "glyphicon btc-curr"}, " "), task.bounty), 
		" ", 
		React.createElement("span", {className: "label label-info"}, React.createElement("span", {className: "glyphicon btc-curr"}, " "), task.penalty)
		), 
		React.createElement("div", {className: "description"}, 
		task.description
		)
		)
		)
	    );
	}
    });

    var Invite = React.createClass({displayName: "Invite",

	getInitialState: function(){
	    var invite = this.props.invite;
	    var self = this;

	    for(var task in invite.tasks){

		invite.tasks[task].setUpdate(
		    function(){
			console.log("lele");
			self.setState({x:'y'});});
	    }
	    
	    return {invite: invite};
	},

	render: function(){

	    var price = 0;

	    for(var task_ in this.state.invite.tasks){
		var task = this.state.invite.tasks[task_];

		if(task.selected)
		    price = price + task.task.penalty;
	    }

	    return (
		React.createElement("div", {className: "invite"}, 
		React.createElement("div", {className: "participants"}, 
		this.state.invite.duty.participants.map(
		    function(p){
			return (
			    React.createElement("span", {className: "label label-info label-participant"}, 
			    p.username
			    ));
		    })
		), 
		React.createElement("div", {className: "available-tasks"}, 
		React.createElement("h4", null, "Available Tasks"), 
		React.createElement("div", {className: "checkbox"}, 
		this.state.invite.tasks.map(function(task){
		  return (React.createElement(InviteTask, {task: task}));
	         }), 
		price
		)
		)
		)
	    );
	}
    });

    var Invites = React.createClass({displayName: "Invites",

	getInitialState: function(){

	    return {active: this.props.invites.length > 0 ? 0 : undefined}
	},

	selectInvite: function(k,v,e){

	    this.setState({active:k});
	},

	render: function(){

	    var listElems = hs.map(
		function(x){		    
		    return (
			React.createElement("div", {className: "invite-sel"}, 
			x.duty.name
			)
		    );
		},
		this.props.invites
	    );

	    return (
		React.createElement("div", {className: "col-md-12"}, 
		React.createElement("div", {className: "col-md-4"}, 
		React.createElement(widgets.List, {items: listElems, active: this.state.active, select: this.selectInvite})
		), 
		React.createElement("div", {className: "col-md-8"}, 
		React.createElement(Invite, {invite: this.props.invites[this.state.active]})
		)
		));
	    
	}
    });

    var tasks = [
	defs.InviteTaskS.create({task: {task_id: 4, penalty: 50, bounty: 10.4}, selected: false}),
	defs.InviteTaskS.create({task: {task_id: 4, penalty: 50, bounty: 10.4}, selected: false})
    ]
    var participants = [
	{username: "user1"},
	{username: "user2"}
    ];

    var invite = {tasks: tasks, duty: {name: "Kaiser", participants: participants}};

    React.render(
	React.createElement(Invites, {invites: [invite]}),
	document.getElementById('main')
    );
});