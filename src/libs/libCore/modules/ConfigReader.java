package libs.libCore.modules;

import com.google.gson.*;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.text.NumberFormat;
import java.util.*;

import static java.lang.Math.toIntExact;

@SuppressWarnings("unchecked")
public class ConfigReader {

    private Context scenarioCtx;
    private FileCore FileCore;

    public ConfigReader() {
        this.scenarioCtx = ThreadContext.getContext("Scenario");
        this.FileCore = scenarioCtx.get("FileCore",FileCore.class);
    }


    /**
     * Creates or updates existing storage based on the config file content
     * Content file structure will be checked in case it is not parsable
     * an error will be indicated in the log
     * Content shall be in the form of json
     *
     * @param path path to the config file
     */
    public void create(String path) {

        JsonElement root = null;
        JsonObject object = null;
        String sFile = null;
        HashMap<String, Object> result = null;
        File file = new File(path);

        sFile = FileCore.readToString(file);

        if ( sFile.contains("#include ") ) {
            List<String> lines = FileCore.readLines(file);
            String sFileWithoutIncludes = "";
            for (String line : lines){
                String tLine = line.trim();
                tLine = StringUtils.remove(tLine,'"');
                tLine = StringUtils.remove(tLine,"'");
                if ( tLine.startsWith("#include") && tLine.endsWith(".config")) {
                    String pathToIncludedConfigFile = tLine.substring(9);
                    pathToIncludedConfigFile = FileCore.getProjectPath() + File.separator + pathToIncludedConfigFile.trim();
                    Log.debug("Found included configuration file");
                    create(pathToIncludedConfigFile);
                } else {
                    sFileWithoutIncludes = sFileWithoutIncludes + line + System.getProperty("line.separator");
                }
            }
            sFileWithoutIncludes = sFileWithoutIncludes.trim();
            sFile = sFileWithoutIncludes;
        }

        if ( ! sFile.startsWith("{") ) {
            sFile = "{" + sFile + "}";
        }

        Log.debug("Reading configuration file " + path);

        //read the JSON file and make sure that format is correct
        try {
            root = new JsonParser().parse(sFile);
        } catch (JsonSyntaxException e) {
                Log.error("Typo in file " + file.getAbsolutePath(), e);
        }

        //read each entry and create new shared object for it
        if(root.isJsonObject()){
            Set<Map.Entry<String, JsonElement>> entries = root.getAsJsonObject().entrySet();//will return members of your object
            if ( entries.size() > 0 ) {
                for (Map.Entry<String, JsonElement> entry : entries) {
                    try {
                        object = root.getAsJsonObject().get(entry.getKey()).getAsJsonObject();
                    } catch (NullPointerException e) {
                        Log.error("No objects defined in configuration file!", e);
                    }

                    result = parseObject(object);

                    //if ctx object already exists overwrite/update its content else create new one
                    HashMap<String, Object> tmpMap = scenarioCtx.get(entry.getKey(), HashMap.class);
                    if (tmpMap == null) {
                        scenarioCtx.put(entry.getKey(), HashMap.class, result);
                    } else {
                        //tmpMap.putAll(result);
                        deepMerge(tmpMap, result);
                        scenarioCtx.put(entry.getKey(), HashMap.class, tmpMap);
                    }
                }
            } else {
                Log.debug("No objects found");
            }
        }
    }

    /**
     * Parses json object
     * helper function used to parse config files content
     * Parsing is done to HashMap
     * Values can be of type: Integer, Long, Double, String, Boolean, ListArray, HashMap
     *
     * @param object jsonObject extractet from the config file
     * @return HashMap
     *
     */
    //missing support for array of arrays!!!!
    private HashMap<String, Object> parseObject(JsonObject object) {
        Set<Map.Entry<String, JsonElement>> set = object.entrySet();
        Iterator<Map.Entry<String, JsonElement>> iterator = set.iterator();
        HashMap<String, Object> map = new HashMap<>();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonElement> entry = iterator.next();
            String key = entry.getKey();
            if (entry.getValue().isJsonPrimitive()) {
                if(entry.getValue().getAsJsonPrimitive().isNumber()){
                    Number num = null;
                    try {
                        num = NumberFormat.getInstance(Locale.getDefault()).parse(entry.getValue().getAsString());
                        if ( num instanceof Long ) {
                            Long tVal = (Long) num;
                            try {
                                int iVal = toIntExact(tVal);
                                num = iVal;
                            } catch (ArithmeticException e) {
                                //do nothing just return Long
                            }
                        }
                    } catch (Exception e) {
                        Log.error("Not able to parse String to Number for " + key + " : " +
                                entry.getValue().getAsString(), e);
                    }
                    if (num != null){
                        map.put(key, num);
                    }
                }else if(entry.getValue().getAsJsonPrimitive().isString()){
                    map.put(key, entry.getValue().getAsString());
                }else if(entry.getValue().getAsJsonPrimitive().isBoolean()){
                    map.put(key, entry.getValue().getAsBoolean());
                }else{
                    Log.warn("Didn't recognized type of primitive data. Going to put null value!");
                    map.put(key, null);
                }
            } else if (entry.getValue().isJsonArray()){
                ArrayList<Object> tmpArray = new ArrayList();
                for (int i = 0; i < entry.getValue().getAsJsonArray().size(); i++) {
                    if(entry.getValue().getAsJsonArray().get(i).isJsonPrimitive()){
                        if(entry.getValue().getAsJsonArray().get(i).getAsJsonPrimitive().isBoolean()){
                            tmpArray.add(entry.getValue().getAsJsonArray().get(i).getAsBoolean());
                        }else if(entry.getValue().getAsJsonArray().get(i).getAsJsonPrimitive().isString()){
                            tmpArray.add(entry.getValue().getAsJsonArray().get(i).getAsString());
                        }else if(entry.getValue().getAsJsonArray().get(i).getAsJsonPrimitive().isNumber()){
                            Number num = null;
                            try {
                                num = NumberFormat.getInstance(Locale.getDefault()).parse(entry.getValue().getAsJsonArray().get(i).getAsString());
                                if ( num instanceof Long ) {
                                    Long tVal = (Long) num;
                                    try {
                                        int iVal = toIntExact(tVal);
                                        num = iVal;
                                    } catch (ArithmeticException e) {
                                        //do nothing just return Long
                                    }
                                }
                            } catch (Exception e) {
                                Log.error("Not able to parse String to Number for " +
                                        key + " : " +
                                        entry.getValue().getAsJsonArray().get(i).getAsString(), e);
                            }
                            if (num != null){
                                tmpArray.add(num);
                            }
                        }else{
                            Log.warn("Didn't recognized type of data in an array. Going to put null value!");
                            tmpArray.add(null);
                        }
                    } else if (entry.getValue().getAsJsonArray().get(i).isJsonObject()) {
                        HashMap<String, Object> tMap = parseObject(entry.getValue().getAsJsonArray().get(i).getAsJsonObject());
                        tmpArray.add(tMap);
                    }
                }
                map.put(key, tmpArray);
            }else if(entry.getValue().isJsonObject()){
                HashMap<String, Object> tMap = parseObject(entry.getValue().getAsJsonObject());
                map.put(key, tMap);
            }
            else {
                Log.warn("Didn't recognized type of object data. Going to put null value!");
                map.put(key, null);
            }
        }
        return map;
    }


    // This is fancier than Map.putAll(Map)
    // https://stackoverflow.com/questions/25773567/recursive-merge-of-n-level-maps

    /**
     * helper method used to merge maps together. Recursion is used to handle nested maps.
     *
     * @param original
     * @param newMap
     * @return
     */
    private Map deepMerge(Map original, Map newMap) {
        for (Object key : newMap.keySet()) {
            if (newMap.get(key) instanceof Map && original.get(key) instanceof Map) {
                Map originalChild = (Map) original.get(key);
                Map newChild = (Map) newMap.get(key);
                original.put(key, deepMerge(originalChild, newChild));
            } else if (newMap.get(key) instanceof List && original.get(key) instanceof List) {
                List originalChild = (List) original.get(key);
                List newChild = (List) newMap.get(key);
                for (Object each : newChild) {
                    if (!originalChild.contains(each)) {
                        originalChild.add(each);
                    }
                }
            } else {
                original.put(key, newMap.get(key));
            }
        }
        return original;
    }


}