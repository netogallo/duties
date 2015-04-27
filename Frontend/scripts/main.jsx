$.getJSON(
    "scripts/schema.json",
    function(schema){

	var Auth = {user: "user1"}
	
	var hs = require('prelude-ls');

	tv4.addSchema('User',schema.User);
	tv4.addSchema('Task',schema.Task);
	tv4.addSchema('Duty',schema.Duty);

	var validator = hs.curry(function(schema,model){
	    if(!tv4.validate(model,schema))
		throw tv4.error;

	    return model;
	});

	var DutyList = React.createClass({

	    getInitialState: function(){
		
		return {duties: this.props.duties ? this.props.duties : []};
	    },

	    render: function(){

		var self = this;

		var clickItem = hs.curry(function(duty,e){
		    self.setState({active: duty});

		    if(self.props.selectDuty)
			self.props.selectDuty(duty);
		});

		return (<div className="dutyList list-group">
		    {this.state.duties.map(function(d){
			
			var classes=["list-group-item"];
			
			if(d==self.state.active){
			    classes.push("active");
			}
			
			return (<a onClick={clickItem(d)} className={hs.unwords(classes)} href="#">{d.name}</a>)})}
		    </div>);
	    }
	});

	var Task = React.createClass({

	    getInitialState: function(){

		return {reported: hs.find(function(user){return Auth.user==user},this.props.task.votes), votes: this.props.task.votes}
	    },

	    handleReport: function(e){
		
		var votes;
		var self = this;
		if(this.state.reported)
		    votes = hs.filter(function(user){return user != Auth.user},this.state.votes);
		else
		    votes = hs.concat([[Auth.user],this.state.votes]);
		
		var callback = function(){
		    if(self.props.onReport){
			self.props.task.votes = votes;
			self.props.onReport(self.props.task);
		    }
		}

		this.setState({reported: !this.state.reported, votes: votes},callback);
	    },
	    
	    render: function(){

		var reportCss = ["report-btn","btn","btn-default"];

		var reportBtnCss = ["label"];

		if(this.props.total && this.props.total / 2 <= this.state.votes.length)
		    reportBtnCss.push("label-warning");
		else
		    reportBtnCss.push("label-success");

		if(this.state.reported)
		    reportCss.push("active");

		if(this.props.task){

		    return (
			<div className="task col-md-4">
			<div className="taskHead">
			<h3>{this.props.task.name}</h3>
			</div>
			<div className="taskBody">
			<span className={hs.unwords(reportBtnCss)}>Reports <span className="badge">{this.state.votes.length}</span></span>
			&nbsp;
			<span className="label label-info">{this.props.task.entrusted}</span>
			&nbsp;
			<span className="label label-info"><span className="glyphicon btc-curr">&nbsp;</span>{this.props.task.penalty}</span>
			<div className="description">
			{this.props.task.description}
			</div>
			<span className="report">
			<button type="button" onClick={this.handleReport} className={hs.unwords(reportCss)}><span className="glyphicon glyphicon-flag"></span>{" Report"}</button>
			</span>
			</div>
		    </div>);
		}
	    }
	});

	var Duty = React.createClass({

	    computeVotes: function(){
		var votes = {};
		hs.map(
		    function(task){
			votes[task.task_id] = task.votes;
		    },
		    this.props.duty.tasks);

		return votes;
	    },

	    computeCredit: function(votes){
		var self = this;
		var penalty = {};

		hs.map(
		    function(user){

			var loss = hs.fold(function(s,task){
			    if(task.entrusted == user.username && self.props.duty.participants.length / 2 <= votes[task.task_id].length)
				return s - task.penalty;
			    else
				return s;
			},0,self.props.duty.tasks);
			penalty[user.username] = loss;
		    },
		    this.props.duty.participants);
		return penalty;
	    },
	    
	    getInitialState: function(){
		if(this.props.duty){
		    var votes = computeVotes();
		    var credit = this.computeCredit(votes);
		    return {votes: votes, credit: credit};
		}else
		    return {votes: {}, credit: {}};
	    },

	    handleTaskUpdate: function(task){
		var votes = this.state.votes;
		votes[task.task_id] = task.votes;
		var credit = this.computeCredit(votes);
		this.setState({votes: votes, credit: credit});
		
	    },

	    render: function(){
		var self = this;

		if(this.props.duty)
		    return (
		    <div className="duty" style={this.props.duty == {} ? {display:'none'} : {}}>
		    <h3>{this.props.duty.name}</h3>
		    <div className="participants">
		    {this.props.duty.participants.map(function(participant){
		    return (<span className="label label-info"><span>{participant.username}</span> <span className="glyphicon btc-curr">&nbsp;</span><span>{self.state.credit[participant.username] ? self.state.credit[participant.username] : 0}</span></span>);
		    })}
		    </div>
		    <div className="tasks">
		    {this.props.duty.tasks.map(function(task){return <Task total={self.props.duty.participants.length} onReport={self.handleTaskUpdate} task={task}/>;})}
		    </div>
		    </div>);
		else
		    return <div className="duty"></div>;
	    }

	});

	var Duties = React.createClass({

	    getInitialState: function(){
		return {duties: this.props.duties ? this.props.duties : [], duty: undefined};
	    },

	    addDuties: function(duties){
		
		this.setState({duties: hs.concat([[duties],this.state.duties])});
	    },

	    render: function(){

		var self = this;

		var select = function(duty){
		    self.setState({duty: duty});
		};

		return (
		    <div className="duties">
		    <div className="col-md-4">
		    <DutyList selectDuty={select} duties={this.state.duties} />
		    </div>
		    <div className="col-md-8">
		    <Duty duty={this.state.duty}/>
		    </div>
		    </div>
		);
	    }
	});

	/*
	function modal(header,body){     
	    return <div id="theModal" className="modal fade" role="dialog" tabIndex="-1">
	    <div className="modal-dialog">
	    <div className="modal-content">
	    <div className="modal-header">
	    {header}
	    </div>
	    <div className="modal-body">
	    {body}
	    </div>
	    </div>
	    </div>
	    </div>;
	}
	*/
	
	

	var tasks = [{name: "Task", entrusted: "user2", description: "Task description", penalty: 50, votes: []}];
	var users = hs.map(validator(schema.User),[{username: "user1"},{username: "user2"}]);
	var duties = [{name: "duty1", participants: users, tasks: tasks},{name: "duty2", participants: users, tasks:[]}];
		    
	React.render(<Duties duties={duties}/>,
	    document.getElementById('main')
	);
    });