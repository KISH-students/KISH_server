package org.kish.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.kish.MainLogger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Set;

import static org.kish.KishServer.GSON;
import static org.kish.KishServer.PRETTY_GSON;

@SuppressWarnings("unchecked")
public class Config extends LinkedHashMap<String, Object>{
    public final String fileName;

    private final boolean isLoggingEnabled = true;
    private final boolean saveWithPrettyGson = true;
    private boolean isLoaded = false;

    public Config(String fileName) {
        this.fileName = fileName;
        this.reload();
    }

    public void reload(){
        File jsonFile = new File(fileName);
        if(this.isLoggingEnabled) MainLogger.warn("Config 불러오는 중 : " + jsonFile.getAbsolutePath());

        try {
            String json;
            if (!jsonFile.exists()) {
                json = "{}";
            } else {
                json = FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8);
            }
            JSONObject jsonObject;
            jsonObject = (JSONObject) (new JSONParser().parse(json));
            this.putAll(jsonToMap(jsonObject));
        } catch (ParseException | IOException e) {
            MainLogger.error("Config 준비 중 오류가 발생하였습니다.", e);
            return;
        }

        for (ConfigOption item : ConfigOption.values()) {
            if(!this.containsKey(item.getKey())) this.put(item.getKey(), item.getDefValue());
        }

        this.isLoaded = true;
        save();
    }

    public LinkedHashMap<String, Object> jsonToMap(JSONObject jsonObject){
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for(String key : (Set<String>) jsonObject.keySet()){
            Object data = jsonObject.get(key);
            if(data instanceof Long) this.put(key, ((Long) data).intValue());
             map.put(key, data);
        }
        return map;
    }

    public void deleteFile(){
        File file = new File(fileName);
        if(!file.delete()) MainLogger.warn("Could not delete : " + file.getAbsolutePath());
        else if (isLoggingEnabled) MainLogger.warn("제거됨 : " + fileName);
    }

    public void save(){
        if(!this.isLoaded){
            MainLogger.error("DB가 정상적으로 로드되지 않아 데이터 손실 방지를 위해 저장되지 않습니다.");
            return;
        }
        if(isLoggingEnabled) MainLogger.warn("저장하는 중 : " + fileName);

        /*JSONObject jsonObject = new JSONObject();
        for(String key : this.keySet()){
            jsonObject.put(key, this.get(key));
        }*/
        try {
            if(saveWithPrettyGson){
                FileUtils.write(new File(fileName), PRETTY_GSON.toJson(this), StandardCharsets.UTF_8);
            }else {
                FileUtils.write(new File(fileName), GSON.toJson(this), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            MainLogger.error("DB 저장중 오류가 발생하였습니다.", e);
        }
    }

    public Object get(ConfigOption option) {
        return super.get(option.getKey());
    }

    public long getLong(String k){
        return (long) this.get(k);
    }

    public boolean isLoaded() { return isLoaded; }

    /*private void runListener(){
        if(this.dataChangeListener == null) return;
        this.dataChangeListener.run();
    }*/
}
