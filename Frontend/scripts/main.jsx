$.getJSON(
    "scripts/schema.json",
    function(schema){
	
	var hs = require('prelude-ls');

	tv4.addSchema('User',schema.User);
	tv4.addSchema('Task',schema.Task);
	tv4.addSchema('Duty',schema.Duty);

	var DutyList = React.createClass({

	    getInitialState: function(){
		
		return {duties: this.props.duties ? this.props.duties : []};
	    },

	    render: function(){

		var self = this;

		var clickItem = hs.curry(function(duty,e){
		    self.setState({active: duty});
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

	var Duties = React.createClass({

	    getInitialState: function(){
		return {duties: this.props.duties ? this.props.duties : []};
	    },

	    addDuties: function(duties){
		
		this.setState({duties: hs.concat([[duties],this.state.duties])});
	    },

	    render: function(){

		return (<div className="duties">
			<DutyList duties={this.state.duties}/>
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
	
	
	var duties = [{name: "duty1"},{name: "duty2"}];

	React.render(<Duties duties={duties}/>,
	    document.getElementById('main')
	);
    });