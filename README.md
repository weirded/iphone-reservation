# iPhone Reservation

Small toy AWS Lambda function. It checks Apple Store inventory for availability of given models and sends an SMS (using Twilio) if available. 

### Inputs

Here is a template for the inputs for the function: 

```json
{
  "searchParameters": {
    "desiredColors": ["Space Gray", "Silver"],
    "desiredCarriers": ["AT&T"],
    "desiredStates": ["California"],
    "desiredModelNames": ["iPhone 6s Plus"],
    "desiredCities": ["Palo Alto", "Santa Clara", "Berkeley", "Emeryville", "Los Gatos", "Santa Clara", "San Jose", "San Mateo", "San Francisco"],
    "desiredCapacities": [64, 128]
  },
  "toPhone": "+14085555555",
  "twilioConfiguration": {
    "accountSid": "[fill]",
    "authToken": "[fill]",
    "fromPhone": "+14085555555"
  }
}
```

### Main

You can run the included `Main` method to test the configuration. Simply save the JSON in a file and pass the file as an argument. 