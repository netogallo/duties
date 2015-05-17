requirejs(["server","defs","widgets","hs","ui"],function(server,defs,widgets,hs,ui){

    var InviteTask = widgets.InviteTask;

    var Invite = React.createClass({

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
		<div className="invite">
		<div className="participants">
		<h4>Participants</h4>
		{this.state.invite.duty.participants.map(
		    function(p){
			return (
			    <span className="label label-info label-participant">
			    {p.username}
			    </span>);
		    })}
		</div>
		<div className="available-tasks">
		<h4>Available Tasks</h4>
		<div className="checkbox">
		{this.state.invite.tasks.map(function(task){
		  return (<InviteTask task={task} />);
	         })}
		{price}
		</div>
		</div>
		</div>
	    );
	}
    });

    var Invites = React.createClass({

	loadInvites: function(){

	    var self = this;
	    server.api.invitesReq({type: 'GET'})
	    .done(function(invites){
		var taskRefs = []
		for(var invite in invites){
		    for(var task in invites[invite].tasks){
			
			taskRefs.push(invites[invite].tasks[task]);
		    }
		}
		server.api.toTask({data: taskRefs})
		.done(function(tasks){

		    for(var invite in invites){
			for(var task in invites[invite].tasks){
			    
			    var task_ = hs.find(
				function(tsk){
				    return tsk.id == invites[invite].tasks[task].task_id;
				},
				tasks);
			    if(task_){
				invites[invite].duty = {id: invites[invite].tasks[task].duty_id};
				invites[invite].tasks[task] = task_;
			    }else
				console.log("Bad Ref",invites[invite].tasks[task]);
			}
		    }
		    self.setState({invites: invites});
		});
	    })
	},

	getInitialState: function(){

	    this.loadInvites();
	    return {invites: [], active: undefined};
	},

	selectInvite: function(k,v,e){

	    this.setState({active:k});
	},

	render: function(){

	    var listElems = hs.map(
		function(x){		    
		    return (
			<div className="invite-sel">
			{x.duty.id}
			</div>
		    );
		},
		this.props.invites
	    );

	    return (
		<div className="col-md-12">
		<div className="col-md-4">
		<widgets.List items={listElems} active={this.state.active} select={this.selectInvite}/>
		</div>
		<div className="col-md-8">
		<Invite invite={this.props.invites[this.state.active]} />
		</div>
		</div>);
	    
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

    ui.render({
	nav: ui.LoggedMenu,
	title: <h2>Invites</h2>,
	body: <Invites />
    });
});