# iPhone Reservation Checker

I wrote this to play with AWS Lambda. It's a function that checks Apple Store inventory for availability of given models and sends an SMS (using Twilio) if available. 

### Running it in Lambda

Create the .jar with `mvn clean package`, then create a Lambda function by uploading the .jar (using the AWS Console or AWS CLI). In the inputs, provide your arguments (as per the template below).

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