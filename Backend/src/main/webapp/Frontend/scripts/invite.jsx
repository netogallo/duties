requirejs(["server","defs","widgets","hs","ui"],function(server,defs,widgets,hs,ui){

    var InviteTask = widgets.InviteTask;

    var Invite = React.createClass({

	loadBtcAddress: function(){
	    var self = this;
	    if(this.props.invite)
	    for(var task in this.props.invite.tasks){
		if(!self.state.addrs[self.props.invite.tasks[task].id]){
		    server.api.addressReq({
			//type: 'GET',
			data: {task_id: this.props.invite.tasks[task].id}
		    })
		    .done(function(addr){
			self.state.addrs[self.props.invite.tasks[task].id] = addr.btc_address;
			self.setState({addrs: self.state.addrs});
		    });
		}
	    }
	},

	getInitialState: function(){
	    var invite = this.props.invite;
	    var self = this;
	    this.loadBtcAddress();

	    /*
	    for(var task in invite.tasks){

		invite.tasks[task].setUpdate(
		    function(){
			console.log("lele");
			self.setState({x:'y'});});
	    }*/
	    
	    return {invite: invite, tasks: {}, addrs: {}};
	},

	render: function(){
	    var self = this;
	    
	    if(this.props.invite){
		this.loadBtcAddress();
		var tasks = hs.map(
		    function(task){
			if(!self.state.tasks[task.id]){
			    self.state.tasks[task.id] = defs.CheckS.create({status: false, value: task});
			}
			return self.state.tasks[task.id];
		    },this.props.invite.tasks);
			    
		
		return (
		    <div className={"invite " + this.props.className}>
			<div className="col-container-1">
			<h4>{this.props.invite.duty.name}</h4>
			</div>

			<div className="invite-body col-container-10 ui-scroll">
			<div className="participants">
			<h4>Participants</h4>
			<div className="statusHolder">
			{this.props.invite.duty.participants.map(
			    function(p){
				return (
					<span className="label label-info label-participant">{p.username}
				    </span>);
			    })}
		        </div>
			</div>
		    
			<div className="available-tasks">
			<h4>Available Tasks</h4>
			<div className="with-floats status-holder">
			{tasks.map(function(task){
			    return (<InviteTask task={task} address={self.state.addrs[task.value.id]} />);
			})}
		        </div>
		        </div>
		    
		        </div>
		    </div>
		);
	    }else
		return (<div className="invite"></div>);
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
		server.api.mapTasksReq({data: taskRefs})
		.done(function(tasks){

		    for(var invite in invites){
			for(var task in invites[invite].tasks){
			    
			    var task_ = hs.find(
				function(tsk){
				    return tsk.id == invites[invite].tasks[task].task_id;
				},
				tasks);
			    if(task_){
				invites[invite].tasks[task] = task_;
			    }else
				console.log("Bad Ref",invites[invite].tasks[task]);
			}
		    }
		    self.setState({invites: hs.filter(function(i){return i.duty;},invites)});
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
		    console.log(x);
		    return (
			<div className="invite-sel">
			{x.duty.name}
			</div>
		    );
		},
		this.state.invites
	    );

	    return (
		<div className="invites">
		<widgets.List items={listElems} active={this.state.active} select={this.selectInvite} className="col-md-4 col-container-12 ui-scroll"/>
		<Invite className="col-md-8 col-container-12" invite={this.state.active ? this.state.invites[this.state.active] : undefined} />
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
