package lt.owo.snowbot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.data.stored.ActivityBean;
import discord4j.core.object.data.stored.PresenceBean;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.presence.Status;

public class SnowBot {
	/** These will be changed to env vars or args eventually **/
	private static String SNOW_BOT_ID = "";
	private static Settings SETTINGS;
	private static Map<String, Command> commands = new HashMap<>(); // Holds all commands, actually what is executed as a Command (aka MessageCreateEvent)
	private static Map<String, String> msgCommands = new HashMap<>(); // Holds all non-special commands as a String so it can be saved to file easily
	private static ArrayList<String> restrictedCommands = new ArrayList<String>(); // List of all commands that should only be executable by an admin
	private static ArrayList<String> admins; // List of admins (Discord IDs)
	
	interface Command {
	    void execute(MessageCreateEvent event);
	}

	public static void main(String[] args) {
		SETTINGS = FileIO.loadSettings("settings.json");
		
		if (SETTINGS == null) {
			System.out.println("ERROR: Could not read settings file. Make sure 'settings.json' exists in the working directory");
			return;
		}
		
		loadData();
		
		DiscordClientBuilder builder = new DiscordClientBuilder(SETTINGS.AUTH_TOKEN);
		DiscordClient client = builder.build();
		
		if (!loadSpecials(client)) {
			System.out.println("An error occured while loading special commands!\n");
		}
    
		setupListeners(client);

		client.login().block();
	}
  
	private static void setupListeners(DiscordClient client) {
		// ReadyEvent (login response)
		client.getEventDispatcher().on(ReadyEvent.class)
      		.subscribe(event -> {
      			User self = event.getSelf();
      			System.out.println("\n#########################");
      			System.out.println(String.format("# Logged in as %s#%s", self.getUsername(), self.getDiscriminator()));
      			System.out.println("#########################\n");
      			SNOW_BOT_ID = self.getId().asString();
      			
      			String stat = FileIO.loadStatusFromFile(SETTINGS.STAT_FILE);
      			if (stat.length() > 2) {
      				PresenceBean presb = new PresenceBean();
    				presb.setStatus(Status.ONLINE.getValue());
    				ActivityBean act = new ActivityBean();
    				act.setName(stat);
    				act.setType(Activity.Type.LISTENING.getValue());
    				presb.setActivity(act);
    				Presence pres = new Presence(presb);
    				
    				client.updatePresence(pres).subscribe();
      			}
      	});
		
		// Handle user commands
		client.getEventDispatcher().on(MessageCreateEvent.class)
			.subscribe(event -> {
				final String content = event.getMessage().getContent().orElse("");
				boolean isBot = event.getMessage().getAuthor().get().isBot();
				boolean hasPrefix = content.startsWith(SETTINGS.CMD_PREFIX);
				//Apparently this is different depending on mobile app or desktop app...
				//Moved this to a method now since it's used a lot later on
				//boolean hasMention = (content.startsWith("<@!" + SNOW_BOT_ID + ">") || content.startsWith("<@" + SNOW_BOT_ID + ">"));
				
				if (!isBot && (hasPrefix || hasMention(content))) {
					String comm = "";
					
					if (hasPrefix) {
						// Grab all text before first space + trim off the prefix
						comm = content.split(" ")[0].substring(SETTINGS.CMD_PREFIX.length());
					} else { //hasMention
						comm = content.split(" ")[1];
					}
					
					if (restrictedCommands.contains(comm)) {
						String author = event.getMessage().getAuthor().get().getId().asString();
						
						if (admins.contains(author)) {
							executeIfCommandExists(comm, event);
						} else {
							executeIfCommandExists("unauthorized", event);
						}
					} else {
						executeIfCommandExists(comm, event);
					}
				}
		});
	}
	
	private static void executeIfCommandExists(String comm, MessageCreateEvent event) {
		if (commands.containsKey(comm)) {
			commands.get(comm).execute(event);
		}
	}
	
	private static boolean loadSpecials(DiscordClient client) {
		/**
		 * Shutdown
		 */
		commands.put("shutdown", event -> {
			executeIfCommandExists("sayGoodbye", event);
			
			client.logout().subscribe();
		});
		
		restrictedCommands.add("shutdown");
		
		
		/**
		 * Add command
		 */		
		commands.put("addcmd", event -> {
			String content = event.getMessage().getContent().orElse("");
			int mentioned = 0;
			
			if (hasMention(content)) {
				mentioned = 1;
			}
			
			String[] parsed = content.split(" ", 3 + mentioned);
			
			if (parsed.length != 3 + mentioned) {
				genMessageEvent("Invalid # of arguments").execute(event);
			} else if (parsed[1 + mentioned].contains(",")) {
				genMessageEvent("The command name cannot contain commas").execute(event);
			} else {
				if (addNewCommand(parsed[1 + mentioned], parsed[2 + mentioned])) {
					genMessageEvent("Command created").execute(event);
					FileIO.saveCommandsFile(SETTINGS.COMM_FILE, msgCommands);
				} else {
					genMessageEvent("This command name is already in use").execute(event);
				}
			}
		});
		
		restrictedCommands.add("addcmd");
		
		/**
		 * Delete command
		 */		
		commands.put("delcmd", event -> {
			String content = event.getMessage().getContent().orElse("");
			int mentioned = 0;
			
			if (hasMention(content)) {
				mentioned = 1;
			}
			
			String[] parsed = content.split(" ", 2 + mentioned);
			
			if (parsed.length != 2 + mentioned) {
				genMessageEvent("Invalid # of arguments").execute(event);
			} else if (parsed[1 + mentioned].contains(",")) {
				genMessageEvent("The command name cannot contain commas").execute(event);
			} else {
				if (removeCommand(parsed[1 + mentioned])) {
					genMessageEvent("Command removed").execute(event);
					FileIO.saveCommandsFile(SETTINGS.COMM_FILE, msgCommands);
				} else {
					genMessageEvent("There is no command by this name").execute(event);
				}
			}
		});
		
		restrictedCommands.add("delcmd");
		
		/**
		 * Add admin
		 */		
		commands.put("addadm", event -> {
			String content = event.getMessage().getContent().orElse("");
			int mentioned = 0;
			
			if (hasMention(content)) {
				mentioned = 1;
			}
			
			String[] parsed = content.split(" ", 2 + mentioned);
			
			if (parsed.length != 2 + mentioned) {
				genMessageEvent("Invalid # of arguments").execute(event);
			} else if (!parsed[1 + mentioned].startsWith("<@!")) {
				genMessageEvent("Please use a ping to indicate the user").execute(event);
			} else {
				String id = parsed[1 + mentioned].substring(3);
				id = id.substring(0, id.length()-1);
				if (addNewAdmin(id)) {
					genMessageEvent("Admin added").execute(event);
					FileIO.saveAdminsFile(SETTINGS.ADMIN_FILE, admins);
				} else {
					genMessageEvent("This user is already admin").execute(event);
				}
			}
		});
		
		restrictedCommands.add("addadm");
		
		
		/**
		 * Delete admin
		 */		
		commands.put("deladm", event -> {
			String content = event.getMessage().getContent().orElse("");
			int mentioned = 0;
			
			if (hasMention(content)) {
				mentioned = 1;
			}
			
			String[] parsed = content.split(" ", 2 + mentioned);
			
			if (parsed.length != 2 + mentioned) {
				genMessageEvent("Invalid # of arguments").execute(event);
			} else if (!parsed[1 + mentioned].startsWith("<@!")) {
				genMessageEvent("Please use a ping to indicate the user").execute(event);
			} else {
				String id = parsed[1 + mentioned].substring(3);
				id = id.substring(0, id.length()-1);
				
				// Don't let admins remove me as admin
				if (id.equals(SETTINGS.OWNER_ID)) {
					genMessageEvent("no u").execute(event);
				} else if (removeAdmin(id)) {
					genMessageEvent("Admin removed").execute(event);
					FileIO.saveAdminsFile(SETTINGS.ADMIN_FILE, admins);
				} else {
					genMessageEvent("This user is already not an admin").execute(event);
				}
			}
		});
		
		restrictedCommands.add("deladm");
		
		
		/**
		 * Set Status
		 */		
		commands.put("setstat", event -> {
			String content = event.getMessage().getContent().orElse("");
			int mentioned = 0;
			
			if (hasMention(content)) {
				mentioned = 1;
			}
			
			String[] parsed = content.split(" ", 2 + mentioned);
			
			if (parsed.length != 2 + mentioned) {
				genMessageEvent("Invalid # of arguments").execute(event);
			} else {
				PresenceBean presb = new PresenceBean();
				presb.setStatus(Status.ONLINE.getValue());
				ActivityBean act = new ActivityBean();
				act.setName(parsed[1 + mentioned]);
				act.setType(Activity.Type.LISTENING.getValue());
				presb.setActivity(act);
				Presence pres = new Presence(presb);
				
				client.updatePresence(pres).subscribe();
				
				FileIO.saveStatusFile(SETTINGS.STAT_FILE, parsed[1 + mentioned]);
			}
		});
		
		restrictedCommands.add("setstat");
		
		
		/**
		 * help menu
		 */		
		commands.put("help", event -> {
			String output = "Available Commands: ";
			
			for (String key : commands.keySet()) {
				if (!restrictedCommands.contains(key)) {
					output += key + ", ";
				}
			}
			
			output = output.substring(0, output.length()-2);
			genMessageEvent(output).execute(event);
		});
		
		
		
		/**
		 * help menu - admin
		 */		
		commands.put("helpadm", event -> {
			String output = "Admin Commands: ";
			
			for (String key : commands.keySet()) {
				if (restrictedCommands.contains(key)) {
					output += key + ", ";
				}
			}
			
			output = output.substring(0, output.length()-2);
			genMessageEvent(output).execute(event);
		});
		
		restrictedCommands.add("helpadm");
		
		
		
		/**
		 * UTC Scheduler
		 */
		commands.put("matchschedule", event -> {
			String content = event.getMessage().getContent().orElse("");
			int mentioned = 0;
			
			if (hasMention(content)) {
				mentioned = 1;
			}
			
			String[] parsed = content.split(" ");
			
			if (parsed.length < 2 + mentioned) {
				genMessageEvent("Invalid # of arguments").execute(event);
				genMessageEvent("Usage: " + SETTINGS.CMD_PREFIX + "matcheschedule PLAYER_1_TIMEZONE PLAYER_2_TIMEZONE ...").execute(event);
				genMessageEvent("Example for player 1 = UTC-5 and player 2 = UTC+2:").execute(event);
				genMessageEvent(SETTINGS.CMD_PREFIX + "matchschedule -5 2").execute(event);
			} else {
				boolean validInput = true;
				ArrayList<Integer> timezones = new ArrayList<Integer>();
				
				for (int i = 1 + mentioned; i < parsed.length; i++) {
					try {
						int timezone = Integer.parseInt(parsed[i]);
						
						timezones.add(timezone);
					} catch (Exception e) {
						validInput = false;
					}
				}
				
				if (validInput) {
					ArrayList<Integer> goodTimes = matchSchedule(timezones);
					String good = "Good Match Times: ";
					
					if (goodTimes.size() == 0) {
						good += "none :(";
					} else {
						for (int time : goodTimes) {
							good += time + " UTC, ";
						}
						
						//Remove trailing comma and space
						good = good.substring(0, good.length()-2);	
					}
					
					genMessageEvent(good).execute(event);
				} else {
					genMessageEvent("Error reading timezone inputs, make sure to only add the number and a \"-\" if applicable.  Example: " + SETTINGS.CMD_PREFIX + "matchschedule -5 2").execute(event);
				}
			}
		});
		
		
		return true;
	}
	

	
	private static void loadData() {
		/**
		 * Admins
		 */
		admins = FileIO.loadAdminsFromFile(SETTINGS.ADMIN_FILE);
		
		if (admins == null) {
			System.out.println("An error occured while loading admins from file!\n");
			admins = new ArrayList<String>();
		}
		
		/**
		 * Commands
		 */
		HashMap<String, String> comms = FileIO.loadCommandsFromFile(SETTINGS.COMM_FILE);
		
		if (comms == null) {
			System.out.println("An error occured while loading commands from file!\n");
		} else {
			for (String key : comms.keySet()) {
				addNewCommand(key, comms.get(key));
			}
		}
	}
	
	private static boolean addNewCommand(String prompt, String response) {
		// Command already exists or invalid command name entered
		if (commands.containsKey(prompt) || prompt.contains(",")) {
			return false;
		}
		
		commands.put(prompt, genMessageEvent(response));
		msgCommands.put(prompt, response);
		
		return true;
	}
	
	private static boolean removeCommand(String prompt) {
		// Command didn't exist
		if (!commands.containsKey(prompt)) {
			return false;
		}
		
		commands.remove(prompt);
		
		if (msgCommands.containsKey(prompt)) {
			msgCommands.remove(prompt);
		}
		
		return true;
	}
	
	private static boolean addNewAdmin(String admin) {
		// Admin already exists or invalid name entered
		if (admin.length() < 3 || admins.contains(admin)) {
			return false;
		}
		
		admins.add(admin);
		
		return true;
	}
	
	private static boolean removeAdmin(String admin) {
		// Admin didn't exist
		if (!admins.contains(admin)) {
			return false;
		}
		
		admins.remove(admin);
		
		return true;
	}
	
	private static Command genMessageEvent(String response) {
		return event -> {
			event.getMessage()
			.getChannel().block()
	        .createMessage(response).block();
		};
	}
	
	private static boolean hasMention(String content) {
		return (content.startsWith("<@!" + SNOW_BOT_ID + ">") || content.startsWith("<@" + SNOW_BOT_ID + ">"));
	}
	
	private static ArrayList<Integer> matchSchedule(ArrayList<Integer> timezones) {
		int[] timeUTC = new int[] {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23};
		ArrayList<Integer> goodMatchTimes = new ArrayList<Integer>();
		int earliestAllowedLocalTime = 10;
		int latestAllowedLocalTime = 22;

		
		for (int time : timeUTC) {
			boolean timeIsOk = true;
			//System.out.println("---- Looking at " + time + " UTC ----");
			
			for (int timezone : timezones) {
				int localTime = time + timezone;
				
				/**
				 *  Make sure localTime is adjusted to 0-23
				 */
				if (localTime < 0) {
					localTime = 24 + localTime;
					// Prev day in local time
				} else if (localTime > 23) {
					localTime = localTime - 24;
					// Next day in local time
				}
				
				if (localTime >= earliestAllowedLocalTime && localTime <= latestAllowedLocalTime) {
					// we good
				} else {
					timeIsOk = false; // :( time is bad for this person
				}
			}
			
			if (timeIsOk) {
				goodMatchTimes.add(time);
			}
		}
		
		return goodMatchTimes;
	}
}