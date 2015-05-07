/* Pin setup */
int enablePin = 6; //changed from 11
const int in1Pin = 5; //changed from 10
const int in2Pin = 4; //changed from 9
const int switchPin = 7;
const int potPin = 0;
const int vibPin = 3;
int val;

void setup()
{
  pinMode(in1Pin, OUTPUT);
  pinMode(in2Pin, OUTPUT);
  pinMode(enablePin, OUTPUT);
  pinMode(switchPin, INPUT_PULLUP);
  pinMode(vibPin, OUTPUT);
  Serial.begin(9600);
}
 
void loop()
{
  int speed = analogRead(potPin);
  Serial.println(speed);
  
    digitalWrite(vibPin, HIGH);
    delay(200);
    digitalWrite(vibPin, LOW);
    delay(100);
    digitalWrite(vibPin, HIGH);
    delay(200);
    digitalWrite(vibPin, LOW);
   
  
    if (speed> 500) {
    val = map(speed, 500, 845, 0, 225);
    //setMotor(speed, reverse);
    analogWrite(enablePin, val);
    digitalWrite(in1Pin, 1);
    digitalWrite(in2Pin, 0);
    Serial.println(val);
    delay(5000);
    analogWrite(enablePin, LOW);
    }
    
    if (speed<= 500){
    val = map(speed, 155, 500, 255, 0);
    //setMotor(speed, reverse);
    analogWrite(enablePin, val);
    digitalWrite(in1Pin, 0);
    digitalWrite(in2Pin, 1);
    Serial.println(val);
    delay(5000);
    analogWrite(enablePin, LOW);
    }
  delay(5000);
  
}
 
