define(["defs","util","hs"],function(defs,util,hs){

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
	List: List
    };
});