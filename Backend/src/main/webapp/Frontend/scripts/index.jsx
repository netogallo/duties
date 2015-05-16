requirejs(["ui","hs","server"],function(ui,hs,server){

    var CreateUser = React.createClass({

	createUser: function(e){

	    e.preventDefault();
	    $.post(server.api.user,
	    {json: JSON.stringify({
		username: $('input[name="email"]').val(),
		password: $('input[name="password"]').val(),
		id: "fasd89h38h"
	    })})
	    .done(
		function(data){
		    console.log("good");
		    console.log(data);
		})
	    .fail(
		function(data){
		    console.log("bad");
		    console.log(data);
		});
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

    ui.render({
	title: "Duites!",
	nav: [],
	body: (<Homepage />)
    });
});