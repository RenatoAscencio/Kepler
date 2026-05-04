package org.alexdev.kepler.game.player.register;

public class RegisterValue {
    private String label;
    private RegisterDataType dataType;
    private String value;
    private boolean flag;
    private boolean present;

    public RegisterValue(String label, RegisterDataType dataType) {
        this.label = label;
        this.dataType = dataType;
    }

    public RegisterDataType getDataType() {
        return dataType;
    }

    public boolean getFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
        this.present = true;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
        this.present = true;
    }

    public String getLabel() {
        return label;
    }

    public boolean isPresent() {
        return present;
    }
}
