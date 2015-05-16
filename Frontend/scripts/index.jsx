requirejs(["ui","hs"],function(ui,hs){

    var CreateUser = React.createClass({

	createUser: function(e){

	    e.preventDefault();
	    $.ajax
	},

	render: function(){

	    return (
		<div className={"register-user " + this.props.className}>
		<form onSubmit={this.createUser}>
		<label htmlFor="email">Email</label>
		<input type="text" id="email" className="form-control" name="email"></input>
		<label htmlFor="email">Password</label>
		<input type="password" id="password" className="form-control" name="password"></input>
		<input type="submit" value="Create User"></input>
		</div>);
		
    });
});