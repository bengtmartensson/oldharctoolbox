package harc;

public class commandmapping {
    // Command to be mapped
    private command_t cmd;
    private String house;
    private short deviceno;
    private command_t new_cmd;
    private String remotename;

    public commandmapping(command_t cmd, String house, short deviceno, command_t new_cmd, String remotename) {
        this.cmd = cmd;
        this.house = house;
        this.deviceno = deviceno;
        this.new_cmd = new_cmd;
        this.remotename = remotename;
    }

    public command_t get_cmd() {
        return cmd;
    }

    public String get_house() {
        return house;
    }

    public short get_deviceno() {
        return deviceno;
    }

    public command_t get_new_cmd() {
        return new_cmd;
    }

    public String get_remotename() {
        return remotename;
    }

}
