function post(params) {
    var form = document.createElement("form");
    form.setAttribute("method", 'post');
    form.setAttribute("action", '/login');
    for(var key in params) {
        if(params.hasOwnProperty(key)) {
            var hiddenField = document.createElement("input");
            hiddenField.setAttribute("type", "hidden");
            hiddenField.setAttribute("name", key);
            hiddenField.setAttribute("value", params[key]);
            form.appendChild(hiddenField);}}
    document.body.appendChild(form);
    form.submit();}

function loadKey(aft) {
    var un = localStorage.getItem("shopuser");
    var pk = localStorage.getItem("shoppass");
    if(un && pk) { post({username: un, password: pk, '__anti-forgery-token': aft}); }}
