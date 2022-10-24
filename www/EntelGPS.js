var exec = require('cordova/exec');
exports.startGPS = function (arg0, success, error) {
    exec(success, error, 'EntelGPS', 'startGPS', [arg0]);
};
