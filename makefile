DEVICE_SRC := IoTDevice \
MessageCode

SERVER_SRC := Device \
Domain \
IoTServer \
ServerThread \
ServerManager \
ServerResponse \
DomainStorage \
DeviceStorage \
UserStorage \
ServerAuth \
ServerConfig

BIN_DIR := bin
DEVICE_DIR := iotclient
SERVER_DIR := iotserver
HELPER_DIR := iohelper

DEVICE_FULL_PATHS := $(addsuffix .java,$(addprefix $(DEVICE_DIR)/,$(DEVICE_SRC))) 
SERVER_FULL_PATHS := $(addsuffix .java,$(addprefix $(SERVER_DIR)/,$(SERVER_SRC))) 

all:
	javac -d bin $(DEVICE_FULL_PATHS) $(SERVER_FULL_PATHS)
	jar cvfe IoTDevice.jar iotclient.IoTDevice -C ./bin $(DEVICE_DIR) \
-C ./bin $(HELPER_DIR) -C ./bin iotserver/ServerResponse.class
	jar cvfe IoTServer.jar iotserver.IoTServer -C ./bin $(SERVER_DIR) \
-C ./bin iotclient/MessageCode.class -C ./bin $(HELPER_DIR)
clean:
	rm -r bin; mkdir bin
