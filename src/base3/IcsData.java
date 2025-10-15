/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package base3;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import org.json.JSONObject;

/**
 *
 * @author Kevin
 */
  
class IcsData {

    byte load_f;
    SipData sipData0;
    SipData sipData1;
    SlotData[] slotDatas = new SlotData[14];
    String actionStr = "";
    int actionStep = 0;
    int actionStatus = 0;
    int actionInx = 0;
    String actionInf = "";
    int selfSlot = 0;
    int debugCnt=0;
    int mastctr=0;
    public HashMap<String, Object> exStatusMap = new HashMap();
    String logStr="";
    

    IcsData() {
        load_f = 1;
        for (int i = 0; i < slotDatas.length; i++) {
            slotDatas[i] = new SlotData();
        }
        sipData0 = new SipData();
        sipData1 = new SipData();
        //debug();
    }

    void debug() {
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String tstr = ft.format(dNow);
        //=================================
        slotDatas[0].exist_f=1;
        slotDatas[0].type = "";
        slotDatas[0].count = 0;
        slotDatas[0].ip = "127.0.0.1";
        slotDatas[0].port=23499;
        //status = 0;//0:none(dark), 1:exist(y blink) ,2: ready(y), 3:paraSet loaded(green blink), 4:pbx run(g), 5:error(red)
        slotDatas[0].status = 1;
        slotDatas[0].inf = "系統啟動完成";

        slotDatas[1].exist_f=1;
        slotDatas[1].type = "";
        slotDatas[1].count = 0;
        slotDatas[1].ip = "127.0.0.1";
        slotDatas[1].port=23400;
        slotDatas[1].status = 1;
        slotDatas[1].inf = "";

        slotDatas[2].exist_f=1;
        slotDatas[2].type = "";
        slotDatas[2].count = 0;
        slotDatas[2].ip = "127.0.0.1";
        slotDatas[2].port=23401;
        slotDatas[2].status = 1;
        slotDatas[2].inf = "";
        
        
        /*
        slotDatas[1].type = "ctr";
        slotDatas[1].count = 1;

        slotDatas[2].type = "sip";
        slotDatas[2].ip = "127.0.0.1";

        slotDatas[3].type = "fxo";

        slotDatas[4].type = "fxs";

        slotDatas[5].type = "fxs";
        slotDatas[5].count = 1;

        slotDatas[6].type = "t1s";

        slotDatas[7].type = "roip";

        slotDatas[8].type = "roip";
        slotDatas[8].count = 1;

        slotDatas[9].type = "mag";

        slotDatas[10].type = "record";
        //status = 0;//0:none(dark), 1:exist(y blink) ,2: ready(y), 3:paraSet loaded(green blink), 4:pbx run(g), 5:error(red)
        slotDatas[0].status = 1;
        slotDatas[1].status = 2;
        slotDatas[2].status = 3;
        slotDatas[3].status = 4;
        slotDatas[4].status = 5;
        slotDatas[0].inf = "系統啟動中";
        slotDatas[1].inf = "系統啟動完成";
        slotDatas[2].inf = "載入使用者設定";
        slotDatas[3].inf = "裝置功能備便";
        slotDatas[4].inf = "板卡異常";
         */

    }
}
