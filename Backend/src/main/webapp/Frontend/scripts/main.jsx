requirejs(["server","signal"],function(server,signal){
    console.log(signal);
    $.getJSON(
	"/scripts/schema.json?v=x",
	function(schema){
	    
	    console.log("what");

	    var hs = prelude('prelude-ls');

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
		    {this.props.duties.map(function(d){
			
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
			    return username == server.getLoggedUser().username;}
			    ,this.props.task.votes);
		},

		handleReport: function(e){
		    
		    var votes;
		    var self = this;
		    var logged = server.getLoggedUser();
		    if(this.isReported())
			votes = hs.filter(function(user){return user != logged.username},this.props.task.votes);
		    else
			votes = hs.concat([[logged.username],this.props.task.votes]);
		    
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
			    <div className="taskStatus">
			    <span className={hs.unwords(reportBtnCss)}>Reports <span className="badge">{this.props.task.votes.length}</span></span>
			    &nbsp;
			    <span className="label label-info">{this.props.task.entrusted}</span>
			    &nbsp;
			    <span className="label label-info"><span className="glyphicon btc-curr">&nbsp;</span>{this.props.task.penalty}</span>
			    </div>
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

	    var DutyEdit = React.createClass({

		saveDuty: function(e){

		    e.preventDefault();
		    if(this.props.onSubmit){

			this.props.onSubmit.apply(this,[{
			    duty_name: $('input[name="duty-name"]').val()
			}]);
		    }
		},

		render: function(){
		    
		    return (
			<div className={this.props.className}>
			<form onSubmit={this.saveDuty}>
			<label htmlFor="duty-name">Name</label>
			<input type="text" id="duty-name" className="form-control" name="duty-name"></input>
			<input type="submit" value="Create Duty"></input>
			</form>
			</div>);
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
			    <div className="taskOperations">
			    {dialog}
			    <button type="button" className="btn btn-primary btn-sm" data-toggle="modal" data-target="#task-edit">Create Task</button>
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

		saveDuty: function(dutySpec){
		    
		    var self = this;
		    var duty = {
			name: dutySpec.duty_name,
			participants: [],
			tasks: []
		    };

		    server.saveDuty(duty,function(result){

			if(!result.error){

			    var dutyS = DutyS.create(duty);

			    dutyS.setUpdate(function(){
				self.setState({x:'y'});
			    });

			    self.setState({duties: hs.concat([[dutyS],self.state.duties])});
			}
		    });
		},

		render: function(){

		    var self = this;

		    return (
			<div className="duties">
			<div className="col-md-4">
			<DutyList selectDuty={this.selectDuty} duties={this.state.duties} />
			<Dialog id="duty-edit">
			<DutyEdit onSubmit={this.saveDuty} className="modal-body"/>
			</Dialog>
			<button type="button" className="btn btn-primary btn-lg" data-toggle="modal" data-target="#duty-edit">Create Duty</button>
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

	    var tasks = hs.map(TaskS.create,[{name: "Task", entrusted: "user2", description: "Task description", penalty: 50, votes: []}]);
	    var users = hs.map(validator(schema.User),[{username: "user1"},{username: "user2"}]);
	    var duties = hs.map(DutyS.create,[{name: "duty1", participants: users, tasks: tasks},{name: "duty2", participants: users, tasks:[]}]);
	    
	    console.log("Render");

	    React.render(<Duties duties={duties}/>,
		document.getElementById('main')
	    );
	}).fail(function(r,e){console.log(e);});
});