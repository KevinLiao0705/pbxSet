/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package base3;

import com.fazecast.jSerialComm.SerialPort;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Administrator
 */
public class Ics {

    static Ics scla;
    Ics cla;
    public String errStr = "";
    public String okStr = "";
    public int errCnt = 0;

    Map<String, CmdTask> taskMap;

    int sipPhoneDeviceId = 0x1947;
    //====================
    KvComm fpgaComm;
    KvComm sip0Comm;
    KvComm sip1Comm;
    KvComm sip2Comm;
    int mainSoftPhone_exist_f = 1;
    int subSoftPhone_exist_f = 1;
    int monitorPhone_exist_f = 1;
    //String mainSoftPhone_ip = "192.168.0.33";
    //String subSoftPhone_ip = "192.168.0.39";
    int mainSoftPhone_port = 1236;
    int subSoftPhone_port = 1236;
    int monitorPhone_port = 1236;
    int sipSocketPort0 = 1336;
    int sipSocketPort1 = 1337;
    int sipSocketPort2 = 1338;
    //====================
    int myDeviceId = 0x2712;
    int mySerialId = 0x0000;
    int sipDeviceId = 0xd300;
    int sipSerialId = 0x0000;
    int rs485DeviceId = 0x2402;
    int devicePcioId = 0x2501;
    IcsData icsData;
    //====================
    String commandStr = "";
    Ssocket socketServer;
    int socketServerPort = 8324;
    //========================================================
    byte[] sockUartData_buf = new byte[4096];
    int sockUartData_len = 0;
    int sockUartData_tx_f = 0;
    //====================
    byte[] sockUartCmd_buf = new byte[4096];
    int sockUartCmd_len = 0;
    int sockUartCmd_tx_f = 0;

    String tickBackUserName = "";
    //========================================================

    Timer icsTimer = null;
    int emuTimer = 0;
    HashMap<String, Object> sip0Commands;
    HashMap<String, Object> sip1Commands;
    HashMap<String, Object> sip2Commands;
    TaskStack taskStack;
    ConsoleMain cm1;
    ConsoleSlot cs1, cs2, cs3, cs4, cs5, cs6;

    JSONObject tickBackValue;
    int debugCnt = 1;
    HashMap<String, Object> paraSetMap = new HashMap();

    public Ics() {
        cla = this;

        Ics.scla = this;
        sip0Commands = new HashMap<String, Object>();
        sip1Commands = new HashMap<String, Object>();
        sip2Commands = new HashMap<String, Object>();
        taskMap = new HashMap<String, CmdTask>();
        cla.getParaSetMap();
        icsData = new IcsData();
        taskStack = new TaskStack(20);
        taskStack.exeTask = new TaskStackExe() {
            @Override
            public String exe(CmdTask task) {
                String str;
                int slotCnt = 0;
                if (task.name.equals("slotTest")) {
                    slotCnt = Lib.str2int(task.paras[0], 1);
                    switch (task.stepInx) {
                        case 0:
                            task.stepInx++;
                            task.stepDly = 50;
                            icsData.actionStr = "slotTest";
                            str = "\n測試 步驟 0";
                            icsData.actionInf = str;
                            cla.icsData.actionStatus = 1;
                            cla.icsData.actionStep = task.stepInx;
                            cla.icsData.actionInx = Lib.str2int(task.paras[1], 0);
                            System.out.println("test step 0");
                            return null;
                        case 1:
                            task.stepInx++;
                            task.stepDly = 50;
                            str = "\n測試 步驟 1";
                            icsData.actionInf = str;
                            cla.icsData.actionStep = task.stepInx;
                            System.out.println("test step 1");
                            return null;
                        case 2:
                            task.stepInx++;
                            task.stepDly = 50;
                            str = "\n測試 步驟 2";
                            icsData.actionInf = str;
                            cla.icsData.actionStep = task.stepInx;
                            System.out.println("test step 2");
                            return null;
                        case 3:
                            task.stepInx++;
                            task.stepDly = 50;
                            str = "\n測試 步驟 3";
                            icsData.actionInf = str;
                            cla.icsData.actionStep = task.stepInx;
                            System.out.println("test step 3");
                            return null;
                        default:
                            if (taskStack.taskEnd(task) == 1) {
                                cla.icsData.actionStep = task.stepInx;
                                
                                if (icsData.slotDatas[slotCnt].status==4) {
                                    icsData.actionInf = "\n測試成功";
                                    icsData.actionStatus = 2;
                                } else {
                                    icsData.actionInf = "\n測試失敗";
                                    icsData.actionStatus = 3;
                                }
                                System.out.println("end test");
                                return "end task";
                            } else {
                                return null;
                            }
                    }
                }
                taskStack.taskMap.remove(task.name);
                return null;
            }
        };
        //taskStack.addTask("test");
        //=======================================================================
        sip0Comm = new KvComm("sipData0", "serverSocket");
        sip0Comm.serverSocket.format = 1;
        sip0Comm.serverSocket.rxcon_ltim = 100;
        sip0Comm.serverSocket.port = sipSocketPort0;
        sip0Comm.serverSocket.stm.setCallBack(new BytesCallback() {
            @Override
            public String prg(byte[] bytes, int len) {
                return sipRxPrg(bytes, len, 0);
            }
        });
        sip0Comm.open();
        //=======================================================================
        sip1Comm = new KvComm("sipData1", "serverSocket");
        sip1Comm.serverSocket.format = 1;
        sip1Comm.serverSocket.rxcon_ltim = 100;
        sip1Comm.serverSocket.port = sipSocketPort1;
        sip1Comm.serverSocket.stm.setCallBack(new BytesCallback() {
            @Override
            public String prg(byte[] bytes, int len) {
                return sipRxPrg(bytes, len, 1);
            }
        });
        sip1Comm.open();
        //=======================================================================
        sip2Comm = new KvComm("sipData2", "serverSocket");
        sip2Comm.serverSocket.format = 1;
        sip2Comm.serverSocket.rxcon_ltim = 100;
        sip2Comm.serverSocket.port = sipSocketPort2;
        sip2Comm.serverSocket.stm.setCallBack(new BytesCallback() {
            @Override
            public String prg(byte[] bytes, int len) {
                return sipRxPrg(bytes, len, 1);
            }
        });
        sip2Comm.open();
        //=======================================================================
        
        
        

        if (icsTimer == null) {
            icsTimer = new Timer(20, new IcsTm1(cla));  //about 30ms 
            icsTimer.start();
        }

        //KvWebSocketServer.startWebSocketServer(GB.webSocketServerPort);
        cm1 = new ConsoleMain(cla);
        cm1.create();

    }

    int getSlotCnt(String exNumber) {
        Object obj = cla.paraSetMap.get("phExNos");
        JSONArray exnos;
        try {
            exnos = new JSONArray(obj.toString());
        } catch (Exception ex) {
            return -1;
        }
        String[] exnoStrs = Lib.toStringArray(exnos);
        int yes_f = 0;
        String slotType = "";
        String[] strA;
        int slotCnt = -2;
        for (int i = 0; i < exnoStrs.length; i++) {
            strA = exnoStrs[i].split("~");
            if (!strA[3].trim().equals(exNumber)) {
                continue;
            }
            slotType = strA[0].trim();
            if (slotType.equals("soft")) {
                slotType = "sip";
            }
            slotCnt = Lib.str2int(strA[1].trim(), 0);
            for (int j = 0; j < cla.icsData.slotDatas.length; j++) {
                SlotData slotData = cla.icsData.slotDatas[j];
                if (slotData.type.equals(slotType) && slotCnt == slotData.count) {
                    return j;
                }
            }
        }

        obj = cla.paraSetMap.get("broadGroups");
        JSONArray broadObj;
        try {
            broadObj = new JSONArray(obj.toString());
        } catch (Exception ex) {
            return -1;
        }
        String[] broadStrs = Lib.toStringArray(broadObj);
        yes_f = 0;
        slotType = "";
        for (int i = 0; i < broadStrs.length; i++) {
            strA = broadStrs[i].split("~");
            if (!strA[0].trim().equals(exNumber)) {
                continue;
            }
            slotType = "sip";
            slotCnt = 0;
            for (int j = 0; j < cla.icsData.slotDatas.length; j++) {
                SlotData slotData = cla.icsData.slotDatas[j];
                if (slotData.type.equals(slotType) && slotCnt == slotData.count) {
                    return j;
                }
            }
        }

        obj = cla.paraSetMap.get("meetGroups");
        JSONArray meetObj;
        try {
            meetObj = new JSONArray(obj.toString());
        } catch (Exception ex) {
            return -1;
        }
        String[] meetStrs = Lib.toStringArray(meetObj);
        yes_f = 0;
        slotType = "";
        for (int i = 0; i < meetStrs.length; i++) {
            strA = meetStrs[i].split("~");
            if (!strA[0].trim().equals(exNumber)) {
                continue;
            }
            slotType = "sip";
            slotCnt = 0;
            for (int j = 0; j < cla.icsData.slotDatas.length; j++) {
                SlotData slotData = cla.icsData.slotDatas[j];
                if (slotData.type.equals(slotType) && slotCnt == slotData.count) {
                    return j;
                }
            }
        }

        return -1;

    }

    String sipRxPrg(byte[] bytes, int len, int rxId) {
        int inx = 0;
        int deviceId = (bytes[0] & 255) + (bytes[1] & 255) * 256;
        int serialId = (bytes[2] & 255) + (bytes[3] & 255) * 256;
        int groupId = (bytes[4] & 255) + (bytes[5] & 255) * 256;
        int packLen = (bytes[6] & 255) + (bytes[7] & 255) * 256;
        int commandId = (bytes[8] & 255) + (bytes[9] & 255) * 256;

        String icsUiSet = cla.paraSetMap.get("icsUiSet").toString();
        String[] strA = icsUiSet.split("~");
        for (int i = 0; i < strA.length; i++) {
            String[] strB = strA[i].split("\\.");
            if (strB.length != 4) {
                continue;
            }
            int lowIp = Lib.str2int(strB[3], 255);
            lowIp += Lib.str2int(strB[2], 255) * 256;
            if (lowIp != serialId) {
                continue;
            }
            serialId = i;
            break;
        }
        if (deviceId != sipPhoneDeviceId) {
            return null;
        }

        if (groupId != 0xab00) {
            return null;
        }
        if (commandId != 0x1000) {
            return null;
        }
        inx += 10;
        SipData sipData = null;
        if (serialId == 1) {
            sipData = icsData.sipData0;
        }
        if (serialId == 2) {
            sipData = icsData.sipData1;
        }
        if (serialId == 3) {
            sipData = icsData.sipData2;
        }
        if (sipData == null) {
            return null;
        }
        for (;;) {
            int readLen = cla.decSipInf(sipData, bytes, inx, len);
            if (readLen <= 0) {
                break;
            }
            inx += readLen;
            if (inx >= len) {
                break;
            }
            continue;
        }
        return null;
    }

    int decSipInf(SipData sipData, byte[] bytes, int stInx, int totalLen) {
        int inx = stInx;
        byte[] strBytes;
        sipData.connectTime = 0;

        int cmd = (bytes[inx++] & 255);
        int len = (bytes[inx++] & 255);
        if (len > 255) {
            return -1;
        }
        if ((stInx + len) >= totalLen) {
            return -1;
        }
        switch (cmd) {
            case 0x00:
                sipData.ioBuf = (bytes[inx++] & 255);
                sipData.ioBuf += (bytes[inx++] & 255) * 256;
                sipData.ioBuf += (bytes[inx++] & 255) * 256 * 256;
                sipData.ioBuf += (bytes[inx++] & 255) * 256 * 256 * 256;
                break;
            case 0x10:
                sipData.phoneSta = bytes[inx++];
                sipData.connectSta = bytes[inx++];
                sipData.handStatus = bytes[inx++];
                sipData.earSpeakerVol = bytes[inx++];
                sipData.phsetSpeakerVol = bytes[inx++];
                sipData.earMicSens = bytes[inx++];
                sipData.phsetMicSens = bytes[inx++];
                sipData.sipFlag = (bytes[inx++] & 255);
                sipData.sipFlag += (bytes[inx++] & 255) * 256;
                sipData.sipFlag += (bytes[inx++] & 255) * 256 * 256;
                break;
            case 0x11:
                if (len == 0) {
                    sipData.sipStatus = "";
                    break;
                }
                strBytes = new byte[len];
                for (int i = 0; i < len; i++) {
                    strBytes[i] = bytes[inx++];
                }
                sipData.sipStatus = new String(strBytes, StandardCharsets.UTF_8);
                break;
            case 0x12:
                if (len == 0) {
                    sipData.sipAction = "";
                    break;
                }
                strBytes = new byte[len];
                for (int i = 0; i < len; i++) {
                    strBytes[i] = bytes[inx++];
                }
                sipData.sipAction = new String(strBytes, StandardCharsets.UTF_8);
                break;
            case 0x13:
                if (len == 0) {
                    sipData.callto = "";
                    break;
                }
                strBytes = new byte[len];
                for (int i = 0; i < len; i++) {
                    strBytes[i] = bytes[inx++];
                }
                sipData.callto = new String(strBytes, StandardCharsets.UTF_8);
                break;
            case 0x14:
                if (len == 0) {
                    sipData.callfrom = "";
                    break;
                }
                strBytes = new byte[len];
                for (int i = 0; i < len; i++) {
                    strBytes[i] = bytes[inx++];
                }
                sipData.callfrom = new String(strBytes, StandardCharsets.UTF_8);
                break;
            case 0x16:
                if (len == 0) {
                    sipData.selfName = "";
                    break;
                }
                strBytes = new byte[len];
                for (int i = 0; i < len; i++) {
                    strBytes[i] = bytes[inx++];
                }
                sipData.selfName = new String(strBytes, StandardCharsets.UTF_8);
                break;
            case 0x17:
                if (len == 0) {
                    sipData.selfNumber = "";
                    break;
                }
                strBytes = new byte[len];
                for (int i = 0; i < len; i++) {
                    strBytes[i] = bytes[inx++];
                }
                sipData.selfNumber = new String(strBytes, StandardCharsets.UTF_8);
                break;
                
                

        }
        return (len + 2);

    }

    public void getParaSetMap() {
        cla.paraSetMap.clear();
        String fileName = GB.exePath + "/paraSet.json";
        System.out.println(fileName);
        try {
            String fileFullName = GB.exePath + "/paraSet.json";
            Path file = Paths.get(fileFullName);
            BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
            String nowParaSetTime = attr.lastModifiedTime().toString();
            if (!GB.preParaSetTime.equals(nowParaSetTime)) {
                if(GB.preParaSetTime.length()!=0){
                    if(cla.cm1.nsta.type.equals("ctr")){
                        if(GB.ctrMast_f==1){
                            int slotCnt=-1;
                            for(int i=0;i<14;i++){
                                SlotData slotData = cla.cm1.icsData.slotDatas[i];
                                if(slotData.type.equals("ctr")){
                                    if(cla.cm1.nsta.slotCnt!=i){
                                        slotCnt=i;
                                        break;
                                    }
                                }
                            }
                            if(slotCnt>=0){
                                cla.cm1.cmdFunc("upLoadFile "+slotCnt+" paraSet.json t.json");
                            }    
                        }
                    }
                }
                GB.preParaSetTime = nowParaSetTime;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        File file = new File(fileName);
        if (file.exists() && !file.isDirectory()) {
            String jsonStr = Lib.readStringFile(fileName);
            if (jsonStr == null) {
                return;
            }
            try {
                JSONObject jsObj = new JSONObject(jsonStr);
                Iterator<String> it = jsObj.keys();
                while (it.hasNext()) {
                    String key = it.next();
                    Object obj = jsObj.get(key);
                    paraSetMap.put(key, obj);
                }
                return;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return;

    }

    public boolean handleCommand(JSONObject cmdJso) {
        String retStr = "";
        retStr = jsobjGet(cmdJso, "act");
        errCnt = 0;
        okStr = "";
        errStr = "";
        JSONObject webOut;
        try {
            switch (retStr) {
                case "saveParaSet":
                    cla.getParaSetMap();
                    break;
                case "testResponse":
                    webOut = new JSONObject();
                    webOut.put("status", "OK");
                    GB.webRetStr = webOut.toString();
                    break;
                case "transGsmToMp3":
                    retStr = jsobjGet(cmdJso, "path");
                    if (retStr == null) {
                        return true;
                    }
                    String path = retStr.toString();
                    retStr = jsobjGet(cmdJso, "inFileName");
                    if (retStr == null) {
                        return true;
                    }
                    String inFileName = retStr.toString();
                    retStr = jsobjGet(cmdJso, "outFileName");
                    if (retStr == null) {
                        return true;
                    }
                    String outFileName = retStr.toString();
                    path = GB.webRootPath + "/user-webIcs/record/";
                    String exeStr = "ffmpeg.exe -y -i " + inFileName + " -vn -ar 8000 -ac 1 -b:a 192k " + outFileName;
                    //path="D:/kevin/myCode/webIcs/build/web/user-webIcs/record/";
                    Process process = Runtime.getRuntime().exec(path + exeStr, null, new File(path));
                    process.waitFor();

                    break;
                default:
                    errCnt = 1;
                    errStr = "No this Command !!!";
                    break;
            }
        } catch (Exception ex) {
            errCnt = 1;
            errStr = "userSet.json Formate Error !!!";
        }
        return true;
    }

    public String jsobjGet(JSONObject in, String name) {
        try {
            String retStr = in.get(name).toString();
            return retStr;
        } catch (JSONException ex) {
            return null;
        }
    }

    public void selfTestPrg(JSONObject jobj) {
        try {
            String act = (String) jobj.get("act");
            String subAct = (String) jobj.get("subAct");
            int slotCnt = (int) jobj.get("slotCnt");
            int actInx = (int) jobj.get("actInx");
            String cmdStr = subAct + "~" + slotCnt + "~" + actInx;
            String[] strA = cmdStr.split("~");
            switch (strA[0]) {
                case "slotTest":
                    cla.taskStack.addTaskStrA(strA);
                    cla.taskStack.addHoldKey("slotTest", "testHoldKey");
                    break;
            }
        } catch (Exception ex) {

        }

    }

    static JSONObject wsCallBack(String userName, JSONObject mesJson, String actStr, JSONObject outJson) {
        Ics cla = Ics.scla;
        JSONObject valueJson;
        JSONArray arrayJson;
        Object obj;
        String str;

        String[] strA = actStr.split("#");
        if (!strA[0].equals("tick")) {
            System.out.println(actStr);
        }
        try {
            switch (strA[0]) {
                case "tick":
                    Date dNow = new Date();
                    SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    String tstr = ft.format(dNow);
                    cla.icsData.slotDatas[0].startTime = tstr;
                    //=============================================

                    /*
                    Object userName=mesJson.get("userName").toString();
                    if(userName!=null){
                        ConnectCla conObj=cla.connectMap.get(userName.toString());
                        if(conObj!=null){
                            conObj.time=0;
                            break;
                        }
                        else{
                            conObj=new ConnectCla(userName.toString(),10);
                            cla.connectMap.put(userName.toString(), conObj);
                        }
                    }
                     */
                    //=============================================


                    String jstr = KvJson.objToJson(cla.icsData, "base3");
                    if (jstr == null) {
                        break;
                    }
                    JSONObject jobj = new JSONObject(jstr);
                    outJson.put("icsDatas", jobj.toString());
                    if (cla.tickBackValue != null) {
                        if ((userName.equals(cla.tickBackUserName))) {
                            outJson.put("tickBackValue", cla.tickBackValue.toString());
                            cla.tickBackValue = null;
                        }
                    }
                    break;
                case "phoneKeyClick":
                    obj = Lib.getJson(mesJson, "key");
                    if (obj == null) {
                        break;
                    }
                    String key = (String) obj;
                    strA = key.split("~");
                    obj = Lib.getJson(mesJson, "phoneSet");
                    if (obj == null) {
                        break;
                    }
                    int phoneSet = (int) obj;
                    HashMap<String, Object> sipCommands;
                    sipCommands=null;
                    if (phoneSet == 0) 
                        sipCommands = cla.sip0Commands;
                    if (phoneSet == 1) 
                        sipCommands = cla.sip1Commands;
                    if (phoneSet == 2) 
                        sipCommands = cla.sip2Commands;
                    if(sipCommands==null)
                        break;
                    if (strA[0].equals("hotline")) {
                        if (phoneSet == 0) {
                            obj = GB.paraSetMap.get("phAHotlines");
                        } else {
                            obj = GB.paraSetMap.get("phBHotlines");
                        }
                        arrayJson = (JSONArray) obj;
                        str = arrayJson.optString(Integer.parseInt(strA[1]));
                        strA = str.split("~");
                        if (strA[0].length() > 0 && strA[1].length() > 0) {
                            String number = strA[1];
                            sipCommands.put("keyIn", "call " + number);
                            Base3.log.info("callNumber: softPhone" + (phoneSet + 1) + " ==> " + number);

                        }

                    } else {
                        sipCommands.put("keyIn", key);
                    }
                    break;
                case "callNumber":
                    obj = Lib.getJson(mesJson, "number");
                    if (obj == null) {
                        break;
                    }
                    String number = (String) obj;
                    obj = Lib.getJson(mesJson, "phoneSet");
                    if (obj == null) {
                        break;
                    }
                    phoneSet = (int) obj;
                    sipCommands=null;
                    if (phoneSet == 0) 
                        sipCommands = cla.sip0Commands;
                    if (phoneSet == 1) 
                        sipCommands = cla.sip1Commands;
                    if (phoneSet == 2) 
                        sipCommands = cla.sip2Commands;
                    if(sipCommands==null)
                        break;
                    sipCommands.put("keyIn", "call " + number);
                    Base3.log.info("callNumber: softPhone" + (phoneSet + 1) + " ==> " + number);
                    break;
                case "listenNumber":
                    obj = Lib.getJson(mesJson, "number");
                    if (obj == null) {
                        break;
                    }
                    number = (String) obj;
                    obj = Lib.getJson(mesJson, "phoneSet");
                    if (obj == null) {
                        break;
                    }
                    phoneSet = (int) obj;
                    sipCommands=null;
                    if (phoneSet == 0) 
                        sipCommands = cla.sip0Commands;
                    if (phoneSet == 1) 
                        sipCommands = cla.sip1Commands;
                    if (phoneSet == 2) 
                        sipCommands = cla.sip2Commands;
                    if(sipCommands==null)
                        break;
                        
                    sipCommands.put("keyIn", "call *870" + number);
                    Base3.log.info("callNumber: softPhone" + (phoneSet + 1) + " ==> " + number);
                    break;

                case "selfTest":
                    outJson.put("status", "OK");
                    outJson.put("message", "Command Has Received");
                    cla.selfTestPrg(mesJson);
                    break;
                case "selfTestAllStop":
                    outJson.put("status", "OK");
                    outJson.put("message", "Command Has Received");
                    cla.commandStr = "";
                    cla.taskStack.taskMap.remove("slotTest");
                    break;
                case "getExRecordNames":
                    outJson.put("status", "OK");
                    outJson.put("message", "Command Has Received");
                    obj = Lib.getJson(mesJson, "exNumber");
                    if (obj == null) {
                        break;
                    }
                    String exNumber = (String) obj;
                    int slotCnt = cla.getSlotCnt(exNumber);
                    if (slotCnt < 0) {
                        break;
                    }
                    cla.cm1.cmdFunc("getExRecordNames " + slotCnt + " " + exNumber);

                    cla.tickBackUserName = userName;
                    break;
                case "getRecordFile":
                    outJson.put("status", "OK");
                    outJson.put("message", "Command Has Received");
                    obj = Lib.getJson(mesJson, "fileName");
                    if (obj == null) {
                        break;
                    }
                    String fileName = (String) obj;
                    obj = Lib.getJson(mesJson, "exNumber");
                    if (obj == null) {
                        break;
                    }
                    exNumber = (String) obj;
                    slotCnt = cla.getSlotCnt(exNumber);
                    if (slotCnt < 0) {
                        break;
                    }
                    cla.cm1.cmdFunc("getRecordFile " + slotCnt + " " + fileName);
                    cla.tickBackUserName = userName;
                    break;

                case "getBinFile":
                    outJson.put("status", "OK");
                    outJson.put("message", "Command Has Received");
                    cla.cm1.cmdFunc("getBinFile");
                    cla.tickBackUserName = userName;
                    break;
                case "getTextFile":
                    obj = Lib.getJson(mesJson, "fileName");
                    if (obj == null) {
                        break;
                    }
                    String fName = obj.toString();
                    String contextStr = Lib.readStringFile(fName);
                    if (contextStr == null) {
                        contextStr = "";
                    }
                    outJson.put("status", "OK");
                    outJson.put("message", "Command Has Received");
                    outJson.put("context", contextStr);
                    cla.cm1.cmdFunc("getBinFile");
                    cla.tickBackUserName = userName;
                    break;

                case "reloadIcsConf":
                    outJson.put("status", "OK");
                    outJson.put("message", "Command Has Received");
                    cla.cm1.cmdFunc("reNewParaSet -1 forceAll");
                    break;

                case "icsShutDown":
                    outJson.put("status", "OK");
                    outJson.put("message", "Command Has Received");
                    cla.cm1.cmdFunc("icsShutDown -1 forceAll");
                    break;
                case "icsRestart":
                    outJson.put("status", "OK");
                    outJson.put("message", "Command Has Received");
                    cla.cm1.cmdFunc("icsRestart -1 forceAll");
                    break;

                case "reloadIcsExtensions":
                    outJson.put("status", "OK");
                    outJson.put("message", "Command Has Received");
                    cla.cm1.cmdFunc("reNewExtensions -1 forceAll");
                    break;
                case "reloadParaSet":
                    cla.getParaSetMap();
                    break;
                default:
                    outJson.put("status", "ERROR");
                    outJson.put("message", "No This Command");
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return outJson;

        }

        return outJson;

    }

}

class IcsTm1 implements ActionListener {

    String str;
    Ics cla;
    File file;
    FileInputStream reader;
    int secBaseTime = 0;
    int tm1Cnt = 0;
    int tm1Buf = 0;
    int tm1Flag = 0;
    int secCnt = 0;

    IcsTm1(Ics owner) {
        cla = owner;
    }

    public void commandPrg() {
        try {
            String[] strA = cla.commandStr.split("#");
            switch (strA[0]) {
                case "selfTest":
                    break;
            }

        } catch (Exception ex) {
            cla.commandStr = "";
        }
        cla.commandStr = "";
    }

    public void commSip(KvComm comm, HashMap<String, Object> cmds) {
        byte[] bytes = comm.serverSocket.stm.tbuf;
        int len;
        int inx = 0;
        Ics cla = Ics.scla;

        int serialId = 0xffff;

        bytes[inx++] = (byte) (cla.sipPhoneDeviceId & 255);
        bytes[inx++] = (byte) ((cla.sipPhoneDeviceId >> 8) & 255);
        bytes[inx++] = (byte) (serialId & 255);
        bytes[inx++] = (byte) ((serialId >> 8) & 255);
        bytes[inx++] = (byte) (0x00);//
        bytes[inx++] = (byte) (0xAB);//group id
        len = 0;
        int lenAddr = inx;
        bytes[inx++] = (byte) (len & 255);
        bytes[inx++] = (byte) ((len >> 8) & 255);
        int cmdInx = 0x1000;    //tick
        bytes[inx++] = (byte) (cmdInx & 255);
        bytes[inx++] = (byte) ((cmdInx >> 8) & 255);
        //==================================================================================
        for (String key : cmds.keySet()) {
            String[] strA = key.split("#");
            Object obj = cmds.get(key);
            if (strA[0].equals("click")) {
                bytes[inx++] = (byte) (0x1f);
                bytes[inx++] = (byte) (0x8);
                String[] strB = GB.real_ip_str.split("\\.");
                int para0 = Integer.parseInt(strB[1]) + Integer.parseInt(strB[0]) * 256;
                int para1 = Integer.parseInt(strB[3]) + Integer.parseInt(strB[2]) * 256;
                int para2 = comm.serverSocket.port;
                int para3 = 0x00;
                bytes[inx++] = (byte) (para0 & 255);
                bytes[inx++] = (byte) ((para0 >> 8) & 255);
                bytes[inx++] = (byte) (para1 & 255);
                bytes[inx++] = (byte) ((para1 >> 8) & 255);
                bytes[inx++] = (byte) (para2 & 255);
                bytes[inx++] = (byte) ((para2 >> 8) & 255);
                bytes[inx++] = (byte) (para3 & 255);
                bytes[inx++] = (byte) ((para3 >> 8) & 255);
                cmds.remove(key);
                break;
            }
            if (strA[0].equals("keyIn")) {
                String keyInStr = (String) obj;
                if (keyInStr.equals("#")) {
                    keyInStr = "ok";
                }

                byte[] byteArray = keyInStr.getBytes();
                len = byteArray.length;
                bytes[inx++] = (byte) (0x14);
                bytes[inx++] = (byte) (len & 255);
                for (int i = 0; i < byteArray.length; i++) {
                    bytes[inx++] = byteArray[i];
                }
                cmds.remove(key);
                break;
            }

        }
        len = inx - 10;
        bytes[lenAddr] = (byte) (len & 255);
        bytes[lenAddr + 1] = (byte) ((len >> 8) & 255);

        comm.serverSocket.stm.tbuf_byte = inx;
        comm.serverSocket.tx_startMode = "txIpStm";
    }

    public void emuPrg() {
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        secBaseTime++;
        if (secBaseTime > 20) {
            secBaseTime = 0;
            secCnt += 1;

        }
        tm1Cnt++;
        tm1Flag = tm1Cnt ^ tm1Buf;
        tm1Buf = tm1Cnt;


        /*
        for (String key : cla.icsData.exStatusMap.keySet()) {
            ExStatus exObj = (ExStatus) cla.icsData.exStatusMap.get(key);
            if(exObj.status<2)
                continue;
            if (exObj.status != exObj.preStatus) {
                int chg = exObj.preStatus * 16 + exObj.status;
                exObj.preStatus = exObj.status;
                if (chg == 0x24) {
                    Root.log(1, "Extension " + key + " dial to " + exObj.callWith + ".");
                    continue;
                }
                if (chg == 0x43) {
                    Root.log(1, "Extension " + key + " connect to " + exObj.callWith + ".");
                    continue;
                }
                if (chg == 0x32) {
                    Root.log(1, "Extension " + key + " disconnect.");
                    continue;
                }
            }
        }
         */
        try {
            String icsUiSet = cla.paraSetMap.get("icsUiSet").toString();
            String[] strA = icsUiSet.split("~");

            if (cla.mainSoftPhone_exist_f != 0) {
                cla.sip0Comm.serverSocket.tx_ip = strA[1];
                cla.sip0Comm.serverSocket.tx_port = cla.mainSoftPhone_port;
                cla.sip0Commands.put("click", 0);
                if (cla.sip0Comm.serverSocket.tx_startMode.length() == 0) {
                    commSip(cla.sip0Comm, cla.sip0Commands);
                    cla.icsData.sipData0.connectTime++;
                    if (cla.icsData.sipData0.connectTime == 30) {
                        cla.icsData.sipData0.sipStatus = "中山科學研究院";
                        cla.icsData.sipData0.sipAction = "軟體電話 (未連線)";
                    }
                }

            }

            if (cla.subSoftPhone_exist_f != 0) {
                cla.sip1Comm.serverSocket.tx_ip = strA[2];
                cla.sip1Comm.serverSocket.tx_port = cla.subSoftPhone_port;
                cla.sip1Commands.put("click", 0);
                if (cla.sip1Comm.serverSocket.tx_startMode.length() == 0) {
                    commSip(cla.sip1Comm, cla.sip1Commands);
                    cla.icsData.sipData1.connectTime++;
                    if (cla.icsData.sipData1.connectTime == 30) {
                        cla.icsData.sipData1.sipStatus = "中山科學研究院";
                        cla.icsData.sipData1.sipAction = "軟體電話 (未連線)";
                    }
                }
            }
            
            if (cla.monitorPhone_exist_f != 0) {
                cla.sip2Comm.serverSocket.tx_ip = strA[3];
                cla.sip2Comm.serverSocket.tx_port = cla.monitorPhone_port;
                cla.sip2Commands.put("click", 0);
                if (cla.sip2Comm.serverSocket.tx_startMode.length() == 0) {
                    commSip(cla.sip2Comm, cla.sip2Commands);
                    cla.icsData.sipData2.connectTime++;
                    if (cla.icsData.sipData2.connectTime == 30) {
                        cla.icsData.sipData2.sipStatus = "中山科學研究院";
                        cla.icsData.sipData2.sipAction = "軟體電話 (未連線)";
                    }
                }
            }
            
            
            commandPrg();
        } catch (Exception ex) {
            ex.printStackTrace();
            //System.out.println("ics tm1 error !!!");
        }

    }
}

class IcsUartC {

    String name;
    SerialPort uartPort;
    int seted_f = 0;
    CommPortSender uartTx;
    CommPortReceiver uartRx;
    String portStr = "1";
    String boudrateStr = "115200";
    String parityStr = "None";//Noen | Even | Odd
    public BytesCallback cbk;

    IcsUartC(String _name) {
        name = _name;
    }

    void setCallBack(BytesCallback callBackPrg) {
        cbk = callBackPrg;
    }

    public static String listUart() {
        String comName;
        SerialPort[] ports = SerialPort.getCommPorts();
        String str = "";
        for (int i = 0; i < ports.length; i++) {
            SerialPort sp = ports[i];
            comName = sp.getSystemPortName();
            if (i != 0) {
                str += ",";
            }
            str += comName;
        }
        return str;
    }

    public String setUart() {
        String errStr = null;
        try {
            closeUart();
            int sys232Port = Lib.str2int(portStr, 1);
            int sys232DataBit = 8;
            int sys232Boudrate = Lib.str2int(boudrateStr, 115200);
            String comErr = openUart("COM" + sys232Port, parityStr, sys232Boudrate);
            return comErr;
        } catch (Exception ex) {
            String comErr = "userSet.json Formate Error !!!";
            return comErr;
        }
    }

    public void closeUart() {
        if (uartPort != null) {
            uartRx.terminate();
            Lib.thSleep(10);
            uartPort.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
            uartPort.removeDataListener();
            boolean result = uartPort.closePort();
            uartPort = null;
            seted_f = 0;
        }
    }

    public String openUart(String portName, String Parity, int boudrate) {
        String comName;
        SerialPort[] ports = SerialPort.getCommPorts();
        seted_f = 0;
        if (ports.length == 0) {
            return "Uart1: No serial ports available!";
        }
        int portToUse = -1;
        for (int i = 0; i < ports.length; i++) {
            SerialPort sp = ports[i];
            comName = sp.getSystemPortName();//.toLowerCase();
            if (comName.equals(portName)) {
                portToUse = i;
                break;
            }
        }
        if (portToUse < 0) {
            return "Uart1: No this port on this system!";
        }
        int parity = SerialPort.NO_PARITY;
        if (Parity.equals("Even")) {
            parity = SerialPort.EVEN_PARITY;
        }
        if (Parity.equals("Odd")) {
            parity = SerialPort.ODD_PARITY;
        }
        uartPort = ports[portToUse];
        uartPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        uartPort.setComPortParameters(boudrate, 8, SerialPort.ONE_STOP_BIT, parity);
        //serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);
        //serialPort.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
        //logger.debug("Going to open the port...");
        boolean result = uartPort.openPort();
        if (result) {
            uartTx = new CommPortSender();
            uartTx.setWriterStream(uartPort.getOutputStream());
            // setup serial port reader
            uartRx = new CommPortReceiver(uartPort.getInputStream());
            uartRx.setCallBack((bytes, len) -> rxPrg(bytes, len));
            uartRx.start();
            seted_f = 1;
        } else {
            seted_f = 0;
            uartPort = null;
            return "Uart1: This port is in used !!!";
        }
        return null;
    }

    String rxPrg(byte[] bts, int len) {
        if (cbk != null) {
            cbk.prg(bts, len);
        }
        return null;
    }

}

class ServerReturnC {

    byte[] buf;
    int len = 0;
    int tx_f = 0;
    int size = 0;

    ServerReturnC(int _size) {
        size = _size;
        buf = new byte[size];
    }
}

//status = 0;//0:none(dark), 1:exist(y blink) ,2: ready(y), 3:paraSet loaded(green blink), 4:pbx run(g), 5:error(red)
