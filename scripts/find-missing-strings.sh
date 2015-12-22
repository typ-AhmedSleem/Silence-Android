#!/usr/bin/env bash

output=""
for file in $(find src/ -type f); do
    output=$(cat $file | grep -o "R.string.[A-Za-z_]*" | sed 's/R.string.//g')$'\n'$output
done

for string in $output; do
    result=$(grep $string res/values/strings.xml)
    if [[ $result == "" ]]; then
        is_a_comment=$(grep -r 'src/' -e "^\/\/.*R.string.$string.*")
        if [[ $is_a_comment == "" ]]; then
            echo "$string is missing!"
        fi
    fi
done
