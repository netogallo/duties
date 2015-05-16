define([],function(){

    var Container = React.createClass({

	render: function(){
	
	    return (
		<div className="container">
		<div className="navbar navbar-static">
		<div className="navbar-inner">
		{this.props.nav.map(function(n){
		    return (<li><a href={n.href}>{n.name}</a></li>);
		})}
		</div>
		</div>
		<div className="container-head">
		{this.props.title}
		</div>
		<div className="container-body">
		{this.props.body}
		</div>
		</div>);
	}

    });

    return {

	Container: Container,
	render: function(c){

	    React.render(
		<Container nav={c.nav} title={c.title} body={c.body} />,
		document.getElementById('main')
	    );
	}
    };
})