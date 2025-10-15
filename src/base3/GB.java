package base3;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
/*
    install java 11
    sudo apt search openjdk
    sudo apt install openjdk-11-jdk
    sudo apt install make
    sudo apt-get install gcc g++
    sudo apt install net-tools
    (controller need) sudo apt install ffmpeg -y
    chmod paraSet.json to 666

    copy "voice/josn" path to "/usr/share/asterisk/sounds/en_US/"
    copy "musiconhold.conf" file to "/etc/asterisk"
    copy "phoneHoldingMusic.gsm" file to "/var/lib/asterisk/moh"


*/
public class GB {

    //0: window ConsoleMain
    //1: window ConsoleSlot
    //2: linux ConsoleMain
    //3: linux ConsoleSlot
    static int prgMode = 0;
    static String version = "2.0";
    
    
    //=====================================
    public static int webSocketServerPort = 25666;
    public static String appName = "pbxSet";
    public static String winWebRootPath = "E:/kevin/myCode/webIcs/build/web";
    public static String linuxWebRootPath = "/var/lib/tomcat9/webapps/ROOT";
    
    
    
    static int ctrMast_f=0;
    public static String webRootPath = "";
    static String slotType = "none";
    static int slotCount = 0;
    static int ctrUiPort = 1237;
    static int slotUiPortxxx = 1236;
    static int ctrIoPort = 1437;
    static int slotIoPort = 1436;
    //static int slotIoPort=1434;
    public static final int MAX_PARA_LEN = 8192;
    static int lang = 1;//0:english 1:chinese 
    //=====================================================
    static int process_inx = 3;   //0:console sipph,1:desktop sipph,2:PhoneUi,3:Phone6in1 
    static int os_inx = 0;     //0:windows 1://linux    
    static String asteriskConfPath = "./asteridkConfPath";

    //===============================
    static int ngrep_on_f = 1;
    static int linphone_twinkle_f = 1;
    static int ictcon_on_f = 1;
    static int cursorOff_f = 0;
    //================================================
    static int fullScr_f = 0;
    static int frameOn_f = 0;
    static int syssec_f = 1;
    static int phSyssec_f = 0;
    static int syssec_xor = 0;
    static String macStr;
    static int syssec_gen_f = 0;
    //================================================
    static int winFrame_bm = 30;
    static int winFrame_wm = 8;
    static int winFrame_hm = 38;
    static String syssec = "123-125-222-456-111-123";
    static String web_password = "1234";
    public static int icsGroupId=191;
    public static int mastctr=0;
    public static String virtualIp="";
    public static String webRetStr = "";
    public static HashMap<String, ConnectCla> connectMap = new HashMap();
    
    
    //================================================
    //sipmd ui use
    //================================================
    //
    static String mainpbx_ip = "192.168.191.191";
    //static String mainpbx_ip = "127.0.0.1";
    static String mainpbx_user = "none";
    static String mainpbx_password = "";
    static String mainpbx_hostName = "";
    static String mainpbx_homePrompt = "";

    static String netName = "eth0";
    static String maskStr = "255.255.0.0";
    static String gatewayStr = "192.168.0.1";
    static String startTime = "";

    //
    //sipmd_ui tx "sipphone information" to sipui_ui over socket
    static String sipui_ui_ip = "192.168.0.181";
    static int sipui_ui_port = 1336;
    //================================================
    //sipui ui use
    //================================================exit

    //sipui_ui tx "command data" to sipmd_ui over socket
    static String sipmd_ui_ip = "192.168.0.45";
    //static int sipmd_ui_port = 1236;

    //================================================
    public static String sipui_iptype = "0";
    static String sipui_ip_str = "192.168.0.76";
    static String sipui_ipmask_str = "255.255.0.0";
    static String sipui_gateway_str = "192.168.0.1";
    static String sipmd_ip_str = "192.168.0.188";
    static String sipmd_ipmask_str = "255.255.0.0";
    static String sipmd_gateway_str = "192.168.0.1";
    static String switch_ip_str = "192.168.1.10";
    static String switch_ipmask_str = "255.255.0.0";
    static String switch_gateway_str = "192.168.0.1";
    //==============================================================================
    //static int sipmd_port = 1236;   //sipui tx "command data" to sipmd(this port) over socket
    public static String exePath = "";
    public static String preParaSetTime="";

    //==============================================================================
    //windows debug use===================================================================
    static String setdata_xml = "./setdata.xml";
    static String setdata_db = "./setdata.db";
    static String interfaces_path = "./interfaces";
    static String twinkleCfg_path = "./twinkle.cfg";
    static String ntp_path = "./timesyncd.conf";
    //==============================================================================
    //nkv6in1_ui linux use
    //static String setdata_xml = "./setdata.xml";
    //static String setdata_db = "./setdata.db";
    //static String interfaces_path="/etc/network/interfaces";
    //static String twinkleCfg_path = "./twinkle.cfg";
    //static String ntp_path = "/etc/systemd/timesyncd.conf";
    //===============================================================================
    //sipphone_ui linux use
    //static String setdata_xml="/var/lib/tomcat9/webapps/ROOT/setdata.xml";
    //static String setdata_db="/var/lib/tomcat9/webapps/ROOT/setdata.db";
    //static String interfaces_path="/etc/network/interfaces";
    //static String twinkleCfg_path = "/home/pi/.twinkle/twinkle.cfg";
    //static String ntp_path = "/etc/systemd/timesyncd.conf";
    //===============================================================================

    static String real_ip_str = "";
    static String set_ip_str = "";
    static String set_ipmask_str = "";
    static String set_gateway_str = "";

    static byte[] realIp = new byte[]{0, 0, 0, 0};

    static String[] paraName = new String[MAX_PARA_LEN];
    static String[] paraValue = new String[MAX_PARA_LEN];

    static String[] ictPhnos = new String[256];
    static int ictPhnos_amt = 0;

    //================================================
    static int paraLen = 0;
    static String ret_str;

    static int action_inx = 0;
    static int action_step = 0;
    static int action_tim = 0;
    //web use=========================================
    public static String sipmd_iptype = "0";
    //public static String sip_ip = "192.168.0.152";
    //public static String sipmd_ipmask_str = "255.255.0.0";
    //public static String sipmd_gateway_str = "192.168.0.1";
    public static String phone_name = "Kevin";
    public static String phone_no = "303";
    public static String sip_server_ip = "192.168.0.3";
    public static String sip_server_pin = "123456789";

    //==================================================================
    public static Map<String, Object> paraSetMap = new HashMap();
    public static Map<String, ExNoObj> exNoMap = new HashMap();
    public static Map<String, ExGroupObj> exGroupMap = new HashMap();

    //=================================================================
    public static String ntp_dns = "192.168.0.22";
    public static String ntp_adj_time = "160";

    //0: win.ui, 1: win.sip, 2: pi.ui, 3: pi.sip  
    static String getSlotIp() {
        return GB.getSlotIp(GB.slotType, GB.slotCount);
    }

    static String getSlotIp(String slotType, int slotCount) {
        int lip = 0;
        //GB.icsGroupId=191;
        
        if (slotType.equals("ctr")) {
            GB.netName = "enp2s0";
            lip = slotCount * 10 + 9;
               
        }
        if (slotType.equals("sip")) {
            GB.netName = "enp2s0";
            lip = slotCount * 10 + 2;
        }
        if (slotType.equals("fxo")) {
            GB.netName = "eno1";
            lip = slotCount * 10 + 4;
        }
        if (slotType.equals("fxs")) {
            GB.netName = "eno1";
            lip = slotCount * 10 + 5;
        }
        if (slotType.equals("t1s")) {
            GB.netName = "eno1";
            lip = slotCount * 10 + 3;
        }
        if (slotType.equals("mag")) {
            GB.netName = "eno1";
            lip = slotCount * 10 + 7;
        }
        if (slotType.equals("roip")) {
            GB.netName = "eno1";
            lip = slotCount * 10 + 6;
        }
        if (slotType.equals("rec")) {
            GB.netName = "eno1";
            lip = slotCount * 10 + 8;
        }
        return "192.168."+GB.icsGroupId+"."+lip;
    }

    static void chgSlotType() {
        if (GB.slotType.equals("sip")) {
            if (GB.prgMode == 2 || GB.prgMode == 3) {
                GB.mainpbx_ip = "127.0.0.1";
            } else {
                GB.mainpbx_ip = GB.getSlotIp();
            }
            GB.mainpbx_user = "mainpbx";
            GB.mainpbx_password = "123456789";
            GB.mainpbx_hostName = "mainpbx";
            GB.mainpbx_homePrompt = "mainpbx@mainpbx:~$";
        }
        if (GB.slotType.equals("fxo")) {
            if (GB.prgMode == 2 || GB.prgMode == 3) {
                GB.mainpbx_ip = "127.0.0.1";
            } else {
                GB.mainpbx_ip = GB.getSlotIp();
            }
            GB.mainpbx_user = "fxopbx";
            GB.mainpbx_password = "123456789";
            GB.mainpbx_hostName = "fxopbx";
            GB.mainpbx_homePrompt = "fxopbx@fxopbx:~$";
        }
        if (GB.slotType.equals("fxs")) {
            if (GB.prgMode == 2 || GB.prgMode == 3) {
                GB.mainpbx_ip = "127.0.0.1";
            } else {
                GB.mainpbx_ip = GB.getSlotIp();
            }
            GB.mainpbx_user = "fxspbx";
            GB.mainpbx_password = "123456789";
            GB.mainpbx_hostName = "fxspbx";
            GB.mainpbx_homePrompt = "fxspbx@fxspbx:~$";
        }
        
        if (GB.slotType.equals("mag")) {
            if (GB.prgMode == 2 || GB.prgMode == 3) {
                GB.mainpbx_ip = "127.0.0.1";
            } else {
                GB.mainpbx_ip = GB.getSlotIp();
            }
            GB.mainpbx_user = "magpbx";
            GB.mainpbx_password = "123456789";
            GB.mainpbx_hostName = "magpbx";
            GB.mainpbx_homePrompt = "magpbx@magpbx:~$";
        }
        
        if (GB.slotType.equals("t1s")) {
            if (GB.prgMode == 2 || GB.prgMode == 3) {
                GB.mainpbx_ip = "127.0.0.1";
            } else {
                GB.mainpbx_ip = GB.getSlotIp();
            }
            GB.mainpbx_user = "t1pbx";
            GB.mainpbx_password = "123456789";
            GB.mainpbx_hostName = "t1pbx";
            GB.mainpbx_homePrompt = "t1pbx@t1pbx:~$";
        }
        if (GB.slotType.equals("ctr")) {
            if (GB.prgMode == 2 || GB.prgMode == 3) {
                GB.mainpbx_ip = "127.0.0.1";
            } else {
                GB.mainpbx_ip = GB.getSlotIp();
            }
            GB.mainpbx_user = "controller";
            GB.mainpbx_password = "123456789";
            GB.mainpbx_hostName = "controller";
            GB.mainpbx_homePrompt = "controller@controller:~$";
        }

    }

    static void initGB() {
        if (GB.prgMode == 0) {
            GB.process_inx = 0; //0:consoleMain, 1:consoleSlot 
            GB.os_inx = 0;     //0:windows 1://linux    
            GB.setdata_xml = "./setdata.xml";
            GB.setdata_db = "./setdata.db";
            GB.interfaces_path = "./interfaces";
            GB.asteriskConfPath = "./asteriskConfPath";
            GB.webRootPath = GB.winWebRootPath;
        }
        if (GB.prgMode == 1) {
            GB.process_inx = 1; //0:consoleMain, 1:consoleSlot 
            GB.os_inx = 0;     //0:windows 1://linux    
            GB.setdata_xml = "./setdata.xml";
            GB.setdata_db = "./setdata.db";
            GB.interfaces_path = "./interfaces";
            GB.asteriskConfPath = "./asteriskConfPath";
            GB.webRootPath = GB.winWebRootPath;
        }
        if (GB.prgMode == 2) {
            GB.process_inx = 0; //0:consoleMain, 1:consoleSlot 
            GB.os_inx = 1;     //0:windows 1://linux    
            GB.setdata_xml = "./setdata.xml";
            GB.setdata_db = "./setdata.db";
            GB.interfaces_path = "/etc/network/interfaces";
            GB.asteriskConfPath = "./asteriskConfPath";
            GB.webRootPath = GB.linuxWebRootPath;
        }
        if (GB.prgMode == 3) {
            GB.process_inx = 1; //0:consoleMain, 1:consoleSlot 
            GB.os_inx = 1;     //0:windows 1://linux    
            GB.setdata_xml = "./setdata.xml";
            GB.setdata_db = "./setdata.db";
            GB.interfaces_path = "/etc/network/interfaces";
            GB.asteriskConfPath = "./asteriskConfPath";
            GB.webRootPath = GB.linuxWebRootPath;
        }
        GB.chgSlotType();
        //=============================================================
    }

    public static void loadPara2Form() {

        Class type;
        Object obj;
        int i;
        String str;
        String[] strA;
        String[][] strAA;

        //java.lang.reflect.Field[] f3 = cla.getClass().getDeclaredFields();
        java.lang.reflect.Field[] f3 = GB.class.getDeclaredFields();
        for (i = 0; i < f3.length; i++) {
            f3[i].setAccessible(true);
            try {
                obj = f3[i].get(GB.class);

                if (obj instanceof String[][]) {
                    str = f3[i].getName();
                    strAA = (String[][]) obj;
                    for (int j = 0; j < paraLen; j++) {
                        String[] sbufA;
                        sbufA = paraName[j].split("~");
                        if (sbufA.length == 3) {
                            if (str.equals(sbufA[0])) {
                                byte[] bytes = new byte[paraValue[j].length()];
                                String str1 = paraValue[j];
                                for (int m = 0; m < str1.length(); m++) {
                                    bytes[m] = (byte) str1.charAt(m);
                                }
                                strAA[Integer.parseInt(sbufA[1])][Integer.parseInt(sbufA[2])] = new String(bytes, Charset.forName("UTF-8"));
                            }
                        }
                    }
                } else if (obj instanceof String[]) {
                    str = f3[i].getName();
                    strA = (String[]) obj;
                    for (int j = 0; j < paraLen; j++) {
                        String[] sbufA;
                        sbufA = paraName[j].split("~");
                        if (sbufA.length == 2) {
                            if (str.equals(sbufA[0])) {
                                byte[] bytes = new byte[paraValue[j].length()];
                                String str1 = paraValue[j];
                                for (int m = 0; m < str1.length(); m++) {
                                    bytes[m] = (byte) str1.charAt(m);
                                }
                                strA[Integer.parseInt(sbufA[1])] = new String(bytes, Charset.forName("UTF-8"));
                            }
                        }
                    }
                } else if (obj instanceof int[]) {
                    str = f3[i].getName();
                    int[] intA = (int[]) obj;
                    for (int j = 0; j < paraLen; j++) {
                        String[] sbufA;
                        sbufA = paraName[j].split("~");
                        if (sbufA.length == 2) {
                            if (str.equals(sbufA[0])) {
                                intA[Integer.parseInt(sbufA[1])] = Integer.parseInt(paraValue[j]);
                            }
                        }
                    }
                } else if (obj instanceof String) {
                    str = f3[i].getName();

                    for (int j = 0; j < paraLen; j++) {
                        if (str.equals(paraName[j])) {
                            byte[] bytes = new byte[paraValue[j].length()];
                            String str1 = paraValue[j];
                            for (int m = 0; m < str1.length(); m++) {
                                bytes[m] = (byte) str1.charAt(m);
                            }
                            f3[i].set(GB.class, new String(bytes, Charset.forName("UTF-8")));
                        }
                    }
                } else if (obj instanceof Integer) {
                    str = f3[i].getName();
                    for (int j = 0; j < paraLen; j++) {
                        if (str.equals(paraName[j])) {
                            f3[i].set(GB.class, Integer.parseInt(paraValue[j]));
                        }
                    }
                } else {

                }
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(GB.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    static void clrPara() {
        for (int i = 0; i < GB.paraLen; i++) {
            GB.paraName[i] = null;
            GB.paraValue[i] = null;
            GB.paraLen = 0;
        }
    }

    static int newPara(String name, String value) {
        if (paraLen >= MAX_PARA_LEN) {
            return 0;
        }
        paraName[paraLen] = name;
        paraValue[paraLen] = value;
        paraLen++;
        return 1;
    }

    static int editPara(String name, String value) {
        int i;
        for (i = 0; i < paraLen; i++) {
            if (paraName[i].equals(name)) {
                paraValue[i] = value;
                return 1;
            }
        }
        return 0;
    }

    static int editNewPara(String name, String value) {
        if (editPara(name, value) == 0) {
            return newPara(name, value);
        }
        return 1;
    }

    static String getPara(String name) {
        int i;
        for (i = 0; i < paraLen; i++) {
            if (paraName[i].equals(name)) {
                return paraValue[i];
            }
        }
        return null;
    }

    static int deletePara(String name) {
        int i;
        for (i = 0; i < paraLen; i++) {
            if (paraName[i].equals(name)) {
                i++;
                for (; i < paraLen; i++) {
                    paraName[i - 1] = paraName[i];
                    paraValue[i - 1] = paraValue[i];
                }
                paraLen--;
                return 1;
            }
        }
        return 0;
    }

}
