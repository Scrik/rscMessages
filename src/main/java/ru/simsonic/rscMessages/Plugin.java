package ru.simsonic.rscMessages;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.mcstats.MetricsLite;
import ru.simsonic.utilities.CommandAnswerException;
import ru.simsonic.utilities.LanguageUtility;

public final class Plugin extends JavaPlugin
{
	private static final Logger consoleLog = Logger.getLogger("Minecraft");
	private static final String chatPrefix = "{_YL}[rscm] {GOLD}";
	private final Database connection = new Database(this);
	private final Commands commands = new Commands(this);
	private MetricsLite metrics;
	final HashMap<String, RowList> lists = new HashMap<>();
	@Override
	public void onLoad()
	{
		saveDefaultConfig();
		switch(getConfig().getInt("internal.version", 1))
		{
			case 1:
				// NEWEST VERSION
				break;
			default:
				// UNSUPPORTED VERSION
				break;
		}
		consoleLog.log(Level.INFO, "[rscMessages] Plugin has been loaded.");
	}
	@Override
	public void onEnable()
	{
		// Read settings 
		reloadConfig();
		// Setup connection
		final String hostname = getConfig().getString("settings.connection.hostname", "localhost:3306");
		final String username = getConfig().getString("settings.connection.username", "user");
		final String password = getConfig().getString("settings.connection.password", "pass");
		final String prefixes = getConfig().getString("settings.connection.prefixes", "rscm_");
		getConfig().set("settings.connection.hostname", hostname);
		getConfig().set("settings.connection.username", username);
		getConfig().set("settings.connection.password", password);
		getConfig().set("settings.connection.prefixes", prefixes);
		connection.Initialize("rscMessages", hostname, username, password, prefixes);
		// Metrics
		try
		{
			metrics = new MetricsLite(this);
			metrics.start();
			consoleLog.info("[rscMessages] Metrics enabled.");
		} catch(IOException ex) {
			consoleLog.log(Level.INFO, "[rscMessages][Metrics] Exception:\n{0}", ex.getLocalizedMessage());
		}
		// Fetch lists and schedule them
		lists.putAll(connection.fetch());
		scheduleBroadcasts();
		// Done
		consoleLog.log(Level.INFO, "[rscMessages] Plugin has been successfully enabled.");
	}
	@Override
	public void onDisable()
	{
		saveConfig();
		metrics = null;
		consoleLog.info("[rscMessages] Plugin has been disabled.");
	}
	private void scheduleBroadcasts()
	{
		final BukkitScheduler scheduler = getServer().getScheduler();
		for(final RowList list : lists.values())
		{
			list.task = scheduler.runTaskLater(this, new Runnable()
			{
				@Override
				public void run()
				{
					broadcastList(list);
					// Resetup itself again
					list.task = scheduler.runTaskLater(Plugin.this, this, 20 * list.delay_sec);
				}
			}, 20 * list.delay_sec);
		}
	}
	private void broadcastList(RowList list)
	{
		final RowMessage message = list.getNextMessage(getServer().getWorlds().get(0).getTime());
		broadcastMessage(message);
	}
	private void broadcastMessage(RowMessage message)
	{
		message.lastBroadcast = this.getServer().getWorlds().get(0).getTime();
		final String text = message.rowList.prefix + message.text;
		for(Player player : getServer().getOnlinePlayers())
			if(player.hasPermission("rscm.receive." + message.rowList.name.toLowerCase()))
				player.sendMessage(LanguageUtility.processStringStatic(text));
	}
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		try
		{
			switch(label.toLowerCase())
			{
				case "rscm":
					execute(sender, args);
					break;
			}
		} catch(CommandAnswerException ex) {
			for(String answer : ex.getMessageArray())
				sender.sendMessage(LanguageUtility.processStringStatic(chatPrefix + answer));
		}
		return true;
	}
	private void execute(CommandSender sender, String[] args) throws CommandAnswerException
	{
		if(args.length == 0)
			throw new CommandAnswerException("{MAGENTA}rscMessages {GRAY}" + getDescription().getVersion() + "{MAGENTA} by SimSonic.");
		final ArrayList<String> result = new ArrayList<>();
		final String command = args[0].toLowerCase();
		args = Arrays.copyOfRange(args, 1, (args.length > 5) ? args.length - 1 : 4);
		switch(command)
		{
			case "list":
				commands.list(sender, args[0]);
				return;
			case "add":
				String add_text = LanguageUtility.glue(Arrays.copyOfRange(args, 1, args.length - 1), " ");
				commands.add(sender, args[0], add_text);
				return;
			case "edit":
				int edit_id = -1;
				String edit_text;
				try
				{
					edit_id = Integer.parseInt(args[1]);
					edit_text = LanguageUtility.glue(Arrays.copyOfRange(args, 2, args.length - 2), " ");
				} catch(NumberFormatException ex) {
					edit_text = LanguageUtility.glue(Arrays.copyOfRange(args, 1, args.length - 1), " ");
				}
				commands.edit(sender, args[0], edit_id, edit_text);
				return;
			case "remove":
				int remove_id = -1;
				try
				{
					remove_id = Integer.parseInt(args[1]);
				} catch(NumberFormatException ex) {
				}
				commands.remove(sender, args[0], remove_id);
				return;
			case "set":
				int set_id = -1;
				try
				{
					set_id = Integer.parseInt(args[1]);
				} catch(NumberFormatException ex) {
				}
				commands.set(sender, args[0], args[1], set_id, args[3]);
				return;
			case "broadcast":
				int broadcast_id = -1;
				try
				{
					broadcast_id = Integer.parseInt(args[1]);
				} catch(NumberFormatException ex) {
				}
				// <list> [#]
				commands.broadcast(sender, args[0], broadcast_id);
				return;
			case "reload":
				if(sender.hasPermission("rscm.admin"))
				{
					reloadConfig();
					getPluginLoader().disablePlugin(this);
					getPluginLoader().enablePlugin(this);
				}
				return;
		}
		throw new CommandAnswerException(result);
	}
}