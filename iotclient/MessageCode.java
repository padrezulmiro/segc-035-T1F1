package iotclient;

/**
 * Represents messages to be sent between server and client.
 */
public enum MessageCode {
    WRONG_PWD("WRONG_PWD # Wrong password. Try again."),
    OK_NEW_USER("OK_NEW_USER # This user isn't registered."),
    OK_USER("OK_USER # User exists and pswd is correct."),
    NOK_DEVID("NOK_DEVID # This device ID is already connected with this user. Try another <dev-id>."),
    OK_DEVID("OK_DEVID # User and device."),
    NOK_TESTED("NOK_TESTED # Server did not validate."),
    OK_TESTED("OK_TESTED # Server validated."),
    OK("OK"),
    NOK("NOK"),
    NODM("NODM # Domain does not exist."),
    NOPERM("NOPERM # User doesn't have read permission."),
    NODATA("NODATA # Device id has no published data."),
    NOID("NOID # Device id does not exist."),
    USEREXISTS("USEREXISTS # User already exists.");

    private String desc;

    MessageCode(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

}