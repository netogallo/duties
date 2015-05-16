define(function(){

    var hs = prelude('prelude-ls');

    return {

	mapi: hs.curry(
	    function(f,xs){
		var xs_;
		if(xs.constructor == [].constructor)
		    xs_ = [];
		else
		    xs_ = {};

		for(var i in xs){

		    xs_[i] = f(i,xs[i])
		}

		return xs_;
	    })
    };
});
