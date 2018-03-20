function post(path, params, method) {
    method = method || "post"; // Set method to post by default if not specified.

    // The rest of this code assumes you are not using a library.
    // It can be made less wordy if you use one.
    var form = document.createElement("form");
    form.setAttribute("method", method);
    form.setAttribute("action", path);

    for(var key in params) {
        if(params.hasOwnProperty(key)) {
            var hiddenField = document.createElement("input");
            hiddenField.setAttribute("type", "hidden");
            hiddenField.setAttribute("name", key);
            hiddenField.setAttribute("value", params[key]);

            form.appendChild(hiddenField);
        }
    }

    document.body.appendChild(form);
    form.submit();
}

function loadKey(aft) {
    var un = localStorage.getItem("shopuser");
    var pk = localStorage.getItem("shoppass");
    //var aft = req.headers['__anti-forgery-token'];
    if(un && pk) {
        post('/login', {username: un, password: pk, '__anti-forgery-token': aft});
    }
}
