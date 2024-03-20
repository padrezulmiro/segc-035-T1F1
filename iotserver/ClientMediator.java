package iotserver;

import java.io.InputStream;
import java.util.ArrayList;

import iotclient.MessageCode;

public class ClientMediator {
    private String clientUID;
    private String clientDID;
    private String clientFullID;
    private Device device;

    public ClientMediator(String fullID) {
        this(fullID.split(":")[0], fullID.split(":")[1]);
    }
    
    public ClientMediator(String userID, String devID) {
        this.clientUID = userID;
        this.clientDID = devID;
        this.clientFullID = userID + ":" + devID;
        this.device = IoTServer.DEVICES.get(clientFullID);
    }

    public ServerResponse createDomain(String domainName) {
        if (domainExists(domainName)) {
            return new ServerResponse(MessageCode.NOK);
        }

        Domain domain = new Domain(domainName, clientUID);
        IoTServer.DOMAINS.put(domainName, domain);
        return new ServerResponse(MessageCode.OK);
    }

    public ServerResponse addUserToDomain(String newUserId, String domainName) {
        if (!domainExists(domainName)) {
            return new ServerResponse(MessageCode.NODM);
        }

        Domain domain = IoTServer.DOMAINS.get(domainName);

        if (!domain.isOwner(clientUID)) {
            return new ServerResponse(MessageCode.NOPERM);
        }

        if (domain.registerUser(newUserId)) {
            return new ServerResponse(MessageCode.OK);
        } else {
            // XXX is this the correct implementation?
            return new ServerResponse(MessageCode.USEREXISTS);
        }
    }

    public ServerResponse registerDevice(String domainName) {
        if (!domainExists(domainName)) {
            return new ServerResponse(MessageCode.NODM);
        }

        Domain domain = IoTServer.DOMAINS.get(domainName);
        if (!domain.isRegistered(clientUID)) {
            return new ServerResponse(MessageCode.NOPERM);
        }

        device.registerInDomain(domainName);
        return new ServerResponse(MessageCode.OK);
    }

    public ServerResponse registerTemperature(String temperatureString) {
        float temperature;
        try {
            temperature = Float.parseFloat(temperatureString);
        } catch (NumberFormatException e) {
            return new ServerResponse(MessageCode.NOK);
        }

        device.registerTemperature(temperature);
        return new ServerResponse(MessageCode.OK);
    }

    public ServerResponse registerImage(InputStream imageStream) {
        throw new UnsupportedOperationException();
    }

    public ServerResponse getTemperatures(String domainName) {
        throw new UnsupportedOperationException();
    }

    public ServerResponse getImage(String targetUserId, String targetDevId) {
        throw new UnsupportedOperationException();
    }

    private boolean domainExists(String domainName) {
        return IoTServer.DOMAINS.containsKey(domainName);
    }
}
