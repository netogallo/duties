define(["signal","hs"],function(signal,hs){
    var Types = signal.Types;
    var Signal = signal.Signal;

    var InviteTaskS = Signal({
	    selected: {type: Types.prim},
	    task: {type: Types.prim}
    });

    var TaskS = Signal({
	id: {type: Types.prim},
	name: {type: Types.prim},
	entrusted: {type: Types.prim},
	description: {type: Types.prim},
	penalty: {type: Types.prim},
	recurrent: {type: Types.prim},
	state: {type: Types.prim},
	reported_by: {type: Types.prim},
	payments: {type: Types.prim}
    });

    var DutyS = Signal({
	id: {type: Types.prim},
	name: {type: Types.prim},
	author: {type: Types.prim},
	participants: {type: Types.prim},
	tasks: {type: Types.array.of(TaskS)},
	unsaved: {type: Types.prim},
    });

    var CheckS = Signal({
	status: {type: Types.prim},
	value:  {type: Types.prim},
    });

    var SuggestS = Signal({
	selection: {type: Types.prim},
	suggestions: {type: Types.prim}
    });

    return {
	InviteTaskS: InviteTaskS,
	TaskS : TaskS,
	DutyS : DutyS,
	CheckS: CheckS,
	SuggestS: SuggestS,
	readChecks: function(sigs){

	    return hs.map(
		function(v){return v.value;},
		hs.filter(function(v){return v.status;},sigs));
	}
    };
});
