define(["defs","util","hs"],function(defs,util,hs){

    var InviteTask = React.createClass({
	
	getInitialState: function(){

	    return {selected: this.props.selected ? true : false};
	},

	onChange: function(value){
	    
	    this.setState({selected: !this.state.selected});
	    if(this.props.onChange)
		this.props.onChange(this.state.selected);
	},

	render: function(){
	    var task = this.props.task;

	    return (
		<div className="task col-md-4">
		<div className="taskHead">
		<h3><input onChange={this.onChange} type="checkbox" value={this.state.selected}>Test</input></h3>
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
	InviteTask: InviteTask
    };
});