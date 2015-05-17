define(["server"],function(server){
    var Container = React.createClass({

	login: function(e){

	    e.preventDefault();
	    server.api.loginReq({
		data: {
		    username: $('input[name="email-login"]').val(),
		    password: $('input[name="password-login"]').val()
		}
	    })
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
		<div className="container">
		<div className="navbar navbar-static">
		<div className="navbar-inner">
		<ul role="navigation" className="nav">
		{this.props.nav.map(function(n){
		    return (<li><a href={n.href}>{n.name}</a></li>);
		})}
		<li className="dropdown">
                <a href="#" data-toggle="dropdown" className="dropdown-toggle">Log In<b className="caret"></b></a>
                <ul className="dropdown-menu">
                <li>
		<form onSubmit={this.login}>
		<label htmlFor="email-login">Email</label>
		<input type="text" id="email-login" className="form-control" name="email-login"></input>
		<label htmlFor="password-login">Password</label>
		<input type="password" id="password-login" className="form-control" name="password-login"></input>
		<input type="submit" value="Log In"></input>
		</form>
		</li>
                </ul>
		</li>
		</ul>
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

    var LoggedMenu = [
	{name: "Invites", href: ""}
    ];

    return {

	Container: Container,
	LoggedMenu: LoggedMenu,
	render: function(c){

	    React.render(
		<Container nav={c.nav} title={c.title} body={c.body} />,
		document.getElementById('main')
	    );
	}
    };
})