target=( com.ziclix.python.sql.DataHandler org.python.core.BaseBytes org.python.core.PyArray org.python.core.PyByteArray org.python.core.PyFloat org.python.core.PyList org.python.core.PySystemState org.python.core.PyTuple )

if [ -d currentPopulation ]
then
    echo rm -r currentPopulation
    rm -r currentPopulation
fi

for t in ${target[@]}
do
    tmp=${t//"."/"/"}
    # echo $tmp
    className=$tmp".class"
    originalClassName=$tmp"-original.class"
    if [ ! -f $originalClassName ]
    then
        echo cp $className $originalClassName
        cp $className $originalClassName
    else
        echo cp $originalClassName $className
        cp $originalClassName $className
    fi
done