define(["server","util"],function(server,util){
    var Container = React.createClass({

	getInitialState: function(){
	    var self = this;
	    server.onLogin(
		function(user,rep){

		    console.log(user);
		    if(!user.error){

			self.setState({user: user});
		    }
		});

	    return {user: undefined};
	},

	login: function(e){

	    e.preventDefault();
	    server.api.loginReq({
		data: {
		    username: $('input[name="email-login"]').val(),
		    password: $('input[name="password-login"]').val()
		}
	    });
	},

	render: function(){
	
	    var login = this.state.user ?
	        <li>Welcome! {this.state.user.username}</li>
		:
		(<li className="dropdown">
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
                </li>);


	    return (
		<div className="container">
		<div className="navbar navbar-static">
		<div className="navbar-inner">
		<ul role="navigation" className="nav nav-tabs">
		{this.props.nav.map(function(n){
		    return (<li><a href={n.href}>{n.name}</a></li>);
		})}
                
		{login}
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

    var host = "/Frontend/"

    var LoggedMenu = [
	{name: "Duties", href: host + "duties.html"},
	{name: "Invites", href: host + "invite.html"}
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