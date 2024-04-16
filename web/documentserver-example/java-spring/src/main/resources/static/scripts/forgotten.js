function deleteFile(event) {
    let filename = event.currentTarget.getAttribute("data");
    filename = encodeURIComponent(filename);
    let url = `/forgotten/${filename}`;

    fetch(url, {
        method: "DELETE",
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