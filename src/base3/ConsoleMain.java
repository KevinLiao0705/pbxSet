package base3;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.FileWriter;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

//asterisk sound location: usr/share/asterisk/sounds/en/
public class ConsoleMain {

    static ConsoleMain scla;
    String title_str = "title_str";
    int fullScr_f = 0;
    int winW = 1600;
    int winH = 800;
    int debug_f = 1;
    int cmdInx = 0;
    Timer tm1 = null;
    PbxSet pbxSet;
    ConsoleMainCmdExe cexe;
    Map<String, CmdTask> taskMap;
    Map<String, ChkRxA> rxMap;
    Map<String, CmdStatus> cmdStaMap;
    //SlotSta[] slotStaA = new SlotSta[16];
    int ctrIoPort = 23501;
    int myDeviceId = 0x2403;
    int devicePcioId = 0x2301;
    int mySerialId = 0x0000;
    int ctrCommPort = 23499;

    KvComm ioComm;
    KvComm slotComm;
    IcsData icsData;
    NowSlotSta nsta = new NowSlotSta();

    int[] getSlotDataDelays = new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
    int[] getSlotDataWaits = new int[]{50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50};
    int[] getSlotDataTimes = new int[16];
    int[] getSlotDataRxfs = new int[16];
    int debugCnt = 0;
    Ics owner;
    int pcioConTime=0;
    //===========================
    public ConsoleMain(Ics _owner) {
        ConsoleMain.scla = this;
        owner = _owner;
        nsta.ip = "127.0.0.1";
        nsta.port = ctrCommPort;
        //nsta.port = 23400;
        nsta.type = "ctr";
        nsta.count = 0;
        nsta.status = 4;
        icsData = owner.icsData;
    }

    public void create() {
        String str;
        String errStr;
        //=======================================================
        final ConsoleMain cla = this;
        //=======================================================
        /*
        pbxSet = new PbxSet();
        pbxSet.create();
        pbxSet.shellRx = new ShellRx() {
            @Override
            public void sshRx(String str) {
                if (str != null) {
                    String[] strA = str.split("\n");
                    int inx = 0;

                    while (inx < strA.length) {
                        System.out.println(strA[inx]);
                        inx = decShell(strA, inx);
                    }
                    //System.out.print("***BREAK***");
                } else {
                    //System.out.print("***END***");
                }
            }
        };
         */
        //=======================================================
        rxMap = new HashMap<String, ChkRxA>();
        taskMap = new HashMap<String, CmdTask>();
        cmdStaMap = new HashMap<String, CmdStatus>();
        cexe = new ConsoleMainCmdExe(cla, taskMap);
        //=======================================
        ioComm = new KvComm("ioComm", "serverSocket");
        ioComm.serverSocket.format = 1;
        ioComm.serverSocket.rxcon_ltim = 100;
        ioComm.serverSocket.port = ctrIoPort;
        ioComm.serverSocket.stm.setCallBack(new BytesCallback() {
            @Override
            public String prg(byte[] bytes, int len) {
                gnRxPrg("", bytes, len);
                socketServerReturn();
                return null;

            }
        });
        ioComm.open();
        //=======================================

        slotComm = new KvComm("slotComm", "serverSocket", 65536 * 256);
        slotComm.serverSocket.format = 2;
        slotComm.serverSocket.rxcon_ltim = 100;
        slotComm.serverSocket.port = nsta.port;
        slotComm.serverSocket.setCallBack(new BytesCallback() {
            @Override
            public String prg(byte[] bytes, int len) {
                FileWriter fw;

                for (int i = 0; i < cla.slotComm.serverSocket.myRxDataList.size(); i++) {
                    MyRxData mrd = cla.slotComm.serverSocket.myRxDataList.get(i);
                    try {
                        if (mrd.format == 2) {
                            continue;
                        }
                        if (mrd.format == 0) {
                            String jstr = new String(bytes, mrd.offset, mrd.len, "UTF-8");
                            JSONObject jobj = new JSONObject(jstr);
                            String cmdStr = jobj.get("act").toString();
                            if (cmdStr.equals("actResponse")) {
                                //System.out.println("<CtrRxUiRsp>" + jobj.get("actName").toString());
                                String actName = jobj.get("actName").toString();
                                if (actName.equals("getSlotData")) {
                                    int slotCnt = (int) jobj.get("slotCnt");
                                    /*
                                    System.out.print(","+slotCnt);
                                    debugCnt++;
                                    if(debugCnt>=80){
                                        System.out.print("\n");
                                        debugCnt=0;
                                    }
                                     */

                                    cla.getSlotDataRxfs[slotCnt] = 1;

                                    SlotData slotData = cla.icsData.slotDatas[slotCnt];
                                    JSONObject slotInf = (JSONObject) jobj.get("slotInf");
                                    if (slotInf != null) {
                                        //slotData.type = (String) slotInf.get("type");
                                        //slotData.count = (int) slotInf.get("count");
                                        //slotData.status = (int) slotInf.get("status");
                                        slotData.action = (String) slotInf.get("action");
                                        slotData.inf = (String) slotInf.get("inf");
                                        try {
                                            slotData.softVer = (String) slotInf.get("softVer");
                                            slotData.firmVer = (String) slotInf.get("firmVer");
                                            slotData.startTime = (String) slotInf.get("startTime");
                                        } catch (Exception ex) {

                                        }
                                        slotData.connectTime = 0;
                                    }
                                    try {
                                        slotData.exInf = (String) jobj.get("exInf");

                                        String[] strA = slotData.exInf.split("~");
                                        for (int j = 0; j < strA.length; j++) {
                                            String[] strB = strA[j].split(",");
                                            if (strB.length != 3) {
                                                continue;
                                            }
                                            String exNo = strB[0].trim();
                                            Object obj = cla.icsData.exStatusMap.get(exNo);
                                            ExStatus st = new ExStatus(exNo);
                                            st.status = Lib.str2int(strB[1].trim(), 0);
                                            st.callWith = strB[2].trim();
                                            st.connectTime=0;
                                            if (obj != null) {
                                                ExStatus exObj = (ExStatus) obj;
                                                int staChg = exObj.status * 16 + st.status;
                                                if (staChg == 0x24) {
                                                    icsData.logStr = "Extension " + exNo + " dial to " + st.callWith + ".";
                                                }
                                                if (staChg == 0x43) {
                                                    icsData.logStr = "Extension " + exNo + " connect to " + st.callWith + ".";
                                                }
                                                if (staChg == 0x32) {
                                                    icsData.logStr = "Extension " + exNo + " disconnect.";
                                                }

                                            }
                                            cla.icsData.exStatusMap.put(exNo, st);
                                        }

                                    } catch (Exception Ex) {

                                    }

                                }
                                String addr = slotComm.serverSocket.rxip;
                                ChkRxA crx = cla.rxMap.get(addr);
                                if (crx != null) {
                                    crx.reti = (int) jobj.get("reti");
                                    crx.retCmdInx = (int) jobj.get("cmdInx");
                                    crx.statusStr = jobj.get("status").toString();
                                    crx.jobj = jobj;
                                    crx.rxTrig_f = 1;
                                }
                                continue;
                            }
                            if (cmdStr.equals("getSlotInf")) {
                                //System.out.println("<CtrRxUiRsp>" + jobj.get("actName").toString());
                                JSONObject jout = new JSONObject();
                                jout.put("act", "actResponse");
                                jout.put("actName", cmdStr);
                                jout.put("status", "ok");
                                jout.put("cmdInx", (int) jobj.get("cmdInx"));
                                jout.put("reti", 1);
                                jout.put("slotInf", nsta.getJson());
                                jout.put("exInf", "");
                                String str = "";
                                cla.slotComm.serverSocket.txPackRetJsonStr(jout.toString());
                                continue;
                            }

                            if (cmdStr.equals("getSlotData")) {
                                //System.out.println("<CtrRxUiRsp>" + jobj.get("actName").toString());
                                JSONObject jout = new JSONObject();
                                jout.put("act", "actResponse");
                                jout.put("actName", cmdStr);
                                jout.put("status", "ok");
                                jout.put("cmdInx", (int) jobj.get("cmdInx"));
                                jout.put("reti", 1);
                                jout.put("slotInf", nsta.getJson());
                                int slotCnt = (int) jobj.get("slotCnt");
                                jout.put("slotCnt", slotCnt);
                                String str = "";
                                cla.slotComm.serverSocket.txPackRetJsonStr(jout.toString());
                                continue;
                            }

                            if (cmdStr.equals("upLoadFile")) {
                                String wFileName = jobj.get("wFileName").toString();
                                String content = jobj.get("content").toString();
                                fw = new FileWriter(wFileName);
                                fw.write(content);
                                fw.flush();
                                fw.close();
                                String retStr = Lib.actResponse(cmdStr, "ok", cmdInx);
                                cla.slotComm.serverSocket.txPackRetJsonStr(retStr);
                                System.out.println("\n" + cmdStr + " " + wFileName);
                                continue;
                            }

                        }
                    } catch (Exception ex) {
                    }

                }
                return null;
            }
        });
        slotComm.open();

        //=====================================
        //CmdTask task1 = new CmdTask("test");
        //task1.retryAmt = 0;
        //cexe.addMap(task1);
        //=====================================
        if (cla.tm1 == null) {
            cla.tm1 = new Timer();
            tm1.schedule(new ConsoleMainTm1(cla), 1000, 20);
        }
        //
        if(GB.webSocketIp.length()!=0)
            KvWebSocketServer.startWebSocketServer(GB.webSocketIp,GB.webSocketServerPort);//<<debug

        //=====================================
        System.out.println("ConsoleMain Ready.");
        while (true) {
            Scanner input = new Scanner(System.in);
            str = input.nextLine().trim();
            if (str.length() == 0) {
                continue;
            }
            errStr = cmdFunc(str);
            if (errStr != null) {
                System.out.println(errStr);
            }
        }
    }

    public void socketServerReturn() {
        byte[] sockUartData_buf = new byte[22];
        int inx = 0;
        sockUartData_buf[inx++] = (byte) ((myDeviceId) & 255);
        sockUartData_buf[inx++] = (byte) ((myDeviceId >> 8) & 255);
        sockUartData_buf[inx++] = (byte) ((mySerialId) & 255);
        sockUartData_buf[inx++] = (byte) ((mySerialId >> 8) & 255);
        int groupFlag = 0xAB00;
        sockUartData_buf[inx++] = (byte) ((groupFlag) & 255);
        sockUartData_buf[inx++] = (byte) ((groupFlag >> 8) & 255);
        int payLoadLen = 14;
        sockUartData_buf[inx++] = (byte) ((payLoadLen) & 255);
        sockUartData_buf[inx++] = (byte) ((payLoadLen >> 8) & 255);
        int cmdInx = 0x1000;
        sockUartData_buf[inx++] = (byte) ((cmdInx) & 255);
        sockUartData_buf[inx++] = (byte) ((cmdInx >> 8) & 255);

        sockUartData_buf[inx++] = (byte) ((nsta.status) & 255);
        sockUartData_buf[inx++] = (byte) ((nsta.status >> 8) & 255);

        sockUartData_buf[inx++] = (byte) ((nsta.channelFlag) & 255);
        sockUartData_buf[inx++] = (byte) ((nsta.channelFlag >> 8) & 255);
        //String strA=nsta.ip.split("\\.");
        sockUartData_buf[inx++] = (byte) ((GB.realIp[0]) & 255);
        sockUartData_buf[inx++] = (byte) ((GB.realIp[1]) & 255);
        sockUartData_buf[inx++] = (byte) ((GB.realIp[2]) & 255);
        sockUartData_buf[inx++] = (byte) ((GB.realIp[3]) & 255);

        sockUartData_buf[inx++] = (byte) ((nsta.ledFlag) & 255);
        sockUartData_buf[inx++] = (byte) ((nsta.ledFlag >> 8) & 255);
        sockUartData_buf[inx++] = (byte) ((nsta.ledFlag >> 16) & 255);
        sockUartData_buf[inx++] = (byte) ((nsta.ledFlag >> 24) & 255);

        int sockUartData_len = inx;

        MyStm stm = ioComm.serverSocket.stm;
        int stx_index = 0;
        stm.tbuf[stx_index++] = (byte) ((devicePcioId) & 255);
        stm.tbuf[stx_index++] = (byte) ((devicePcioId >> 8) & 255);
        stm.tbuf[stx_index++] = (byte) (255);
        stm.tbuf[stx_index++] = (byte) (255);

        stm.tbuf[stx_index++] = (byte) (0x10);//uart0
        stm.tbuf[stx_index++] = (byte) (0x00);//flag
        stm.tbuf[stx_index++] = (byte) (sockUartData_len & 255);//len low byte
        stm.tbuf[stx_index++] = (byte) ((sockUartData_len >> 8) & 255);//len high byte
        for (int i = 0; i < sockUartData_len; i++) {
            stm.tbuf[stx_index++] = sockUartData_buf[i];
        }
        stm.tbuf_byte = stx_index;
        ioComm.serverSocket.txReturn();
    }

    public String gnRxPrg(String name, byte[] bts, int len) {
        ConsoleMain cla = this;
        int inx = 0;
        int inxLim = len;
        int packageId = (bts[inx + 1] & 255) * 256 + (bts[inx + 0] & 255);
        int packageSerialId = (bts[inx + 3] & 255) * 256 + (bts[inx + 2] & 255);
        int packageGroupId = (bts[inx + 4] & 255);
        int packageFlags = (bts[inx + 5] & 255);
        int packageLen = (bts[inx + 7] & 255) * 256 + (bts[inx + 6] & 255);
        inx += 8;
        if (packageId == 0x2303 && packageGroupId == 0x10) {//from Pcio uart11
            int deviceId = (bts[inx + 1] & 255) * 256 + (bts[inx + 0] & 255);
            int deviceSerialId = (bts[inx + 3] & 255) * 256 + (bts[inx + 2] & 255);
            inx += 4;
            if (deviceId == myDeviceId && deviceSerialId == 0x00) {//from slot device
                pcioConTime=0;
                while (inx + 4 < inxLim) {
                    int groupFlag = (bts[inx + 1] & 255) * 256 + (bts[inx + 0] & 255);
                    int dataLen = (bts[inx + 3] & 255) * 256 + (bts[inx + 2] & 255);
                    int ix = inx + 4;
                    if (groupFlag == 0xAB00) {//dataBeginId
                        int cmdInx = (bts[ix + 1] & 255) * 256 + (bts[ix + 0] & 255);
                        ix += 2;
                        if (cmdInx == 0x1000) {
                            nsta.mcuFlag0 = (bts[ix + 0] & 255);
                            nsta.swFlag = (bts[ix + 1] & 255);
                            int slotType = nsta.swFlag >> 4;
                            int slotCount = nsta.swFlag & 3;
                            int slotInx=nsta.mcuFlag0;

                            GB.icsGroupId = (bts[ix + 6] & 255);
                            GB.mastctr = (bts[ix + 7] & 255);
                            cla.icsData.mastctr=GB.mastctr;

                            //0:none,1:ctr,2:sip,3:fxo,4:fxs,5:t1s,6:mag,7:roip,8:rec
                            String typeStr = "none";
                            if (slotType == 1) {
                                typeStr = "ctr";
                            }
                            if (slotType == 2) {
                                typeStr = "sip";
                            }
                            if (slotType == 3) {
                                typeStr = "fxo";
                            }
                            if (slotType == 4) {
                                typeStr = "fxs";
                            }
                            if (slotType == 5) {
                                typeStr = "t1s";
                            }
                            if (slotType == 6) {
                                typeStr = "mag";
                            }
                            if (slotType == 7) {
                                typeStr = "roip";
                            }
                            if (slotType == 8) {
                                typeStr = "rec";
                            }
                            
                            if(typeStr.equals("ctr")){
                                if((GB.mastctr&0x80)!=0){
                                    int mastSlotCnt=GB.mastctr&15;
                                    if(mastSlotCnt==slotInx){
                                        if(GB.ctrMast_f==0){
                                            GB.slotType="none";
                                        }
                                        GB.ctrMast_f=1;
                                    }
                                    else{
                                        if(GB.ctrMast_f==1){
                                            
                                        }
                                        GB.ctrMast_f=0;
                                    }
                                }
                            }    
                            if (!GB.slotType.equals(typeStr) || GB.slotCount != slotCount) {
                                GB.slotType = typeStr;
                                GB.slotCount = slotCount;
                                if(GB.ctrMast_f==1)
                                    System.out.println("\n mastCtr enable");
                                GB.chgSlotType();
                                String ipStr = GB.getSlotIp();
                                resetIp(ipStr);
                                if (GB.slotType.equals("ctr")) {
                                    if (GB.ctrMast_f == 1) {
                                        if(GB.os_inx==0)
                                            KvWebSocketServer.startWebSocketServer(GB.real_ip_str,GB.webSocketServerPort);
                                        else
                                            KvWebSocketServer.startWebSocketServer(GB.virtualIp,GB.webSocketServerPort);
                                    }
                                }
                            }

                            nsta.mcuFlag1 = (bts[ix + 3] & 255) * 256 + (bts[ix + 2] & 255);
                            nsta.firmVer = "" + ((bts[ix + 4] & 255) >> 4) + "." + (bts[ix + 4] & 15);
                            nsta.setIp = bts[ix + 4] & 255;
                            nsta.setIp <<= 8;
                            nsta.setIp += bts[ix + 5] & 255;
                            nsta.setIp <<= 8;
                            nsta.setIp += bts[ix + 6] & 255;
                            nsta.setIp <<= 8;
                            nsta.setIp += bts[ix + 7] & 255;
                        }
                        ix += 8;
                        int dbgchs = 0;
                        for (int i = 0; i < 128; i++) {
                            nsta.allSlotSta[i] = bts[ix + i];
                            dbgchs += (bts[ix + i] & 255);
                        }
                        int chs = (bts[ix + 128] & 255) + (bts[ix + 129] & 255) * 256;
                        if (chs != dbgchs) {
                            chs += 0;

                        }

                        for (int i = 0; i < icsData.slotDatas.length; i++) {
                            SlotData slotData = icsData.slotDatas[i];
                            int type = (nsta.allSlotSta[8 * i + 1] >> 4) & 15;
                            int slotCnt = (nsta.allSlotSta[8 * i + 1]) & 3;
                            int slotSta = (nsta.allSlotSta[8 * i + 0]) & 7;
                            String ipStr = "";
                            ipStr += nsta.allSlotSta[8 * i + 4] & 255;
                            ipStr += "." + (nsta.allSlotSta[8 * i + 5] & 255);
                            ipStr += "." + (nsta.allSlotSta[8 * i + 6] & 255);
                            ipStr += "." + (nsta.allSlotSta[8 * i + 7] & 255);

                            //if (i == 6 && type == 0) {
                            //    System.out.print("xxx");
                            //}
                            if (type == 0) {
                                slotData.type = "";
                                slotData.count = 0;
                                slotData.exist_f = 0;
                                slotData.status = 0;
                                slotData.ip = "";
                            }
                            if (type == 1) {
                                slotData.type = "ctr";
                                slotData.exist_f = 1;
                                slotData.count = slotCnt;
                                slotData.status = slotSta;
                                slotData.ip = ipStr;
                            }
                            if (type == 2) {
                                slotData.type = "sip";
                                slotData.exist_f = 1;
                                slotData.count = slotCnt;
                                slotData.status = slotSta;
                                //ipStr="192.168.0.28";<<debug
                                slotData.ip = ipStr;
                                slotData.port = 23400;
                            }
                            if (type == 5) {
                                slotData.type = "t1s";
                                slotData.exist_f = 1;
                                slotData.count = slotCnt;
                                slotData.status = slotSta;
                                slotData.ip = ipStr;
                                slotData.port = 23400;
                            }
                            if (type == 3) {
                                slotData.type = "fxo";
                                slotData.exist_f = 1;
                                slotData.count = slotCnt;
                                slotData.status = slotSta;
                                slotData.ip = ipStr;
                                slotData.port = 23400;
                                //slotData.ip = "192.168.0.28";//<<debug
                            }
                            if (type == 4) {
                                slotData.type = "fxs";
                                slotData.exist_f = 1;
                                slotData.count = slotCnt;
                                slotData.status = slotSta;
                                //ipStr="192.168.0.28";//<<debug
                                slotData.ip = ipStr;
                                slotData.port = 23400;
                            }
                            if (type == 6) {
                                slotData.type = "mag";
                                slotData.exist_f = 1;
                                slotData.count = slotCnt;
                                slotData.status = slotSta;
                                slotData.ip = ipStr;
                                slotData.port = 23400;
                            }
                            if (type == 7) {
                                slotData.type = "roip";
                                slotData.exist_f = 1;
                                slotData.count = slotCnt;
                                slotData.status = slotSta;
                                slotData.ip = ipStr;
                                slotData.port = 23400;
                            }
                            if (type == 8) {
                                slotData.type = "rec";
                                slotData.exist_f = 1;
                                slotData.count = slotCnt;
                                slotData.status = slotSta;
                                slotData.ip = ipStr;
                                slotData.port = 23400;
                            }
                        }

                    }
                    inx += dataLen + 4;
                }
            }
        }
        return null;
    }

    public int decShell(String[] strA, int inx) {
        int index = inx;
        inx++;
        switch (strA[index]) {
            case "mainpbx@mainpbx:~$ ":
                return inx;
        }
        return inx;
    }

    public void clrSlotActed() {
        for (int i = 0; i < icsData.slotDatas.length; i++) {
            icsData.slotDatas[i].acted_f = 0;
        }
    }

    public void resetIp(String ipStr) {
        if (ipStr.equals("")) {
            return;
        }
        String str = "changeIp from " + GB.real_ip_str + " to " + ipStr;
        System.out.println(str);
        /*
        if (GB.set_ip_str.equals(ipStr)) {
            if (GB.real_ip_str.equals(ipStr)) {
                return;
            }
        }
        */
        String cmdStr = "changeIp " + GB.netName + " " + ipStr;
        cmdPrg(cmdStr);
    }

    public String cmdPrg(String cmdstr) {
        String errStr = null;
        String content = null;
        if (cmdstr.equals("exit")) {
            System.exit(0);
            return errStr;
        }
        String[] strCmdA = cmdstr.split(" ");

        if (strCmdA[0].equals("changeIp")) {

            //String winCmds="netsh interface ip set address name=乙太網路 source=static addr="+ipStr;
            //String winCmds = "netsh interface ip set address name=區域連線 source=static addr=" + strCmdA[1];
            System.out.println("Change " + strCmdA[1] + " Ip to " + strCmdA[2]);
            if (GB.os_inx == 0) {
                String winCmds = "netsh interface ip set address name=" + strCmdA[1] + " source=static addr=" + strCmdA[2];
                Process pp;
                try {
                    
                    
                    //Lib.wrInterfaces(strCmdA[1], strCmdA[2], GB.maskStr, GB.gatewayStr);
                    System.out.println("Change " + strCmdA[1] + " Ip to " + strCmdA[2] + " OK.");
                    
                    String ntpIp = GB.paraSetMap.get("ntpServerIp").toString();
                    Lib.wrNtp(ntpIp);
                    
                    //pp = java.lang.Runtime.getRuntime().exec(winCmds);
                    //pp.waitFor();
                    //System.out.print(pp);
                } catch (Exception ex) {
                    Logger.getLogger(ConsoleMain.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                String netName=strCmdA[1];
                String netAddr=strCmdA[2];
                
                
                GB.virtualIp="";

                String cmdStr = "sudo /usr/sbin/ifconfig " + netName + " " + netAddr + " netmask " + GB.maskStr;
                System.out.print("\n" + cmdStr);
                //Lib.wrInterfaces(strCmdA[1], strCmdA[2], GB.maskStr, GB.gatewayStr);
                if (Lib.exe(cmdStr) == 0) {
                    System.out.println("\nChange " + netName + " Ip to " + netAddr + " OK.");
                    GB.real_ip_str = strCmdA[2];
                    String[] strA = GB.real_ip_str.split("\\.");
                    for (int i = 0; i < 4; i++) {
                        GB.realIp[i] = (byte) (Lib.str2int(strA[i], 0) & 255);
                    }

                    String ntpIp = GB.paraSetMap.get("ntpServerIp").toString();
                    Lib.wrNtp(ntpIp);
                    
                    
                    if(GB.ctrMast_f==1){
                        netName=netName+":1";
                        String[] strX=netAddr.split("\\.");
                        netAddr=strX[0]+"."+ strX[1]+"."+strX[2]+".99";
                        GB.virtualIp=netAddr;
                        
                        cmdStr = "sudo /usr/sbin/ifconfig " + netName + " " + netAddr + " netmask " + GB.maskStr;
                        System.out.print("\n" + cmdStr);
                        if (Lib.exe(cmdStr) == 0) {
                            System.out.println("\nChange virturl " + netName + " Ip to " + netAddr + " OK.");
                        }
                        else{
                            System.out.println("Change virturl " + netName + " Ip to " + netAddr + " Error !!! ");
                        }
                        
                    }    

                    
                    
                    
                } else {
                    System.out.println("Change " + netName + " Ip to " + netAddr + " Error !!! ");
                }
            }
            return errStr;
        }
        return errStr;
    }

    public void addTask(String[] strCmdA, int retryAmt, int retryDly) {
        CmdTask task1 = new CmdTask(strCmdA[0]);
        for (int i = 1; i < strCmdA.length; i++) {
            task1.paras[i - 1] = strCmdA[i];
        }
        task1.retryAmt = retryAmt;
        task1.retryDly = retryDly;
        cexe.addMap(task1);
    }

    public String cmdFunc(String cmdstr) {
        final ConsoleMain cla = this;
        String errStr = null;
        String cmd = "";
        if (cmdstr.equals("exit")) {
            System.exit(0);
            return errStr;
        }

        String[] strCmdA = cmdstr.split(" ");

        if (strCmdA[0].equals("txsskui")) {
            if (cmdstr.length() > 8) {
                //slotComm.serverSocket.txip("127.0.0.1", cmdstr.substring(8), GB.slotUiPort);
            }
            return errStr;

        }

        if (strCmdA[0].equals("txFileToSocket")) {
            //txFileToSocket ip port inputFile output file
            if (strCmdA.length == 1) {
                strCmdA = "txFileToSocket 127.0.0.1 23567 paraSet.json paraSetTmp.json".split(" ");
            }
            if (strCmdA.length == 5) {
                String content = Lib.fileToString(strCmdA[3]);
                if (content != null) {
                    try {
                        JSONObject jobj = new JSONObject();
                        jobj.put("act", "writeFile");
                        jobj.put("wFileName", strCmdA[4]);
                        jobj.put("content", content);
                        int port = Lib.str2int(strCmdA[2], -1);
                        if (port < 0) {
                            return "command format error !!!";
                        }
                        slotComm.serverSocket.thTxJsonStr(strCmdA[1], port, jobj.toString());
                    } catch (Exception ex) {
                    }
                }
            }
        }

        //=================================================
        cmd = "testResponse";
        //para0 = soltCnt,all when =-1
        if (strCmdA[0].equals(cmd)) {
            if (cexe.getMap(cmd) == null) {
                clrSlotActed();
                addTask(strCmdA, 1, 10);
                return null;
            }
            return "command is in process !!!";
        }

        cmd = "getExRecordNames";
        //para0 = soltCnt,all when =-1
        if (strCmdA[0].equals(cmd)) {
            if (cexe.getMap(cmd) == null) {
                clrSlotActed();
                addTask(strCmdA, 1, 10);
                return null;
            }
            return "command is in process !!!";
        }

        cmd = "getRecordFile";
        //para0 = soltCnt,all when =-1
        if (strCmdA[0].equals(cmd)) {
            if (cexe.getMap(cmd) == null) {
                clrSlotActed();
                addTask(strCmdA, 1, 10);
                return null;
            }
            return "command is in process !!!";
        }

        cmd = "getSlotInf";
        //para0 = soltCnt,all when =-1
        if (strCmdA[0].equals(cmd)) {
            if (cexe.getMap(cmd) == null) {
                clrSlotActed();
                addTask(strCmdA, 1, 10);
                return null;
            }
            return "command is in process !!!";
        }
        cmd = "icsShutDown";
        //para0 = soltCnt,all when =-1
        if (strCmdA[0].equals(cmd)) {
            if (cexe.getMap(cmd) == null) {
                clrSlotActed();
                addTask(strCmdA, 1, 10);
                return null;
            }
            return "command is in process !!!";
        }

        cmd = "icsRestart";
        //para0 = soltCnt,all when =-1
        if (strCmdA[0].equals(cmd)) {
            if (cexe.getMap(cmd) == null) {
                clrSlotActed();
                addTask(strCmdA, 1, 10);
                return null;
            }
            return "command is in process !!!";
        }

        cmd = "stopAsterisk";
        //para0 = soltCnt,all when =-1
        if (strCmdA[0].equals(cmd)) {
            if (cexe.getMap(cmd) == null) {
                clrSlotActed();
                addTask(strCmdA, 1, 10);
                return null;
            }
            return "command is in process !!!";
        }

        cmd = "startAsterisk";
        //para0 = soltCnt,all when =-1
        if (strCmdA[0].equals(cmd)) {
            if (cexe.getMap(cmd) == null) {
                clrSlotActed();
                addTask(strCmdA, 1, 10);
                return null;
            }
            return "command is in process !!!";
        }

        cmd = "reNewParaSet";
        //para0 = soltCnt,all when =-1
        if (strCmdA[0].equals(cmd)) {
            if (cexe.getMap(cmd) == null) {
                clrSlotActed();
                for (int i = 0; i < icsData.slotDatas.length; i++) {
                    if (icsData.slotDatas[i].type.equals(cla.nsta.type)) {
                        if (icsData.slotDatas[i].count == cla.nsta.count) {
                            icsData.slotDatas[i].acted_f = 1;     //self slot
                        }
                    }
                }
                if (strCmdA[2].equals("bypassActed")) {
                    for (int i = 0; i < icsData.slotDatas.length; i++) {
                        if (icsData.slotDatas[i].status != 2) {
                            icsData.slotDatas[i].acted_f = 1;
                        }
                    }
                }
                addTask(strCmdA, 1, 50);
                return null;
            }
            return "command is in process !!!";
        }

        cmd = "reNewExtensions";
        //para0 = soltCnt,all when =-1
        if (strCmdA[0].equals(cmd)) {
            if (cexe.getMap(cmd) == null) {
                clrSlotActed();
                for (int i = 0; i < icsData.slotDatas.length; i++) {
                    if (icsData.slotDatas[i].type.equals(cla.nsta.type)) {
                        if (icsData.slotDatas[i].count == cla.nsta.count) {
                            icsData.slotDatas[i].acted_f = 1;     //self slot
                        }
                    }
                }
                if (strCmdA[2].equals("bypassActed")) {
                    for (int i = 0; i < icsData.slotDatas.length; i++) {
                        if (icsData.slotDatas[i].status != 2) {
                            icsData.slotDatas[i].acted_f = 1;
                        }
                    }
                }
                addTask(strCmdA, 1, 50);
                return null;
            }
            return "command is in process !!!";
        }

        cmd = "upLoadFile";
        //para0 = soltCnt,all when =-1
        //para1 = from file name
        //para2 = to file name
        if (strCmdA[0].equals(cmd)) {
            if (strCmdA.length < 4) {
                return "format error !!! ex:upLoadFile slot formFileName toFileName";
            }
            if (cexe.getMap(cmd) == null) {
                clrSlotActed();
                addTask(strCmdA, 1, 50);
                return null;
            }
            return "command is in process !!!";
        }

        cmd = "readFile";
        //para0 = soltCnt,all when =-1
        //para1 = readFileName
        if (strCmdA[0].equals(cmd)) {
            if (strCmdA.length < 3) {
                return "format error !!! ex:upLoadFile slot formFileName toFileName";
            }
            if (cexe.getMap(cmd) == null) {
                clrSlotActed();
                addTask(strCmdA, 1, 50);
                return null;
            }
            return "command is in process !!!";
        }

        if (cmdstr.equals("bypassSystemSecurity")) {
            return errStr;
        }
        if (cmdstr.equals("clearSystemSecurity")) {
            return errStr;
        }

        return "Command Not Found !!!";
    }

}

class ConsoleMainTm1 extends TimerTask {

    String str;
    ConsoleMain cla;
    int getSlotInfTime = 0;
    int getSlotDataTime = 0;
    String preSlotStaStr = "";
    int sameSlotStaCnt = 0;
    int chkParaSetTime = 0;
    int exConnectCnt = 0;


    ConsoleMainTm1(ConsoleMain owner) {
        cla = owner;
    }

    @Override
    public void run() {

        exConnectCnt++;
        if(exConnectCnt==50){
            exConnectCnt=0;
            cla.icsData.exStatusMap.keySet();
            
            Set<String> iss = cla.icsData.exStatusMap.keySet();
            Object[] objA = iss.toArray();
            for (int i = 0; i < objA.length; i++) {
                if (objA[i] == null) {
                    continue;
                }
                String key = (String) objA[i];
                ExStatus exStatus = (ExStatus)cla.icsData.exStatusMap.get(key);
                exStatus.connectTime++;
                if(exStatus.connectTime>4){
                    cla.icsData.exStatusMap.remove(key);
                }

            }
            
            
            
            
            
        }
        
        
        cla.pcioConTime++;
        if(cla.pcioConTime==100){
            for(int i=0;i<14;i++){
                cla.icsData.slotDatas[i].type="";
                cla.icsData.slotDatas[i].count=0;
                cla.icsData.slotDatas[i].status=0;
            }    
        }
        
        
        if (cla.icsData.logStr.length() != 0) {
            Base3.log.info(cla.icsData.logStr);
            cla.icsData.logStr = "";
        }

        if (GB.ctrMast_f != 0) {
            SlotData[] slotDatas = cla.icsData.slotDatas;
            if (cla.cexe.taskMap.keySet().isEmpty()) {
                if (++getSlotDataTime >= 4) {
                    getSlotDataTime = 0;
                    for (int i = 0; i < slotDatas.length; i++) {
                        SlotData sd = slotDatas[i];
                        int get_f=0;
                        if(sd.type.equals("sip"))
                            get_f=1;
                        if(sd.type.equals("fxs"))
                            get_f=1;
                        if(sd.type.equals("mag"))
                            get_f=1;
                        
                        if (sd.exist_f == 1 && get_f==1) {
                            int txYes_f = 0;
                            if (cla.getSlotDataRxfs[i] == 1) {
                                cla.getSlotDataTimes[i]++;
                                if (cla.getSlotDataTimes[i] >= cla.getSlotDataDelays[i]) {
                                    txYes_f = 1;
                                }
                            } else {
                                cla.getSlotDataTimes[i]++;
                                if (cla.getSlotDataTimes[i] >= cla.getSlotDataWaits[i]) {
                                    txYes_f = 1;
                                }
                            }

                            if (txYes_f == 1) {
                                cla.getSlotDataTimes[i] = 0;
                                JSONObject json = new JSONObject();
                                try {
                                    json.put("act", "getSlotData");
                                    json.put("cmdInx", cla.cmdInx++);
                                    json.put("slotCnt", i);
                                    String tmpStr = json.toString();
                                    cla.getSlotDataRxfs[i] = 0;
                                    cla.slotComm.serverSocket.thTxJsonStr(sd.ip, sd.port, tmpStr);

                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }

                        }
                    }
                }
            }

            CmdTask task = cla.cexe.taskMap.get("reNewParaSet");
            if (task == null) {
                int slotReady_f = 0;
                for (int i = 0; i < slotDatas.length; i++) {
                    if (slotDatas[i].status == 2) {
                        slotReady_f = 1;
                    }
                }
                if (slotReady_f == 1) {
                    cla.cmdFunc("reNewParaSet -1 bypassActed");
                }
            }
        }

        /*
                sameSlotStaCnt++;
                if (sameSlotStaCnt == 3) {
                    int slotReady_f = 0;
                    for (int i = 0; i < slotDatas.length; i++) {
                        if (slotDatas[i].status == 2) {
                            slotReady_f = 1;
                        }
                    }
                    if (slotReady_f == 1) {
                        cla.cmdFunc("reNewParaSet -1 bypassActed");
                    }
                    return;
                }
                
                cla.owner.icsData.debugCnt++;
                String errStr = cla.cmdFunc("getSlotInf -1");
                if (errStr == null) {
                    String nowSlotStaStr = "";
                    for (int i = 0; i < slotDatas.length; i++) {
                        nowSlotStaStr += slotDatas[i].status;
                    }
                    if (!nowSlotStaStr.equals(preSlotStaStr)) {
                        preSlotStaStr = nowSlotStaStr;
                        sameSlotStaCnt = 0;
                    }
                }
         */
        try {
            cla.cexe.exeTaskMap();
        } catch (Exception ex) {
            ex.printStackTrace();
            //System.out.println(ex.toString());
        }
    }

}

class ConsoleMainCmdExe {

    ConsoleMain cla;
    Map<String, CmdTask> taskMap;

    ConsoleMainCmdExe(ConsoleMain owner, Map<String, CmdTask> _taskMap) {
        cla = owner;
        taskMap = _taskMap;
    }

    public void exeTaskMap() {
        try {
            Set<String> iss = taskMap.keySet();
            Object[] objA = iss.toArray();
            for (int i = 0; i < objA.length; i++) {
                if (objA[i] == null) {
                    continue;
                }
                String key = (String) objA[i];
                CmdTask task1 = taskMap.get(key);
                if (task1 == null) {
                    continue;
                }
                exeTask(task1);

            }

            /*
            for (String key : iss) {
                CmdTask task1 = taskMap.get(key);
                if (task1 == null) {
                    continue;
                }
                exeTask(task1);
            }
             */
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public CmdTask getMap(String name) {
        CmdTask task = taskMap.get(name);
        return task;
    }

    public void addMap(CmdTask task) {
        taskMap.put(task.name, task);
    }

    public int taskEnd(CmdTask task) {
        task.stepInx = 0;
        task.stepTim = 0;
        task.retryTim = 0;
        task.retryCnt += 1;
        if (task.retryAmt > 0) {
            if (task.retryCnt >= task.retryAmt) {
                taskMap.remove(task.name);
                return 1;
            }
        }
        return 0;
    }

    //if slotCnt < 0 commsnd slot all
    public int task_slotCommand(CmdTask task, int slotCnt, int slotStepDly, int waitResponseDly) {
        switch (task.stepInx) {
            case 0:
                task.stepDly = 1;
                task.stepInx++;
                if (slotCnt < 0) {
                    task.stepCnt = 0;
                } else {
                    task.stepCnt = slotCnt;
                }
                task.stepAmt = cla.icsData.slotDatas.length;
                if (task.retryCnt == 0) {
                    CmdStatus sta = new CmdStatus(task.name);
                    cla.cmdStaMap.put(task.name, sta);
                }
                cla.rxMap.clear();
            case 1:
                if (task.stepTim < task.stepDly) {
                    task.stepTim++;
                    return 0;
                }
                CmdStatus sta1 = cla.cmdStaMap.get(task.name);
                sta1.index++;
                try {
                    if (cla.icsData.slotDatas[task.stepCnt].exist_f == 1) {
                        if (cla.icsData.slotDatas[task.stepCnt].acted_f == 0) {
                            if (cla.icsData.slotDatas[task.stepCnt].status >= 1) {
                                String act = task.name;
                                cla.icsData.slotDatas[task.stepCnt].command = task.name;
                                String ip = cla.icsData.slotDatas[task.stepCnt].ip;
                                int port = cla.icsData.slotDatas[task.stepCnt].port;
                                if (cla.icsData.slotDatas[task.stepCnt].type.equals("ctr")) {
                                    port = cla.ctrCommPort;
                                }
                                ChkRxA chkRx1 = new ChkRxA(ip, act, cla.cmdInx);
                                task.jobj.put("act", act);
                                task.jobj.put("cmdInx", cla.cmdInx++);
                                String tmpStr = task.jobj.toString();
                                cla.slotComm.serverSocket.thTxJsonStr(ip, port, tmpStr);
                                chkRx1.slotCnt = task.stepCnt;
                                cla.rxMap.put(ip, chkRx1);
                            }
                        }
                    }
                } catch (Exception ex) {
                    System.out.println(ex.toString());
                }
                if (slotCnt < 0) {
                    task.stepCnt++;
                    if (task.stepCnt < task.stepAmt) {
                        task.stepTim = 0;
                        task.stepDly = slotStepDly;
                        return 0;
                    }
                }
                task.stepTim = 0;
                task.stepDly = waitResponseDly;
                task.stepInx++;
                return 0;
            case 2:
                int allResp_f = 1;
                for (String keyStr : cla.rxMap.keySet()) {
                    ChkRxA crx = cla.rxMap.get(keyStr);
                    if (crx.reti == 0) {
                        allResp_f = 0;
                        break;
                    }
                }
                if (allResp_f == 0) {
                    if (task.stepTim < task.stepDly) {
                        task.stepTim++;
                        return 0;
                    }
                }
                for (String keyStr : cla.rxMap.keySet()) {
                    ChkRxA crx = cla.rxMap.get(keyStr);
                    for (int i = 0; i < cla.icsData.slotDatas.length; i++) {
                        if (cla.icsData.slotDatas[i].ip.equals(keyStr)) {
                            if (crx.reti == 0) {
                                String errStr = cla.slotComm.serverSocket.thTxErrMap.get(keyStr);
                                if (errStr != null) {
                                    cla.icsData.slotDatas[i].inf = errStr;
                                } else {
                                    cla.icsData.slotDatas[i].inf = "No response !!!";
                                    //System.out.println(cla.slotStaA[i].info);
                                }
                            } else {
                                //System.out.println("Socket Connet OK.");
                                cla.icsData.slotDatas[i].acted_f = 1;
                            }
                        }
                    }
                }
                task.stepInx++;
                return 0;
            default:
                if (taskEnd(task) == 1) {
                    CmdStatus sta = cla.cmdStaMap.get(task.name);
                    sta.actStr = "command end ok";
                    sta.status = "ok";
                    return 2;
                } else {
                    System.out.println("retrying .....");
                }
                return 1;
        }
    }

    public int exeTask(CmdTask task) {
        String content = "";
        int slotCnt = 0;
        int endf = 0;
        if (task.retryTim < task.retryDly) {
            task.retryTim++;
            return 0;
        }
        switch (task.name) {
            case "reNewExtensions":
                int ii = 0;
                ii += 1;
            case "reNewParaSet":
                if (task.retryCnt == 0 && task.stepInx == 0) {
                    String fileFullName = GB.exePath + "/paraSet.json";
                    content = Lib.fileToString(fileFullName);
                    Lib.jsonPut(task.jobj, "content", content);
                }
                slotCnt = Lib.str2int(task.paras[0], -1);
                endf = task_slotCommand(task, slotCnt, 4, 100);
                if (endf == 2) {
                    System.out.println(cla.cmdStaMap.get(task.name).actStr);
                }
                for (String key : cla.rxMap.keySet()) {
                    ChkRxA rxObj = cla.rxMap.get(key);
                    if (rxObj.rxTrig_f == 1) {
                        rxObj.rxTrig_f = 0;
                        System.out.println(rxObj.jobj.toString());
                    }
                }
                return endf;

            case "upLoadFile":
                if (task.retryCnt == 0 && task.stepInx == 0) {
                    content = Lib.fileToString(task.paras[1]);
                    if (content == null) {
                        taskMap.remove(task.name);
                        System.out.println("read \"" + task.paras[1] + "\" file error !!!");
                        return 2;
                    }
                    Lib.jsonPut(task.jobj, "content", content);
                    Lib.jsonPut(task.jobj, "wFileName", task.paras[2]);
                }
                slotCnt = Lib.str2int(task.paras[0], -1);
                endf = task_slotCommand(task, slotCnt, 10, 100);
                if (endf == 2) {
                    System.out.println(cla.cmdStaMap.get(task.name).actStr);
                }
                for (String key : cla.rxMap.keySet()) {
                    ChkRxA rxObj = cla.rxMap.get(key);
                    if (rxObj.rxTrig_f == 1) {
                        rxObj.rxTrig_f = 0;
                        //System.out.println(rxObj.jobj.toString());
                    }
                }
                return endf;

            case "readFile":
                if (task.retryCnt == 0 && task.stepInx == 0) {
                    Lib.jsonPut(task.jobj, "rFileName", task.paras[1]);
                }
                slotCnt = Lib.str2int(task.paras[0], -1);
                endf = task_slotCommand(task, slotCnt, 10, 100);
                if (endf == 2) {
                    System.out.println(cla.cmdStaMap.get(task.name).actStr);
                }
                for (String key : cla.rxMap.keySet()) {
                    ChkRxA rxObj = cla.rxMap.get(key);
                    if (rxObj.rxTrig_f == 1) {
                        rxObj.rxTrig_f = 0;
                        //System.out.println(rxObj.jobj.toString());
                    }
                }
                return endf;
            case "getExRecordNames":
                slotCnt = Lib.str2int(task.paras[0], -1);
                if (task.retryCnt == 0 && task.stepInx == 0) {
                    Lib.jsonPut(task.jobj, "exNumber", task.paras[1]);
                }
                endf = task_slotCommand(task, slotCnt, 10, 100);
                if (endf == 2) {
                    System.out.println(cla.cmdStaMap.get(task.name).actStr);
                }
                for (String key : cla.rxMap.keySet()) {
                    ChkRxA rxObj = cla.rxMap.get(key);
                    if (rxObj.rxTrig_f == 1) {
                        rxObj.rxTrig_f = 0;
                        cla.owner.tickBackValue = rxObj.jobj;
                        //System.out.println(rxObj.jobj.toString());
                    }
                }
                return endf;

            case "getRecordFile":
                slotCnt = Lib.str2int(task.paras[0], -1);
                if (task.retryCnt == 0 && task.stepInx == 0) {
                    Lib.jsonPut(task.jobj, "fileName", task.paras[1]);
                }
                endf = task_slotCommand(task, slotCnt, 10, 100);
                if (endf == 2) {
                    System.out.println(cla.cmdStaMap.get(task.name).actStr);
                }
                for (String key : cla.rxMap.keySet()) {
                    ChkRxA rxObj = cla.rxMap.get(key);
                    if (rxObj.rxTrig_f == 1) {
                        rxObj.rxTrig_f = 0;
                        try {
                            int packageId;
                            packageId = (int) rxObj.jobj.get("readFilePackageId");
                            String fullFileName = rxObj.jobj.get("fileName").toString();
                            int lastSlash = fullFileName.lastIndexOf("/");
                            String fileName = fullFileName.substring(lastSlash + 1);
                            MyRxData rxd = cla.slotComm.serverSocket.myRxDataList.get(packageId);
                            byte[] bytes = cla.slotComm.serverSocket.inbuf;
                            String soundPath = GB.webRootPath + "/user-webIcs/record/";
                            FileOutputStream fs = new FileOutputStream(new File(soundPath + fileName));
                            BufferedOutputStream bs = new BufferedOutputStream(fs);
                            bs.write(bytes, rxd.offset, rxd.len);
                            bs.close();

                            String outFileName = fileName.split("\\.")[0] + ".mp3";
                            String exePath = GB.webRootPath + "/user-webIcs/record/";
                            String exeStr = "";
                            Process process = null;
                            if (GB.os_inx == 0) { //window
                                exeStr = "ffmpeg -y -i " + fileName + " -vn -ar 8000 -ac 1 -b:a 192k " + outFileName;
                                process = Runtime.getRuntime().exec(exePath + exeStr, null, new File(exePath));
                            } else { //linux
                                exeStr = "ffmpeg -y -i " + fileName + " -vn -ar 8000 -ac 1 -b:a 192k " + outFileName;
                                process = Runtime.getRuntime().exec(exeStr, null, new File(exePath));
                            }
                            process.waitFor();
                            rxObj.jobj.put("outFileName", outFileName);
                            rxObj.jobj.put("path", "./user-webIcs/record/");
                            cla.owner.tickBackValue = rxObj.jobj;

                            //System.out.println(rxObj.jobj.toString());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            return endf;
                        }

                    }
                }
                return endf;
            case "testResponse":
            case "getSlotInf":
            case "startAsterisk":
            case "stopAsterisk":
            case "icsRestart":
            case "icsShutDown":
                slotCnt = Lib.str2int(task.paras[0], -1);
                endf = task_slotCommand(task, slotCnt, 4, 100);
                if (endf == 2) {
                    System.out.println(cla.cmdStaMap.get(task.name).actStr);
                    if (task.name.equals("icsRestart")) {
                        try {
                            Runtime.getRuntime().exec(new String[]{"sudo", "reboot"});
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    if (task.name.equals("icsShutDown")) {
                        try {
                            Runtime.getRuntime().exec(new String[]{"sudo", "poweroff"});
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    if (task.name.equals("getSlotInf")) {
                        for (int i = 0; i < cla.icsData.slotDatas.length; i++) {
                            SlotData slotData = cla.icsData.slotDatas[i];
                            if (slotData != null) {
                                slotData.connectTime++;
                                if (slotData.connectTime >= 3) {
                                    if (slotData.type.equals("roip")) {
                                        continue;
                                    }
                                    if (slotData.type.equals("mag")) {
                                        continue;
                                    }
                                    if (slotData.type.equals("ctr")) {
                                        continue;
                                    }
                                    if (slotData.type.equals("record")) {
                                        continue;
                                    }
                                    slotData.type = "";
                                    slotData.count = 0;
                                }
                            }
                        }
                    }
                }
                for (String key : cla.rxMap.keySet()) {
                    ChkRxA rxObj = cla.rxMap.get(key);
                    if (rxObj.rxTrig_f == 1) {
                        rxObj.rxTrig_f = 0;
                        System.out.println(rxObj.jobj.toString());
                        try {
                            if (rxObj.act.equals("getSlotInf")) {
                                System.out.println("getSlotInf rx from slot " + rxObj.slotCnt);
                                SlotData slotData = cla.icsData.slotDatas[rxObj.slotCnt];
                                JSONObject slotInf = (JSONObject) rxObj.jobj.get("slotInf");
                                if (slotInf != null) {
                                    slotData.type = (String) slotInf.get("type");
                                    slotData.count = (int) slotInf.get("count");
                                    slotData.status = (int) slotInf.get("status");
                                    slotData.action = (String) slotInf.get("action");
                                    slotData.inf = (String) slotInf.get("inf");
                                    slotData.connectTime = 0;
                                }
                                slotData.exInf = (String) rxObj.jobj.get("exInf");
                                String[] strA = slotData.exInf.split("~");
                                for (int i = 0; i < strA.length; i++) {
                                    String[] strB = strA[i].split(",");
                                    if (strB.length != 3) {
                                        continue;
                                    }
                                    ExStatus st = new ExStatus(strB[0].trim());
                                    st.status = Lib.str2int(strB[1].trim(), 0);
                                    st.callWith = strB[2].trim();
                                    cla.icsData.exStatusMap.put(strB[0], st);
                                }

                            }
                        } catch (Exception ex) {

                        }

                    }
                }
                return endf;
            default:
                return 0;
        }
    }
}

//status = 0;//0:none(dark), 1:exist(y blink) ,2: ready(y), 3:paraSet loaded(green blink), 4:pbx run(g), 5:error(red)
class SlotData {

    int exist_f = 0;
    String type;
    int count = 0;
    int status = 0;
    String inf = "";
    String softVer = GB.version;
    String firmVer = "0.0";
    String startTime = GB.startTime;
    String action = "";
    int acted_f = 0;
    String ip = "";
    int port = 23500;
    String command = "";
    String exInf = "";
    int connectTime = 0;

    SlotData() {
        type = "";
        startTime = GB.startTime;
    }
}
