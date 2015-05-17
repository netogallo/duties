requirejs(["server","signal","defs","ui","util"],function(server,signal,defs,ui,util){
    
    
    console.log("what");

    var hs = prelude('prelude-ls');

    /*
    tv4.addSchema('User',schema.User);
    tv4.addSchema('Task',schema.Task);
    tv4.addSchema('Duty',schema.Duty);
    */

    /*
    var validator = hs.curry(function(schema,model){
	if(!tv4.validate(model,schema))
	    throw tv4.error;

	return model;
    });
    */

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
		    task_penalty: parseInt($('input[name="task-penalty"]').val())
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
		,this.props.task.reports);
	},

	handleReport: function(e){
	    
	    var reports;
	    var self = this;
	    var logged = server.getLoggedUser();
	    if(this.isReported())
		reports = hs.filter(function(user){return user != logged.username},this.props.task.votes);
	    else
		reports = hs.concat([[logged.username],this.props.task.reports]);
	    
	    this.props.task.update({reports: reports});
	},
	
	render: function(){

	    var reportCss = ["report-btn","btn","btn-default"];

	    var reportBtnCss = ["label"];

	    if(this.props.total && this.props.total / 2 <= this.props.task.reports.length)
		reportBtnCss.push("label-warning");
	    else
		reportBtnCss.push("label-success");

	    if(this.isReported())
		reportCss.push("active");

	    var report;
	    
	    if(this.props.task.entrusted)
		report = <button type="button" onClick={this.handleReport} className={hs.unwords(reportCss)}><span className="glyphicon glyphicon-flag"></span>{" Report"}</button>

	    if(this.props.task){

		return (
		    <div className="task col-md-4">
		    <div className={util.if_(this.props.task.entrusted)("taskHead")("taskHead taskEmpty")}>
		    <h3>{this.props.task.name}</h3>
		    </div>
		    <div className="taskBody taskEmptyBody">
		    <div className="taskStatus">
		    <span className={hs.unwords(reportBtnCss)}>Reports <span className="badge">{this.props.task.reports.length}</span></span>
		    &nbsp;
		    <span className="label label-info">{this.props.task.entrusted}</span>
		    &nbsp;
		    <span className="label label-info"><span className="glyphicon btc-curr">&nbsp;</span>{this.props.task.penalty}</span>
		    </div>
		    <div className="description">
		    {this.props.task.description}
		    </div>
		    <span className="report">
		    {report}
		    </span>
		    </div>
		    </div>);
	    }
	}
    });

    var DutyEdit = React.createClass({

	createDuty: function(e){

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
		<form onSubmit={this.createDuty}>
		<label htmlFor="duty-name">Name</label>
		<input type="text" id="duty-name" className="form-control" name="duty-name"></input>
		<input type="submit" value="Create Duty"></input>
		</form>
		</div>);
	}
    });

    var Duty = React.createClass({

	saveDuty: function(e){

	    var duty = this.props.duty.restore();
	    duty.author = server.getUser();
	    duty.unsaved = undefined;
	    server.api.dutyReq({data: duty})
	    .done(function(data){
		console.log("good");
		console.log(data);
	    })
	    .fail(function(data){
		console.log("fail");
		console.log(data);
	    });
		
	},
	
	render: function(){
	    var self = this;

	    var reports = {};
	    var penalty = {};

	    var tasks = hs.partition(
		function(t){return t.entrusted;},
		this.props.duty ? this.props.duty.tasks : []);

	    var boundTasks = tasks[0];
	    var freeTasks = tasks[1];

	    hs.map(
		function(task){
		    reports[task.task_id] = task.reports;
		},
		this.props.duty ? this.props.duty.tasks : []);

	    hs.map(
		function(user){		
		    var loss = hs.fold(function(s,task){
			if(task.entrusted == user.username && self.props.duty.participants.length / 2 <= reports[task.task_id].length)
			    return s - task.penalty;
			else
			    return s;
		    },0,self.props.duty.tasks);
		    penalty[user.username] = loss;
		},
		this.props.duty ? this.props.duty.participants : []);


	    var taskSave = function(taskProps){
		
		var task = defs.TaskS.create({
		    name: taskProps.task_name,
		    //entrusted: "",
		    description: taskProps.task_description,
		    penalty: taskProps.task_penalty,
		    recurrent: false,
		    reports: []
		});

		self.props.duty.update({tasks: hs.concat([[task],self.props.duty.tasks])});
	    };

	    var dialog = (
		<Dialog id="task-edit">
		<TaskEdit onSubmit={taskSave} className="modal-body"/>
		</Dialog>);

	    var operations;

	    if(this.props.duty && this.props.duty.unsaved)
		operations = (
		    <div className="taskOperations">
		    {dialog}
		    <button type="button" className="btn btn-primary btn-sm" data-toggle="modal" data-target="#task-edit">Create Task</button>
		    <button type="button" onClick={this.saveDuty} className="btn btn-primary btn-sm">Save Duty</button>
		    </div>);
	    else
		operations = (
		    <div className="taskOperations">
		    {dialog}
		    <button type="button" className="btn btn-primary btn-sm" data-toggle="modal" data-target="#task-edit">Send Invite</button>
		    </div>);
		


	    if(this.props.duty)
		return (
		    <div className="duty" style={this.props.duty == {} ? {display:'none'} : {}}>
		    <h3>{this.props.duty.name}</h3>
		    <div className="participants">
		    {this.props.duty.participants.map(function(participant){
			return (<span className="label label-info"><span>{participant.username}</span> <span className="glyphicon btc-curr">&nbsp;</span><span>{penalty[participant.username] ? penalty[participant.username] : 0}</span></span>);
		    })}
		    </div>
		    {operations}
		    <div className="tasks">
		    <div>
		    <h4>Tasks Assigned to Users</h4>
		    {boundTasks.map(function(task){return <Task total={self.props.duty.participants.length} onReport={self.handleTaskUpdate} task={task}/>;})}
		    </div>
		    <div>
		    <h4>Free Tasks</h4>
		    {freeTasks.map(function(task){return <Task total={self.props.duty.participants.length} onReport={self.handleTaskUpdate} task={task}/>;})}
		    </div>
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

	loadDuties: function(){
	    
	    var self = this;
	    $.get(server.api.duties)
	    .done(function(data){

		self.setState({duties: hs.map(defs.DutyS.create)});
	    });

	},

	getInitialState: function(){
	    var self = this;
	    //this.loadDuties();

	    
	    var dutyList = this.props.duties ? this.props.duties : [];

	    for(var duty in dutyList){

		dutyList[duty].setUpdate(
		    function(){
			console.log('setting state');
			self.setState({x: 'y'});
		    });
	    }
	    return {duties: dutyList, duty: undefined};
	    
	    
	    //return {duties: [], duty: undefined};
	    
	},

	selectDuty: function(duty){
	    this.setState({duty: duty});
	},

	saveDuty: function(dutySpec){
	    
	    var self = this;
	    var duty = {
		name: dutySpec.duty_name,
		participants: [],
		tasks: [],
		unsaved: true
	    };

	    server.saveDuty(duty,function(result){

		if(!result.error){

		    var dutyS = defs.DutyS.create(duty);

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

    var tasks = hs.map(defs.TaskS.create,[{name: "Task", entrusted: "user2", description: "Task description", penalty: 50, reports: []}]);
    var users = [{username: "user1"},{username: "user2"}]; //hs.map(validator(schema.User),[{username: "user1"},{username: "user2"}]);
    var duties = hs.map(defs.DutyS.create,[{name: "duty1", participants: users, tasks: tasks},{name: "duty2", participants: users, tasks:[]}]);
    
    console.log("Render");

    ui.render({
	nav: ui.LoggedMenu,
	title: <h2>Duties</h2>,
	body: <Duties duties={duties}/>    
    });
});