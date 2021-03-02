package com.djfuzz;

import com.djfuzz.Vector.LevenshteinDistance;
import com.djfuzz.Vector.MathTool;
import com.djfuzz.coevolution.ClusterTool;
import com.djfuzz.coevolution.Fitness;
import com.djfuzz.record.Recover;
import com.djfuzz.rf.*;
import com.djfuzz.util.OfflineClusterUtil;
import soot.G;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class DjfuzzFrameworkResumable {
    private static final int DEAD_END = -1;
    public static final int POPULATION_LIMIT = 20;
    public static String cpSeparator = ":";  // classpath separator

    private static Action gotoAction = new GotoAction();
    private static Action lookupAction = new LookupAction();
    private static Action returnAction = new ReturnAction();

    DjfuzzFrameworkResumable(){
        cpSeparator = File.pathSeparator;
        makeDir("./AcceptHistory/");
        makeDir("./RejectHistory/");
        makeDir("./nolivecode/");
        makeDir("./tmp/");
        makeDir("./tmpRecord");
    }

    public void makeDir(String path){
        File f = new File(path);
        if(!f.exists()){
            f.mkdir();
        }
    }

    public static Map<String, Action> getActionContainer() {
        return actionContainer;
    }

    public static void setActionContainer(Map<String, Action> actionContainer) {
        DjfuzzFrameworkResumable.actionContainer = actionContainer;
    }

    private static Map<String, Action> actionContainer = new HashMap<>();
    static {
        actionContainer.put(State.RETURN, returnAction);
        actionContainer.put(State.LOOK_UP, lookupAction);
        actionContainer.put(State.GOTO, gotoAction);
    }

    public void process(String className, int iterationLimit, String[] args, String classPath, String dependencies, String jvmOptions) throws IOException {
        if(classPath != null && !classPath.equals("")){
            if(classPath.contains(File.pathSeparator)){
                int psIdx = classPath.indexOf(File.pathSeparator);
                Main.setGenerated(classPath.substring(0, psIdx));
                String extraCp = "";
                for(String cpComponent: classPath.substring(psIdx+1).split(File.pathSeparator)){
                    if(cpComponent.endsWith(".jar")){
                        extraCp+=File.pathSeparator+cpComponent;
                    }else{
                        File d = new File(cpComponent);
                        for(File f:d.listFiles()){
                            if(f.getName().endsWith(".jar")){
                                extraCp += File.pathSeparator + cpComponent+f.getName();
                            }
                        }
                    }
                }
                Main.setExtraCp(extraCp);
            }else{
                Main.setGenerated(classPath);
            }
            classPath = Main.getGenerated();
        }else{
            classPath = "";
        }

        int iterationLeft = readLeftIterationNum(classPath+"/currentPopulation/", iterationLimit);
        if(iterationLeft <= 0)
            return;

        // redirect the ouput to the log file
//        PrintStream newStream=new PrintStream("./"+className+(iterationLimit-iterationLeft)+".log");
//        System.setOut(newStream);
//        System.setErr(newStream);

        System.out.println("Iteration left: "+ iterationLeft);
        iterationLimit = iterationLeft;

        if(dependencies != null && !dependencies.equals("")){
            if(!dependencies.endsWith(".jar")){  // directory
                File d = new File(dependencies);
                String tmp = "";
                for(File f:d.listFiles()){
                    if(f.getName().endsWith(".jar")){
                        tmp+=dependencies+f.getName()+File.pathSeparator;
                    }
                }
                dependencies = tmp;
            }
            Main.setDependencies(dependencies);
        }
        MutateClass.switchSelectStrategy();
        Main.initial(args);
        List<State> mutateRejectHistory = new ArrayList<>(); // once accpeted but get out
        List<Double> averageDistance = new ArrayList<>();
        Random random = new Random();

        List<State> mutateAcceptHistory = readCurrentPopulation(classPath+"currentPopulation/", classPath, className, args, jvmOptions);
        checkBackpathNull(mutateAcceptHistory, "In replacement, mutateAcceptHistory:(size="+mutateAcceptHistory.size()+")");
        if(mutateAcceptHistory.size() == 0){
            MutateClass mutateClass = new MutateClass();
            mutateClass.initialize(className, args, null, jvmOptions);
            mutateClass.saveCurrentClass(); // in case 1st backtrack no backup
            State startState = new State();
            startState.setTarget(mutateClass);
            mutateAcceptHistory.add(startState);
            checkBackpathNull(mutateAcceptHistory, "In initialization, mutateAcceptHistory:(size="+mutateAcceptHistory.size()+")");
        }
        int iterationCount = 0;
        while (iterationCount < iterationLimit) {
            int currentSize = mutateAcceptHistory.size();
            for (int j = 0; j < currentSize; j ++) {
                State current = mutateAcceptHistory.get(j);
                current.setTarget(Recover.recoverFromPath(current.getTarget()));
                String recordPath = OfflineClusterUtil.convertBack2Log(current.getTarget().getBackPath());
                String nextActionString = current.selectActionAndMutatedMethod();
                Action nextAction = DjfuzzFrameworkResumable.getActionContainer().get(nextActionString);
                State nextState = nextAction.proceedAction(current.getTarget(), mutateAcceptHistory);
                iterationCount ++;
                MutateClass newOne = nextState.getTarget();
                if (newOne != null) {
                    int totalSize = mutateAcceptHistory.size() + mutateRejectHistory.size();
                    System.out.println("Current size is : " + totalSize + ", iteration is :" + iterationCount + ", average distance is " + MathTool.mean(averageDistance));
                    MethodCounter currentCounter = newOne.getCurrentMethod();
                    int distance = LevenshteinDistance.computeLevenshteinDistance(current.getTarget().getClassPureInstructionFlow(), newOne.getClassPureInstructionFlow());
                    averageDistance.add(distance / 1.0);
                    System.out.println("Distance is " + distance + " signature is " + currentCounter.getSignature());
                    DjfuzzFramework.showListElement(newOne.getMethodLiveCodeStringBySignature(currentCounter.getSignature()));
                    DjfuzzFramework.showListElement(current.getTarget().getMethodLiveCodeStringBySignature(currentCounter.getSignature()));
                    System.out.println(newOne.getClassPureInstructionFlow().size());
                    DjfuzzFramework.showListElement(newOne.getClassPureInstructionFlow());
                    System.out.println(current.getTarget().getClassPureInstructionFlow().size());
                    DjfuzzFramework.showListElement(current.getTarget().getClassPureInstructionFlow());
                    nextState.getTarget().saveCurrentClass();
                    mutateAcceptHistory.add(nextState);
                    checkBackpathNull(mutateAcceptHistory, "In iteration, mutateAcceptHistory:(size="+mutateAcceptHistory.size()+")");
                    current.updateMethodScore(nextActionString, distance / 1.0);
                } else {
                    current.updateMethodScore(nextActionString, DEAD_END);
                }
            }
            for (State state: mutateAcceptHistory) {
                state.setCoFitnessScore(Fitness.fitness(state, mutateAcceptHistory));
            }
            if (mutateAcceptHistory.size() > POPULATION_LIMIT) {
                mutateAcceptHistory.sort(new Comparator<State>() {
                    @Override
                    public int compare(State o1, State o2) {
                        double scoreOne = o1.getCoFitnessScore();
                        double scoreTwo = o2.getCoFitnessScore();
                        if (Math.abs(scoreOne - scoreTwo) < .00001) {
                            return 0;
                        }
                        return (o2.getCoFitnessScore() - o1.getCoFitnessScore()) > 0 ? 1 : -1;
                    }
                });
                for (int j = POPULATION_LIMIT; j < mutateAcceptHistory.size(); j ++) {
                    mutateRejectHistory.add(mutateAcceptHistory.get(j));
                    DjfuzzFramework.dumpSingleMutateClass(mutateAcceptHistory.get(j).getTarget(), "./RejectHistory/");
                }
                mutateAcceptHistory = mutateAcceptHistory.subList(0, POPULATION_LIMIT);
                dumpAcceptPopulation(mutateAcceptHistory, className);
                saveCurrentPopulation(mutateAcceptHistory, classPath+"currentPopulation/",
                        iterationLimit-iterationCount, classPath);
            }else{
//                dumpAcceptPopulation(mutateAcceptHistory, className);
                saveCurrentPopulation(mutateAcceptHistory, classPath+"currentPopulation/",
                        iterationLimit-iterationCount, classPath);
            }
        }

        ClusterTool.getEvoClusterData(mutateAcceptHistory, mutateRejectHistory);

        List<Double> totalScore = new ArrayList<>();
        try{
            System.out.println("Average distance is " + MathTool.mean(averageDistance));
            System.out.println("var is " + MathTool.standardDeviation(averageDistance));
            System.out.println("max is " + Collections.max(averageDistance));
        }catch (Exception e){
            e.printStackTrace();
        }
        for (State state: mutateAcceptHistory) {
            System.out.print(state.getCoFitnessScore() + " ");
            totalScore.add(state.getCoFitnessScore());
        }
        System.out.println();
        System.out.println("Basic pattern average: " + MathTool.mean(totalScore));
        mutateRejectHistory.addAll(mutateAcceptHistory);
        for (State state: mutateRejectHistory) {
            state.setCoFitnessScore(Fitness.fitness(state, mutateRejectHistory));
            totalScore.add(state.getCoFitnessScore());
        }
        System.out.println("Total average:" + MathTool.mean(totalScore));
        System.out.println();
        System.out.println(MathTool.mean(totalScore));
        G.reset();
    }

    public static void checkBackpathNull(List<State> list, String msg){
        for(int i = 0; i< list.size();i ++){
            State s = list.get(i);
            if(s.getTarget().getBackPath() == null || s.getTarget().getBackPath().equals("")){
                System.err.println("**********" + msg+" "+i+"th state has no backpath!"+"**********");
                list.remove(s);
                i--;
            }
        }
    }

    public static List<State> readCurrentPopulation(String populationPath, String classPath, String className, String[] args, String jvmOptions) {
        List<State> list = new ArrayList<>();
        File file = new File(populationPath);
        String dstFilePath = classPath+className.replaceAll("[.]","/")+".class";
        File dstFile = new File(dstFilePath);
        if(!file.exists() || file.listFiles().length == 0){
            return list;
        }
        for(File f: file.listFiles()){
            if(!f.getName().endsWith(".class"))
                continue;
            System.out.println("Loading individual: "+f.getName());
            G.reset();  // important!!
            Main.initial(args);
            MutateClass mc = new MutateClass();
            State s = new State();
            Map<String, List<Double>> methodScores = new HashMap<>();
            try{
                FileReader fr = new FileReader(f.getPath());
                BufferedReader br = new BufferedReader(fr);
                String line = null;
                String backPath = br.readLine();
                mc.setBackPath(backPath);  // backpath
//                System.out.println(f.getName()+" set backpath: "+backPath);
                File mcFile = new File(backPath);
                if(!mcFile.exists()){  // can not find tmp class file in tmp dir
                    System.err.println("Can not find individual "+mcFile.getName()+" in tmp dir!");
                    continue;
                }
                Files.copy(mcFile.toPath(), dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
//                MutateClass.setWantReload(true);
                mc.initialize(className, args, null, jvmOptions);
//                MutateClass.setWantReload(false);
                s.setTarget(mc);
                while((line = br.readLine())!=null){
                    String[] content = line.split("[;]");
                    List<Double> listDouble = new ArrayList<>();
                    for(int i = 1; i < content.length; i++){
                        listDouble.add(Double.parseDouble(content[i]));
                    }
                    methodScores.put(content[0], listDouble);
                }
                br.close();
                s.setMethodScores(methodScores);
                list.add(s);
            }catch (Exception e){
                e.printStackTrace();
            }

        }
        return list;
    }

    // synchronize the population to AcceptHistory directory
    public static void dumpAcceptPopulation(List<State> stateList, String className){
        File acc = new File("./AcceptHistory/");
        for(File f: acc.listFiles()){
            if(f.getName().contains(className)){
                f.delete();
            }
        }
        for(State s: stateList){
            DjfuzzFramework.dumpSingleMutateClass(s.getTarget(), "./AcceptHistory/");
        }
    }

    public static int readLeftIterationNum(String populationPath, int iterationLimit){
        int leftIterationNum = iterationLimit;
        File infoFile = new File(populationPath + "population.info");
        if(!infoFile.exists())
            return iterationLimit;
        try {
            FileReader fr = new FileReader(infoFile.getPath());
            BufferedReader br = new BufferedReader(fr);
            String line = br.readLine();
            leftIterationNum = Integer.parseInt(line.split("[:]")[1]);
            System.out.println("Iteration left: "+ leftIterationNum);
        }catch (Exception e){
            e.printStackTrace();
        }
        return leftIterationNum;

    }

    public static void saveCurrentPopulation(List<State> stateList, String populationPath,
                                             int leftIterationNum, String currentClassPath){
        File file = new File(populationPath);
        if(!file.exists()){
            file.mkdir();
        }else{
            for(File f: file.listFiles()){
                f.delete();
            }
        }
        for (State s: stateList){
            saveState(s, populationPath);
        }
        String infoName = "population.info";
        File f = new File(populationPath + infoName);
        try {
            FileWriter fw = new FileWriter(f.getPath(), false);
            fw.write("Iteration Left:"+leftIterationNum);
            fw.close();
        }catch (Exception e){
            e.printStackTrace();
        }
//        cleanTmpFolder(currentClassPath);
    }

    public static void cleanTmpFolder(String currentClassPath){
        String targetDir = "./tmp/";
        List<String> classPath = new ArrayList<>(Arrays.asList(
                "./sootOutput/avrora-cvs-20091224/",
                "./sootOutput/batik-all/",
                "./sootOutput/eclipse/",
                "./sootOutput/sunflow-0.07.2/",
                "./sootOutput/jython/",
                "./sootOutput/fop/",
                "./sootOutput/pmd-4.2.5/",
                "./sootOutput/ant/ant-launcher/",
                "./sootOutput/apache-maven-3.6.3/boot/plexus-classworlds-2.6.0/",
                "./sootOutput/resolver/",
                "./sootOutput/apache-any23-cli-2.3/",
                "./sootOutput/xalan/",
                "sootOutput/ivy-2.5.0/",
                "sootOutput/tika-app-1.24/",
                "./sootOutput/junit-ant/"
        ));
        if(!classPath.contains(currentClassPath)){
            classPath.add(currentClassPath);
        }
        File target = new File("./tmpClass/");
        if(target.exists())
            deleteFile(target);
        target.mkdir();
        for(String cp: classPath){
//            System.out.println("Processing " + cp);
            String populationPath = cp + "currentPopulation/";
            File population = new File(populationPath);
            if(!population.exists())
                continue;
            for(File f: population.listFiles()){
                if (f.getName().endsWith(".class")){
                    String tmpFileName = "./tmp/" + f.getName();
                    String targetName = tmpFileName.replace("./tmp/", "./tmpClass/");
                    File source = new File(tmpFileName);
                    File dest = new File(targetName);
                    try{
                        Files.copy(source.toPath(), dest.toPath());
                    }catch(FileAlreadyExistsException e){
//                        System.err.println("File Already Exists: " + source.getName());
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
        deleteFile(new File("./tmp/"));
        target.renameTo(new File("./tmp/"));
    }

    public static void deleteFile(File file){
        if (file.isFile()){
            file.delete();
        }else{
            String[] childFilePath = file.list();
            for (String path:childFilePath){
                File childFile= new File(file.getAbsoluteFile()+"/"+path);
                deleteFile(childFile);
            }
            file.delete();
        }
    }

    public static void saveState(State s, String directory){
//        DjfuzzFramework.dumpSingleMutateClass(s.getTarget(), directory);
        if(s.getTarget().getBackPath() == null)
            return;
        String logName = s.getTarget().getBackPath().replace("./tmp/","");
        File file = new File(directory+logName);
        try {
            FileWriter fw = new FileWriter(file.getPath(), false);
            fw.write(s.getTarget().getBackPath()+"\n");
            String line = "";
            for (String key : s.getMethodScores().keySet()) {
                line = key+";";
                List<Double> list = s.getMethodScores().get(key);
                for (double num:list){
                    line += num + ";";
                }
                fw.write(line + "\n");
            }
            fw.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        DjfuzzFrameworkResumable fwk = new DjfuzzFrameworkResumable();

//        Main.useJunit("./junit-4.12.jar", "./hamcrest-core-1.3.jar",
//                "", "com.djfuzz.HelloTest");
//        fwk.process("com.djfuzz.Hello", 5, args,
//                "./sootOutput/", "", "");

//        Main.useJunit("./junit-4.12.jar", "./hamcrest-core-1.3.jar",
//                "./tools.jar", "org.apache.tools.ant.AntClassLoaderTest");
//        fwk.process("org.apache.tools.ant.AntClassLoader", 10000, args,
//                "./sootOutput/junit-ant/",
//                "", "");
//        Main.useJunit("./junit-4.12.jar", "./hamcrest-core-1.3.jar",
//                "./tools.jar", "org.apache.tools.ant.DirectoryScannerTest");
//        fwk.process("org.apache.tools.ant.DirectoryScanner", 10000, args,
//                "./sootOutput/junit-ant/",
//                "", "");
//        Main.useJunit("./junit-4.12.jar", "./hamcrest-core-1.3.jar",
//                "./tools.jar", "org.apache.tools.ant.ProjectTest");
//        fwk.process("org.apache.tools.ant.Project", 10000, args,
//                "./sootOutput/junit-ant/",
//                "", "");
//        Main.useJunit("./junit-4.12.jar", "./hamcrest-core-1.3.jar",
//                "./tools.jar", "org.apache.tools.ant.launch.LocatorTest");
//        fwk.process("org.apache.tools.ant.launch.Locator", 10000, args,
//                "./sootOutput/junit-ant/",
//                "", "");


//        Main.useJunit("", "sootOutput/junit-junit/hamcrest-core-1.3.jar",
//                "./tools.jar", "org.junit.rules.TempFolderRuleTest");
//        fwk.process("org.junit.rules.TemporaryFolder", 5000, args,
//                "./sootOutput/junit-junit/",
//                "", "");
//        Main.useJunit("", "sootOutput/junit-junit/hamcrest-core-1.3.jar",
//                "./tools.jar", "org.junit.rules.TestWatcherTest");
//        fwk.process("org.junit.rules.TestWatcher", 5000, args,
//                "./sootOutput/junit-junit/",
//                "", "");
//        Main.useJunit("", "sootOutput/junit-junit/hamcrest-core-1.3.jar",
//                "./tools.jar", "org.junit.internal.runners.ErrorReportingRunnerTest");
//        fwk.process("org.junit.internal.runners.ErrorReportingRunner", 5000, args,
//                "./sootOutput/junit-junit/",
//                "", "");
//        Main.useJunit("", "sootOutput/junit-junit/hamcrest-core-1.3.jar",
//                "./tools.jar", "org.junit.samples.money.MoneyTest");
//        fwk.process("junit.samples.money.Money", 5000, args,
//                "./sootOutput/junit-junit/",
//                "", "");



//        fwk.process("org.eclipse.core.runtime.adaptor.EclipseStarter", 18000,
//                new String[]{"-debug"}, "./sootOutput/eclipse/", null, "");
//        fwk.process("org.sunflow.Benchmark", 50000,
//                new String[]{"-bench","2","256"},
//                "./sootOutput/sunflow-0.07.2/",
//                "dependencies/janino-2.5.15.jar", "");
//        fwk.process("org.apache.fop.cli.Main", 3000,
//                new String[]{"-xml","sootOutput/fop/name.xml","-xsl","sootOutput/fop/name2fo.xsl","-pdf","sootOutput/fop/name.pdf"},
//                "./sootOutput/fop/",
//                "dependencies/xmlgraphics-commons-1.3.1.jar" + cpSeparator +
//                        "dependencies/commons-logging.jar" + cpSeparator +
//                        "dependencies/avalon-framework-4.2.0.jar" + cpSeparator +
//                        "dependencies/batik-all.jar" + cpSeparator +
//                        "dependencies/commons-io-1.3.1.jar", "");
//        fwk.process("org.python.util.jython", 6000,
//                new String[]{"sootOutput/jython/hello.py"},
//                "./sootOutput/jython/",
//                "dependencies/guava-r07.jar" + cpSeparator +
//                        "dependencies/constantine.jar" + cpSeparator +
//                        "dependencies/jnr-posix.jar" + cpSeparator +
//                        "dependencies/jaffl.jar" + cpSeparator +
//                        "dependencies/jline-0.9.95-SNAPSHOT.jar" + cpSeparator +
//                        "dependencies/antlr-3.1.3.jar" + cpSeparator +
//                        "dependencies/asm-3.1.jar", "");
//        fwk.process("org.apache.tools.ant.launch.Launcher", 50000,
//                new String[]{"compile", "jar", "run"},
//                "./sootOutput/ant/ant-launcher/",
//                null, "");
//        fwk.process("org.apache.ivy.Main", 50000, new String[]{},
//                "sootOutput/ivy-2.5.0/",null, "");
    }
}