#!/bin/bash

DATA_PATH="/Users/cuong/IdeaProjects/jrelifix/BugsDataset"

for bugid in `ls patches`; do
  git --git-dir $DATA_PATH/.git checkout $bugid
  path="./patches/$bugid"
  for file in `ls $path`; do
    line=`head -n1 $path/$file`
    file_path=${line:4}
    file_path=`find $DATA_PATH -type f | grep $file_path`
    file_path=${file_path:${#DATA_PATH}}
    if [ ! -z "$file_path" ]; then
      folder_path=`dirname $file_path`
      mkdir -p ./original_fault_files/$bugid$folder_path
      cp $DATA_PATH$file_path ./original_fault_files/$bugid$folder_path
      echo $file_path
    fi
    break
  done
done