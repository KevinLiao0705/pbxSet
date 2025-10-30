/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package base3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Administrator
 */
public class PbxSet {

    byte[] ioBuf = new byte[16];
    //===============================
    int shellCommandStatus = 0;      //0:ready,1:play dial tone
    //==============================
    int shlFirstIn_f = 0;
    //============================
    Ssh sshShl = null;
    ShlrxTd shlrxTd = null;
    ShlconTd shlconTd = null;
    int shlrxTd_run_f = 0;
    int shlrxTd_destroy_f = 0;
    int shlconTd_run_f = 0;
    int shlconTd_destroy_f = 0;
    ShellRx shellRx;

    //============================
    Timer tm1 = null;//for display
    Vt100 vtshl;

    PbxSet() {
        int i;
    }

    public void create() {

        int i = 0;

        final PbxSet cla = this;

        //=================================================
        vtshl = new Vt100();
        vtshl.clr_telscr();
        vtshl.vtcmp = new Vtcmp() {
            @Override
            public void cmp() {
                cla.vtcmpShl();
            }
        };

        if (cla.shlrxTd == null) {
            cla.shlrxTd = new ShlrxTd(cla);
            cla.shlrxTd.start();
            cla.shlrxTd_run_f = 1;
            cla.shlrxTd_destroy_f = 0;
        }
        if (cla.shlconTd == null) {
            cla.shlconTd = new ShlconTd(cla);
            cla.shlconTd.start();
            cla.shlconTd_run_f = 1;
            cla.shlconTd_destroy_f = 0;
        }

        //===================================================
        //general timer
        if (cla.tm1 == null) {
            cla.tm1 = new Timer();
            //tm1.schedule(new PbxSetTm1(cla), 1000, 20);
        }
        //======================================

    }

    void vtcmpShl() {
        PbxSet cla = this;
        int i = 0;
        String str;
        //============================================
        if (cla.vtshl.cmp("@raspberrypi:~$")) {
            if (shlFirstIn_f == 0) {

            }
            return;
        }
        if (cla.vtshl.cmp("Playing WAVE")) {
            shellCommandStatus = 1;
            return;
        }
    }

    public void sshWriteShl(String shellCommand) {
        PbxSet cla = this;
        if (cla.sshShl == null || cla.sshShl.connect_f == 0) {
            return;
        }
        try {
            cla.sshShl.outStrm.write(shellCommand.getBytes());
        } catch (IOException ex) {
        }
        try {
            cla.sshShl.outStrm.flush();
        } catch (IOException ex) {
        }
    }

    public void sshWriteByteShl(byte[] bytes) {
        PbxSet cla = this;
        if (cla.sshShl == null || cla.sshShl.connect_f == 0) {
            return;
        }
        try {
            cla.sshShl.outStrm.write(bytes);
        } catch (IOException ex) {
        }
        try {
            cla.sshShl.outStrm.flush();
        } catch (IOException ex) {
        }
    }

    void reset_network() {
        String cmdStr;
        cmdStr = "sudo ifconfig eth0 ";
        cmdStr += GB.sipmd_ip_str;
        cmdStr += " netmask ";
        cmdStr += GB.sipmd_ipmask_str;
        cmdStr += " broadcast ";
        cmdStr += GB.sipmd_gateway_str;
        Lib.exe(cmdStr);

        GB.real_ip_str = GB.sipmd_ip_str;
        GB.set_ipmask_str = GB.sipmd_ipmask_str;
        GB.set_gateway_str = GB.sipmd_gateway_str;

        //============================    
    }

    void txShellEsc() {
        byte[] bytes;
        bytes = new byte[2];
        bytes[0] = 0x03;
        bytes[1] = 13;
        //sshWriteShl(new String(bytes));
        sshWriteShl("kill $PID\n");
        shellCommandStatus = 0;

    }

    void txret_ssksip_inf(Ssocket ssk) {
        PbxSet cla = this;
        byte[] bytes;
        int i;
        try {
            for (i = 0; i < ssk.stm.txlen; i++) {
                ssk.outstr.write(ssk.stm.tdata[i]);
            }
        } catch (IOException ex) {
        }
    }

    public String getIaxHead(String slotType, int slotCnt, String exType, int exSlotCnt) {
        String iaxHead = "";
        //========================================
        if (slotType.equals("sip")) {
            if (exType.equals("sip") || exType.equals("roip") || exType.equals("soft")) {
                if (exSlotCnt == slotCnt) {
                    iaxHead = "PJSIP/";
                } else {
                    iaxHead = "IAX2/" + slotType + "Pbx_" + exSlotCnt + "/";
                }
                return iaxHead;
            } else {
                iaxHead = "IAX2/" + exType + "Pbx_" + exSlotCnt + "/";
                return iaxHead;
            }
        }
        if (slotType.equals("fxo")) {
            if (exType.equals(slotType)) {
                if (exSlotCnt == slotCnt) {
                    iaxHead = "DAHDI/G1/";
                } else {
                    iaxHead = "IAX2/" + slotType + "Pbx_" + exSlotCnt + "/";
                }
                return iaxHead;
            } else {
                if (exType.equals("soft")) {
                    exType = "sip";
                }
                iaxHead = "IAX2/" + exType + "Pbx_" + exSlotCnt + "/";
                return iaxHead;
            }
        }

        if (slotType.equals("fxs") || slotType.equals("mag") || slotType.equals("t1s")) {
            if (exType.equals(slotType)) {
                if (exSlotCnt == slotCnt) {
                    iaxHead = "DAHDI/";
                } else {
                    iaxHead = "IAX2/" + slotType + "Pbx_" + exSlotCnt + "/";
                }
                return iaxHead;
            } else {
                if (exType.equals("soft")) {
                    exType = "sip";
                }
                iaxHead = "IAX2/" + exType + "Pbx_" + exSlotCnt + "/";
                return iaxHead;
            }
        }

        return iaxHead;

    }

    //*000 call sipPbx1
    //*001 call sipPbx2
    //*010 call fxoPbx1
    //*011 call fxoPbs2
    //*020 call fxsPbx1
    //*021 call fxsPbs2
    //*030 call t1Pbx1
    //*031 call t1Pbs2
    public String getIaxConf(String sipType, int slotCnt) {
        String content = "";
        content += "\n[general]";
        content += "\nbandwidth=low";
        content += "\ndisallow=lpc10";
        content += "\njitterbuffer=no";
        content += "\nencryption=yes";
        content += "\nautokill=yes";

        content += "\n";
        content += "\n[guest]";
        content += "\ntype=user";
        content += "\ncontext=public";
        content += "\ncallerid=\"Guest IAX User\"";
        content += "\n";
        content += "\n[iaxtel]";
        content += "\ntype=user";
        content += "\ncontext=default";
        content += "\nauth=rsa";
        content += "\ninkeys=iaxtel";
        content += "\n";
        content += "\n[iaxfwd]";
        content += "\ntype=user";
        content += "\ncontext=default";
        content += "\nauth=rsa";
        content += "\ninkeys=freeworlddialup";
        content += "\n";
        content += "\n[pbxin]";
        content += "\ntype=user";
        if (sipType.equals("sip")) {
            content += "\ncontext=from-pstn";
        }
        if (sipType.equals("fxo")) {
            content += "\ncontext=from-pstn";
        }
        if (sipType.equals("fxs")) {
            content += "\ncontext=from-pstn";
        }
        if (sipType.equals("t1s")) {
            content += "\ncontext=from-pstn";
        }
        if (sipType.equals("mag")) {
            content += "\ncontext=from-pstn";
        }

        String[] strA = "sip~fxo~fxs~t1s~mag".split("~");
        for (int i = 0; i < strA.length; i++) {
            for (int j = 0; j < 4; j++) {
                content += "\n";
                content += "\n[" + strA[i] + "Pbx_" + j + "]";
                content += "\nhost=" + GB.getSlotIp(strA[i], j);
                content += "\ntype=peer";
                content += "\nqualify=yes";
                content += "\ncontext=from-pstn";
            }
        }

        JSONArray jsArr = (JSONArray) GB.paraSetMap.get("icsGroups");
        int len = jsArr.length();
        for (int inx = 0; inx < len; inx++) {
            String strGroups;
            try {
                strGroups = jsArr.get(inx).toString();
            } catch (Exception ex) {
                continue;
            }
            strA = strGroups.split("~");
            if (strA.length != 8) {
                continue;
            }
            for (int i = 0; i < strA.length; i++) {
                strA[i] = strA[i].trim();
            }
            if (!strA[5].equals("JosnPbx")) {
                continue;
            }
            int no = Lib.str2int(strA[0], -1);
            if (no < 0) {
                continue;
            }
            if (no >= 100) {
                continue;
            }
            String noStr = "";
            if (no < 10) {
                noStr += "0" + no;
            } else {
                noStr += no;
            }
            String pbxStr = "exPbx_" + noStr;
            //String ipPortStr = strA[2] + ":" + strA[3];
            String ipPortStr = strA[2];
            content += "\n";
            content += "\n[" + pbxStr + "]";
            content += "\nhost=" + ipPortStr;
            content += "\ntype=peer";
            content += "\nqualify=yes";
            content += "\ncontext=from-pstn";

        }

        return content;

    }

    public String getChanConf(String slotType, int slotCnt) {
        String content = "";
        content += "\n[channels]";
        content += "\nusecallerid=yes";
        content += "\ncallerid=asreceived";
        content += "\ncallwaiting=yes";
        content += "\nusecallingpres=yes";
        content += "\ncallwaitingcallerid=yes";
        content += "\nthreewaycalling=yes";
        content += "\ntransfer=yes";
        content += "\ncanpark=yes";
        content += "\ncancallforward=yes";
        content += "\ncallreturn=yes";
        content += "\nechocancel=yes";
        content += "\nechocancelwhenbridged=yes";
        content += "\ngroup=1";
        content += "\ncallgroup=1";
        content += "\npickupgroup=1";
        content += "\nrxgain=0";
        content += "\ntxgain=0";
        content += "\nhwrxgain=0";
        content += "\nhwtxgain=3.0";
        if (slotType.equals("fxo")) {
            content += "\ncidsignalling=dtmf";
            content += "\ncidstart=dtmf";
            content += "\nbusydetect=yes";
            content += "\nbusycount=3";
        }
        if (slotType.equals("fxs")) {
            content += "\ncidsignalling=bell";
        }
        if (slotType.equals("mag")) {
            content += "\ncidsignalling=bell";
        }
        content += "\nsendcalleridafter =1";
        content += "\ncontext=public";
        content += "\n";

        if (slotType.equals("fxs")) {
            int cntMax = 8;
            int cntMin = 0;
            int inx = 0;
            for (String key : GB.exNoMap.keySet()) {
                ExNoObj obj = GB.exNoMap.get(key);
                if (!obj.type.equals(slotType)) {
                    continue;
                }
                if (obj.slotCnt != slotCnt) {
                    continue;
                }
                if (obj.channel < cntMin) {
                    continue;
                }
                if (obj.channel >= cntMax) {
                    continue;
                }
                content += "\n";
                content += "\n[phone" + (inx + 1) + "]";
                content += "\ndahdichan=" + (obj.channel + 1);
                content += "\nsignalling=fxo_ks";
                content += "\ncallerid=" + obj.no;
                content += "\ncontext=from-dahdi";
                inx++;
            }
        }

        if (slotType.equals("mag")) {
            int cntMax = 8;
            int cntMin = 0;
            int inx = 0;
            for (String key : GB.exNoMap.keySet()) {
                ExNoObj obj = GB.exNoMap.get(key);
                if (!obj.type.equals(slotType)) {
                    continue;
                }
                if (obj.slotCnt != slotCnt) {
                    continue;
                }
                if (obj.channel < cntMin) {
                    continue;
                }
                if (obj.channel >= cntMax) {
                    continue;
                }
                content += "\n";
                content += "\n[phone" + (inx + 1) + "]";
                content += "\ndahdichan=" + (obj.channel + 5);
                content += "\nsignalling=fxo_ks";
                content += "\ncallerid=" + obj.no;
                content += "\ncontext=from-dahdi";
                inx++;
            }
        }

        if (slotType.equals("t1s")) {
            int cntMax = 96;
            int cntMin = 0;
            int inx = 0;
            for (String key : GB.exNoMap.keySet()) {
                ExNoObj obj = GB.exNoMap.get(key);
                if (!obj.type.equals(slotType)) {
                    continue;
                }
                if (obj.slotCnt != slotCnt) {
                    continue;
                }
                if (obj.channel < cntMin) {
                    continue;
                }
                if (obj.channel >= cntMax) {
                    continue;
                }
                int mod = obj.channel % 100;
                content += "\n";
                content += "\n[phone" + (inx + 1) + "]";
                content += "\ndahdichan=" + (obj.channel + 1);
                content += "\nsignalling=fxo_ks";
                content += "\ncallerid=" + obj.no;
                content += "\ncontext=from-dahdi";
                inx++;
            }
        }

        if (slotType.equals("fxo")) {
            int inx = 0;
            for (int i = 0; i < 8; i++) {
                content += "\n";
                content += "\n[phone" + (inx + 1) + "]";
                content += "\ndahdichan=" + (i + 1);
                content += "\ncontext=from-dahdi";
                inx++;
            }
        }
        return content;
    }

    public String getConfbridgeConf() {
        String str;
        String content = "";
        content += "\n[general]";
        content += "\n[default_user]";
        content += "\ntype=user";
        content += "\n[default_bridge]";
        content += "\ntype=bridge";
        content += "\n";
        content += "\n[admin_user]";
        content += "\ntype=user";
        content += "\nmarked=yes";
        content += "\nadmin=yes";
        content += "\nmusic_on_hold_when_empty=yes";
        content += "\nannounce_user_count=no";
        content += "\nannounce_only_user=no";
        JSONArray jsArr = (JSONArray) GB.paraSetMap.get("meetGroups");
        int len = jsArr.length();
        for (int i = 0; i < len; i++) {
            String groupStr = "";
            try {
                groupStr = jsArr.get(i).toString() + " ";
            } catch (Exception ex) {
                continue;
            }
            String[] strA = groupStr.split("~");
            if (strA.length != 4) {
                continue;
            }
            content += "\n";
            content += "\n;ConferenceRoom";
            content += "\n;=============================================";
            str = "\n[myconf" + strA[0] + "]";
            content += str;
            content += "\ntype=bridge";
        }
        return content;

    }

    public String getPjsipConf() {
        int len;

        //=================================================       
        String content = "";
        content += "\n[transport-udp]";
        content += "\ntype=transport";
        content += "\nprotocol=udp";
        content += "\nbind=0.0.0.0";
        content += "\n";
        

        String[] strA;
        try {

            JSONArray jArr = (JSONArray) GB.paraSetMap.get("exNoGroups");
            int gsLen = jArr.length();
            String[] groups = new String[gsLen];
            GB.exGroupMap.clear();
            for (int ii = 0; ii < gsLen; ii++) {
                groups[ii] = jArr.get(ii).toString().split("~")[0];
            }

            for (String key : GB.exNoMap.keySet()) {
                ExNoObj obj = GB.exNoMap.get(key);
                int yes_f = 0;
                if (obj.type.equals("sip")) {
                    yes_f = 1;
                }
                if (obj.type.equals("soft")) {
                    yes_f = 1;
                }
                if (obj.type.equals("roip")) {
                    yes_f = 1;
                }
                if (yes_f == 0) {
                    continue;
                }
                String takeGroup = obj.ringGroup;

                content += "\n;=====================================";
                content += "\n[" + obj.no + "]";
                content += "\ntype=endpoint";
                content += "\ncontext=from-pstn";
                content += "\ndisallow=all";
                content += "\nallow=speex16";
                content += "\nallow=ulaw";
                content += "\nallow=alaw";
                
                
                
                content += "\ntransport=transport-udp";
                content += "\nauth=" + obj.no + "";
                content += "\naors=" + obj.no + "";
                if(takeGroup.length()!=0){
                    for(int ii=0;ii<groups.length;ii++){
                        if(groups[ii].equals(takeGroup)){
                            content += "\npickup_group="+ii;
                            content += "\ncall_group="+ii;
                            break;
                        }
                    }
                }
                content += "\n[" + obj.no + "]";
                content += "\ntype=auth";
                content += "\nauth_type=userpass";
                content += "\npassword=" + obj.loginPassword + "";
                content += "\nusername=" + obj.no + "";
                content += "\n[" + obj.no + "]";
                content += "\ntype=aor";
                content += "\nmax_contacts=1";
                content += "\n";

            }

            JSONArray jsArr = (JSONArray) GB.paraSetMap.get("icsGroups");
            len = jsArr.length();
            for (int inx = 0; inx < len; inx++) {
                String strGroups = jsArr.get(inx).toString();
                strA = strGroups.split("~");
                if (strA.length != 8) {
                    continue;
                }
                for (int i = 0; i < strA.length; i++) {
                    strA[i] = strA[i].trim();
                }
                if (strA[5].equals("JosnPbx")) {
                    continue;
                }
                int no = Lib.str2int(strA[0], -1);
                if (no < 0) {
                    continue;
                }
                if (no >= 100) {
                    continue;
                }
                String noStr = "";
                if (no < 10) {
                    noStr += "0" + no;
                } else {
                    noStr += no;
                }
                String pbxStr = "exPbx_" + noStr;
                String ipPortStr = strA[2] + ":" + strA[3];
                content += "\n;=====================================";
                content += "\n[" + pbxStr + "]";
                content += "\ntype=endpoint";
                content += "\ntransport=transport-udp";
                //content += "\ncontext=from-pstn";
                content += "\ncontext=" + pbxStr + "_in";
                content += "\ndisallow=all";
                content += "\nallow=speex16";
                content += "\nallow=ulaw";
                content += "\nallow=alaw";
                
                
                content += "\noutbound_auth=" + pbxStr;
                content += "\naors=" + pbxStr;

                content += "\n[" + pbxStr + "]";
                content += "\ntype=aor";
                content += "\nqualify_frequency=60";
                content += "\ncontact=sip:" + ipPortStr;

                content += "\n[" + pbxStr + "]";
                content += "\ntype=identify";
                content += "\nendpoint=" + pbxStr;
                content += "\nmatch=" + ipPortStr;

                if (strA[6].equals("") || strA[7].equals("")) {
                    content += "\n";
                    continue;
                }
                content += "\n[" + pbxStr + "]";
                content += "\ntype=auth";
                content += "\nauth_type=userpass";
                content += "\nusername=" + strA[6];
                content += "\npassword=" + strA[7];
                content += "\n";

            }

        } catch (Exception ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());

        }

        return content;

    }

    public String getExPjsipConf() {
        String content = "";
        String exRegisterPin = GB.paraSetMap.get("exRegisterPin").toString();

        content += "\n[transport-udp]";
        content += "\ntype=transport";
        content += "\nprotocol=udp";
        content += "\nbind=0.0.0.0";
        int len;
        String[] strA;
        try {
            JSONArray jsArr = (JSONArray) GB.paraSetMap.get("icsGroups");
            len = jsArr.length();
            for (int inx = 0; inx < len; inx++) {
                String strGroups = jsArr.get(inx).toString();
                strA = strGroups.split("~");
                if (strA.length != 8) {
                    continue;
                }
                for (int i = 0; i < strA.length; i++) {
                    strA[i] = strA[i].trim();
                }
                if (strA[5].equals("JosnPbx")) {
                    continue;
                }
                int no = Lib.str2int(strA[0], -1);
                if (no < 0) {
                    continue;
                }
                if (no >= 100) {
                    continue;
                }
                String noStr = "";
                if (no < 10) {
                    noStr += "0" + no;
                } else {
                    noStr += no;
                }
                String pbxStr = "exPbx_" + noStr;
                String ipPortStr = strA[2] + ":" + strA[3];
                content += "\n;=====================================";
                content += "\n[" + pbxStr + "]";
                content += "\ntype=endpoint";
                content += "\ntransport=transport-udp";
                //content += "\ncontext=from-pstn";
                content += "\ncontext=" + pbxStr + "_in";
                content += "\ndisallow=all";
                content += "\nallow=speex16";
                content += "\nallow=ulaw";
                content += "\nallow=alaw";
                content += "\noutbound_auth=" + pbxStr;
                content += "\naors=" + pbxStr;

                content += "\n[" + pbxStr + "]";
                content += "\ntype=aor";
                content += "\nqualify_frequency=60";
                content += "\ncontact=sip:" + ipPortStr;

                content += "\n[" + pbxStr + "]";
                content += "\ntype=identify";
                content += "\nendpoint=" + pbxStr;
                content += "\nmatch=" + ipPortStr;

                if (strA[6].equals("") || strA[7].equals("")) {
                    content += "\n";
                    continue;
                }
                content += "\n[" + pbxStr + "]";
                content += "\ntype=auth";
                content += "\nauth_type=userpass";
                content += "\nusername=" + strA[6];
                content += "\npassword=" + strA[7];
                content += "\n";

            }

        } catch (Exception ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());

        }

        return content;

    }

    public ArrayList<String> getRingGroup(ExNoObj obj) {
        ArrayList<String> ringGroupList = new ArrayList<String>();
        ringGroupList.add(obj.no);
        /*
        if (obj.ringGroup != null) {
            if (obj.ringGroup.length() > 0 && (!obj.jmpGroup.equals("all"))) {
                ExGroupObj objGroup = GB.exGroupMap.get(obj.ringGroup);
                if (objGroup != null) {
                    for (int i = 0; i < objGroup.noList.size(); i++) {
                        String noStr = objGroup.noList.get(i);
                        if (!noStr.equals(obj.no)) {
                            ringGroupList.add(noStr);
                        }
                    }
                } else {
                    String[] strB = obj.ringGroup.split(",");
                    for (int i = 0; i < strB.length; i++) {
                        String noStr = strB[i].trim();
                        if (noStr.length() == 0) {
                            continue;
                        }
                        if (!noStr.equals(obj.no)) {
                            ringGroupList.add(noStr);
                        }
                    }

                }
            }
        }
        */
        return ringGroupList;
    }

    public String getComExtensions() {
        String content = "";
        content += "\n;=============================================";
        content += "\nexten => 10000,1,Answer()";
        content += "\n  same => n,Playback(hello-world)";
        content += "\n";
        content += "\nexten => " + "_*00." + "," + "1" + ",Goto(sipTrunk0,${EXTEN:3},1)";
        content += "\nexten => " + "_*01." + "," + "1" + ",Goto(sipTrunk1,${EXTEN:3},1)";
        content += "\nexten => " + "_*02." + "," + "1" + ",Goto(sipTrunk2,${EXTEN:3},1)";
        content += "\nexten => " + "_*03." + "," + "1" + ",Goto(sipTrunk3,${EXTEN:3},1)";
        content += "\nexten => " + "_*10." + "," + "1" + ",Goto(fxoTrunk0,${EXTEN:3},1)";
        content += "\nexten => " + "_*11." + "," + "1" + ",Goto(fxoTrunk1,${EXTEN:3},1)";
        content += "\nexten => " + "_*12." + "," + "1" + ",Goto(fxoTrunk2,${EXTEN:3},1)";
        content += "\nexten => " + "_*13." + "," + "1" + ",Goto(fxoTrunk3,${EXTEN:3},1)";
        content += "\nexten => " + "_*20." + "," + "1" + ",Goto(fxsTrunk0,${EXTEN:3},1)";
        content += "\nexten => " + "_*21." + "," + "1" + ",Goto(fxsTrunk1,${EXTEN:3},1)";
        content += "\nexten => " + "_*22." + "," + "1" + ",Goto(fxsTrunk2,${EXTEN:3},1)";
        content += "\nexten => " + "_*23." + "," + "1" + ",Goto(fxsTrunk3,${EXTEN:3},1)";
        content += "\nexten => " + "_*30." + "," + "1" + ",Goto(t1sTrunk0,${EXTEN:3},1)";
        content += "\nexten => " + "_*31." + "," + "1" + ",Goto(t1sTrunk1,${EXTEN:3},1)";
        content += "\nexten => " + "_*32." + "," + "1" + ",Goto(t1sTrunk2,${EXTEN:3},1)";
        content += "\nexten => " + "_*33." + "," + "1" + ",Goto(t1sTrunk3,${EXTEN:3},1)";
        content += "\nexten => " + "_*40." + "," + "1" + ",Goto(magTrunk0,${EXTEN:3},1)";
        content += "\nexten => " + "_*41." + "," + "1" + ",Goto(magTrunk1,${EXTEN:3},1)";
        content += "\nexten => " + "_*42." + "," + "1" + ",Goto(magTrunk2,${EXTEN:3},1)";
        content += "\nexten => " + "_*43." + "," + "1" + ",Goto(magTrunk3,${EXTEN:3},1)";
        content += "\nexten => " + "_*870." + "," + "1" + ",Goto(monitorSeg,${EXTEN},1)";
        content += "\nexten => " + "_*9." + "," + "1" + ",Goto(fxoDirect0,${EXTEN},1)";

        int len = 0;
        String[] strA;
        JSONArray jsArr = (JSONArray) GB.paraSetMap.get("icsGroups");
        len = jsArr.length();
        for (int inx = 0; inx < len; inx++) {
            String strGroups;
            try {
                strGroups = jsArr.get(inx).toString();
            } catch (Exception ex) {
                continue;
            }
            strA = strGroups.split("~");
            if (strA.length != 8) {
                continue;
            }
            for (int i = 0; i < strA.length; i++) {
                strA[i] = strA[i].trim();
            }
            String pbxType = "PJSIP";
            if (strA[5].equals("JosnPbx")) {
                pbxType = "IAX2";
            }
            int no = Lib.str2int(strA[0], -1);
            if (no < 0) {
                continue;
            }
            if (no >= 100) {
                continue;
            }
            String noStr = "";
            if (no < 10) {
                noStr += "0" + no;
            } else {
                noStr += no;
            }
            String pbxStr = "exPbx_" + noStr;

            content += "\nexten => " + "_*5" + noStr + "." + "," + "1" + ",Goto(" + pbxStr + "Trunk,${EXTEN:4},1)";
        }

        String[] aixHeadTbl = {
            "IAX2/sipPbx_0/", "IAX2/sipPbx_1/", "IAX2/sipPbx_2/", "IAX2/sipPbx_3/",
            "IAX2/fxoPbx_0/", "IAX2/fxoPbx_1/", "IAX2/fxoPbx_2/", "IAX2/fxoPbx_3/",
            "IAX2/fxsPbx_0/", "IAX2/fxsPbx_1/", "IAX2/fxsPbx_2/", "IAX2/fxsPbx_3/",
            "IAX2/t1sPbx_0/", "IAX2/t1sPbx_1/", "IAX2/t1sPbx_2/", "IAX2/t1sPbx_3/",
            "IAX2/magPbx_0/", "IAX2/magPbx_1/", "IAX2/magPbx_2/", "IAX2/magPbx_3/"
        };
        String[] trunkTbl = {
            "sipTrunk0", "sipTrunk1", "sipTrunk2", "sipTrunk3",
            "fxoTrunk0", "fxoTrunk1", "fxoTrunk2", "fxoTrunk3",
            "fxsTrunk0", "fxsTrunk1", "fxsTrunk2", "fxsTrunk3",
            "t1sTrunk0", "t1sTrunk1", "t1sTrunk2", "t1sTrunk3",
            "magTrunk0", "magTrunk1", "magTrunk2", "magTrunk3"
        };

        for (int i = 0; i < 20; i++) {
            content += "\n";
            content += "\n[" + trunkTbl[i] + "]";
            content += "\nexten => " + "_X!" + ",1,NoOp()";
            content += "\n  same => n,Dial(" + aixHeadTbl[i] + "${EXTEN})";
            content += "\n  same => n,Hangup()";
        }

        for (int inx = 0; inx < len; inx++) {
            String strGroups;
            try {
                strGroups = jsArr.get(inx).toString();
            } catch (Exception ex) {
                continue;
            }
            strA = strGroups.split("~");
            if (strA.length != 8) {
                continue;
            }
            for (int i = 0; i < strA.length; i++) {
                strA[i] = strA[i].trim();
            }
            String pbxType = "PJSIP";
            if (strA[5].equals("JosnPbx")) {
                pbxType = "IAX2";
            }
            int no = Lib.str2int(strA[0], -1);
            if (no < 0) {
                continue;
            }
            if (no >= 100) {
                continue;
            }
            String noStr = "";
            if (no < 10) {
                noStr += "0" + no;
            } else {
                noStr += no;
            }
            String pbxStr = "exPbx_" + noStr;

            content += "\n";
            content += "\n[" + pbxStr + "Trunk" + "]";
            content += "\nexten => " + "_X!" + ",1,NoOp()";
            if (pbxType.equals("PJSIP")) {
                content += "\n  same => n,Dial(" + pbxType + "/${EXTEN}@" + pbxStr + ")";
            } else {
                content += "\n  same => n,Dial(" + pbxType + "/" + pbxStr + "/${EXTEN}" + ")";
            }
            content += "\n  same => n,Hangup()";

            content += "\n";
            content += "\n[" + pbxStr + "_in" + "]";
            content += "\nexten => " + "_X." + ",1,NoOp()";
            //content += "\nexten => " + "_."  + "," + "1" + ",Goto(" + "from-pstn" + ",${EXTEN:"+strA[4].length()+"},1)";
            content += "\n  same => n,Set(CALLERID(name)=<pbx" + noStr + ">${CALLERID(name)})";
            content += "\n  same => n,Goto(" + "from-pstn" + ",${EXTEN:" + strA[4].length() + "},1)";

        }

        //===================================================================================
        content += "\n";
        content += "\n[" + "monitorSeg" + "]";
        content += "\nexten => " + "_*870." + ",1,NoOp()";
        content += "\n  same => n,ChanSpy(" + "PJSIP/" + "${EXTEN:4})";

        //===================================================================================
        content += "\n";
        content += "\n[" + "fxoDirect0" + "]";
        content += "\nexten => " + "_*9X!" + ",1,NoOp()";
        content += "\n  same => n,Dial(" + "IAX2/fxoPbx_0/" + "${EXTEN})";
        content += "\n  same => n,Hangup()";

        //===================================================================================
        content += "\n";
        content += "\n[errPermission]";
        content += "\nexten => " + "10000" + ",1,NoOp()";
        content += "\n  same => n,Playback(josn/errorPermission)";
        content += "\n  same => n,Hangup()";
        //===================================================================================
        content += "\n";
        content += "\n[canclePhone]";
        content += "\nexten => " + "10000" + ",1,NoOp()";
        content += "\n  same => n,Hangup()";
        //===================================================================================
        content += "\n";
        content += "\n[pinError]";
        content += "\nexten => " + "10000" + ",1,NoOp()";
        content += "\n  same => n,Playback(josn/pinError)";
        content += "\n  same => n,Hangup()";
        //content += "\n";
        //content += "\n;same => n,SayDigits(${CALLERID(num)})";
        //content += "\n;same => n,Set(CALLERID(all)=\"Jane Smith\"<2095551213>)";
        //content += "\n;same => n,Set(CALLERID(name)=KevinLiao)";
        //content += "\n;same => n,Set(CALLERID(num)=2095551214)";
        content += "\n";
        return content;

    }

    public String getSipExtensions(String slotType, int slotCnt) {
        String bstr;
        try {
            String processType = GB.paraSetMap.get("processType").toString();

            String content = "";
            content += "\n[general]";
            content += "\nstatic=yes";
            content += "\nwriteprotect=no";
            content += "\nclearglobalvars=no";
            content += "\n";
            content += "\n[globals]";
            content += "\nCONSOLE=Console/dsp";
            content += "\n";
            content += "\n[from-pstn]";

            String iaxHead = "";
            int fxsCnt = 0;
            for (String key : GB.exNoMap.keySet()) {
                ExNoObj obj = GB.exNoMap.get(key);

                int sipStep = 1;
                content += "\n";
                content += "\n; Type:" + obj.type + ", slotCnt:" + obj.slotCnt + ", Name:" + obj.name + ", No:" + obj.no + ", Channel:" + obj.channel + ", jmpNumber:" + obj.jmpNumber;
                content += "\n; ringGroup:" + obj.ringGroup;
                content += "\n; jmpGroup:" + obj.jmpGroup;
                //==============================================================================
                content += "\nexten => " + obj.no + "," + sipStep + ",NoOp(${CALLERID})";
                sipStep++;

                ArrayList<String> ringGroupList = this.getRingGroup(obj);

                if (ringGroupList.size() <= 1) {
                    //content += "\nexten => " + phNo + ",1,Goto(sipContext,${EXTEN},1)";
                    content += "\n  same => " + sipStep + ",GotoIf($[\"${CALLERID(num)}\" = \"" + obj.no + "\"]?callSelf)";
                    sipStep++;
                    //===============================================================
                    String exten = "${EXTEN}";
                    String dialTarget = getIaxHead(slotType, slotCnt, obj.type, obj.slotCnt) + exten;
                    //===============================================================
                    String fstr = "/home/" + GB.mainpbx_hostName + "/kevin/pbxSetExe/record/";
                    fstr += "${STRFTIME(${EPOCH},,%y%m%d_%H%M%S)}_${EXTEN}_${CALLERID(num)}.gsm";
                    content += "\n  same => " + sipStep + ",MixMonitor(" + fstr + ",b)";
                    sipStep++;
                    //==================================================================
                    content += "\n  same => " + sipStep + ",Dial(" + dialTarget + "," + obj.sipPhoneRingTime + ")";
                    sipStep++;
                } else {
                    String dialTarget = "";
                    for (int i = 0; i < ringGroupList.size(); i++) {
                        if (i != 0) {
                            dialTarget += "&";
                        }
                        dialTarget += getIaxHead(slotType, slotCnt, obj.type, obj.slotCnt) + ringGroupList.get(i);
                    }
                    String fstr = "/home/" + GB.mainpbx_hostName + "/kevin/pbxSetExe/record/";
                    fstr += "${STRFTIME(${EPOCH},,%y%m%d_%H%M%S)}_${EXTEN}_${CALLERID(num)}.gsm";
                    content += "\n  same => " + sipStep + ",MixMonitor(" + fstr + ",b)";
                    sipStep++;
                    content += "\n  same => " + sipStep + ",Dial(" + dialTarget + "," + obj.sipPhoneRingTime + ")";
                    sipStep++;

                }
                if (obj.jmpGroup != null) {
                    ExGroupObj objGroup = GB.exGroupMap.get(obj.jmpGroup);
                    if (objGroup != null) {
                        for (int i = 0; i < objGroup.noList.size(); i++) {
                            String noStr = objGroup.noList.get(i);
                            if (noStr.equals(obj.no)) {
                                continue;
                            }
                            content += "\n  same => " + sipStep + ",GotoIf($[\"${CALLERID(num)}\" = \"" + noStr + "\"]?" + (sipStep + 2) + ")";
                            sipStep++;
                            content += "\n  same => " + sipStep + ",Dial(" + getIaxHead(slotType, slotCnt, obj.type, obj.slotCnt) + noStr + "," + obj.sipPhoneRingTime + ")";
                            sipStep++;
                            content += "\n  same => " + sipStep + ",NoOp()";
                            sipStep++;
                        }
                    } else {
                        if (obj.jmpGroup.length() > 0 && (!obj.jmpGroup.equals("all"))) {
                            String[] strB = obj.jmpGroup.split(",");
                            for (int i = 0; i < strB.length; i++) {
                                String noStr = strB[i].trim();
                                if (noStr.length() == 0) {
                                    continue;
                                }
                                if (noStr.equals(obj.no)) {
                                    continue;
                                }
                                content += "\n  same => " + sipStep + ",GotoIf($[\"${CALLERID(num)}\" = \"" + noStr + "\"]?" + (sipStep + 2) + ")";
                                sipStep++;
                                content += "\n  same => " + sipStep + ",Dial(" + getIaxHead(slotType, slotCnt, obj.type, obj.slotCnt) + noStr + "," + obj.sipPhoneRingTime + ")";
                                sipStep++;
                                content += "\n  same => " + sipStep + ",NoOp()";
                                sipStep++;

                            }
                        }

                    }
                }

                content += "\n  same => " + sipStep + ",Hangup()";
                sipStep++;
                content += "\n  same => " + sipStep + "(callSelf),Goto(canclePhone,10000,1)";
                sipStep++;
            }
            JSONArray jsArr;
            int len;

            //Broadcast
            content += "\n";
            content += "\n;Broadcast";
            content += "\n;=============================================";
            jsArr = (JSONArray) GB.paraSetMap.get("broadGroups");
            len = jsArr.length();
            for (int i = 0; i < len; i++) {
                String groupStr = jsArr.get(i).toString() + " ";
                String[] strA = groupStr.split("~");
                if (strA.length != 3) {
                    continue;
                }
                String[] strB = strA[1].split(",");
                String[] strC = strA[2].split(",");
                if (strB.length < 1) {
                    continue;
                }
                if (strC.length < 1) {
                    continue;
                }

                bstr = "";
                int next = 0;

                ExGroupObj groupObj = GB.exGroupMap.get(strC[0].trim());

                if (groupObj != null) {
                    String tmpStr = "";
                    for (String noStr : groupObj.noList) {
                        if (tmpStr.length() > 0) {
                            tmpStr += ",";
                        }
                        tmpStr += noStr;
                    }
                    strC = tmpStr.split(",");
                }

                for (int j = 0; j < strC.length; j++) {
                    ExNoObj obj = GB.exNoMap.get(strC[j].trim());
                    if (obj == null) {
                        continue;
                    }
                    if (next >= 1) {
                        bstr += "&";
                    }
                    next++;
                    bstr += getIaxHead(slotType, slotCnt, obj.type, obj.slotCnt);
                    bstr += obj.no;
                }

                if (bstr.equals("")) {
                    continue;
                }
                //            content += "\n  same => " + sipStep + ",GotoIf($[\"${CALLERID(num)}\" = \"" + noStr + "\"]?" + (sipStep + 2) + ")";
                if (i != 0) {
                    content += "\n";
                }
                content += "\nexten => " + strA[0].trim() + ",1,NoOp()";
                content += "\n  same => n,Answer()";

                if (strB[0].trim().equals("all")) {
                    content += "\n  same => n,GotoIf($[1 > 0]?broadEntry)";
                } else {
                    ExGroupObj group = GB.exGroupMap.get(strB[0].trim());
                    if (group != null) {
                        for (String noStr : group.noList) {
                            content += "\n  same => n,GotoIf($[\"${CALLERID(num)}\" = \"" + noStr + "\"]?" + "broadEntry" + ")";
                        }
                    } else {
                        for (int j = 0; j < strB.length; j++) {
                            content += "\n  same => n,GotoIf($[\"${CALLERID(num)}\" = \"" + strB[j].trim() + "\"]?" + "broadEntry" + ")";
                        }
                    }
                }

                content += "\n  same => n,Goto(errPermission,10000,1)";
                content += "\n  same => n(broadEntry),NoOp()";
                String fstr = "/home/" + GB.mainpbx_hostName + "/kevin/pbxSetExe/record/";
                fstr += "${STRFTIME(${EPOCH},,%y%m%d_%H%M%S)}_${EXTEN}_${CALLERID(num)}.gsm";
                content += "\n  same => " + "n" + ",MixMonitor(" + fstr + ",b)";
                content += "\n  same => n,Set(CALLERID(num)=*0*" + strA[0].trim() + ")";
                content += "\n  same => n,Page(" + bstr + ",i,10)";
                content += "\n  same => n,Hangup()";
            }
            content += "\n;=============================================";

            //groupCalls
            content += "\n";
            content += "\n;groupCalls";
            content += "\n;=============================================";
            jsArr = (JSONArray) GB.paraSetMap.get("groupCalls");
            len = jsArr.length();
            for (int i = 0; i < len; i++) {
                String groupStr = jsArr.get(i).toString() + " ";
                String[] strA = groupStr.split("~");
                if (strA.length != 3) {
                    continue;
                }
                String[] strB = strA[1].split(",");
                if (strB.length < 1) {
                    continue;
                }

                bstr = "";
                int next = 0;

                ExGroupObj groupObj = GB.exGroupMap.get(strB[0].trim());

                if (groupObj != null) {
                    String tmpStr = "";
                    for (String noStr : groupObj.noList) {
                        if (tmpStr.length() > 0) {
                            tmpStr += ",";
                        }
                        tmpStr += noStr;
                    }
                    strB = tmpStr.split(",");
                }

                for (int j = 0; j < strB.length; j++) {
                    ExNoObj obj = GB.exNoMap.get(strB[j].trim());
                    if (obj == null) {
                        continue;
                    }
                    if (next >= 1) {
                        bstr += "&";
                    }
                    next++;
                    bstr += getIaxHead(slotType, slotCnt, obj.type, obj.slotCnt);
                    bstr += obj.no;
                }

                if (bstr.equals("")) {
                    continue;
                }
                //            content += "\n  same => " + sipStep + ",GotoIf($[\"${CALLERID(num)}\" = \"" + noStr + "\"]?" + (sipStep + 2) + ")";
                if (i != 0) {
                    content += "\n";
                }
                content += "\nexten => " + strA[0].trim() + ",1,NoOp()";
                content += "\n  same => n,Answer()";

                String fstr = "/home/" + GB.mainpbx_hostName + "/kevin/pbxSetExe/record/";
                fstr += "${STRFTIME(${EPOCH},,%y%m%d_%H%M%S)}_${EXTEN}_${CALLERID(num)}.gsm";
                content += "\n  same => " + "n" + ",MixMonitor(" + fstr + ",b)";
                content += "\n  same => n,Dial(" + bstr + "," + strA[2].trim() + ")";
                content += "\n  same => n,Hangup()";
            }
            content += "\n;=============================================";

            //Conference
            content += "\n";
            content += "\n;Conference";
            content += "\n;=============================================";
            jsArr = (JSONArray) GB.paraSetMap.get("meetGroups");
            len = jsArr.length();
            for (int i = 0; i < len; i++) {
                String groupStr = jsArr.get(i).toString() + " ";
                String[] strA = groupStr.split("~");
                if (strA.length != 4) {
                    continue;
                }
                String[] strB = strA[2].split(",");
                String[] strC = strA[3].split(",");
                if (strB.length < 1) {
                    continue;
                }
                if (strC.length < 1) {
                    continue;
                }

                //            content += "\n  same => " + sipStep + ",GotoIf($[\"${CALLERID(num)}\" = \"" + noStr + "\"]?" + (sipStep + 2) + ")";
                if (i != 0) {
                    content += "\n";
                }
                content += "\nexten => " + strA[0].trim() + ",1,NoOp()";
                content += "\n  same => n,Answer()";
                content += "\n  same => n,GotoIf($[${GROUP_COUNT(1@${EXTEN})} > 0]?userMenber)";

                if (strB[0].trim().equals("all")) {
                    content += "\n  same => n,GotoIf($[1 > 0]?adminEntry)";
                } else {
                    ExGroupObj group = GB.exGroupMap.get(strB[0].trim());
                    if (group != null) {
                        for (String noStr : group.noList) {
                            content += "\n  same => n,GotoIf($[\"${CALLERID(num)}\" = \"" + noStr + "\"]?" + "adminEntry" + ")";
                        }
                    } else {
                        for (int j = 0; j < strB.length; j++) {
                            content += "\n  same => n,GotoIf($[\"${CALLERID(num)}\" = \"" + strB[j].trim() + "\"]?" + "adminEntry" + ")";
                        }
                    }
                }
                content += "\n  same => n,Goto(errPermission,10000,1)";

                content += "\n  same => n(adminEntry),NoOp()";
                content += "\n  same => n,Read(ConfPin,josn/pinPass," + strA[1].trim().length() + ",,2,5)";
                content += "\n  same => n,GotoIf($[\"${ConfPin}\" = \"" + strA[1].trim() + "\"]?" + "adminMeetEntry" + ")";
                content += "\n  same => n,Goto(pinError,10000,1)";

                content += "\n  same => n(userMenber),NoOp()";
                if (strC[0].trim().equals("all")) {
                    content += "\n  same => n,GotoIf($[1 > 0]?adminEntry)";
                } else {
                    ExGroupObj group = GB.exGroupMap.get(strB[0].trim());
                    if (group != null) {
                        for (String noStr : group.noList) {
                            content += "\n  same => n,GotoIf($[\"${CALLERID(num)}\" = \"" + noStr + "\"]?" + "adminEntry" + ")";
                        }
                    } else {
                        for (int j = 0; j < strC.length; j++) {
                            content += "\n  same => n,GotoIf($[\"${CALLERID(num)}\" = \"" + strC[j].trim() + "\"]?" + "adminEntry" + ")";
                        }
                    }
                }
                content += "\n  same => n,Goto(errPermission,10000,1)";
                content += "\n  same => n(adminMeetEntry),NoOp()";
                String fstr = "/home/" + GB.mainpbx_hostName + "/kevin/pbxSetExe/record/";
                fstr += "${STRFTIME(${EPOCH},,%y%m%d_%H%M%S)}_${EXTEN}_${CALLERID(num)}.gsm";
                content += "\n  same => " + "n" + ",MixMonitor(" + fstr + ",b)";
                content += "\n  same => n(meetEntry),NoOp()";
                content += "\n  same => n,Set(GROUP(${EXTEN})=1)";
                content += "\n  same => n,ConfBridge(" + strA[0].trim() + ",myconf" + strA[0].trim() + ",admin_user)";
                content += "\n  same => n,Hangup()";
            }

            content += this.getComExtensions();
            return content;
        } catch (Exception ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());

        }
        return null;
    }

    public String getFxoExtensions(String slotType, int slotCnt) {
        String bstr;
        String fstr;
        int sipStep;

        try {
            String processType = GB.paraSetMap.get("processType").toString();
            String content = "";
            content += "\n[general]";
            content += "\nstatic=yes";
            content += "\nwriteprotect=no";
            content += "\nclearglobalvars=no";
            content += "\n";
            content += "\n[globals]";
            content += "\nCONSOLE=Console/dsp";
            content += "\nIAXINFO=guest";
            content += "\nTRUNK=DAHDI/G2";
            content += "\nTRUNKMSD=1";

            content += "\n";
            content += "\n[from-pstn]";
            content += "\nexten => 10000,1,Answer()";
            content += "\n  same => n,Playback(hello-world)";
            content += "\n  same => n,Hangup()";

            for (String key : GB.exNoMap.keySet()) {
                ExNoObj obj = GB.exNoMap.get(key);
                if (obj.type.equals("fxo") && obj.slotCnt == slotCnt) {
                    if (obj.jmpNumber.length() != 0) {
                        content += "\n";
                        content += "\nexten => " + obj.no + ",1,NoOp()";
                        content += "\n  same => n,Answer()";
                        fstr = "/home/" + GB.mainpbx_hostName + "/kevin/pbxSetExe/record/";
                        fstr += "${STRFTIME(${EPOCH},,%y%m%d_%H%M%S)}_${EXTEN}_${CALLERID(num)}.gsm";
                        content += "\n  same => " + "n" + ",MixMonitor(" + fstr + ",b)";
                        content += "\n  same => n,Dial(DAHDI/G1/" + obj.jmpNumber + ")";
                        content += "\n  same => n,Hangup()";
                    }
                }
            }

            content += "\n";
            content += "\nexten => _*9.,1,NoOp()";
            content += "\n  same => n,Answer()";
            fstr = "/home/" + GB.mainpbx_hostName + "/kevin/pbxSetExe/record/";
            fstr += "${STRFTIME(${EPOCH},,%y%m%d_%H%M%S)}_${EXTEN:2}_${CALLERID(num)}.gsm";
            content += "\n  same => " + "n" + ",MixMonitor(" + fstr + ",b)";
            content += "\n  same => n,Dial(DAHDI/G1/${EXTEN:2})";
            content += "\n  same => n,Hangup()";

            content += "\n";
            content += "\n[from-dahdi]";
            content += "\nexten => s,1,NoOp()";
            content += "\n  same => 2,Answer()";
            content += "\n  same => 3,Read(DtmfIn,josn/inputExNumber,0,,2,2)";
            sipStep = 4;
            for (String key : GB.exNoMap.keySet()) {
                ExNoObj obj = GB.exNoMap.get(key);
                if (!obj.type.equals("fxo")) {
                    content += "\n";
                    content += "\n; Type:" + obj.type + ", slotCnt:" + obj.slotCnt + ", Name:" + obj.name + ", No:" + obj.no + ", Channel:" + obj.channel + ", jmpNumber:" + obj.jmpNumber;
                    content += "\n; ringGroup:" + obj.ringGroup;
                    content += "\n; jmpGroup:" + obj.jmpGroup;

                    content += "\n  same => " + sipStep + ",GotoIf($[\"${DtmfIn}\" != \"" + obj.no + "\"]?" + (sipStep + 2) + ")";
                    sipStep++;

                    String exten = obj.no;
                    String dialTarget = getIaxHead(slotType, slotCnt, obj.type, obj.slotCnt) + exten;

                    content += "\n  same => " + sipStep + ",Dial(" + dialTarget + "," + obj.sipPhoneRingTime + ")";
                    sipStep++;
                    content += "\n  same => " + sipStep + ",NoOp()";
                    sipStep++;
                }
            }

            content += "\n";
            content += "\n;Broadcast";
            content += "\n;=============================================";
            JSONArray jsArr;
            int len;
            jsArr = (JSONArray) GB.paraSetMap.get("broadGroups");
            len = jsArr.length();
            for (int i = 0; i < len; i++) {
                String groupStr = jsArr.get(i).toString() + " ";
                String[] strA = groupStr.split("~");
                if (strA.length != 3) {
                    continue;
                }
                content += "\n  same => " + sipStep + ",GotoIf($[\"${DtmfIn}\" != \"" + strA[0] + "\"]?" + (sipStep + 3) + ")";
                sipStep++;
                content += "\n  same => n,Set(CALLERID(name)=${CALLERID(num)})";
                sipStep++;
                content += "\n  same => " + sipStep + ",Dial(" + getIaxHead(slotType, slotCnt, "sip", 0) + strA[0] + "," + "30" + ")";
                sipStep++;
            }

            content += "\n";
            content += "\n;Conference Call";
            content += "\n;=============================================";
            jsArr = (JSONArray) GB.paraSetMap.get("meetGroups");
            len = jsArr.length();
            for (int i = 0; i < len; i++) {
                String groupStr = jsArr.get(i).toString() + " ";
                String[] strA = groupStr.split("~");
                if (strA.length != 4) {
                    continue;
                }
                content += "\n  same => " + sipStep + ",GotoIf($[\"${DtmfIn}\" != \"" + strA[0] + "\"]?" + (sipStep + 3) + ")";
                sipStep++;
                content += "\n  same => n,Set(CALLERID(name)=${CALLERID(num)})";
                sipStep++;
                content += "\n  same => " + sipStep + ",Dial(" + getIaxHead(slotType, slotCnt, "sip", 0) + strA[0] + "," + "30" + ")";
                sipStep++;
            }

            content += "\n  same => " + sipStep + ",Playback(josn/noExNumber)";
            sipStep++;
            content += "\n  same => " + sipStep + ",Goto(autocall,${EXTEN},3)";
            sipStep++;
            content += "\n  same => " + sipStep + ",Hangup()";

            //===================================================================================
            content += "\n";
            content += "\n[errPermission]";
            content += "\nexten => " + "10000" + ",1,NoOp()";
            content += "\n  same => n,Playback(josn/errorPermission)";
            content += "\n  same => n,Hangup()";
            //===================================================================================
            content += "\n";
            content += "\n[canclePhone]";
            content += "\nexten => " + "10000" + ",1,NoOp()";
            content += "\n  same => n,Hangup()";
            //===================================================================================
            content += "\n";
            content += "\n[pinError]";
            content += "\nexten => " + "10000" + ",1,NoOp()";
            content += "\n  same => n,Playback(josn/pinError)";
            content += "\n  same => n,Hangup()";
            return content;
        } catch (Exception ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());

        }
        return null;
    }

    public String getFxsExtensions(String slotType, int slotCnt) {
        String bstr;
        String fstr;
        int sipStep;

        try {
            String processType = GB.paraSetMap.get("processType").toString();
            String content = "";
            content += "\n[general]";
            content += "\nstatic=yes";
            content += "\nwriteprotect=no";
            content += "\nclearglobalvars=no";
            content += "\n";
            content += "\n[globals]";
            content += "\nCONSOLE=Console/dsp";
            content += "\nIAXINFO=guest";
            content += "\nTRUNK=DAHDI/G2";
            content += "\nTRUNKMSD=1";

            content += "\n";
            content += "\n[from-pstn]";

            for (String key : GB.exNoMap.keySet()) {
                ExNoObj obj = GB.exNoMap.get(key);
                if (obj.type.equals(slotType) && obj.slotCnt == slotCnt) {
                    content += "\n";
                    content += "\n; Type:" + obj.type + ", slotCnt:" + obj.slotCnt + ", Name:" + obj.name + ", No:" + obj.no + ", Channel:" + obj.channel + ", jmpNumber:" + obj.jmpNumber;
                    content += "\n; ringGroup:" + obj.ringGroup;
                    content += "\n; jmpGroup:" + obj.jmpGroup;
                    content += "\nexten => " + obj.no + ",1,NoOp()";
                    content += "\n  same => n,Answer()";
                    fstr = "/home/" + GB.mainpbx_hostName + "/kevin/pbxSetExe/record/";
                    fstr += "${STRFTIME(${EPOCH},,%y%m%d_%H%M%S)}_${EXTEN}_${CALLERID(num)}.gsm";
                    content += "\n  same => " + "n" + ",MixMonitor(" + fstr + ",b)";
                        String exten = "" + (obj.channel + 1);
                    if(slotType.equals("mag"))
                        exten = "" + (obj.channel + 5);
                    String dialTarget = getIaxHead(slotType, slotCnt, obj.type, obj.slotCnt) + exten;
                    content += "\n  same => n,Dial(" + dialTarget + "," + obj.sipPhoneRingTime + ")";
                    content += "\n  same => n,Hangup()";
                }
            }

            content += "\n";
            content += "\n[from-dahdi]";
            sipStep = 4;
            for (String key : GB.exNoMap.keySet()) {
                ExNoObj obj = GB.exNoMap.get(key);
                content += "\n";
                content += "\n; Type:" + obj.type + ", slotCnt:" + obj.slotCnt + ", Name:" + obj.name + ", No:" + obj.no + ", Channel:" + obj.channel + ", jmpNumber:" + obj.jmpNumber;
                content += "\n; ringGroup:" + obj.ringGroup;
                content += "\n; jmpGroup:" + obj.jmpGroup;
                if (obj.type.equals(slotType) && obj.slotCnt == slotCnt) {
                    content += "\nexten => " + obj.no + ",1,NoOp()";
                    content += "\n  same => n,Answer()";
                    fstr = "/home/" + GB.mainpbx_hostName + "/kevin/pbxSetExe/record/";
                    fstr += "${STRFTIME(${EPOCH},,%y%m%d_%H%M%S)}_${EXTEN}_${CALLERID(num)}.gsm";
                    content += "\n  same => " + "n" + ",MixMonitor(" + fstr + ",b)";
                    String exten = "" + (obj.channel + 1);
                    String dialTarget = getIaxHead(slotType, slotCnt, obj.type, obj.slotCnt) + exten;
                    content += "\n  same => n,Dial(" + dialTarget + "," + obj.sipPhoneRingTime + ")";
                    content += "\n  same => n,Hangup()";
                    continue;
                }
                content += "\nexten => " + obj.no + ",1,NoOp()";
                String exten = obj.no;
                String dialTarget = getIaxHead(slotType, slotCnt, obj.type, obj.slotCnt) + exten;
                content += "\n  same => n,Dial(" + dialTarget + "," + obj.sipPhoneRingTime + ")";
                content += "\n  same => n,Hangup()";

            }

            content += "\n";
            content += "\n;Broadcast";
            content += "\n;=============================================";
            JSONArray jsArr;
            int len;
            jsArr = (JSONArray) GB.paraSetMap.get("broadGroups");
            len = jsArr.length();
            for (int i = 0; i < len; i++) {
                String groupStr = jsArr.get(i).toString() + " ";
                String[] strA = groupStr.split("~");
                if (strA.length != 3) {
                    continue;
                }
                content += "\n";
                content += "\nexten => " + strA[0] + ",1,NoOp()";
                String exten = strA[0];
                String dialTarget = getIaxHead(slotType, slotCnt, "sip", 0) + exten;
                content += "\n  same => n,Dial(" + dialTarget + "," + "30" + ")";
                content += "\n  same => n,Hangup()";
            }

            content += "\n";
            content += "\n;Conference Call";
            content += "\n;=============================================";
            jsArr = (JSONArray) GB.paraSetMap.get("meetGroups");
            len = jsArr.length();
            for (int i = 0; i < len; i++) {
                String groupStr = jsArr.get(i).toString() + " ";
                String[] strA = groupStr.split("~");
                if (strA.length != 4) {
                    continue;
                }

                content += "\n";
                content += "\nexten => " + strA[0] + ",1,NoOp()";
                String exten = strA[0];
                String dialTarget = getIaxHead(slotType, slotCnt, "sip", 0) + exten;
                content += "\n  same => n,Dial(" + dialTarget + "," + "30" + ")";
                content += "\n  same => n,Hangup()";

            }

            content += "\n  same => " + sipStep + ",Playback(josn/noExNumber)";
            sipStep++;
            content += "\n  same => " + sipStep + ",Goto(autocall,${EXTEN},3)";
            sipStep++;
            content += "\n  same => " + sipStep + ",Hangup()";
            //===================================================================================
            content += this.getComExtensions();
            return content;

        } catch (Exception ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());

        }
        return null;
    }

    public String getT1sExtensions(String slotType, int slotCnt) {
        String bstr;
        String fstr;
        int sipStep;

        try {
            String processType = GB.paraSetMap.get("processType").toString();
            String content = "";
            content += "\n[general]";
            content += "\nstatic=yes";
            content += "\nwriteprotect=no";
            content += "\nclearglobalvars=no";
            content += "\n";
            content += "\n[globals]";
            content += "\nCONSOLE=Console/dsp";
            content += "\nIAXINFO=guest";
            content += "\nTRUNK=DAHDI/G2";
            content += "\nTRUNKMSD=1";

            content += "\n";
            content += "\n[from-pstn]";

            for (String key : GB.exNoMap.keySet()) {
                ExNoObj obj = GB.exNoMap.get(key);
                if (obj.type.equals(slotType) && obj.slotCnt == slotCnt) {
                    content += "\n";
                    content += "\n; Type:" + obj.type + ", slotCnt:" + obj.slotCnt + ", Name:" + obj.name + ", No:" + obj.no + ", Channel:" + obj.channel + ", jmpNumber:" + obj.jmpNumber;
                    content += "\n; ringGroup:" + obj.ringGroup;
                    content += "\n; jmpGroup:" + obj.jmpGroup;
                    content += "\nexten => " + obj.no + ",1,NoOp()";
                    content += "\n  same => n,Answer()";
                    fstr = "/home/" + GB.mainpbx_hostName + "/kevin/pbxSetExe/record/";
                    fstr += "${STRFTIME(${EPOCH},,%y%m%d_%H%M%S)}_${EXTEN}_${CALLERID(num)}.gsm";
                    content += "\n  same => " + "n" + ",MixMonitor(" + fstr + ",b)";
                    String exten = "G1" + (obj.channel + 1)+"/"+obj.no;
                    String dialTarget = getIaxHead(slotType, slotCnt, obj.type, obj.slotCnt) + exten;
                    content += "\n  same => n,Dial(" + dialTarget + "," + obj.sipPhoneRingTime + ")";
                    content += "\n  same => n,Hangup()";
                }
            }

            content += "\n";
            content += "\n[from-dahdi]";
            sipStep = 4;
            for (String key : GB.exNoMap.keySet()) {
                ExNoObj obj = GB.exNoMap.get(key);
                content += "\n";
                content += "\n; Type:" + obj.type + ", slotCnt:" + obj.slotCnt + ", Name:" + obj.name + ", No:" + obj.no + ", Channel:" + obj.channel + ", jmpNumber:" + obj.jmpNumber;
                content += "\n; ringGroup:" + obj.ringGroup;
                content += "\n; jmpGroup:" + obj.jmpGroup;
                if (obj.type.equals(slotType) && obj.slotCnt == slotCnt) {
                    content += "\nexten => " + obj.no + ",1,NoOp()";
                    content += "\n  same => n,Answer()";
                    fstr = "/home/" + GB.mainpbx_hostName + "/kevin/pbxSetExe/record/";
                    fstr += "${STRFTIME(${EPOCH},,%y%m%d_%H%M%S)}_${EXTEN}_${CALLERID(num)}.gsm";
                    content += "\n  same => " + "n" + ",MixMonitor(" + fstr + ",b)";
                    String exten = "G1" + (obj.channel + 1)+"/"+obj.no;
                    String dialTarget = getIaxHead(slotType, slotCnt, obj.type, obj.slotCnt) + exten;
                    content += "\n  same => n,Dial(" + dialTarget + "," + obj.sipPhoneRingTime + ")";
                    content += "\n  same => n,Hangup()";
                    continue;
                }
                content += "\nexten => " + obj.no + ",1,NoOp()";
                String exten = obj.no;
                String dialTarget = getIaxHead(slotType, slotCnt, obj.type, obj.slotCnt) + exten;
                content += "\n  same => n,Dial(" + dialTarget + "," + obj.sipPhoneRingTime + ")";
                content += "\n  same => n,Hangup()";

            }

            content += "\n";
            content += "\n;Broadcast";
            content += "\n;=============================================";
            JSONArray jsArr;
            int len;
            jsArr = (JSONArray) GB.paraSetMap.get("broadGroups");
            len = jsArr.length();
            for (int i = 0; i < len; i++) {
                String groupStr = jsArr.get(i).toString() + " ";
                String[] strA = groupStr.split("~");
                if (strA.length != 3) {
                    continue;
                }
                content += "\n";
                content += "\nexten => " + strA[0] + ",1,NoOp()";
                String exten = strA[0];
                String dialTarget = getIaxHead(slotType, slotCnt, "sip", 0) + exten;
                content += "\n  same => n,Dial(" + dialTarget + "," + "30" + ")";
                content += "\n  same => n,Hangup()";
            }

            content += "\n";
            content += "\n;Conference Call";
            content += "\n;=============================================";
            jsArr = (JSONArray) GB.paraSetMap.get("meetGroups");
            len = jsArr.length();
            for (int i = 0; i < len; i++) {
                String groupStr = jsArr.get(i).toString() + " ";
                String[] strA = groupStr.split("~");
                if (strA.length != 4) {
                    continue;
                }

                content += "\n";
                content += "\nexten => " + strA[0] + ",1,NoOp()";
                String exten = strA[0];
                String dialTarget = getIaxHead(slotType, slotCnt, "sip", 0) + exten;
                content += "\n  same => n,Dial(" + dialTarget + "," + "30" + ")";
                content += "\n  same => n,Hangup()";

            }

            content += "\n  same => " + sipStep + ",Playback(josn/noExNumber)";
            sipStep++;
            content += "\n  same => " + sipStep + ",Goto(autocall,${EXTEN},3)";
            sipStep++;
            content += "\n  same => " + sipStep + ",Hangup()";
            //===================================================================================
            content += this.getComExtensions();
            return content;

        } catch (Exception ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());

        }
        return null;
        
        
        
    }

    public String getMagExtensions(String slotType, int slotCnt) {
        return this.getFxsExtensions(slotType, slotCnt);
    }

    public String cmdPrg(String cmdstr) {
        String errStr = null;
        String[] strCmdA = cmdstr.split(" ");
        if (strCmdA[0].equals("wconf")) {
            System.out.println("\n" + cmdstr);

            if (strCmdA.length != 3) {
                return "Error: must input format like <wconf slotType slotCnt>";
            }
            String slotType = strCmdA[1];
            int slotCnt = Lib.str2int(strCmdA[2], 0);
            JSONArray jsArr;
            int inx;
            int len;
            try {
                /*
                BufferedReader reader = new BufferedReader(new FileReader("./paraSet.json"));
                StringBuilder stringBuilder = new StringBuilder();
                char[] buffer = new char[10];
                while (reader.read(buffer) != -1) {
                    stringBuilder.append(new String(buffer));
                    buffer = new char[10];
                }
                reader.close();
                 */
                String content = Lib.readFile("paraSet.json");

                GB.paraSetMap.clear();
                //String content = stringBuilder.toString();
                JSONObject jsPara = new JSONObject(content);
                Iterator<String> it = jsPara.keys();
                while (it.hasNext()) {
                    String key = it.next();
                    GB.paraSetMap.put(key, jsPara.get(key));
                }

                String bstr = GB.paraSetMap.get("sipPhoneRingTime").toString();
                int sipPhoneRingTime = Lib.str2int(bstr, 60);
                jsArr = (JSONArray) GB.paraSetMap.get("phExNos");
                len = jsArr.length();
                GB.exNoMap.clear();
                for (inx = 0; inx < len; inx++) {
                    String strPhExNo = jsArr.get(inx).toString() + " ";
                    String[] strA = strPhExNo.split("~");
                    if (strA.length < 8) {
                        continue;
                    }
                    ExNoObj exNoObj = new ExNoObj();
                    exNoObj.type = strA[0].trim();
                    exNoObj.slotCnt = Lib.str2int(strA[1].trim(), 0);
                    exNoObj.name = strA[2].trim();
                    exNoObj.no = strA[3].trim();
                    exNoObj.channel = Lib.str2int(strA[4].trim(), 0);
                    exNoObj.ringGroup = strA[5].trim();
                    exNoObj.jmpGroup = strA[6].trim();
                    exNoObj.jmpNumber = strA[7].trim();
                    exNoObj.loginPassword = strA[8].trim();
                    exNoObj.sipPhoneRingTime = Lib.str2int(strA[9].trim(), 30);
                    GB.exNoMap.put(exNoObj.no, exNoObj);
                }

                jsArr = (JSONArray) GB.paraSetMap.get("exNoGroups");
                len = jsArr.length();
                GB.exGroupMap.clear();
                for (inx = 0; inx < len; inx++) {
                    String strGroups = jsArr.get(inx).toString();
                    String[] strA = strGroups.split("~");
                    if (strA.length != 2) {
                        continue;
                    }
                    ExGroupObj exGroupObj = new ExGroupObj();
                    exGroupObj.name = strA[0].trim();
                    if (exGroupObj.name.length() == 0) {
                        continue;
                    }
                    String[] strB = strA[1].split(",");
                    for (int i = 0; i < strB.length; i++) {
                        String strNo = strB[i].trim();
                        if (strNo.length() > 0) {
                            exGroupObj.noList.add(strB[i].trim());
                        }
                    }
                    GB.exGroupMap.put(exGroupObj.name, exGroupObj);
                }

                String sPath = GB.asteriskConfPath;
                ArrayList<String> astr = Lib.readFileNames(sPath, "*.conf".split("~"));
                for (int i = 0; i < astr.size(); i++) {
                    File sourceFile = new File(sPath + "/" + astr.get(i));
                    sourceFile.delete();
                }

                File sourceFile, destFile;
                String wfName = "./extensions.conf";
                String contentStr = "";
                FileWriter fw;

                if (strCmdA[1].equals("sip")) {
                    contentStr = getSipExtensions(slotType, slotCnt);
                    wfName = "./extensions/sipExten/extensions.conf";
                    fw = new FileWriter(wfName);
                    fw.write(contentStr);
                    fw.flush();
                    fw.close();
                    sourceFile = new File(wfName);
                    destFile = new File(GB.asteriskConfPath + "/" + "extensions.conf");
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("copy" + wfName);
                    //
                    contentStr = getIaxConf("sip", Lib.str2int(strCmdA[2], 0));
                    wfName = "./extensions/sipExten/iax.conf";
                    fw = new FileWriter(wfName);
                    fw.write(contentStr);
                    fw.flush();
                    fw.close();
                    sourceFile = new File(wfName);
                    destFile = new File(GB.asteriskConfPath + "/" + "iax.conf");
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    //
                    contentStr = getPjsipConf();
                    wfName = "./extensions/sipExten/pjsip.conf";
                    fw = new FileWriter(wfName);
                    fw.write(contentStr);
                    fw.flush();
                    fw.close();
                    sourceFile = new File(wfName);
                    destFile = new File(GB.asteriskConfPath + "/" + "pjsip.conf");
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    //
                    contentStr = getConfbridgeConf();
                    wfName = "./extensions/sipExten/confbridge.conf";
                    fw = new FileWriter(wfName);
                    fw.write(contentStr);
                    fw.flush();
                    fw.close();
                    sourceFile = new File(wfName);
                    destFile = new File(GB.asteriskConfPath + "/" + "confbridge.conf");
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                if (strCmdA[1].equals("fxo")) {
                    contentStr = getFxoExtensions(slotType, slotCnt);
                    wfName = "./extensions/fxoExten/extensions.conf";
                    fw = new FileWriter(wfName);
                    fw.write(contentStr);
                    fw.flush();
                    fw.close();
                    sourceFile = new File(wfName);
                    destFile = new File(GB.asteriskConfPath + "/" + "extensions.conf");
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    //==================
                    contentStr = getIaxConf("fxo", Lib.str2int(strCmdA[2], 0));
                    wfName = "./extensions/fxoExten/iax.conf";
                    fw = new FileWriter(wfName);
                    fw.write(contentStr);
                    fw.flush();
                    fw.close();
                    sourceFile = new File(wfName);
                    destFile = new File(GB.asteriskConfPath + "/" + "iax.conf");
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    //==================
                    contentStr = getChanConf("fxo", Lib.str2int(strCmdA[2], 0));
                    wfName = "./extensions/fxoExten/chan_dahdi.conf";
                    fw = new FileWriter(wfName);
                    fw.write(contentStr);
                    fw.flush();
                    fw.close();
                    sourceFile = new File(wfName);
                    destFile = new File(GB.asteriskConfPath + "/" + "chan_dahdi.conf");
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                }

                if (strCmdA[1].equals("fxs")) {
                    contentStr = getFxsExtensions(slotType, slotCnt);
                    wfName = "./extensions/fxsExten/extensions.conf";
                    fw = new FileWriter(wfName);
                    fw.write(contentStr);
                    fw.flush();
                    fw.close();
                    sourceFile = new File(wfName);
                    destFile = new File(GB.asteriskConfPath + "/" + "extensions.conf");
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    //=====================
                    contentStr = getExPjsipConf();
                    wfName = "./extensions/fxsExten/pjsip.conf";
                    fw = new FileWriter(wfName);
                    fw.write(contentStr);
                    fw.flush();
                    fw.close();
                    sourceFile = new File(wfName);
                    destFile = new File(GB.asteriskConfPath + "/" + "pjsip.conf");
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    //
                    contentStr = getIaxConf("fxs", Lib.str2int(strCmdA[2], 0));
                    wfName = "./extensions/fxsExten/iax.conf";
                    fw = new FileWriter(wfName);
                    fw.write(contentStr);
                    fw.flush();
                    fw.close();
                    sourceFile = new File(wfName);
                    destFile = new File(GB.asteriskConfPath + "/" + "iax.conf");
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    //=====================
                    contentStr = getChanConf("fxs", Lib.str2int(strCmdA[2], 0));
                    wfName = "./extensions/fxsExten/chan_dahdi.conf";
                    fw = new FileWriter(wfName);
                    fw.write(contentStr);
                    fw.flush();
                    fw.close();
                    sourceFile = new File(wfName);
                    destFile = new File(GB.asteriskConfPath + "/" + "chan_dahdi.conf");
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                if (strCmdA[1].equals("t1s")) {
                    contentStr = getT1sExtensions(slotType, slotCnt);
                    wfName = "./extensions/t1sExten/extensions.conf";
                    fw = new FileWriter(wfName);
                    fw.write(contentStr);
                    fw.flush();
                    fw.close();
                    sourceFile = new File(wfName);
                    destFile = new File(GB.asteriskConfPath + "/" + "extensions.conf");
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    //=====================
                    contentStr = getExPjsipConf();
                    wfName = "./extensions/t1sExten/pjsip.conf";
                    fw = new FileWriter(wfName);
                    fw.write(contentStr);
                    fw.flush();
                    fw.close();
                    sourceFile = new File(wfName);
                    destFile = new File(GB.asteriskConfPath + "/" + "pjsip.conf");
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    //
                    contentStr = getIaxConf("t1s", Lib.str2int(strCmdA[2], 0));
                    wfName = "./extensions/t1sExten/iax.conf";
                    fw = new FileWriter(wfName);
                    fw.write(contentStr);
                    fw.flush();
                    fw.close();
                    sourceFile = new File(wfName);
                    destFile = new File(GB.asteriskConfPath + "/" + "iax.conf");
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    //=====================
                    //contentStr = getChanConf("t1s", Lib.str2int(strCmdA[2], 0));
                    //wfName = "./extensions/t1sExten/chan_dahdi.conf";
                    //fw = new FileWriter(wfName);
                    //fw.write(contentStr);
                    //fw.flush();
                    //fw.close();
                    //sourceFile = new File(wfName);
                    //destFile = new File(GB.asteriskConfPath + "/" + "chan_dahdi.conf");
                    //Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                if (strCmdA[1].equals("mag")) {
                    contentStr = getFxsExtensions(slotType, slotCnt);
                    wfName = "./extensions/magExten/extensions.conf";
                    fw = new FileWriter(wfName);
                    fw.write(contentStr);
                    fw.flush();
                    fw.close();
                    sourceFile = new File(wfName);
                    destFile = new File(GB.asteriskConfPath + "/" + "extensions.conf");
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    //=====================
                    contentStr = getExPjsipConf();
                    wfName = "./extensions/magExten/pjsip.conf";
                    fw = new FileWriter(wfName);
                    fw.write(contentStr);
                    fw.flush();
                    fw.close();
                    sourceFile = new File(wfName);
                    destFile = new File(GB.asteriskConfPath + "/" + "pjsip.conf");
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    //

                    contentStr = getIaxConf("mag", Lib.str2int(strCmdA[2], 0));
                    wfName = "./extensions/magExten/iax.conf";
                    fw = new FileWriter(wfName);
                    fw.write(contentStr);
                    fw.flush();
                    fw.close();
                    sourceFile = new File(wfName);
                    destFile = new File(GB.asteriskConfPath + "/" + "iax.conf");
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    //=====================
                    contentStr = getChanConf("mag", Lib.str2int(strCmdA[2], 0));
                    wfName = "./extensions/magExten/chan_dahdi.conf";
                    fw = new FileWriter(wfName);
                    fw.write(contentStr);
                    fw.flush();
                    fw.close();
                    sourceFile = new File(wfName);
                    destFile = new File(GB.asteriskConfPath + "/" + "chan_dahdi.conf");
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                System.out.println("ok\n");

            } catch (Exception ex) {
                ex.printStackTrace();

                return ex.toString();
            }
            return errStr;
        }
        if (cmdstr.equals("bypassSystemSecurity")) {
            //Base3.scla.netInf(1);
            return errStr;
        }
        if (cmdstr.equals("clearSystemSecurity")) {
            //Base3.scla.editNewDb("syssec", "");
            return errStr;
        }
        return "Command Not Found !!!";
    }

}

class ShlconTd extends Thread {

    PbxSet cla;
    int dis_connect_tim = 0;

    ShlconTd(PbxSet owner) {
        cla = owner;
    }

    @Override
    public void run() { // override Thread's run()
        //Test cla=Test.thisCla;
        for (;;) {
            if (cla.shlconTd_run_f == 1) {
                //==========================
                int ibuf;
                if (!GB.slotType.equals("none")) {
                    ibuf = Lib.ping(GB.mainpbx_ip);
                    if (ibuf == 0) {
                        dis_connect_tim = 0;
                        if (cla.sshShl == null) {
                            System.out.println("GB.mainpbx_ip =  "+GB.mainpbx_ip);
                            cla.sshShl = new Ssh(GB.mainpbx_ip, GB.mainpbx_user, GB.mainpbx_password);
                            cla.sshShl.connect();
                            if (cla.sshShl.connect_f == 0) {
                                cla.sshShl = null;
                            }

                        }
                    } else {
                        dis_connect_tim++;
                        if (dis_connect_tim >= 5) {
                            if (cla.sshShl != null) {
                                cla.sshShl.connect_f = 0;
                                cla.sshShl = null;
                            }
                        }
                    }
                }
                //==========================
                Lib.thSleep(100);
                if (cla.shlconTd_destroy_f == 1) {
                    break;
                }
            }
        }
    }
}

class ShlrxTd extends Thread {

    PbxSet cla;

    ShlrxTd(PbxSet owner) {
        cla = owner;
    }

    @Override
    public void run() { // override Thread's run()
        //Test cla=Test.thisCla;
        String str;
        int inData_f = 0;
        for (;;) {
            if (cla.shlrxTd_run_f == 1) {
                if (cla.sshShl != null && cla.sshShl.connect_f == 1) {
                    try {
                        int lineCnt = 0;

                        if (cla.sshShl.inStrm.available() > 0) {
                            byte[] data = new byte[cla.sshShl.inStrm.available()];
                            int nLen = cla.sshShl.inStrm.read(data);
                            if (nLen < 0) {
                            } else if (nLen != 0) {
                                /*
                                for(int i=0;i<nLen;i++){
                                    str=Lib.byteToHexString(data[i]);
                                    if(lineCnt!=0)
                                        str=","+str;
                                    else
                                        str="\n"+str;
                                    System.out.print(str);
                                    lineCnt+=1;
                                    if(lineCnt>=16)
                                        lineCnt=0;
                                }
                                 */
                                cla.vtshl.dataAvailable(data);            //<<debug
                                cla.shellRx.sshRx(cla.vtshl.incha);       //<<debug
                                inData_f = 1;
                            } else {
                            }
                        } else {
                            if (inData_f == 1) {
                                cla.shellRx.sshRx(null);       //<<debug
                            }
                            inData_f = 0;
                        }

                    } catch (IOException ex) {
                    }
                }
                Lib.thSleep(10);
                if (cla.shlrxTd_destroy_f == 1) {
                    break;
                }
            }
        }
    }
}

// unit =20ms
//at PhoneCs.java
abstract class ShellRx {

    public abstract void sshRx(String str);
}

abstract class SskUiRx {

    public abstract void socketRx(int format);
}

abstract class PbxSetRx {

    public abstract void sshRx(String str);
}

class ExNoObj {

    String type;
    String name;
    String no;
    int slotCnt;
    int channel;
    String jmpNumber;
    String ringGroup;
    String jmpGroup;
    String loginPassword;
    int sipPhoneRingTime;

    ExNoObj() {
    }
}

class ExGroupObj {

    String name;
    ArrayList<String> noList;

    ExGroupObj() {
        noList = new ArrayList<String>();
    }
}
