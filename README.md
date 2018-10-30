# Zookeeper: Distributed Scoreboard

1. Clone this repository
2. Go to the cloned directory
3. Execute the following commands to setup your environment.
##### Make sure to run these commands from all terminals/clients.
```
  cd class
  source ./config.sh
```
4. Make sure zookeeper server is running.
5. Test - 

##### Make sure to run the above test commands from class directory.
```
  watcher 12.34.45.87:6000 N -- where N is an integer
  player 12.34.45.87:6000 name
  player 12.34.45.87:6000 "first last"
  player 12.34.45.87:6000 name count delay score
```
