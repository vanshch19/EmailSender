document.addEventListener('DOMContentLoaded', function () {
    // Populate the dropdown with collection names on page load
    populateCollectionDropdown();


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
});

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
    console.log(form)

    // const data = {};
    //     formData.forEach((value, key) => {
    //         data[key] = value;
    //     });

    const composedData = JSON.parse(localStorage.getItem('compose'));    
    console.log(composedData.to)
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
        subject : composedData.subject,
        message : composedData.message,
        plainTextMessage : composedData.plainTextMessage,
        to : composedData.to
    };

    console.log(data)

    // Log data to verify
    // console.log('Form Data:', data);

    // Make an API call to send the email
    axios.post('/send-email', data)
        .then(response => {
            // Hide loading modal
            // console.log("hi")
            loadingModal.hide();
            alert('Emails sent successfully!');
        })
        .catch(error => {
            // Hide loading modal
            loadingModal.hide();
            console.error('Error sending emails:', error);
            alert('Failed to send emails.');
        });
});
