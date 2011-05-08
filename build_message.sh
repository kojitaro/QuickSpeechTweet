
mkdir -p temp

function convert {
say -o temp/$2 $1
afconvert temp/$2.aiff -f m4af -o res/raw/$2.m4a
}

convert "Please speak." lets_tweet
convert "An error has occurred." error
convert "Completed." complete
convert "once more" oncemore

