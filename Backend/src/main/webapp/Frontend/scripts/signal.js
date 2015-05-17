define(["hs"],function(hs){

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

			isSignal: true,

			restore: function(){
			    
			    var res = {};
			    for(var prop in spec){

				var type = spec[prop].type;

				if(type == prim)
				    res[prop] = this[prop];

				else if(type.type == signalT)
				    res[prop] = this[prop].restore();
				else if(type.type && type.type == arrayT && type.of.type == signalT){
				    res[prop] = hs.map(function(v){return v.restore()},this[prop]);
				}
			    }

			    return res;
			},

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
				throw ("The property " + prop + " is not defined in spec: " + JSON.stringify(spec));

			    this[prop] = value;

			    function setSignal(obj_,constr){

				var obj = obj_.isSignal ? obj_ : constr(obj_)
				//if(!obj.isSignal)
				    

				obj.setUpdate(function(){self.update.apply(self,[])});
			    }

			    if(type.type == signalT){
				
				setSignal(self[prop],type.type.create);
			    }

			    else if(type.type && type.type.type == arrayT && type.type.of.type == signalT){

				for(var i in self[prop]){
				    console.log(type.type.of);
				    setSignal(self[prop][i],type.type.of.create);
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
