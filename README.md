# segc-035-T1F2
Fase 2 do 1º trabalho de Segurança e Confiabilidade

## Authors
- Bruna Santos - fc56328
- Yichen Cao - fc58165
- Filipe Costa - fc55549

## Compiling and building
Please use the Makefile:
```
$ make
```

### Attestation
For attestation to work correctly, `attestation.txt` should have `IoTDevice.jar`'s file path.

## Executing
### Server
```
$ java -jar IoTServer.jar [port] <password-cipher> <keystore> <password-keystore> <2FA-APIKey>
```
Args:
- `port` - TCP port used by the server. Default value is `12345`.
- `<password-cifra>` - password used to generate the symmetric key for file encryption
- `<keystore>` - Keystore with server's key pair.
- `<password-keystore>` - Keystore's password.
- `<2FA-APIKey>` - API key for Two Factor Authentication.

#### Storing of images on the server-side
Images that are sent to the server are stored in `./img/`.

### Client
```
$ java -jar IoTDevice.jar <serverAddress> <truststore> <keystore> <password-keystore> <dev-id> <user-id>
```
Args:
- `<serverAddress>` - identifies the server with the format `<IP/hostname>[:Port]`. Default port value is `12345`.
- `<truststore>` - Truststore containing server's and users' public key certificates.
- `<keystore>` - Keystore with `<user-id>`'s key pair.
- `<password-keystore>` - Keystore's password.
- `<dev-id>` - `int` Device identifier.
- `<user-id>` - `String` Local user identifier.

#### Sending/Receiving of Images and Temperatures
If you want to send an image `./image_name.jpg` to the server, you can execute the `EI ./image_name.jpg` command in the prompt.

Temperatures and images that are requested from the server via `RT` and `RI`,
respectively, are stored under `./temps_domainName.txt` `./Img_username_deviceID.jpg`.
