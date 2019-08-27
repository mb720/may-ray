# May Ray
An experimental, lightweight server.

# Build and run
Builds and starts the server listening on port 8443 using TLS:

    ./gradlew run

To deactivate TLS (but still run on port 8443):

    ./gradlew run --args="--use-tls=no"

# Server configuration
The configuration file used per default is at `config/server.properties`.

It contains the interface and port that the server listens to for incoming client connections.

The configuration file also contains the path and password of the [Java keystore](https://en.wikipedia.org/wiki/Java_KeyStore) that contains the certificate used for TLS connections.

Run May Ray like this to specify a different configuration file:

    ./gradlew run --args="--config=./path/to/config.properties"

# Self-signed certificate included

May Ray ships with a self-signed certificate at `tls/keystore.jks`.

You'll have to add an exception for the certificate in your browser in order to connect to May Ray via TLS.

When using [cURL](https://curl.haxx.se/), add the `-k` flag to allow self-signed certificates:

    curl -k "https://localhost:8443/"

