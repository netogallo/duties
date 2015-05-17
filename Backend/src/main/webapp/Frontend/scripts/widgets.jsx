define(["defs","util","hs"],function(defs,util,hs){

    var Search = React.createClass({

	onChange: function(event){
	    var self = this;
	    if(this.props.onChange)
		this.props.onChange(
		    $('input[name="suggest-text"]').val(),
		    function(results){
			self.props.sig.update({suggestions: results});
		    });
	},

	onClick: hs.curry(function(self,value,event){

	    var changed = false;
	    for(var sel in self.props.sig.selection){

		if(self.props.sig.selection[sel] == value){
		    changed = true;
		}
	    }

	    
	    if(!changed)
		self.props.sig.selection.push(value);

	    console.log(self.props.sig.selection);
	    self.props.sig.update({selection: self.props.sig.selection});
	}),
	
	render: function(){
	    var self = this;

	    return (
		<div className="search-box">
		<label htmlFor="suggest-text">Search User</label>
		<input name="suggest-text" id="suggest-text" onChange={this.onChange} type="text"/>
		<div className="selection-box">
		{this.props.sig.selection.map(
		    function(elem){
			return (<button type="button" className="btn btn-default"> {elem.str} </button>);
		    })}
		</div>
		<div className="suggest-box">
		{this.props.sig.suggestions.map(
		    function(elem){
			console.log(elem);
			return (<button type="button" onClick={self.onClick(self)(elem)} className="btn btn-default">{elem.str}</button>);
		    })}
		</div>
		</div>);
	}
    });

    var InviteTask = React.createClass({
	
	getInitialState: function(){
	    console.log("state");
	    return {selected: this.props.selected ? true : false};
	},

	onChange: function(value){
	    
	    this.props.task.update({status: !this.props.task.status});
	},

	render: function(){
	    var task = this.props.task.value;
	    console.log(task);
	    return (
		<div className="task col-md-4">
		<div className="taskHead">
		<h3><input onChange={this.onChange} type="checkbox" value={this.props.task.status}></input>{task.name}</h3>
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
    

    var List = React.createClass({

	select: function(k,v){
	    var self = this;
	    
	    return function(e){
		if(self.props.select)
		    self.props.select(k,v,e);
	    }
	},

	render: function(){

	    var vals = util.mapi(
		function(i,v){return {key:i,value:v};},
		this.props.items);

	    var self = this;

	    return (
		<div className="widget-list list-group">
		{vals.map(function(d){
		    var classes=["list-group-item"];
		    if(d.key==self.props.active){
			classes.push("active");
		    }
		    return (
			<a onClick={self.select(d.key,d.value)} className={hs.unwords(classes)}>
			{d.value}
			</a>);
		})}
		</div>);
	}
    });

    return {
	List: List,
	InviteTask: InviteTask,
	Search: Search
    };
});