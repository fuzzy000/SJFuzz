targetClass=("sootOutput/eclipse/org/eclipse/core/runtime/adaptor/EclipseStarter.class" "sootOutput/fop/org/apache/fop/cli/Main.class" "sootOutput/jython/org/python/util/jython.class" "sootOutput/sunflow-0.07.2/org/sunflow/Benchmark.class" "sootOutput/ant/ant-launcher/org/apache/tools/ant/launch/Launcher.class" "sootOutput/ivy-2.5.0/org/apache/ivy/Main.class" "sootOutput/junit-ant/org/apache/tools/ant/AntClassLoader.class" "sootOutput/junit-ant/org/apache/tools/ant/DirectoryScanner.class" "sootOutput/junit-ant/org/apache/tools/ant/Project.class" "sootOutput/junit-ant/org/apache/tools/ant/launch/Locator.class" "sootOutput/junit-junit/org/junit/rules/TemporaryFolder.class" "sootOutput/junit-junit/org/junit/rules/TestWatcher.class" "sootOutput/junit-junit/org/junit/internal/runners/ErrorReportingRunner.class" "sootOutput/junit-junit/junit/samples/money/Money.class")

replaceClass=("sootOutput/eclipse/org/eclipse/core/runtime/adaptor/EclipseStarter-original.class" "sootOutput/fop/org/apache/fop/cli/Main-original.class" "sootOutput/jython/org/python/util/jython-original.class" "sootOutput/sunflow-0.07.2/org/sunflow/Benchmark-original.class" "sootOutput/ant/ant-launcher/org/apache/tools/ant/launch/Launcher-original.class" "sootOutput/ivy-2.5.0/org/apache/ivy/Main-original.class" "sootOutput/junit-ant/org/apache/tools/ant/AntClassLoader-original.class" "sootOutput/junit-ant/org/apache/tools/ant/DirectoryScanner-original.class" "sootOutput/junit-ant/org/apache/tools/ant/Project-original.class" "sootOutput/junit-ant/org/apache/tools/ant/launch/Locator-original.class" "sootOutput/junit-junit/org/junit/rules/TemporaryFolder-original.class" "sootOutput/junit-junit/org/junit/rules/TestWatcher-original.class" "sootOutput/junit-junit/org/junit/internal/runners/ErrorReportingRunner-original.class" "sootOutput/junit-junit/junit/samples/money/Money-original.class")

classPath=("sootOutput/eclipse/" "sootOutput/fop/" "sootOutput/jython/" "sootOutput/sunflow-0.07.2/" "sootOutput/ant/ant-launcher/" "sootOutput/ivy-2.5.0/" "sootOutput/junit-ant/" "sootOutput/junit-ant/" "sootOutput/junit-ant/" "sootOutput/junit-ant/" "sootOutput/junit-junit/" "sootOutput/junit-junit/" "sootOutput/junit-junit/" "sootOutput/junit-junit/")


# for originalClass in ${replaceClass[@]}

for((i=0;i<14;i++))
do
    if [ ! -f $originalClass ]
    then
        echo $originalClass not exist!!
    fi
    echo copying ${replaceClass[$i]} to ${targetClass[$i]}
    cp ${replaceClass[$i]} ${targetClass[$i]}

    rm ${classPath[$i]}MutationCounter.log
    rm -r ${classPath[$i]}currentPopulation
done

rm AcceptHistory/*
rm RejectHistory/*
rm nolivecode/*
rm tmp/*
