package kr.uracle.ums.fpc.tps;


import java.io.Serializable;

/**
 * Created by Y.B.H(mium2) on 17. 3. 17..
 */
public class TpsInfoBean implements Serializable {

    private static final long serialVersionUID = -6555183970228047453L;
    private String SERVER_KIND = "";
    private String QUEUE_SIZE = "0";
    private String MAX_INPUT_CNT = "0";
    private String MAX_OUTPUT_CNT = "0";
    private String INPUT_CNT ="0";
    private String OUT_CNT = "0";
    private String INTERVAL =  "60";
    private String CHECK_DATE = "00000000000000";

    public String getSERVER_KIND() {
        return SERVER_KIND;
    }

    public void setSERVER_KIND(String SERVER_KIND) {
        this.SERVER_KIND = SERVER_KIND;
    }

    public String getQUEUE_SIZE() {
        return QUEUE_SIZE;
    }

    public void setQUEUE_SIZE(String QUEUE_SIZE) {
        this.QUEUE_SIZE = QUEUE_SIZE;
    }

    public String getMAX_INPUT_CNT() {
        return MAX_INPUT_CNT;
    }

    public void setMAX_INPUT_CNT(String MAX_INPUT_CNT) {
        this.MAX_INPUT_CNT = MAX_INPUT_CNT;
    }

    public String getMAX_OUTPUT_CNT() {
        return MAX_OUTPUT_CNT;
    }

    public void setMAX_OUTPUT_CNT(String MAX_OUTPUT_CNT) {
        this.MAX_OUTPUT_CNT = MAX_OUTPUT_CNT;
    }

    public String getINPUT_CNT() {
        return INPUT_CNT;
    }

    public void setINPUT_CNT(String INPUT_CNT) {
        this.INPUT_CNT = INPUT_CNT;
    }

    public String getOUT_CNT() {
        return OUT_CNT;
    }

    public void setOUT_CNT(String OUT_CNT) {
        this.OUT_CNT = OUT_CNT;
    }

    public String getCHECK_DATE() {
        return CHECK_DATE;
    }

    public void setCHECK_DATE(String CHECK_DATE) {
        this.CHECK_DATE = CHECK_DATE;
    }

    public String getINTERVAL() {
        return INTERVAL;
    }

    public void setINTERVAL(String INTERVAL) {
        this.INTERVAL = INTERVAL;
    }
}
