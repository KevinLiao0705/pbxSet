package base3;

import org.json.JSONObject;

public class PbxChannel {
    public String channel;
    public String location;
    public int status=0;
    public String application;
  
    public PbxChannel() {
    }
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("channel", channel);
            json.put("status", status);
            json.put("location", location);
            json.put("application", application);
        } catch (Exception ex) {
            return null;
        }
        return json;
    }
}