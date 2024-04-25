# Raspberry Pi FileStorage system

### Setup

- Set up a wireless access point on Raspberry Pi according to the [setup guide](/pi_setup/setup.md)
- Connect to the Pi and deploy the FileStorage server to the Pi using `./gradlew deploy`
- Run the client on your local machine (`src/main/java/com/nedap/university/Client`)

### Using the client

The client has six commands available, use them by entering the corresponding number (1-6)
- 1 ...`<SEND>`..... Send a file to the Pi
- 2 ...`<REPLACE>`.. Send and replace a file on the Pi
- 3 ...`<RETRIEVE>`. Retrieve a file from the Pi
- 4 ...`<DELETE>`... Delete a file from the Pi
- 5 ...`<LIST>`..... Receive a list of all files currently stored on the Pi
- 6 ...`<EXIT>`..... Exit the client

### Using the server

After receiving the first send request, the Pi will create a new directory under `/home/pi` called
`FileStorage`. All subsequent files sent will be stored in this directory. A subdirectory is created
here called ServerLog, a `.txt` file containing all previous interactions with clients.
