# SnowBot
A simple Discord bot made by [Snowleopard](https://sn.owo.lt) using the [Discord4J](https://github.com/Discord4J/Discord4J) framework.

# Building
* From project root build with Maven via `mvn clean install`
* Copy `target/SnowBot-1.0.0-spring-boot.jar` to desired location

# Testing / Development
The bot can be run from an IDE (such as Eclipse) via main class `lt.owo.snowbot.SnowBot`.  Be sure to setup `settings.json` in the project root first.

# Usage
* Create `settings.json` in the same directory as `SnowBot-1.0.0-spring-boot.jar`
  * Start from `settings-example.json` and update values
  * If you want `data_location` to be in a folder create the folder (required)
  * You can look at the examples in `data/` and create any of them if you don't want to start fresh (optional)
* `cd` to the directory and run `java -jar SnowBot-1.0.0-spring-boot.jar`

# Configuration
Description of each item in `settings.json`:
* command_prefix
  * Prefix to use in Discord when calling commands, ex: `!` for `!ping`
* bot_auth_token
  * Bot Authorization token from Discord, see the [Dev Portal](https://discord.com/developers/applications)
* owner_id
  * This is your Discord ID and is used to prevent admins from removing you as admin via `deladm` command
  * To find yours right click your name in discord and select `Copy ID` (it will be a long number)
* data_location.commands
  * Location for your commands csv file to be saved, see `data/commandList-example.csv`
* data_location.admins
  * Location for your admins csv file to be saved, see `data/adminList-example.csv` (you should add your owner_id here too)
* data_location.status
  * Location to save the last status you set for the bot so it uses it again if restarted (Listening to ___)
  * See `data/laststat-example.txt`
