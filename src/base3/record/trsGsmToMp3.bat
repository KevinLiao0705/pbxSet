ffmpeg.exe -y -i test.gsm -vn -ar 8000 -ac 1 -b:a 192k test.mp3
./ffmpeg -y -i test.mp3 -vn -ar 8000 -ac 1 -b:a 13000 test1.gsm