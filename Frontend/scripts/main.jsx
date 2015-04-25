$.getJSON(
    "scripts/schema.json",
    function(schema){

	React.render(
            <h1>Hello, world!</h1>,
            document.getElementById('example')
	);
    });