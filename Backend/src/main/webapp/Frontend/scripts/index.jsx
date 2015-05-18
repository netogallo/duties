requirejs(["ui","hs","server"],function(ui,hs,server){

    var CreateUser = React.createClass({

	createUser: function(e){

	    e.preventDefault();
	    $.post(server.api.user,
	    {json: JSON.stringify({
		username: $('input[name="email"]').val(),
		password: $('input[name="password"]').val(),
	    })})
	    .done(function(data){
		ui.alertSuccess("Account has been created!");
	    })
	    .fail(function(data){
		ui.alertError("The username already exists!");
	    });
	},

	render: function(){

	    return (
		<div className={"register-user " + this.props.className}>
		<form onSubmit={this.createUser}>
		<label htmlFor="email">Email</label>
		<input type="text" id="email" className="form-control" name="email"></input>
		<label htmlFor="password">Password</label>
		<input type="password" id="password" className="form-control" name="password"></input>
		<input className="btn btn-default" type="submit" value="Create User"></input>
		</form>
		</div>);
	}
    });

    var Homepage = React.createClass({
	
	render: function(){

	    return(
		<div>
		<div className="splash col-md-8">
		</div>
		<div className="register col-md-4">
		<h3>Create an Account</h3>
		<CreateUser />
		</div>
		</div>
	    );
	}
    });

    server.onLogin(function(user,e){if(!user.error) window.location = 'duties.html'});

    ui.render({
	title: "Duites!",
	nav: [],
	body: (<Homepage />)
    });
});
