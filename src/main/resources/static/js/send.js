document.addEventListener('DOMContentLoaded', function () {
    // Populate the dropdown with collection names on page load
    populateCollectionDropdown();

    // Add event listener for dropdown changes
    document.getElementById('distributionList').addEventListener('change', function () {
        const collectionName = this.value;
        if (collectionName) {
            fetchTableData(collectionName);
        }
    });
});


function populateCollectionDropdown() {
    const xhr = new XMLHttpRequest();
    xhr.open('GET', '/collections', true); // Endpoint to get collection names
    xhr.onload = function () {
        if (xhr.status === 200) {
            const collections = JSON.parse(xhr.responseText);
            const dropdown = document.getElementById('distributionList');
            dropdown.innerHTML = ''; // Clear existing options

            // Create default option
            const defaultOption = document.createElement('option');
            defaultOption.value = '';
            defaultOption.innerText = 'Choose a Collection';
            defaultOption.selected = true;
            defaultOption.disabled = true;
            dropdown.appendChild(defaultOption);

            // Populate dropdown with collection names
            collections.forEach(collection => {
                const option = document.createElement('option');
                option.value = collection;
                option.innerText = collection;
                dropdown.appendChild(option);
            });
        } else {
            console.error('Failed to fetch collections:', xhr.status, xhr.statusText);
        }
    };
    xhr.onerror = function () {
        console.error('Request failed');
    };
    xhr.send();
}

function fetchTableData(collectionName) {
    console.log('Fetching table data for:', collectionName);
    const xhr = new XMLHttpRequest();
    xhr.open('GET', `/collections/${collectionName}`, true); // Updated endpoint
    xhr.onload = function () {
        if (xhr.status === 200) {
            const response = JSON.parse(xhr.responseText);
            // console.log('Table headers:', response.tableHeaders);
            localStorage.setItem('collectionHeaders', response.tableHeaders);
            localStorage.setItem('table', collectionName);
            // console.log('Stored headers in local storage:', localStorage.getItem('collectionHeaders'));

            console.log('Response from server:', response);
            console.log('Table headers:', response.tableHeaders);
            console.log('Table data:', response.tableData);

            // Display the table
            displayTable({
                tableHeaders: response.tableHeaders,
                tableData: response.tableData
            });
        } else {
            console.error('Failed to fetch table data:', xhr.status, xhr.statusText);
        }
    };
    xhr.onerror = function () {
        console.error('Request failed');
    };
    xhr.send();
}

const fileInput = document.getElementById('fileInput');
    const collectionNameInput = document.getElementById('collectionName');
    const uploadButton = document.getElementById('uploadButton');
    const messageDiv = document.getElementById('message');

    // Function to validate inputs
    function validateInputs() {
        const fileSelected = fileInput.files.length > 0;
        const collectionName = collectionNameInput.value.trim();
        
        if (fileSelected && collectionName) {
            uploadButton.disabled = false;
            messageDiv.style.display = 'none';
        } else {
            uploadButton.disabled = true;
            if (!fileSelected || !collectionName) {
                messageDiv.innerText = 'Both file and collection name are required.';
                messageDiv.style.display = 'block';
            } else {
                messageDiv.style.display = 'none';
            }
        }
    }

    // Event listeners for input changes
    fileInput.addEventListener('change', validateInputs);
    collectionNameInput.addEventListener('input', validateInputs);

    // Handle the upload button click
    uploadButton.addEventListener('click', function () {
        const fileSelected = fileInput.files.length > 0;
        const collectionName = collectionNameInput.value.trim();

        if (fileSelected && collectionName) {
            uploadFile(); // Call the function to handle file upload
        } else {
            // Display a message if validation fails
            messageDiv.innerText = 'Both file and collection name are required.';
            messageDiv.style.display = 'block';
            setTimeout(() => {
                messageDiv.style.display = 'none';
            }, 3000); // Hide the message after 3 seconds
        }
    });
    

function uploadFile() {
    const form = document.getElementById('uploadForm');
    const formData = new FormData(form);

    const xhr = new XMLHttpRequest();
    xhr.open('POST', '/upload', true);
    xhr.onload = function () {
        if (xhr.status === 200) {
            const response = JSON.parse(xhr.responseText);
            console.log('File uploaded successfully:', response);
            populateCollectionDropdown();
            displayTable({
                tableHeaders: response.tableHeaders,
                tableData: response.tableData
            });
        } else {
            console.error('Failed to upload file:', xhr.status, xhr.statusText);
            document.getElementById('message').innerText = 'Failed to upload file.';
        }
    };
    xhr.onerror = function () {
        console.error('Request failed');
    };
    xhr.send(formData);
}

function displayTable(data) {
    console.log("Displaying table...");
    const tableContainer = document.getElementById('tableContainer');
    tableContainer.innerHTML = '';

    // Create table
    const table = document.createElement('table');
    table.id = 'dataTable';
    table.classList.add('table', 'table-striped');

    // Table header
    const thead = document.createElement('thead');
    const headerRow = document.createElement('tr');
    data.tableHeaders.forEach(header => {
        const th = document.createElement('th');
        th.innerText = header;
        headerRow.appendChild(th);
    });
    thead.appendChild(headerRow);
    table.appendChild(thead);

    // Table body
    const tbody = document.createElement('tbody');
    data.tableData.forEach(row => {
        const tr = document.createElement('tr');
        data.tableHeaders.forEach(header => {
            const td = document.createElement('td');
            td.innerText = row[header] || '';
            td.contentEditable = true;
            tr.appendChild(td);
        });
        tbody.appendChild(tr);
    });
    table.appendChild(tbody);

    // Append table
    tableContainer.appendChild(table);

    // Show the save button
    document.getElementById('saveContainer').style.display = 'block';
}


// Delete functionality

document.getElementById('deleteButton').addEventListener('click', function () {
    const collectionName = document.getElementById('distributionList').value;
    if (collectionName) {
        deleteCollection(collectionName);
    } else {
        alert('Please select a collection to delete.');
    }
});

function deleteCollection(collectionName) {
    const xhr = new XMLHttpRequest();
    xhr.open('DELETE', `/collections/${collectionName}`, true); // Endpoint to delete a collection
    xhr.onload = function () {
        if (xhr.status === 200) {
            console.log('Collection deleted successfully:', collectionName);
            populateCollectionDropdown(); // Refresh the dropdown list

            // Clear the displayed table data
            const tableContainer = document.getElementById('tableContainer');
            tableContainer.innerHTML = '';
            document.getElementById('saveContainer').style.display = 'none';
        } else {
            console.error('Failed to delete collection:', xhr.status, xhr.statusText);
            alert('Failed to delete collection.');
        }
    };
    xhr.onerror = function () {
        console.error('Request failed');
    };
    xhr.send();
}

function submitData() {
    const table = document.querySelector('#dataTable');
    if (!table) {
        console.error('No table found to save.');
        return;
    }

    // Extract updated table data
    const rows = Array.from(table.querySelectorAll('tbody tr'));
    const data = rows.map(row => {
        const cells = Array.from(row.querySelectorAll('td'));
        const rowData = {};
        cells.forEach((cell, index) => { 
            const header = table.querySelectorAll('thead th')[index].innerText;
            rowData[header] = cell.innerText;
        });
        return rowData;
    });

    // Get the collection name
    const collectionName = document.getElementById('distributionList').value;

    // Send the updated data via AJAX
    const xhr = new XMLHttpRequest();
    xhr.open('POST', '/save', true);
    xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');

    xhr.onload = function () {
        if (xhr.status === 200) {
            const response = JSON.parse(xhr.responseText);
            console.log('Data saved successfully', response);
            
            // Keep the table visible and ensure the save button stays visible
            // Commented out to keep the save button visible
            // document.getElementById('saveContainer').style.display = 'none';
        } else {
            console.error('Failed to save data:', xhr.status, xhr.statusText);
        }
    };

    xhr.onerror = function () {
        console.error('Request failed');
    };

    // Send data to server
    xhr.send('tableData=' + encodeURIComponent(JSON.stringify(data)) + '&collectionName=' + encodeURIComponent(collectionName));
}



// Toggle sidebar functionality
const hamBurger = document.querySelector(".toggle-btn");
hamBurger.addEventListener("click", function () {
    document.querySelector("#sidebar").classList.toggle("expand");
});

