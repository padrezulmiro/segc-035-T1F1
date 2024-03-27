# segc-035-T1F1
Fase 1 do 1º trabalho de Segurança e Confiabilidade

## Authors
- Bruna Santos - fc5628
- Yichen Cao - fc58165
- Filipe Costa - fc55549

## Compiling and building
Please use the Makefile:
```
$ make
```

## Executing
### Server
```
$ java -jar IoTServer.jar [port]
```
Args:
- `port` - TCP port used by the server. Default value is `12345`.

### Client
```
$ java -jar IoTDevice.jar <serverAddress> <dev-id> <user-id>
```
Args:
- `<serverAddress>` - identifies the server with the format `<IP/hostname>[:Port]`. Default port value is `12345`.
- `<dev-id>` - `int` Device identifier
- `<user-id>` - `String` Local user identifier 

## Limitations