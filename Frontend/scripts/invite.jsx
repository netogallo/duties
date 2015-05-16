requirejs(["server","defs","widgets","hs"],function(server,defs,widgets,hs){

    var InviteTask = React.createClass({
	
	onChange: function(value){
	    
	    this.props.task.update({selected: !this.props.task.selected});
	},

	render: function(){
	    var task = this.props.task.task;

	    return (
		<div className="task col-md-4">
		<div className="taskHead">
		<h3><input onChange={this.onChange} type="checkbox" value={this.props.task.selected}>Test</input></h3>
		</div>
		<div className="taskBody">
		<div className="taskStatus">
		<span className="label label-success"><span className="glyphicon btc-curr">&nbsp;</span>{task.bounty}</span>
		&nbsp;
		<span className="label label-info"><span className="glyphicon btc-curr">&nbsp;</span>{task.penalty}</span>
		</div>
		<div className="description">
		{task.description}
		</div>
		</div>
		</div>
	    );
	}
    });

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
			<div className="invite-sel">
			{x.duty.name}
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

    React.render(
	<Invites invites={[invite]} />,
	document.getElementById('main')
    );
});