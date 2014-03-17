#!/bin/bash

OBJECTS=`ls *.png *.jpg`

for obj in $OBJECTS; do 
	echo Processing $obj; 
	convert $obj ${obj/%\.*/\.eps}
done

