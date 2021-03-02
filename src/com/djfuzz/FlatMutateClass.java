package com.djfuzz;

import com.djfuzz.differential.DifferentialLogger;
import com.djfuzz.rf.State;
import com.djfuzz.solver.JVMOutputResult;
import com.djfuzz.solver.UniqueChildSaver;
import com.djfuzz.solver.UniquenessJudge;
import soot.SootMethod;
import soot.jimple.Stmt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Yicheng Ouyang on 2021/1/13
 */
public class FlatMutateClass extends MutateClass{
    private List<JVMOutputResult> jvmOutputResults = new ArrayList<>();

    public FlatMutateClass(){
        setClassPureInstructionFlow(new ArrayList<>());
    }

    public void addJvmOutputResults(JVMOutputResult outputResult) throws IOException{
        jvmOutputResults.add(outputResult);
//        System.out.println("The size is " + outputResult.getLiveInstructions().size());
        if (this.getClassPureInstructionFlow().size() == 0) {
//            setClassPureInstructionFlow(outputResult.getLiveInstructions());
            for (SootMethod method : getSootClass().getMethods()) {
                this.getMethodLiveBody().put(method.getSignature(), method.retrieveActiveBody());
            }
            this.setClassPureInstructionFlow(outputResult.getLiveInstructions());
            Set<String> classPureInstructionFlowSet= new HashSet<>();
            for(String s: getClassPureInstructionFlow()){
                String[] elements = s.split("[*]+");
                String currentStmt = elements[3].trim();
                classPureInstructionFlowSet.add(currentStmt);
            }
            this.setLiveMethod(Main.getLiveMethod(classPureInstructionFlowSet,
                    this.getClassPureInstructionFlow(), this.getSootClass().getMethods()));

//            int counter = 0;
            for (SootMethod method : getLiveMethod()) {
//            methodOriginalQuery.put(method.getSignature(), Main.getAllStatementsSet(method)); // for tp selection: all stmts

                getMethodOriginalStmtList().put(method.getSignature(), Main.getAllStatementsList(method));

                getMethodMap().put(method.getSignature(), method);
                Set<String> usedStmt = classPureInstructionFlowSet;
                List<Stmt> liveStmt = Main.getActiveInstructions(usedStmt, getSootClass(), method.getSignature(), getActiveArgs());
//                methodLiveQuery.put(method.getSignature(), changeListToSet(liveStmt));
//                UsedStatementHelper.addClassMethodUsedStmt(className, method.getSignature(), usedStmt);
                getMethodLiveCode().put(method.getSignature(), liveStmt);
//                int callCount = 1;
//                if (previousMutationCounter != null && counter < previousMutationCounter.size()){
//                    callCount = previousMutationCounter.get(counter++).getCount();
//                }
//            int callCount = previousMutationCounter == null ? 1 : previousMutationCounter.get(counter++).getCount();
//                mutationCounter.add(new MethodCounter(method.getSignature(), callCount));
            }
            transformStmtToString(getMethodOriginalStmtList(), getMethodOriginalStmtStringList());
            transformStmtToStringAdvanced(getMethodLiveCode(), getMethodLiveCodeString());
        }
    }

    @Override
    public void initializeSootClass(List<MethodCounter> previousMutationCounter) throws IOException {
        Main.outputClassFile(getSootClass());
        this.jvmOutputResults = new ArrayList<>();
        this.setClassPureInstructionFlow(new ArrayList<>());
        DjfuzzFrameworkResumableAdvanced.fillClassWithOutput(this);
        if (UniquenessJudge.isDifferenceUnique(this.jvmOutputResults)) {
//            DifferentialLogger.writeMsgToLog(getBackPath());
            UniqueChildSaver.saveChild(this);
            if (this.getParent() != null) {
                this.getParent().foundUniqueChild();
                State parent = this.getParent().getCorrespondingState().deepCopyBySettingTarget(this.getParent());
//                parent.setTarget(this.getParent());
                DjfuzzFrameworkResumableAdvanced.getDiffFramework().submitClass(parent);
                if (UniquenessJudge.haveLBC(this.jvmOutputResults)){
                    if (this.getCorrespondingState() == null)
                        this.setCorrespondingState(new State());
                    State child = this.getCorrespondingState().deepCopyBySettingTarget(this);
//                    child.setTarget(this);
                    DjfuzzFrameworkResumableAdvanced.getDiffFramework().submitClass(child);
                }
            } else {
                if (this.getCorrespondingState() == null)
                    this.setCorrespondingState(new State());
                State child = this.getCorrespondingState().deepCopyBySettingTarget(this);
                child.setTarget(this);
                DjfuzzFrameworkResumableAdvanced.getDiffFramework().submitClass(child);
            }
        }
    }

    public MutateClass deepCopy(String signature) throws IOException {

        MutateClass result = new FlatMutateClass();
        result.setActiveArgs(getActiveArgs());
        result.setJvmOptions(getJvmOptions());
        result.setClassName(getClassName());
        result.setSootClass(getSootClass());
//        result.setBackPath(this.getBackPath());
        result.setCurrentMethod(this.getCurrentMethod());  // current method can be only changed in iteration()
        result.setParent(this);

        result.initializeSootClass(getMutationCounter());
//        setWantReload(false);
        if (result.getClassPureInstructionFlow().size() == 0 || result.getMethodLiveCodeBySignature(result.getCurrentMethod().getSignature()).size() == 0) {
            Main.temporaryOutput(result.getSootClass(), "./nolivecode/", System.currentTimeMillis() + ".");
            return null;
        }

        return result;


    }

}
