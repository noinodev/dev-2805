javac -d bin/server server/MatchmakingServer.java
echo Main-Class: MatchmakingServer > bin/server/manifest.txt
jar cfm stun-server/StunServer.jar bin/server/manifest.txt -C bin/server/ .