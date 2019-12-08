To run the databases </br>
`systemctl start mongod`

Checking databases Status </br>
`sudo service mongod status`

To run a server </br>
`mvn spring-boot:run`

To run a Client </br>
`mvn exec:java`


Running files
1. run mongodb 
`systemctl start mongod`

2. compile and run authenticationserver with
`cd authenticationServer`
`mvn compile spring-boot:run`

3. compile and run timer bot
`cd botTimer`
`mvn compile spring-boot:run`

4. repeat step 2 for alphabetizer and quoter bot

5. compile and run channel
`cd channel`
`mvn compile spring-boot:run`

if you want additional channels, change the `applications.properties` to a new port and run the channel,
 while keeping the original server running.

6. compile and run client
`cd client`
`mvn compile exec:java`