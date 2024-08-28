// Initialize CKEditor
CKEDITOR.replace('message', {
    allowedContent: true,
    extraAllowedContent: 'div(*); a[*]',
});

// Retrieve headers from local storage and split by comma
const headersString = localStorage.getItem('collectionHeaders');
const headers = headersString ? headersString.split(',') : [];
console.log(headers);

// Function to populate dropdown with headers
function populateDropdown(dropdownId) {
    const dropdown = document.getElementById(dropdownId);
    dropdown.innerHTML = ''; // Clear existing content
    headers.forEach(header => {
        const li = document.createElement('li');
        li.textContent = header.trim(); // Trim whitespace from headers
        li.addEventListener('click', () => {
            // Handle header selection here
            console.log('Selected:', header);
            insertHeaderToInput(header.trim(), dropdownId);
            dropdown.classList.remove('dropdown-show'); // Close the dropdown
        });
        dropdown.appendChild(li);
    });
}

// Function to insert header value into the corresponding input field
function insertHeaderToInput(header, dropdownId) {
    let inputFieldId;
    if (dropdownId === 'dropdown-to') {
        inputFieldId = 'to';
    } else if (dropdownId === 'dropdown-subject') {
        inputFieldId = 'subject';
    }

    if (inputFieldId) {
        const inputField = document.getElementById(inputFieldId);
        if (inputField) {
            const formattedHeader = `#${header}#`; // Format the header with `#`
            if (inputField.value) {
                inputField.value += ' ' + formattedHeader; // Add space before the next header
            } else {
                inputField.value = formattedHeader; // Set the value directly if empty
            }
        }
    }
}

// Populate dropdowns with headers
populateDropdown('dropdown-to');
populateDropdown('dropdown-subject');

// Handle "Choose Template" button click
document.querySelector('button#choose-template').addEventListener('click', function() {
    console.log('Choose Template button clicked');
    const templateModal = document.getElementById('template-modal');
    const uploadSection = document.getElementById('upload-section');
    
    // Show the modal and fetch templates
    uploadSection.style.display = 'none'; // Hide the upload section initially
    fetchTemplates();
    templateModal.style.display = 'block';
});

// Handle "Upload New Template" button click
document.getElementById('show-upload-section').addEventListener('click', function() {
    const uploadSection = document.getElementById('upload-section');
    uploadSection.style.display = 'block'; // Show the upload section
});

// Handle file upload
document.getElementById('upload-template-button').addEventListener('click', function() {
    const fileInput = document.getElementById('upload-template');
    const uploadMessage = document.getElementById('upload-message');
    
    if (fileInput.files.length === 0) {
        uploadMessage.textContent = 'No file selected.';
        return;
    }
    
    const file = fileInput.files[0];
    const reader = new FileReader();
    
    reader.onload = function(event) {
        const content = event.target.result;
        CKEDITOR.instances['message'].setData(content);
        uploadMessage.textContent = 'File uploaded and content loaded.';
    };
    
    reader.onerror = function() {
        uploadMessage.textContent = 'Error reading file.';
    };
    
    reader.readAsText(file);
});

// Fetch predefined templates
function fetchTemplates() {
    const templates = [
        { id: 'template1', name: 'Template 1', content: '<h1>Welcome</h1><p>This is template 1.</p>' },
        { id: 'template2', name: 'Template 2', content: '<h1>Greeting</h1><p>This is template 2.</p>' },
        // Add more templates as needed
    ];
    
    const templateList = document.getElementById('template-list');
    templateList.innerHTML = '';
    
    templates.forEach(template => {
        const listItem = document.createElement('li');
        listItem.textContent = template.name;
        listItem.style.cursor = 'pointer';
        listItem.addEventListener('click', function() {
            if (template.content) {
                CKEDITOR.instances['message'].setData(template.content);
            } else if (template.path) {
                fetch(template.path)
                    .then(response => response.text())
                    .then(content => {
                        CKEDITOR.instances['message'].setData(content);
                    })
                    .catch(error => console.error('Error loading template:', error));
            }
            
            document.getElementById('template-modal').style.display = 'none';
        });
        templateList.appendChild(listItem);
    });
}

// Close modal when clicking on the close button
document.getElementById('close-modal').addEventListener('click', function() {
    console.log('Close button clicked');
    document.getElementById('template-modal').style.display = 'none';
});

// Close modal when clicking outside the modal content
window.addEventListener('click', function(event) {
    console.log('Window click event');
    if (event.target === document.getElementById('template-modal')) {
        document.getElementById('template-modal').style.display = 'none';
    }
});

// Toggle dropdown visibility
document.querySelectorAll('.field-button').forEach(button => {
    button.addEventListener('click', function(event) {
        const dropdownId = this.getAttribute('data-dropdown');
        const dropdown = document.getElementById(dropdownId);
        dropdown.classList.toggle('dropdown-show');
        event.stopPropagation(); // Prevent click event from bubbling up
    });
});

// Close dropdown if clicked outside
window.addEventListener('click', () => {
    document.querySelectorAll('.dropdown-content.dropdown-show').forEach(dropdown => {
        dropdown.classList.remove('dropdown-show');
    });
});



// Handle checkbox change event
document.getElementById('show-plain-text').addEventListener('change', function() {
    const isChecked = this.checked;
    const ckeditorInstance = CKEDITOR.instances['message'];

    if (ckeditorInstance) {
        if (isChecked) {
            // Disable CKEditor
            ckeditorInstance.setReadOnly(true);
            // Add a class to make toolbar buttons look disabled
            document.querySelector('.cke').classList.add('disabled');
        } else {
            // Enable CKEditor
            ckeditorInstance.setReadOnly(false);
            // Remove the disabled class from toolbar buttons
            document.querySelector('.cke').classList.remove('disabled');
        }
    }
});



// Handle "Text from HTML" button click
document.getElementById('text-from-html').addEventListener('click', () => {
    const htmlContent = CKEDITOR.instances['message'].getData();
    // console.log(htmlContent);
    const formattedText = htmlToPlainText(htmlContent);
    document.getElementById('plain-text').value = formattedText;
});

function htmlToPlainText(html) {
    // Create a new DOMParser to parse the HTML
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');
    
    // Function to recursively extract text from elements
    function extractText(node) {
        let text = '';
        node.childNodes.forEach(child => {
            if (child.nodeType === Node.TEXT_NODE) {
                text += child.textContent.trim() + ' ';
            } else if (child.nodeType === Node.ELEMENT_NODE) {
                if (child.tagName === 'IMG') {
                    // Add a blank line for images
                    text += '\n';
                } else if (child.tagName === 'P' || child.tagName === 'DIV' || child.tagName === 'BR') {
                    // Add a blank line for block elements
                    text += '\n';
                }
                text += extractText(child);
            }
        });
        return text;
    }
    
    // Start extracting text from the body
    return extractText(doc.body).trim();
}



// Select the Save button
const saveButton = document.getElementById('save-button');

saveButton.addEventListener('click', async function() {
    console.log("hi");

    // Get values from the input fields
    let toInputValue = document.getElementById("to").value.trim();
    let emailList = [];

    const collectionName = localStorage.getItem('table');
    console.log("Collection Name:", collectionName);

    const parts = toInputValue.split(',');
    console.log("Parts:", parts);

    for (let part of parts) {
        part = part.trim();
        if (part.startsWith('#') && part.endsWith('#')) {
            const fieldName = part.substring(1, part.length - 1); // Extract the field name exactly as it is
            console.log("Field Name:", fieldName);

            try {
                const response = await axios.get(`/fetch-data`, {
                    params: {
                        collectionName: collectionName,
                        columnName: fieldName
                    }
                });

                if (response.status === 200) { // Check for successful response
                    const emailsFromDB = response.data;
                    console.log("Emails from DB:", emailsFromDB);

                    if (Array.isArray(emailsFromDB)) {
                        emailList.push(...emailsFromDB);
                    } else {
                        console.error("Unexpected data format:", emailsFromDB);
                    }
                } else {
                    console.error(`Error: Received status ${response.status}`);
                }

            } catch (error) {
                console.error(`Error fetching data for ${fieldName}:`, error);
            }
        } else {
            emailList.push(part); // Add manual emails directly to the list
        }
    }

    const subjectValue = document.getElementById('subject').value;

    // Check if "Plain Text" checkbox is checked
    const isPlainTextChecked = document.getElementById('show-plain-text').checked;

    // Get the CKEditor instance and its content
    const ckeditorInstance = CKEDITOR.instances.message;
    const messageContent = ckeditorInstance ? ckeditorInstance.getData() : '';

    // Check if plain text textarea has content
    const plainTextTextarea = document.getElementById('plain-text');
    const plainTextMessage = plainTextTextarea.value.trim() !== '' ? plainTextTextarea.value.trim() : '';
    console.log("Plain Text Message:", plainTextMessage);

    // Create an object to store in localStorage
    const composeData = {
        to: emailList,
        subject: subjectValue,
        message: isPlainTextChecked ? '' : messageContent, // Send empty HTML content if plain text is checked
        plainTextMessage: plainTextMessage
    };

    // Save the data in localStorage
    localStorage.setItem('compose', JSON.stringify(composeData));

    // Optional: Notify the user that data has been saved
    alert('Data has been saved!');

});

// Toggle sidebar functionality
const hamBurger = document.querySelector(".toggle-btn");
hamBurger.addEventListener("click", function () {
    document.querySelector("#sidebar").classList.toggle("expand");
});