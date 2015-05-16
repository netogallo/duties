define(["signal"],function(signal){
    var Types = signal.Types;
    var Signal = signal.Signal;

    var InviteTaskS = Signal({
	    selected: {type: Types.prim},
	    task: {type: Types.prim}
    });

    var TaskS = Signal({
	name: {type: Types.prim},
	entrusted: {type: Types.prim},
	description: {type: Types.prim},
	penalty: {type: Types.prim},
	votes: {type: Types.prim}
    });

    var DutyS = Signal({
	name: {type: Types.prim},
	participants: {type: Types.prim},
	tasks: {type: Types.array.of(TaskS)}
    });

    return {
	InviteTaskS: InviteTaskS,
	TaskS : TaskS,
	DutyS : DutyS
    };
});