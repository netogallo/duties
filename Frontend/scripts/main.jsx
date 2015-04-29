$.getJSON(
    "scripts/schema.json",
    function(schema){

	var Auth = {user: "user1"}
	
	var hs = require('prelude-ls');

	tv4.addSchema('User',schema.User);
	tv4.addSchema('Task',schema.Task);
	tv4.addSchema('Duty',schema.Duty);

	var prim = {type: 'prim'};

	var array = {

	    of: function(type){return {type: 'array', of: type};}
	}

	var Signal = function(spec){

	    return {
		
		type: 'signal',

		create: function(obj){

		    var res = {

			update: function(fields){

			    for(field in fields){
				
				this.setProp(field,fields[field]);

			    }

			    this.updateFn ? this.updateFn() : null;
			},

			setUpdate: function(update){
			    
			    this.updateFn = update;
			},
			
			setProp : function(prop,value){
			
			    var type = spec[prop];
			    var self = this;

			    if(!type)
				throw ("The property " + prop + " is not defined in spec: " + JSON.stringyfy(spec));

			    this[prop] = value;

			    function setSignal(obj){

				obj.setUpdate(function(){self.update.apply(self,[])});
			    }

			    if(type.type == 'signal'){
				
				setSignal(self[prop]);
			    }

			    else if(type.type && type.type.type == 'array' && type.type.of.type == 'signal'){

				for(var i in self[prop]){
				    console.log(type.type.of);
				    setSignal(self[prop][i]);
				}
			    }
			}
		    };

		    res.update(obj);
		    return res;
		}
	    }
	};

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

	var TaskEdit = React.createClass({

	    saveTask: function(e){

		e.preventDefault();
		if(this.props.onSubmit){

		    this.props.onSubmit.apply(this,[{
			
			task_name: $('input[name="task-name"]').val(),
			task_description: $('input[name="task-description"]').val(),
			task_penalty: $('input[name="task-penalty"]').val()
		    }]);
		}
	    },

	    render: function(){
		
		return (
		    <div className={this.props.className}>
		    <form onSubmit={this.saveTask}>
		    <label htmlFor="task-name">Name</label>
		    <input type="text" id="task-name" className="form-control" name="task-name"></input>
		    <label htmlFor="task-description">Description</label>
		    <input type="text" id="task-description" className="form-control" name="task-description"></input>
		    <label htmlFor="task-penalty">Penalty</label>
		    <input type="text" id="task-penalty" className="form-control" name="task-penalty"></input>
		    <input type="submit" className="form-control" value="Create Task"></input>
		    </form>
		    </div>
		);
	    }
	});

	var Dialog = React.createClass({

	    render: function(){

		return (
		    <div id={this.props.id} className="modal fade">
		    <div className="modal-dialog">
		    <div className="modal-content">
		    {this.props.children}
		    </div>
		    </div>
		    </div>
		    
		);

	    }

	});

	var Task = React.createClass({
	    
	    isReported: function(){

		return hs.find(
		    function(username){
			return username == Auth.user;}
			,this.props.task.votes);
	    },

	    handleReport: function(e){
		
		var votes;
		var self = this;
		if(this.isReported())
		    votes = hs.filter(function(user){return user != Auth.user},this.props.task.votes);
		else
		    votes = hs.concat([[Auth.user],this.props.task.votes]);
		
		this.props.task.update({votes: votes});
	    },
	    
	    render: function(){

		var reportCss = ["report-btn","btn","btn-default"];

		var reportBtnCss = ["label"];

		if(this.props.total && this.props.total / 2 <= this.props.task.votes.length)
		    reportBtnCss.push("label-warning");
		else
		    reportBtnCss.push("label-success");

		if(this.isReported())
		    reportCss.push("active");

		if(this.props.task){

		    return (
			<div className="task col-md-4">
			<div className="taskHead">
			<h3>{this.props.task.name}</h3>
			</div>
			<div className="taskBody">
			<span className={hs.unwords(reportBtnCss)}>Reports <span className="badge">{this.props.task.votes.length}</span></span>
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
	    
	    render: function(){
		var self = this;

		var votes = {};
		var penalty = {};

		hs.map(
		    function(task){
			votes[task.task_id] = task.votes;
		    },
		    this.props.duty ? this.props.duty.tasks : []);

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
		    this.props.duty ? this.props.duty.participants : []);


		var taskSave = function(taskProps){
		    
		    var task = TaskS.create({
			name: taskProps.task_name,
			entrusted: "",
			description: taskProps.task_description,
			penalty: taskProps.task_penalty,
			votes: []
		    });

		    self.props.duty.update({tasks: hs.concat([[task],self.props.duty.tasks])});
		};

		var dialog = (
		    <Dialog id="task-edit">
		    <TaskEdit onSubmit={taskSave} className="modal-body"/>
		    </Dialog>);

		if(this.props.duty)
		    return (
		    <div className="duty" style={this.props.duty == {} ? {display:'none'} : {}}>
		    <h3>{this.props.duty.name}</h3>
		    <div className="participants">
		    {this.props.duty.participants.map(function(participant){
		    return (<span className="label label-info"><span>{participant.username}</span> <span className="glyphicon btc-curr">&nbsp;</span><span>{penalty[participant.username] ? penalty[participant.username] : 0}</span></span>);
		    })}
		    </div>
		    <div className="tasks">
		    {this.props.duty.tasks.map(function(task){return <Task total={self.props.duty.participants.length} onReport={self.handleTaskUpdate} task={task}/>;})}
		    </div>
	            <div className="taskOperations">
			{dialog}
			<button type="button" className="btn btn-primary btn-lg" data-toggle="modal" data-target="#task-edit">Create Task</button>
	            </div>
		    </div>);
		else
		    return <div className="duty"></div>;
	    }

	});

	var Duties = React.createClass({

	    updateDuties: function(duid,dutyUp){

		for(var prop in dutyUp){
		    this.state.duties[duid][prop] = dutyUp[prop];
		}

		this.setState({duties: this.state.duties});
	    },

	    getInitialState: function(){
		var self = this;
		var dutyList = this.props.duties ? this.props.duties : [];

		for(var duty in dutyList){

		    dutyList[duty].setUpdate(
			function(){
			    console.log('setting state');
			    self.setState({x: 'y'});
			});
		}
		
		return {duties: dutyList, duty: undefined};
	    },

	    selectDuty: function(duty){
		this.setState({duty: duty});
	    },

	    render: function(){

		var self = this;

		return (
		    <div className="duties">
		    <div className="col-md-4">
		    <DutyList selectDuty={this.selectDuty} duties={this.state.duties} />
		    </div>
		    <div className="col-md-8">
		    <Duty duty={this.state.duty} />
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
	
    var TaskS = Signal({
	name: {type: prim},
	entrusted: {type: prim},
	description: {type: prim},
	penalty: {type: prim},
	votes: {type: prim}
    });

    var DutyS = Signal({
	name: {type: prim},
	participants: {type: prim},
	tasks: {type: array.of(TaskS)}
    });
	

    var tasks = hs.map(TaskS.create,[{name: "Task", entrusted: "user2", description: "Task description", penalty: 50, votes: []}]);
    var users = hs.map(validator(schema.User),[{username: "user1"},{username: "user2"}]);
    var duties = hs.map(DutyS.create,[{name: "duty1", participants: users, tasks: tasks},{name: "duty2", participants: users, tasks:[]}]);
		    
    React.render(<Duties duties={duties}/>,
	document.getElementById('main')
    );
});