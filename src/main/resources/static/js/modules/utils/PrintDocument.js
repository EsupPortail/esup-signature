export class PrintDocument {

    constructor() {
        console.info("init PrintDocument");
    }

    launchPrint(url) {
        console.info("printing : " + url);
        $.get(url, function( data ) {
            printJS({printable: data, type: 'pdf', base64: true})
        });

    }

}
