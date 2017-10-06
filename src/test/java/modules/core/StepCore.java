package modules.core;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import ru.yandex.qatools.allure.annotations.Attachment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.*;

public class StepCore {

    private SharedContext ctx;
    private FileCore FileCore;

    // PicoContainer injects class SharedContext
    public StepCore(SharedContext ctx) {
        this.ctx = ctx;
        this.FileCore = ctx.Object.get("FileCore",FileCore.class);
    }

    /**
     * Waits defined amount of time in seconds
     *
     * @param seconds number of seconds to wait
     */
    public void sleep (Integer seconds) {
        try {
            Log.debug("Waiting for " + seconds + " seconds");
            Thread.sleep(seconds*1000);
        } catch (InterruptedException e) {
            Log.error("", e);
        }
    }

    /**
     * returns type of value
     * helper function
     *
     * @param value object value
     *
     * @return type of value
     */
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

    /**
     * Checks if string provided as an input to the step def is actually a key in the storage
     * Returns input value or value extracted from storage.
     * Please note that in this case type of input is String but returned value can be one of
     * String, Double, Long, Boolean
     *
     * @param input key in the storage or value
     *
     * @return value from storage or input
     */
    public <T> T checkIfInputIsVariable(String input) {
        T result = (T) input;
        Storage Storage = ctx.Object.get("Storage", Storage.class);
        T tmp = Storage.get(input);

        //check if String contains boolean
        if ( BooleanUtils.toBooleanObject(input) != null ) {
            Boolean b = BooleanUtils.toBoolean(input);
            result = (T) b;
            Log.debug("Converted String " + input + " to Boolean");
        }

        //check if String contains number
        if(NumberUtils.isNumber(input)){
            Number num = null;
            try {
                num = NumberFormat.getInstance(Locale.getDefault()).parse(input);
            } catch (Exception e) {
                Log.debug("Checking if String contains a numeric value " + input);
                Log.error("Not able to parse String to Number for " + input, e);
            }
            Class<T> typeKey = (Class<T>) getType(num);
            result = typeKey.cast(num);
            Log.debug("Converted String " + input + " to number");
        }

        if ( tmp != null ){
            result = tmp;
            Log.debug("Converted element from storage: " + input + " to " + result);
        }

        return result;
    }

    public void compareWithTemplate(String templateName, String pathToResults) {
        Log.debug("Template comparison started");
        Boolean result = false;

        String templatePath = searchForTemplate(templateName);

        File template = new File(templatePath);
        File results = new File(pathToResults);

        String sFile = FileCore.readToString(template);
        String sResults = FileCore.readToString(results);

        //evaluate the template
        String templateAfteEval = replaceInTemplate(sFile);

        //attach template after evaluation to the report
        File temp = FileCore.createTempFile(templateName,"template");
        FileCore.appendToFile(temp, templateAfteEval);
        String tempPath = temp.getAbsolutePath();
        attachFileToReport(templateName + ".template","text/plain",tempPath);

        //trim template content and string to compare
        String templateContent = templateAfteEval.trim().replaceAll("\\s+","");
        String resultToCompare = sResults.trim().replaceAll("\\s+","");

        //compare ignoring white spaces
        if ( ! resultToCompare.matches(templateContent) ) {
            Log.error("Template comparison failed!");
        }
    }

    public File evaluateTemplate(String templateName) {
        String templatePath = searchForTemplate(templateName);

        File template = new File(templatePath);
        String sFile = FileCore.readToString(template);

        //evaluate the template
        String templateAfteEval = replaceInTemplate(sFile);

        //attach template after evaluation to the report
        File temp = FileCore.createTempFile(templateName,"template");
        FileCore.appendToFile(temp, templateAfteEval);
        String tempPath = temp.getAbsolutePath();
        attachFileToReport(templateName + ".template","text/plain",tempPath);

        return temp;

    }

    private String replaceInTemplate (String input) {
        Integer beignIdx = input.indexOf("${");
        Integer endIx = input.indexOf("}", beignIdx);

        if (beignIdx != -1) {
            if ( endIx == -1 ){
                Log.error("Typo in template! Missing closing bracket }. Can't do variable substitution!");
            }

            String toReplace = input.substring(beignIdx+2, endIx);
            String toCheck = toReplace;
            if ( toReplace.startsWith("ctx.") ){
                toCheck = toReplace.substring(4);
            }
            String result = checkIfInputIsVariable(toCheck).toString();

            if (  ! toReplace.equals("ctx." + result) ) {
                return replaceInTemplate(input.replace("${" + toReplace + "}", result));
            }
        }

        return input;
    }

    /**
     * Returns paths to to template file with particular name
     * Search is done in local and global templates directories
     *
     * @param templateName String, name of the template file without extension
     * @return templatePath String, path to the template file
     */
    public String searchForTemplate(String templateName) {
        //find global template dir
        String projectDir = FileCore.getProjectPath();
        String globalTemplateDir = projectDir + File.separator + "template";

        //find local template dir
        String localDir = ctx.Object.get("FeatureFileDir", String.class);
        String localTemplateDir = localDir + File.separator + "template";

        //search for template first in local dir
        Log.debug("Looking for template " + templateName + " in " + localTemplateDir);
        ArrayList<String> templates = FileCore.searchForFile(localTemplateDir,templateName + ".template");

        //if local template not found search for it in global dir
        if ( templates.size() < 1 ) {
            Log.debug("Looking for template " + templateName + " in " + globalTemplateDir);
            templates = FileCore.searchForFile(globalTemplateDir,templateName + ".template");
        }

        if ( templates.size() < 1 ) {
            Log.error("Template " + templateName + ".template was not found!");
        }

        //return the template if multiple files found return just the first one!
        String templatePath = templates.get(0);
        Log.debug("Template found in " + templatePath);

        return templatePath;
    }

    public String applyPositiveFilter (File input, List<String> filters) {

        if ( filters == null ) {
            Log.error("List of positive filters null!");
        }
        if ( filters.size() < 1 ) {
            Log.error("List of positive filters is empty!");
        }

        String output = "";
        List<String> lines = FileCore.readLines(input);

        String sFilter = "";
        for (String filter : filters) {
            sFilter = sFilter + ", " + filter;
        }

        String n = System.lineSeparator();

        Log.debug("Going to apply positive filter [" + sFilter.substring(1) + "]");
        for ( String line : lines ) {
            for ( String filter : filters) {
                if ( line.contains(filter) ) {
                    output = output + line + n;
                }
            }
        }

        output = output.trim();

        return output;
    }


    public String applyNegativeFilter (File input, List<String> filters) {

        if ( filters == null ) {
            Log.error("List of negative filters null!");
        }
        if ( filters.size() < 1 ) {
            Log.error("List of negative filters is empty!");
        }

        String output = "";
        List<String> lines = FileCore.readLines(input);

        String sFilter = "";
        for (String filter : filters) {
            sFilter = sFilter + ", " + filter;
        }

        String n = System.lineSeparator();

        Log.debug("Going to apply negative filter [" + sFilter.substring(1) + "]");
        for ( String line : lines ) {
            Boolean isMatch = false;
            for ( String filter : filters) {
                if ( line.contains(filter) ) {
                    isMatch = true;
                }
            }
            if ( ! isMatch ) {
                output = output + line + n;
            }
        }

        output = output.trim();

        return output;
    }

    public String applyBlockFilter (File input, List<Map<String, String>> filters) {

        if ( filters == null ) {
            Log.error("List of block filters null!");
        }
        if ( filters.size() < 1 ) {
            Log.error("List of block filters is empty!");
        }

        String output = "";
        String n = System.lineSeparator();
        List<String> lines = FileCore.readLines(input);

        for ( Map<String, String> filter : filters) {
            Boolean isMatch = false;
            String begin = filter.get("begin");
            String end = filter.get("end");

            if (begin == null || begin == "") {
                Log.error("begin keyword of block filter " + filter + " null or empty!");
            }

            if (end == null || end == "") {
                Log.error("end keyword of block filter " + filter + " null or empty!");
            }

            for ( String line : lines ) {
                if ( line.contains( end ) ) {
                    output = output + line + n;
                    isMatch = false;
                }
                if ( line.contains( begin ) ) {
                    isMatch = true;
                    }
                if ( isMatch ) {
                    output = output + line + n;
                }
            }

        }

        output = output.trim();

        return output;
    }

    /**
     * Attaches file to the report
     *
     * @param name name of the file to be displayed in the report
     * @param type type of file like text/plain or application/pdf etc.
     * @param path path to the file
     */
    @Attachment(value="{0}", type="{1}")
    public byte[] attachFileToReport(String name, String type, String path) {
        byte[] bytes = null;

        File file = new File(path);
        try {
            bytes = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        } catch (IOException e) {
            Log.error( "File " + file.getAbsolutePath() + " not found!", e );
        }

        Log.debug("File " + path + " with name " + name + " attached to report");

        return bytes;
    }

    /**
     * Attaches screenshot to the report
     *
     * @param name name of the screenshot
     */
    @Attachment(value="{0}", type="image/png")
    public byte[] attachScreenshotToReport(String name, byte[] screenshot){
        String tName = StringUtils.deleteWhitespace(name);
        Log.debug("Screenshot with name " + tName + " attached to report");
        return screenshot;
    }

    /**
     * Attaches text to the report
     *
     * @param name of the text to be displayed in the report
     * @param message content of the text to be displayed in the report
     */
    @Attachment(value="{0}", type="text/plain")
    public String attachMessageToReport(String name, String message){
        Log.debug("Message with name " + name + " attached to report");
        return message;
    }

}