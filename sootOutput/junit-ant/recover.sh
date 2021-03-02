target=( org.apache.tools.ant.DefaultLogger org.apache.tools.mail.MailMessage org.apache.tools.tar.TarEntry org.apache.tools.tar.TarOutputStream org.apache.tools.zip.ZipEntry org.apache.tools.zip.AsiExtraField org.apache.tools.zip.ExtraFieldUtils org.apache.tools.zip.ZipLong org.apache.tools.zip.ZipShort org.apache.tools.zip.ZipOutputStream )

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