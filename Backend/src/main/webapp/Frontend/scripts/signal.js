define(function(){

    var prim = {type: 'prim'};

    var signalT = 'signal';
    
    var arrayT = 'array';

    var array = {

	of: function(type){return {type: arrayT, of: type};}
    };

    return {
	
	Types: {
	    prim: prim,
	    array: array
	},

	Signal: function(spec){

	    return {
		    
		type: signalT,

		create: function(obj){

		    var res = {

			update: function(fields){

			    for(field in fields){
				
				this.setProp(field,fields[field]);

			    }

			    this.updateFn ? this.updateFn() : null;
			},

			setUpdate: function(update){
			    
			    this.updateFn = update;
			},
			
			setProp : function(prop,value){
			    
			    var type = spec[prop];
			    var self = this;

			    if(!type)
				throw ("The property " + prop + " is not defined in spec: " + JSON.stringyfy(spec));

			    this[prop] = value;

			    function setSignal(obj){

				obj.setUpdate(function(){self.update.apply(self,[])});
			    }

			    if(type.type == signalT){
				
				setSignal(self[prop]);
			    }

			    else if(type.type && type.type.type == arrayT && type.type.of.type == signalT){

				for(var i in self[prop]){
				    console.log(type.type.of);
				    setSignal(self[prop][i]);
				}
			    }
			}
		    };

		    res.update(obj);
		    return res;
		}
	    }
	    
	}

    };

});
