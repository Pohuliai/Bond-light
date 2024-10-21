
var gateway = `ws://${window.location.hostname}/ws`;
var websocket;
let config, values;

window.addEventListener('load', onload);

function onload(event) {
    const promises = [
        getConfig(), 
        getValues(), 
        getADCVoltage(),
        initWebSocket()
    ];

    Promise.all(promises)
        .then((results) => {
            console.log("Всі операції виконані успішно");
            config = results[0];
            values = results[1];
            const ADCVoltage = results[2];
            generateCards(config);
            updateConfig(config);
            updateSlidersValue(values);
            updateBatteryInfo(ADCVoltage);
            hideLoader();
            showAll();
            setInterval(async () => {
                await updateBatteryInfoLoop();
            }, 5000);
        })
        .catch((error) => {
            console.error("Помилка при виконанні операцій:", error);
            hideLoader(); 
            const errorText = document.getElementById('errorText');
            errorText.classList.toggle('hidden');
            reloadPage();
        });
}

function initWebSocket() {
    return new Promise((resolve, reject) => {
        console.log('Trying to open a WebSocket connection…');
        websocket = new WebSocket(gateway);
        websocket.onopen = function(event) {
            resolve();
        };
        websocket.onclose = function(event) {
            console.log('Connection closed');
            reject(new Error('WebSocket connection closed'));
            setTimeout(initWebSocket, 2000);
        };
        websocket.onerror = function(event) {
            reject(new Error('WebSocket error'));
        };
        websocket.onmessage = onMessage;
    });
}

function updateSliderPWM(element) {
    var sliderNumber = element.id.charAt(element.id.length-1);
    var sliderValue = document.getElementById(element.id).value;
    document.getElementById("sliderValue"+sliderNumber).innerHTML = sliderValue;
    console.log(sliderValue);
    websocket.send(sliderNumber+"s"+sliderValue.toString());
}

function onMessage(event) {
    if (event.data === 'restart') {
        reloadPage();
    } else {
        updateSlidersValue(JSON.parse(event.data));
    }
}

function getConfig(){
    return fetch('config.json')
        .then(response => response.json())
        .then(data => {
            return data;
        })
        .catch(error => {
            console.error("Error loading JSON:", error);
            throw error;
        });
}
function getADCVoltage(){
    return fetch('/batt')
        .then(response => response.text())
        .then(data => {
            const voltage = parseFloat(data);
            return voltage;
        })
        .catch(error => {
            console.error("Error:", error);
            throw error;
        });
}

function getValues(){
    return fetch('/getValues')
        .then(response => response.json())
        .then(data => {
            return data;
        })
        .catch(error => {
            console.error("Error:", error);
            throw error;
        });
}

function restartDevice(){
    fetch('/restart')
        .then(response => response.text())
        .then(data => {
            reloadPage();
            return data;
        })
        .catch(error => {
            console.error("Error:", error);
        });
}

function generateCards(config) {
    const container = document.getElementById("cardsContainer");
    for (let i = 0; i < config.chAvailable; i++) {
        const card = document.createElement("div");
        card.classList.add("card");

        const cardTitle = document.createElement("p");
        cardTitle.classList.add("card-title");
        cardTitle.textContent = config.chNames[i]; 
        card.appendChild(cardTitle);

        const sliderWrapper = document.createElement("p");
        sliderWrapper.classList.add("switch");
        const slider = document.createElement("input");
        slider.type = "range";
        slider.min = "0";
        slider.max = "100";
        slider.step = "1";
        slider.value = "0";
        slider.id = `slider${i}`;
        slider.onchange = function() {
            updateSliderPWM(this);
        };
        slider.classList.add("slider");
        sliderWrapper.appendChild(slider);
        card.appendChild(sliderWrapper);

        const stateWrapper = document.createElement("p");
        stateWrapper.classList.add("state");
        stateWrapper.innerHTML = `Яскравість: <span id="sliderValue${i}"></span> &percnt;`;
        card.appendChild(stateWrapper);

        container.appendChild(card);
    }
}

function updateConfig(config){
    document.getElementById('ssid').value = config.SSID;
    document.getElementById('password').value = config.PASS;
    document.getElementById('chAvailable').value = config.chAvailable;
    document.getElementById('PWMfreq').value = config.PWMfreq;
    document.getElementById('vRef').value = config.vRef;
    document.getElementById('R1').value = config.R1;
    document.getElementById('R2').value = config.R2;
    generateChNamesFields();
}



// function calculateBatteryPercent(voltage) {
//     const maxVoltage = 14.1;
//     const minVoltage = 11.2;
//     let percent = ((voltage - minVoltage) / (maxVoltage - minVoltage)) * 100;

//     if (percent > 100) percent = 100;
//     if (percent < 0) percent = 0;

//     return Math.round(percent);
// }

function updateBatteryInfo(ADCVoltage){
    const voltage = ADCVoltage * ((config.R1 + config.R2) / config.R2)
    document.getElementById("batteryVoltage").innerText = voltage.toFixed(2);
    // const batteryPercent = calculateBatteryPercent(voltage);
    // document.getElementById("batteryPercent").innerText = batteryPercent;
}

function toggleSettings() {
    const settingsContainer = document.getElementById('settings-container');
    settingsContainer.classList.toggle('hidden');
}

function generateChNamesFields() {
    const chAvailable = document.getElementById('chAvailable').value;
    const chNamesContainer = document.getElementById('chNamesContainer');
    chNamesContainer.innerHTML = '';

    for (let i = 0; i < chAvailable; i++) {
        const label = document.createElement('label');
        label.textContent = `chNames[${i}]`;

        const input = document.createElement('input');
        input.type = 'text';
        input.classList.add('input-field-child');
        input.id = `chName${i}`;
        if (config.chNames[i] == undefined){
            input.value = `Нове ім\'я ${i}`;
        } else {
            input.value = config.chNames[i];
        }

        chNamesContainer.appendChild(label);
        chNamesContainer.appendChild(input);
    }
}

function saveSettings(){
    config.SSID = document.getElementById('ssid').value;
    config.PASS = document.getElementById('password').value;
    config.chAvailable = parseInt(document.getElementById('chAvailable').value, 10);
    config.PWMfreq = parseInt(document.getElementById('PWMfreq').value, 10);
    config.vRef = parseFloat(document.getElementById('vRef').value);
    config.R1 = parseInt(document.getElementById('R1').value, 10);
    config.R2 = parseInt(document.getElementById('R2').value, 10);
    for (let i = 0; i < config.chAvailable; i++){
        config.chNames[i] = document.getElementById(`chName${i}`).value;
    }
    console.log(config);

    fetch('/changeSettings', {
        method: 'POST',
        headers: {
            'Content-Type': "application/json", 
        },
        body: JSON.stringify(config),
    })
    .then(response => response.text()) 
    .then(data => {
        alert(`Сервер відповів: ${data}`); 
    })
    .catch(error => {
        alert('Помилка при відправці даних: ' + error); 
    });
}

function hideLoader() {
    document.querySelector('.loader-wrapper').classList.toggle('hidden');
}

function reloadPage() {
    setTimeout(() => {
        window.location.reload();
    }, 2000);
}

function showAll(){
    document.querySelector('.card-grid').classList.toggle('hidden');
    document.querySelector('.buttons-grid').classList.toggle('hidden');
    const batteryVoltageText = document.getElementById('batteryVoltageText');
    batteryVoltageText.classList.toggle('hidden');
    // const batteryPercentText = document.getElementById('batteryPercentText');
    // batteryPercentText.classList.toggle('hidden');
}

function updateSlidersValue(myObj){
    console.log(myObj);
    var keys = Object.keys(myObj);
        for (var i = 0; i < keys.length; i++) {
            var key = keys[i];
            var sliderNumber = key.charAt(key.length - 1);
            if (myObj[key] === null || myObj[key] === undefined) {
                document.getElementById(key).innerHTML = "0";
                document.getElementById("slider" + sliderNumber).value = "0";
                websocket.send(sliderNumber + "s0");
            } else {
                document.getElementById(key).innerHTML = myObj[key];
                document.getElementById("slider" + sliderNumber).value = myObj[key];
            }
        }
}

async function updateBatteryInfoLoop(){
    const ADCVoltage = await getADCVoltage();
    updateBatteryInfo(ADCVoltage);
}
