if [ "$1" = "" ]
then
	echo "Please specify version."
	exit 
else
	echo "Backup Version is $1 ."
fi

tar -cvf ../bridge-"$1".tar.gz ../bridge
ls ..
