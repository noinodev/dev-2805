javac -d bin/client src/*.java
echo Main-Class: Tetris2805 > bin/client/manifest.txt
xcopy "src\resources" "bin\client\resources" /E /I /Y /Q
xcopy "src\data" "goblin-tetris\src\data" /E /I /Y /Q
jar cfm goblin-tetris/GoblinTetris.jar bin/client/manifest.txt -C bin/client/ .