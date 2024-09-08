document.addEventListener('DOMContentLoaded', function () {
    // Populate the dropdown with collection names on page load
    populateCollectionDropdown();
});

    function populateCollectionDropdown() {
        axios.get('/collections') // Endpoint to get collection names
            .then(response => {
                const collections = response.data;
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
            })
            .catch(error => {
                console.error('Failed to fetch collections:', error);
    });
    }    

// Bring the data from mongodb
let arr = [];
const distributionList = document.getElementById('distributionList');
distributionList.addEventListener('change',async function () {
    const selectedCollection = distributionList.value;
    console.log(selectedCollection);

    try{
        const data = await axios.get(`/collections/${selectedCollection}`);
    // console.log(data.data.tableData)
    
    const tableData = data.data.tableData;

    arr = []
    tableData.forEach((obj)=>{
        arr.push(obj.Email)
    })

    displayArray(arr);
    // console.log(arr)
    }
    catch(err){
        console.log(err);
    }
});

// Just to Display Array to check whether emails added or not
function displayArray(arr){
    console.log(arr);
}

document.getElementById('sendButton').addEventListener('click', function () {
    // Show loading modal
    var loadingModal = new bootstrap.Modal(document.getElementById('loadingModal'));
    loadingModal.show();

    // Collect form data
    const form = document.getElementById('emailForm');
    const formData = new FormData(form);

    const composedData = JSON.parse(localStorage.getItem('compose'));    

    const data = {
        distributionList: formData.get('distributionList'),
        segmentList: formData.get('segmentList'),
        senderEmail: formData.get('senderEmail'),
        senderName: formData.get('senderName'),
        sendInterval: formData.get('sendInterval') || 0,
        timeUnit: formData.get('timeUnit'),
        smtpServer: formData.get('smtpServer'),
        authenticationRequired: formData.get('authenticationRequired') === 'on', // Check for 'on' as checkboxes submit 'on' when checked
        username: formData.get('username'),
        password: formData.get('password'),
        port: formData.get('port'),
        ssl: formData.get('ssl') === 'on', // Check for 'on' as checkboxes submit 'on' when checked
        sendMode: formData.get('sendMode'),
        subject: composedData.subject,
        message: composedData.message,
        plainTextMessage: composedData.plainTextMessage,
        to: composedData.to
    };

    // Make an API call to send the email
    axios.post('/sendemailsToA', data)
        .then(response => {
            // Hide loading modal
            loadingModal.hide();
            
            // Show result modal
            displayResultModal(response.data);
        })
        .catch(error => {
            // Hide loading modal
            loadingModal.hide();
            console.error('Error sending emails:', error);
        });
});

function displayResultModal(result) {
    const sentEmails = result.sentEmails || [];
    const failedEmails = result.failedEmails || [];

    // Construct modal content
    const sentEmailsHtml = sentEmails.map(email => `<li>${email}</li>`).join('');
    const failedEmailsHtml = failedEmails.map(email => `<li>${email}</li>`).join('');

    document.getElementById('modal-sent-emails').innerHTML = sentEmailsHtml;
    document.getElementById('modal-failed-emails').innerHTML = failedEmailsHtml;

    // Show result modal
    var resultModal = new bootstrap.Modal(document.getElementById('resultModal'));
    resultModal.show();
}



















// document.getElementById('sendButton').addEventListener('click', function () {
//     // Show loading modal
//     var loadingModal = new bootstrap.Modal(document.getElementById('loadingModal'));
//     loadingModal.show();

//     // Collect form data
//     const form = document.getElementById('emailForm');
//     const formData = new FormData(form);
//     console.log(form)

//     // const data = {};
//     //     formData.forEach((value, key) => {
//     //         data[key] = value;
//     //     });

//     const composedData = JSON.parse(localStorage.getItem('compose'));    
//     console.log(composedData.to)
//     const data = {
//         distributionList: formData.get('distributionList'),
//         segmentList: formData.get('segmentList'),
//         senderEmail: formData.get('senderEmail'),
//         senderName: formData.get('senderName'),
//         sendInterval: formData.get('sendInterval') || 0,
//         timeUnit: formData.get('timeUnit'),
//         smtpServer: formData.get('smtpServer'),
//         authenticationRequired: formData.get('authenticationRequired') === 'on', // Check for 'on' as checkboxes submit 'on' when checked
//         username: formData.get('username'),
//         password: formData.get('password'),
//         port: formData.get('port'),
//         ssl: formData.get('ssl') === 'on', // Check for 'on' as checkboxes submit 'on' when checked
//         sendMode: formData.get('sendMode'),
//         subject : composedData.subject,
//         message : composedData.message,
//         plainTextMessage : composedData.plainTextMessage,
//         to : composedData.to
//     };

//     console.log(data)

//     // Log data to verify
//     // console.log('Form Data:', data);

//     // Make an API call to send the email
//     axios.post('/sendemailsToA', data)
//         .then(response => {
//             // Hide loading modal
//             // console.log("hi")
//             loadingModal.hide();
//         })
//         .catch(error => {
//             // Hide loading modal
//             loadingModal.hide();
//             console.error('Error sending emails:', error);
//         });
// });


document.getElementById('uploadButton').addEventListener('click', function(event) {
    event.preventDefault(); // Prevent default form submission
    
    const form = document.getElementById('uploadEmailsForm');
    const formData = new FormData(form);
    console.log(form)
    axios.post('/uploademails', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
    .then(response => {
        alert('File uploaded and data processed successfully.');
        // Handle success response here if needed
        console.log(response.data);
    })
    .catch(error => {
        alert('Failed to upload file.');
        console.error(error);
    });
});

