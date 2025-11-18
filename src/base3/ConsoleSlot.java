package base3;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import static java.lang.Double.isNaN;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
//asterisk sound location: usr/share/asterisk/sounds/en/

public class ConsoleSlot {

    static ConsoleSlot scla;
    String title_str = "title_str";
    int fullScr_f = 0;
    int winW = 1600;
    int winH = 800;
    int debug_f = 1;
    int cmdInx = 0;
    int sshPrint_f = 0;
    int cleanRegister_f = 1;
    Timer tm1 = null;
    PbxSet pbxSet;
    int slotCommPort = 23400;
    int slotIoPort = 23500;
    ConsoleSlotCmdExe cexe;
    Map<String, CmdTask> taskMap;
    Map<String, ChkRxA> rxMap;
    Map<String, CmdStatus> cmdStaMap;
    Map<String, ExStatus> exStaMap;
    Map<String, ExStatus> exStaMapTmp;
    Map<String, PbxChannel> pbxChannelMapTmp;
    ExStatus exStaTmp;
    NowSlotSta nsta = new NowSlotSta();
    Map<String, CmdObj> cmdObjMap;

    int exStatusFlag = 0;
    int pbxStatusTim = 0;
    int pbxStatusDly = 10;
    int pjsipShowAors_f = 0;
    int pjsipListEndpoints_f = 0;
    int pjsipShowEndpoints_f = 0;
    int dahdiShowChannel_f = 0;
    int coreShowChannels_f = 0;
    int pjsipAction_f = 0;
    int pjsipShowChannelStats_f = 1;
    int nstaStep = 0;
    KvComm uiComm;
    KvComm ioComm;
    String recordPath = "./record";

    int myDeviceId = 0x2403;
    int mySerialId = 0x0000;
    int devicePcioId = 0x2301;

    //String recordPath = "E:/kevin/myCode/webIcs/web/user-webIcs/record";
    //String recordPath = "D:/Kevin/myCode/pbxSet/record";
    //String recordPath = "E:/kevin/myCode/pbxSet/record";
    //String recordPath="D:/kevin/myCode/webIcs/web/user-webIcs/record";
    //===========================
    public ConsoleSlot() {
        ConsoleSlot.scla = this;
    }

    public void create() {
        String str;
        //=======================================================
        final ConsoleSlot cla = this;
        //nsta.ip = "192.168,200,200";//defalu ip
        //nsta.count = 1;
        //nsta.type = "sip";
        //nsta.status=3;
        //=======================================================
        pbxSet = new PbxSet();
        pbxSet.create();
        pbxSet.shellRx = new ShellRx() {
            @Override
            public void sshRx(String str) {
                if (str != null) {
                    //System.out.println("***BREAK***");
                    String[] strA = str.split("\n");
                    int inx = 0;
                    while (inx < strA.length) {
                        if (cla.sshPrint_f == 1) {
                            System.out.println(strA[inx]);
                        }
                        try {
                            inx = decShell(strA, inx);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    //System.out.print("***BREAK***");
                } else {
                    //System.out.print("***END***");
                }
            }
        };

        //=======================================
        uiComm = new KvComm("uiComm", "serverSocket", 65536 * 256);
        uiComm.serverSocket.format = 2;
        uiComm.serverSocket.rxcon_ltim = 100;
        uiComm.serverSocket.port = slotCommPort;
        uiComm.serverSocket.setCallBack(new BytesCallback() {
            @Override
            public String prg(byte[] bytes, int len) {
                cla.uiCommRx(bytes, len);
                return null;
            }
        });
        uiComm.open();

        //=======================================
        ioComm = new KvComm("ioComm", "serverSocket");
        ioComm.serverSocket.format = 1;
        ioComm.serverSocket.rxcon_ltim = 100;
        ioComm.serverSocket.port = slotIoPort;
        ioComm.serverSocket.stm.setCallBack(new BytesCallback() {
            @Override
            public String prg(byte[] bytes, int len) {
                gnRxPrg("", bytes, len);
                socketServerReturn();
                return null;

            }
        });
        ioComm.open();

        //=======================================================
        rxMap = new HashMap<String, ChkRxA>();
        taskMap = new HashMap<String, CmdTask>();
        cmdStaMap = new HashMap<String, CmdStatus>();
        cmdObjMap = new HashMap<String, CmdObj>();
        //exStaMap = new HashMap<String, ExStatus>();
        cexe = new ConsoleSlotCmdExe(cla, taskMap);
        //=======================================
        //CmdTask task1 = new CmdTask("test");
        //task1.retryAmt = 0;
        //cexe.addMap(task1);
        //=====================================
        if (cla.tm1 == null) {
            cla.tm1 = new Timer();
            tm1.schedule(new ConsoleSlotTm1(cla), 1000, 20);
        }
        //=====================================
        System.out.println("Console Slot Ready.");
        boolean commandInput_b = true;
        while (commandInput_b) {
            Scanner input = new Scanner(System.in);
            str = input.nextLine().trim();
            if (!str.equals("")) {
                cmdPrg(str);
            }
        }

    }

    public void socketServerReturn() {
        byte[] sockUartData_buf = new byte[64];
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

        sockUartData_buf[inx++] = (byte) ((exStatusFlag) & 255);
        sockUartData_buf[inx++] = (byte) ((exStatusFlag >> 8) & 255);
        sockUartData_buf[inx++] = (byte) ((exStatusFlag >> 16) & 255);
        sockUartData_buf[inx++] = (byte) ((exStatusFlag >> 24) & 255);

        if (!GB.paraSetMap.isEmpty()) {
            String magPhoneNumber = GB.paraSetMap.get("magPhoneCallNumber").toString();
            byte[] bts = magPhoneNumber.getBytes();
            int len = bts.length & 15;
            if (len != 0) {
                sockUartData_buf[inx++] = (byte) (0xab);
                sockUartData_buf[inx++] = (byte) (len);
                for (int i = 0; i < len; i++) {
                    sockUartData_buf[inx++] = bts[i];
                }
            }
        }

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

        /*
        if (sockUartCmd_tx_f == 1) {
            sockUartCmd_tx_f = 0;
            stm.tbuf[stx_index++] = (byte) (0x10);//uart0
            stm.tbuf[stx_index++] = (byte) (0x00);//flag
            stm.tbuf[stx_index++] = (byte) (sockUartCmd_len & 255);//len low byte
            stm.tbuf[stx_index++] = (byte) ((sockUartCmd_len >> 8) & 255);//len high byte
            for (int i = 0; i < sockUartCmd_len; i++) {
                stm.tbuf[stx_index++] = sockUartCmd_buf[i];
            }
            stm.tbuf_byte = stx_index;
            ioComm.serverSocket.txReturn();
            return;
        } else {
            int inx = 0;
            cla.sockUartData_buf[inx++] = (byte) (cla.fpgaDeviceId & 255);
            cla.sockUartData_buf[inx++] = (byte) ((cla.fpgaDeviceId >> 8) & 255);
            cla.sockUartData_buf[inx++] = (byte) (0x00);//serial id
            cla.sockUartData_buf[inx++] = (byte) (0x00);//serial id
            //=========================================================
            cla.sockUartData_buf[inx++] = (byte) (0x00);//groupId
            cla.sockUartData_buf[inx++] = (byte) (0x00);//flag
            cla.sockUartData_buf[inx++] = (byte) (0x0a);//len low
            cla.sockUartData_buf[inx++] = (byte) (0x00);//len high
            cla.sockUartData_buf[inx++] = (byte) (0x1000 & 255);//command low
            cla.sockUartData_buf[inx++] = (byte) (0x1000 >> 8);//command high
            cla.sockUartData_buf[inx++] = (byte) (0x10);//para0 low byte
            cla.sockUartData_buf[inx++] = (byte) (0x32);//para0 high byte
            cla.sockUartData_buf[inx++] = (byte) (0x54);//para1 low byte
            cla.sockUartData_buf[inx++] = (byte) (0x76);//para1 high byte
            cla.sockUartData_buf[inx++] = (byte) (0x98);//para2 low byte
            cla.sockUartData_buf[inx++] = (byte) (0xba);//para2 high byte
            cla.sockUartData_buf[inx++] = (byte) (0xdc);//para3 low byte
            cla.sockUartData_buf[inx++] = (byte) (0xfe);//para3 high byte
            cla.sockUartData_len = inx;
            sockUartData_tx_f = 1;

        }
         */
 /*
        int sockUartData_tx_f=0;
        int sockUartData_len=10;
        byte[] sockUartData_buf=new byte[]{1,2,3,4,5,6,7,8,9,10};
        if (sockUartData_tx_f == 1) {
            sockUartData_tx_f = 0;
            stm.tbuf[stx_index++] = (byte) (0x09);//uart0
            stm.tbuf[stx_index++] = (byte) (0x00);//flag
            stm.tbuf[stx_index++] = (byte) (sockUartData_len & 255);//len low byte
            stm.tbuf[stx_index++] = (byte) ((sockUartData_len >> 8) & 255);//len high byte
            for (int i = 0; i < sockUartData_len; i++) {
                stm.tbuf[stx_index++] = sockUartData_buf[i];
            }
            stm.tbuf_byte = stx_index;
            ioComm.serverSocket.txReturn();
            return;
        } else {
            stm.tbuf[stx_index++] = (byte) (0x00);//system
            stm.tbuf[stx_index++] = (byte) (0x00);//flag
            stm.tbuf[stx_index++] = (byte) (0x0002 & 255);//len low byte
            stm.tbuf[stx_index++] = (byte) ((0x0002 >> 8) & 255);//len high byte
            stm.tbuf[stx_index++] = (byte) (0x000e & 255);//no data
            stm.tbuf[stx_index++] = (byte) ((0x000e >> 8) & 255);//no data
            stm.tbuf_byte = stx_index;
            ioComm.serverSocket.txReturn();
                    
                    
                    
                    
                    
                    
            System.out.println("rxdata");
            return;
        }
         */
    }

    public void resetIp(String ipStr) {
        if (ipStr.equals("")) {
            return;
        }
        if (GB.set_ip_str.equals(ipStr)) {
            if (GB.real_ip_str.equals(ipStr)) {
                return;
            }
        }
        String cmdStr = "changeIp " + GB.netName + " " + ipStr;
        System.out.println("\n" + cmdStr);
        cmdPrg(cmdStr);
    }

    public String gnRxPrg(String name, byte[] bts, int len) {
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
                            GB.icsGroupId = (bts[ix + 6] & 255);
                            if (GB.icsGroupId == 0) {
                                GB.icsGroupId = 191;
                            }

                            int slotType = nsta.swFlag >> 4;
                            int slotCount = nsta.swFlag & 3;
                            int slotCnt = nsta.mcuFlag0 & 15;

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
                            if (!GB.slotType.equals(typeStr) || GB.slotCount != slotCount) {
                                GB.slotType = typeStr;
                                GB.slotCount = slotCount;
                                GB.chgSlotType();
                                String ipStr = GB.getSlotIp();
                                System.out.println("\nchangeIp to " + ipStr);
                                String[] strA=ipStr.split("\\.");
                                String ntpIp="192.168."+strA[2]+".99";
                                Lib.wrNtp(ntpIp);

                                resetIp(ipStr);
                            }
                            nsta.type = typeStr;
                            nsta.count = slotCount;
                            nsta.slotCnt = slotCnt;
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
                        for (int i = 0; i < 128; i++) {
                            nsta.allSlotSta[i] = bts[ix + i];
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
        String[] strB;
        String[] strC;
        inx++;
        if (nsta.action.equals("pjsipShowAors")) {
            if (strA[index].contains("Contact:  <Aor/ContactUri...")) {
                exStaMapTmp = new HashMap<String, ExStatus>();
                pjsipShowAors_f = 1;
                return inx;
            }
            if (pjsipShowAors_f == 1) {
                if (strA[index].contains(" Aor:  ")) {
                    strB = strA[index].trim().split("\\s+");
                    ExStatus est = new ExStatus(strB[1]);
                    est.status = 1;
                    exStaMapTmp.put(strB[1], est);
                    return inx;
                }
                if (strA[index].contains("    Contact:  ")) {
                    strB = strA[index].trim().split("\\s+");
                    strC = strB[1].split("/");
                    ExStatus est = new ExStatus(strC[0]);
                    est.status = 2;
                    exStaMapTmp.put(strC[0], est);
                    return inx;
                }
                if (strA[index].contains(GB.mainpbx_homePrompt)) {
                    pjsipShowAors_f = 0;
                    exStaMap = exStaMapTmp;
                    return inx;
                }

            }
            return inx;
        }

        if (nsta.action.equals("dahdiShowChannel")) {
    
            if (strA[index].contains("Channel:")) {
                strB = strA[index].trim().split("\\s+");
                int ch = Lib.str2int(strB[1], 0) - 1;
                int chAmt = 8;
                if (nsta.type.equals("mag")) {
                    ch = Lib.str2int(strB[1], 0) - 5;
                    chAmt = 4;
                }
                if (ch >= 0 && ch <= chAmt) {
                    dahdiShowChannel_f = 1;
                    if (exStaMapTmp == null) {
                        exStaMapTmp = new HashMap<String, ExStatus>();
                    }
                    exStaTmp = new ExStatus("");
                    exStaTmp.ch = ch;
                }
                return inx;
            }
            if (dahdiShowChannel_f == 1) {
                if (strA[index].contains("Dialing: yes")) {
                    exStaTmp.flag |= 1;
                    return inx;
                }
                if (strA[index].contains("Owner: DAHDI")) {
                    exStaTmp.flag |= 2;
                    return inx;
                }
                if (strA[index].contains("currently ON")) {
                    exStaTmp.flag |= 4;
                    return inx;
                }
                if (strA[index].contains("Caller ID:")) {
                    strB = strA[index].trim().split("\\s+");
                    exStaTmp.name = strB[2];
                    return inx;
                }
                if (strA[index].contains("Hookstate")) {
                    if (strA[index].contains("Offhook")) {
                        exStaTmp.flag |= 8;
                    }

                    if (exStaTmp.name.length() != 0) {
                        exStaTmp.status = 2;
                        if (exStaTmp.flag == 3)//local ring
                        {
                            exStaTmp.status = 5;
                        }
                        if (exStaTmp.flag == 6)//dialing
                        {
                            exStaTmp.status = 4;
                        }
                        if (exStaTmp.flag == 14) {
                            exStaTmp.status = 3;
                        }
                        if (exStaMapTmp != null) {
                            exStaMapTmp.put(exStaTmp.name, exStaTmp);
                        }
                        
                        exStaTmp = null;
                        pjsipAction_f = 0;
                        dahdiShowChannel_f = 0;
                        return inx;
                    }
                }
            }
        }

        if (nsta.action.equals("pjsipShowEndpoints")) {
            if (strA[index].contains("Endpoint:  <Endpoint/CID...")) {
                exStaMapTmp = new HashMap<String, ExStatus>();
                pjsipShowEndpoints_f = 1;
                exStaTmp = null;
                //System.out.println("\n"+"pjsipShowEndpoints");
                return inx;
            }
            if (pjsipShowEndpoints_f == 1) {
                if (strA[index].contains("Endpoint:")) {
                    strB = Lib.splitSeg(strA[index]);
                    if (strB.length < 3) {
                        return inx;
                    }
                    exStaTmp = new ExStatus(strB[1]);
                    if (strB[2].equals("Unavailable")) {
                        exStaTmp.status = 1;
                    }
                    if (strB[2].equals("Not in use")) {
                        exStaTmp.status = 2;
                        if (exStaTmp.name.equals("137")) {
                            int xxx = 1;
                        }
                    }
                    if (strB[2].equals("In use")) {

                        exStaTmp.status = 3;
                        //System.out.println("\n"+"pjsipShowEndpoints In use");
                    }
                    if (strB[2].equals("Ringing")) {
                        exStaTmp.status = 5;
                    }
                    exStaTmp.callWith = "";
                    return inx;
                }

                if (strA[index].contains("Contact:")) {
                    if (exStaTmp != null) {
                        if (exStaTmp.name.equals("137")) {
                            int xxx = 1;
                        }
                        if (exStaTmp.status == 1) {
                            exStaTmp.status = 2;
                        }
                    }
                    return inx;
                }
                if (strA[index].contains("Transport:")) {
                    if (exStaTmp == null) {
                        return inx;
                    }
                    if (exStaTmp.status < 3) {
                        exStaMapTmp.put(exStaTmp.name, exStaTmp);
                        exStaTmp = null;
                    }
                }
                if (strA[index].contains("Channel:")) {
                    if (exStaTmp == null) {
                        return inx;
                    }
                    strB = strA[index].trim().split("\\s+");
                    //strB = Lib.splitSeg(strA[index]);
                    if (strB.length < 3) {
                        return inx;
                    }
                    if (strB[2].equals("Ring")) {
                        exStaTmp.status = 4;
                    }
                    if (strB[2].equals("Ringing")) {
                        exStaTmp.status = 5;
                    }
                }

                if (strA[index].contains("Exten:")) {
                    if (exStaTmp == null) {
                        return inx;
                    }
                    strB = strA[index].trim().split("\\s+");
                    if (strB.length < 3) {
                        return inx;
                    }
                    String callWith;
                    callWith = Lib.getStrBetween(strA[index], "<", ">");
                    if (callWith == null) {
                        callWith = "";
                    }
                    String callFromName;
                    callFromName = Lib.getStrBetween(strA[index], "\"", "\"");
                    if (callFromName == null) {
                        callFromName = "";
                    }
                    String callToNo;
                    callToNo = Lib.getStrBetween(strA[index], "Exten:", "CLCID:");
                    if (callToNo == null) {
                        callToNo = "";
                    }
                    callToNo = callToNo.trim();
                    if (callWith.equals("")) {
                        callWith = callToNo;
                    }

                    if (callWith.contains("*0*")) {
                        callWith = callWith.substring(3);
                    }
                    exStaTmp.callWith = callWith;
                    exStaTmp.callFromName = callFromName;
                    exStaTmp.callToNo = callToNo;
                    exStaMapTmp.put(exStaTmp.name, exStaTmp);
                    exStaTmp = null;
                }

                if (strA[index].contains("Objects found:")) {
                    pjsipShowEndpoints_f = 0;
                    if (exStaMapTmp != null) {
                        exStaMap = exStaMapTmp;
                        exStaMapTmp = null;
                        pjsipAction_f = 0;
                    }
                    return inx;
                }
            }
            return inx;
        }

        if (nsta.action.equals("coreShowChannels")) {
            if (strA[index].contains("Channel")) {
                pbxChannelMapTmp = new HashMap<String, PbxChannel>();
                coreShowChannels_f = 1;
                exStaTmp = null;
                return inx;
            }
            if (coreShowChannels_f == 1) {
                strB = Lib.splitSeg(strA[index]);
                if (strB.length != 4) {
                    return inx;
                }
                strC = strB[0].split("-");
                if (strC.length != 2) {
                    return inx;
                }

                if (strA[index].contains("Endpoint:")) {
                    strB = Lib.splitSeg(strA[index]);
                    if (strB.length < 3) {
                        return inx;
                    }
                    exStaTmp = new ExStatus(strB[1]);
                    if (strB[2].equals("Unavailable")) {
                        exStaTmp.status = 1;
                    }
                    if (strB[2].equals("Not in use")) {
                        exStaTmp.status = 2;
                    }
                    if (strB[2].equals("In use")) {
                        exStaTmp.status = 3;
                    }
                    if (strB[2].equals("Ringing")) {
                        exStaTmp.status = 5;
                    }
                    exStaTmp.callWith = "";
                    return inx;
                }

                if (strA[index].contains("Transport:")) {
                    if (exStaTmp == null) {
                        return inx;
                    }
                    if (exStaTmp.status < 3) {
                        exStaMapTmp.put(exStaTmp.name, exStaTmp);
                        exStaTmp = null;
                    }
                }
                if (strA[index].contains("Channel:")) {
                    if (exStaTmp == null) {
                        return inx;
                    }
                    strB = strA[index].trim().split("\\s+");
                    //strB = Lib.splitSeg(strA[index]);
                    if (strB.length < 3) {
                        return inx;
                    }
                    if (strB[2].equals("Ring")) {
                        exStaTmp.status = 4;
                    }
                    if (strB[2].equals("Ringing")) {
                        exStaTmp.status = 5;
                    }
                }

                if (strA[index].contains("Exten:")) {
                    if (exStaTmp == null) {
                        return inx;
                    }
                    strB = strA[index].trim().split("\\s+");
                    if (strB.length < 3) {
                        return inx;
                    }
                    String callWith;
                    callWith = Lib.getStrBetween(strA[index], "<", ">");
                    if (callWith == null) {
                        callWith = "";
                    } else {
                        if (callWith.equals("")) {
                            callWith = strB[1];
                        }

                    }
                    if (callWith.contains("*0*")) {
                        callWith = callWith.substring(3);
                    }
                    exStaTmp.callWith = callWith;
                    exStaMapTmp.put(exStaTmp.name, exStaTmp);
                    exStaTmp = null;
                }

                if (strA[index].contains("Objects found:")) {
                    pjsipShowEndpoints_f = 0;
                    if (exStaMapTmp != null) {
                        exStaMap = exStaMapTmp;
                        exStaMapTmp = null;
                        pjsipAction_f = 0;
                    }
                    return inx;
                }
            }
            return inx;
        }

        if (nsta.action.equals("pjsipListEndpoints")) {
            if (strA[index].contains("Endpoint:  <Endpoint/CID...")) {
                exStaMapTmp = new HashMap<String, ExStatus>();
                pjsipListEndpoints_f = 1;
                return inx;
            }
            if (pjsipListEndpoints_f == 1) {
                if (strA[index].contains("Endpoint:")) {
                    strB = Lib.splitSeg(strA[index]);
                    ExStatus est = new ExStatus(strB[1]);
                    est.status = 1;
                    if (strB[2].equals("Not in Use")) {
                        est.status = 2;
                    }
                    if (strB[2].equals("In use")) {
                        est.status = 3;
                    }
                    est.callWith = "";
                    exStaMapTmp.put(strB[1], est);
                    return inx;
                }
                if (strA[index].contains("Objects found:")) {
                    pjsipListEndpoints_f = 0;
                    exStaMap = exStaMapTmp;
                    return inx;
                }

            }
            return inx;
        }

        if (nsta.action.equals("pjsipShowChannelStats")) {
            if (exStaMap == null) {
                return inx;
            }
            if (strA[index].contains("No objects found.")) {
                for (String keyStr : exStaMap.keySet()) {
                    ExStatus ex = exStaMap.get(keyStr);
                    if (ex.status >= 3) {
                        ex.status = 2;
                    }
                }
                return inx;
            }
            if (strA[index].contains("BridgeId ChannelId")) {
                pjsipShowChannelStats_f = 1;
                return inx;
            }
            if (pjsipShowChannelStats_f == 1) {
                if (strA[index].contains(GB.mainpbx_homePrompt)) {
                    pjsipShowChannelStats_f = 0;
                    return inx;
                }
                if (strA[index].contains("not valid")) {
                    strB = strA[index].trim().split("\\s+");
                    strC = strB[0].split("-");
                    strB = strC[0].split("/");
                    ExStatus ex = exStaMap.get(strB[1]);
                    if (ex != null) {
                        ex.status = 3;
                    }
                    return inx;
                }

                strB = strA[index].trim().split("\\s+");
                if (strB.length == 13) {
                    strC = strB[1].split("-");
                    ExStatus ex = exStaMap.get(strC[0]);
                    if (ex != null) {
                        ex.status = 4;
                    }
                }
                return inx;
            }

            return inx;
        }

        if (strA[index].contains(GB.mainpbx_homePrompt)) {
            if (nsta.status <= 1) {
                nsta.status = 2;
                System.out.println("<ShellInf> linux is ready.");
            }
            if (nsta.action.equals("dialplanReload")) {
                nsta.action = "";
                pbxStatusTim = 0;
                nsta.status = 4;
                System.out.println("<ShellInf> dialplanReload ok");
            }

            if (nsta.action.equals("pjsipReload")) {
                nsta.action = "";
                pbxStatusTim = 0;
                nsta.status = 4;
                System.out.println("<ShellInf> pjsipReload ok");
            }

            if (nsta.action.equals("startAsterisk")) {
                nsta.action = "";
                nsta.asteriskSta = 1;
                nsta.status = 4;
                pbxStatusTim = 0;
                System.out.println("<ShellInf> asterisk start....");
            }
            if (nsta.action.equals("stopAsterisk")) {
                nsta.action = "";
                nsta.asteriskSta = 0;
                System.out.println("<ShellInf> asterisk stopped.");
            }

            return inx;

        }
        return inx;
    }

    //changeIp netName ip
    //ex: changeIp eno1 192.168.191.5
    public String cmdPrg(String cmdstr) {
        final ConsoleSlot cla = this;
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
            System.out.print("\nChange " + strCmdA[1] + " Ip to " + strCmdA[2]);
            if (GB.os_inx == 0) {
                String winCmds = "netsh interface ip set address name=" + strCmdA[1] + " source=static addr=" + strCmdA[2];
                Process pp;
                try {
                    //pp = java.lang.Runtime.getRuntime().exec(winCmds);
                    //pp.waitFor();
                    //System.out.print(pp);
                } catch (Exception ex) {
                    Logger.getLogger(ConsoleSlot.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                String cmdStr = "sudo /usr/sbin/ifconfig " + strCmdA[1] + " " + strCmdA[2] + " netmask " + GB.maskStr;
                System.out.print("\n" + cmdStr);
                //Lib.wrInterfaces(strCmdA[1], strCmdA[2], GB.maskStr, GB.gatewayStr);
                if (Lib.exe(cmdStr) == 0) {
                    System.out.print("\nChange " + strCmdA[1] + " Ip to " + strCmdA[2] + " OK.");
                    GB.real_ip_str = strCmdA[2];
                    String[] strA = GB.real_ip_str.split("\\.");
                    for (int i = 0; i < 4; i++) {
                        GB.realIp[i] = (byte) (Lib.str2int(strA[i], 0) & 255);
                    }
                } else {
                    System.out.print("\nChange " + strCmdA[1] + " Ip to " + strCmdA[2] + " Error !!! ");
                }
            }
            return errStr;

        }

        if (strCmdA[0].equals("runBat")) {
            try {
                System.out.println("run start");
                ProcessBuilder processBuilder = new ProcessBuilder("./" + strCmdA[1]);
                Process process = processBuilder.start();
                int exitVal = process.waitFor();
                System.out.println("run end");
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
            return errStr;
        }

        if (strCmdA[0].equals("copyConf")) {
            try {
                if (GB.prgMode == 3) {
                    String sPath = "";
                    if (GB.slotType.equals("fxo")) {
                        sPath = "./extensions/fxoExten";
                    }
                    if (GB.slotType.equals("sip")) {
                        sPath = "./extensions/sipExten";
                    }
                    if (GB.slotType.equals("fxs")) {
                        sPath = "./extensions/fxsExten";
                    }
                    if (GB.slotType.equals("mag")) {
                        sPath = "./extensions/magExten";
                    }
                    if (GB.slotType.equals("t1s")) {
                        sPath = "./extensions/t1sExten";
                    }
                    String dPath = "./testDir";
                    if (GB.prgMode == 3) {
                        dPath = "/etc/asterisk";
                    }
                    ArrayList<String> astr = Lib.readFileNames(sPath, "*.conf".split("~"));
                    System.out.println("copy file");
                    for (int i = 0; i < astr.size(); i++) {
                        System.out.println(astr.get(i));
                        File sourceFile = new File(sPath + "/" + astr.get(i));
                        File destFile = new File(dPath + "/" + astr.get(i));
                        Lib.copyFileUsingStream(sourceFile, destFile);
                        //Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        //Files.copy(sourceFile.toPath(), destFile.toPath(),StandardCopyOption.REPLACE_EXISTING);

                    }
                    System.out.println("copy file end");
                    return errStr;
                }

                System.out.println("copy file");
                //String winCmds = "netsh interface ip set address name=區域連線 source=static addr=" + strCmdA[1];
                String hostName = GB.mainpbx_hostName;
                String url = GB.mainpbx_ip;
                String password = GB.mainpbx_password;
                String destPath = "/etc/asterisk";
                String winCmds = "pscp -p -pw " + password + " ./asteriskConfPath/*.conf " + hostName + "@" + url + ":" + destPath;
                Process pp = java.lang.Runtime.getRuntime().exec(winCmds);
                //int exitVal = pp.waitFor();
                System.out.println("copy file end");
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
            return errStr;
        }

        if (strCmdA[0].equals("stopAsterisk")) {
            System.out.println("stopAsterisk");
            nsta.action = "stopAsterisk";
            nsta.actionTim = 50;
            pbxSet.sshWriteShl("sudo asterisk -rx \"core stop now\"\n");
            return errStr;
        }

        if (strCmdA[0].equals("cleanRegister")) {
            System.out.println("cleanRegister");
            nsta.action = "cleanRegister";
            nsta.actionTim = 50;
            pbxSet.sshWriteShl("sudo asterisk -rx \"database deltree registrar/contact\"\n");
            return errStr;
        }

        if (strCmdA[0].equals("startAsterisk")) {
            System.out.println("startAsterisk");
            nsta.action = "startAsterisk";
            nsta.actionTim = 1000;
            pbxSet.sshWriteShl("sudo systemctl start asterisk\n");
            return errStr;
        }

        if (strCmdA[0].equals("icsShutDown")) {
            System.out.println("icsShutDown");
            nsta.action = "icsShutDown";
            nsta.actionTim = 1000;
            try {
                Runtime.getRuntime().exec(new String[]{"sudo", "poweroff"});
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return errStr;
        }

        if (strCmdA[0].equals("icsRestart")) {
            System.out.println("sudo reboot");
            nsta.action = "icsRestart";
            nsta.actionTim = 1000;
            try {
                Runtime.getRuntime().exec(new String[]{"sudo", "reboot"});
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return errStr;
        }

        if (strCmdA[0].equals("dialplanReload")) {
            System.out.println("dialplanReload");
            nsta.action = "dialplanReload";
            nsta.actionTim = 100;
            pbxSet.sshWriteShl("sudo asterisk -rx \"dialplan reload\"\n");
            return errStr;
        }

        if (strCmdA[0].equals("pjsipReload")) {
            System.out.println("pjsipReload");
            nsta.action = "pjsipReload";
            nsta.actionTim = 100;
            pbxSet.sshWriteShl("sudo asterisk -rx \"pjsip reload\"\n");
            return errStr;
        }

        if (strCmdA[0].equals("pjsipShowEndpoints")) {
            nsta.action = "pjsipShowEndpoints";
            nsta.actionTim = 50;
            pbxSet.sshWriteShl("sudo asterisk -rx \"pjsip show endpoints\"\n");
            return errStr;
        }

        if (strCmdA[0].equals("pjsipListEndpoints")) {
            nsta.action = "pjsipListEndpoints";
            nsta.actionTim = 50;
            pbxSet.sshWriteShl("sudo asterisk -rx \"pjsip list endpoints\"\n");
            return errStr;
        }
        if (strCmdA[0].equals("pjsipShowAors")) {
            nsta.action = "pjsipShowAors";
            nsta.actionTim = 50;
            pbxSet.sshWriteShl("sudo asterisk -rx \"pjsip show aors\"\n");
            return errStr;
        }
        if (strCmdA[0].equals("pjsipShowChannelStats")) {
            nsta.action = "pjsipShowChannelStats";
            nsta.actionTim = 50;
            pbxSet.sshWriteShl("sudo asterisk -rx \"pjsip show channelstats\"\n");
            return errStr;
        }

        if (strCmdA[0].equals("transParaSet")) {
            if (strCmdA.length != 3) {
                return "Format error !!!";
            }
            CmdObj cobj;
            cobj = new CmdObj("pbxSet", "wconf " + strCmdA[1] + " " + strCmdA[2], 0);
            cla.cmdObjMap.put("pbxSet~wconf", cobj);

            if (cleanRegister_f == 0) {
                cleanRegister_f = 1;
                cobj = new CmdObj("cla", "cleanRegister", cobj.delay + 50);
                cla.cmdObjMap.put("cla~cleanRegister", cobj);
            }

            cobj = new CmdObj("cla", "stopAsterisk", cobj.delay + 100);
            cla.cmdObjMap.put("cla~stopAsterisk", cobj);

            cobj = new CmdObj("cla", "copyConf", cobj.delay + 150);
            cla.cmdObjMap.put("cla~copyConf", cobj);

            cobj = new CmdObj("cla", "startAsterisk", cobj.delay + 200);
            cla.cmdObjMap.put("cla~startAsterisk", cobj);

            System.out.println("\ntransParaSet start....");
            return errStr;
        }

        if (strCmdA[0].equals("transExtensions")) {
            if (strCmdA.length != 3) {
                return "Format error !!!";
            }
            CmdObj cobj;
            cobj = new CmdObj("pbxSet", "wconf " + strCmdA[1] + " " + strCmdA[2], 0);
            cla.cmdObjMap.put("pbxSet~wconf", cobj);

            //cobj = new CmdObj("cla", "stopAsterisk", cobj.delay + 50);
            //cla.cmdObjMap.put("cla~stopAsterisk", cobj);
            cobj = new CmdObj("cla", "copyConf", cobj.delay + 50);
            cla.cmdObjMap.put("cla~copyConf", cobj);

            cobj = new CmdObj("cla", "pjsipReload", cobj.delay + 100);
            cla.cmdObjMap.put("cla~pjsipReload", cobj);

            cobj = new CmdObj("cla", "dialplanReload", cobj.delay + 150);
            cla.cmdObjMap.put("cla~dialplanReload", cobj);

            System.out.println("\ntransExtensions start....");
            return errStr;
        }

        if (strCmdA[0].equals("wconf")) {
            errStr = pbxSet.cmdPrg(cmdstr);
            if (errStr != null) {
                System.out.println(errStr);
            }
            return errStr;
        }

        if (strCmdA[0].equals("readFile")) {
            try {
                content = Lib.readFile("paraSet.json");
            } catch (Exception ex) {

            }

            return errStr;
        }

        if (cmdstr.equals("bypassSystemSecurity")) {
            return errStr;
        }
        if (cmdstr.equals("clearSystemSecurity")) {
            return errStr;
        }
        System.out.println("<SysInf> no this command !!!");
        return "Command Not Found !!!";

    }

    void sskioRx(byte[] bts, int len) {
    }

    void uiCommRx(byte[] bytes, int len) {
        ConsoleSlot cla = this;
        FileWriter fw;
        String jstr = "";
        String contentStr;
        String retStr;
        for (int i = 0; i < cla.uiComm.serverSocket.myRxDataList.size(); i++) {
            MyRxData mrd = cla.uiComm.serverSocket.myRxDataList.get(i);
            try {
                if (mrd.format == 2) {
                    continue;
                }
                if (mrd.format == 0) {
                    jstr = new String(bytes, mrd.offset, mrd.len, "UTF-8");
                    JSONObject jobj = new JSONObject(jstr);
                    String cmdStr = jobj.get("act").toString();
                    int cmdInx = (int) jobj.get("cmdInx");

                    if (cmdStr.equals("testResponse")) {
                        retStr = Lib.actResponse(cmdStr, "ok", cmdInx);
                        cla.uiComm.serverSocket.txPackRetJsonStr(retStr);
                        return;
                    }
                    if (cmdStr.equals("getExRecordNames")) {
                        JSONObject jout = new JSONObject();
                        jout.put("act", "actResponse");
                        jout.put("actName", cmdStr);
                        jout.put("status", "ok");
                        jout.put("cmdInx", cmdInx);
                        jout.put("reti", 1);
                        String exNumber = jobj.get("exNumber").toString();
                        String path = cla.recordPath;
                        jout.put("path", path);
                        String[] strA = "*.gsm".split(",");
                        ArrayList<String> astr = Lib.readFileNames(path, strA);
                        ArrayList<String> bstr = new ArrayList();
                        for (int j = 0; j < astr.size(); j++) {
                            String fileName = astr.get(j);
                            String[] strB = fileName.split("_");
                            if (strB.length != 4) {
                                continue;
                            }
                            if (strB[2].equals(exNumber)) {
                                File file = new File(path + "/" + fileName);
                                long fsize = file.length();
                                if (fsize < 1000) {
                                    continue;
                                }
                                bstr.add(fileName);
                            }
                        }
                        String nstr = Lib.stringListToString(bstr);

                        /*
                        String path = cla.recordPath + "/" + exNumber;
                        jout.put("path", path);
                        String[] strA = "*.gsm".split(",");
                        ArrayList<String> astr = Lib.readFileNames(path, strA);
                        String nstr = Lib.stringListToString(astr);
                         */
                        jout.put("fileNames", nstr);
                        jout.put("ip", GB.real_ip_str);
                        cla.uiComm.serverSocket.txPackRetJsonStr(jout.toString());
                        System.out.println("\n" + cmdStr);
                        return;

                    }

                    if (cmdStr.equals("getRecordFile")) {
                        String fileName = jobj.get("fileName").toString();
                        JSONObject jout = new JSONObject();
                        jout.put("act", "actResponse");
                        jout.put("actName", cmdStr);
                        jout.put("status", "ok");
                        jout.put("cmdInx", cmdInx);
                        jout.put("reti", 1);
                        jout.put("fileName", fileName);
                        jout.put("readFilePackageId", 1);
                        //=====================================
                        TrxData txData = new TrxData(2);
                        txData.formats[0] = 0;
                        txData.packageIds[0] = 0;
                        txData.datas[0] = jout.toString().getBytes();
                        File file = new File(fileName);  // assume args[0] is the path to file
                        txData.formats[1] = 2;
                        txData.packageIds[1] = 1;
                        txData.datas[1] = Files.readAllBytes(Paths.get(fileName));
                        cla.uiComm.serverSocket.txPackRet(txData);
                        System.out.println("\n" + cmdStr);
                        return;

                    }

                    if (cmdStr.equals("icsShutDown")) {
                        retStr = Lib.actResponse(cmdStr, "ok", cmdInx);
                        cla.uiComm.serverSocket.txPackRetJsonStr(retStr);
                        cla.cmdPrg("icsShutDown");
                        return;
                    }

                    if (cmdStr.equals("startAsterisk")) {
                        retStr = Lib.actResponse(cmdStr, "ok", cmdInx);
                        cla.uiComm.serverSocket.txPackRetJsonStr(retStr);
                        cla.cmdPrg("startAsterisk");
                        return;
                    }

                    if (cmdStr.equals("icsRestart")) {
                        retStr = Lib.actResponse(cmdStr, "ok", cmdInx);
                        cla.uiComm.serverSocket.txPackRetJsonStr(retStr);
                        cla.cmdPrg("icsRestart");
                        return;
                    }

                    if (cmdStr.equals("icsShutDown")) {
                        retStr = Lib.actResponse(cmdStr, "ok", cmdInx);
                        cla.uiComm.serverSocket.txPackRetJsonStr(retStr);
                        cla.cmdPrg("icsShutDown");
                        return;
                    }

                    if (cmdStr.equals("stopAsterisk")) {
                        retStr = Lib.actResponse(cmdStr, "ok", cmdInx);
                        cla.uiComm.serverSocket.txPackRetJsonStr(retStr);
                        cla.cmdPrg("stopAsterisk");
                        return;
                    }
                    if (cmdStr.equals("getSlotInf") || cmdStr.equals("getSlotData")) {
                        JSONObject jout = new JSONObject();
                        jout.put("act", "actResponse");
                        jout.put("actName", cmdStr);
                        jout.put("status", "ok");
                        jout.put("cmdInx", cmdInx);
                        jout.put("reti", 1);
                        jout.put("slotInf", nsta.getJson());
                        jout.put("slotCnt", nsta.slotCnt);

                        String str = "";
                        if (exStaMap != null) {
                            for (String keyStr : exStaMap.keySet()) {
                                ExStatus st = exStaMap.get(keyStr);
                                if (!str.equals("")) {
                                    str += "~";
                                }
                                str += keyStr + "," + st.status + "," + st.callWith + " ";
                            }
                            jout.put("exInf", str);
                        }
                        cla.uiComm.serverSocket.txPackRetJsonStr(jout.toString());
                        return;

                    }

                    if (cmdStr.equals("reNewParaSet")) {
                        retStr = Lib.actResponse(cmdStr, "ok", cmdInx);
                        cla.uiComm.serverSocket.txPackRetJsonStr(retStr);

                        contentStr = jobj.get("content").toString();
                        String wFileName = "paraSet.json";
                        String content = jobj.get("content").toString();
                        fw = new FileWriter(wFileName);
                        fw.write(content);
                        fw.flush();
                        fw.close();
                        System.out.println("\n" + "save paraSet.json");
                        cla.nsta.status = 3;
                        if (cla.nsta.type.equals("rec")) {
                            cla.nsta.status = 4;
                            return;
                        }
                        cla.cmdPrg("transParaSet " + nsta.type + " " + nsta.count);

                        //pbxSet.cmdPrg("wconf " + nsta.type + " " + nsta.count);
                        //pbxSet.cmdPrg("copyConf");
                        //System.out.println("<SlotRxUi>reNewParaSet");
                        //cla.cmdPrg("startAsterisk");
                        return;
                    }

                    if (cmdStr.equals("reNewExtensions")) {
                        retStr = Lib.actResponse(cmdStr, "ok", cmdInx);
                        cla.uiComm.serverSocket.txPackRetJsonStr(retStr);

                        contentStr = jobj.get("content").toString();
                        String wFileName = "paraSet.json";
                        String content = jobj.get("content").toString();
                        fw = new FileWriter(wFileName);
                        fw.write(content);
                        fw.flush();
                        fw.close();
                        System.out.println("\n" + "save paraSet.json");
                        cla.nsta.status = 3;
                        if (cla.nsta.type.equals("rec")) {
                            cla.nsta.status = 4;
                            return;
                        }
                        cla.cmdPrg("transExtensions " + nsta.type + " " + nsta.count);

                        //pbxSet.cmdPrg("wconf " + nsta.type + " " + nsta.count);
                        //pbxSet.cmdPrg("copyConf");
                        //System.out.println("<SlotRxUi>reNewParaSet");
                        //cla.cmdPrg("startAsterisk");
                        return;
                    }

                    if (cmdStr.equals("upLoadFile")) {
                        String wFileName = jobj.get("wFileName").toString();
                        String content = jobj.get("content").toString();
                        fw = new FileWriter(wFileName);
                        fw.write(content);
                        fw.flush();
                        fw.close();
                        retStr = Lib.actResponse(cmdStr, "ok", cmdInx);
                        cla.uiComm.serverSocket.txPackRetJsonStr(retStr);
                        System.out.println("\n" + cmdStr);
                        return;
                    }

                    if (cmdStr.equals("readFile")) {
                        String rFileName = jobj.get("rFileName").toString();
                        String content = Lib.fileToString(rFileName);
                        if (content == null) {
                            retStr = Lib.actResponse(cmdStr, "read file error !!!", cmdInx);
                            cla.uiComm.serverSocket.txPackRetJsonStr(retStr);
                            return;
                        }
                        JSONObject jout = new JSONObject();
                        jout.put("act", "actResponse");
                        jout.put("actName", cmdStr);
                        jout.put("status", "ok");
                        jout.put("cmdInx", cmdInx);
                        jout.put("reti", 1);
                        jout.put("rFileName", rFileName);
                        jout.put("content", content);
                        cla.uiComm.serverSocket.txPackRetJsonStr(jout.toString());
                        System.out.println("\n" + cmdStr);
                        return;
                    }
                }
            } catch (Exception ex) {
                System.out.println(jstr);
                System.out.println(ex.toString());

            }
        }

    }
}
//20ms

class ConsoleSlotTm1 extends TimerTask {

    String str;
    ConsoleSlot cla;

    ConsoleSlotTm1(ConsoleSlot owner) {
        cla = owner;
    }

    void cmdPrg() {
        for (String key : cla.cmdObjMap.keySet()) {
            CmdObj cobj = cla.cmdObjMap.get(key);
            cobj.time++;
            if (cobj.time >= cobj.delay) {
                if (cobj.className.equals("pbxSet")) {
                    cla.pbxSet.cmdPrg(cobj.cmdName);
                } else {
                    cla.cmdPrg(cobj.cmdName);
                }
                cla.cmdObjMap.remove(key);
                return;
            }
        }
    }

    @Override
    public void run() {
        int i;
        i = 10;
        cmdPrg();
        if (cla.nsta.actionTim > 0) {
            cla.nsta.actionTim--;
            if (cla.nsta.actionTim == 0) {
                cla.nsta.action = "";
            }
        }
        cla.cexe.exeTaskMap();
        if (cla.pjsipAction_f == 1) {
            cla.pbxStatusTim++;
        } else {
            cla.pbxStatusTim = cla.pbxStatusDly;
        }
        if (cla.pbxStatusTim >= cla.pbxStatusDly) {
            if (cla.nsta.action.equals("")) {
                cla.pbxStatusTim = 0;
                if (cla.nsta.asteriskSta == 1) {
                    if (cla.nsta.type.equals("sip")) {
                        if (++cla.nstaStep >= 1) {
                            cla.nstaStep = 0;
                        }
                        if (cla.nstaStep == 0) {
                            cla.nsta.action = "pjsipShowEndpoints";
                            cla.nsta.actionTim = 50;
                            cla.pbxSet.sshWriteShl("sudo asterisk -rx \"pjsip show endpoints\"\n");
                            cla.pjsipAction_f = 1;
                        }
                        /*
                    if (cla.nstaStep == 1) {
                        cla.nsta.action = "pjsipListEndpoints";
                        cla.nsta.actionTim = 50;
                        cla.pbxSet.sshWriteShl("sudo asterisk -rx \"pjsip list endpoints\"\n");
                    }
                    if (cla.nstaStep == 2) {
                        cla.nsta.action = "pjsipShowAors";
                        cla.nsta.actionTim = 50;
                        cla.pbxSet.sshWriteShl("sudo asterisk -rx \"pjsip show aors\"\n");
                    }
                    if (cla.nstaStep == 3) {
                        cla.nsta.action = "pjsipShowChannelStats";
                        cla.nsta.actionTim = 50;
                        cla.pbxSet.sshWriteShl("sudo asterisk -rx \"pjsip show channelstats\"\n");
                    }
                         */

                    }

                    if (cla.nsta.type.equals("mag")) {
                        cla.exStatusFlag = 0;
                        if (cla.exStaMapTmp != null) {
                            Set<String> keySet = cla.exStaMapTmp.keySet();
                            for (String keyStr : keySet) {
                                ExStatus ex = cla.exStaMapTmp.get(keyStr);
                                ex.connectTime++;
                                if (ex.connectTime > 50 * 4) {
                                    cla.exStaMapTmp.remove(keyStr);
                                }
                                cla.exStatusFlag += ex.status << (ex.ch * 4);
                            }
                            cla.exStaMap = cla.exStaMapTmp;
                        }
                        if (++cla.nstaStep >= 4) {
                            cla.nstaStep = 0;
                        }
                        cla.nsta.action = "dahdiShowChannel";
                        cla.nsta.actionTim = 10;
                        cla.pbxSet.sshWriteShl("sudo asterisk -rx \"dahdi show channel " + (cla.nstaStep + 5) + "\"\n");
                        cla.pjsipAction_f = 1;
                    }

                    if (cla.nsta.type.equals("fxs")) {
                        cla.exStatusFlag = 0;
                        if (cla.exStaMapTmp != null) {
                            Set<String> keySet = cla.exStaMapTmp.keySet();
                            for (String keyStr : keySet) {
                                ExStatus ex = cla.exStaMapTmp.get(keyStr);
                                ex.connectTime++;
                                if (ex.connectTime > 50 * 4) {
                                    cla.exStaMapTmp.remove(keyStr);
                                }
                                cla.exStatusFlag += ex.status << (ex.ch * 4);
                            }
                            cla.exStaMap = cla.exStaMapTmp;
                        }
                        if (++cla.nstaStep >= 8) {
                            cla.nstaStep = 0;
                        }
                        cla.nsta.action = "dahdiShowChannel";
                        cla.nsta.actionTim = 10;
                        cla.pbxSet.sshWriteShl("sudo asterisk -rx \"dahdi show channel " + (cla.nstaStep + 1) + "\"\n");
                        cla.pjsipAction_f = 1;
                    }

                }
            }
            /*
            if (cla.nsta.asteriskSta == 1) {
                cla.nsta.action = "pjsip show aors";
                cla.nsta.actionTim = 50;
                cla.pbxSet.sshWriteShl("sudo asterisk -rx \"pjsip show aors\"\n");
            }
             */
        }

    }

}

class ConsoleSlotCmdExe {

    ConsoleSlot cla;
    Map<String, CmdTask> taskMap;

    ConsoleSlotCmdExe(ConsoleSlot owner, Map<String, CmdTask> _taskMap) {
        cla = owner;
        taskMap = _taskMap;
    }

    public void exeTaskMap() {
        for (String key : taskMap.keySet()) {
            exeTask(taskMap.get(key));
        }
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

    public int exeTask(CmdTask task) {
        if (task.retryTim < task.retryDly) {
            task.retryTim++;
            return 0;
        }
        switch (task.name) {
            case "reNewParaSet":
                return 0;
            case "test":
                return 0;
            default:
                return 0;
        }
    }
}

class CmdObj {

    String className = "";
    String cmdName;
    int time;
    int delay;

    public CmdObj(String _className, String _cmdName, int _delay) {
        className = _className;
        cmdName = _cmdName;
        delay = _delay;
        time = 0;
    }
}

class NowSlotSta {

    int status = 2;//0:none(dark), 1:exist(y blink) ,2: ready(y), 3:paraSet loaded(green blink), 4:pbx run(greeen) 5:error(red),
    String ip = GB.real_ip_str;
    int port = 49999;
    String type = GB.slotType;//ctr | sip | fxo | fxs | t1s | roip | mag  | record 
    int count = GB.slotCount;
    String inf = "";
    String action = "";
    int asteriskSta = 0;//
    int actionTim = 0;
    int slotCnt = 0;
    int swFlag;
    int mcuFlag0;
    int mcuFlag1;
    int setIp;
    String firmVer = "0.0";
    int channelFlag = 0;
    int ledFlag = 0;
    byte[] allSlotSta = new byte[128];

    NowSlotSta() {
    }

    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("ip", ip);
            json.put("type", type);
            json.put("count", count);
            json.put("status", status);
            json.put("inf", inf);
            json.put("action", action);
            json.put("asteriskSta", asteriskSta);
            json.put("softVer", GB.version);
            json.put("firmVer", firmVer);
            json.put("startTime", GB.startTime);
        } catch (Exception ex) {
            return null;
        }
        return json;
    }

}
