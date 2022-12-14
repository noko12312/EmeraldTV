// Removes elements by classname
function removeElementsByClass(classname){
    let elements = document.getElementsByClassName(classname);
    while(elements.length > 0){
        elements[0].parentNode.removeChild(elements[0]);
    }
}

function removeSingleElementByClass(classname, pos){
    let elements = document.getElementsByClassName(classname);
    if(elements[pos] != null){
        elements[pos].parentNode.removeChild(elements[pos]);
    }

}

function disableSelectionByClass(classname){
    let elements = document.getElementsByClassName(classname);
    for(let i=0; i<elements.length; i++){
        elements[i].tabIndex = -1;
    }
}

function replaceWithTextByClass(classname){
    var d;
    let elements = document.getElementsByClassName(classname);
    for(let i=0; i<elements.length; i++){
        d = document.createElement("p");
        d.innerHTML = elements[i].innerHTML;
        elements[i].parentNode.replaceChild(d, elements[i]);
    }
}