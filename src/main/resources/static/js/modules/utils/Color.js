export class Color {


    static changeColInUri(data, colfrom, colto) {
        return new Promise((resolve, reject) => {
            let img = document.createElement("img");
            img.onload = function() {
                let canvas = document.createElement("canvas");
                img.style.visibility = "hidden";
                document.body.appendChild(img);

                canvas.width = img.offsetWidth;
                canvas.height = img.offsetHeight;

                let ctx = canvas.getContext("2d");
                ctx.drawImage(img, 0, 0);

                img.parentNode.removeChild(img);

                let imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
                data = imageData.data;

                let rgbfrom = Color.hexToRgb(colfrom);
                let rgbto = Color.hexToRgb(colto);

                let r, g, b;
                for (let x = 0, len = data.length; x < len; x += 4) {
                    r = data[x];
                    g = data[x + 1];
                    b = data[x + 2];

                    if ((r === rgbfrom[0]) &&
                        (g === rgbfrom[1]) &&
                        (b === rgbfrom[2])) {

                        data[x] = rgbto[0];
                        data[x + 1] = rgbto[1];
                        data[x + 2] = rgbto[2];

                    }
                }
                ctx.putImageData(imageData, 0, 0);
                resolve(canvas.toDataURL());
            }
            img.src = data;
        });
    }

    static hexToRgb(hex) {
        // Expand shorthand form (e.g. "03F") to full form (e.g. "0033FF")
        const shorthandRegex = /^#?([a-f\d])([a-f\d])([a-f\d])$/i;
        hex = hex.toString().replace(shorthandRegex, (m, r, g, b) => {
            return r + r + g + g + b + b;
        });

        const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
        return result
            ? [
                parseInt(result[1], 16),
                parseInt(result[2], 16),
                parseInt(result[3], 16),
            ]
            : null;
    }

    static rgbToHex(r, g, b) {
        return "#" + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1);
    }

}