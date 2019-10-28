function check(register)
{
     document.getElementById("firstNameError").style.display = "none";
     document.getElementById("lastNameError").style.display = "none";
     document.getElementById("loginError").style.display = "none";
     document.getElementById("passwordError").style.display = "none";
     document.getElementById("emailError").style.display = "none";
     document.getElementById("birthdayError").style.display = "none";
     document.getElementById("genderError").style.display = "none";

    var status = true

	for (i = 0; i < register.length; i++)
	{
		var element = register.elements[i];
		if (!element.disabled && !element.readonly && (element.type == "text" || element.type == "password" || element.type == "date" || element.id == "gender") && element.value == "")
		{
		    var error = element.id + "Error"
            document.getElementById(error).style.display = "block";
			//alert("Please fill all fields!");

			status = false;
		}

		if ((element.name == "firstname" || element.name == "lastname"  || element.name == "email") && element.value.length < 3){
		    var error = element.id + "Error"
            document.getElementById(error).style.display = "block";
			//alert(element.name + " should be longer!");
        	status = false;
		}

		if ((element.type == "password" || element.name == "login") && element.value.length < 7){
			var error = element.id + "Error"
            document.getElementById(error).style.display = "block";
        	//alert(element.name + " should be at least 7 characters long!");
            status = false;
        }

	}

	if(checkUsernameAvailabe(element.value) == false){
            status = false;
    }

    if(status == true){
    addUser(register);
    }

	return status;
}

function checkFirstname(element)
 {
     document.getElementById("firstNameError").style.display = "none";

     if(element.value.length < 3){
         document.getElementById("firstNameError").style.display = "block";
     }

     return true;
 }

 function checkLastname(element)
 {
     document.getElementById("lastNameError").style.display = "none";

     if(element.value.length < 3){
         document.getElementById("lastNameError").style.display = "block";
     }

     return true;
 }

 function checkLogin(element)
 {

     if(checkUsernameAvailabe(element.value) == false){
        alert("exists");
     }

     document.getElementById("loginError").style.display = "none";

     if(element.value.length < 7){
         document.getElementById("loginError").style.display = "block";
     }

     return true;
 }

  function checkPassword(element)
  {
      document.getElementById("passwordError").style.display = "none";

      if(element.value.length < 7){
          document.getElementById("passwordError").style.display = "block";
      }

      return true;
  }

function checkEmail(element)
   {
       document.getElementById("emailError").style.display = "none";

       if(element.value.length < 3){
           document.getElementById("emailError").style.display = "block";
       }

       return true;
   }

function addUser(register){
        var xhr = new XMLHttpRequest();
        xhr.open('POST', 'http://localhost:8889/', false);
        xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
        var data = new FormData();
        data.append('firstname', 'document.getElementById(firstname)');
        data.append('lastname', 'document.getElementById(lastname)');
        data.append('login', 'document.getElementById(login)');
        data.append('password', 'document.getElementById(password)');
        data.append('email', 'document.getElementById(email)');
        data.append('birthday', 'document.getElementById(birthday)');
        data.append('gender', 'document.getElementById(gender)');
        xhr.send(data);

   }

   function checkUsernameAvailabe(name){

    available = true;
    var result = null;
    var xmlhttp = new XMLHttpRequest();
    xmlhttp.open("GET", 'http://localhost:8889/tmp/users', false);
    xmlhttp.send();
    if (xmlhttp.status==200) {
    result = xmlhttp.responseText;
    }

    var arrayOfLines = result.split("\n");

    var i=0;
    for(i; i< arrayOfLines.length; i++){
        if(arrayOfLines[i].trim() == name.trim()){
            return false;
        }

    }

  return available;

  }