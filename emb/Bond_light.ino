#include <Arduino.h>
#include <WiFi.h>
#include <AsyncTCP.h>
#include <ESPAsyncWebServer.h>
#include "LittleFS.h"
#include <ArduinoJson.h>
#include "esp_system.h"

#define BATT_VOLTAGE 34

uint8_t chPins[6] = {12, 13, 14, 15, 16, 17};
uint8_t currentDutyCycle[6] = {0, 0, 0, 0, 0, 0};
uint8_t targetDutyCycle[6] = {0, 0, 0, 0, 0, 0};
uint8_t retryCount = 0;
uint32_t lastUpdateTime[6] = {0, 0, 0, 0, 0, 0};
String sliderValues[6] = {"0", "0", "0", "0", "0", "0"};
int stepSize[6]; 
uint16_t transitionTime = 300;
unsigned long timer = 0;

bool localMode = false, reconnection = false;

struct Config {
  String SSID;
  String PASS;
  uint8_t chAvailable;
  uint16_t PWMfreq;
  float vRef;
  uint16_t R1;
  uint16_t R2;
} _config;

AsyncWebServer server(80);

AsyncWebSocket ws("/ws");

String message = "";
StaticJsonDocument<200> sliderValuesJSON;
StaticJsonDocument<100> battery;


String getSliderValue(uint8_t channel){
  String jsonString = "{\n\"sliderValue";
  jsonString += String(channel);
  jsonString += "\":\"";
  jsonString += sliderValues[channel];
  jsonString += "\"\n}";
  return jsonString;
}

String getAllSlidersValue(){
  String sliderName = "sliderValue";
  for (uint8_t i = 0; i < _config.chAvailable; i++){
    sliderValuesJSON[sliderName + String(i)] = sliderValues[i];
  }

  String jsonString;
  serializeJson(sliderValuesJSON, jsonString);
  return jsonString;
}

void initFS() {
  if (!LittleFS.begin()) {
    Serial.println("An error has occurred while mounting LittleFS");
  }
  else{
   Serial.println("LittleFS mounted successfully");
  }
}

void initWiFi(String ssid, String password) {
  WiFi.mode(WIFI_AP_STA);
  WiFi.begin(ssid, password);
  Serial.print("Connecting to WiFi ..");
  uint8_t netCount = 0;
  while (WiFi.status() != WL_CONNECTED) {
    netCount++;
    delay(500);
    Serial.print(".");
    if (netCount > 20){
      localMode = true;
      WiFi.disconnect();
      break;
    }
  }
  if (!localMode){
    Serial.println("Connected to WiFi: ");
    Serial.println(WiFi.localIP());
  }
  String apSSID = "ESP_HOME";
  WiFi.softAP(apSSID.c_str());
  Serial.println("Access Point Created: ");
  Serial.println(WiFi.softAPIP());
}

void setBrightness(uint8_t channel, uint8_t newDutyCycle) {
  targetDutyCycle[channel] = newDutyCycle;
  int delta = abs(targetDutyCycle[channel] - currentDutyCycle[channel]);
  if (delta > 0) {
    stepSize[channel] = delta;
    transitionTime = 300;
    if (stepSize[channel] > 20 && stepSize[channel] < 40){
      transitionTime = 600;
    } else if (stepSize[channel] > 40 && stepSize[channel] < 80) {
      transitionTime = 900;
    }
    lastUpdateTime[channel] = millis();
  }
}

void smoothTransition() {
  for (uint8_t i = 0; i < _config.chAvailable; i++) {
    uint32_t currentTime = millis();
    if (currentDutyCycle[i] != targetDutyCycle[i]) {
      uint32_t stepInterval = transitionTime / stepSize[i];
      if (currentTime - lastUpdateTime[i] >= stepInterval) {
        lastUpdateTime[i] = currentTime;
        if (currentDutyCycle[i] < targetDutyCycle[i]) {
          currentDutyCycle[i]++;
        } else if (currentDutyCycle[i] > targetDutyCycle[i]) {
          currentDutyCycle[i]--;
        }
        ledcWrite(chPins[i], currentDutyCycle[i]);
      }
    }
  }
}

void notifyClients(String _message) {
  ws.textAll(_message);
}

void handleWebSocketMessage(void *arg, uint8_t *data, size_t len) {
  AwsFrameInfo *info = (AwsFrameInfo*)arg;
  if (info->final && info->index == 0 && info->len == len && info->opcode == WS_TEXT) {
    data[len] = 0;
    message = (char*)data;
    uint8_t channel = message[0] - '0';
    String sliderValue = message.substring(2);
    sliderValues[channel] = sliderValue;
    setBrightness(channel, map(sliderValue.toInt(), 0, 100, 0, 180)); //map(sliderValue.toInt(), 0, 100, 0, 200)
    notifyClients(getSliderValue(channel));
  }
}
void onEvent(AsyncWebSocket *server, AsyncWebSocketClient *client, AwsEventType type, void *arg, uint8_t *data, size_t len) {
  switch (type) {
    case WS_EVT_CONNECT:
      Serial.printf("WebSocket client #%u connected from %s\n", client->id(), client->remoteIP().toString().c_str());
      break;
    case WS_EVT_DISCONNECT:
      Serial.printf("WebSocket client #%u disconnected\n", client->id());
      break;
    case WS_EVT_DATA:
      handleWebSocketMessage(arg, data, len);
      break;
    case WS_EVT_PONG:
    case WS_EVT_ERROR:
      break;
  }
}

void initWebSocket() {
  ws.onEvent(onEvent);
  server.addHandler(&ws);
}

void setup() {
  Serial.begin(115200);
  initFS();
  readConfig(&_config);

  for (uint8_t i = 0; i < _config.chAvailable; i++){
    pinMode(chPins[i], OUTPUT);
  }
  for (uint8_t i = 0; i < _config.chAvailable; i++){
    ledcAttachChannel(chPins[i], _config.PWMfreq, 8, i);
  }
  initWiFi(_config.SSID, _config.PASS);
  initWebSocket();
  analogRead(BATT_VOLTAGE);
 
  server.on("/", HTTP_GET, [](AsyncWebServerRequest *request){
    request->send(LittleFS, "/index.html", "text/html");
  });
  server.on("/question", HTTP_GET, [](AsyncWebServerRequest *request){
    request->send(200, "text/html", "HELLO");
  });
  server.on("/restart", HTTP_GET, [](AsyncWebServerRequest *request){
    request->send(200, "text/html", "restart");
    notifyClients("restart");
    delay(500);
    esp_restart();
  });
  server.on("/getValues", HTTP_GET, [](AsyncWebServerRequest *request){
    request->send(200, "application/json", getAllSlidersValue());
  });
  server.on("/changeSettings", HTTP_POST, [](AsyncWebServerRequest *request) {}, NULL,
    [](AsyncWebServerRequest *request, uint8_t *_data, size_t len, size_t index, size_t total) {
      String receivedData = "";
      File configFile;
      for (size_t i = 0; i < len; i++) {
          receivedData += (char)_data[i];
      }
      configFile = LittleFS.open("/config.json", "w");
      if (!configFile) {
        request->send(500, "text/plain", "Файл не вдалося відкрити");
        return;
      }
      configFile.print(receivedData);
      configFile.close();
      request->send(200, "text/plain", "Налаштування успішно змінено");
      notifyClients("restart");
      delay(500);
      esp_restart();
  });
  
  server.on("/batt", HTTP_GET, [](AsyncWebServerRequest *request){
    double adcValue = ReadVoltage(BATT_VOLTAGE);
    request->send(200, "text/html", String(adcValue, 5));
  });
  server.serveStatic("/", LittleFS, "/");
  server.begin();
}

void loop() {
  smoothTransition();
  ws.cleanupClients();

  if (millis() - timer > 1000 && !reconnection){
    if (WiFi.status() != WL_CONNECTED){
      //Serial.println("No connection");
      retryCount++;
    } else {
      //Serial.println("Connection is well");
      retryCount = 0;
    }
    timer = millis();
  }

  if (retryCount > 12 && !reconnection){
    WiFi.disconnect();
    reconnection = true;
    retryCount = 0;
  }

  if (reconnection){
    //Serial.println("Reconnection");
    WiFi.begin(_config.SSID.c_str(), _config.PASS.c_str());
    reconnection = false;
  }
}

void readConfig(Config *_config){
  File configFile;
   if (!LittleFS.exists("/config.json")) {
    configFile = LittleFS.open("/config.json", "w");
    if (!configFile) {
      Serial.println("Failed to create config file");
      return;
    }
    StaticJsonDocument<512> doc;
    doc["SSID"] = "netis";
    doc["PASS"] = "85403950";
    doc["chAvailable"] = 4;
    JsonArray chNames = doc.createNestedArray("chNames");
    chNames.add("CH_1");
    chNames.add("CH_2");
    chNames.add("CH_3");
    chNames.add("CH_4");
    doc["PWMfreq"] = 1500;
    doc["vRef"] = 3.35;
    doc["R1"] = 51000;
    doc["R2"] = 10000;
    
    if (serializeJson(doc, configFile) == 0) {
      Serial.println("Failed to write to file");
    }
    configFile.close();
  }

  configFile = LittleFS.open("/config.json", "r");
  size_t size = configFile.size();
  char* buf = new char[size];
  configFile.readBytes(buf, size);
  StaticJsonDocument<512> doc;
  auto error = deserializeJson(doc, buf);
  if (error) {
    Serial.println("Failed to parse config file");
    return;
  }
  _config->SSID = doc["SSID"].as<String>();
  _config->PASS = doc["PASS"].as<String>();
  _config->chAvailable = doc["chAvailable"].as<uint8_t>();
  _config->PWMfreq = doc["PWMfreq"].as<uint16_t>();
  _config->vRef = doc["vRef"].as<float>();
  _config->R1 = doc["R1"].as<uint16_t>();
  _config->R2 = doc["R2"].as<uint16_t>();
  delete[] buf;
  configFile.close();
}

double ReadVoltage(byte pin){
  double reading = analogRead(pin);
  if(reading < 1 || reading > 4095) return 0;
  return -0.000000000009824 * pow(reading,3) + 0.000000016557283 * pow(reading,2) + 0.000854596860691 * reading + 0.065440348345433;
  //return -0.000000000000016 * pow(reading,4) + 0.000000000118171 * pow(reading,3)- 0.000000301211691 * pow(reading,2)+ 0.001109019271794 * reading + 0.034143524634089;
}
