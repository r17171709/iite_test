package com.renyu.iitebletest.model;

/**
 * Created by renyu on 16/3/3.
 */
public class BLEConnectModel {

    public enum BLESTATE {
        STATE_CONNECTED,
        STATE_DISCONNECTED,
        STATE_CONNECTING,
        STATE_SCAN,
        STATE_NOSCAN,
        STATE_CANCELSCAN,
        STATE_MOREDEVICE
    }

    BLESTATE blestate;

    public BLESTATE getBlestate() {
        return blestate;
    }

    public void setBlestate(BLESTATE blestate) {
        this.blestate = blestate;
    }
}
