define(["signal"],function(signal){
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
	reports: {type: Types.prim},
	recurrent: {type: Types.prim}
    });

    var DutyS = Signal({
	id: {type: Types.prim},
	name: {type: Types.prim},
	author: {type: Types.prim},
	participants: {type: Types.prim},
	tasks: {type: Types.array.of(TaskS)},
	unsaved: {type: Types.prim}
    });

    return {
	InviteTaskS: InviteTaskS,
	TaskS : TaskS,
	DutyS : DutyS
    };
});
