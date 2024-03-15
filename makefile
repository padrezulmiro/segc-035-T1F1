DEVICE_SRC := IoTDevice \
MessageCode

SERVER_SRC := Device \
Domain \
IoTServer \
ServerThread \
User

BIN_DIR := bin
DEVICE_DIR := iotclient
SERVER_DIR := iotserver

DEVICE_FULL_PATHS := $(addsuffix .java,$(addprefix $(DEVICE_DIR)/,$(DEVICE_SRC))) 
SERVER_FULL_PATHS := $(addsuffix .java,$(addprefix $(SERVER_DIR)/,$(SERVER_SRC))) 

all:
	javac -d bin $(DEVICE_FULL_PATHS) $(SERVER_FULL_PATHS)

clean:
	rm -r bin; mkdir bin