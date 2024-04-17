function deleteFile(event) {
    let filename = event.currentTarget.getAttribute("data");
    filename = encodeURIComponent(filename);
    let url = `webeditor.ashx?type=removeforgotten&filename=${filename}`;

    fetch(url, {
        headers: {
            "Content-Type": "application/json",
        }
    }).then(result => {
        if(result.status == 204) {
            document.location.reload(true);
        }
    });
}

document.querySelectorAll('.delete-file').forEach(el => {
    el.addEventListener('click', deleteFile);
});