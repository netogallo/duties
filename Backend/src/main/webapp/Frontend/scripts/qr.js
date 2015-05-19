define([],function(){

    var queue = [];

    var cache = {};

    var qrCode = new QRCode(
	"qr-place",
	{
	    text: "JS is stupid",
	    width: 128,
	    height: 128
	});
    
    var mkQR = function(str){

	qrCode.makeCode(str);
	return $('#qr-place > img').attr('src');

    };

    return {
	mkQR: mkQR
    };

});
