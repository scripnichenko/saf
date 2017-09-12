package modules.core;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriverException;
import ru.yandex.qatools.allure.annotations.Attachment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class StepUtil {
    protected SharedContext ctx;

    // PicoContainer injects class SharedContext
    public StepUtil (SharedContext ctx) {
        this.ctx = ctx;
    }

    public <T> void set(String textKey, T value) {
        //get test data
        HashMap<String, Object> testDataMap = ctx.obj.get("TestData",HashMap.class);

        //set test data value
        Class<T> typeKey = (Class<T>) getType(value);
        Log.debug("typeKey is " + typeKey.toString());
        testDataMap.put(textKey, value);
        Log.info("New object of type " + typeKey.toString() + " set with name " + textKey);
    }

    private <T> Class<?> getType(T value){
        if(value.getClass().getName().contains("String")){
            return String.class;
        }
        if(value.getClass().getName().contains("Double")){
            return Double.class;
        }
        if(value.getClass().getName().contains("Long")){
            return Long.class;
        }
        if(value.getClass().getName().contains("ArrayList")){
            return ArrayList.class;
        }
        if(value.getClass().getName().contains("HashMap")){
            return HashMap.class;
        }
        if(value.getClass().getName().contains("Boolean")){
            return Boolean.class;
        }
        Log.error("Type of object " + value.getClass().getName() + " not supported!");
        return null;
    }

    public void printTestData() {
        Log.info("Going to view the current state of test data");
        HashMap<String, Object> testDataMap = ctx.obj.get("TestData",HashMap.class);
        Log.info("--- start ---");
        for(Map.Entry<String, Object>entry : testDataMap.entrySet()) {
            String[] tmp = entry.getValue().getClass().getName().split(Pattern.quote(".")); // Split on period.
            String type = tmp[2];
            Log.info("(" + type + ")" + entry.getKey() + " = " + entry.getValue().toString());
        }
        Log.info("--- end ---");
    }

    public <T> T get(String path) {

        //if no dots in the path return just the storage
        if(!path.contains(".")){
            Object value = ctx.obj.get(path,HashMap.class);
            return (T) value;
        }

        //get hashmap with particular storage
        String [] tmp = path.split("\\.");
        Object value = ctx.obj.get(tmp[0],HashMap.class);
        String sTmp = "";
        for(int i=1;i<tmp.length;i++){
            sTmp = sTmp+"."+tmp[i];
        }

        //iterate over elements
        String [] elements = sTmp.substring(1).split("\\.");
        for(String element : elements) {
            String ename = element.split("\\[")[0];

            if(AbstractMap.class.isAssignableFrom(value.getClass())) {
                value = ( (AbstractMap<String, Object>) value).get(ename);

                if(element.contains("[")) {
                    if(List.class.isAssignableFrom(value.getClass())) {
                        Integer index = Integer.valueOf(element.substring(element.indexOf("[")+1, element.indexOf("]")) );
                        value = ((List<Object>) value).get(index);
                    }
                    else {
                        return null;
                    }
                }
            }
            else {
                return null;
            }
        }

        return (T) value;
    }


    public void sleep (Integer seconds) {
        try {
            Log.debug("Waiting for " + seconds + " seconds");
            Thread.sleep(seconds*1000);
        } catch (InterruptedException e) {
            Log.error("Something went wrong during sleep");
            Log.error(e.getMessage());
        }
    }

    public <T> T checkIfInputIsVariable(String input) {
        T result = (T) input;
        if(input.contains(".")) {
            result = get(input);
            Log.debug(input + " = " + result);
        }

        return result;
    }

    public <T> T checkIfInputIsVariableAndReturnString(String input) {
        T result = (T) input;
        if(input.contains(".")) {
            result = get(input);
            Log.debug(input + " = " + result);
        }

        return (T) result.toString();
    }

    @Attachment(value="{0}", type="{1}")
    public byte[] attachFileToReport(String name, String type, String path) {
        byte[] bytes = null;

        File file = new File(path);
        try {
            bytes = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        } catch (IOException e) {
            Log.error("File " + file.getAbsolutePath() + " not found!");
            Log.error(e.getMessage());
        }

        return bytes;
    }

    @Attachment(value="{0}", type="image/png")
    public byte[] attachScreenshotToReport(String name){
        byte[] screenshot = null;
        try {
            screenshot = ((TakesScreenshot) ctx.driver).getScreenshotAs(OutputType.BYTES);
        } catch (WebDriverException e) {
            Log.error("Screenshot can't be taken");
            Log.error(e.getMessage());
        }

        return screenshot;
    }

    @Attachment(value="{0}", type="text/plain")
    public String attachMessageToReport(String name, String message){
        return message;
    }

}