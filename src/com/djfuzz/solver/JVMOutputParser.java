package com.djfuzz.solver;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.Match;
import fj.P;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.djfuzz.process.JVMExecutor.SPLIT_STRING;

/**
 * Created by Yicheng Ouyang on 2021/1/7
 */
public class JVMOutputParser implements OutputParser {

    private static final String EXECUTED_LINE_PATTERN =
            ".*\\*\\*\\*\\* Executed Line: \\*\\*\\*\\* \\d+ \\*\\*\\*\\*.*";
    // 1) testCodeSource(org.apache.tools.ant.AntClassLoaderTest)
    private static final String JUNIT_ERR_LINE_PATTERN = "\\d+\\) .*\\(.*\\)";
    private static final String COMMON_EXCEPTION_KEYWORD_PATTERN = "[\\s\\S]*?[^A-Za-z]([A-Za-z]+?Exception)[^A-Za-z][\\s\\S]*";
    private static final String COMMON_ERROR_KEYWORD_PATTERN = "[\\s\\S]*?[^A-Za-z]([A-Za-z]+?Error)[^A-Za-z][\\s\\S]*";
    private static final String COMMON_FAILURE_KEYWORD_PATTERN = "[\\s\\S]*?[^A-Za-z]([A-Za-z]+?Failure)[^A-Za-z][\\s\\S]*";

    private static String[] outputMessagePattern = new String[]{
//            "[\\s\\S]*ClassFormatError[\\s\\S]*",
//            "[\\s\\S]*ClassNotFoundException[\\s\\S]*",
//            "[\\s\\S]*NoClassDefFoundError[\\s\\S]*",
//            "[\\s\\S]*VerifyError[\\s\\S]*",
            "[\\s\\S]*Main method not found[\\s\\S]*",
//            "[\\s\\S]*UnsupportedClassVersionError[\\s\\S]*",
            "[\\s\\S]*Could not find or load main class[\\s\\S]*",
//            "[\\s\\S]*ClassCircularityError[\\s\\S]*",
//            "[\\s\\S]*IncompatibleClassChangeError[\\s\\S]*",
//            "[\\s\\S]*UnsatisfiedLinkError[\\s\\S]*",
//            "[\\s\\S]*ExceptionInInitializerError[\\s\\S]*",
//            "[\\s\\S]*FileNotFoundException[\\s\\S]*",
//            "[\\s\\S]*NullPointerException[\\s\\S]*",
//            "[\\s\\S]*IllegalMonitorStateException[\\s\\S]*",
    };

    private static String[] outputType = new String[]{
//            "ClassFormatError",
//            "ClassNotFoundException",
//            "NoClassDefFoundError",
//            "VerifyError",
            "Main method not found",
//            "UnsupportedClassVersionError",
            "Could not find or load main class",
//            "ClassCircularityError",
//            "IncompatibleClassChangeError",
//            "UnsatisfiedLinkError",
//            "ExceptionInInitializerError",
//            "FileNotFoundException",
//            "NullPointerException",
//            "IllegalMonitorStateException"
    };

    // Todo: Error msg for each test is print in stdout by JUnit
    @Override
    public JVMOutputResult parseOutput(String output){

        // this kind of split keeps empty string.
        String[] tmp = output.split(Pattern.quote(SPLIT_STRING), -1);
        if (tmp.length != 3){
            System.out.println("Argument JVMoutput has wrong format!");
            return null;
        }
        String jvmTag = tmp[0];
        String stdout = tmp[1];
        String stderr = tmp[2];
        JVMOutputResult outputResult = new JVMOutputResult();

        outputResult.jvmTag = jvmTag;

        // parse the stdout into liveInstructions and clean output
        String[] outLines = stdout.split("\n");
        StringBuilder outSb = new StringBuilder();
        for (String line:outLines){
            if (Pattern.matches(EXECUTED_LINE_PATTERN, line)){
                if (!outputResult.liveInstructions.contains(line))
                    outputResult.liveInstructions.add(line);
            }else{
                outSb.append(line + "\n");
            }
        }
        String outSbString = outSb.toString();
//        outputResult.out = outSb.toString();

        // Junit mode
        if (outLines[0].contains("JUnit version")){
            StringBuilder junitOutSb = new StringBuilder();
            StringBuilder junitErrSb = new StringBuilder();
            outLines = outSbString.split("\n");
            boolean startFailureMsg = false;
            StringBuilder tmpSb = new StringBuilder();
            for (String line: outLines){
                if (!startFailureMsg){
                    if (line.startsWith("Time: ")){
                        startFailureMsg = true;
                        continue;
                    }else{
                        junitOutSb.append(line);
                    }
                }else{
                    if (Pattern.matches(JUNIT_ERR_LINE_PATTERN, line)){
                        junitErrSb.append(parseErrStrByKeyword(tmpSb.toString()) + "\n");
                        tmpSb.setLength(0);
                        tmpSb.append(line);
                    }else{
                        tmpSb.append(line);
                    }
                }
            }
            junitErrSb.append(parseErrStrByKeyword(tmpSb.toString()) + "\n");
            outputResult.out = junitOutSb.toString();
            outputResult.parsedErr = junitErrSb.toString();
        }else{
            outputResult.out = outSb.toString();
            outputResult.parsedErr = parseErrStrByKeyword(stderr);
        }

        return outputResult;
    }

    public static String parseErrStrByKeyword(String errStr){
        Pattern errorPattern = Pattern.compile(COMMON_ERROR_KEYWORD_PATTERN);
        Matcher errorMatcher = errorPattern.matcher(errStr);
        if (errorMatcher.find()){
            return errorMatcher.group(1);
        }

        Pattern exceptionPattern = Pattern.compile(COMMON_EXCEPTION_KEYWORD_PATTERN);
        Matcher exceptionMatcher = exceptionPattern.matcher(errStr);
        if (exceptionMatcher.find()){
            return exceptionMatcher.group(1);
        }

        Pattern failurePattern = Pattern.compile(COMMON_FAILURE_KEYWORD_PATTERN);
        Matcher failureMatcher = failurePattern.matcher(errStr);
        if (failureMatcher.find()){
            return failureMatcher.group(1);
        }

        for(int i = 0; i < outputMessagePattern.length; i++){
            if (Pattern.matches(outputMessagePattern[i], errStr)){
                return outputType[i];
            }
        }

        return errStr;
    }

    public static void main(String[] args) {
        try {
//            File file = new File("./src/com/djfuzz/solver/sample.txt");
//            FileInputStream fis = new FileInputStream(file);
//            byte[] data = new byte[(int) file.length()];
//            fis.read(data);
//            fis.close();
//
//            String str = new String(data, "UTF-8");
//            JVMOutputResult outputResult = new JVMOutputParser().parseOutput(str);


//            String out = "jvm1||****||out\n" + "||****||err";
            String out = "openjdk8||****||JUnit version 4.12\n" +
                    ".<org.apache.tools.zip.ZipOutputStream: void <clinit>()> **** Executed Line: **** 0 **** <org.apache.tools.zip.ZipOutputStream: java.lang.String DEFAULT_ENCODING> = null\n" +
                    "<org.apache.tools.zip.ZipOutputStream: org.apache.tools.zip.ZipLong toDosTime(java.util.Date)> **** Executed Line: **** 1 **** $r1 = staticinvoke <org.apache.tools.zip.ZipUtil: org.apache.tools.zip.ZipLong toDosTime(java.util.Date)>(r0)\n" +
                    ".<org.apache.tools.zip.ZipOutputStream: long adjustToLong(int)> **** Executed Line: **** 1 **** $l1 = staticinvoke <org.apache.tools.zip.ZipUtil: long adjustToLong(int)>(i0)\n" +
                    "<org.apache.tools.zip.ZipOutputStream: long adjustToLong(int)> **** Executed Line: **** 1 **** $l1 = staticinvoke <org.apache.tools.zip.ZipUtil: long adjustToLong(int)>(i0)\n" +
                    "<org.apache.tools.zip.ZipOutputStream: long adjustToLong(int)> **** Executed Line: **** 1 **** $l1 = staticinvoke <org.apache.tools.zip.ZipUtil: long adjustToLong(int)>(i0)\n" +
                    "\n" +
                    "Time: 0.069\n" +
                    "\n" +
                    "OK (2 tests)\n||****||";

            String out2 = "openjdk9||****||JUnit version 4.12\n" +
                    ".<org.apache.tools.zip.ZipOutputStream: void <clinit>()> **** Executed Line: **** 0 **** <org.apache.tools.zip.ZipOutputStream: java.lang.String DEFAULT_ENCODING> = null\n" +
                    "<org.apache.tools.zip.ZipOutputStream: org.apache.tools.zip.ZipLong toDosTime(java.util.Date)> **** Executed Line: **** 1 **** $r1 = staticinvoke <org.apache.tools.zip.ZipUtil: org.apache.tools.zip.ZipLong toDosTime(java.util.Date)>(r0)\n" +
                    ".<org.apache.tools.zip.ZipOutputStream: long adjustToLong(int)> **** Executed Line: **** 1 **** $l1 = staticinvoke <org.apache.tools.zip.ZipUtil: long adjustToLong(int)>(i0)\n" +
                    "<org.apache.tools.zip.ZipOutputStream: long adjustToLong(int)> **** Executed Line: **** 1 **** $l1 = staticinvoke <org.apache.tools.zip.ZipUtil: long adjustToLong(int)>(i0)\n" +
                    "<org.apache.tools.zip.ZipOutputStream: long adjustToLong(int)> **** Executed Line: **** 1 **** $l1 = staticinvoke <org.apache.tools.zip.ZipUtil: long adjustToLong(int)>(i0)\n" +
                    "\n" +
                    "Time: 0.126\n" +
                    "\n" +
                    "OK (2 tests)\n||****||";

            String out3 = "openjdk11||****||JUnit version 4.12\n" +
                    ".<org.apache.tools.zip.ZipOutputStream: void <clinit>()> **** Executed Line: **** 0 **** <org.apache.tools.zip.ZipOutputStream: java.lang.String DEFAULT_ENCODING> = null\n" +
                    "<org.apache.tools.zip.ZipOutputStream: org.apache.tools.zip.ZipLong toDosTime(java.util.Date)> **** Executed Line: **** 1 **** $r1 = staticinvoke <org.apache.tools.zip.ZipUtil: org.apache.tools.zip.ZipLong toDosTime(java.util.Date)>(r0)\n" +
                    ".<org.apache.tools.zip.ZipOutputStream: long adjustToLong(int)> **** Executed Line: **** 1 **** $l1 = staticinvoke <org.apache.tools.zip.ZipUtil: long adjustToLong(int)>(i0)\n" +
                    "<org.apache.tools.zip.ZipOutputStream: long adjustToLong(int)> **** Executed Line: **** 1 **** $l1 = staticinvoke <org.apache.tools.zip.ZipUtil: long adjustToLong(int)>(i0)\n" +
                    "<org.apache.tools.zip.ZipOutputStream: long adjustToLong(int)> **** Executed Line: **** 1 **** $l1 = staticinvoke <org.apache.tools.zip.ZipUtil: long adjustToLong(int)>(i0)\n" +
                    "\n" +
                    "Time: 0.136\n" +
                    "\n" +
                    "OK (2 tests)\n||****||";

            String out4 = "j9-8||****||JUnit version 4.12\n" +
                    ".<org.apache.tools.zip.ZipOutputStream: void <clinit>()> **** Executed Line: **** 0 **** <org.apache.tools.zip.ZipOutputStream: java.lang.String DEFAULT_ENCODING> = null\n" +
                    "<org.apache.tools.zip.ZipOutputStream: org.apache.tools.zip.ZipLong toDosTime(java.util.Date)> **** Executed Line: **** 1 **** $r1 = staticinvoke <org.apache.tools.zip.ZipUtil: org.apache.tools.zip.ZipLong toDosTime(java.util.Date)>(r0)\n" +
                    ".<org.apache.tools.zip.ZipOutputStream: long adjustToLong(int)> **** Executed Line: **** 1 **** $l1 = staticinvoke <org.apache.tools.zip.ZipUtil: long adjustToLong(int)>(i0)\n" +
                    "<org.apache.tools.zip.ZipOutputStream: long adjustToLong(int)> **** Executed Line: **** 1 **** $l1 = staticinvoke <org.apache.tools.zip.ZipUtil: long adjustToLong(int)>(i0)\n" +
                    "<org.apache.tools.zip.ZipOutputStream: long adjustToLong(int)> **** Executed Line: **** 1 **** $l1 = staticinvoke <org.apache.tools.zip.ZipUtil: long adjustToLong(int)>(i0)\n" +
                    "\n" +
                    "Time: 0.042\n" +
                    "\n" +
                    "OK (2 tests)\n||****||";

            String out5 = "j9-9||****||JUnit version 4.12\n" +
                    ".<org.apache.tools.zip.ZipOutputStream: void <clinit>()> **** Executed Line: **** 0 **** <org.apache.tools.zip.ZipOutputStream: java.lang.String DEFAULT_ENCODING> = null\n" +
                    "<org.apache.tools.zip.ZipOutputStream: org.apache.tools.zip.ZipLong toDosTime(java.util.Date)> **** Executed Line: **** 1 **** $r1 = staticinvoke <org.apache.tools.zip.ZipUtil: org.apache.tools.zip.ZipLong toDosTime(java.util.Date)>(r0)\n" +
                    ".<org.apache.tools.zip.ZipOutputStream: long adjustToLong(int)> **** Executed Line: **** 1 **** $l1 = staticinvoke <org.apache.tools.zip.ZipUtil: long adjustToLong(int)>(i0)\n" +
                    "<org.apache.tools.zip.ZipOutputStream: long adjustToLong(int)> **** Executed Line: **** 1 **** $l1 = staticinvoke <org.apache.tools.zip.ZipUtil: long adjustToLong(int)>(i0)\n" +
                    "<org.apache.tools.zip.ZipOutputStream: long adjustToLong(int)> **** Executed Line: **** 1 **** $l1 = staticinvoke <org.apache.tools.zip.ZipUtil: long adjustToLong(int)>(i0)\n" +
                    "\n" +
                    "Time: 0.133\n" +
                    "\n" +
                    "OK (2 tests)\n||****||";

            String out6 = "j9-11||****||JUnit version 4.12\n" +
                    ".<org.apache.tools.zip.ZipOutputStream: void <clinit>()> **** Executed Line: **** 0 **** <org.apache.tools.zip.ZipOutputStream: java.lang.String DEFAULT_ENCODING> = null\n" +
                    "<org.apache.tools.zip.ZipOutputStream: org.apache.tools.zip.ZipLong toDosTime(java.util.Date)> **** Executed Line: **** 1 **** $r1 = staticinvoke <org.apache.tools.zip.ZipUtil: org.apache.tools.zip.ZipLong toDosTime(java.util.Date)>(r0)\n" +
                    ".<org.apache.tools.zip.ZipOutputStream: long adjustToLong(int)> **** Executed Line: **** 1 **** $l1 = staticinvoke <org.apache.tools.zip.ZipUtil: long adjustToLong(int)>(i0)\n" +
                    "<org.apache.tools.zip.ZipOutputStream: long adjustToLong(int)> **** Executed Line: **** 1 **** $l1 = staticinvoke <org.apache.tools.zip.ZipUtil: long adjustToLong(int)>(i0)\n" +
                    "<org.apache.tools.zip.ZipOutputStream: long adjustToLong(int)> **** Executed Line: **** 1 **** $l1 = staticinvoke <org.apache.tools.zip.ZipUtil: long adjustToLong(int)>(i0)\n" +
                    "\n" +
                    "Time: 0.194\n" +
                    "\n" +
                    "OK (2 tests)\n||****||";

            JVMOutputParser jvmOutputParser = new JVMOutputParser();
//            JVMOutputResult outputResult = new JVMOutputParser().parseOutput(out);
            List<JVMOutputResult> resultList = new ArrayList<>();
            resultList.add(jvmOutputParser.parseOutput(out));
            resultList.add(jvmOutputParser.parseOutput(out2));
            resultList.add(jvmOutputParser.parseOutput(out3));
            resultList.add(jvmOutputParser.parseOutput(out4));
            resultList.add(jvmOutputParser.parseOutput(out5));
            resultList.add(jvmOutputParser.parseOutput(out6));
            System.out.println(UniquenessJudge.isDifferenceUnique(resultList));
            System.out.println("finish!");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
