define(function(){

    var hs = prelude('prelude-ls');

    var if_ = hs.curry(function(cond,yes,no){

	if(cond)
	    return yes;
	else
	    return no;
    });

    return {
	if_ : if_,
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
